package models.matcher;

/**
 * Metrics for evaluation: precision/recall and F2-measure (computed)
 * @author federico
 *
 */
public class EvaluationMetrics {
	
	private double precision;
	private double recall;
	private double f_measure;
	
	public EvaluationMetrics(double precision, double recall) {
		super();
		this.precision = precision;
		this.recall = recall;
		this.f_measure = 2 * (precision * recall) / (precision + recall);
	}

	public double getPrecision() {
		return precision;
	}

	public double getRecall() {
		return recall;
	}

	public double getF_measure() {
		return f_measure;
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
        str.append("\nPRECISION = " + this.precision);
        str.append("\nRECALL = " + this.recall);
        str.append("\nF-MEASURE = " + this.f_measure);
        return str.toString();
	}
}
