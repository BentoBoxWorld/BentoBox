package world.bentobox.bentobox.database.mongodb;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.util.JSON;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.mysql.adapters.FlagAdapter;
import world.bentobox.bentobox.database.mysql.adapters.LocationAdapter;
import world.bentobox.bentobox.database.mysql.adapters.PotionEffectTypeAdapter;
import world.bentobox.bentobox.database.mysql.adapters.WorldAdapter;
import world.bentobox.bentobox.database.objects.DataObject;

/**
 *
 * Class that inserts a <T> into the corresponding database-table.
 *
 * @author tastybento
 *
 * @param <T>
 */
@SuppressWarnings("deprecation")
public class MongoDBDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

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
    public MongoDBDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector dbConnecter) {
        super(plugin, type, dbConnecter);
        this.dbConnecter = dbConnecter;
        /*
      Connection to the database
         */
        MongoDatabase database = (MongoDatabase) dbConnecter.createConnection();
        collection = database.getCollection(dataObject.getCanonicalName());
        IndexOptions indexOptions = new IndexOptions().unique(true);
        collection.createIndex(Indexes.text(UNIQUEID), indexOptions);
    }

    // Gets the GSON builder
    private Gson getGSON() {
        // excludeFieldsWithoutExposeAnnotation - this means that every field to be stored should use @Expose
        // enableComplexMapKeySerialization - forces GSON to use TypeAdapters even for Map keys
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().enableComplexMapKeySerialization();
        // Register adapters
        builder.registerTypeAdapter(Location.class, new LocationAdapter(plugin)) ;
        builder.registerTypeAdapter(World.class, new WorldAdapter(plugin));
        builder.registerTypeAdapter(Flag.class, new FlagAdapter(plugin));
        builder.registerTypeAdapter(PotionEffectType.class, new PotionEffectTypeAdapter());
        // Keep null in the database
        builder.serializeNulls();
        // Allow characters like < or > without escaping them
        builder.disableHtmlEscaping();
        return builder.create();
    }

    @Override
    public List<T> loadObjects() {
        List<T> list = new ArrayList<>();
        Gson gson = getGSON();
        for (Document document : collection.find(new Document())) {
            // The deprecated serialize option does not have a viable alternative without involving a huge amount of custom code
            String json = JSON.serialize(document);
            json = json.replaceFirst(MONGO_ID, UNIQUEID);
            list.add(gson.fromJson(json, dataObject));
        }
        return list;
    }

    @Override
    public T loadObject(String uniqueId) {
        Document doc = collection.find(new Document(MONGO_ID, uniqueId)).limit(1).first();
        Gson gson = getGSON();
        String json = JSON.serialize(doc).replaceFirst(MONGO_ID, UNIQUEID);
        // load single object
        return gson.fromJson(json, dataObject);
    }

    @Override
    public void saveObject(T instance) {
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        DataObject dataObj = (DataObject)instance;
        try {
            Gson gson = getGSON();
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
        } catch (Exception e) {
            plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(T instance) {
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        try {
            collection.findOneAndDelete(new Document(MONGO_ID, ((DataObject)instance).getUniqueId()));
        } catch (Exception e) {
            plugin.logError("Could not delete object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public boolean objectExists(String uniqueId) {
        return collection.find(new Document(MONGO_ID, uniqueId)).first() != null;
    }

    @Override
    public void close() {
        dbConnecter.closeConnection();

    }


}
