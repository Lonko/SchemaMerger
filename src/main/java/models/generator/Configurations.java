package models.generator;

import java.util.Properties;

public class Configurations {
	
	private int maxPages;
	private int minPages;
	private int sources;
	private int sizeCurveType;
	private int prodCurveType;
	private int attrCurveType;
	private int attributes;
	private String[] cardinalityClasses;
	private int[] cardinalityPercentages;
	private String[] tokenClasses;
	private int[] tokenPercentages;

	public Configurations(Properties prop){
		this.maxPages = Integer.valueOf(prop.getProperty("maxPages"));
		this.minPages = Integer.valueOf(prop.getProperty("minPages"));
		this.sources = Integer.valueOf(prop.getProperty("sources"));
		this.sizeCurveType = Integer.valueOf(prop.getProperty("curveSizes"));
		this.prodCurveType = Integer.valueOf(prop.getProperty("curveProds"));
		this.attrCurveType = Integer.valueOf(prop.getProperty("curveAttrs"));
		this.attributes = Integer.valueOf(prop.getProperty("attributes"));
		loadPercentages(prop);
		String[] tokens = prop.getProperty("tokens").split("-");
		for(int i = 0; i < tokens.length; i++){
			this.tokenPercentages[i] = Integer.valueOf(tokens[i]);
		}
		
	}
	
	public void loadPercentages(Properties prop){
		String[] cardClasses = prop.getProperty("carninalityClasses").split("-");
		String[] tokensClasses = prop.getProperty("tokensClasses").split("-");
		String[] cards = prop.getProperty("cardinality").split("-");
		String[] tokens = prop.getProperty("tokens").split("-");
		
		//check if the number of classes is the same as the number of percentages
		if(cardClasses.length != cards.length || tokensClasses.length != tokens.length)
			throw new IllegalArgumentException();

		this.cardinalityClasses = new String[cards.length];
		this.cardinalityPercentages = new int[cards.length];
		this.tokenClasses = new String[tokens.length];
		this.tokenPercentages = new int[tokens.length];
		
		//load cardinality classes and percentages
		int totalCard = 0;
		for(int i = 0; i < cards.length; i++){
			this.cardinalityClasses[i] = cardClasses[i];
			this.cardinalityPercentages[i] = Integer.valueOf(cards[i]);
			totalCard += this.cardinalityPercentages[i];
		}
		
		//load tokens classes and percentages
		int totalTokens = 0;
		for(int i = 0; i < tokens.length; i++){
			this.tokenClasses[i] = tokensClasses[i];
			this.tokenPercentages[i] = Integer.valueOf(tokens[i]);
			totalTokens += this.tokenPercentages[i];
		}
		
		if(totalCard != 100 || totalTokens != 100)
			throw new IllegalArgumentException();
	}

	public int getMaxPages() {
		return maxPages;
	}

	public void setMaxPages(int maxPages) {
		this.maxPages = maxPages;
	}

	public int getMinPages() {
		return minPages;
	}

	public void setMinPages(int minPages) {
		this.minPages = minPages;
	}

	public int getSources() {
		return sources;
	}

	public void setSources(int sources) {
		this.sources = sources;
	}
	
	public int getSizeCurveType() {
		return sizeCurveType;
	}

	public void setSizeCurveType(int sizeCurveType) {
		this.sizeCurveType = sizeCurveType;
	}

	public int getProdCurveType() {
		return prodCurveType;
	}

	public void setProdCurveType(int prodCurveType) {
		this.prodCurveType = prodCurveType;
	}

	public int getAttrCurveType() {
		return attrCurveType;
	}

	public void setAttrCurveType(int attrCurveType) {
		this.attrCurveType = attrCurveType;
	}

	public int getAttributes() {
		return attributes;
	}

	public void setAttributes(int attributes) {
		this.attributes = attributes;
	}

	public int[] getCardinalityPercentages() {
		return cardinalityPercentages;
	}

	public void setCardinalityPercentages(int[] cardinalityPercentages) {
		this.cardinalityPercentages = cardinalityPercentages;
	}

	public int[] getTokenPercentages() {
		return tokenPercentages;
	}

	public void setTokenPercentages(int[] tokenPercentages) {
		this.tokenPercentages = tokenPercentages;
	}

	public String[] getCardinalityClasses() {
		return cardinalityClasses;
	}

	public String[] getTokenClasses() {
		return tokenClasses;
	}
}
