package connectors.dao;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.Source;
import model.SourceProductPage;

/**
 * DAO for operations used for schema alignment algorithm.
 * <p>
 * Note: the idea of separating this class from {@link SyntheticDatasetDao} is
 * due to concentrating bad conception in a single class to refactor, but after
 * refactoring we should consider if joining the 2 classes OR separate them in a
 * different way.
 * 
 * @author federico
 *
 */
public interface AlignmentDao {

	/**
	 * Get sample of record linkages
	 * 
	 * @param size
	 * @param category
	 * @return
	 */
	public List<SourceProductPage> getRLSample(int size, String category);

	/**
	 * Fetches the schemas of all sources that belong to one of the categories
	 * selected. Doesn't consider schemas without attributes
	 * 
	 * @param categories
	 * @return
	 */
	public Map<Source, List<String>> getSchemas(List<String> categories);

	/**
	 * Fetches the schema of a single source
	 * 
	 * @param category
	 * @param website
	 * @return
	 */
	public List<String> getSingleSchema(Source source);

	public Map<SourceProductPage, List<SourceProductPage>> getProdsInRL(List<String> websites, String category);

	/**
	 * Return product pages that match given filters.If a parameter is not provided
	 * (empty string), that filter won't be applied
	 * 
	 * @param category
	 *            category of product page
	 * @param website1
	 *            website of product page
	 * @param website2
	 *            the PP should be linked with at least a page of that website
	 * @param attribute1
	 *            the PP should provide that attribute
	 * @return
	 */
	public List<SourceProductPage> getProds(String category, String website1, String website2, String attribute1);

	public List<Entry<SourceProductPage, SourceProductPage>> getProdsInRL(List<SourceProductPage> cList1,
			String website2, String attribute2);

	public SourceProductPage getIfValid(String url);
}
