package launchers;

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
import matcher.FeatureExtractor;
import matcher.TrainingSetGenerator;
import models.matcher.Match;
import models.matcher.Schema;
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
			System.out.println(orderedWebsites.get(i).toUpperCase());
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
	
	public Map<String, List<String>> generateTrainingSets(MongoDBConnector mdbc, List<String> categories, 
																		Map<String, List<String>> clonedSources){
		
		TrainingSetGenerator tsg = new TrainingSetGenerator(mdbc, new FeatureExtractor(), clonedSources);
		Map<String, List<String>> trainingSets = new HashMap<>();
		
		for(String category : categories){
			System.out.println(category.toUpperCase());
			trainingSets.put(category, tsg.getTrainingSetWithTuples(500, 5000, false, true, 0.25, category));
		}
		
		return trainingSets;
	}
	
	public static void main(String [] args){
		boolean useDefaultDB = false;
		boolean useFeature = true;
		long start = System.currentTimeMillis();
		FileDataConnector fdc;
		long time = System.currentTimeMillis() - start;
		MongoDBConnector mdbc;
		time =  System.currentTimeMillis() - start;
		if(useDefaultDB){
			 fdc = new FileDataConnector();
			 mdbc = new MongoDBConnector(fdc);
		} else {
			String conf = "src/main/resources/config.properties";
			String ds = "src/main/resources/filtered_specifications";
			String rl = "src/main/resources/id2category2urls.json";
			String ts = "src/main/resources/classification";
			fdc = new FileDataConnector(ds, rl, ts, conf);
			mdbc = new MongoDBConnector("mongodb://localhost:27017", "FilteredDataset", fdc);
		}
//		String modelPath;
//		if(useFeature)
//			modelPath = "/home/marco/workspace/SchemaMerger/src/main/resources/classification/modelN.rda";
//		else
//			modelPath = "/home/marco/workspace/SchemaMerger/src/main/resources/classification/modelN_noC.rda";

		String modelPath = "/home/marco/workspace/SchemaMerger/src/main/resources/classification/"+
							"modelClean_noMI.rda";
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
		List<String> categories = Arrays.asList(/*"tv", "software", "camera", "toilets", "shoes", "monitor",
												"sunglasses", "cutlery", "notebook", "headphone"*/ "all");
		try{
			String category = "headphone";
			//camera
//			websites = Arrays.asList("gosale.com",  "price-hunt.com",  "shopping.dealtime.com",  "shopping.com",
//					"hookbag.ca",  "buzzillions.com",  "keenpricefinder.com",  "shopmania.in"/*,  "www.amazon.com",
//					"buy.net",  "digichar.com",  "www.ebay.ca",  "www.ebay.com",  "www.ebay.com.sg",  "livehotdeals.com",  
//					"www.eglobalcentral.co.uk",  "www.aliexpress.com",  "search.tryondailybulletin.com",  "search.greenvilleadvocate.com",
//					"yellowpages.observer-reporter.com",  "www.ebay.co.uk",  "www.trademe.co.nz",  "www.tootoo.com",  "www.ebay.in",
//					"www.alibaba.com",  "www.ebay.ie",  "www.alzashop.com",  "www.equinenow.com",  "all-spares.com",  "www.cdiscount.com",
//					"www.amazon.ca",  "www.exporters.sg",  "www.ebay.ph",  "www.guidechem.com",  "www.lelong.com.my",
//					"www.comprasnoparaguai.com",  "www.chargerfun.com",  "www.productreview.com.au",  "www.bidorbuy.co.za",
//					"uae.souq.com",  "www.camerachums.com",  "www.eafsupplychain.com",  "www.pinnaclemicro.com"
//				    */);
			//headphone
			websites = Arrays.asList("shopping.com",  "shopping.dealtime.com",  "pcpartpicker.com",  "ca.pcpartpicker.com",
					"bhphotovideo.com",  "builderdepot.com",  "ebay.com",  "digichar.com",  /*"gumtree.com.au", */
					"rdexpo.com"/*,  "www.amazon.in",  "www.junglee.com",  "www.alibaba.com",  "www.alzashop.com",  
					"www.direct-sales-online.com",  "www.ebay.co.uk",  "forum.lowyat.net",  "www.cdwg.com",  "www.amazon.co.uk", 
					"www.ebay.in",  "uae.souq.com",  "shopping.indiatimes.com"
					*/);
			//monitor
//			websites = Arrays.asList("pcpartpicker.com",  "ca.pcpartpicker.com",  "shopping.com",  "onlajn.net",  "ebay.com",
//					"ebay.com.au",/*  "keenpricefinder.com",*/  "shop.bt.com"/*,  "www.businessdirect.bt.com", 
//					"makingbuyingeasy.co.uk",  "www.pcs-store.com",  "www.alzashop.com",  "www.ohc24.ch",  "ipon.hu",  
//					"www.okobe.co.uk",  "www.best-deal-items.com",  "www.price.com.hk",  "desktop.wolf-sold.com",  "www.vology.com",
//					"www.macconnection.com",  "www.pcconnection.com",  "www.cleverboxes.com",  "nextag.com",  "www.softwarecity.ca",
//					"www.imldirect.it",  "www.kingsfieldcomputers.co.uk",  "www.itenergy.co.uk",  "www.jrlinton.co.uk",  "www.mosltd.co.uk",
//					"www.datamart.co.uk",  "www.imldirect.com",  "www.alvio.com",  "www.diwo.it",  "www.ruret.com", 
//					"www.cheapofficetoolsonline.com",  "ie.picclick.com",  "www.bechtle.co.uk",  "www.bechtle.ie",  "www.gumtree.com",
//					"forum.lowyat.net",  "www.alibaba.com",  "www.comprasnoparaguai.com",  "www.aliexpress.com"
//				    */);
			//notebook
//			websites = Arrays.asList("isupplyhub.com",  "amazon.com",  "cdw.com",  "buyspry.com",  "hookbag.com",
//					"flexshopper.com",  "orbitdirect.net",  "ebay.com",  "topendelectronic.co.uk",  "ebay.ph"/*,
//					"www.ebay.co.uk",  "www.amazon.co.uk",  "www.cdwg.com",  "www.cdw.ca",  "www.ebay.in",  "www.macconnection.com",
//					"www.tiptopelectronics.com.au",  "digichar.com",  "www.dealmoon.com",  "www.shopyourworld.com",  "www.ebay.ca", 
//					"www.pricequebec.com",  "www.zumthing.com",  "www.bidorbuy.co.za",  "www.alibaba.com",  "www.vology.com"
//				    */);
			//tv with www
//			websites = Arrays.asList("vanvreedes.com",  "brothersmain.com",  "www.wettsteins.com",  "www.jsappliance.com",
//					"www.digiplususa.com",  "www.rewstv.com",  "www.dakotatv.com",  "www.buzzillions.com",  "www.bhphotovideo.com",
//					"www.alltimetvs.com",  "buying-reviews.com",  "www.ebay.com",  "homecinemaboutique.com",  "www.amazon.com", 
//					"www.flexshopper.com",  "www.amazon.ca",  "www.electronicsexpress.com",  "www.electronicexpress.com",  
//					"buy.net",  "www.price-hunt.com",  "www.pricedekho.com",  "www.getprice.com.au",  "www.hookbag.ca", 
//					"www.pflanzzone.com",  "www.iq-av.com",  "pcmicrostore.com",  "www.amazon.co.uk",  "www.alibaba.com",  
//					"www.otto.de",  "www.tootoo.com",  "www.productreview.com.au",  "www.ebay.com.au",  "search.dicksmith.com.au"/*,
//					"www.frys.com",  "livehotdeals.com",  "www.brandsmart.com",  "www.brandsmartusa.com",  "www.digitalmonster.com",
//					"www.onlinestore.it",  "www.tiendaclic.mx",  "www.ebay.ie",  "www.lelong.com.my",  "eg.bkam.com",  
//					"www.c2coffer.com",  "www.china-telecommunications.com",  "www.evomag.ro"
//				    */);
			//tv without www
//			websites = Arrays.asList("vanvreedes.com",  "brothersmain.com",  "wettsteins.com",  "jsappliance.com",  "digiplususa.com",
//					"rewstv.com",  "dakotatv.com",  "buzzillions.com",  "bhphotovideo.com",  "alltimetvs.com"/*,  "buying-reviews.com",
//					"ebay.com",  "homecinemaboutique.com",  "amazon.com",  "flexshopper.com",  "amazon.ca",  "electronicsexpress.com",
//					"electronicexpress.com",  "buy.net",  "price-hunt.com",  "pricedekho.com",  "getprice.com.au",  "hookbag.ca",
//					"pflanzzone.com",  "iq-av.com",  "pcmicrostore.com",  "amazon.co.uk",  "alibaba.com",  "otto.de",  "tootoo.com",
//					"productreview.com.au",  "ebay.com.au",  "search.dicksmith.com.au",  "frys.com",  "livehotdeals.com",  "brandsmart.com",
//					"brandsmartusa.com",  "digitalmonster.com",  "onlinestore.it",  "tiendaclic.mx",  "ebay.ie",  "lelong.com.my",
//					"eg.bkam.com",  "c2coffer.com",  "china-telecommunications.com",  "evomag.ro"
//				    */);
//			Match match = cm.getMatch(websites, "tv", 0);
//			fdc.printMatch("wettsteins(card1)", match.toCSVFormat());
			fdc.printSchema("schema_noMI_"+category, c.matchAllSourcesInCategory(websites, category, cm, 0, useFeature));
		} finally {
			r.stop();          
		}
//		try{
//			String csPath = "/home/marco/workspace/SchemaMerger/src/main/resources/classification/clones.csv";
//////			fdc.printClonedSources("clones", c.findClonedSources(mdbc, categories));
//			Map<String, List<String>> clonedSources = fdc.readClonedSources(csPath);
//			Map<String, List<String>> tSets = c.generateTrainingSets(mdbc, categories, clonedSources);
//			fdc.printAllTrainingSets(tSets);
//		} finally {
//			r.stop();          
//		}
		time =  System.currentTimeMillis() - start;
		System.out.println("Fine match (" + (time/1000) + " s)");
	}
}
