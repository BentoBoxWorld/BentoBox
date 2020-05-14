package world.bentobox.bentobox.database.mongodb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Bukkit;

import com.google.gson.Gson;
import com.mongodb.MongoClientException;
import com.mongodb.MongoNamespace;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.util.JSON;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.json.AbstractJSONDatabaseHandler;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.objects.Table;

/**
 *
 * Class that inserts a <T> into the corresponding database-table.
 *
 * @author tastybento
 *
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class MongoDBDatabaseHandler<T> extends AbstractJSONDatabaseHandler<T> {

    private static final String UNIQUEID = "uniqueId";
    private static final String MONGO_ID = "_id";

    private MongoCollection<Document> collection;
    private DatabaseConnector dbConnecter;

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param dbConnecter - authentication details for the database
     */
    MongoDBDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector dbConnecter) {
        super(plugin, type, dbConnecter);
        this.dbConnecter = dbConnecter;

        boolean connected = true; // if it is set to false, it will consider there has been an error upon connecting.
        try {
            // Connection to the database
            MongoDatabase database = (MongoDatabase) dbConnecter.createConnection(dataObject);
            if (database == null) {
                plugin.logError("Could not connect to the database. Are the credentials in the config.yml file correct?");
                connected = false;
            } else {
                // Check for old collections
                String oldName = plugin.getSettings().getDatabasePrefix() + type.getCanonicalName();
                String newName = getName(plugin, dataObject);
                if (!oldName.equals((newName)) && collectionExists(database, oldName) && !collectionExists(database, newName)){
                    collection = database.getCollection(oldName);
                    collection.renameCollection(new MongoNamespace(database.getName(), newName));
                } else {
                    collection = database.getCollection(newName);
                }
                IndexOptions indexOptions = new IndexOptions().unique(true);
                collection.createIndex(Indexes.text(UNIQUEID), indexOptions);
            }
        } catch (MongoTimeoutException e) {
            plugin.logError("Could not connect to the database. MongoDB timed out.");
            plugin.logError("Error code: " + e.getCode());
            plugin.logError("Errors: " + String.join(", ", e.getErrorLabels()));
            connected = false;
        } catch (MongoClientException e) {
            plugin.logError("Could not connect to the database. An unhandled error occurred.");
            plugin.logStacktrace(e);
            connected = false;
        }

        if (!connected) {
            plugin.logWarning("Disabling BentoBox...");
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    private boolean collectionExists(MongoDatabase database, final String collectionName) {
        for (final String name : database.listCollectionNames()) {
            if (name.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }

    private String getName(BentoBox plugin, Class<T> type) {
        return plugin.getSettings().getDatabasePrefix() +
                (type.getAnnotation(Table.class) == null ?
                        type.getCanonicalName()
                        : type.getAnnotation(Table.class)
                        .name());
    }

    @Override
    public List<T> loadObjects() {
        List<T> list = new ArrayList<>();
        Gson gson = getGson();
        for (Document document : collection.find(new Document())) {
            // The deprecated serialize option does not have a viable alternative without involving a huge amount of custom code
            String json = JSON.serialize(document);
            json = json.replaceFirst(MONGO_ID, UNIQUEID);
            try {
                list.add(gson.fromJson(json, dataObject));
            } catch (Exception e) {
                plugin.logError("Could not load object :" + e.getMessage());
            }
        }
        return list;
    }

    @Override
    public T loadObject(String uniqueId) {
        Document doc = collection.find(new Document(MONGO_ID, uniqueId)).limit(1).first();
        Gson gson = getGson();
        String json = JSON.serialize(doc).replaceFirst(MONGO_ID, UNIQUEID);
        // load single object
        return gson.fromJson(json, dataObject);
    }

    @Override
    public CompletableFuture<Boolean> saveObject(T instance) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        // Null check
        if (instance == null) {
            plugin.logError("MongoDB database request to store a null. ");
            completableFuture.complete(false);
            return completableFuture;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            completableFuture.complete(false);
            return completableFuture;
        }
        DataObject dataObj = (DataObject)instance;
        try {
            Gson gson = getGson();
            String toStore = gson.toJson(instance);
            // Change uniqueId to _id
            toStore = toStore.replaceFirst(UNIQUEID, MONGO_ID);
            // This parses JSON to a Mongo Document
            Document document = Document.parse(toStore);
            // Filter based on the id
            Bson filter = new Document(MONGO_ID, dataObj.getUniqueId());
            // Set the options to upsert (update or insert if doc is not there)
            FindOneAndReplaceOptions options = new FindOneAndReplaceOptions().upsert(true);
            // Do the deed
            collection.findOneAndReplace(filter, document, options);
            completableFuture.complete(true);
        } catch (Exception e) {
            plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
            completableFuture.complete(false);
        }
        return completableFuture;
    }

    @Override
    public void deleteID(String uniqueId) {
        try {
            collection.findOneAndDelete(new Document(MONGO_ID, uniqueId));
        } catch (Exception e) {
            plugin.logError("Could not delete object " + getName(plugin, dataObject) + " " + uniqueId + " " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(T instance) {
        // Null check
        if (instance == null) {
            plugin.logError("MondDB database request to delete a null. ");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        deleteID(((DataObject)instance).getUniqueId());
    }

    @Override
    public boolean objectExists(String uniqueId) {
        return collection.find(new Document(MONGO_ID, uniqueId)).first() != null;
    }

    @Override
    public void close() {
        dbConnecter.closeConnection(dataObject);
    }
}
