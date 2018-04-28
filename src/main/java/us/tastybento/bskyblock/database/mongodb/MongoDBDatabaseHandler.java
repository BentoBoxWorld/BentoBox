package us.tastybento.bskyblock.database.mongodb;

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
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.util.JSON;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.mysql.adapters.FlagAdapter;
import us.tastybento.bskyblock.database.mysql.adapters.LocationAdapter;
import us.tastybento.bskyblock.database.mysql.adapters.PotionEffectTypeAdapter;
import us.tastybento.bskyblock.database.mysql.adapters.WorldAdapter;
import us.tastybento.bskyblock.database.objects.DataObject;

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

    /**
     * Connection to the database
     */
    private MongoDatabase database = null;
    private MongoCollection<Document> collection;
    private DatabaseConnecter dbConnecter;

    private BSkyBlock bskyblock;

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - BSkyBlock plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param dbConnecter - authentication details for the database
     */
    public MongoDBDatabaseHandler(BSkyBlock plugin, Class<T> type, DatabaseConnecter dbConnecter) {
        super(plugin, type, dbConnecter);
        this.bskyblock = plugin;
        this.dbConnecter = dbConnecter;
        database = (MongoDatabase)dbConnecter.createConnection();
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
        builder.registerTypeAdapter(Flag.class, new FlagAdapter(bskyblock));
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
        MongoCursor<Document> it = collection.find(new Document()).iterator();
        while (it.hasNext()) {
            // The deprecated serialize option does not have a viable alternative without involving a huge amount of custom code
            String json = JSON.serialize(it.next());
            json = json.replaceFirst(MONGO_ID, UNIQUEID);
            list.add(gson.fromJson(json, dataObject));
        }
        return list;
    }

    @Override
    public T loadObject(String uniqueId) { 
        Document doc = collection.find(new Document(MONGO_ID, uniqueId)).limit(1).first();
        if (doc != null) {
            Gson gson = getGSON();
            String json = JSON.serialize(doc).replaceFirst(MONGO_ID, UNIQUEID);
            // load single object
            return gson.fromJson(json, dataObject);
        }
        return null;
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

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#objectExists(java.lang.String)
     */
    @Override
    public boolean objectExists(String key) {
        return collection.find(new Document(MONGO_ID, key)).first() != null ? true : false;
    }

    @Override
    public void close() {
        dbConnecter.closeConnection();

    }


}
