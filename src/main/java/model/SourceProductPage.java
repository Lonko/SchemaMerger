package model;

import java.util.List;

/**
 * Represents a single product page found in a specific source
 * 
 * @author federico
 *
 */
public class SourceProductPage extends AbstractProductPage {
	
	private String url;
	private String website;
	private List<String> linkage;
	private List<Integer> ids;
	
	public SourceProductPage(String category, String url, String website, List<String> linkage, List<Integer> ids) {
		super(category);
		this.url = url;
		this.website = website;
		this.linkage = linkage;
		this.ids = ids;
	}

	public String getUrl() {
		return url;
	}

	public String getWebsite() {
		return website;
	}

	public List<String> getLinkage() {
		return linkage;
	}

	public List<Integer> getIds() {
		return ids;
	}
	
	


}
