package launchers;

import generator.CatalogueGenerator;
import generator.DictionaryStringGenerator;
import generator.RandomStringGenerator;
import generator.SourcesGenerator;
import generator.StringGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import models.generator.Configurations;
import models.generator.CurveFunction;

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
    private List<String> sourcesByLinkage;
    private Map<String, Integer> attrLinkage;

    public SyntheticDatasetGenerator() {
        this.fdc = new FileDataConnector();
        this.conf = new Configurations(fdc.readConfig());
        this.mdbc = new MongoDBConnector(conf.getMongoURI(), conf.getDatabaseName(), this.fdc);
        String path = conf.getStringPathFile();
        if (path.equals(""))
            this.sg = new RandomStringGenerator(SOURCE_NAME_LENGTH, ATTRIBUTE_NAME_LENGTH, TOKEN_LENGTH);
        else
            this.sg = new DictionaryStringGenerator(path);
    }

    public SyntheticDatasetGenerator(FileDataConnector fdc, MongoDBConnector mdbc, Configurations conf) {
        this.fdc = fdc;
        this.conf = conf;
        this.mdbc = mdbc;
        String path = conf.getStringPathFile();
        if (path.equals(""))
            this.sg = new RandomStringGenerator(SOURCE_NAME_LENGTH, ATTRIBUTE_NAME_LENGTH, TOKEN_LENGTH);
        else
            this.sg = new DictionaryStringGenerator(path);
    }

    // upload Catalogue to MongoDB in batches
    private void uploadCatalogue(List<Document> catalogue) {
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
    public void generateCatalogue() {
        CatalogueGenerator cg = new CatalogueGenerator(this.conf, this.sg);
        List<Document> catalogue = cg.createCatalogue();
        uploadCatalogue(catalogue);
        this.sizes = cg.getSizeCurve();
        this.prodLinkage = cg.getProductLinkageCurve();
        this.attrFixedTokens = cg.getAttrFixedToken();
        this.attrValues = cg.getAttrValues();
    }

    // generate and upload sources
    public void generateSources() {
        SourcesGenerator sg = new SourcesGenerator(this.mdbc, this.conf, this.sg, this.sizes,
                this.prodLinkage, this.attrFixedTokens, this.attrValues);
        this.sourcesBySize = sg.prepareSources();
        this.attrLinkage = sg.createSources(this.sourcesBySize);
        this.sourcesByLinkage = sg.getLinkageOrder(this.sourcesBySize);
    }

    public int getCatalogueSize() {
        return this.prodLinkage.getYValues().length;
    }

    public int getDatasetSize() {
        return this.prodLinkage.getSampling();
    }

    public int getSourceSize(String name) {
        int i = this.sourcesBySize.indexOf(name);
        return this.sizes.getYValues()[i];
    }

    public List<String> getSourcesBySize() {
        return this.sourcesBySize;
    }

    public List<String> getSourcesByLinkage() {
        return this.sourcesByLinkage;
    }

    public Map<String, Integer> getAttrLinkage() {
        return this.attrLinkage;
    }

    public static void main(String[] args) {
        SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator();

        long start = System.currentTimeMillis();
        sdg.generateCatalogue();
        long middle = System.currentTimeMillis();
        sdg.generateSources();
        long end = System.currentTimeMillis();
        long timeForCatalogue = middle - start;
        long timeForDataset = end - middle;
        long catalogueTime = timeForCatalogue / 1000;
        long datasetTime = timeForDataset / 1000;
        long totalTime = (end - start) / 1000;

        System.out.println("Prodotti nel catalogo: " + sdg.getCatalogueSize());
        System.out.println("Prodotti nel dataset: " + sdg.getDatasetSize());
        System.out.println("Tempo di generazione del Catalogo: " + (catalogueTime / 60) + " min "
                + (catalogueTime % 60) + " sec");
        System.out.println("Tempo di generazione del Dataset: " + (datasetTime / 60) + " min "
                + (datasetTime % 60) + " s ");
        System.out.println("Tempo di esecuzione totale: " + (totalTime / 60) + " min " + (totalTime % 60)
                + " s ");

    }
}
