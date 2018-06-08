package testutils;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import model.AbstractProductPage.Specifications;
import model.SourceProductPage;

/**
 * Utilities for testing
 * @author federico
 *
 */
public class TestUtils {
	
	public static <T,Q> Entry<T,Q>  entry(T key, Q value){
		return new AbstractMap.SimpleEntry<>(key, value);
	}
	
	public static <T,Q> Map<T,Q> map(@SuppressWarnings("unchecked") Entry<T,Q>...entries){
		HashMap<T, Q> result = new HashMap<T,Q>();
		for (Entry<T,Q> entry: entries) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	
	@SafeVarargs
	public static Specifications spec(Entry<String,String>...entries) {
		Specifications result = new Specifications();
		for (Entry<String, String> entry: entries) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;		
	}

	@SafeVarargs
	/**
	 * Generate a sourceproduct page with only specifications
	 * @param specs
	 * @return
	 */
	public static SourceProductPage spp(Entry<String, String>...specs) {
		SourceProductPage sourceProductPage = new SourceProductPage(null, null, null);
		for (Entry<String, String> entry : specs) {
			sourceProductPage.addAttributeValue(entry.getKey(), entry.getValue());
		}
		return sourceProductPage;
	}
	
	/**
	 * Generate a sourceproduct page with specifications AND website
	 * @param specs
	 * @return
	 */
	@SafeVarargs
	public static SourceProductPage spp(String website, Entry<String, String>...specs) {
		SourceProductPage sourceProductPage = new SourceProductPage(null, null, website);
		for (Entry<String, String> entry : specs) {
			sourceProductPage.addAttributeValue(entry.getKey(), entry.getValue());
		}
		return sourceProductPage;
	}
	
}
