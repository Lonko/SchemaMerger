package model;

import java.util.Arrays;
import java.util.Scanner;

/**
 * A product source, defined for synthetic data generation.
 * <p>
 * Useful to encode information in source, such ash head or tail sources (using
 * {@link #toString()} ), that can be reused later when doing experiments
 * 
 * @author federico
 *
 */
public class SyntheticSource {
	
	public static final String SEPARATOR = "--s--";
	public static final String FORMAT = "%s.%s";
	
	private String domain;
	private String extension;
	private HeadOrTail headOrTail;
	private Double valueErrorRate;
	private Double linkageErrorRate;
	private Double linkageMissingRate;
	
	public SyntheticSource(String domain, String extension) {
		super();
		this.domain = domain;
		this.extension = extension;
	}
	
	public SyntheticSource(String domain, String extension, HeadOrTail headOrTail, Double valueErrorRate,
			Double linkageErrorRate, Double linkageMissingRate) {
		super();
		this.domain = domain;
		this.extension = extension;
		this.headOrTail = headOrTail;
		this.valueErrorRate = valueErrorRate;
		this.linkageErrorRate = linkageErrorRate;
		this.linkageMissingRate = linkageMissingRate;
	}

	public HeadOrTail getHeadOrTail() {
		return headOrTail;
	}

	public void setHeadOrTail(HeadOrTail headOrTail) {
		this.headOrTail = headOrTail;
	}
	
	public String getDomain() {
		return domain;
	}

	public Double getValueErrorRate() {
		return valueErrorRate;
	}

	public void setValueErrorRate(Double valueErrorRate) {
		this.valueErrorRate = valueErrorRate;
	}

	public Double getLinkageErrorRate() {
		return linkageErrorRate;
	}

	public void setLinkageErrorRate(Double linkageErrorRate) {
		this.linkageErrorRate = linkageErrorRate;
	}

	public Double getLinkageMissingRate() {
		return linkageMissingRate;
	}

	public void setLinkageMissingRate(Double linkageMissingRate) {
		this.linkageMissingRate = linkageMissingRate;
	}

	public String getExtension() {
		return extension;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((domain == null) ? 0 : domain.hashCode());
		result = prime * result + ((extension == null) ? 0 : extension.hashCode());
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
		SyntheticSource other = (SyntheticSource) obj;
		if (domain == null) {
			if (other.domain != null)
				return false;
		} else if (!domain.equals(other.domain))
			return false;
		if (extension == null) {
			if (other.extension != null)
				return false;
		} else if (!extension.equals(other.extension))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		String allData = String.join(SEPARATOR, Arrays.asList(domain, headOrTail.name(), print2decimal(this.valueErrorRate), 
				print2decimal(this.linkageErrorRate), print2decimal(this.linkageMissingRate)));
		return String.format(FORMAT, allData, extension);		
	}
	
	private String print2decimal(double doubleVersion) {
		return String.valueOf(Math.round(doubleVersion * 1000));
	}

	/**
	 * Parse source from encoded text
	 * @param sourceText
	 * @return
	 */
	public static SyntheticSource parseSource(String sourceText) {
		int extensionDot = sourceText.lastIndexOf('.');
		String extension = sourceText.substring(extensionDot+1);
		String domainWithFeatures = sourceText.substring(0, extensionDot);
		try (Scanner scanner = new Scanner(domainWithFeatures)){
			scanner.useDelimiter(SEPARATOR);
			String domain = scanner.next();
			HeadOrTail ht = HeadOrTail.valueOf(scanner.next());
			Double valueErrorRate = scanner.nextInt() / (double) 1000;
			Double linkageErrorRate = scanner.nextInt() / (double) 1000;
			Double linkageMissingRate = scanner.nextInt() / (double) 1000;
			return new SyntheticSource(domain, extension, ht, valueErrorRate, linkageErrorRate, linkageMissingRate);
		}
	}
	
	
	
	

}
