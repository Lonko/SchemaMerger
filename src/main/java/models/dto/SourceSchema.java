package models.dto;

import java.util.List;
import java.util.Set;

/**
 * Represents a specific source
 * @author federico
 *
 */
public class SourceSchema {
	private String category;
	private String website;
	private Set<String> attributes;
	private List<Integer> ids;
	
	public SourceSchema(String category, String website, Set<String> attributes, List<Integer> ids) {
		super();
		this.category = category;
		this.website = website;
		this.attributes = attributes;
		this.ids = ids;
	}
	public String getCategory() {
		return category;
	}
	public String getWebsite() {
		return website;
	}
	public Set<String> getAttributes() {
		return attributes;
	}
	public List<Integer> getIds() {
		return ids;
	}
	
	
	

}
