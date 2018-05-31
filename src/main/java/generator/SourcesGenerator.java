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

import connectors.dao.SyntheticDatasetDao;
import model.CatalogueProductPage;
import model.SourceProductPage;
import model.SyntheticAttribute;
import models.generator.Configurations.RecordLinkageErrorType;
import models.generator.CurveFunction;
import models.generator.CurveFunctionFactory;
import models.generator.CurveFunctionFactory.CurveFunctionType;

/**
 * Generator of product pages of each source, provided the catalogue schema and pages
 * @author mmonteleone
 *
 */
public class SourcesGenerator {

	private SyntheticDatasetDao dao;
	private StringGenerator stringGenerator;
	private CurveFunction aExtLinkageCurve;
	private CurveFunction sourceSizes;
	private CurveFunction productLinkage;
	// Map <Source, ids of products in source>
	private Map<String, List<Integer>> source2Ids = new HashMap<>();
	// Map <id, Sources in which it appears>
	private Map<Integer, List<String>> id2Sources = new HashMap<>();
	
	// Map of source name --> Error rates for that source (value, wrong linkage-missing linkage) 
	private Map<String, Double> source2valueErrorRate;
	private Map<String, Double> source2linkageErrorRate;
	private Map<String, Double> source2missingLinkageRate;
	/*
	 * Maps to replace by implementing Attribute objects
	 */
	// Map <Attribute, schema>
	private Map<String, List<SyntheticAttribute>> schemas = new HashMap<>();
	// Map <Attribute, linkage>

	/**
	 * Number of source attributes for each cluster (all correspondent attributes
	 * have same name in synthetic, sot it can be used as map key)
	 */
	private Map<SyntheticAttribute, Integer> linkage = new HashMap<>();
	// Map <Attribute, fixed token>
	private Map<SyntheticAttribute, String> fixedTokens = new HashMap<>();
	// Map <Attribute, values>
	private Map<SyntheticAttribute, List<String>> values = new HashMap<>();
	private SourceGeneratorConfiguration conf;

	public SourcesGenerator(SyntheticDatasetDao dao, SourceGeneratorConfiguration conf, StringGenerator sg, CurveFunction sizes,
			CurveFunction prods, CurveFunction attrsCurve, Map<SyntheticAttribute, String> fixedTokens, Map<SyntheticAttribute, List<String>> values) {
		this.conf = conf;
		this.dao = dao;
		this.fixedTokens = fixedTokens;
		this.values = values;
		this.sourceSizes = sizes;
		this.productLinkage = prods;
		this.aExtLinkageCurve = attrsCurve; 
		this.stringGenerator = sg;
	}

	// assigns attributes to each source
	private void assignAttributes(List<String> sourcesNames) {
		List<String> shuffledSources = new ArrayList<>(sourcesNames);
		List<SyntheticAttribute> attributes = new ArrayList<>(this.fixedTokens.keySet());
		int[] attrLinkage = this.aExtLinkageCurve.getYValues();

		// for each attribute
		for (int i = 0; i < attributes.size(); i++) {
			SyntheticAttribute attribute = attributes.get(i);
			int linkage = attrLinkage[i];
			Collections.shuffle(shuffledSources);
			this.linkage.put(attribute, linkage);
			/*
			 * add it to the schema of n random sources with n given by the yValue on the
			 * curve for the external attribute linkage
			 */
			for (int j = 0; j < linkage; j++) {
				String source = shuffledSources.get(j);
				List<SyntheticAttribute> schema = this.schemas.getOrDefault(source, new ArrayList<SyntheticAttribute>());
				schema.add(attribute);
				this.schemas.put(source, schema);
			}
		}
	}

	/*
	 * try replacing a product p in a full source with product n and put p in a
	 * different source (that doesn't already have a page of the same product p)
	 */
	private boolean tryReplace(int idN, int[] sizes, List<String> sourceNames) {
		// sources (already full) to which the product n hasn't been assigned
		List<String> availableSources = new ArrayList<>();
		// ids of products in availableSources
		Set<Integer> possibleSwitchIds = new HashSet<>();

		for (String source : sourceNames) {
			if (!this.source2Ids.get(source).contains(idN)) {
				availableSources.add(source);
				possibleSwitchIds.addAll(this.source2Ids.get(source));
			}
		}

		// try replacing
		for (int idP : possibleSwitchIds) {
			List<String> sourcesP = this.id2Sources.get(idP);
			// list of sources with p without n
			List<String> intersection = new ArrayList<>(availableSources);
			intersection.retainAll(sourcesP);
			String candidateSource;
			if (intersection.size() == 0)
				continue;
			else
				candidateSource = intersection.get(0);
			for (String source : sourceNames) {
				int index = sourceNames.indexOf(source);
				if (!this.source2Ids.get(source).contains(idP) && this.source2Ids.get(source).size() != sizes[index]) {
					// update info P
					List<Integer> ids = this.source2Ids.get(source);
					List<String> sources = this.id2Sources.get(idP);
					ids.add(idP);
					sources.add(source);
					sources.remove(candidateSource);
					this.source2Ids.put(source, ids);
					this.id2Sources.put(idP, sources);
					// update info N
					ids = this.source2Ids.get(candidateSource);
					sources = this.id2Sources.get(idN);
					ids.remove(Integer.valueOf(idP));
					ids.add(idN);
					sources.add(candidateSource);
					this.source2Ids.put(candidateSource, ids);
					this.id2Sources.put(idN, sources);

					return true;
				}
			}
		}

		return false;
	}

	// assigns products to each source
	private void assignProducts(List<String> sourcesNames) {
		List<String> shuffledSources = new ArrayList<>(sourcesNames);
		int[] prodsLinkage = this.productLinkage.getYValues();
		int[] sSizes = this.sourceSizes.getYValues();

		// for each product
		for (int id = 0; id < prodsLinkage.length; id++) {
			Collections.shuffle(shuffledSources);
			int linkage = prodsLinkage[id];
			int j = 0;
			/*
			 * add it to the ids lists of n random sources with n given by the yValue on the
			 * curve for the product linkage while checking not to go above the source size
			 */
			while (j < linkage) {
				// if there are still sources with space available
				if (j < shuffledSources.size()) {
					String source = shuffledSources.get(j);
					List<Integer> ids = this.source2Ids.getOrDefault(source, new ArrayList<Integer>());
					int index = sourcesNames.indexOf(source);
					// skip if this source is full
					if (ids.size() == sSizes[index]) {
						// remove source from list of available sources and
						// continue
						shuffledSources.remove(source);
						continue;
					}
					List<String> sources = this.id2Sources.getOrDefault(id, new ArrayList<String>());
					sources.add(source);
					ids.add(id);
					// put lists in maps in case they were created for the first
					// time during this iteration
					this.id2Sources.put(id, sources);
					this.source2Ids.put(source, ids);
					j++;
				} else {
					/*
					 * if all sources (without the page relative to this product) are full, try
					 * replacing another product's page
					 */
					if (tryReplace(id, sSizes, sourcesNames)) {
						j++;
						continue;
					} else {
						throw new IllegalStateException("A problem has occurred in the assignment of products' pages");
					}
				}

			}
		}
	}

	// selects a subset of attributes for the head and tail partitions of a
	// source's schema
	private List<SyntheticAttribute> getHeadAttributes(List<SyntheticAttribute> schema, CurveFunction curve) {
		// internal head/tail attributes
		List<SyntheticAttribute> headAttributes = new ArrayList<>();
		List<SyntheticAttribute> tailAttributes = new ArrayList<>();
		int head = curve.getHeadThreshold();

		// partition the attributes according to both internal and external
		// linkage
		for (SyntheticAttribute attribute : schema) {
			if (attribute.isHead())
				headAttributes.add(attribute);
			else
				tailAttributes.add(attribute);
			if (headAttributes.size() == head)
				break;
		}
		if (headAttributes.size() < head)
			for (SyntheticAttribute attribute : tailAttributes) {
				headAttributes.add(attribute);
				if (headAttributes.size() == head)
					break;
			}

		return headAttributes;
	}

	// assigns a subset of attributes to each prod in source
	private Map<Integer, List<SyntheticAttribute>> getProdsAttrs(String source, List<SyntheticAttribute> schema, CurveFunction curve) {
		List<SyntheticAttribute> headAttributes = getHeadAttributes(schema, curve);
		List<SyntheticAttribute> tailAttributes = new ArrayList<>(schema);
		tailAttributes.removeAll(headAttributes);
		int threshold = headAttributes.size();
		List<Integer> shuffledIds = new ArrayList<>(this.source2Ids.get(source));
		Map<Integer, List<SyntheticAttribute>> prodsAttrs = new HashMap<>();

		// for each attribute
		for (int i = 0; i < schema.size(); i++) {
			SyntheticAttribute attribute;
			if (i < threshold)
				attribute = headAttributes.get(i);
			else
				attribute = tailAttributes.get(i - threshold);
			int linkage = curve.getYValues()[i];
			Collections.shuffle(shuffledIds);
			/*
			 * add it to the attributes of n random products with n given by the yValue on
			 * the curve for the internal attribute linkage
			 */
			for (int j = 0; j < linkage; j++) {
				int id = shuffledIds.get(j);
				List<SyntheticAttribute> attrs = prodsAttrs.getOrDefault(id, new ArrayList<SyntheticAttribute>());
				attrs.add(attribute);
				prodsAttrs.put(id, attrs);
			}
		}

		return prodsAttrs;
	}

	/**
	 * assigns an error type (including "none") to each attribute; does not include
	 * random error, which is taken into consideration during the actual product
	 * page creation
	 */
	private Map<SyntheticAttribute, String> checkErrors(List<SyntheticAttribute> schema) {
		Map<SyntheticAttribute, String> errors = new HashMap<>();
		Random rand = new Random();

		for (SyntheticAttribute attribute : schema) {
			double chance = rand.nextDouble();
			if (chance <= this.conf.getDifferentFormatChance())
				errors.put(attribute, "format");
			else if (chance <= this.conf.getDifferentRepresentationChance() + this.conf.getDifferentFormatChance())
				errors.put(attribute, "representation");
			else
				errors.put(attribute, "none");
		}

		return errors;
	}

	// applies format error to attributes' values
	private Map<SyntheticAttribute, List<String>> applyErrors(List<SyntheticAttribute> schema, Map<SyntheticAttribute, String> errors) {
		Map<SyntheticAttribute, List<String>> fErrors = new HashMap<>();

		for (SyntheticAttribute attribute : schema) {
			// if the attribute has been assigned error type "format" or
			// "representation"
			if (errors.containsKey(attribute) && !errors.get(attribute).equals("none")) {
				String type = errors.get(attribute);
				List<String> newValues = new ArrayList<>();
				String fixedString = this.fixedTokens.get(attribute);
				String[] fixTokens = fixedString.split(" ");
				List<String> newFixTokens = new ArrayList<>();
				// generate new fixed tokens
				for (int i = 0; i < fixTokens.length; i++) {
					String newToken = this.stringGenerator.generateAttributeToken();
					if (!newToken.equals(fixTokens[i]) && !newFixTokens.contains(newToken))
						newFixTokens.add(newToken);
					else
						i--;
				}

				String newFixedString = String.join(" ", newFixTokens);
				// generate new random tokens and values
				for (String value : this.values.get(attribute)) {
					String randomString = value.substring(0, value.length() - fixedString.length() - 1);
					String newRandomString = randomString;
					if (type.equals("format")) {
						String[] randTokens = randomString.split(" ");
						List<String> newRandTokens = new ArrayList<>();
						for (int j = 0; j < randTokens.length; j++) {
							String newToken = this.stringGenerator.generateAttributeToken();
							if (!newToken.equals(randTokens[j]) && !newRandTokens.contains(newToken))
								newRandTokens.add(newToken);
							else
								j--;
						}
						newRandomString = String.join(" ", newRandTokens);
					}
					newValues.add(newRandomString + " " + newFixedString);
				}
				fErrors.put(attribute, newValues);
			}
		}

		return fErrors;
	}

	/**
	 * Updates attributes values by applying the new values based on the errors
	 * 
	 * @param page
	 * @param oldSpecs
	 * @param newValues
	 * @param attrs
	 */
	private void addSpecsToProductPage(SourceProductPage page, Map<String, String> oldSpecs,
			Map<SyntheticAttribute, List<String>> newValues, List<SyntheticAttribute> attrs) {
		Random rand = new Random();
		double errorRateForSource = this.source2valueErrorRate.get(page.getSource().getWebsite());

		// check each attribute
		for (String attributeRepr : oldSpecs.keySet()) {
			SyntheticAttribute attribute = SyntheticAttribute.parseAttribute(attributeRepr);
			// continue if attribute has not been assigned to this product
			if (!attrs.contains(attribute))
				continue;
			String oldValue = oldSpecs.get(attributeRepr);
			String newValue = oldValue;
			// get modified value (with error) if necessary
			if (newValues.containsKey(attribute)) {
				int index = this.values.get(attribute).indexOf(oldValue);
				newValue = newValues.get(attribute).get(index);
			}
			List<String> tokens = new ArrayList<>();
			// random error
			Double attributeErrorRate = attribute.getErrorRate();
			double localErrorRateForValue = attributeErrorRate + errorRateForSource - attributeErrorRate * errorRateForSource;
			for (String token : newValue.split(" ")) {
				double chance = rand.nextDouble();
				if (chance <= localErrorRateForValue / newValue.split(" ").length)
					tokens.add(this.stringGenerator.generateAttributeToken());
				else
					tokens.add(token);
			}
			page.addAttributeValue(attribute.toString(), String.join(" ", tokens));
		}
	}

	// generates the products' pages for the source
	private List<SourceProductPage> createProductsPages(String source, Map<SyntheticAttribute, List<String>> newValues,
			Map<Integer, List<SyntheticAttribute>> pAttrs, List<CatalogueProductPage> products) {
		List<SourceProductPage> prodPages = new ArrayList<>();
		Random rnd = new Random();
		double missingLinkageChanceForSource = this.source2missingLinkageRate.get(source);
		double wrongLinkageChanceForSource = this.source2linkageErrorRate.get(source);

		for (CatalogueProductPage prod : products) {
			int realIds = prod.getId();
			List<SyntheticAttribute> attrs = pAttrs.get(realIds);
			String url = source + "/" + realIds + "/";
			SourceProductPage page = new SourceProductPage(this.conf.getCategories().get(0), url, source);

			// linkage and IDs
			List<Integer> ids = buildProductIds(missingLinkageChanceForSource, wrongLinkageChanceForSource, rnd, realIds);
			page.setIds(ids);
			for (Integer id : ids) {
				for (String rlSource : this.id2Sources.get(id)) {
					if (!rlSource.equals(source)) {
						assignLinkageToPage(missingLinkageChanceForSource, wrongLinkageChanceForSource, rnd, id, page, rlSource);
					}
				}
			}

			addSpecsToProductPage(page, prod.getSpecifications(), newValues, attrs);
			prodPages.add(page);
		}

		return prodPages;
	}

	/**
	 * Assign correct linkage to pages, potentially adding error or missing values
	 * @param wrongLinkageChanceForSource 
	 * @param missingLinkageChanceForSource 
	 * 
	 * @param rnd
	 * @param id
	 * @param page
	 * @param rlSource
	 */
	private void assignLinkageToPage(double missingLinkageChanceForSource, double wrongLinkageChanceForSource, 
			Random rnd, int id, SourceProductPage page, String rlSource) {
		// By default the correct linkage should be added to the page
		boolean setCorrectLinkage;
		boolean setWrongLinkage;

		/*
		 * linkageCorrectIndex < this.conf.getMissingLinkageChance() ----> no linkage <p>
		 * this.conf.getMissingLinkageChance() <= linkageCorrectIndex < this.conf.getLinkageErrorChance() ----> wrong
		 * <p> linkage this.conf.getMissingLinkageChance() + this.conf.getLinkageErrorChance() <= linkageCorrectIndex
		 * ----> correct linkage
		 * 
		 * If linkage error is not configured, linkageCorrectIndex is always 1.
		 */
		double linkageCorrectIndex = 1;
		if (this.conf.getRlErrorType().equals(RecordLinkageErrorType.LINKAGE)) {
			linkageCorrectIndex = rnd.nextDouble();
		}
		setCorrectLinkage = missingLinkageChanceForSource + wrongLinkageChanceForSource <= linkageCorrectIndex;
		setWrongLinkage = missingLinkageChanceForSource <= linkageCorrectIndex
				&& linkageCorrectIndex < wrongLinkageChanceForSource + missingLinkageChanceForSource;
		List<String> pageLinkage = page.getLinkage();

		if (setCorrectLinkage) {
			pageLinkage.add(rlSource + "/" + id + "/");
		}
		if (setWrongLinkage) {
			int wrongProdId = rnd.nextInt(this.id2Sources.size());
			int wrongSourceId = rnd.nextInt(this.id2Sources.get(wrongProdId).size());
			pageLinkage.add(this.id2Sources.get(wrongProdId).get(wrongSourceId) + "/" + wrongProdId + "/");
		}
	}

	/**
	 * Find IDs of product in dataset, provided its REAL id and estimating linkage
	 * errors
	 * 
	 * @param rnd
	 * @param id
	 * @param linkage
	 */
	private List<Integer> buildProductIds(double missingLinkageChanceForSource, double wrongLinkageChanceForSource, Random rnd, int realId) {
		// If the error is not on ID but on Linkage, then we just return the real ID
		if (!this.conf.getRlErrorType().equals(RecordLinkageErrorType.ID)) {
			return Arrays.asList(realId);
		}

		List<Integer> ids = new ArrayList<>();
		/*
		 * error < this.conf.getMissingLinkageChance() => no linkage this.conf.getMissingLinkageChance() <= error <
		 * this.conf.getLinkageErrorChance() => wrong linkage this.conf.getMissingLinkageChance() + this.conf.getLinkageErrorChance() <=
		 * error => correct linkage
		 */
		double error = rnd.nextDouble();

		if (error > missingLinkageChanceForSource + wrongLinkageChanceForSource) {
			// no error
			ids.add(realId);
		} else if (error > missingLinkageChanceForSource) {
			// add wrong linkage url
			int wrongProdId = rnd.nextInt(this.id2Sources.size());
			ids.add(wrongProdId);
		}
		return ids;
	}

	/**
	 * Generates all pages in a source
	 * 
	 * @param sourceName
	 * @param size
	 * @param products
	 * @return
	 */
	private List<SourceProductPage> createSource(String sourceName, int size, List<CatalogueProductPage> products) {
		List<SyntheticAttribute> schema = this.schemas.get(sourceName);
		CurveFunction aIntLinkage = CurveFunctionFactory.buildCurveFunction(CurveFunctionType.EXP, size, schema.size(), 1);
		Map<Integer, List<SyntheticAttribute>> prodsAttrs = getProdsAttrs(sourceName, schema, aIntLinkage);
		Map<SyntheticAttribute, String> attrErrors = checkErrors(schema);
		Map<SyntheticAttribute, List<String>> newValues = applyErrors(schema, attrErrors);
		return createProductsPages(sourceName, newValues, prodsAttrs, products);
	}

	// assigns attributes and products to each source
	public List<String> prepareSources() {
		// generate sources ids
		Set<String> sourceNamesSet = new HashSet<>();
		List<String> sourcesNames = new ArrayList<>();

		while (sourceNamesSet.size() < this.conf.getSources()) {
			sourceNamesSet.add("www." + this.stringGenerator.generateSourceName() + ".com");
		}
		sourcesNames.addAll(sourceNamesSet);

		assignAttributes(sourcesNames);
		assignProducts(sourcesNames);
		this.source2valueErrorRate = this.conf.getValueErrorChanceClasses().assignClasses(sourcesNames);
		this.source2linkageErrorRate = this.conf.getLinkageErrorChanceClasses().assignClasses(sourcesNames);
		this.source2missingLinkageRate = this.conf.getMissingLinkageChanceClasses().assignClasses(sourcesNames);

		return sourcesNames;
	}

	// returns a list of source names ordered by linkage with the previous
	// sources
	public List<String> getLinkageOrder(List<String> sourcesNames) {
		List<String> orderedSources = new ArrayList<>();
		List<String> sources2Visit = new ArrayList<>(sourcesNames);
		// the first is the one with the most product pages
		orderedSources.add(sourcesNames.get(0));
		sources2Visit.remove(sourcesNames.get(0));

		// each iteration adds a new source to orderedSources
		while (sources2Visit.size() > 0) {
			int maxLinkage = -1;
			String source = "";
			// Set of product ids of the sources in orderedSources
			Set<Integer> idsInPrevSources = new HashSet<>();
			for (String s : orderedSources) {
				idsInPrevSources.addAll(this.source2Ids.get(s));
			}
			/*
			 * each iteration searches for the source with the most linkage towards the
			 * sources in orderedSources
			 */
			for (int j = 0; j < sources2Visit.size(); j++) {
				String currentSource = sources2Visit.get(j);
				Set<Integer> idsInCurrentSource = new HashSet<>(this.source2Ids.get(currentSource));
				idsInCurrentSource.retainAll(idsInPrevSources);
				int linkage = idsInCurrentSource.size();
				if (linkage > maxLinkage) {
					source = currentSource;
					maxLinkage = linkage;
				}
			}
			orderedSources.add(source);
			sources2Visit.remove(source);
		}

		return orderedSources;
	}

	// generates the complete sources and returns attributes' linkage info
	public Map<SyntheticAttribute, Integer> createSources(List<String> sourcesNames, boolean delete) {
		List<SourceProductPage> sourcePages;

		// generates sources
		if (delete)
			this.dao.deleteAllSourceProductPages();

		for (int i = 0; i < sourcesNames.size(); i++) {
			String source = sourcesNames.get(i);
			int size = this.sourceSizes.getYValues()[i];
			List<Integer> ids = this.source2Ids.get(source);
			List<CatalogueProductPage> products = this.dao.getCatalogueProductsWithIds(ids);
			sourcePages = createSource(source, size, products);
			this.dao.uploadSource(sourcePages);

			System.out.println(
					"Sorgenti caricate: " + (i + 1) + "\t(# pagine della corrente: " + sourcePages.size() + ")");
		}
		this.dao.finalizeSourceUpload();
		return this.linkage;
	}
}
