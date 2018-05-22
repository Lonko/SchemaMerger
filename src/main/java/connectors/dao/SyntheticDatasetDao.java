package connectors.dao;

import java.util.List;

import model.CatalogueProductPage;
import model.SourceProductPage;

/**
 * Interface for adding synthetic data to an external DB
 * 
 * @author federico
 *
 */
public interface SyntheticDatasetDao {

	/**
	 * Add all product pages of a catalogue to the DB
	 * 
	 * @param catalogue
	 * @param delete
	 */
	public void uploadCatalogue(List<CatalogueProductPage> catalogue, boolean delete);

	/**
	 * Add all product pages of a specific source to the DB
	 * 
	 * @param productPages
	 */
	public void uploadSource(List<SourceProductPage> productPages);

	/**
	 * This method should be called when upload of all source product pages have
	 * terminated
	 */
	public void finalizeSourceUpload();

	/**
	 * Delete all existing source product pages
	 */
	public void deleteAllSourceProductPages();

	/**
	 * Retrieve list of product with the provided ids from the catalogue
	 * 
	 * @param ids
	 * @return
	 */
	public List<CatalogueProductPage> getCatalogueProductsWithIds(List<Integer> ids);

}