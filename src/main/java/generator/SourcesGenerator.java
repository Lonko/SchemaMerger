package generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.bson.Document;

import connectors.MongoDBConnector;
import models.generator.Configurations;
import models.generator.ConstantCurveFunction;
import models.generator.CurveFunction;
import models.generator.RationalCurveFunction;

public class SourcesGenerator {
	
	//random
	private static final double RANDOM_ERROR_CHANCE = 0.001;
	//for example 1 inch = 2.5 cm (affects all tokens)
	private static final double DIFFERENT_FORMAT_CHANCE = 0.05;
	//for example 1 cm = 1 centimeter (affects only fixed tokens)
	private static final double DIFFERENT_REPRESENTATION_CHANCE = 0.05;
	private MongoDBConnector mdbc;
	private StringGenerator stringGenerator;
	private CurveFunction aExtLinkageCurve;
	private CurveFunction sourceSizes;
	private CurveFunction productLinkage;
	private int nSources;
	private int nAttributes;
	private Map<String, List<String>> schemas = new HashMap<>();
	//Map <Source, ids of products in source>
	private Map<String, List<Integer>> source2Ids = new HashMap<>();
	//Map <id, Sources in which it appears>
	private Map<Integer, List<String>> id2Sources = new HashMap<>();
	//Map <Attribute, fixed token>
	private Map<String, String> fixedTokens = new HashMap<>();
	//Map <Attribute, values>
	private Map<String, List<String>> values = new HashMap<>();
	//list of "head" attribute (in terms of number of sources in which they appear)
	private List<String> headAttributes = new ArrayList<>();
	//list of "tail" attribute (in terms of number of sources in which they appear)
	private List<String> tailAttributes = new ArrayList<>();
	
	public SourcesGenerator(MongoDBConnector mdbc, Configurations conf, StringGenerator sg, 
							CurveFunction sizes, CurveFunction prods, Map<String, String> fixedTokens,
							Map<String, List<String>> values){
		
		this.mdbc = mdbc;
		String curveType = conf.getAttrCurveType();
		this.nSources = conf.getSources();
		this.nAttributes = conf.getAttributes();
		this.fixedTokens = fixedTokens;
		this.values = values;
		this.sourceSizes = sizes;
		this.productLinkage = prods;
		if(curveType.equals("0"))
			this.aExtLinkageCurve = new ConstantCurveFunction(this.nSources, this.nAttributes, 1);
		else
			this.aExtLinkageCurve = new RationalCurveFunction(curveType, this.nSources, this.nAttributes, 1);
		
		this.stringGenerator = sg;		
	}
	
	//assigns attributes to each source
	private void assignAttributes(List<String> sourcesNames){
		List<String> shuffledSources = new ArrayList<>(sourcesNames);
		List<String> attributes = new ArrayList<>();
		attributes.addAll(this.fixedTokens.keySet());
		int[] attrLinkage = this.aExtLinkageCurve.getYValues();
		int headThreshold = this.aExtLinkageCurve.getHeadThreshold();
		
		//for each attribute
		for(int i = 0; i < attributes.size(); i++){
			String attribute = attributes.get(i);
			int linkage = attrLinkage[i];
			Collections.shuffle(shuffledSources);
			//mark attribute as head or tail
			if(i <= headThreshold)
				this.headAttributes.add(attribute);
			else
				this.tailAttributes.add(attribute);
			/* add it to the schema of n random sources
			 * with n given by the yValue on the curve for the external attribute linkage
			 */
			for(int j = 0; j < linkage; j++){
				String source = shuffledSources.get(j);
				List<String> schema = this.schemas.getOrDefault(source, new ArrayList<String>());
				schema.add(attribute);
				this.schemas.put(source, schema);
			}
		}
	}
	
	/* try replacing a product p in a full source with product n
	 * and put p in a different source (that doesn't already have a
	 * page of the same product p)
	 */
	private boolean tryReplace(int idN, int[] sizes, List<String> sourceNames){
		//sources (already full) to which the product n hasn't been assigned 
		List<String> availableSources = new ArrayList<>();
		//ids of products in availableSources
		Set<Integer> possibleSwitchIds = new HashSet<>();
		
		for(String source : sourceNames){
			if(!this.source2Ids.get(source).contains(idN)){
				availableSources.add(source);
				possibleSwitchIds.addAll(this.source2Ids.get(source));
			}
		}
		
		//try replacing
		for(int idP : possibleSwitchIds){
			List<String> sourcesP = this.id2Sources.get(idP);
			//list of sources with p without n
			List<String> intersection = new ArrayList<>(availableSources);
			intersection.retainAll(sourcesP);
			String candidateSource;
			if(intersection.size() == 0)
				continue;
			else
				candidateSource = intersection.get(0);
			for(String source : sourceNames){
				int index = sourceNames.indexOf(source);
				if(!this.source2Ids.get(source).contains(idP) &&
					this.source2Ids.get(source).size() != sizes[index]){
					//update info P
					List<Integer> ids = this.source2Ids.get(source);
					List<String> sources = this.id2Sources.get(idP);
					ids.add(idP);
					sources.add(source);
					sources.remove(candidateSource);
					this.source2Ids.put(source, ids);
					this.id2Sources.put(idP, sources);
					//update info N
					ids = this.source2Ids.get(candidateSource);
					sources = this.id2Sources.get(idN);
					ids.remove(idP);
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
	
	//assigns products to each source
	private void assignProducts(List<String> sourcesNames){
		List<String> shuffledSources = new ArrayList<>(sourcesNames);
		int[] prodsLinkage = this.productLinkage.getYValues();
		int[] sSizes = this.sourceSizes.getYValues();
		
		//for each product
		for(int id = 0; id < prodsLinkage.length; id++){
			Collections.shuffle(shuffledSources);
			int linkage = prodsLinkage[id];
			int j = 0, i = 0;
			/* add it to the ids lists of n random sources
			 * with n given by the yValue on the curve for the product linkage
			 * while checking not to go above the source size
			 */
			while(j < linkage){
				if(i == shuffledSources.size()){
					if(tryReplace(id, sSizes, sourcesNames)){
						j++;
						continue;
					}else{
						System.err.println("IMPOSSIBLE TO PLACE PRODUCT PAGE");
						try {
							System.in.read();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}					
				String source = shuffledSources.get(i);
				List<Integer> ids = this.source2Ids.getOrDefault(source, new ArrayList<Integer>());
				int index = sourcesNames.indexOf(source);
				//skip if this source is full
				if(ids.size() == sSizes[index]){
					shuffledSources.remove(source);
					continue;
				}
				List<String> sources = this.id2Sources.getOrDefault(id, new ArrayList<String>());
				sources.add(source);
				ids.add(id);
				i++;
				j++;
			}
		}
	}
	
	//selects a subset of attributes for the head and tail partitions of a source's schema
	private List<String> getHeadAttributes(List<String> schema, CurveFunction curve){
		List<String> headAttributes = new ArrayList<>();
		List<String> tailAttributes = new ArrayList<>();
		int head = curve.getHeadThreshold();
		
		//partition the attributes according to both internal and external linkage
		for(String attribute : schema){
			boolean isHead = this.headAttributes.contains(attribute);
			if(isHead)
				headAttributes.add(attribute);
			else
				tailAttributes.add(attribute);
			if(headAttributes.size() == head)
				break;
		}
		if(headAttributes.size() < head)
			for(String attribute : tailAttributes){
				headAttributes.add(attribute);
				if(headAttributes.size() == head)
					break;				
			}
		
		return headAttributes;
	}
	
	//assigns a subset of attributes to each prod in source
	private Map<Integer, List<String>> getProdsAttrs(String source, List<String> schema, CurveFunction curve){
		List<String> headAttributes = getHeadAttributes(schema, curve);
		List<String> tailAttributes = new ArrayList<>(schema);
		tailAttributes.removeAll(headAttributes);
		int threshold = headAttributes.size();
		List<Integer> shuffledIds = new ArrayList<>(this.source2Ids.get(source));
		Map<Integer, List<String>> prodsAttrs = new HashMap<>();
		
		//for each attribute
		for(int i = 0; i < schema.size(); i++){
			String attribute = "";
			if(i < threshold)
				attribute = headAttributes.get(i);
			else
				attribute = tailAttributes.get(i-threshold);
			int linkage = curve.getYValues()[i];
			Collections.shuffle(shuffledIds);
			/* add it to the attributes of n random products
			 * with n given by the yValue on the curve for the internal attribute linkage
			 */
			for(int j = 0; j < linkage; j++){
				int id = shuffledIds.get(j);
				List<String> attrs = prodsAttrs.getOrDefault(id, new ArrayList<String>());
				attrs.add(attribute);
				prodsAttrs.put(id, attrs);
			}
		}
		
		return prodsAttrs;
	}
	
	/* assigns an error type (including "none") to each attribute;
	 * does not include random error, which is taken into consideration
	 * during the actual product page creation
	 */
	private Map<String, String> checkErrors(List<String> schema){
		Map<String, String> errors = new HashMap<>();
		Random rand = new Random();
		
		for(String attribute : schema){
			double chance = rand.nextDouble();
			if(chance <= DIFFERENT_FORMAT_CHANCE)
				errors.put(attribute, "format");
			else if(chance <= DIFFERENT_REPRESENTATION_CHANCE)
				errors.put(attribute, "representation");
			else
				errors.put(attribute, "none");
		}
		
		return errors;
	}
	
	//applies format error to attributes' values
	private Map<String, List<String>> applyErrors(List<String> schema, Map<String, String> errors){
		Map<String, List<String>> fErrors = new HashMap<>();
		
		for(String attribute : schema){
			//if the attribute has been assigned error type "format"
			if(errors.containsKey(attribute) && !errors.get(attribute).equals("none")){
				String type = errors.get(attribute);
				List<String> newValues = new ArrayList<>();
				String fixedPart = this.fixedTokens.get(attribute);
				String[] fixTokens = fixedPart.split(" ");
				List<String> newFixTokens = new ArrayList<>();
				//generate new fixed tokens
				for(int i = 0; i < fixTokens.length; i++){
					String newToken = this.stringGenerator.generateAttributeToken();
					if(!newToken.equals(fixTokens[i]) && !newFixTokens.contains(newToken))
						newFixTokens.add(newToken);
					else
						i--;
				}
				String newFixedPart = String.join(" ", newFixTokens);
				//generate new random tokens and values
				for(String value : this.values.get(attribute)){
					String randomPart = value.substring(0, value.length()-fixedPart.length()-1);
					String newRandomPart = randomPart;
					if(type.equals("format")){
						String[] randTokens = randomPart.split(" ");
						List<String> newRandTokens = new ArrayList<>();
						for(int j = 0; j < randTokens.length; j++){
							String newToken = this.stringGenerator.generateAttributeToken();
							if(!newToken.equals(randTokens[j]) && !newRandTokens.contains(newToken))
								newRandTokens.add(newToken);
							else
								j--;
						}
						newRandomPart = String.join(" ", newRandTokens);
					}
					newValues.add(newRandomPart+" "+newFixedPart);
				}
				fErrors.put(attribute, newValues);
			}
		}
		
		return fErrors;
	}
	
	//updates attributes values by applying the new values based on the errors
	private Document generateSpecs(Document oldSpecs, Map<String, List<String>> newValues, List<String> attrs){
		Document newSpecs = new Document();
		Random rand = new Random();
		
		//check each attribute
		for(String attribute : oldSpecs.keySet()){
			//continue if attribute has not been assigned to this product
			if(!attrs.contains(attribute))
				continue;
			String oldValue = oldSpecs.getString(attribute);
			String newValue = oldValue;
			//get modified value (with error) if necessary
			if(newValues.containsKey(attribute)){
				int index = this.values.get(attribute).indexOf(oldValue);
				newValue = newValues.get(attribute).get(index);
			}
			List<String> tokens = new ArrayList<>();
			//random error
			for(String token : newValue.split(" ")){
				double chance = rand.nextDouble();
				if(chance <= RANDOM_ERROR_CHANCE)
					tokens.add(this.stringGenerator.generateAttributeToken());
				else
					tokens.add(token);				
			}
			newSpecs.append(attribute, String.join(" ", tokens));
		}
		
		return newSpecs;
	}
	
	//generates the products' pages for the source
	private List<Document> createProductsPages(String source, Map<String, List<String>> newValues,
												Map<Integer, List<String>> pAttrs, List<Document> products){
		List<Document> prodPages = new ArrayList<>();
		
		for(Document prod : products){
			int id = prod.getInteger("_id");
			List<String> attrs = pAttrs.get(id);
			Document page = new Document();
			Document newSpecs = generateSpecs(prod.get("spec", Document.class), newValues, attrs);
			List<String> linkage = new ArrayList<>();
			for(String rlSource : this.id2Sources.get(id))
				if(!rlSource.equals(source))
					linkage.add(rlSource+"/"+id+"/");
			
			page.append("category", "fakeCategory");
			page.append("url", source+"/"+id+"/");
			page.append("spec", newSpecs);
			page.append("linkage", linkage);
			page.append("website", source);
			
			prodPages.add(page);
		}
		
		
		return prodPages;
	}
	
	//generates all sources
	private List<Document> createSource(String sourceName, int size, List<Document> products){
		List<String> schema = this.schemas.get(sourceName);
		CurveFunction aIntLinkage = new RationalCurveFunction("2", size, schema.size(), 1);
		Map<Integer, List<String>> prodsAttrs = getProdsAttrs(sourceName, schema, aIntLinkage);
		Map<String, String> attrErrors = checkErrors(schema);
		Map<String, List<String>> newValues = applyErrors(schema, attrErrors);
		return createProductsPages(sourceName, newValues, prodsAttrs, products);
	}
	
	//assigns attributes and products to each source
	private List<String> prepareSources(){
		//generate sources ids
		Set<String> sourceNamesSet = new HashSet<>();
		List<String> sourcesNames = new ArrayList<>();

		while(sourceNamesSet.size() < this.nSources){
			sourceNamesSet.add("www."+this.stringGenerator.generateSourceName()+".com");
		}
		sourcesNames.addAll(sourceNamesSet);
		
		assignAttributes(sourcesNames);
		assignProducts(sourcesNames);
		
		return sourcesNames;
	}
	
	//uploads a source to mongodb
	private void uploadSource(List<Document> productPages){
		int batchSize = 100, uploadedProds = 0;		
		
		//each iteration is a batch of products to upload
		while(uploadedProds != productPages.size()){
			int size = (productPages.size() - uploadedProds > batchSize) ? 
								batchSize : 
								productPages.size() - uploadedProds;
			List<Document> batch = new ArrayList<>();
			for(int i = 0; i < size; i++){
				int index = uploadedProds+i;
				batch.add(productPages.get(index));
			}
			this.mdbc.insertBatch(batch, "Products");
			uploadedProds += size;
		}
	}
	
	public void createSources(){
		List<String> sourcesNames = prepareSources();
		List<Document> sourcePages;
		
		this.mdbc.dropCollection("Products");
		for(int i = 0; i < sourcesNames.size(); i++){
			String source = sourcesNames.get(i);
			int size = this.sourceSizes.getYValues()[i];
			List<Integer> ids = this.source2Ids.get(source);
			List<Document> products = this.mdbc.getFromCatalogue(ids);
			sourcePages = createSource(source, size, products);
			uploadSource(sourcePages);
			System.out.println("Sorgenti caricate: " + (i+1)+"\t(# pagine della corrente: "
								+sourcePages.size()+")");
		}
	}

}
