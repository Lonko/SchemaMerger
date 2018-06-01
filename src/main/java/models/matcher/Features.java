package models.matcher;

import matcher.TrainingSetGenerator;

/**
 * For provided pair of attributes (not present here), features + match
 * probability (in case of traning set, always 0 or 1)
 * 
 * @author marco
 * @see TrainingSetGenerator
 *
 */
public class Features {

	private double sourceJSD = 0.0;
	private double categoryJSD = 0.0;
	private double sourceJC = 0.0;
	private double categoryJC = 0.0;
	private double sourceMI = 0.0;
	private double categoryMI = 0.0;
	private double match = 0.0;

	public Features() {

	}

	public double getSourceJSD() {
		return sourceJSD;
	}

	public void setSourceJSD(double sourceJSD) {
		this.sourceJSD = sourceJSD;
	}

	public double getCategoryJSD() {
		return categoryJSD;
	}

	public void setCategoryJSD(double catalogJSD) {
		this.categoryJSD = catalogJSD;
	}

	public double getSourceJC() {
		return sourceJC;
	}

	public void setSourceJC(double sourceJC) {
		this.sourceJC = sourceJC;
	}

	public double getCategoryJC() {
		return categoryJC;
	}

	public void setCategoryJC(double catalogJC) {
		this.categoryJC = catalogJC;
	}

	public double getSourceMI() {
		return sourceMI;
	}

	public void setSourceMI(double sourceMI) {
		this.sourceMI = sourceMI;
	}

	public double getCategoryMI() {
		return categoryMI;
	}

	public void setCategoryMI(double catalogMI) {
		this.categoryMI = catalogMI;
	}

	public double getMatch() {
		return match;
	}

	public void setMatch(double match) {
		this.match = match;
	}

	public boolean hasNan() {
		return Double.isNaN(this.sourceJSD) || Double.isNaN(this.categoryJSD) || Double.isNaN(this.sourceJC)
				|| Double.isNaN(this.categoryJC) || Double.isNaN(this.sourceMI) || Double.isNaN(this.categoryMI)
				|| Double.isNaN(this.match);
	}

	public String toString() {
		return this.sourceJSD + "," + this.categoryJSD + "," + this.sourceJC + "," + this.categoryJC + ","
				+ this.sourceMI + "," + this.categoryMI;
	}

}
