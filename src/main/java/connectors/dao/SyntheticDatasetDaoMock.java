package connectors.dao;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import model.CatalogueProductPage;
import model.SourceProductPage;

/**
 * Mock for synthetic access DAO
 */
public class SyntheticDatasetDaoMock implements SyntheticDatasetDao {

	private Map<Integer, CatalogueProductPage> id2catalogue = new HashMap<>();
	private boolean print;

	/**
	 * If print is true, every access to a method prints a sysout
	 */
	public SyntheticDatasetDaoMock(boolean print) {
		this.id2catalogue = new HashMap<>();
		this.print = print;
	}

	@Override
	public void uploadCatalogue(List<CatalogueProductPage> catalogue, boolean delete) {
		if (print) {
			System.out.println("uploadCatalogue " + catalogue + "," + delete);
		}
		for (CatalogueProductPage cpp : catalogue) {
			this.id2catalogue.put(cpp.getId(), cpp);
		}
	}

	@Override
	public void uploadSource(List<SourceProductPage> productPages) {
		if (print) {
			System.out.println("uploadSource " + productPages);
		}
	}

	@Override
	public void finalizeSourceUpload() {
		if (print) {
			System.out.println("finalizeSourceUpload");
		}
	}

	@Override
	public void deleteAllSourceProductPages() {
		if (print) {
			System.out.println("deleteAllSourceProductPages");
		}
	}

	@Override
	public List<CatalogueProductPage> getCatalogueProductsWithIds(List<Integer> ids) {
		if (print) {
			System.out.println("getCatalogueProductsWithIds " + ids);
		}
		LinkedList<CatalogueProductPage> linkedList = new LinkedList<>();
		for (Integer id : ids) {
			linkedList.add(this.id2catalogue.get(id));
		}
		return linkedList;
	}

}
