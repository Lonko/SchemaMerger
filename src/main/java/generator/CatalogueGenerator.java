package generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import model.CatalogueProductPage;
import models.generator.CurveFunction;
import models.generator.CurveFunctionFactory;
import models.generator.TokenClass;

public class CatalogueGenerator {

	private static final String CATEGORY_NAME_FOR_SYNTHETIC_DATASET = "fakeCategory";
	private StringGenerator stringGenerator;
	private CurveFunction sizeCurve;
	private CurveFunction productLinkageCurve;
	private Map<Integer, List<String>> fixedTokenPool = new HashMap<>();
	private Map<String, List<String>> attrValues = new HashMap<>();
	private Map<String, String> attrFixedToken = new HashMap<>();
	private Map<String, Integer> cardinalities = new HashMap<>();
	private Map<String, TokenClass> tokens = new HashMap<>();
	private CatalogueConfiguration conf;
	private int nProducts;

	public CatalogueGenerator(CatalogueConfiguration conf, StringGenerator sg) {
		this.conf = conf;
		this.sizeCurve = CurveFunctionFactory.buildCurveFunction(conf.getSizeCurveType(), 
				conf.getMaxPages(), conf.getSources(), conf.getMinPages());
		//TODO is it ok to always use maxLinkage as y0? In older version, in case of constant curve,
		//conf.getSources were used
		this.productLinkageCurve = CurveFunctionFactory.buildCurveFunction(conf.getSizeCurveType(), 
				conf.getMaxLinkage(), this.sizeCurve.getSampling());
		this.nProducts = this.productLinkageCurve.getYValues().length;
		this.stringGenerator = sg;
	}

	// generates the pool of token to use for the fixed part of the values
	private void generateTokenPools() {
		// generate a different pool for each cardinality Class
		for (Integer nToken : this.conf.getCardinalityClasses().getClasses()) {
			Set<String> tokenPool = new HashSet<>();
			// nToken*15 to generate a pool bigger than necessary
			for (int i = 0; i < nToken * nToken; i++) {
				tokenPool.add(this.stringGenerator.generateAttributeToken());
			}
			this.fixedTokenPool.put(nToken, new ArrayList<String>(tokenPool));
		}
	}

	// generates the attributes' names and their possible values
	private void prepareAttributes() {
		// generate attributes ids
		Set<String> attrNamesSet = new HashSet<>();
		List<String> attrNames = new ArrayList<>();

		while (attrNamesSet.size() < this.conf.getAttributes()) {
			attrNamesSet.add(this.stringGenerator.generateAttributeName());
		}
		attrNames.addAll(attrNamesSet);

		this.cardinalities = this.conf.getCardinalityClasses().assignClasses(attrNames);
		this.tokens = this.conf.getTokenClasses().assignClasses(attrNames);

		generateTokenPools();
		for (String attribute : attrNames) {
			int cardinality = this.cardinalities.get(attribute);
			List<String> tokenPool = this.fixedTokenPool.get(cardinality);
			Collections.shuffle(tokenPool);
			TokenClass attrToken = this.tokens.get(attribute);
			String fixedTokens = "";

			/*
			 * TODO FP: perché avere un pool di token fissi, invece di generarli per ogni
			 * attributo? I token fissi sono fissi all'interno dell'attributo, non tra
			 * attributi o sbaglio?
			 */
			for (int i = 0; i < attrToken.getFixed(); i++) {
				// If we reach the end of available tokens, we come back from the beginning
				fixedTokens += tokenPool.get(i % tokenPool.size()) + " ";
			}

			this.attrFixedToken.put(attribute, fixedTokens.trim());

			List<String> generatedValues = new ArrayList<String>();
			Set<String> valueSet = new HashSet<>();
			while (valueSet.size() < cardinality) {
				String value = "";
				for (int j = 0; j < attrToken.getRandom(); j++)
					value += this.stringGenerator.generateAttributeToken() + " ";
				value += fixedTokens;
				valueSet.add(value.trim());
			}

			generatedValues.addAll(valueSet);
			this.attrValues.put(attribute, generatedValues);
		}
	}

	// generates a single product
	private CatalogueProductPage generateProductPage(int id) {
		CatalogueProductPage page = new CatalogueProductPage(id, CATEGORY_NAME_FOR_SYNTHETIC_DATASET);
		for (Map.Entry<String, List<String>> attribute : this.attrValues.entrySet()) {
			String name = attribute.getKey();
			int index = new Random().nextInt(attribute.getValue().size());
			String value = attribute.getValue().get(index);
			page.addAttributeValue(name, value);
		}
		return page;
	}

	// generates all products and adds them to the catalogue
	private List<CatalogueProductPage> generateProducts() {
		List<CatalogueProductPage> products = new ArrayList<CatalogueProductPage>();
		List<String> attributes = new ArrayList<>();
		attributes.addAll(this.attrValues.keySet());

		// each iteration is a batch of products to upload
		for (int i = 0; i < this.nProducts; i++) {
			CatalogueProductPage prod = generateProductPage(i);
			products.add(prod);
		}

		return products;
	}

	public List<CatalogueProductPage> createCatalogue() {
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
