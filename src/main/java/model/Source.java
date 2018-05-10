package model;

/**
 * A source of data (website + category)
 * @author federico
 *
 */
public class Source {
	
	private String category;
	private String website;
	
	public Source(String category, String website) {
		super();
		this.category = category;
		this.website = website;
	}

	public String getCategory() {
		return category;
	}

	public String getWebsite() {
		return website;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((website == null) ? 0 : website.hashCode());
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
		Source other = (Source) obj;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		if (website == null) {
			if (other.website != null)
				return false;
		} else if (!website.equals(other.website))
			return false;
		return true;
	}
	
	@Override
	public String toString(){
		return this.category + "###" + this.website;
	}
	
	
	
	

}
