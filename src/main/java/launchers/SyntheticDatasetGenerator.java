package launchers;

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

import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import model.Configurations;
import model.CurveFunction;

public class SyntheticDatasetGenerator {

	public SyntheticDatasetGenerator(){
		
	}	
	
	//generates random string of given length
	private String generateRandomString(int length){
		String allowedChars = "abcdefghijklmnopqrstuvwxyz"
								+"ABCDEFGHIJKLMNOPQRSTUVWXYZ"
								+"0123456789";
		char[] chars = allowedChars.toCharArray();
		StringBuilder sb = new StringBuilder(length);
		Random random = new Random();
		for (int i = 0; i < length; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		
		return sb.toString();
	}
	
	//assigns to each attribute a cardinality/token class
	private void assignClasses(List<String> attrs, 
							  Map<String, String> attrsClasses, String[] classes, int[] percentages){
		
		List<Integer> indexes = IntStream.range(0, attrs.size())
								 .boxed()
								 .collect(Collectors.toList());
		Collections.shuffle(indexes);
		
		int[] partitions = new int[percentages.length];
		int acc = 0;
		for(int i = 0; i < percentages.length-1; i++){
			int partition = percentages[i] * attrs.size() / 100;
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
	
	//generates the attributes' names and their possible values
	private Map<String, List<String>> prepareAttributes(int nAttr, String[] cardClasses,
								  int[] cardPercentages, String[] tokenClasses, int[] tokenPercentages){
		
		//generate attributes ids
		Set<String> attrNamesSet = new HashSet<>();
		List<String> attrNames = new ArrayList<>();
		while(attrNamesSet.size() < nAttr){
			attrNamesSet.add(generateRandomString(20));
		}
		attrNames.addAll(attrNamesSet);
		
		//attribute -> cardinality
		Map<String, String> cardinalities = new HashMap<>();
		assignClasses(attrNames, cardinalities, cardClasses, cardPercentages);
		//attribute -> token type
		Map<String, String> tokens = new HashMap<>();
		assignClasses(attrNames, tokens, tokenClasses, tokenPercentages);
		//attribute -> value domain
		Map<String, List<String>> values = new HashMap<>();
		
		
		for(String attribute : attrNames){
//			System.out.println(cardinalities.size());
			int cardinality = Integer.valueOf(cardinalities.get(attribute));
			String[] attrTokens = tokens.get(attribute).split("-");
			String fixedTokens = "";
			
			for(int i = 0; i < Integer.valueOf(attrTokens[1]); i++){
				fixedTokens += generateRandomString(7)+" ";
			}
			
			List<String> generatedValues = new ArrayList<String>();
			Set<String> valueSet = new HashSet<>();
			while(valueSet.size() < cardinality){
				String value = "";
				for(int j = 0; j < Integer.valueOf(attrTokens[0]); j++)
					value += generateRandomString(7)+" ";
				value += fixedTokens;
				valueSet.add(value.trim());
			}
			
			generatedValues.addAll(valueSet);			
			values.put(attribute, generatedValues);			
		}
		
		return values;
	}
	
	//generates a single product
	private Document generateProduct(Map<String, List<String>> attributes, int id){
		Document prod = new Document();
		Document specs = new Document();
		prod.append("_id", id);
		prod.append("category", "fakeCategory");
		
		for(Map.Entry<String, List<String>> attribute : attributes.entrySet()){
			String name = attribute.getKey();
			int index = new Random().nextInt(attribute.getValue().size());
			String value = attribute.getValue().get(index);
			specs.append(name, value);
		}
		prod.append("spec", specs);		
		
		return prod;
	}
	
	//generates all products and adds them to the catalogue
	private void generateProducts(int nProds, Map<String, List<String>> attrValues, FileDataConnector fdc){ 
		MongoDBConnector mdbc = new MongoDBConnector("mongodb://localhost:27017", "SyntheticDataset", fdc);
		mdbc.dropCollection("Catalogue");
		int batchSize = 20, updatedProds = 0;		
		List<String> attributes = new ArrayList<>();
		attributes.addAll(attrValues.keySet());
		
		//each iteration is a batch of products to upload
		while(updatedProds != nProds){
			int size = (nProds - updatedProds > batchSize) ? batchSize : nProds-updatedProds;
			List<Document> batch = new ArrayList<>();
			for(int i = 0; i < size; i++){
				int id = updatedProds+i;
				Document prod = generateProduct(attrValues, id);
				batch.add(prod);
			}
			mdbc.insertBatch(batch, "Catalogue");
			updatedProds += size;
		}
	}
	
	public static void main(String[] args){
		SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator();
		FileDataConnector fdc = new FileDataConnector();
		Configurations conf = new Configurations(fdc.readConfig());
		
		//read configuration parameters
		int maxSize = conf.getMaxPages();
		int minSize = conf.getMinPages();
		int nSources = conf.getSources();
		int nAttr = conf.getAttributes();
		int nProds;
		int typeSize = conf.getSizeCurveType();
		int typePLinkage = conf.getProdCurveType();
		int typeALinkage = conf.getAttrCurveType();
		
		//create curves
		CurveFunction sizeCurve, pLinkageCurve, aLinkageCurve;
		sizeCurve = new CurveFunction(typeSize, maxSize, nSources, minSize);
		pLinkageCurve = new CurveFunction(typePLinkage, nSources, sizeCurve.getSampledIntegral());
		nProds = pLinkageCurve.getYValues().length;
		aLinkageCurve = new CurveFunction(typeALinkage, nSources, nAttr, 1);
		
		//prepare attributes
		String[] cardClasses = conf.getCardinalityClasses();
		int[] cardPercentages = conf.getCardinalityPercentages();
		String[] tokenClasses = conf.getTokenClasses();
		int[] tokenPercentages = conf.getTokenPercentages();
		Map<String, List<String>> attrValues;
		attrValues = sdg.prepareAttributes(nAttr, cardClasses, cardPercentages, tokenClasses, tokenPercentages);
		
		//generate products
		sdg.generateProducts(nProds, attrValues, fdc);
		
	}
}
