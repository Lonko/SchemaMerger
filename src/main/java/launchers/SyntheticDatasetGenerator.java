package launchers;

import generator.CatalogueGenerator;
import generator.DictionaryStringGenerator;
import generator.RandomStringGenerator;
import generator.SourcesGenerator;
import generator.StringGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.bson.Document;

import com.sun.istack.internal.logging.Logger;

import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import models.generator.Configurations;
import models.generator.CurveFunction;
import models.generator.LaunchConfiguration;

public class SyntheticDatasetGenerator {

    private static final int SOURCE_NAME_LENGTH = 15;
    private static final int ATTRIBUTE_NAME_LENGTH = 7;
    private static final int TOKEN_LENGTH = 4;
    private static final int BATCH_SIZE = 100;
    private FileDataConnector fdc;
    private Configurations conf;
    private MongoDBConnector mdbc;
    private StringGenerator sg;
    private CurveFunction sizes;
    private CurveFunction prodLinkage;
    private Map<String, String> attrFixedTokens;
    private Map<String, List<String>> attrValues;
    private List<String> sourcesBySize;
    private Map<String, Integer> attrLinkage;
    
    private static final Logger log = Logger.getLogger(SyntheticDatasetGenerator.class);

    public SyntheticDatasetGenerator(LaunchConfiguration lc) {
        this.fdc = new FileDataConnector(lc.getConfigFile());
        this.conf = new Configurations(fdc.readConfig());
        this.mdbc = new MongoDBConnector(conf.getMongoURI(), conf.getDatabaseName(), this.fdc);
        String path = conf.getStringPathFile();
        if (path.equals(""))
            this.sg = new RandomStringGenerator(SOURCE_NAME_LENGTH, ATTRIBUTE_NAME_LENGTH, TOKEN_LENGTH);
        else
            this.sg = new DictionaryStringGenerator(path);
    }

    // upload Catalogue to MongoDB in batches
    private void uploadCatalogue(List<Document> catalogue, boolean delete) {
        if(delete)
            this.mdbc.dropCollection("Catalogue");
        
        int uploadedProds = 0;

        // each iteration is a batch of products to upload
        while (uploadedProds != catalogue.size()) {
            int size = (catalogue.size() - uploadedProds > BATCH_SIZE) ? BATCH_SIZE : catalogue.size()
                    - uploadedProds;
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int id = uploadedProds + i;
                batch.add(catalogue.get(id));
            }
            this.mdbc.insertBatch(batch, "Catalogue");
            uploadedProds += size;
        }
    }

    // generate and upload catalogue
    private void generateCatalogue(boolean delete) {
        CatalogueGenerator cg = new CatalogueGenerator(this.conf, this.sg);
        List<Document> catalogue = cg.createCatalogue();
        uploadCatalogue(catalogue, delete);
        this.sizes = cg.getSizeCurve();
        this.prodLinkage = cg.getProductLinkageCurve();
        this.attrFixedTokens = cg.getAttrFixedToken();
        this.attrValues = cg.getAttrValues();
    }

    // generate and upload sources
    private void generateSources(boolean delete) {
        SourcesGenerator sg = new SourcesGenerator(this.mdbc, this.conf, this.sg, this.sizes,
                this.prodLinkage, this.attrFixedTokens, this.attrValues);
        this.sourcesBySize = sg.prepareSources();
        this.attrLinkage = sg.createSources(this.sourcesBySize, delete);
    }

    private int getCatalogueSize() {
        return this.prodLinkage.getYValues().length;
    }

    private int getDatasetSize() {
        return this.prodLinkage.getSampling();
    }

    public Map<String, Integer> getAttrLinkage() {
        return this.attrLinkage;
    }
    
    /**
     * Launch full generation of catalog, log time and some details
     * @param delete
     */
    public void launchGeneration(boolean delete) {
    	log.info("Generate catalogue...");
        long start = System.currentTimeMillis();
        generateCatalogue(delete);
        long middle = System.currentTimeMillis();
        log.info("Generate sources...");
        generateSources(delete);
        long end = System.currentTimeMillis();
        
        long timeForCatalogue = middle - start;
        long timeForDataset = end - middle;
        long catalogueTime = timeForCatalogue / 1000;
        long datasetTime = timeForDataset / 1000;
        long totalTime = (end - start) / 1000;
        

        log.info("Prodotti nel catalogo: " + getCatalogueSize());
        log.info("Prodotti nel dataset: " + getDatasetSize());
        log.info("Tempo di generazione del Catalogo: " + (catalogueTime / 60) + " min "
                + (catalogueTime % 60) + " sec");
        log.info("Tempo di generazione del Dataset: " + (datasetTime / 60) + " min "
                + (datasetTime % 60) + " s ");
        log.info("Tempo di esecuzione totale: " + (totalTime / 60) + " min " + (totalTime % 60)
                + " s ");
    }

    public static void main(String[] args) {
        LaunchConfiguration lc = LaunchConfiguration.getConfigurationFromArgs(args);
        System.out.println("DELETE EXISTING DATASET? (Y/N)");
        boolean delete = false;
        try (Scanner scanner = new Scanner(System.in)){
        	delete = Character.toLowerCase(scanner.next().charAt(0)) == 'y';
        }
        SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator(lc);
        sdg.launchGeneration(delete);
    }
}
