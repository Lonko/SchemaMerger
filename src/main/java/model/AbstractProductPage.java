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
	public static class Specifications extends HashMap<String, String>{
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
}
