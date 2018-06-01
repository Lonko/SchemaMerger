package models.matcher;

public class Tuple {
	private String attribute1;
	private String attribute2;
	private String website1;
	private String website2;
	private String category;

	public Tuple(String attribute1, String attribute2, String website1, String website2, String category) {
		super();
		this.attribute1 = attribute1;
		this.attribute2 = attribute2;
		this.website1 = website1;
		this.website2 = website2;
		this.category = category;
	}

	/**
	 * TODO riformulare
	 * Produce a negative example, i.e. 2 different attributes from 2 pages, from this object that should be a positive example between the 2 pages
	 * @param t
	 * @return
	 */
	public Tuple getMixedTuple(Tuple t) {
		String a1 = this.getAttribute1();
		String a2 = t.getAttribute2();
		String w1 = this.getWebsite1();
		String w2 = this.getWebsite2();
		String c = this.getCategory();
		return new Tuple(a1, a2, w1, w2, c);
	}

	public String getLinkageIdentity() {
		return this.website1 + "#" + this.website2 + "#" + this.category;
	}

	public String getAttribute1() {
		return attribute1;
	}

	public String getAttribute2() {
		return attribute2;
	}

	public String getWebsite1() {
		return website1;
	}

	public String getWebsite2() {
		return website2;
	}

	public String getCategory() {
		return category;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute1 == null) ? 0 : attribute1.hashCode());
		result = prime * result + ((attribute2 == null) ? 0 : attribute2.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((website1 == null) ? 0 : website1.hashCode());
		result = prime * result + ((website2 == null) ? 0 : website2.hashCode());
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
		Tuple other = (Tuple) obj;
		if (attribute1 == null) {
			if (other.attribute1 != null)
				return false;
		} else if (!attribute1.equals(other.attribute1))
			return false;
		if (attribute2 == null) {
			if (other.attribute2 != null)
				return false;
		} else if (!attribute2.equals(other.attribute2))
			return false;
		if (category == null) {
			if (other.category != null)
				return false;
		} else if (!category.equals(other.category))
			return false;
		if (website1 == null) {
			if (other.website1 != null)
				return false;
		} else if (!website1.equals(other.website1))
			return false;
		if (website2 == null) {
			if (other.website2 != null)
				return false;
		} else if (!website2.equals(other.website2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Tuple {\n\tattribute1 => " + attribute1 + ",\n\tattribute2 => " + attribute2 + ",\n\tschema1 => "
				+ website1 + ",\n\tschema2 => " + website2 + ",\n\tcategory => " + category + "\n}";
	}

	public String toRowString() {
		return this.attribute1.replace(",", "#;#") + "," + this.attribute2.replace(",", "#;#") + ","
				+ this.website1.replace(",", "#;#") + "," + this.website2.replace(",", "#;#") + ","
				+ this.category.replace(",", "#;#") + ",";
	}
}
