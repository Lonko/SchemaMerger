package connectors.dao;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import model.AbstractProductPage.Specifications;
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
	 * Get a sample of N random pages from dataset, where N=size, provided they have
	 * at least 1 attribute in specifications and 1 page in linkage
	 * 
	 * @param size
	 * @param category
	 * @return
	 */
	public List<SourceProductPage> getSamplePagesFromCategory(int size, String category);

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

	/**
	 * Provides information about how pages outside catalog can be linked to pages
	 * in catalog.<br/>
	 * Note that catalog is built iteratively, adding one site at the type. One need
	 * to provide the current websites that compose the catalog parameter).<br/>
	 * The algorithm is the following:
	 * <ul>
	 * <li>Finds all pages in catalog, with at least 1 linkage and 1 attribute
	 * <li>For each of these pages (say p1):
	 * <ul>
	 * <li>Find all linked pages in catalog (say p2,p3) and all linked pages outside
	 * catalog (say p4,p5)
	 * <li>Build a map p4 --> p1,p2,p3; p5 --> p1,p2,p3
	 * </ul>
	 * 
	 * @param websites
	 *            websites pertaining to the catalog currently
	 * @param category
	 * @return
	 */
	public Map<SourceProductPage, List<SourceProductPage>> getProdsInRL(List<String> websites, String category);

	/**
	 * Return product pages that match given filters.If a parameter is not provided
	 * (empty string), that filter won't be applied
	 * 
	 * @param category
	 *            category of product page
	 * @param website2
	 *            the PP should be linked with at least a page of that website
	 * @param attribute1
	 *            the PP should provide that attribute
	 * @return
	 */
	public List<SourceProductPage> getPagesLinkedWithSource2filtered(String category,
			String website2, String attribute1);

	/**
	 * Find all pages of source website2 linked by pages in cList1, and having
	 * attribute2.
	 * <p>
	 * For each of them (say linked page), build an 'inverse' map entry [spec of
	 * page in cList1 --> linked page]
	 * 
	 * @param cList1
	 * @param website2
	 * @param attribute2
	 * @return
	 */
	public List<Entry<Specifications, SourceProductPage>> getPairsOfPagesInLinkage(List<SourceProductPage> cList1, String website2,
			String attribute2);

	/**
	 * Provides a product page found in the dataset with the provided URL, if it
	 * exists.
	 * <p>
	 * Typically used to find pages from a URL found in linkage
	 * ({@link SourceProductPage#getLinkage()}).
	 * <p>
	 * Note that it may happen that pages provided in linkage does not exist in the
	 * dataset, because of noise in the linkage (or pages deleted as not relevant)
	 * 
	 * @param url
	 * @return the product page with that link, null if it does not exist
	 */
	public SourceProductPage getPageFromUrlIfExistsInDataset(String url);
}
