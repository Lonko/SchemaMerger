package model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single product page.
 * 
 * It can be found in catalogue or in a specific website 
 * 
 * @author federico
 *
 */
public abstract class AbstractProductPage {
	
	private String category;
	private Map<String, String> specifications;
	
	public AbstractProductPage(String category) {
		this.category = category;
		this.specifications = new HashMap<>();
	}

	public String getCategory() {
		return category;
	}

	public Map<String, String> getSpecifications() {
		return specifications;
	}
	
	public void addAttributeValue(String name, String value) {
		this.specifications.put(name, value);
	}	
}
