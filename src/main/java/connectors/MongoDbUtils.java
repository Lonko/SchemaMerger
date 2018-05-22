package connectors;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import model.AbstractProductPage;
import model.CatalogueProductPage;
import model.SourceProductPage;

/**
 * Common methods for DAO
 * 
 * @author federico
 *
 */
public class MongoDbUtils {

	// field names
	public static final String WEBSITE = "website";
	public static final String IDS = "ids";
	public static final String ID = "id";
	public static final String LINKAGE = "linkage";
	public static final String SPECS = "specs";
	public static final String URL = "url";
	public static final String CATEGORY = "category";
	public static final String ATTRIBUTES = "attributes";

	public static final String SCHEMAS_COLLECTION = "Schemas";
	public static final String PRODUCTS_COLLECTION_NAME = "Products";
	public static final String CATALOGUE_COLLECTION_NAME = "Catalogue";

	public static void insertBatch(MongoDatabase db, List<Document> docs, String collectionName) {
		MongoCollection<Document> collection = db.getCollection(collectionName)
				.withWriteConcern(WriteConcern.JOURNALED);
		collection.insertMany(docs);
	}

	public static CatalogueProductPage convertDocumentToCataloguePage(Document doc) {
		CatalogueProductPage cpp = new CatalogueProductPage(doc.getInteger(ID), doc.getString(CATEGORY));
		addSpecsToDocument(doc, cpp);
		return cpp;
	}

	private static void addSpecsToDocument(Document doc, AbstractProductPage app) {
		Document specs = doc.get(SPECS, Document.class);
		for (Entry<String, Object> entry : specs.entrySet()) {
			app.addAttributeValue(entry.getKey(), entry.getValue().toString());
		}
	}

	/**
	 * Generates the products' pages for the source
	 * 
	 * @param sourceProductPage
	 * @return
	 */
	public static Document convertProductPageToDocument(SourceProductPage sourceProductPage) {
		Document page = new Document();
		page.append(CATEGORY, sourceProductPage.getSource().getCategory());
		page.append(URL, sourceProductPage.getUrl());
		page.append(SPECS, sourceProductPage.getSpecifications());
		page.append(LINKAGE, sourceProductPage.getLinkage());
		page.append(IDS, sourceProductPage.getIds());
		page.append(WEBSITE, sourceProductPage.getSource().getWebsite());
		return page;
	}

	@SuppressWarnings("unchecked")
	public static SourceProductPage convertDocumentToProductPage(Document doc) {
		SourceProductPage page = docToProductPageHelper(doc);
		page.setLinkage(doc.get(LINKAGE, List.class));
		return page;
	}

	public static SourceProductPage convertUnwindedDocumentToProductPage(Document doc) {
		SourceProductPage page = docToProductPageHelper(doc);
		page.setLinkage(Arrays.asList(doc.getString(LINKAGE)));
		return page;
	}

	@SuppressWarnings("unchecked")
	private static SourceProductPage docToProductPageHelper(Document doc) {
		SourceProductPage page = new SourceProductPage(doc.getString(CATEGORY), doc.getString(URL),
				doc.getString(WEBSITE));
		addSpecsToDocument(doc, page);
		page.setIds(doc.get(IDS, List.class));
		return page;
	}
}
