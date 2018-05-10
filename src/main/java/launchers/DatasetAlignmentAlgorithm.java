package launchers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import connectors.FileDataConnector;
import connectors.RConnector;
import connectors.dao.AlignmentDao;
import matcher.CategoryMatcher;
import matcher.DynamicCombinationsCalculator;
import matcher.FeatureExtractor;
import matcher.TrainingSetGenerator;
import model.Source;
import models.generator.Configurations;
import models.matcher.EvaluationMetrics;
import models.matcher.Schema;

/**
 * Main class for the Agrawal dataset alignment algorithm
 * 
 * @author federico
 *
 */
public class DatasetAlignmentAlgorithm {
	
	/**
	 * Define whether the model has already been trained
	 */
	//TODO move to configuration/args
    private static final boolean ALREADY_TRAINED = true;
    
    /**
     * List of websites in real dataset, ordered by linkage.
     * In the dataset we already have a Schemas collection with sources, BUT we don't have the
     * good order.
     */
    // TODO 1 move to configuration 2 COMPUTE this order using dataset
	private static final List<String> WEBSITES_SORTED_REAL_DATASET = Arrays.asList("gosale.com", "price-hunt.com", "shopping.dealtime.com");

	/**
	 * Se TRUE, gli attributi delle sorgenti che non matchano con nessun attributo
	 * del catalogo vengono scartati. <br/>
	 * OLD doc by Marco: "se true simula un'operazione di sintesi dei prodotti"
	 * (???)
	 */
	private static final boolean WITH_REFERENCE = false;
	
	private AlignmentDao dao;
	private FileDataConnector fdc; 
	private RConnector r; 
	private Configurations config;
			
	public DatasetAlignmentAlgorithm(AlignmentDao dao, FileDataConnector fdc, RConnector r, Configurations config) {
		super();
		this.dao = dao;
		this.fdc = fdc;
		this.r = r;
		this.config = config;
	}

	// cardinality parameter is currently useless
    public Schema matchAllSourcesInCategory(List<String> orderedWebsites, String category,
                        CategoryMatcher cm, int cardinality, boolean useMI, boolean matchToOne) {

        Schema schema = new Schema();
        List<String> currentMatchSources = new ArrayList<>();
        currentMatchSources.add(orderedWebsites.get(0));
        // match su tutte le altre sorgenti
        for (int i = 1; i < orderedWebsites.size(); i++) {
            System.out.println("-->" + orderedWebsites.get(i) + "<-- (" + i + ")");
            currentMatchSources.add(orderedWebsites.get(i));
            cm.getMatch(currentMatchSources, category, cardinality, schema, useMI, matchToOne);
        }

        return schema;
    }

    /**
     * checks the schemas of the sources in the selected categories and returns
     * the couples of sources with schemas that have an overlap of attribute
     * (based on their names) above 50% of the smallest of the two sources.
     */
    public Map<Source, List<Source>> findClonedSources(List<String> categories) {
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
                double minSize = (attributes1.size() > attributes2.size()) ? attributes2.size() : attributes1
                        .size();
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
    public Map<String, List<String>> generateTrainingSets(List<String> categories,
            Map<String, List<String>> clonedSources) {

        TrainingSetGenerator tsg = new TrainingSetGenerator(this.dao, new FeatureExtractor(), clonedSources);
        Map<String, List<String>> trainingSets = new HashMap<>();

        for (String category : categories) {
            System.out.println(category.toUpperCase());
            trainingSets.put(category, tsg.getTrainingSetWithTuples(300, 10000, false, true, 0.25, category));
        }

        return trainingSets;
    }

    public EvaluationMetrics evaluateSyntheticResults(List<List<String>> clusters, Map<String, Integer> expectedClusterSizes) {
        DynamicCombinationsCalculator dcc = new DynamicCombinationsCalculator();
        int truePositives = 0, falsePositives = 0, expectedPositives = 0;
        double p, r;

        // calculate expected positives
        for (int clusterSize : expectedClusterSizes.values())
            // cluster of cardinality == 1 are not considered
            if (clusterSize > 1)
                expectedPositives += dcc.calculateCombinations(clusterSize, 2);

        // calculate true and false positives
        for (List<String> cluster : clusters) {
            int size = cluster.size();
            // cluster of cardinality == 1 are not considered
            if (size > 1) {
                Collection<Long> cCollection = cluster.stream().map(attr -> attr.split("###")[0])
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).values();
                List<Long> counters = new ArrayList<>(cCollection);
                // true positives
                int truePositivesCluster = counters.stream().mapToInt(Long::intValue)
                        .map(c -> dcc.calculateCombinations(c, 2)).sum();
				truePositives += truePositivesCluster;
                // false positives
                falsePositives += dcc.calculateCombinations(size, 2) - truePositivesCluster;
            }
        }
        int computedPositives = truePositives + falsePositives;
		p = computedPositives == 0 ? 1 : truePositives / (double) computedPositives;
        r = expectedPositives == 0 ? 1 : truePositives / (double) (expectedPositives);
        return new EvaluationMetrics(p,r);
    }
    
    public void alignmentSyntheticDataset(){
        // Synthetic dataset generation
        System.out.println("INIZIO GENERAZIONE DATASET");
        SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(fdc, config);
        boolean reset = true;
        sdg.generateCatalogue(reset);
        sdg.generateSources(reset);
        System.out.println("FINE GENERAZIONE DATASET");

        r.start();
        List<String> categories = config.getCategories();
        try {
            // Training / model loading
            if (ALREADY_TRAINED) {
                System.out.println("LOADING DEL MODEL");
                r.loadModel(config.getModelPath());
                System.out.println("FINE LOADING DEL MODEL");
            } else {
                System.out.println("INIZIO GENERAZIONE TRAINING SET");
                Map<String, List<String>> tSet = generateTrainingSets(categories,
                        new HashMap<String, List<String>>());
                fdc.printTrainingSet("trainingSet", tSet.get(categories.get(0)));
                System.out.println("FINE GENERAZIONE TRAINING SET - INIZIO TRAINING");
                r.train(config.getTrainingSetPath() + "/trainingSet.csv", config.getModelPath());
                System.out.println("FINE TRAINING");
            }

            // Classification
            System.out.println("INIZIO GENERAZIONE SCHEMA");
            CategoryMatcher cm = new CategoryMatcher(this.dao, r);
            boolean withReference = true;
            List<String> sources = sdg.getSourcesByLinkage();
            Schema schema = matchAllSourcesInCategory(sources, categories.get(0), cm, 0, true, withReference);
            fdc.printMatchSchema("clusters", schema);
            System.out.println("FINE GENERAZIONE SCHEMA");

            // Result Evaluation
            System.out.println("INIZIO VALUTAZIONE RISULTATI");
            List<List<String>> clusters = schema.schema2Clusters();
            Map<String, Integer> sizes = sdg.getAttrLinkage();
            if(withReference){
                clusters = clusters.stream()
                                   .filter(cluster -> {
                                       boolean hasValidAttribute = false;
                                       for(String a : cluster){
                                           String source = a.split("###")[1];
                                           if(source.equals(sources.get(0))){
                                               hasValidAttribute = true;
                                               break;
                                           }
                                       }
                                       return hasValidAttribute;
                                   })
                                   .collect(Collectors.toList());
                String category = config.getCategories().get(0);
                List<String> validAttributes = this.dao.getSingleSchema(new Source(category, sources.get(0)));
                sizes.keySet().retainAll(validAttributes);
            }
            EvaluationMetrics evaluateSyntheticResults = evaluateSyntheticResults(clusters, sizes);
            System.out.println(evaluateSyntheticResults.toString());
            System.out.println("FINE VALUTAZIONE RISULTATI");
        } finally {
            r.stop();
        }
    }
    
    public void alignmentRealDataset(){
        r.start();
        //si possono definire più categorie nel fine di configurazione
        List<String> categories = config.getCategories();
        
        try{
            // Training / model loading
            if (ALREADY_TRAINED) {
                System.out.println("LOADING DEL MODEL");
                r.loadModel(config.getModelPath());
                System.out.println("FINE LOADING DEL MODEL");
            } else {
                System.out.println("INIZIO GENERAZIONE TRAINING SET");
                String csPath = config.getTrainingSetPath() + "/clones.csv";
                fdc.printClonedSources("clones", findClonedSources( categories));
                Map<String, List<String>> clonedSources = fdc.readClonedSources(csPath);
                Map<String, List<String>> tSet = generateTrainingSets(categories, clonedSources);
                fdc.printTrainingSet("trainingSet", tSet.get(categories.get(0)));
                System.out.println("FINE GENERAZIONE TRAINING SET - INIZIO TRAINING");
                r.train(config.getTrainingSetPath() + "/trainingSet.csv", config.getModelPath());
                System.out.println("FINE TRAINING");
            }
            // Classification
            System.out.println("INIZIO GENERAZIONE SCHEMA");
            CategoryMatcher cm = new CategoryMatcher(this.dao, r);
            Schema schema = matchAllSourcesInCategory(WEBSITES_SORTED_REAL_DATASET, categories.get(0), cm, 0, true, WITH_REFERENCE);
            fdc.printMatchSchema("clusters", schema);
            System.out.println("FINE GENERAZIONE SCHEMA");
        } finally {
        r.stop();
        }
    }
}
