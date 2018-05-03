package connectors.dao;

import java.util.List;
import java.util.Map;

import org.bson.Document;

/**
 * DAO for operations used for schema alignment algorithm.
 * 
 * TODO: this class has not a clear separation between model and persistence (EG
 * there are some Mongo {@link Document} objects in parameters/return type),
 * this should be corrected.
 * <p>
 * Also the idea of separating this class from {@link SyntheticDatasetDao} is
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
	public List<Document> getRLSample(int size, String category);

	/**
	 * Fetches the schemas of all sources that belong to one of the categories
	 * selected. Doesn't consider schemas without attributes
	 * 
	 * @param categories
	 * @return
	 */
	public Map<String, List<String>> getSchemas(List<String> categories);

	/**
	 * Fetches the schema of a single source
	 * 
	 * @param category
	 * @param website
	 * @return
	 */
	public List<String> getSingleSchema(String category, String website);

	public Map<Document, List<Document>> getProdsInRL(List<String> websites, String category);

	public List<Document> getProds(String category, String website1, String website2, String attribute1);

	public List<Document[]> getProdsInRL(List<Document> cList1, String website2, String attribute2);

	public Document getIfValid(String url);
}
