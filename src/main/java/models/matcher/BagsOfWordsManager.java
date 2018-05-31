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

	public BagsOfWordsManager(String aCatalog, String aSource,
			List<Entry<Specifications, SourceProductPage>> prods) {
		this.catalogBagOfWords = new ArrayList<>();
		this.sourceBagOfWords = new ArrayList<>();

		for (Entry<Specifications, SourceProductPage> couple : prods) {
			String[] wordsCatalog = couple.getKey().get(aCatalog).split("( |(###))+");
			String[] wordsSource = couple.getValue().getSpecifications().get(aSource).split("( |(###))+");
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
