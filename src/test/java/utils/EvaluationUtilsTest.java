package utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import models.matcher.EvaluationMetrics;

/**
 * Tester for {@link EvaluationUtils}, globally and for specific categories
 * @author federico
 *
 */
public class EvaluationUtilsTest {
	
	private static List<String> GLOBAL_CATEGORY = Arrays.asList("GLOBAL");
	
	/** A global category is always added, so we do not have to add a new one*/
	private static Map<String, Function<Integer, Boolean>> DUMMY_CATEGORY_FUNCTION = new HashMap<>();

	@Test
	public void testEvaluateSyntheticInWrongCluster() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 2), Arrays.asList(2)), 
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(0.333, 0.5)), DUMMY_CATEGORY_FUNCTION);
	}

	@Test
	public void testEvaluateSyntheticSplitOneCluster() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1), Arrays.asList(2), Arrays.asList(2)), 
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(1, 0.5)), DUMMY_CATEGORY_FUNCTION);
				
	}

	@Test
	public void testEvaluateSyntheticSplitOneBigCluster() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1), Arrays.asList(2, 2, 2), Arrays.asList(2, 2)),
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(1, 5. / 11)), DUMMY_CATEGORY_FUNCTION);
				
	}

	@Test
	public void testEvaluateSyntheticMerge2Clusters() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 2, 2), Arrays.asList(3)), 
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(0.333, 1)), DUMMY_CATEGORY_FUNCTION);
	}

	@Test
	public void testEvaluateSyntheticGroupsCorrect() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 1), Arrays.asList(2, 2, 2)),
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(1, 1)), DUMMY_CATEGORY_FUNCTION);
	}

	@Test
	public void testEvaluateSyntheticSingleGroupCorrect() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 1, 1)), 
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(1, 1)), DUMMY_CATEGORY_FUNCTION);
	}

	@Test
	public void testEvaluateSyntheticSingleGroupWrong() {
		evaluateSyntheticResultsHelper(
				Arrays.asList(Arrays.asList(1), Arrays.asList(1), Arrays.asList(1), Arrays.asList(1)),
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(Double.NaN, 0)), DUMMY_CATEGORY_FUNCTION);
	}

	@Test
	public void testEvaluateSyntheticAllSeparatedCorrect() {
		evaluateSyntheticResultsHelper(
				Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3), Arrays.asList(4)), 
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(Double.NaN, Double.NaN)), DUMMY_CATEGORY_FUNCTION);
	}

	@Test
	public void testEvaluateSyntheticAllSeparatedWrong() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 2, 3, 4)), 
				generateMap(GLOBAL_CATEGORY, new EvaluationMetrics(0, Double.NaN)), DUMMY_CATEGORY_FUNCTION);
	}
	
	/**
	 * Evaluation tests with different categories
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testEvaluateDataWithCategories() {
		evaluateSyntheticResultsHelper(Arrays.asList(
						Arrays.asList(1,1,1,1),
						Arrays.asList(2,2,2,3),
						Arrays.asList(3),
						Arrays.asList(105,106,107,108))
				, generateMap(Arrays.asList("less106", "more106", "GLOBAL"), new EvaluationMetrics(0.75,0.9), new EvaluationMetrics(0.0,Double.NaN), new EvaluationMetrics(0.5,0.9))
				, generateMap(Arrays.asList("less106", "more106"), x -> x < 100, x -> x > 100));
	}

	/**
	 * Example: {{1,1,2}, {2}} --> indicates that there are 2 clusters, and one of
	 * the elements are in the wrong one.
	 * 
	 * @param input
	 */
	private void evaluateSyntheticResultsHelper(List<List<Integer>> inputs, Map<String, EvaluationMetrics> expectedMetrics,
			Map<String, Function<Integer, Boolean>> functions) {
		Map<Integer, Integer> expectedClusters = inputs.stream().flatMap(List::stream)
				.collect(Collectors.groupingBy(Function.identity(), Collectors.reducing(0, e -> 1, Integer::sum)));
		Map<String, EvaluationMetrics> evaluate = EvaluationUtils.evaluate(inputs, expectedClusters, functions);
		for (Entry<String, EvaluationMetrics> entry : evaluate.entrySet()) {
			EvaluationMetrics expectedMetric = expectedMetrics.get(entry.getKey());
			EvaluationMetrics computedMetric = entry.getValue();
			assertEquals(expectedMetric.getPrecision(), computedMetric.getPrecision(), 0.01);
			assertEquals(expectedMetric.getRecall(), computedMetric.getRecall(), 0.01);
		}
	}
	
	private static <T> Map<String, T> generateMap(List<String> elements, @SuppressWarnings("unchecked") T...metrics) {
		Map<String, T> result = new HashMap<>();
		for (int i = 0 ; i < elements.size(); i++) {
			result.put(elements.get(i), metrics[i]);
		}
		return result;
	}
	
}
