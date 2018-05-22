package connectors;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;

/**
 * Singleton factory for MongoDB connections, used by DAO Note that only 1
 * MongoClient object should be used per application:
 * 
 * http://mongodb.github.io/mongo-java-driver/3.4/driver/tutorials/connect-to-mongodb/#mongoclient
 * 
 * @author federico
 *
 */
public class MongoDbConnectionFactory {

	private static MongoDbConnectionFactory instance;

	public static MongoDbConnectionFactory getMongoInstance(String uri, String db) {
		if (instance == null) {
			instance = new MongoDbConnectionFactory(uri, db);
		}
		return instance;
	}

	private MongoClient mc;
	private String databaseName;

	/*
	 * CONSTRUCTORS
	 */
	private MongoDbConnectionFactory() {
		this("mongodb://localhost:27017", "Dataset");
	}

	private MongoDbConnectionFactory(String uri, String db) {
		MongoClientURI mcUri = new MongoClientURI(uri);
		this.mc = new MongoClient(mcUri);
		this.databaseName = db;
	}

	public MongoDatabase getDatabase() {
		return this.mc.getDatabase(this.databaseName);
	}
}
