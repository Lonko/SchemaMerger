package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import matcher.DynamicCombinationsCalculator;
import models.matcher.EvaluationMetrics;
import models.matcher.EvaluationMetricsBuilder;

public class EvaluationUtils {

	private static final String GLOBAL = "GLOBAL";

	/**
	 * Evaluate clustering results for each particular category of elements
	 * 
	 * @param computedClusters
	 * @param expectedClusterSizes
	 * @param categoryName2function
	 */
	public static <T> Map<String, EvaluationMetrics> evaluate(List<List<T>> computedClusters,
			Map<T, Integer> expectedClusterSizes, Map<String, Function<T, Boolean>> categoryName2function) {
		
		//add global
		categoryName2function.put(GLOBAL, x -> true);
		
		//We evaluate data for each category
		Map<String, EvaluationMetricsBuilder> resultsBuilder = categoryName2function.keySet().stream().collect(Collectors.toMap(Function.identity(), ignored -> new EvaluationMetricsBuilder()));
		
		DynamicCombinationsCalculator dcc = new DynamicCombinationsCalculator();
		
		// calculate expected positives
		for (Entry<T, Integer> element2expectedClusterSize : expectedClusterSizes.entrySet()) {
			// cluster of cardinality == 1 are not considered
			if (element2expectedClusterSize.getValue() > 1) {
				int clusterSize = dcc.calculateCombinations(element2expectedClusterSize.getValue(), 2);
				addEvidences(categoryName2function, resultsBuilder, element2expectedClusterSize.getKey(), evaluation -> evaluation.addExpectedPositives(clusterSize));
			}
		}

		// calculate true and false positives
		for (List<T> cluster : computedClusters) {
			int size = cluster.size();
			// cluster of cardinality == 1 are not considered
			if (size > 1) {
				Map<T, Long> collected = cluster.stream().collect(Collectors.groupingBy(Function.identity(), 
						Collectors.counting()));
				for (Entry<T, Long> element2computedSize : collected.entrySet()) {
					int combinations = dcc.calculateCombinations(element2computedSize.getValue().intValue(), 2);
					addEvidences(categoryName2function, resultsBuilder, element2computedSize.getKey(), eval -> {
						eval.addComputedPositives(combinations); 
						eval.addTruePositives(combinations); } );
				}
				
				//Now add all combinations of different attributes in the same cluster.
				//Example: cluster AAA BB, so A and B are in same cluster even if they should not.
				//We should add 3*2 to evidences, BUT if 1 attribute belongs to 1 category and the other not, then it is divided by 2
				ArrayList<T> listValues = new ArrayList<>(collected.keySet());
				for (int i = 0; i < listValues.size()-1; i++) {
					for (int j = i+1; j < listValues.size(); j++) {
						T element1 = listValues.get(i);
						T element2 = listValues.get(j);
						long totalSize = collected.get(element1) * collected.get(element2);
						for (Entry<String,  Function<T, Boolean>> category2functionEntry : categoryName2function.entrySet()) {
							double accountForIt = 0;
							if (category2functionEntry.getValue().apply(element1)) {
								accountForIt += 0.5;
							}
							
							if (category2functionEntry.getValue().apply(element2)) {
								accountForIt += 0.5;
							}
							
							resultsBuilder.get(category2functionEntry.getKey()).addComputedPositives(accountForIt * totalSize);
						}
					}
				}
			}
		}
			
		Map<String, EvaluationMetrics> results = resultsBuilder.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
		return results;
	}

	private static <T> void addEvidences(Map<String, Function<T, Boolean>> categoryName2function,
			Map<String, EvaluationMetricsBuilder> resultsBuilder, T element,
			Consumer<EvaluationMetricsBuilder> evidencesAdder) {
		for (Entry<String, EvaluationMetricsBuilder> category2evaluationMetrics : resultsBuilder.entrySet()) {
			if (categoryName2function.get(category2evaluationMetrics.getKey()).apply(element)) {
				evidencesAdder.accept(category2evaluationMetrics.getValue());
			}
		}
	}
}
