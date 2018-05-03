package connectors.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;

import connectors.MongoDbConnectionFactory;
import connectors.MongoDbUtils;
import model.CatalogueProductPage;
import model.SourceProductPage;

/**
 * Convert catalogue data to Mongo
 * @author federico
 *
 */
public class MongoSyntheticDao implements SyntheticDatasetDao {
	
	private static final String SCHEMAS_COLLECTION = "Schemas";
	private static final String PRODUCTS_COLLECTION_NAME = "Products";
	private static final String CATALOGUE_COLLECTION_NAME = "Catalogue";
	
	private MongoDatabase database;
		
    public MongoSyntheticDao(MongoDbConnectionFactory mongoConnector) {
		super();
		this.database = mongoConnector.getDatabase();
	}

	private static final int BATCH_SIZE = 100;

    
	@Override
    public void uploadCatalogue(List<CatalogueProductPage> catalogue, boolean delete) {
        if(delete)
        	this.database.getCollection(CATALOGUE_COLLECTION_NAME).drop();
        
        int uploadedProds = 0;

        //upload Catalogue to MongoDB in batches
        // each iteration is a batch of products to upload
        while (uploadedProds != catalogue.size()) {
            int size = (catalogue.size() - uploadedProds > BATCH_SIZE) ? BATCH_SIZE : catalogue.size()
                    - uploadedProds;
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int id = uploadedProds + i;
                Document doc = convertCataloguePageToDocument(catalogue.get(id));
                batch.add(doc);
            }
            MongoDbUtils.insertBatch(this.database, batch, CATALOGUE_COLLECTION_NAME);
            uploadedProds += size;
        }
    }

	private Document convertCataloguePageToDocument(CatalogueProductPage productPage) {
        Document prod = new Document();
        Document specs = new Document();
        prod.append("id", productPage.getId());
        prod.append("category", productPage.getCategory());
        for (Entry<String, String> attributeEntry: productPage.getSpecifications().entrySet()) {
        	specs.append(attributeEntry.getKey(), attributeEntry.getValue());
        }
        prod.append("specs", specs);
        return prod;
	}
	
	private CatalogueProductPage convertDocumentToCataloguePage(Document doc) {
		CatalogueProductPage cpp = new CatalogueProductPage(doc.getInteger("id"), doc.getString("category"));
		Document specs = doc.get("specs", Document.class);
		for (Entry<String, Object> entry: specs.entrySet()) {
			cpp.addAttributeValue(entry.getKey(), entry.getValue().toString());
		}
        return cpp;
	}
	
	@Override
    public void uploadSource(List<SourceProductPage> productPages) {
        int uploadedProds = 0;

        // each iteration is a batch of product pages to upload
        while (uploadedProds != productPages.size()) {
            int size = (productPages.size() - uploadedProds > BATCH_SIZE) ? BATCH_SIZE : productPages.size()
                    - uploadedProds;
            List<Document> batch = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                int index = uploadedProds + i;
                Document docToUpload = convertProductPageToDocument(productPages.get(index));
                batch.add(docToUpload);
            }
            this.insertBatch(batch, PRODUCTS_COLLECTION_NAME);
            uploadedProds += size;
        }
    }
	
    // generates the products' pages for the source
    private Document convertProductPageToDocument(SourceProductPage sourceProductPage) {
        Document page = new Document();
        page.append("category", sourceProductPage.getCategory());
        page.append("url", sourceProductPage.getUrl());
        page.append("spec", sourceProductPage.getSpecifications());
        page.append("linkage", sourceProductPage.getLinkage());
        page.append("ids", sourceProductPage.getIds());
        page.append("website", sourceProductPage.getWebsite());
        return page;
	}

	@Override
	public void deleteAllSourceProductPages() {
		dropCollection(PRODUCTS_COLLECTION_NAME);
	}

	@Override
	public List<CatalogueProductPage> getCatalogueProductsWithIds(List<Integer> ids) {
        List<CatalogueProductPage> products = new ArrayList<>();
        MongoCollection<Document> collection = this.database.getCollection("Catalogue");
        collection.find(Filters.in("id", ids)).forEach((Document d) -> products.add(convertDocumentToCataloguePage(d)));
		return products;
	}

	@Override
	public void finalizeSourceUpload() {
		MongoCollection<Document> productCollection = this.database.getCollection(PRODUCTS_COLLECTION_NAME);
		addProductsIndexes(productCollection);
        dropCollection(SCHEMAS_COLLECTION);
        initializeSchemaCollection();
	}
	
	
	// METHODS TO INITIALIZE COLLECTION.
	// TODO Note that there is a common part above, but it is tricky to factorize
	
	private void initializeSchemaCollection() {
		MongoCollection<Document> schemaCollection = getCollectionWithWriteConcern(SCHEMAS_COLLECTION);
		if (schemaCollection.count() == 0) {
	        MongoCollection<Document> products = this.database.getCollection("Products");
	        Map<String, Set<String>> schemas = new TreeMap<>();

	        products.find().projection(Projections.fields(Projections.include("spec", "website", "category")))
	                .forEach((Document d) -> loadSchema(d, schemas));

	        schemas.entrySet().stream().map(this::schema2Document).forEach(schemaCollection::insertOne);

	        //TODO factorize with addProductsIndexes? just URL more
	        schemaCollection.createIndex(Indexes.ascending("category"));
	        schemaCollection.createIndex(Indexes.ascending("website"));
	        schemaCollection.createIndex(Indexes.ascending("website", "category"));			
		}
	}
	
    private void addProductsIndexes(MongoCollection<Document> collection) {
        collection.createIndex(Indexes.ascending("category"));
        collection.createIndex(Indexes.ascending("website"));
        collection.createIndex(Indexes.ascending("website", "category"));
        collection.createIndex(Indexes.hashed("url"));
    }
    
    private void dropCollection(String name) {
    	this.database.getCollection(name).drop();
    }
    
    private MongoCollection<Document> getCollectionWithWriteConcern(String collectionName){
    	return this.database.getCollection(collectionName)
                .withWriteConcern(WriteConcern.JOURNALED);
    }
    
    private void loadSchema(Document d, Map<String, Set<String>> schemas) {
        String source = d.getString("website") + "___" + d.getString("category");
        Document spec = d.get("spec", Document.class);
        Set<String> attributes = spec.keySet();

        Set<String> schema = schemas.getOrDefault(source, new TreeSet<String>());
        schema.addAll(attributes);
        schemas.put(source, schema);
    }
    
    private Document schema2Document(Map.Entry<String, Set<String>> sourceSchema) {
        String[] key = sourceSchema.getKey().split("___");
        String website = key[0];
        String category = key[1];

        return new Document("category", category).append("website", website).append("attributes",
                sourceSchema.getValue());
    }

    private void insertBatch(List<Document> docs, String collectionName) {
        MongoCollection<Document> collection = this.database.getCollection(collectionName).withWriteConcern(
                WriteConcern.JOURNALED);
        collection.insertMany(docs);
    }
    
}
