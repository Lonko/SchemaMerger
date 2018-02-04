package launchers;

import generator.CatalogueGenerator;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import models.generator.Configurations;

public class SyntheticDatasetGenerator {
	
	private	FileDataConnector fdc;
	private Configurations conf;
	private MongoDBConnector mdbc;

	public SyntheticDatasetGenerator(){
		this.fdc = new FileDataConnector();
		this.conf = new Configurations(fdc.readConfig()); 
		this.mdbc = new MongoDBConnector("mongodb://localhost:27017", "SyntheticDataset", this.fdc);
	}	
	
	//upload Catalogue to MongoDB in batches
	private void uploadCatalogue(List<Document> catalogue){ 
		this.mdbc.dropCollection("Catalogue");
		int batchSize = 20, updatedProds = 0;		
		
		//each iteration is a batch of products to upload
		while(updatedProds != catalogue.size()){
			int size = (catalogue.size() - updatedProds > batchSize) ? 
								batchSize : 
								catalogue.size() - updatedProds;
			List<Document> batch = new ArrayList<>();
			for(int i = 0; i < size; i++){
				int id = updatedProds+i;
				batch.add(catalogue.get(id));
			}
			this.mdbc.insertBatch(batch, "Catalogue");
			updatedProds += size;
		}
	}

	//generate and upload catalogue
	public void generateCatalogue(){
		CatalogueGenerator cg = new CatalogueGenerator(this.conf);
		List<Document> catalogue = cg.createCatalogue();
		uploadCatalogue(catalogue);
	}
	
	public void generateSources(){
		
	}
	
	
	
	public static void main(String[] args){
		SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator();
		sdg.generateCatalogue();
		sdg.generateSources();
	}
}
