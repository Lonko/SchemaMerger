package launcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matcher.CategoryMatcher;
import model.Match;
import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import connectors.RConnector;

public class Cohordinator {
	
	public Cohordinator(){
		
	}	
	
	public List<List<String>> matchAllSourcesInCategory(List<String> orderedWebsites, String category,
													CategoryMatcher cm, int cardinality){
		
		Map<String, String> schema = new HashMap<>();
		List<String> currentMatchSources = new ArrayList<>();
		currentMatchSources.add(orderedWebsites.get(0));
		
		//match su tutte le altre sorgenti
		for(int i = 1; i < orderedWebsites.size(); i++){
			currentMatchSources.add(orderedWebsites.get(i));
			Match newMatch = cm.getMatch(currentMatchSources, category, cardinality, schema);
		}
		
		return buildFinalSchema(schema);
	}
	
	private List<List<String>> buildFinalSchema(Map<String, String> schema){
		Map<String, List<String>> schemaMap = new HashMap<>();
		List<List<String>> finalSchema = new ArrayList<>();
		
		schema.entrySet().forEach(entry -> {
			List<String> attributes = schemaMap.getOrDefault(entry.getValue(), new ArrayList<String>());
			attributes.add(entry.getKey());
			schemaMap.put(entry.getValue(), attributes);
		});
		
		finalSchema.addAll(schemaMap.values());
		finalSchema.sort(Comparator.comparing(List::size));
		Collections.reverse(finalSchema);
		
		return finalSchema;
	}
	
	public static void main(String [] args){
		long start = System.currentTimeMillis();
		FileDataConnector fdc = new FileDataConnector();
		long time = System.currentTimeMillis() - start;
		System.out.println("Created FileDataConnector (" + (time/1000) + " s)");
		MongoDBConnector mdbc = new MongoDBConnector(fdc);
		time =  System.currentTimeMillis() - start;
		System.out.println("Created MongoDBConnector (" + (time/1000) + " s)");
//		String ds = "src/main/resources/filtered_specifications";
//		String rl = "src/main/resources/id2category2urls.json";
//		String ts = "src/main/resources/training_sets";
//		FileDataConnector fdc = new FileDataConnector(ds, rl, ts);
//		MongoDBConnector mdbc = new MongoDBConnector("mongodb://localhost:27017", "FilteredDataset", fdc);
//		mdbc.initializeAllCollections();
//		mdbc.initializeCollection("Schemas");
		String modelPath = "/home/marco/workspace/SchemaMerger/src/main/resources/classification/modelN.rda";
		RConnector r = new RConnector(modelPath);
		r.start();
		time =  System.currentTimeMillis() - start;
		System.out.println("Started RConnector (" + (time/1000) + " s)");
		CategoryMatcher cm = new CategoryMatcher(mdbc, r);
		time =  System.currentTimeMillis() - start;		
		System.out.println("Created CategoryMatcher (" + (time/1000) + " s)");
//		fdc.printTrainingSet("tsWithTuples", cm.getTrainingSetWithTuples(1000, 14000, true, 1.0));
		List<String> websites = new ArrayList<>();
		websites.add("vanvreedes.com");
		websites.add("brothersmain.com");
		websites.add("www.wettsteins.com");
		websites.add("www.jsappliance.com");
		websites.add("www.digiplususa.com");
		websites.add("www.rewstv.com");
		websites.add("www.dakotatv.com");
		Cohordinator c = new Cohordinator();
		time =  System.currentTimeMillis() - start;
		try{
//			Match match = cm.getMatch(websites, "tv", 0);
//			fdc.printMatch("wettsteins(card1)", match.toCSVFormat());
			fdc.printSchema("schema7", c.matchAllSourcesInCategory(websites, "tv", cm, 0));
		} finally {
			r.stop();          
		}
		time =  System.currentTimeMillis() - start;
		System.out.println("Fine match (" + (time/1000) + " s)");
	}
}
