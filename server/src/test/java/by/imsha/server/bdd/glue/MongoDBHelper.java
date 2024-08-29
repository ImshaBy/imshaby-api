package by.imsha.server.bdd.glue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class MongoDBHelper {
    private static final String CONNECTION_STRING = "mongodb://localhost:" + SpringGlue.MONGO_PORT;
    private static final String DATABASE_NAME = "imshaby";

    private final MongoClient mongoClient;
    private final MongoDatabase database;

    public MongoDBHelper() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        database = mongoClient.getDatabase(DATABASE_NAME);
    }

    public void cleanDatabase() {
        for (String collectionName : database.listCollectionNames()) {
            database.getCollection(collectionName).deleteMany(new org.bson.Document());
        }
    }

    public void close() {
        mongoClient.close();
    }
}