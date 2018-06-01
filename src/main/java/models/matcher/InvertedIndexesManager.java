package models.matcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InvertedIndexesManager {

	private Map<String, Set<Integer>> catalogIndex = new HashMap<>();
	private Map<String, Set<Integer>> linkedIndex = new HashMap<>();
	private Map<String, Set<Integer>> sourceIndex = new HashMap<>();

	public InvertedIndexesManager() {

	}

	/**
	 * Attribute in catalog --> index of pages of catalog in which this attribute is present
	 * @return
	 */
	public Map<String, Set<Integer>> getCatalogIndex() {
		return catalogIndex;
	}

	public void setCatalogIndex(Map<String, Set<Integer>> catalogIndex) {
		this.catalogIndex = catalogIndex;
	}

	/**
	 * Attribute not in catalog --> index of pages linked (not in catalog) in which this attribute is present 
	 * 
	 * @return
	 */
	public Map<String, Set<Integer>> getLinkedIndex() {
		return linkedIndex;
	}

	public void setLinkedIndex(Map<String, Set<Integer>> linkedIndex) {
		this.linkedIndex = linkedIndex;
	}

	/**
	 * Attribute in new source --> index of pages of the new source
	 * 
	 * @return
	 */
	public Map<String, Set<Integer>> getSourceIndex() {
		return sourceIndex;
	}

	public void setSourceIndex(Map<String, Set<Integer>> sourceIndex) {
		this.sourceIndex = sourceIndex;
	}

}
