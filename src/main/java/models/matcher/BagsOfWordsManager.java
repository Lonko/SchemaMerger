package models.matcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import model.AbstractProductPage.Specifications;
import model.SourceProductPage;

public class BagsOfWordsManager {
	private String attributeCatalog;
	private String attributeSource;
	private List<String> catalogBagOfWords;
	private List<String> sourceBagOfWords;

	/**
	 * 
	 * @param attributeCatalog the first attribute of the pair, usually considered the catalog attribute
	 * @param attributeSource the first attribute of the pair, usually considered the new source attribute
	 * @param pairsOfPage
	 */
	public BagsOfWordsManager(String attributeCatalog, String attributeSource,
			List<Entry<Specifications, SourceProductPage>> pairsOfPage) {
		this.catalogBagOfWords = new ArrayList<>();
		this.sourceBagOfWords = new ArrayList<>();

		for (Entry<Specifications, SourceProductPage> couple : pairsOfPage) {
			// The value attribute in the classification step, in wordsCatalog may have different values merged using ### as separator.
			// It should not happen in wordsSource, (TODO verify)
			String[] wordsCatalog = couple.getKey().get(attributeCatalog).split("( |(###))+", -1);
			String[] wordsSource = couple.getValue().getSpecifications().get(attributeSource).split("( |(###))+", -1);
			catalogBagOfWords.addAll(Arrays.asList(wordsCatalog));
			sourceBagOfWords.addAll(Arrays.asList(wordsSource));
		}
	}

	public String getAttributeCatalog() {
		return attributeCatalog;
	}

	public void setAttributeCatalog(String attributeCatalog) {
		this.attributeCatalog = attributeCatalog;
	}

	public String getAttributeSource() {
		return attributeSource;
	}

	public void setAttributeSource(String attributeSource) {
		this.attributeSource = attributeSource;
	}

	public List<String> getCatalogBagOfWords() {
		return catalogBagOfWords;
	}

	public void setCatalogBagOfWords(List<String> bagOfWordsCatalog) {
		this.catalogBagOfWords = bagOfWordsCatalog;
	}

	public List<String> getSourceBagOfWords() {
		return sourceBagOfWords;
	}

	public void setSourceBagOfWords(List<String> bagOfWordsSource) {
		this.sourceBagOfWords = bagOfWordsSource;
	}

}
