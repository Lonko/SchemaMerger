package launchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import connectors.FileDataConnector;
import connectors.MongoDbConnectionFactory;
import connectors.RConnector;
import connectors.dao.AlignmentDao;
import connectors.dao.MongoAlignmentDao;
import matcher.CategoryMatcher;
import matcher.FeatureExtractor;
import matcher.TrainingSetGenerator;
import model.Source;
import models.generator.Configurations;
import models.generator.LaunchConfiguration;
import models.matcher.Schema;

/**
 * Main class for the Agrawal dataset alignment algorithm
 * <p>
 * Input: dataset and configuration, output: clusters<br/>
 * The class gets the input using the {@link AlignmentDao} (tipically Mongo)
 * 
 * @author federico
 *
 */
public class DatasetAlignmentAlgorithm {

	/**
	 * List of websites in real dataset, ordered by linkage. In the dataset we
	 * already have a Schemas collection with sources, BUT we don't have the good
	 * order.
	 */
	// TODO 1 move to configuration 2 COMPUTE this order using dataset
	private static final List<String> WEBSITES_SORTED_REAL_DATASET = Arrays.asList("gosale.com", "price-hunt.com",
			"shopping.dealtime.com");

	/**
	 * Se TRUE, gli attributi delle sorgenti che non matchano con nessun attributo
	 * del catalogo vengono scartati. <br/>
	 * OLD doc by Marco: "se true simula un'operazione di sintesi dei prodotti"
	 * (???)
	 */
	static final boolean WITH_REFERENCE = false;

	private AlignmentDao dao;
	private FileDataConnector fdc;
	private RConnector r;
	private Configurations config;
	
	/**
	 * Factory method to generate this object
	 * @param dao
	 * @return
	 */
	public static  DatasetAlignmentAlgorithm datasetAlignmentFactory(LaunchConfiguration lc) {
		RConnector r = new RConnector(lc.getConf().getModelPath());
		MongoDbConnectionFactory factory = MongoDbConnectionFactory.getMongoInstance(lc.getConf().getMongoURI(),
				lc.getConf().getDatabaseName());
		AlignmentDao dao = new MongoAlignmentDao(factory);
		DatasetAlignmentAlgorithm algorithm = new DatasetAlignmentAlgorithm(dao, lc.getFdc(), r, lc.getConf());
		return algorithm;
	}

	public DatasetAlignmentAlgorithm(AlignmentDao dao, FileDataConnector fdc, RConnector r, Configurations config) {
		super();
		this.dao = dao;
		this.fdc = fdc;
		this.r = r;
		this.config = config;
	}
	
	public Schema launchAlgorithmOnSyntheticDataset(List<String> sourcesByLinkage) {
		try {
			r.start();
			List<String> categories = config.getCategories();
			// Training / model loading
			if (config.isAlreadyTrained()) {
				System.out.println("LOADING DEL MODEL");
				r.loadModel();
				System.out.println("FINE LOADING DEL MODEL");
			} else {
				System.out.println("INIZIO GENERAZIONE TRAINING SET");
				Map<String, List<String>> tSet = generateTrainingSets(categories, new HashMap<String, List<String>>());
				fdc.printTrainingSet("trainingSet", tSet.get(categories.get(0)));
				System.out.println("FINE GENERAZIONE TRAINING SET - INIZIO TRAINING");
				r.train(config.getTrainingSetPath() + "/trainingSet.csv");
				System.out.println("FINE TRAINING");
			}
	
			// Classification
			System.out.println("INIZIO GENERAZIONE SCHEMA");
			CategoryMatcher cm = new CategoryMatcher(this.dao, r);
			Schema schema = launchClassification(sourcesByLinkage, categories.get(0), cm, 0, true, WITH_REFERENCE);
			fdc.printMatchSchema("clusters", schema);
			System.out.println("FINE GENERAZIONE SCHEMA");
			return schema;
		} finally {
			r.stop();
		}
	}

	public void launchAlgorithmOnRealDataset() {
		try {
			r.start();
			// si possono definire più categorie nel fine di configurazione
			List<String> categories = config.getCategories();
			// Training / model loading
			if (config.isAlreadyTrained()) {
				System.out.println("LOADING DEL MODEL");
				r.loadModel();
				System.out.println("FINE LOADING DEL MODEL");
			} else {
				System.out.println("INIZIO GENERAZIONE TRAINING SET");
				String csPath = config.getTrainingSetPath() + "/clones.csv";
				fdc.printClonedSources("clones", findClonedSources(categories));
				Map<String, List<String>> clonedSources = fdc.readClonedSources(csPath);
				Map<String, List<String>> tSet = generateTrainingSets(categories, clonedSources);
				fdc.printTrainingSet("trainingSet", tSet.get(categories.get(0)));
				System.out.println("FINE GENERAZIONE TRAINING SET - INIZIO TRAINING");
				r.train(config.getTrainingSetPath() + "/trainingSet.csv");
				System.out.println("FINE TRAINING");
			}
			// Classification
			System.out.println("INIZIO GENERAZIONE SCHEMA");
			CategoryMatcher cm = new CategoryMatcher(this.dao, r);
			Schema schema = launchClassification(WEBSITES_SORTED_REAL_DATASET, categories.get(0), cm, 0, true,
					WITH_REFERENCE);
			fdc.printMatchSchema("clusters", schema);
			System.out.println("FINE GENERAZIONE SCHEMA");
		} finally {
			r.stop();
		}
	}

	// cardinality parameter is currently useless
	private Schema launchClassification(List<String> orderedWebsites, String category, CategoryMatcher cm,
			int cardinality, boolean useMI, boolean matchToOne) {
		
		String catalogueSourceName = orderedWebsites.get(0);
		Schema schema = new Schema(catalogueSourceName);
		List<String> currentMatchSources = new ArrayList<>();
		currentMatchSources.add(catalogueSourceName);
		// match su tutte le altre sorgenti
		for (int i = 1; i < orderedWebsites.size(); i++) {
			System.out.println("-->" + orderedWebsites.get(i) + "<-- (" + i + ")");
			currentMatchSources.add(orderedWebsites.get(i));
			cm.getMatch(currentMatchSources, category, cardinality, schema, useMI, matchToOne);
		}

		return schema;
	}

	/**
	 * checks the schemas of the sources in the selected categories and returns the
	 * couples of sources with schemas that have an overlap of attribute (based on
	 * their names) above 50% of the smallest of the two sources.
	 */
	private Map<Source, List<Source>> findClonedSources(List<String> categories) {
		Map<Source, List<Source>> clonedSources = new HashMap<>();
		Map<Source, List<String>> sourceSchemas = this.dao.getSchemas(categories);

		for (Source source1 : sourceSchemas.keySet()) {
			List<String> attributes1 = sourceSchemas.get(source1);
			for (Source source2 : sourceSchemas.keySet()) {
				if (source1.equals(source2))
					continue;
				if (!source1.getCategory().equals(source2.getCategory()))
					continue;
				List<String> attributes2 = sourceSchemas.get(source2);
				double minSize = (attributes1.size() > attributes2.size()) ? attributes2.size() : attributes1.size();
				Set<String> intersection = new HashSet<>(attributes1);
				intersection.retainAll(attributes2);
				if (intersection.size() >= (minSize / 2)) {
					List<Source> clones = clonedSources.getOrDefault(source1, new ArrayList<>());
					clones.add(source2);
					clonedSources.put(source1, clones);
				}
			}
		}

		return clonedSources;
	}

	/**
	 * Launch the generation of tranining sets (cf {@link TrainingSetGenerator})
	 * 
	 * @param mdbc
	 * @param categories
	 * @param clonedSources
	 * @return
	 */
	private Map<String, List<String>> generateTrainingSets(List<String> categories,
			Map<String, List<String>> clonedSources) {

		TrainingSetGenerator tsg = new TrainingSetGenerator(this.dao, new FeatureExtractor(), clonedSources);
		Map<String, List<String>> trainingSets = new HashMap<>();

		for (String category : categories) {
			System.out.println(category.toUpperCase());
			/*
			 * FIXME: non ha senso avere 2 numeri fissi (300 = quantità di sample, e
			 * soprattutto 10.000 = numero di esempi attesi), quando il numero di sorgenti,
			 * attributi ecc sono parametrabili. Bisognerebbe anzi che questi 2 numeri siano
			 * calcolati in funzione delle altre dimensioni, anche se questo calcolo non è
			 * semplice da definire
			 */
			trainingSets.put(category, tsg.getTrainingSetWithTuples(300, 10000, false, true, 0.25, category));
		}

		return trainingSets;
	}

	public AlignmentDao getDao() {
		//TODO is it ok?
		return this.dao;
	}
}
