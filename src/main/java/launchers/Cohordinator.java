package launchers;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.istack.internal.logging.Logger;

import matcher.CategoryMatcher;
import matcher.DynamicCombinationsCalculator;
import matcher.FeatureExtractor;
import matcher.TrainingSetGenerator;
import models.dao.SchemasByLinkageIterator;
import models.generator.Configurations;
import models.generator.LaunchConfiguration;
import models.matcher.Schema;
import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import connectors.RConnector;

public class Cohordinator {

	private static Logger log = Logger.getLogger(Cohordinator.class);
	private FileDataConnector fdc;
	private Configurations config;
	private MongoDBConnector mdbc;
	private RConnector r;
	private SyntheticDatasetGenerator sdg;

	/**
	 * Standard constructor for coordinator
	 * 
	 * @param fdc
	 * @param config
	 * @param mdbc
	 * @param r
	 * @param sdg
	 */
	public Cohordinator(FileDataConnector fdc, Configurations config, MongoDBConnector mdbc, RConnector r,
			SyntheticDatasetGenerator sdg) {
		this.fdc = fdc;
		this.config = config;
		this.mdbc = mdbc;
		this.r = r;
		this.sdg = sdg;
	}

	// cardinality parameter is currently useless
	public Schema matchAllSourcesInCategory(String firstSource, SchemasByLinkageIterator schemasIterator, String category, CategoryMatcher cm,
			int cardinality, boolean useMI, boolean matchToOne) {

		Schema schema = new Schema();
		List<String> currentMatchSources = new ArrayList<>();
		currentMatchSources.add(firstSource);
		// match su tutte le altre sorgenti
		while(schemasIterator.hasNext()) {
			String s = schemasIterator.next();
			System.out.println("-->" + s + "<-- ");
			currentMatchSources.add(s);
			cm.getMatch(currentMatchSources, category, cardinality, schema, useMI, matchToOne);
		}

		return schema;
	}

	/*
	 * checks the schemas of the sources in the selected categories and returns the
	 * couples of sources with schemas that have an overlap of attribute (based on
	 * their names) above 50% of the smallest of the two sources.
	 */
	public Map<String, List<String>> findClonedSources(List<String> categories) {
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
				double minSize = (attributes1.size() > attributes2.size()) ? attributes2.size() : attributes1.size();
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

	public Map<String, List<String>> generateTrainingSets(List<String> categories,
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
				truePositives += counters.stream().mapToInt(Long::intValue).map(c -> dcc.calculateCombinations(c, 2))
						.sum();
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

	/**
	 * Train data, launch categorisation AND evaluation of results
	 */
	private void launchAlgorithm() {
		// Training / model loading
		boolean alreadyTrained = true;
		r.start();
		List<String> categories = config.getCategories();
		try {
			trainModelOrLoadExisting(alreadyTrained, categories);
			classifyAndEvaluate(categories);
			System.out.println("FINE VALUTAZIONE RISULTATI");
		} finally {
			r.stop();
		}
	}

	private void classifyAndEvaluate(List<String> categories) {
		// Classification
		System.out.println("INIZIO GENERAZIONE SCHEMA");
		CategoryMatcher cm = new CategoryMatcher(mdbc, r);
		boolean withReference = true;
		SchemasByLinkageIterator schemasIterator = new SchemasByLinkageIterator(mdbc);
		String firstSource = schemasIterator.next();
		Schema schema = matchAllSourcesInCategory(firstSource, schemasIterator, categories.get(0), cm, 0, true, withReference);
		fdc.printMatchSchema("clusters", schema);
		System.out.println("FINE GENERAZIONE SCHEMA");

		// Result Evaluation, only with synthetic data for the moment
		if (sdg != null) {
			System.out.println("INIZIO VALUTAZIONE RISULTATI");
			List<List<String>> clusters = schema.schema2Clusters();
			Map<String, Integer> sizes = sdg.getAttrLinkage();
			if (withReference) {
				clusters = clusters.stream().filter(cluster -> {
					boolean hasValidAttribute = false;
					for (String a : cluster) {
						String source = a.split("###")[1];
						if (source.equals(firstSource)) {
							hasValidAttribute = true;
							break;
						}
					}
					return hasValidAttribute;
				}).collect(Collectors.toList());
				String category = config.getCategories().get(0);
				List<String> validAttributes = mdbc.getSingleSchema(category, firstSource);
				sizes.keySet().retainAll(validAttributes);
			}
			evaluateSyntheticResults(clusters, sizes);
		}
	}

	private void trainModelOrLoadExisting(boolean alreadyTrained, List<String> categories) {
		if (alreadyTrained) {
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
	}
	
	public static void main(String[] args) { 
		// Setup
		log.info("INIZIO SETUP");
		LaunchConfiguration lc = LaunchConfiguration.getConfigurationFromArgs(args);
		FileDataConnector fdc = new FileDataConnector(lc.getConfigFile());
		Configurations config = new Configurations(fdc.readConfig());
		fdc.setDatasetPath(config.getDatasetPath());
		fdc.setRlPath(config.getRecordLinkagePath());
		fdc.setTsPath(config.getTrainingSetPath());
		MongoDBConnector mdbc = new MongoDBConnector(config.getMongoURI(), config.getDatabaseName(), fdc);
		RConnector r = new RConnector();

		// generate dataset if asked
		SyntheticDatasetGenerator sdg = null;
		if (args.length >= 2 && "GEN_DS".equals(args[1])) {
			sdg = new SyntheticDatasetGenerator(lc);
			sdg.launchGeneration(true);
		}

		Cohordinator c = new Cohordinator(fdc, config, mdbc, r, sdg);
		System.out.println("FINE SETUP");

		c.launchAlgorithm();
	}

}
