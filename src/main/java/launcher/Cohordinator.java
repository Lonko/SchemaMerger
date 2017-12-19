package launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matcher.CategoryMatcher;
import model.Match;
import model.Schema;
import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import connectors.RConnector;

public class Cohordinator {
	
	public Cohordinator(){
		
	}	
	
	public Schema matchAllSourcesInCategory(List<String> orderedWebsites, String category,
													CategoryMatcher cm, int cardinality, boolean useMI){
		
		Schema schema = new Schema();
		List<String> currentMatchSources = new ArrayList<>();
		currentMatchSources.add(orderedWebsites.get(0));
		
		//match su tutte le altre sorgenti
		for(int i = 1; i < orderedWebsites.size(); i++){
			currentMatchSources.add(orderedWebsites.get(i));
			Match newMatch = cm.getMatch(currentMatchSources, category, cardinality, schema, useMI);
		}
		
		return schema;
	}
	
	/* checks the schemas of the sources in the selected categories and returns the couples of sources
	 * with schemas that have an overlap of attribute (based on their names) above 50% of the smallest 
	 * of the two sources. */
	public Map<String, List<String>> findClonedSources(MongoDBConnector mdbc, List<String> categories){
		Map<String, List<String>> clonedSources = new HashMap<>();
		Map<String, List<String>> sourceSchemas = mdbc.getSchemas(categories);
		
		for(String source1 : sourceSchemas.keySet()){
			String category1 = source1.split("###")[0];
			List<String> attributes1 = sourceSchemas.get(source1);
			for(String source2 : sourceSchemas.keySet()){
				if(source1.equals(source2))
					continue;
				String category2 = source2.split("###")[0];
				if(! category1.equals(category2))
					continue;
				List<String> attributes2 = sourceSchemas.get(source2);
				double minSize = (attributes1.size() > attributes2.size()) ? 
									attributes2.size() :
									attributes1.size();
				Set<String> intersection = new HashSet<>(attributes1);
				intersection.retainAll(attributes2);
				if(intersection.size() >= (minSize / 2)){
					List<String> clones = clonedSources.getOrDefault(source1, new ArrayList<>());
					clones.add(source2);
					clonedSources.put(source1, clones);
				}
			}
		}
		
		return clonedSources;
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
		boolean useFeature = false;
		String modelPath;
		if(useFeature)
			modelPath = "/home/marco/workspace/SchemaMerger/src/main/resources/classification/modelN.rda";
		else
			modelPath = "/home/marco/workspace/SchemaMerger/src/main/resources/classification/modelN_noC.rda";
		RConnector r = new RConnector(modelPath);
		r.start();
		time =  System.currentTimeMillis() - start;
		System.out.println("Started RConnector (" + (time/1000) + " s)");
		CategoryMatcher cm = new CategoryMatcher(mdbc, r);
		time =  System.currentTimeMillis() - start;		
		System.out.println("Created CategoryMatcher (" + (time/1000) + " s)");
//		fdc.printTrainingSet("tsWithTuples", cm.getTrainingSetWithTuples(1000, 14000, true, 1.0));
		List<String> websites = new ArrayList<>();
		Cohordinator c = new Cohordinator();
		time =  System.currentTimeMillis() - start;
		List<String> categories = Arrays.asList("tv", "software", "camera", "toilets", "shoes", "monitor",
												"sunglasses", "cutlery", "notebook", "headphone");
		try{
//			Match match = cm.getMatch(websites, "tv", 0);
//			fdc.printMatch("wettsteins(card1)", match.toCSVFormat());
//			fdc.printSchema("schema7_camera_noC", c.matchAllSourcesInCategory(websites, "camera", cm, 0, useFeature));
			fdc.printClonedSources("clones", c.findClonedSources(mdbc, categories));
		} finally {
			r.stop();          
		}
		time =  System.currentTimeMillis() - start;
		System.out.println("Fine match (" + (time/1000) + " s)");
	}
}
