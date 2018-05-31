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
import model.SyntheticAttribute;
import models.generator.CurveFunction;
import models.generator.CurveFunctionFactory;

/**
 * Generator of catalogue schema and pages
 * @author mmonteleone
 *
 */
public class CatalogueGenerator {

	private static final String CATEGORY_NAME_FOR_SYNTHETIC_DATASET = "fakeCategory";
	private StringGenerator stringGenerator;
	private CurveFunction sources2nbPagesCurve;
	private CurveFunction product2nbSourcesCurve;
	private CurveFunction attribute2nbSourcesCurve;
	private Map<Integer, List<String>> fixedTokenPool = new HashMap<>();
	private Map<SyntheticAttribute, List<String>> attrValues = new HashMap<>();
	private Map<SyntheticAttribute, String> attrFixedToken = new HashMap<>();
	private CatalogueConfiguration conf;
	private int nProducts;

	public CatalogueGenerator(CatalogueConfiguration conf, StringGenerator sg) {
		this.conf = conf;
		this.sources2nbPagesCurve = CurveFunctionFactory.buildCurveFunction(conf.getSizeCurveType(), 
				conf.getMaxPages(), conf.getSources(), conf.getMinPages());
		//TODO is it ok to always use maxLinkage as y0? In older version, in case of constant curve,
		//conf.getSources were used
		this.product2nbSourcesCurve = CurveFunctionFactory.buildCurveFunction(conf.getSizeCurveType(), 
				conf.getMaxLinkage(), this.sources2nbPagesCurve.getSampling());
		this.attribute2nbSourcesCurve = CurveFunctionFactory.buildCurveFunction(this.conf.getAttrCurveType(), this.conf.getSources(), 
				conf.getAttributes(), 1);
		this.nProducts = this.product2nbSourcesCurve.getYValues().length;
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
		Set<SyntheticAttribute> attrNamesSet = new HashSet<>();
		List<SyntheticAttribute> attrNames = new ArrayList<>();

		while (attrNamesSet.size() < this.conf.getAttributes()) {
			attrNamesSet.add(new SyntheticAttribute(this.stringGenerator.generateAttributeName()));
		}
		attrNames.addAll(attrNamesSet);

		this.conf.getCardinalityClasses().assignClasses(attrNames, (attr, card) -> attr.setCardinality(card));
		this.conf.getAttributeRandomErrorClasses().assignClasses(attrNames, (attr, errorRate) -> attr.setErrorRate(errorRate));
		this.conf.getTokenClasses().assignClasses(attrNames, (attr, tokenClass) -> attr.setTokenClass(tokenClass));

		generateTokenPools();
		int headThreshold = this.attribute2nbSourcesCurve.getHeadThreshold();
		for (int i = 0; i< attrNames.size(); i++) {
			SyntheticAttribute attribute = attrNames.get(i);
			List<String> tokenPool = this.fixedTokenPool.get(attribute.getCardinality());
			Collections.shuffle(tokenPool);
			String fixedTokens = "";
			// mark attribute as head or tail
			attribute.setHead(i <= headThreshold);

			/*
			 * TODO FP: perché avere un pool di token fissi, invece di generarli per ogni
			 * attributo? I token fissi sono fissi all'interno dell'attributo, non tra
			 * attributi o sbaglio?
			 */
			for (int j = 0; j < attribute.getTokenClass().getFixed(); j++) {
				// If we reach the end of available tokens, we come back from the beginning
				fixedTokens += tokenPool.get(j % tokenPool.size()) + " ";
			}

			this.attrFixedToken.put(attribute, fixedTokens.trim());

			List<String> generatedValues = new ArrayList<String>();
			Set<String> valueSet = new HashSet<>();
			while (valueSet.size() < attribute.getCardinality()) {
				String value = "";
				for (int j = 0; j < attribute.getTokenClass().getRandom(); j++)
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
		for (Map.Entry<SyntheticAttribute, List<String>> attribute : this.attrValues.entrySet()) {
			SyntheticAttribute attr = attribute.getKey();
			int index = new Random().nextInt(attribute.getValue().size());
			String value = attribute.getValue().get(index);
			page.addAttributeValue(attr.toString(), value);
		}
		return page;
	}

	// generates all products and adds them to the catalogue
	private List<CatalogueProductPage> generateProducts() {
		List<CatalogueProductPage> products = new ArrayList<CatalogueProductPage>();
		List<SyntheticAttribute> attributes = new ArrayList<>();
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

	public CurveFunction getSources2nbPagesCurve() {
		return sources2nbPagesCurve;
	}

	public CurveFunction getProduct2nbPagesInLinkageCurve() {
		return product2nbSourcesCurve;
	}

	public CurveFunction getAttribute2nbSourcesCurve() {
		return attribute2nbSourcesCurve;
	}

	public Map<SyntheticAttribute, String> getAttrFixedToken() {
		return attrFixedToken;
	}

	public Map<SyntheticAttribute, List<String>> getAttrValues() {
		return attrValues;
	}
}
