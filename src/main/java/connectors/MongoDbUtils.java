package connectors;

import java.util.List;

import org.bson.Document;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * Common methods for DAO
 * @author federico
 *
 */
public class MongoDbUtils {
	
    public static void insertBatch(MongoDatabase db, List<Document> docs, String collectionName) {
        MongoCollection<Document> collection = db.getCollection(collectionName).withWriteConcern(
                WriteConcern.JOURNALED);
        collection.insertMany(docs);
    }

}
