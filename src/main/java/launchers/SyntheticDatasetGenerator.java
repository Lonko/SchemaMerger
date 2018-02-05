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
	
	private	FileDataConnector fdc;
	private Configurations conf;
	private MongoDBConnector mdbc;
	private StringGenerator sg;
	private CurveFunction sizes;
	private CurveFunction prodLinkage;
	private Map<String, String> attrFixedTokens;
	private Map<String, List<String>> attrValues;

	public SyntheticDatasetGenerator(){
		this.fdc = new FileDataConnector();
		this.conf = new Configurations(fdc.readConfig()); 
		this.mdbc = new MongoDBConnector("mongodb://localhost:27017", "SyntheticDataset", this.fdc);
		String path = conf.getStringPathFile();
		if(path.equals(""))
			this.sg = new RandomStringGenerator(20, 15, 7);
		else 
			this.sg = new DictionaryStringGenerator(path);
	}	
	
	//upload Catalogue to MongoDB in batches
	private void uploadCatalogue(List<Document> catalogue){ 
		this.mdbc.dropCollection("Catalogue");
		int batchSize = 20, uploadedProds = 0;		
		
		//each iteration is a batch of products to upload
		while(uploadedProds != catalogue.size()){
			int size = (catalogue.size() - uploadedProds > batchSize) ? 
								batchSize : 
								catalogue.size() - uploadedProds;
			List<Document> batch = new ArrayList<>();
			for(int i = 0; i < size; i++){
				int id = uploadedProds+i;
				batch.add(catalogue.get(id));
			}
			this.mdbc.insertBatch(batch, "Catalogue");
			uploadedProds += size;
		}
	}

	//generate and upload catalogue
	public void generateCatalogue(){
		CatalogueGenerator cg = new CatalogueGenerator(this.conf, this.sg);
		List<Document> catalogue = cg.createCatalogue();
		uploadCatalogue(catalogue);
		this.sizes = cg.getSizeCurve();
		this.prodLinkage = cg.getProductLinkageCurve();
		this.attrFixedTokens = cg.getAttrFixedToken();
		this.attrValues = cg.getAttrValues();
	}
	
	//generate and upload sources
	public void generateSources(){
		SourcesGenerator sg = new SourcesGenerator(this.mdbc, this.conf, this.sg, this.sizes, 
													this.prodLinkage, this.attrFixedTokens, this.attrValues);
		Map<Integer, Document> catalogue = this.mdbc.getCatalogue();
		sg.createSources(catalogue);
	}
	
	public int getCatalogueSize(){
		return this.prodLinkage.getYValues().length;
	}
	
	public int getDatasetSize(){
		return this.prodLinkage.getSampling();
	}
	
	public static void main(String[] args){
		SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator();
		
		long start = System.currentTimeMillis();
		sdg.generateCatalogue();
		long middle = System.currentTimeMillis();
		sdg.generateSources();
		long end = System.currentTimeMillis();
		long timeForCatalogue = middle - start;
		long timeForDataset = end - middle;
		long totalTime = end - start;
		
		System.out.println("Prodotti nel catalogo: "+sdg.getCatalogueSize());
		System.out.println("Prodotti nel dataset: "+sdg.getDatasetSize());
		System.out.println("Tempo di generazione del Catalogo: "+(timeForCatalogue/1000)+"s");
		System.out.println("Tempo di generazione del Dataset: "+(timeForDataset/1000)+"s");
		System.out.println("Tempo di esecuzione totale: "+(totalTime/1000)+"s");
		
	}
}
