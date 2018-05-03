package model;

/**
 * Represents a single product in a catalogue
 * 
 * @author federico
 *
 */
public class CatalogueProductPage extends AbstractProductPage {
	
	private int id;
	
	public CatalogueProductPage(int id, String category) {
		super(category);
		this.id = id;
	}

	public int getId() {
		return id;
	}	
}
