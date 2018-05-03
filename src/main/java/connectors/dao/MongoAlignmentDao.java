package connectors.dao;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import connectors.MongoDbConnectionFactory;

public class MongoAlignmentDao implements AlignmentDao {

	private MongoDatabase database;
	
    public MongoAlignmentDao(MongoDbConnectionFactory mongoConnector) {
		super();
		this.database = mongoConnector.getDatabase();
	}

    @Override
    public List<Document> getRLSample(int size, String category) {
    	// uses sample method of MongoDB
        MongoCollection<Document> collection = this.database.getCollection("Products");
        List<Document> sample = new ArrayList<>();
        Bson eqFilter = Filters.eq("category", category);
        Bson neFilterS = Filters.ne("spec", new Document());
        Bson neFilterL = Filters.ne("linkage", Collections.EMPTY_LIST);
        Bson andFilter;
        if (category.equals("all"))
            andFilter = Filters.and(neFilterS, neFilterL);
        else
            andFilter = Filters.and(neFilterS, neFilterL, eqFilter);
        Bson sampleBson = Aggregates.sample(size);
        Bson matchBson = Aggregates.match(andFilter);
        collection.aggregate(Arrays.asList(matchBson, sampleBson)).forEach((Document d) -> sample.add(d));

        return sample;
    }
    
    @Override
    public Map<String, List<String>> getSchemas(List<String> categories) {
        Map<String, List<String>> fetchedSchemas = new TreeMap<>();

        this.database
                .getCollection("Schemas")
                .find(Filters.and(Filters.in("category", categories),
                        Filters.ne("attributes", Collections.EMPTY_LIST))).forEach((Document d) -> {
                    String website = d.getString("website");
                    String category = d.getString("category");
                    @SuppressWarnings("unchecked")
                    List<String> attributes = d.get("attributes", List.class);
                    fetchedSchemas.put(category + "###" + website, attributes);
                });

        return fetchedSchemas;
    }
    
    @Override
    public List<String> getSingleSchema(String category, String website){
        Bson wFilter = Filters.eq("website", website);
        Bson cFilter = Filters.eq("category", category);
        Bson andFilter = Filters.and(wFilter, cFilter);

        @SuppressWarnings("unchecked")
        List<String> fetchedSchema = this.database.getCollection("Schemas").find(andFilter).first()
                                                  .get("attributes", List.class);
        
        return fetchedSchema;
    }
    
    //TODO extract some parts
 	@Override
 	public Map<Document, List<Document>> getProdsInRL(List<String> websites, String category) {
 		Map<Document, List<Document>> rlMap = new HashMap<>();
 		MongoCollection<Document> collection = this.database.getCollection("Products");

 		Bson wFilter = Filters.in("website", websites);
 		Bson cFilter = Filters.eq("category", category);
 		Bson sFilter = Filters.ne("spec", new Document());
 		Bson lFilter = Filters.ne("linkage", Collections.EMPTY_LIST);
 		Bson andFilter = Filters.and(wFilter, cFilter, sFilter, lFilter);
 		Bson matchBson = Aggregates.match(andFilter);
 		Bson unwindBson = Aggregates.unwind("$linkage");

 		// get all product's pages in "catalog" with linkage (unwinding the
 		// linkage attribute)
 		List<Document> prods = new ArrayList<>();
 		collection.aggregate(Arrays.asList(matchBson, unwindBson)).forEach((Document d) -> prods.add(d));

 		// create map <URL of a linked page, page in catalog>
 		Map<String, Document> linkageUrls = new HashMap<>();
 		prods.stream().forEach(p -> linkageUrls.put(p.getString("linkage"), p));

 		// get all the linked product's pages
 		// <url, list of document in linkage with the one associated to the key>
 		Map<String, List<Document>> intL = new HashMap<>();
 		// <linked page, page in catalog>
 		Map<Document, Document> extL = new HashMap<>();
 		Bson uFilter = Filters.in("url", linkageUrls.keySet());
 		andFilter = Filters.and(cFilter, uFilter, sFilter);
 		collection.find(andFilter).projection(Projections.include("spec", "url", "website")).forEach((Document p) -> {
 			// if it's not a page in the
 			// catalog create new map
 			// entry
 			if (!websites.contains(getDomain(p.getString("url")))) {
 				Document catalogPage = linkageUrls.get(p.getString("url"));
 				extL.put(p, catalogPage);
 			}
 			// if it's a page in the
 			// catalog add it to the
 			// temporary
 			// list
 			else {
 				Document rlDoc = linkageUrls.get(p.getString("url"));
 				String urlP = p.getString("url");
 				String urlRlDoc = linkageUrls.get(p.getString("url")).getString("url");
 				List<Document> linkageP = intL.getOrDefault(urlP, new ArrayList<Document>());
 				List<Document> linkageRlDoc = intL.getOrDefault(urlRlDoc, new ArrayList<Document>());
 				linkageP.add(rlDoc);
 				linkageRlDoc.add(p);
 				intL.put(urlP, linkageP);
 				intL.put(urlRlDoc, linkageRlDoc);
 			}
 		});

 		// add internal linkage to the lists in rlMap
 		for (Map.Entry<Document, Document> entry : extL.entrySet()) {
 			List<Document> linkageList = new ArrayList<>();
 			if (intL.containsKey(entry.getValue().getString("url")))
 				linkageList = intL.get(entry.getValue().getString("url"));
 			else
 				linkageList.add(entry.getValue());
 			rlMap.put(entry.getKey(), linkageList);
 		}

 		// <linked page, list of pages in catalog>
 		return rlMap;
 	}
 	
     private String getDomain(String url) {
         String domain = null;
         url = url.replaceAll(" ", "%20").replaceAll("\\|", "%7C").replaceAll("\"", "%22");
         if (url.startsWith("http:/") || url.startsWith("https:/")) {
             if (!url.startsWith("http://") && !url.startsWith("https://")) {
                 url = url.replace("http:/", "http://");
                 url = url.replace("https:/", "https://");
             }
         } else
             url = "http://" + url;

         try {
             URI u = new URI(url);
             domain = u.getHost();
         } catch (URISyntaxException e) {
             System.err.println("Couldn't extract host from URL: " + url);
             e.printStackTrace();
         }
         domain = domain.replaceAll("%20", " ").replaceAll("%7C", "|");

         return domain;
     }

 	@Override
 	public List<Document> getProds(String category, String website1, String website2, String attribute) {
         MongoCollection<Document> collection = this.database.getCollection("Products");
         List<Document> prods = new ArrayList<>();
         Bson cFilter = Filters.eq("category", category);
         Bson wFilter = Filters.eq("website", website1);
         Pattern regex = Pattern.compile(".*" + website2.replace(".", "\\.") + ".*", Pattern.CASE_INSENSITIVE);
         Bson lFilter = Filters.eq("linkage", regex);
         Bson aFilter = Filters.exists("spec." + attribute, true);
         Bson andFilter;

         List<Bson> filters = new ArrayList<>();
         if (!category.equals(""))
             filters.add(cFilter);
         if (!website1.equals(""))
             filters.add(wFilter);
         if (!attribute.equals(""))
             filters.add(aFilter);
         if (!website2.equals(""))
             filters.add(lFilter);

         andFilter = Filters.and(filters);

         collection.find(Filters.and(andFilter)).limit(2000).forEach((Document d) -> prods.add(d));

         return prods;
 	}

 	@Override
 	public List<Document[]> getProdsInRL(List<Document> prods, String website, String attribute) {
         Map<String, List<Integer>> rlMap = new HashMap<>();
         List<String> docsToFetch = new ArrayList<>();
         List<Document> fetchedProducts = new ArrayList<>();
         List<Document[]> rlList = new ArrayList<>();

         // get urls of the linkage products
         for (int i = 0; i < prods.size(); i++) {
             Document p = prods.get(i);
             @SuppressWarnings("unchecked")
             List<String> rlProds = p.get("linkage", List.class);
             for (String url : rlProds)
                 if (url.contains(website)) {
                     List<Integer> indexes = rlMap.getOrDefault(url, new ArrayList<Integer>());
                     indexes.add(i);
                     rlMap.put(url, indexes);
                     docsToFetch.add(url);
                 }
         }

         // get all the linkage products containing the relevant attribute
         MongoCollection<Document> collection = this.database.getCollection("Products");
         Bson inFilter = Filters.in("url", docsToFetch);
         Bson attFilter = Filters.exists("spec." + attribute, true);
         Bson andFilter = Filters.and(inFilter, attFilter);
         collection.find(andFilter).projection(Projections.include("spec", "url"))
                 .forEach((Document d) -> fetchedProducts.add(d));

         // create record linkage list
         fetchedProducts.stream().forEach(d -> {
             String url = d.getString("url");
             List<Integer> indexes = rlMap.get(url);
             for (int i : indexes)
                 rlList.add(new Document[] { prods.get(i), d });
         });

         return rlList;
 	}

 	@Override
 	public Document getIfValid(String url) {
         return this.database
                 .getCollection("Products")
                 .find(Filters.and(Filters.eq("url", url), Filters.ne("linkage", Collections.EMPTY_LIST),
                         Filters.ne("spec", new Document()))).first();
 	}
}
