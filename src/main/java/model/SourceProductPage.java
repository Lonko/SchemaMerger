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

	

	@Override
	public String toString() {
		final int maxLen = 10;
		return "SourceProductPage [url=" + url + ", source=" + source + ", linkage="
				+ (linkage != null ? linkage.subList(0, Math.min(linkage.size(), maxLen)) : null) + ", ids="
				+ (ids != null ? ids.subList(0, Math.min(ids.size(), maxLen)) : null) + ", specs="+getSpecifications()+"]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((ids == null) ? 0 : ids.hashCode());
		result = prime * result + ((linkage == null) ? 0 : linkage.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SourceProductPage other = (SourceProductPage) obj;
		if (ids == null) {
			if (other.ids != null)
				return false;
		} else if (!ids.equals(other.ids))
			return false;
		if (linkage == null) {
			if (other.linkage != null)
				return false;
		} else if (!linkage.equals(other.linkage))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}
	
	

}
