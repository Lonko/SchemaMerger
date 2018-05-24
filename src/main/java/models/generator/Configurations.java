package models.generator;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import generator.CatalogueConfiguration;
import generator.SourceGeneratorConfiguration;
import models.generator.CurveFunctionFactory.CurveFunctionType;

public class Configurations implements CatalogueConfiguration, SourceGeneratorConfiguration {

	// Parameters for Connectors' constructors and Training
	private String datasetPath;
	private String recordLinkagePath;
	private String trainingSetPath;
	private String mongoURI;
	private String databaseName;
	private String modelPath;
	private List<String> categories;
	private boolean alreadyTrained;

	// Parameters for generation of Synthetic Dataset
	private int maxPages;
	private int minPages;
	private int sources;
	private int maxLinkage;
	private CurveFunctionType sizeCurveType;
	private CurveFunctionType prodCurveType;
	private CurveFunctionType attrCurveType;
	private int attributes;
	private ClassesPercentageConfiguration<Integer> cardinalityClasses;
	private ClassesPercentageConfiguration<TokenClass> tokenClasses;
	private String stringPathFile = "";
	private double randomErrorChance;
	private double differentFormatChance;
	private double differentRepresentationChance;
	private double missingLinkageChance;
	private double linkageErrorChance;

	/** @see Configurations#getRlErrorType() */
	public enum RecordLinkageErrorType {
		ID, LINKAGE
	}

	private RecordLinkageErrorType rlErrorType;

	public Configurations(Properties prop) {

		this.datasetPath = prop.getProperty("datasetPath");
		this.recordLinkagePath = prop.getProperty("recordLinkagePath");
		this.trainingSetPath = prop.getProperty("trainingSetPath");
		this.mongoURI = prop.getProperty("mongoURI");
		this.databaseName = prop.getProperty("databaseName");
		this.modelPath = prop.getProperty("modelPath");
		this.categories = Arrays.asList(prop.getProperty("categories").split("/"));
		this.alreadyTrained = Boolean.valueOf(prop.getProperty("alreadyTrained"));

		this.maxPages = Integer.valueOf(prop.getProperty("maxPages"));
		this.minPages = Integer.valueOf(prop.getProperty("minPages"));
		this.sources = Integer.valueOf(prop.getProperty("sources"));
		this.maxLinkage = Integer.valueOf(prop.getProperty("maxLinkage"));
		this.sizeCurveType = CurveFunctionType.valueOf(prop.getProperty("curveSizes"));
		this.prodCurveType = CurveFunctionType.valueOf(prop.getProperty("curveProds"));
		this.attrCurveType = CurveFunctionType.valueOf(prop.getProperty("curveAttrs"));
		this.attributes = Integer.valueOf(prop.getProperty("attributes"));
		this.randomErrorChance = Double.valueOf(prop.getProperty("randomError"));
		this.differentFormatChance = Double.valueOf(prop.getProperty("formatChance"));
		this.differentRepresentationChance = Double.valueOf(prop.getProperty("representationChance"));
		this.missingLinkageChance = Double.valueOf(prop.getProperty("missingLinkage"));
		this.linkageErrorChance = Double.valueOf(prop.getProperty("linkageError"));
		this.rlErrorType = RecordLinkageErrorType
				.valueOf(prop.getProperty("recordLinkageErrorType", RecordLinkageErrorType.ID.name()));
		this.cardinalityClasses = new CardinalitiesClassBuilder().generateClassesPercentage(prop.getProperty("cardinalityClasses"), 
				prop.getProperty("cardinality"));
		this.tokenClasses = new TokenClassPercentageBuilder().generateClassesPercentage(prop.getProperty("tokensClasses"), 
				prop.getProperty("tokens"));
		String path = prop.getProperty("stringFilePath");
		if (path != null)
			this.stringPathFile = path;

	}

	/**
	 * If true, uses the trained dump found in modelN.rda, otherwise recreate it
	 * training data.
	 * <p>
	 * If data is changed (or configuration of synthetic dataset), training data
	 * should be recomputed, so put here a FALSE
	 * 
	 * @return
	 */
	public boolean isAlreadyTrained() {
		return alreadyTrained;
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

	public CurveFunctionType getSizeCurveType() {
		return sizeCurveType;
	}

	public CurveFunctionType getProdCurveType() {
		return prodCurveType;
	}

	public CurveFunctionType getAttrCurveType() {
		return attrCurveType;
	}

	public int getAttributes() {
		return attributes;
	}

	public void setAttributes(int attributes) {
		this.attributes = attributes;
	}

	public String getStringPathFile() {
		return stringPathFile;
	}

	public void setStringPathFile(String stringPathFile) {
		this.stringPathFile = stringPathFile;
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

	public void setDifferentRepresentationChance(double differentRepresentationChance) {
		this.differentRepresentationChance = differentRepresentationChance;
	}

	public double getMissingLinkageChance() {
		return missingLinkageChance;
	}

	public void setMissingLinkageChance(double missingLinkageChance) {
		this.missingLinkageChance = missingLinkageChance;
	}

	public double getLinkageErrorChance() {
		return linkageErrorChance;
	}

	public void setLinkageErrorChance(double linkageErrorChance) {
		this.linkageErrorChance = linkageErrorChance;
	}

	public String getDatasetPath() {
		return datasetPath;
	}

	public void setDatasetPath(String datasetPath) {
		this.datasetPath = datasetPath;
	}

	public String getRecordLinkagePath() {
		return recordLinkagePath;
	}

	public void setRecordLinkagePath(String recordLinkagePath) {
		this.recordLinkagePath = recordLinkagePath;
	}

	public String getTrainingSetPath() {
		return trainingSetPath;
	}

	public void setTrainingSetPath(String trainingSetPath) {
		this.trainingSetPath = trainingSetPath;
	}

	public String getMongoURI() {
		return mongoURI;
	}

	public void setMongoURI(String mongoURI) {
		this.mongoURI = mongoURI;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getModelPath() {
		return modelPath;
	}

	public void setModelPath(String modelPath) {
		this.modelPath = modelPath;
	}

	public List<String> getCategories() {
		return categories;
	}

	public void setCategories(List<String> categories) {
		this.categories = categories;
	}

	public int getMaxLinkage() {
		return maxLinkage;
	}

	public void setMaxLinkage(int maxLinkage) {
		this.maxLinkage = maxLinkage;
	}

	/**
	 * Represents the kind of error the linkage should have: linkage (randomly pick
	 * a pair of pages in linkage and break them) or ID (randomly remove ids from
	 * pages), same principle for wrong RL
	 * 
	 * @return
	 */
	public RecordLinkageErrorType getRlErrorType() {
		return rlErrorType;
	}

	public ClassesPercentageConfiguration<Integer> getCardinalityClasses() {
		return cardinalityClasses;
	}

	public ClassesPercentageConfiguration<TokenClass> getTokenClasses() {
		return tokenClasses;
	}	

}
