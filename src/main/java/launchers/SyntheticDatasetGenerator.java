package launchers;

import generator.CatalogueGenerator;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import connectors.FileDataConnector;
import connectors.MongoDBConnector;
import models.generator.Configurations;

public class SyntheticDatasetGenerator {

	public SyntheticDatasetGenerator(){
		
	}	
	
	//generates all products and adds them to the catalogue
	private void uploadCatalogue(List<Document> catalogue, FileDataConnector fdc){ 
		MongoDBConnector mdbc = new MongoDBConnector("mongodb://localhost:27017", "SyntheticDataset", fdc);
		mdbc.dropCollection("Catalogue");
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
			mdbc.insertBatch(batch, "Catalogue");
			updatedProds += size;
		}
	}
	
	public static void main(String[] args){
		SyntheticDatasetGenerator sdg = new SyntheticDatasetGenerator();
		FileDataConnector fdc = new FileDataConnector();
		Configurations conf = new Configurations(fdc.readConfig());
		CatalogueGenerator cg = new CatalogueGenerator(conf);
		
		//generate and upload catalogue
		List<Document> catalogue = cg.createCatalogue();
		sdg.uploadCatalogue(catalogue, fdc);
		
	}
}
