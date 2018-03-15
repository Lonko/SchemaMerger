package launchers;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import matcher.CategoryMatcher;
import matcher.DynamicCombinationsCalculator;
import matcher.FeatureExtractor;
import matcher.TrainingSetGenerator;
import models.generator.Configurations;
import models.generator.LaunchConfiguration;
import models.matcher.Schema;
import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import connectors.RConnector;

public class Cohordinator {

    public Cohordinator() {

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

    /*
     * checks the schemas of the sources in the selected categories and returns
     * the couples of sources with schemas that have an overlap of attribute
     * (based on their names) above 50% of the smallest of the two sources.
     */
    public Map<String, List<String>> findClonedSources(MongoDBConnector mdbc, List<String> categories) {
        Map<String, List<String>> clonedSources = new HashMap<>();
        Map<String, List<String>> sourceSchemas = mdbc.getSchemas(categories);

        for (String source1 : sourceSchemas.keySet()) {
            String category1 = source1.split("###")[0];
            List<String> attributes1 = sourceSchemas.get(source1);
            for (String source2 : sourceSchemas.keySet()) {
                if (source1.equals(source2))
                    continue;
                String category2 = source2.split("###")[0];
                if (!category1.equals(category2))
                    continue;
                List<String> attributes2 = sourceSchemas.get(source2);
                double minSize = (attributes1.size() > attributes2.size()) ? attributes2.size() : attributes1
                        .size();
                Set<String> intersection = new HashSet<>(attributes1);
                intersection.retainAll(attributes2);
                if (intersection.size() >= (minSize / 2)) {
                    List<String> clones = clonedSources.getOrDefault(source1, new ArrayList<>());
                    clones.add(source2);
                    clonedSources.put(source1, clones);
                }
            }
        }

        return clonedSources;
    }

    public Map<String, List<String>> generateTrainingSets(MongoDBConnector mdbc, List<String> categories,
            Map<String, List<String>> clonedSources) {

        TrainingSetGenerator tsg = new TrainingSetGenerator(mdbc, new FeatureExtractor(), clonedSources);
        Map<String, List<String>> trainingSets = new HashMap<>();

        for (String category : categories) {
            System.out.println(category.toUpperCase());
            trainingSets.put(category, tsg.getTrainingSetWithTuples(300, 10000, false, true, 0.25, category));
        }

        return trainingSets;
    }

    public void evaluateSyntheticResults(List<List<String>> clusters, Map<String, Integer> expectedClusterSizes) {
        DynamicCombinationsCalculator dcc = new DynamicCombinationsCalculator();
        int truePositives = 0, falsePositives = 0, expectedPositives = 0;
        double p, r, f;

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
                truePositives += counters.stream().mapToInt(Long::intValue)
                        .map(c -> dcc.calculateCombinations(c, 2)).sum();
                // false positives
                falsePositives += counters.stream().mapToInt(Long::intValue).map(c -> c * (size - c)).sum();
            }
        }

        p = truePositives / (double) (truePositives + falsePositives);
        r = truePositives / (double) (expectedPositives);
        f = 2 * (p * r) / (p + r);

        System.out.println("PRECISION = " + p);
        System.out.println("RECALL = " + r);
        System.out.println("F-MEASURE = " + f);
    }
    
    public void alignmentSyntheticDataset(FileDataConnector fdc, MongoDBConnector mdbc, RConnector r, 
                                          Configurations config){
        // Synthetic dataset generation
        System.out.println("INIZIO GENERAZIONE DATASET");
        SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(fdc, mdbc, config);
        boolean reset = true;
        sdg.generateCatalogue(reset);
        sdg.generateSources(reset);
        System.out.println("FINE GENERAZIONE DATASET");

        boolean alreadyTrained = true;
        r.start();
        List<String> categories = config.getCategories();
        try {
            // Training / model loading
            if (alreadyTrained) {
                System.out.println("LOADING DEL MODEL");
                r.loadModel(config.getModelPath());
                System.out.println("FINE LOADING DEL MODEL");
            } else {
                System.out.println("INIZIO GENERAZIONE TRAINING SET");
                Map<String, List<String>> tSet = generateTrainingSets(mdbc, categories,
                        new HashMap<String, List<String>>());
                fdc.printTrainingSet("trainingSet", tSet.get(categories.get(0)));
                System.out.println("FINE GENERAZIONE TRAINING SET - INIZIO TRAINING");
                r.train(config.getTrainingSetPath() + "/trainingSet.csv", config.getModelPath());
                System.out.println("FINE TRAINING");
            }

            // Classification
            System.out.println("INIZIO GENERAZIONE SCHEMA");
            CategoryMatcher cm = new CategoryMatcher(mdbc, r);
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
                List<String> validAttributes = mdbc.getSingleSchema(category, sources.get(0));
                sizes.keySet().retainAll(validAttributes);
            }
            evaluateSyntheticResults(clusters, sizes);
            System.out.println("FINE VALUTAZIONE RISULTATI");
        } finally {
            r.stop();
        }
    }
    
    public void alignmentRealDataset(FileDataConnector fdc, MongoDBConnector mdbc, RConnector r, 
                                     Configurations config){
        // Training / model loading
        boolean alreadyTrained = true;
        r.start();
        //si possono definire più categorie nel fine di configurazione
        List<String> categories = config.getCategories();
        
        /* È necessario avere la lista di sorgenti ordinate sulle quali fare
         * schema alignment
         */
        List<String> sources = Arrays.asList("gosale.com", "price-hunt.com", "shopping.dealtime.com");

        try{
            // Training / model loading
            if (alreadyTrained) {
                System.out.println("LOADING DEL MODEL");
                r.loadModel(config.getModelPath());
                System.out.println("FINE LOADING DEL MODEL");
            } else {
                System.out.println("INIZIO GENERAZIONE TRAINING SET");
                String csPath = config.getTrainingSetPath() + "/clones.csv";
                fdc.printClonedSources("clones", findClonedSources(mdbc, categories));
                Map<String, List<String>> clonedSources = fdc.readClonedSources(csPath);
                Map<String, List<String>> tSet = generateTrainingSets(mdbc, categories, clonedSources);
                fdc.printTrainingSet("trainingSet", tSet.get(categories.get(0)));
                System.out.println("FINE GENERAZIONE TRAINING SET - INIZIO TRAINING");
                r.train(config.getTrainingSetPath() + "/trainingSet.csv", config.getModelPath());
                System.out.println("FINE TRAINING");
            }
            // Classification
            System.out.println("INIZIO GENERAZIONE SCHEMA");
            CategoryMatcher cm = new CategoryMatcher(mdbc, r);
            //withReference -> se true simula un'operazione di sintesi dei prodotti.
            boolean withReference = false;
            Schema schema = matchAllSourcesInCategory(sources, categories.get(0), cm, 0, true, withReference);
            fdc.printMatchSchema("clusters", schema);
            System.out.println("FINE GENERAZIONE SCHEMA");
        } finally {
        r.stop();
        }
    }

    public static void main(String[] args) {
        Cohordinator c = new Cohordinator();
        System.out.println("UTILIZZARE DATASET SINTETICO? (S/N)");
        Scanner scanner = new Scanner(System.in);
        boolean useSynthDataset = Character.toLowerCase(scanner.next().charAt(0)) == 's';
        scanner.close();

        // Setup
        System.out.println("INIZIO SETUP");
        FileDataConnector fdc = new FileDataConnector();
        Configurations config = new Configurations(fdc.readConfig());
        fdc.setDatasetPath(config.getDatasetPath());
        fdc.setRlPath(config.getRecordLinkagePath());
        fdc.setTsPath(config.getTrainingSetPath());
        MongoDBConnector mdbc = new MongoDBConnector(config.getMongoURI(), config.getDatabaseName(), fdc);
        RConnector r = new RConnector();
        System.out.println("FINE SETUP");
        
        //SCHEMA ALIGNMENT
        if(useSynthDataset)
            c.alignmentSyntheticDataset(fdc, mdbc, r, config);
        else
            c.alignmentRealDataset(fdc, mdbc, r, config);
    }
}
