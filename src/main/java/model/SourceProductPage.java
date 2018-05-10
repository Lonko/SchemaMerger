package model;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents a single product page found in a specific source
 * 
 * @author federico
 *
 */
public class SourceProductPage extends AbstractProductPage {
		
	private String url;
	private Source source;
	private List<String> linkage;
	private List<Integer> ids;
	
	public SourceProductPage(String category, String url, String website) {
		super();
		this.url = url;
		this.source = new Source(category, website);
		this.linkage = new LinkedList<>();
		this.ids = new LinkedList<>();
	}

	public String getUrl() {
		return url;
	}

	public Source getSource() {
		return source;
	}

	public List<String> getLinkage() {
		return linkage;
	}

	public List<Integer> getIds() {
		return ids;
	}

	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}

	public void setLinkage(List<String> listLinkages) {
		this.linkage = listLinkages;
	}
}
