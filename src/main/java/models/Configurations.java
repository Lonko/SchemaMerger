package models;

import java.util.Properties;

public class Configurations {
	
	private int maxPages;
	private int minPages;
	private int sources;
	private int sizeCurveType;
	private int prodCurveType;
	private int attrCurveType;
	private int attributes;
	private final String[] cardinalityClasses = {"2","3","5","10","50","200"};
	private int[] cardinalityPercentages = new int[6];
	private final String[] tokenClasses = {"1-0","2-0","1-1","3-0","2-1","1-2","4-0","3-1","2-2","1-3"};
	private int[] tokenPercentages = new int[10];

	public Configurations(Properties prop){
		this.maxPages = Integer.valueOf(prop.getProperty("maxPages"));
		this.minPages = Integer.valueOf(prop.getProperty("minPages"));
		this.sources = Integer.valueOf(prop.getProperty("sources"));
		this.sizeCurveType = Integer.valueOf(prop.getProperty("curveSizes"));
		this.prodCurveType = Integer.valueOf(prop.getProperty("curveProds"));
		this.attrCurveType = Integer.valueOf(prop.getProperty("curveAttrs"));
		this.attributes = Integer.valueOf(prop.getProperty("attributes"));
		String[] cards = prop.getProperty("cardinality").split("-");
		for(int i = 0; i < cards.length; i++){
			this.cardinalityPercentages[i] = Integer.valueOf(cards[i]);
		}
		String[] tokens = prop.getProperty("tokens").split("-");
		for(int i = 0; i < tokens.length; i++){
			this.tokenPercentages[i] = Integer.valueOf(tokens[i]);
		}
		
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
