package models.matcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains all pair of attributes matched.
 * 
 * @author marco
 *
 */
// TODO remove totalLinkage and matchlinkage that are deprecated
public class Schema {
	private Map<String, String> attributesMap;
	private Map<String, Integer> totalLinkage;
	private Map<String, Integer> matchLinkage;
	private String sourceCatalogueName;

	public Schema(String sourceCatalogueName) {
		this.attributesMap = new HashMap<>();
		this.totalLinkage = new HashMap<>();
		this.matchLinkage = new HashMap<>();
		this.sourceCatalogueName = sourceCatalogueName;
	}

	public List<List<String>> schema2Clusters() {
		Map<String, List<String>> schemaMap = new HashMap<>();
		List<List<String>> finalSchema = new ArrayList<>();

		this.attributesMap.entrySet().forEach(entry -> {
			List<String> attributes = schemaMap.getOrDefault(entry.getValue(), new ArrayList<String>());
			attributes.add(entry.getKey());
			schemaMap.put(entry.getValue(), attributes);
		});

		finalSchema.addAll(schemaMap.values());
		finalSchema.sort(Comparator.comparing(List::size));
		Collections.reverse(finalSchema);

		return finalSchema;
	}

	public Map<String, String> getAttributesMap() {
		return attributesMap;
	}

	public void setAttributesMap(Map<String, String> attributesMap) {
		this.attributesMap = attributesMap;
	}

	public Map<String, Integer> getTotalLinkage() {
		return totalLinkage;
	}

	public void setTotalLinkage(Map<String, Integer> totalLinkage) {
		this.totalLinkage = totalLinkage;
	}

	public Map<String, Integer> getMatchLinkage() {
		return matchLinkage;
	}

	public void setMatchLinkage(Map<String, Integer> matchLinkage) {
		this.matchLinkage = matchLinkage;
	}

	public String getSourceCatalogueName() {
		return sourceCatalogueName;
	}
	
	
}
