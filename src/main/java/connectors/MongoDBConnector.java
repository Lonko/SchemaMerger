package connectors;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.WriteModel;

public class MongoDBConnector {
    private MongoClient mc;
    private MongoDatabase database;
    private FileDataConnector fdc;

    /*
     * CONSTRUCTORS
     */
    public MongoDBConnector(FileDataConnector fdc) {
        this("mongodb://localhost:27017", "Dataset", fdc);
    }

    public MongoDBConnector(String uri, String db, FileDataConnector fdc) {
        MongoClientURI mcUri = new MongoClientURI(uri);
        this.mc = new MongoClient(mcUri);
        this.database = mc.getDatabase(db);
        this.fdc = fdc;
    }

    /*
     * END CONSTRUCTORS
     */

    /*
     * PUBLIC METHODS
     */
    public void dropCollection(String collectionName) {
        this.database.getCollection(collectionName).drop();
    }

    public void changeDB(String dbName) {
        this.database = this.mc.getDatabase(dbName);
    }

    public long countCollection(String collectionName) {
        return this.database.getCollection(collectionName).count();
    }

    public void initializeAllCollections() {
        initializeCollection("Products");
        initializeCollection("RecordLinkage");
        initializeCollection("Schemas");
        updateProductsRL();
    }

    public void initializeCollection(String collectionName) {

        try {
            MongoCollection<Document> collection = this.database.getCollection(collectionName)
                    .withWriteConcern(WriteConcern.JOURNALED);
            if (collection.count() == 0) { // only if it's a new collection
                Method m = this.getClass().getDeclaredMethod("initialize" + collectionName,
                        MongoCollection.class);
                m.invoke(this, collection);
            }
        } catch (NoSuchMethodException | SecurityException e) {
            System.err.println(collectionName + " is not a valid collection name");
            e.printStackTrace();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    public List<Document> getRLSample(int size, String category) {
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

    /*
     * Methods to query the db for products on 3 levels of granularity: 1 -> By
     * Category 2 -> By Category and Website 3 -> By Category, Website and
     * Attribute
     */

    // query only by category
    public List<Document> getProds(String category) {
        return getProds(category, "", "", "");
    }

    // query only by category and website
    public List<Document> getProds(String category, String website) {
        return getProds(category, website, "", "");
    }

    /*
     * query by category, website and attribute. if a parameter is an empty
     * string, it gets ignored.
     */
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

    // used in the match phase
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
        collection.find(andFilter)
                .projection(Projections.include("spec", "url", "website"))
                .forEach((Document p) -> {
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
                            List<Document> linkageRlDoc = intL.getOrDefault(urlRlDoc,
                                    new ArrayList<Document>());
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

    // used in the Training Set generation
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

    /*
     * fetches the schemas of all sources that belong to one of the categories
     * selected doesn't consider schemas without attributes
     */
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

    public Document getProductByUrl(String url) {
        return this.database.getCollection("Products").find(Filters.eq("url", url)).first();
    }

    public Document getIfValid(String url) {
        return this.database
                .getCollection("Products")
                .find(Filters.and(Filters.eq("url", url), Filters.ne("linkage", Collections.EMPTY_LIST),
                        Filters.ne("spec", new Document()))).first();
    }

    public String getWebsite(String url) {
        String website;
        Document doc = this.database.getCollection("Products").find(Filters.eq("url", url)).first();
        try {
            website = doc.getString("website");
        } catch (Exception e) {
            System.err.println(url);
            website = null;
        }

        return website;
    }

    public void insertBatch(List<Document> docs, String collectionName) {
        MongoCollection<Document> collection = this.database.getCollection(collectionName).withWriteConcern(
                WriteConcern.JOURNALED);
        collection.insertMany(docs);
    }

    // retrieve list of product with the provided ids from the catalogue
    public List<Document> getFromCatalogue(List<Integer> ids) {
        List<Document> products = new ArrayList<>();
        MongoCollection<Document> collection = this.database.getCollection("Catalogue");
        collection.find(Filters.in("id", ids)).forEach((Document d) -> products.add(d));

        return products;
    }

    public void addSyntheticProductsIndexes() {
        MongoCollection<Document> collection = this.database.getCollection("Products");
        addProductsIndexes(collection);
    }

    /*
     * END PUBLIC METHODS
     */

    /*
     * COLLECTION INITIALIZATION METHODS
     */
    @SuppressWarnings("unused")
    private void initializeProducts(MongoCollection<Document> collection) {
        Map<String, String> websites = this.fdc.getAllWebsites();
        websites.entrySet().stream().map(this::website2Documents).filter(list -> list.size() > 0)
                .forEach(collection::insertMany);

        addProductsIndexes(collection);

        initializeCollection("Schemas");
    }

    private void addProductsIndexes(MongoCollection<Document> collection) {
        collection.createIndex(Indexes.ascending("category"));
        collection.createIndex(Indexes.ascending("website"));
        collection.createIndex(Indexes.ascending("website", "category"));
        collection.createIndex(Indexes.hashed("url"));
    }

    @SuppressWarnings("unused")
    private void initializeRecordLinkage(MongoCollection<Document> collection) {
        List<JSONObject> jsonList = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Collection<JSONObject> jsonCollection = Collections.checkedCollection(this.fdc.readRecordLinkage()
                .values(), JSONObject.class);
        jsonList.addAll(jsonCollection);
        jsonList.stream().map(this::urls2Documents).filter(list -> list.size() > 0)
                .forEach(collection::insertMany);

        collection.createIndex(Indexes.ascending("category"));
        collection.createIndex(Indexes.hashed("url1"));
        collection.createIndex(Indexes.hashed("url2"));
    }

    /*
     * should only be called after initializeProducts has already been called
     * once
     */
    @SuppressWarnings("unused")
    private void initializeSchemas(MongoCollection<Document> collection) {
        MongoCollection<Document> products = this.database.getCollection("Products");
        Map<String, Set<String>> schemas = new TreeMap<>();

        products.find().projection(Projections.fields(Projections.include("spec", "website", "category")))
                .forEach((Document d) -> loadSchema(d, schemas));

        schemas.entrySet().stream().map(this::schema2Document).forEach(collection::insertOne);

        collection.createIndex(Indexes.ascending("category"));
        collection.createIndex(Indexes.ascending("website"));
        collection.createIndex(Indexes.ascending("website", "category"));
    }

    private void loadSchema(Document d, Map<String, Set<String>> schemas) {
        String source = d.getString("website") + "___" + d.getString("category");
        Document spec = d.get("spec", Document.class);
        Set<String> attributes = spec.keySet();

        Set<String> schema = schemas.getOrDefault(source, new TreeSet<String>());
        schema.addAll(attributes);
        schemas.put(source, schema);
    }

    /*
     * should only be called after both Products and RecordLinkage have been
     * initialized
     */
    private void updateProductsRL() {
        MongoCollection<Document> rl = this.database.getCollection("RecordLinkage");
        int batchSize = 1000;
        List<WriteModel<Document>> updates = new ArrayList<WriteModel<Document>>();

        MongoCursor<Document> cursor = rl.find().iterator();
        try {
            while (cursor.hasNext()) {
                if (updates.size() == batchSize) {
                    bulkUpdates("Products", updates);
                    updates.clear();
                }
                Document d = cursor.next();
                String u1 = d.getString("url1");
                String u2 = d.getString("url2");
                updates.add(new UpdateOneModel<Document>(new Document("url", u1), new Document("$push",
                        new Document("linkage", u2))));
                updates.add(new UpdateOneModel<Document>(new Document("url", u2), new Document("$push",
                        new Document("linkage", u1))));
            }
        } finally {
            cursor.close();
        }

    }

    private void bulkUpdates(String collectionName, List<WriteModel<Document>> updates) {
        MongoCollection<Document> products = this.database.getCollection("Products");
        products.bulkWrite(updates, new BulkWriteOptions().ordered(false));
    }

    private Document schema2Document(Map.Entry<String, Set<String>> sourceSchema) {
        String[] key = sourceSchema.getKey().split("___");
        String website = key[0];
        String category = key[1];

        return new Document("category", category).append("website", website).append("attributes",
                sourceSchema.getValue());
    }

    private List<Document> website2Documents(Map.Entry<String, String> website) {
        List<Document> docs = new ArrayList<>();
        List<File> sources = this.fdc.getWebsiteCategories(website.getValue());

        for (File source : sources) {
            for (Object json : this.fdc.readSource(source.getAbsolutePath())) {
                JSONObject prod = (JSONObject) json;
                Document doc = Document.parse(getValidJSON(prod));
                doc.append("linkage", new ArrayList<>()).append("website", website.getKey())
                        .append("category", source.getName().split("_")[0]);
                docs.add(doc);
            }
        }

        return docs;
    }

    private String getValidJSON(JSONObject json) {
        JSONObject spec = (JSONObject) json.get("spec");
        @SuppressWarnings("unchecked")
        Set<String> fields = Collections.checkedSet(spec.keySet(), String.class);
        List<String> invalidFields = fields.stream()
                .filter(field -> field.contains(".") || field.startsWith("$")).collect(Collectors.toList());

        for (String invField : invalidFields)
            ((JSONObject) json.get("spec")).remove(invField);

        return json.toJSONString();
    }

    private List<Document> urls2Documents(JSONObject json) {
        List<Document> docs = new ArrayList<>();

        for (Object cat : json.keySet()) {
            JSONArray urls = (JSONArray) json.get(cat);
            for (int i = 0; i < urls.size() - 1; i++)
                for (int j = i + 1; j < urls.size(); j++) {
                    Document doc = new Document("category", (String) cat)
                            .append("url1", (String) urls.get(i)).append("url2", (String) urls.get(j));
                    docs.add(doc);
                }
        }

        return docs;
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

    /*
     * END COLLECTION INITIALIZATION METHODS
     */
    public static void main(String[] args) {
        // FileDataConnector fdc = new FileDataConnector();
        // MongoDBConnector mdbc = new MongoDBConnector(fdc);
        // mdbc.initializeCollection("Products");
        // mdbc.updateProductsRL();
        // List<String> s = new ArrayList<String>();
        // s.add("vanvreedes.com");
        // List<Document[]> docs = mdbc.getProdsInRL(s, "tv");
        // docs.forEach(dPair -> {
        // String u1 = dPair[0].getString("url");
        // String u2 = dPair[1].getString("url");
        // System.out.println(u1+"  <------>  "+u2);
        // });
    }
}
