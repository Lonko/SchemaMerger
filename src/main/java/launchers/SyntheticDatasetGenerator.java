package launchers;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

import connectors.FileDataConnector;
import connectors.MongoDbConnectionFactory;
import connectors.dao.SyntheticDatasetDao;
import connectors.dao.MongoSyntheticDao;
import generator.CatalogueGenerator;
import generator.DictionaryStringGenerator;
import generator.RandomStringGenerator;
import generator.SourcesGenerator;
import generator.StringGenerator;
import model.CatalogueProductPage;
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
    private Map<String, String> attrFixedTokens;
    private Map<String, List<String>> attrValues;
    private List<String> sourcesBySize;
    private List<String> sourcesByLinkage;
    /** @see #getAttrLinkage() */
    private Map<String, Integer> attrLinkage;
    
    private SyntheticDatasetDao catalogueDao;
     
    public SyntheticDatasetGenerator(FileDataConnector fdc) {
    	this(fdc, new Configurations(fdc.readConfig()));
    }
    
    public SyntheticDatasetGenerator(FileDataConnector fdc, Configurations conf) {
		this(conf, generateSGFromConf(conf), generateDaoFromConf(fdc, conf));
	}    
    
    private static SyntheticDatasetDao generateDaoFromConf(FileDataConnector fdc2, Configurations conf2) {
    	MongoDbConnectionFactory factory = MongoDbConnectionFactory.getMongoInstance(conf2.getMongoURI(), conf2.getDatabaseName());
		return new MongoSyntheticDao(factory);
	}

	private static StringGenerator generateSGFromConf(Configurations conf2) {
        String path = conf2.getStringPathFile();
        if (path.equals(""))
            return new RandomStringGenerator(SOURCE_NAME_LENGTH, ATTRIBUTE_NAME_LENGTH, TOKEN_LENGTH);
        else
            return new DictionaryStringGenerator(path);
	}

	public SyntheticDatasetGenerator(Configurations conf, StringGenerator sg,
			SyntheticDatasetDao catalogueDao) {
		super();
		this.conf = conf;
		this.sg = sg;
		this.catalogueDao = catalogueDao;
	}

	// generate and upload catalogue
    public void generateCatalogue(boolean delete) {
        CatalogueGenerator cg = new CatalogueGenerator(this.conf, this.sg);
        List<CatalogueProductPage> catalogue = cg.createCatalogue();
        this.catalogueDao.uploadCatalogue(catalogue, delete);
        this.sizes = cg.getSizeCurve();
        this.prodLinkage = cg.getProductLinkageCurve();
        this.attrFixedTokens = cg.getAttrFixedToken();
        this.attrValues = cg.getAttrValues();
    }

    // generate and upload sources
    public void generateSources(boolean delete) {
        SourcesGenerator sg = new SourcesGenerator(this.catalogueDao, this.conf, this.sg, this.sizes,
                this.prodLinkage, this.attrFixedTokens, this.attrValues);
        this.sourcesBySize = sg.prepareSources();
        this.attrLinkage = sg.createSources(this.sourcesBySize, delete);
        this.sourcesByLinkage = sg.getLinkageOrder(this.sourcesBySize);
    }

    public int getCatalogueSize() {
        return this.prodLinkage.getYValues().length;
    }

    public int getDatasetSize() {
        return this.prodLinkage.getSampling();
    }

    public int getSourceSize(String name) {
        int i = this.sourcesBySize.indexOf(name);
        return this.sizes.getYValues()[i];
    }

    public List<String> getSourcesBySize() {
        return this.sourcesBySize;
    }

    public List<String> getSourcesByLinkage() {
        return this.sourcesByLinkage;
    }

    /**
	 * Number of source attributes for each cluster (all correspondent attributes
	 * have same name in synthetic, sot it can be used as map key)
	 *
     * @return
     */
    public Map<String, Integer> getAttrLinkage() {
        return this.attrLinkage;
    }

    public static void main(String[] args) {
        LaunchConfiguration lc = LaunchConfiguration.getConfigurationFromArgs(args);
        FileDataConnector fdc = new FileDataConnector(lc.getConfigFile());
        SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(fdc);
        System.out.println("DELETE EXISTING DATASET? (Y/N)");
        try (Scanner scanner = new Scanner(System.in)){
        	boolean delete = Character.toLowerCase(scanner.next().charAt(0)) == 'y';
        	System.out.println("Generate catalogue...");
	        long start = System.currentTimeMillis();
	        sdg.generateCatalogue(delete);
	        long middle = System.currentTimeMillis();
        	System.out.println("Generate sources...");
	        sdg.generateSources(delete);
	        long end = System.currentTimeMillis();
	        
	        long timeForCatalogue = middle - start;
	        long timeForDataset = end - middle;
	        long catalogueTime = timeForCatalogue / 1000;
	        long datasetTime = timeForDataset / 1000;
	        long totalTime = (end - start) / 1000;
	        
	
	        System.out.println("Prodotti nel catalogo: " + sdg.getCatalogueSize());
	        System.out.println("Prodotti nel dataset: " + sdg.getDatasetSize());
	        System.out.println("Tempo di generazione del Catalogo: " + (catalogueTime / 60) + " min "
	                + (catalogueTime % 60) + " sec");
	        System.out.println("Tempo di generazione del Dataset: " + (datasetTime / 60) + " min "
	                + (datasetTime % 60) + " s ");
	        System.out.println("Tempo di esecuzione totale: " + (totalTime / 60) + " min " + (totalTime % 60)
	                + " s ");
        } 
    }
}
