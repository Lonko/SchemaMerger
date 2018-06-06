package launchers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

import model.HeadOrTail;
import model.Source;
import model.SyntheticAttribute;
import models.generator.LaunchConfiguration;
import models.matcher.EvaluationMetrics;
import models.matcher.Schema;
import utils.EvaluationUtils;

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
		System.out.println("FINE, statistiche: " + sdo.toString());
		System.out.println("INIZIO ALLINEAMENTO");
		Schema schema = algorithm.launchAlgorithmOnSyntheticDataset(sdo.getSourcesNamesByLinkage());

		// Result Evaluation
		System.out.println("INIZIO VALUTAZIONE RISULTATI");
		List<List<String>> clusters = schema.schema2Clusters();
		Map<SyntheticAttribute, Integer> sizes = sdo.getAttrLinkage();
		// FIXME move to configuration
		//If WITHREFERENCE is true, I keep only clusters with at least one source attribute pertaining to the first source (considered as catalog) 
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
			List<SyntheticAttribute> validAttributes = algorithm.getDao()
					.getSingleSchema(new Source(category, schema.getSourceCatalogueName())).stream()
					.map(attr -> SyntheticAttribute.parseAttribute(attr)).collect(Collectors.toList());
			sizes.keySet().retainAll(validAttributes);
		}
		evaluateSyntheticResults(clusters, sizes);
	}

	/**
	 * Evaluate synthetic results according to several categories (
	 * @param clusters
	 * @param expectedClusterSizes
	 */
	public static void evaluateSyntheticResults(List<List<String>> clusters,
			Map<SyntheticAttribute, Integer> expectedClusterSizes) {
		//Attributes in clusters contain their source name, so we remove it (it is not included in expectedClusterSizes)
		
		//TODO here we should also test the sources H/T. NOT SO SIMPLE because of expectedClusterSizes that should be changed
		List<List<SyntheticAttribute>> clustersWithoutSourceName = clusters.stream().map(listSources -> 
			listSources.stream().
				map(sa -> SyntheticAttribute.parseAttribute(sa.split("###")[0]))
				.collect(Collectors.toList())).collect(Collectors.toList());
		
		Map<String, EvaluationMetrics> evaluate = EvaluationUtils.evaluate(clustersWithoutSourceName, expectedClusterSizes, CATEGORY2FUNCTION);
		System.out.println(evaluate.toString());
		System.out.println("FINE VALUTAZIONE RISULTATI");
	}

	/**
	 * This is the map containing the categories for evaluation. For each category
	 * an {@link EvaluationMetrics} will be produced
	 */
	@SuppressWarnings("serial")
	public static final Map<String, Function<SyntheticAttribute, Boolean>> CATEGORY2FUNCTION = new HashMap<String, Function<SyntheticAttribute, Boolean>>() {
		{
			put("cardinality2", elem -> elem.getCardinality() == 2);
			put("cardinality3", elem -> elem.getCardinality() == 3);
			put("cardinality4-10", elem -> 4 <= elem.getCardinality() && elem.getCardinality() <= 10);
			put("cardinality10+", elem -> 4 <= elem.getCardinality() && elem.getCardinality() <= 10);
			put("HEAD attributes", elem -> elem.getHeadOrTail().equals(HeadOrTail.H));
			put("TAIL attributes", elem -> elem.getHeadOrTail().equals(HeadOrTail.T));
			put("Error rate <0.1", elem -> elem.getErrorRate() < 0.1);
			put("Error rate >=0.1", elem -> elem.getErrorRate() >= 0.1);
		}
	};

}
