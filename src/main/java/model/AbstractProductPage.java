package model;

import java.util.HashMap;

/**
 * Represents a single product page.
 * 
 * It can be found in catalogue or in a specific website
 * 
 * @author federico
 *
 */
public abstract class AbstractProductPage {

	@SuppressWarnings("serial")
	public static class Specifications extends HashMap<String, String> {
		public Specifications() {
			super();
		}
	}

	private Specifications specifications;

	public AbstractProductPage() {
		this.specifications = new Specifications();
	}

	public Specifications getSpecifications() {
		return specifications;
	}

	public void addAttributeValue(String name, String value) {
		this.specifications.put(name, value);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((specifications == null) ? 0 : specifications.hashCode());
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
		AbstractProductPage other = (AbstractProductPage) obj;
		if (specifications == null) {
			if (other.specifications != null)
				return false;
		} else if (!specifications.equals(other.specifications))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AbstractProductPage [specifications=" + specifications + "]";
	}
	
}
