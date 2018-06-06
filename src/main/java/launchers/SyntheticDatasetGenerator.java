package launchers;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import connectors.MongoDbConnectionFactory;
import connectors.dao.MongoSyntheticDao;
import connectors.dao.SyntheticDatasetDao;
import generator.CatalogueGenerator;
import generator.DictionaryStringGenerator;
import generator.RandomStringGenerator;
import generator.SourcesGenerator;
import generator.StringGenerator;
import model.CatalogueProductPage;
import model.SyntheticAttribute;
import model.SyntheticSource;
import models.generator.Configurations;
import models.generator.CurveFunction;
import models.generator.LaunchConfiguration;

/**
 * Main class for synthetic dataset generation
 * 
 * @author federico
 *
 */
public class SyntheticDatasetGenerator {

	private static final int SOURCE_NAME_LENGTH = 15;
	private static final int ATTRIBUTE_NAME_LENGTH = 7;
	private static final int TOKEN_LENGTH = 4;
	private Configurations conf;
	private StringGenerator sg;
	private CurveFunction sizes;
	private CurveFunction prodLinkage;
	private Map<SyntheticAttribute, String> attrFixedTokens;
	private Map<SyntheticAttribute, List<String>> attrValues;
	private List<SyntheticSource> sourcesBySize;
	private List<SyntheticSource> sourcesByLinkage;
	/** @see #getAttrLinkage() */
	private Map<SyntheticAttribute, Integer> attrLinkage;
	private SyntheticDatasetDao catalogueDao;
	private CurveFunction sourcesPerAttribute;
	
	/**
	 * Factory method for {@link SyntheticDatasetGenerator}
	 * @param lc
	 * @return
	 */
	public static SyntheticDatasetGenerator sdgBuilder(LaunchConfiguration lc) {
		String path = lc.getConf().getStringPathFile();
		StringGenerator stringGenerator;
		if (path.equals("")) {
			stringGenerator = new RandomStringGenerator(SOURCE_NAME_LENGTH, ATTRIBUTE_NAME_LENGTH, TOKEN_LENGTH);
		} else
			stringGenerator = new DictionaryStringGenerator(path);
		
		MongoDbConnectionFactory factory = MongoDbConnectionFactory.getMongoInstance(lc.getConf().getMongoURI(),
				lc.getConf().getDatabaseName());
		MongoSyntheticDao dao = new MongoSyntheticDao(factory);
		
		SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(lc.getConf(), stringGenerator, 
				dao);
				//new SyntheticDatasetDaoMock(false));
		return sdg;
	}

	public SyntheticDatasetGenerator(Configurations conf, StringGenerator sg, SyntheticDatasetDao catalogueDao) {
		super();
		this.conf = conf;
		this.sg = sg;
		this.catalogueDao = catalogueDao;
	}

	// generate and upload catalogue
	private void generateCatalogue(boolean delete) {
		CatalogueGenerator cg = new CatalogueGenerator(this.conf, this.sg);
		List<CatalogueProductPage> catalogue = cg.createCatalogue();
		this.catalogueDao.uploadCatalogue(catalogue, delete);
		this.sizes = cg.getSources2nbPagesCurve();
		this.prodLinkage = cg.getProduct2nbPagesInLinkageCurve();
		this.sourcesPerAttribute = cg.getAttribute2nbSourcesCurve();
		this.attrFixedTokens = cg.getAttrFixedToken();
		this.attrValues = cg.getAttrValues();
	}

	/**
	 *  generate and upload sources
	 * @param delete
	 */
	private void generateSources(boolean delete) {
		SourcesGenerator sg = new SourcesGenerator(this.catalogueDao, this.conf, this.sg, this.sizes, this.prodLinkage, this.sourcesPerAttribute,
				this.attrFixedTokens, this.attrValues);
		this.sourcesBySize = sg.prepareSources();
		this.attrLinkage = sg.createSources(this.sourcesBySize, delete);
		this.sourcesByLinkage = sg.getLinkageOrder(this.sourcesBySize);
	}

	public SyntheticDataOutputStat generateSyntheticData(boolean delete) {
		System.out.println("Generate catalogue...");
		long start = System.currentTimeMillis();
		generateCatalogue(delete);
		long middle = System.currentTimeMillis();
		System.out.println("Generate sources...");
		generateSources(delete);
		long end = System.currentTimeMillis();
		
		long timeForCatalogue = middle - start;
		long timeForDataset = end - middle;
		long catalogueTime = timeForCatalogue / 1000;
		long datasetTime = timeForDataset / 1000;
		long totalTime = (end - start) / 1000;
		
		SyntheticDataOutputStat syntheticDataOutputStat = new SyntheticDataOutputStat(
				sourcesByLinkage, attrLinkage, catalogueTime, datasetTime, totalTime, 
				this.prodLinkage.getYValues().length, this.prodLinkage.getSampling());
		return syntheticDataOutputStat;
	}
	
	public static void main(String[] args) {
		LaunchConfiguration setupConfiguration = LaunchConfiguration.setupConfiguration(args);
		SyntheticDatasetGenerator sdg = sdgBuilder(setupConfiguration);
		System.out.println("DELETE EXISTING DATASET? (Y/N)");
		try (Scanner scanner = new Scanner(System.in)) {
			boolean delete = Character.toLowerCase(scanner.next().charAt(0)) == 'y';
			SyntheticDataOutputStat output = sdg.generateSyntheticData(delete);
			System.out.println("END, statistics on output: "+output.toString());
		}
	}
	
}
