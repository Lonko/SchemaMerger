package models;

public class Tuple {
	private String attribute1;
	private String attribute2;
	private String schema1;
	private String schema2;
	private String category;
	
	public Tuple(String a1, String a2, String s1, String s2, String category){
		this.attribute1 = a1;
		this.attribute2 = a2;
		this.schema1 = s1;
		this.schema2 = s2;
		this.category = category;
	}
	
	public Tuple getMixedTuple(Tuple t){
		String a1 = this.getAttribute1();
		String a2 = t.getAttribute2();
		String s1 = this.getSchema1();
		String s2 = this.getSchema2();
		String c = this.getCategory();
		return new Tuple(a1, a2, s1, s2, c);
	}
	
	public String getLinkageIdentity(){
		return this.schema1+"#"+this.schema2+"#"+this.category;
	}

	public String getAttribute1() {
		return attribute1;
	}

	public void setAttribute1(String attribute1) {
		this.attribute1 = attribute1;
	}

	public String getAttribute2() {
		return attribute2;
	}

	public void setAttribute2(String attribute2) {
		this.attribute2 = attribute2;
	}

	public String getSchema1() {
		return schema1;
	}

	public void setSchema1(String schema1) {
		this.schema1 = schema1;
	}

	public String getSchema2() {
		return schema2;
	}

	public void setSchema2(String schema2) {
		this.schema2 = schema2;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((attribute1 == null) ? 0 : attribute1.hashCode());
		result = prime * result
				+ ((attribute2 == null) ? 0 : attribute2.hashCode());
		result = prime * result
				+ ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((schema1 == null) ? 0 : schema1.hashCode());
		result = prime * result + ((schema2 == null) ? 0 : schema2.hashCode());
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
		if (schema1 == null) {
			if (other.schema1 != null)
				return false;
		} else if (!schema1.equals(other.schema1))
			return false;
		if (schema2 == null) {
			if (other.schema2 != null)
				return false;
		} else if (!schema2.equals(other.schema2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Tuple {\n\tattribute1 => " + attribute1 + ",\n\tattribute2 => " + attribute2
				+ ",\n\tschema1 => " + schema1 + ",\n\tschema2 => " + schema2
				+ ",\n\tcategory => " + category + "\n}";
	}
	
	public String toRowString(){
		return this.attribute1.replace(",", "#;#") + "," + this.attribute2.replace(",", "#;#")
				+ "," + this.schema1.replace(",", "#;#") + "," + this.schema2.replace(",", "#;#")
				+ "," + this.category.replace(",", "#;#") + ",";
	}
}
