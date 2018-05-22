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

	public Map<String, Set<Integer>> getCatalogIndex() {
		return catalogIndex;
	}

	public void setCatalogIndex(Map<String, Set<Integer>> catalogIndex) {
		this.catalogIndex = catalogIndex;
	}

	public Map<String, Set<Integer>> getLinkedIndex() {
		return linkedIndex;
	}

	public void setLinkedIndex(Map<String, Set<Integer>> linkedIndex) {
		this.linkedIndex = linkedIndex;
	}

	public Map<String, Set<Integer>> getSourceIndex() {
		return sourceIndex;
	}

	public void setSourceIndex(Map<String, Set<Integer>> sourceIndex) {
		this.sourceIndex = sourceIndex;
	}

}
