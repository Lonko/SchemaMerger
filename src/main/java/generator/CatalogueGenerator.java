package generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bson.Document;

import models.generator.Configurations;
import models.generator.ConstantCurveFunction;
import models.generator.CurveFunction;
import models.generator.RationalCurveFunction;

public class CatalogueGenerator {
	
	private StringGenerator stringGenerator;
	private int maxSizeSources;
	private int minSizeSources;
	private int nSources;
	private int maxLinkage;
	private int nAttributes;
	private int nProducts;
	private CurveFunction sizeCurve;
	private CurveFunction productLinkageCurve;
	private String[] cardClasses;
	private double[] cardPercentages;
	private String[] tokenClasses;
	private double[] tokenPercentages;
	private Map<Integer, List<String>> fixedTokenPool = new HashMap<>();
	private Map<String, List<String>> attrValues = new HashMap<>();
	private Map<String, String> attrFixedToken = new HashMap<>();
	private Map<String, String> cardinalities = new HashMap<>();
	private Map<String, String> tokens = new HashMap<>();

	public CatalogueGenerator(Configurations conf, StringGenerator sg){
		this.maxSizeSources = conf.getMaxPages();
		this.minSizeSources = conf.getMinPages();
		this.nSources = conf.getSources();
		this.maxLinkage = conf.getMaxLinkage();
		this.nAttributes = conf.getAttributes();
		
		String typeSize = conf.getSizeCurveType();
		String typePLinkage = conf.getProdCurveType();
		createCurves(typeSize, typePLinkage);
		
		this.cardClasses = conf.getCardinalityClasses();
		this.cardPercentages = conf.getCardinalityPercentages();
		this.tokenClasses = conf.getTokenClasses();
		this.tokenPercentages = conf.getTokenPercentages();
		
		this.stringGenerator = sg;
	}
	
	private void createCurves(String typeSize, String typePLinkage){
		
		if(typeSize.equals("0"))
			this.sizeCurve = new ConstantCurveFunction(this.maxSizeSources,
														this.nSources, this.minSizeSources);
		else
			this.sizeCurve = new RationalCurveFunction(typeSize, this.maxSizeSources,
														this.nSources, this.minSizeSources);
		
		if(typePLinkage.equals("0"))
			this.productLinkageCurve = new ConstantCurveFunction(nSources, sizeCurve.getSampling());
		else
			this.productLinkageCurve = new RationalCurveFunction(typePLinkage, this.maxLinkage,
																sizeCurve.getSampling());
		
		this.nProducts = this.productLinkageCurve.getYValues().length;
	}
	
	//assigns to each attribute a cardinality/token class
	private void assignClasses(List<String> attrs, String classType){
		
		List<Integer> indexes = IntStream.range(0, attrs.size())
								 .boxed()
								 .collect(Collectors.toList());
		Collections.shuffle(indexes);
		
		String[] classes;
		double[] percentages;
		Map<String, String> attrsClasses;
		if(classType.equals("cardinality")){
			classes = this.cardClasses;
			percentages = this.cardPercentages;
			attrsClasses = this.cardinalities;
		} else {
			classes = this.tokenClasses;
			percentages = this.tokenPercentages;
			attrsClasses = this.tokens;
		}
		
		int[] partitions = new int[percentages.length];
		int acc = 0;
		for(int i = 0; i < percentages.length-1; i++){
			int partition = (int) (percentages[i] * attrs.size() / 100);
			partitions[i] = partition;
			acc += partition;
		}
		partitions[percentages.length-1] = attrs.size() - acc;
		
		int currentIndex = 0;
		for(int i = 0; i < partitions.length; i++){
			String currentClass = classes[i];
			for(int j = currentIndex; j < (partitions[i]+currentIndex); j++){
				String attribute = attrs.get(j);
				attrsClasses.put(attribute, currentClass);
			}
			currentIndex += partitions[i];
		
		}
	}

	//generates the pool of token to use for the fixed part of the values
	private void generateTokenPools(){
		//generate a different pool for each cardinality Class
		for(String cardClass : this.cardClasses){
			int nToken = Integer.valueOf(cardClass);
			Set<String> tokenPool = new HashSet<>();
			//nToken*15 to generate a pool bigger than necessary
			for(int i = 0; i < nToken*nToken; i++){
				tokenPool.add(this.stringGenerator.generateAttributeToken());
			}
			this.fixedTokenPool.put(Integer.valueOf(cardClass), new ArrayList<String>(tokenPool));
		}
	}
	
	//generates the attributes' names and their possible values
	private void prepareAttributes(){
		//generate attributes ids
		Set<String> attrNamesSet = new HashSet<>();
		List<String> attrNames = new ArrayList<>();
		
		while(attrNamesSet.size() < this.nAttributes){
			attrNamesSet.add(this.stringGenerator.generateAttributeName());
		}
		attrNames.addAll(attrNamesSet);

		//attribute -> cardinality
		assignClasses(attrNames, "cardinality");
		//attribute -> token type
		assignClasses(attrNames, "tokens");

		generateTokenPools();
		for(String attribute : attrNames){
			int cardinality = Integer.valueOf(this.cardinalities.get(attribute));
			List<String> tokenPool = this.fixedTokenPool.get(cardinality);
			Collections.shuffle(tokenPool);
			String[] attrTokens = this.tokens.get(attribute).split("-");
			String fixedTokens = "";

			for(int i = 0; i < Integer.valueOf(attrTokens[1]); i++){
				fixedTokens += tokenPool.get(i)+" ";
			}
			this.attrFixedToken.put(attribute, fixedTokens.trim());

			List<String> generatedValues = new ArrayList<String>();
			Set<String> valueSet = new HashSet<>();
			while(valueSet.size() < cardinality){
				String value = "";
				for(int j = 0; j < Integer.valueOf(attrTokens[0]); j++)
					value += this.stringGenerator.generateAttributeToken()+" ";
				value += fixedTokens;
				valueSet.add(value.trim());
			}

			generatedValues.addAll(valueSet);			
			this.attrValues.put(attribute, generatedValues);			
		}
	}
	
	//generates a single product
	private Document generateProduct(int id){
		Document prod = new Document();
		Document specs = new Document();
		prod.append("id", id);
		prod.append("category", "fakeCategory");
		
		for(Map.Entry<String, List<String>> attribute : this.attrValues.entrySet()){
			String name = attribute.getKey();
			int index = new Random().nextInt(attribute.getValue().size());
			String value = attribute.getValue().get(index);
			specs.append(name, value);
		}
		prod.append("spec", specs);		
		
		return prod;
	}

	//generates all products and adds them to the catalogue
	private List<Document> generateProducts(){ 	
		List<Document> products = new ArrayList<Document>();
		List<String> attributes = new ArrayList<>();
		attributes.addAll(this.attrValues.keySet());
		
		//each iteration is a batch of products to upload
		for(int i = 0; i < this.nProducts; i++){
			Document prod = generateProduct(i);
			products.add(prod);
		}
		
		return products;
	}
	
	public List<Document> createCatalogue(){
		prepareAttributes();
		return generateProducts();
	}

	public CurveFunction getSizeCurve() {
		return sizeCurve;
	}

	public CurveFunction getProductLinkageCurve() {
		return productLinkageCurve;
	}

	public Map<String, String> getAttrFixedToken() {
		return attrFixedToken;
	}

	public Map<String, List<String>> getAttrValues() {
		return attrValues;
	}
}
