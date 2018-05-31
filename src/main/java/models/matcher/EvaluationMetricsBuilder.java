package models.matcher;

/**
 * Build an evaluation metrics providing TP, FP, FN 
 * @author federico
 *
 */
public class EvaluationMetricsBuilder {
	
	private double truePositives;
	private double expectedPositives;
	private double computedPositives;
	
	public EvaluationMetricsBuilder() {
		super();
	}
	
	/**
	 * If values are already known
	 * @param truePositives
	 * @param falsePositives
	 * @param falseNegatives
	 */
	public EvaluationMetricsBuilder(double truePositives, double expectedPositives, double computedPositives) {
		super();
		this.truePositives = truePositives;
		this.expectedPositives = expectedPositives;
		this.computedPositives = computedPositives;
	}

	public void addTruePositives(double addedTruePositives) {
		this.truePositives += addedTruePositives;
	}
	
	public void addExpectedPositives(double addedExpectedPositives) {
		this.expectedPositives += addedExpectedPositives;
	}
	
	public void addComputedPositives(double addedComputedPositives) {
		this.computedPositives += addedComputedPositives;
	}
	
	public EvaluationMetrics build() {
		return new EvaluationMetrics(truePositives / computedPositives, truePositives / expectedPositives);
	}

}
