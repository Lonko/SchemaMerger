package models.generator;

import java.util.Properties;

public class Configurations {
	
	private int maxPages;
	private int minPages;
	private int sources;
	private String sizeCurveType;
	private String prodCurveType;
	private String attrCurveType;
	private int attributes;
	private String[] cardinalityClasses;
	private double[] cardinalityPercentages;
	private String[] tokenClasses;
	private double[] tokenPercentages;
	private String stringPathFile = "";
	private double randomErrorChance;
	private double differentFormatChance;
	private double differentRepresentationChance;
	private double linkageErrorChance;

	public Configurations(Properties prop){
		this.maxPages = Integer.valueOf(prop.getProperty("maxPages"));
		this.minPages = Integer.valueOf(prop.getProperty("minPages"));
		this.sources = Integer.valueOf(prop.getProperty("sources"));
		this.sizeCurveType = prop.getProperty("curveSizes");
		this.prodCurveType = prop.getProperty("curveProds");
		this.attrCurveType = prop.getProperty("curveAttrs");
		this.attributes = Integer.valueOf(prop.getProperty("attributes"));
		this.randomErrorChance = Double.valueOf(prop.getProperty("randomError"));
		this.differentFormatChance = Double.valueOf(prop.getProperty("formatChance"));
		this.differentRepresentationChance = Double.valueOf(prop.getProperty("representationChance"));
		this.linkageErrorChance = Double.valueOf(prop.getProperty("linkageError"));
		loadPercentages(prop);
		String path = prop.getProperty("stringFilePath");
		if(path != null)
			this.stringPathFile = path;
		
	}
	
	public void loadPercentages(Properties prop){
		String[] cardClasses = prop.getProperty("carninalityClasses").split("/");
		String[] tokensClasses = prop.getProperty("tokensClasses").split("/");
		String[] cards = prop.getProperty("cardinality").split("/");
		String[] tokens = prop.getProperty("tokens").split("/");
		
		//check if the number of classes is the same as the number of percentages
		if(cardClasses.length != cards.length || tokensClasses.length != tokens.length){
			throw new IllegalArgumentException("Card Classes: "+cardClasses.length
												+"\tPercentages :"+cards.length+"\n"
												+"Token Classes: "+tokensClasses.length
												+"\tPercentages : "+tokens.length);
		}

		this.cardinalityClasses = new String[cards.length];
		this.cardinalityPercentages = new double[cards.length];
		this.tokenClasses = new String[tokens.length];
		this.tokenPercentages = new double[tokens.length];
		
		//load cardinality classes and percentages
		double totalCard = 0;
		for(int i = 0; i < cards.length; i++){
			this.cardinalityClasses[i] = cardClasses[i];
			this.cardinalityPercentages[i] = Double.valueOf(cards[i]);
			totalCard += this.cardinalityPercentages[i];
		}
		
		//load tokens classes and percentages
		double totalTokens = 0;
		for(int i = 0; i < tokens.length; i++){
			this.tokenClasses[i] = tokensClasses[i];
			this.tokenPercentages[i] = Double.valueOf(tokens[i]);
			totalTokens += this.tokenPercentages[i];
		}
		
		if(totalCard != 100 || totalTokens != 100)
			throw new IllegalArgumentException("Cardinality Total: "+totalCard+"\n"
												+"Token Total: "+totalTokens);
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
	
	public String getSizeCurveType() {
		return sizeCurveType;
	}

	public void setSizeCurveType(String sizeCurveType) {
		this.sizeCurveType = sizeCurveType;
	}

	public String getProdCurveType() {
		return prodCurveType;
	}

	public void setProdCurveType(String prodCurveType) {
		this.prodCurveType = prodCurveType;
	}

	public String getAttrCurveType() {
		return attrCurveType;
	}

	public void setAttrCurveType(String attrCurveType) {
		this.attrCurveType = attrCurveType;
	}

	public int getAttributes() {
		return attributes;
	}

	public void setAttributes(int attributes) {
		this.attributes = attributes;
	}

	public double[] getCardinalityPercentages() {
		return cardinalityPercentages;
	}

	public void setCardinalityPercentages(double[] cardinalityPercentages) {
		this.cardinalityPercentages = cardinalityPercentages;
	}

	public double[] getTokenPercentages() {
		return tokenPercentages;
	}

	public void setTokenPercentages(double[] tokenPercentages) {
		this.tokenPercentages = tokenPercentages;
	}

	public String[] getCardinalityClasses() {
		return cardinalityClasses;
	}

	public String[] getTokenClasses() {
		return tokenClasses;
	}

	public String getStringPathFile() {
		return stringPathFile;
	}

	public void setStringPathFile(String stringPathFile) {
		this.stringPathFile = stringPathFile;
	}

	public void setCardinalityClasses(String[] cardinalityClasses) {
		this.cardinalityClasses = cardinalityClasses;
	}

	public void setTokenClasses(String[] tokenClasses) {
		this.tokenClasses = tokenClasses;
	}

	public double getRandomErrorChance() {
		return randomErrorChance;
	}

	public void setRandomErrorChance(double randomErrorChance) {
		this.randomErrorChance = randomErrorChance;
	}

	public double getDifferentFormatChance() {
		return differentFormatChance;
	}

	public void setDifferentFormatChance(double differentFormatChance) {
		this.differentFormatChance = differentFormatChance;
	}

	public double getDifferentRepresentationChance() {
		return differentRepresentationChance;
	}

	public void setDifferentRepresentationChance(
			double differentRepresentationChance) {
		this.differentRepresentationChance = differentRepresentationChance;
	}

	public double getLinkageErrorChance() {
		return linkageErrorChance;
	}

	public void setLinkageErrorChance(double linkageErrorChance) {
		this.linkageErrorChance = linkageErrorChance;
	}
}
