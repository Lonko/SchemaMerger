package launchers;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import models.matcher.EvaluationMetrics;

public class CohordinatorTest {

	private DatasetAlignmentAlgorithm alignment;

	@Before
	public void setUp() throws Exception {
		this.alignment = new DatasetAlignmentAlgorithm(null, null, null, null);
	}

	@Test
	public void testEvaluateSyntheticInWrongCluster() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 2), Arrays.asList(2)), 0.333, 0.5);
	}

	@Test
	public void testEvaluateSyntheticSplitOneCluster() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1), Arrays.asList(2), Arrays.asList(2)), 1, 0.5);
	}

	@Test
	public void testEvaluateSyntheticSplitOneBigCluster() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1), Arrays.asList(2, 2, 2), Arrays.asList(2, 2)),
				1, 5. / 11);
	}

	@Test
	public void testEvaluateSyntheticMerge2Clusters() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 2, 2), Arrays.asList(3)), 0.333, 1);
	}

	@Test
	public void testEvaluateSyntheticGroupsCorrect() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 1), Arrays.asList(2, 2, 2)), 1, 1);
	}

	@Test
	public void testEvaluateSyntheticSingleGroupCorrect() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 1, 1, 1)), 1, 1);
	}

	@Test
	public void testEvaluateSyntheticSingleGroupWrong() {
		evaluateSyntheticResultsHelper(
				Arrays.asList(Arrays.asList(1), Arrays.asList(1), Arrays.asList(1), Arrays.asList(1)), 1, 0);
	}

	@Test
	public void testEvaluateSyntheticAllSeparatedCorrect() {
		evaluateSyntheticResultsHelper(
				Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3), Arrays.asList(4)), 1, 1);
	}

	@Test
	public void testEvaluateSyntheticAllSeparatedWrong() {
		evaluateSyntheticResultsHelper(Arrays.asList(Arrays.asList(1, 2, 3, 4)), 0, 1);
	}

	/**
	 * Example: {{1,1,2}, {2}} --> indicates that there are 2 clusters, and one of
	 * the elements are in the wrong one.
	 * 
	 * @param input
	 */
	private void evaluateSyntheticResultsHelper(List<List<Integer>> inputs, double expectedP, double expectedR) {
		Map<String, Integer> expectedClusters = inputs.stream().flatMap(List::stream).map(element -> element.toString())
				.collect(Collectors.groupingBy(e -> e, Collectors.reducing(0, e -> 1, Integer::sum)));
		List<List<String>> computedClusters = inputs.stream()
				.map(input -> input.stream().map(nb -> nb.toString() + "###test").collect(Collectors.toList()))
				.collect(Collectors.toList());
		EvaluationMetrics results = this.alignment.evaluateSyntheticResults(computedClusters, expectedClusters);
		assertEquals(expectedP, results.getPrecision(), 0.001);
		assertEquals(expectedR, results.getRecall(), 0.001);
	}

}
