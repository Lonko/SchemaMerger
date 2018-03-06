package launchers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.Document;

import matcher.CategoryMatcher;
import matcher.DynamicCombinationsCalculator;
import matcher.FeatureExtractor;
import matcher.TrainingSetGenerator;
import models.generator.Configurations;
import models.matcher.Schema;
import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import connectors.RConnector;

public class Cohordinator {

    public Cohordinator() {

    }

    // cardinality parameter is currently useless
    public Schema matchAllSourcesInCategory(List<String> orderedWebsites, String category,
            CategoryMatcher cm, int cardinality, boolean useMI, SyntheticDatasetGenerator sdg) {

        Schema schema = new Schema();
        List<String> currentMatchSources = new ArrayList<>();
        currentMatchSources.add(orderedWebsites.get(0));
        // match su tutte le altre sorgenti
        for (int i = 1; i < orderedWebsites.size(); i++) {
            System.out.println("-->" + orderedWebsites.get(i) + "<-- (" + i + ")");
            currentMatchSources.add(orderedWebsites.get(i));
            if (!cm.getMatch(currentMatchSources, category, cardinality, schema, useMI)) {
                System.out.println("NO MATCH");
            }
        }

        return schema;
    }

    /*
     * checks the schemas of the sources in the selected categories and returns
     * the couples of sources with schemas that have an overlap of attribute
     * (based on their names) above 50% of the smallest of the two sources.
     */
    public Map<String, List<String>> findClonedSources(MongoDBConnector mdbc, List<String> categories) {
        Map<String, List<String>> clonedSources = new HashMap<>();
        Map<String, List<String>> sourceSchemas = mdbc.getSchemas(categories);

        for (String source1 : sourceSchemas.keySet()) {
            String category1 = source1.split("###")[0];
            List<String> attributes1 = sourceSchemas.get(source1);
            for (String source2 : sourceSchemas.keySet()) {
                if (source1.equals(source2))
                    continue;
                String category2 = source2.split("###")[0];
                if (!category1.equals(category2))
                    continue;
                List<String> attributes2 = sourceSchemas.get(source2);
                double minSize = (attributes1.size() > attributes2.size()) ? attributes2.size() : attributes1
                        .size();
                Set<String> intersection = new HashSet<>(attributes1);
                intersection.retainAll(attributes2);
                if (intersection.size() >= (minSize / 2)) {
                    List<String> clones = clonedSources.getOrDefault(source1, new ArrayList<>());
                    clones.add(source2);
                    clonedSources.put(source1, clones);
                }
            }
        }

        return clonedSources;
    }

    public Map<String, List<String>> generateTrainingSets(MongoDBConnector mdbc, List<String> categories,
            Map<String, List<String>> clonedSources) {

        TrainingSetGenerator tsg = new TrainingSetGenerator(mdbc, new FeatureExtractor(), clonedSources);
        Map<String, List<String>> trainingSets = new HashMap<>();

        for (String category : categories) {
            System.out.println(category.toUpperCase());
            trainingSets.put(category, tsg.getTrainingSetWithTuples(500, 15000, false, true, 0.25, category));
        }

        return trainingSets;
    }

    public void evaluateSyntheticResults(List<List<String>> clusters,
            Map<String, Integer> expectedClusterSizes) {
        DynamicCombinationsCalculator dcc = new DynamicCombinationsCalculator();
        int truePositives = 0, falsePositives = 0, expectedPositives = 0;
        double p, r, f;

        // calculate expected positives
        for (int clusterSize : expectedClusterSizes.values())
            // cluster of cardinality == 1 are not considered
            if (clusterSize > 1)
                expectedPositives += dcc.calculateCombinations(clusterSize, 2);

        // calculate true and false positives
        for (List<String> cluster : clusters) {
            int size = cluster.size();
            // cluster of cardinality == 1 are not considered
            if (size > 1) {
                Collection<Long> cCollection = cluster.stream().map(attr -> attr.split("###")[0])
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).values();
                List<Long> counters = new ArrayList<>(cCollection);
                // true positives
                truePositives += counters.stream().mapToInt(Long::intValue)
                        .map(c -> dcc.calculateCombinations(c, 2)).sum();
                // false positives
                falsePositives += counters.stream().mapToInt(Long::intValue).map(c -> c * (size - c)).sum();
            }
        }

        p = truePositives / (double) (truePositives + falsePositives);
        r = truePositives / (double) (expectedPositives);
        f = 2 * (p * r) / (p + r);

        System.out.println("PRECISION = " + p);
        System.out.println("RECALL = " + r);
        System.out.println("F-MEASURE = " + f);
    }

    public static void main(String[] args) {
        // Setup
        System.out.println("INIZIO SETUP");
        FileDataConnector fdc = new FileDataConnector();
        Configurations config = new Configurations(fdc.readConfig());
        fdc.setDatasetPath(config.getDatasetPath());
        fdc.setRlPath(config.getRecordLinkagePath());
        fdc.setTsPath(config.getTrainingSetPath());
        MongoDBConnector mdbc = new MongoDBConnector(config.getMongoURI(), config.getDatasetName(), fdc);
        RConnector r = new RConnector();
        Cohordinator c = new Cohordinator();
        System.out.println("FINE SETUP");

        // Synthetic dataset generation
        System.out.println("INIZIO GENERAZIONE DATASET");
        SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(fdc, mdbc, config);
        sdg.generateCatalogue();
        sdg.generateSources();
        System.out.println("FINE GENERAZIONE DATASET");

        // Training / model loading
        boolean alreadyTrained = true;
        r.start();
        List<String> categories = config.getCategories();
        try {
            if (alreadyTrained) {
                System.out.println("LOADING DEL MODEL");
                r.loadModel(config.getModelPath());
                System.out.println("FINE LOADING DEL MODEL");
            } else {
                System.out.println("INIZIO GENERAZIONE TRAINING SET");
                Map<String, List<String>> tSet = c.generateTrainingSets(mdbc, categories,
                        new HashMap<String, List<String>>());
                fdc.printTrainingSet("trainingSet", tSet.get(categories.get(0)));
                System.out.println("FINE GENERAZIONE TRAINING SET - INIZIO TRAINING");
                r.train(config.getTrainingSetPath() + "/trainingSet.csv", config.getModelPath());
                System.out.println("FINE TRAINING");
            }

            // Classification
            System.out.println("INIZIO GENERAZIONE SCHEMA");
            CategoryMatcher cm = new CategoryMatcher(mdbc, r);
            Schema schema = c.matchAllSourcesInCategory(sdg.getSourcesByLinkage(), categories.get(0), cm, 0,
                    true, sdg);
            fdc.printMatchSchema("clusters", schema);
            System.out.println("FINE GENERAZIONE SCHEMA");

            // Result Evaluation
            System.out.println("INIZIO VALUTAZIONE RISULTATI");
            c.evaluateSyntheticResults(schema.schema2Clusters(), sdg.getAttrLinkage());
            System.out.println("FINE VALUTAZIONE RISULTATI");
        } finally {
            r.stop();
        }

    }

    // public static void main(String [] args){
    // boolean useDefaultDB = false;
    // boolean useFeature = true;
    // long start = System.currentTimeMillis();
    // FileDataConnector fdc;
    // long time = System.currentTimeMillis() - start;
    // MongoDBConnector mdbc;
    // time = System.currentTimeMillis() - start;
    // if(useDefaultDB){
    // fdc = new FileDataConnector();
    // mdbc = new MongoDBConnector(fdc);
    // } else {
    // String conf = "src/main/resources/config.properties";
    // String ds = "src/main/resources/filtered_specifications";
    // String rl = "src/main/resources/id2category2urls.json";
    // String ts = "src/main/resources/classification";
    // fdc = new FileDataConnector(ds, rl, ts, conf);
    // mdbc = new MongoDBConnector("mongodb://localhost:27017",
    // "FilteredDataset", fdc);
    // }
    // // String modelPath;
    // // if(useFeature)
    // // modelPath =
    // "/home/marco/workspace/SchemaMerger/src/main/resources/classification/modelN.rda";
    // // else
    // // modelPath =
    // "/home/marco/workspace/SchemaMerger/src/main/resources/classification/modelN_noC.rda";
    //
    // String modelPath =
    // "/home/marco/workspace/SchemaMerger/src/main/resources/classification/"+
    // "modelClassifier.rda";
    // String tsPath =
    // "/home/marco/workspace/SchemaMerger/src/main/resources/classification/"+
    // "1all_ts.csv";
    // RConnector r = new RConnector();
    // r.start();
    // // r.loadModel(modelPath);
    // time = System.currentTimeMillis() - start;
    // System.out.println("Started RConnector (" + (time/1000) + " s)");
    // CategoryMatcher cm = new CategoryMatcher(mdbc, r);
    // time = System.currentTimeMillis() - start;
    // System.out.println("Created CategoryMatcher (" + (time/1000) + " s)");
    // // fdc.printTrainingSet("tsWithTuples", cm.getTrainingSetWithTuples(1000,
    // 14000, true, 1.0));
    // List<String> websites = new ArrayList<>();
    // Cohordinator c = new Cohordinator();
    // time = System.currentTimeMillis() - start;
    // List<String> categories = Arrays.asList(/*"tv", "software", "camera",
    // "toilets", "shoes", "monitor",
    // "sunglasses", "cutlery", "notebook", "headphone"*/ "all");
    // try{
    // String category = "headphone";
    // //camera
    // // websites = Arrays.asList("gosale.com", "price-hunt.com",
    // "shopping.dealtime.com", "shopping.com",
    // // "hookbag.ca", "buzzillions.com", "keenpricefinder.com",
    // "shopmania.in"/*, "www.amazon.com",
    // // "buy.net", "digichar.com", "www.ebay.ca", "www.ebay.com",
    // "www.ebay.com.sg", "livehotdeals.com",
    // // "www.eglobalcentral.co.uk", "www.aliexpress.com",
    // "search.tryondailybulletin.com", "search.greenvilleadvocate.com",
    // // "yellowpages.observer-reporter.com", "www.ebay.co.uk",
    // "www.trademe.co.nz", "www.tootoo.com", "www.ebay.in",
    // // "www.alibaba.com", "www.ebay.ie", "www.alzashop.com",
    // "www.equinenow.com", "all-spares.com", "www.cdiscount.com",
    // // "www.amazon.ca", "www.exporters.sg", "www.ebay.ph",
    // "www.guidechem.com", "www.lelong.com.my",
    // // "www.comprasnoparaguai.com", "www.chargerfun.com",
    // "www.productreview.com.au", "www.bidorbuy.co.za",
    // // "uae.souq.com", "www.camerachums.com", "www.eafsupplychain.com",
    // "www.pinnaclemicro.com"
    // // */);
    // //headphone
    // websites = Arrays.asList("shopping.com", "shopping.dealtime.com",
    // "pcpartpicker.com", "ca.pcpartpicker.com",
    // "bhphotovideo.com", "builderdepot.com", "ebay.com", "digichar.com",
    // /*"gumtree.com.au", */
    // "rdexpo.com"/*, "www.amazon.in", "www.junglee.com", "www.alibaba.com",
    // "www.alzashop.com",
    // "www.direct-sales-online.com", "www.ebay.co.uk", "forum.lowyat.net",
    // "www.cdwg.com", "www.amazon.co.uk",
    // "www.ebay.in", "uae.souq.com", "shopping.indiatimes.com"
    // */);
    // //monitor
    // // websites = Arrays.asList("pcpartpicker.com", "ca.pcpartpicker.com",
    // "shopping.com", "onlajn.net", "ebay.com",
    // // "ebay.com.au",/* "keenpricefinder.com",*/ "shop.bt.com"/*,
    // "www.businessdirect.bt.com",
    // // "makingbuyingeasy.co.uk", "www.pcs-store.com", "www.alzashop.com",
    // "www.ohc24.ch", "ipon.hu",
    // // "www.okobe.co.uk", "www.best-deal-items.com", "www.price.com.hk",
    // "desktop.wolf-sold.com", "www.vology.com",
    // // "www.macconnection.com", "www.pcconnection.com",
    // "www.cleverboxes.com", "nextag.com", "www.softwarecity.ca",
    // // "www.imldirect.it", "www.kingsfieldcomputers.co.uk",
    // "www.itenergy.co.uk", "www.jrlinton.co.uk", "www.mosltd.co.uk",
    // // "www.datamart.co.uk", "www.imldirect.com", "www.alvio.com",
    // "www.diwo.it", "www.ruret.com",
    // // "www.cheapofficetoolsonline.com", "ie.picclick.com",
    // "www.bechtle.co.uk", "www.bechtle.ie", "www.gumtree.com",
    // // "forum.lowyat.net", "www.alibaba.com", "www.comprasnoparaguai.com",
    // "www.aliexpress.com"
    // // */);
    // //notebook
    // // websites = Arrays.asList("isupplyhub.com", "amazon.com", "cdw.com",
    // "buyspry.com", "hookbag.com",
    // // "flexshopper.com", "orbitdirect.net", "ebay.com",
    // "topendelectronic.co.uk", "ebay.ph"/*,
    // // "www.ebay.co.uk", "www.amazon.co.uk", "www.cdwg.com", "www.cdw.ca",
    // "www.ebay.in", "www.macconnection.com",
    // // "www.tiptopelectronics.com.au", "digichar.com", "www.dealmoon.com",
    // "www.shopyourworld.com", "www.ebay.ca",
    // // "www.pricequebec.com", "www.zumthing.com", "www.bidorbuy.co.za",
    // "www.alibaba.com", "www.vology.com"
    // // */);
    // //tv with www
    // // websites = Arrays.asList("vanvreedes.com", "brothersmain.com",
    // "www.wettsteins.com", "www.jsappliance.com",
    // // "www.digiplususa.com", "www.rewstv.com", "www.dakotatv.com",
    // "www.buzzillions.com", "www.bhphotovideo.com",
    // // "www.alltimetvs.com", "buying-reviews.com", "www.ebay.com",
    // "homecinemaboutique.com", "www.amazon.com",
    // // "www.flexshopper.com", "www.amazon.ca", "www.electronicsexpress.com",
    // "www.electronicexpress.com",
    // // "buy.net", "www.price-hunt.com", "www.pricedekho.com",
    // "www.getprice.com.au", "www.hookbag.ca",
    // // "www.pflanzzone.com", "www.iq-av.com", "pcmicrostore.com",
    // "www.amazon.co.uk", "www.alibaba.com",
    // // "www.otto.de", "www.tootoo.com", "www.productreview.com.au",
    // "www.ebay.com.au", "search.dicksmith.com.au"/*,
    // // "www.frys.com", "livehotdeals.com", "www.brandsmart.com",
    // "www.brandsmartusa.com", "www.digitalmonster.com",
    // // "www.onlinestore.it", "www.tiendaclic.mx", "www.ebay.ie",
    // "www.lelong.com.my", "eg.bkam.com",
    // // "www.c2coffer.com", "www.china-telecommunications.com",
    // "www.evomag.ro"
    // // */);
    // //tv without www
    // // websites = Arrays.asList("vanvreedes.com", "brothersmain.com",
    // "wettsteins.com", "jsappliance.com", "digiplususa.com",
    // // "rewstv.com", "dakotatv.com", "buzzillions.com", "bhphotovideo.com",
    // "alltimetvs.com"/*, "buying-reviews.com",
    // // "ebay.com", "homecinemaboutique.com", "amazon.com", "flexshopper.com",
    // "amazon.ca", "electronicsexpress.com",
    // // "electronicexpress.com", "buy.net", "price-hunt.com",
    // "pricedekho.com", "getprice.com.au", "hookbag.ca",
    // // "pflanzzone.com", "iq-av.com", "pcmicrostore.com", "amazon.co.uk",
    // "alibaba.com", "otto.de", "tootoo.com",
    // // "productreview.com.au", "ebay.com.au", "search.dicksmith.com.au",
    // "frys.com", "livehotdeals.com", "brandsmart.com",
    // // "brandsmartusa.com", "digitalmonster.com", "onlinestore.it",
    // "tiendaclic.mx", "ebay.ie", "lelong.com.my",
    // // "eg.bkam.com", "c2coffer.com", "china-telecommunications.com",
    // "evomag.ro"
    // // */);
    // // Match match = cm.getMatch(websites, "tv", 0);
    // // fdc.printMatch("wettsteins(card1)", match.toCSVFormat());
    // // fdc.printMatchSchema("schema_noMI_"+category,
    // c.matchAllSourcesInCategory(websites, category, cm, 0, useFeature));
    // r.train(tsPath, modelPath);
    // } finally {
    // r.stop();
    // }
    // // try{
    // // String csPath =
    // "/home/marco/workspace/SchemaMerger/src/main/resources/classification/clones.csv";
    // ////// fdc.printClonedSources("clones", c.findClonedSources(mdbc,
    // categories));
    // // Map<String, List<String>> clonedSources =
    // fdc.readClonedSources(csPath);
    // // Map<String, List<String>> tSets = c.generateTrainingSets(mdbc,
    // categories, clonedSources);
    // // fdc.printAllTrainingSets(tSets);
    // // } finally {
    // // r.stop();
    // // }
    // time = System.currentTimeMillis() - start;
    // System.out.println("Fine match (" + (time/1000) + " s)");
    // }
}
