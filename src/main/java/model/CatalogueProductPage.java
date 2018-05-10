package model;

/**
 * Represents a single product in a catalogue
 * 
 * @author federico
 *
 */
public class CatalogueProductPage extends AbstractProductPage {
	
	private int id;
	private String category;
	
	public CatalogueProductPage(int id, String category) {
		super();
		this.id = id;
	}

	public int getId() {
		return id;
	}

	public String getCategory() {
		return category;
	}	
}
