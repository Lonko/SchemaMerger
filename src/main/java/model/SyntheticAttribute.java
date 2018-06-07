package model;

import java.util.Scanner;

import models.generator.TokenClass;

/**
 * An attribute in the catalogue, defined for the synthetic dataset generation.<p>
 * It contains some data as what characteristics will have this attribute (H/T, cardinality...)
 * @author federico
 *
 */
public class SyntheticAttribute {

	/**
	 * Generate attribute from a string representation (toString inverse)
	 * @param attributeText
	 * @return
	 */
	public static SyntheticAttribute parseAttribute(String attributeText) {
		String[] name2features = attributeText.split(SEPARATOR);
		SyntheticAttribute sca = new SyntheticAttribute(name2features[0]);
		
		try (Scanner scanner = new Scanner(name2features[1])){
			scanner.useDelimiter("@");
			sca.setHeadOrTail(HeadOrTail.valueOf(scanner.next()));
			sca.setCardinality(scanner.nextInt());
			sca.setErrorRate(scanner.nextDouble());
			sca.setTokenClass(TokenClass.parseTokenClass(scanner.next()));
			return sca;
		}
	}
	
	private static final String SEPARATOR = "@@@";
	public static final String NAME_FORMAT = "%s@@@%s@%d@%f@%s";
	
	private String name;
	
	//is head according to the number of sources in which it appears
	private HeadOrTail headOrTail;
	private Integer cardinality;
	private Double errorRate;
	private TokenClass tokenClass;
	
	/** TODO this is used because too many toString cause a GC error. Note that this is not very readable 
	 * (see for instance the code added to setXXX adn toString). Think if there are better solutions */
	private String cachedToString;
	
	public SyntheticAttribute(String name) {
		super();
		this.name = name;
	}

	public HeadOrTail getHeadOrTail() {
		return headOrTail;
	}

	public void setHeadOrTail(HeadOrTail headOrTail) {
		this.cachedToString = null;
		this.headOrTail = headOrTail;
	}

	public int getCardinality() {
		return cardinality;
	}

	public void setCardinality(int cardinality) {
		this.cachedToString = null;
		this.cardinality = cardinality;
	}

	public double getErrorRate() {
		return errorRate;
	}

	public void setErrorRate(double errorRate) {
		this.cachedToString = null;
		this.errorRate = errorRate;
	}

	public TokenClass getTokenClass() {
		return tokenClass;
	}

	public void setTokenClass(TokenClass tokenClass) {
		this.cachedToString = null;
		this.tokenClass = tokenClass;
	}
	
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SyntheticAttribute other = (SyntheticAttribute) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		if (this.cachedToString == null) {
			this.cachedToString = String.format(NAME_FORMAT, this.name, this.headOrTail.name() , this.cardinality, this.errorRate, this.tokenClass.toString());
		}
		return this.cachedToString;
	}
}
