package launchers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import matcher.DynamicCombinationsCalculator;
import model.Source;
import models.generator.LaunchConfiguration;
import models.matcher.Schema;
import models.matcher.EvaluationMetrics;

/**
 * Launch controller for Agrawal.
 * <p>
 * Generates synthetic dataset (if configured), launches the algorithm using
 * {@link DatasetAlignmentAlgorithm} and evaluates the results.
 * 
 * @author federico
 *
 */
public class Cohordinator {

	public static void main(String[] args) {
		LaunchConfiguration setupConfiguration = LaunchConfiguration.setupConfiguration(args);
		System.out.println("UTILIZZARE DATASET SINTETICO? (S/N)");
		boolean useSynthDataset = false;
		try (Scanner scanner = new Scanner(System.in)) {
			useSynthDataset = Character.toLowerCase(scanner.next().charAt(0)) == 's';
		}
		DatasetAlignmentAlgorithm algorithm = DatasetAlignmentAlgorithm.datasetAlignmentFactory(setupConfiguration);

		// SCHEMA ALIGNMENT
		if (useSynthDataset)
			testAlignmentOnSyntheticDataset(algorithm, setupConfiguration);
		else
			algorithm.launchAlgorithmOnRealDataset();
	}

	public static void testAlignmentOnSyntheticDataset(DatasetAlignmentAlgorithm algorithm, LaunchConfiguration lc) {
		SyntheticDatasetGenerator sdg = SyntheticDatasetGenerator.sdgBuilder(lc);
		System.out.println("INIZIO GENERAZIONE DATASET SINTETICO");
		SyntheticDataOutputStat sdo = sdg.generateSyntheticData(true);
		System.out.println("FINE, statistiche: "+sdo.toString());
		System.out.println("INIZIO ALLINEAMENTO");
		Schema schema = algorithm.launchAlgorithmOnSyntheticDataset(sdo);

		// Result Evaluation
		System.out.println("INIZIO VALUTAZIONE RISULTATI");
		List<List<String>> clusters = schema.schema2Clusters();
		Map<String, Integer> sizes = sdo.getAttrLinkage();
		//FIXME move to configuration
		if (DatasetAlignmentAlgorithm.WITH_REFERENCE) {
			clusters = clusters.stream().filter(cluster -> {
				boolean hasValidAttribute = false;
				for (String a : cluster) {
					String source = a.split("###")[1];
					if (source.equals(schema.getSourceCatalogueName())) {
						hasValidAttribute = true;
						break;
					}
				}
				return hasValidAttribute;
			}).collect(Collectors.toList());
			String category = lc.getConf().getCategories().get(0);
			List<String> validAttributes = algorithm.getDao()
					.getSingleSchema(new Source(category, schema.getSourceCatalogueName()));
			sizes.keySet().retainAll(validAttributes);
		}
		EvaluationMetrics evaluateSyntheticResults = evaluateSyntheticResults(clusters, sizes);
		System.out.println(evaluateSyntheticResults.toString());
		System.out.println("FINE VALUTAZIONE RISULTATI");
	}
	
	public static EvaluationMetrics evaluateSyntheticResults(List<List<String>> clusters,
			Map<String, Integer> expectedClusterSizes) {
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
		return new EvaluationMetrics(p, r);
	}
}
