package us.tastybento.bskyblock.database.mysql;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
public class MySQLDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    /**
     * Connection to the database
     */
    private Connection connection = null;

    private BSkyBlock bskyblock;

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - BSkyBlock plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param dbConnecter - authentication details for the database
     */
    public MySQLDatabaseHandler(BSkyBlock plugin, Class<T> type, DatabaseConnecter dbConnecter) {
        super(plugin, type, dbConnecter);
        this.bskyblock = plugin;
        connection = (Connection)dbConnecter.createConnection();
        // Check if the table exists in the database and if not, create it
        createSchema();
    }

    /**
     * Creates the table in the database if it doesn't exist already
     */
    private void createSchema() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS `");
        sql.append(dataObject.getCanonicalName());
        sql.append("` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) )");
        // Prepare and execute the database statements
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.logError("Problem trying to create schema for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
        }
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
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT `json` FROM `");
        sb.append(dataObject.getCanonicalName());
        sb.append("`");
        try (Statement preparedStatement = connection.createStatement()) {
            try (ResultSet resultSet = preparedStatement.executeQuery(sb.toString())) {
                // Load all the results
                Gson gson = getGSON();
                while (resultSet.next()) {
                    list.add(gson.fromJson(resultSet.getString("json"), dataObject));
                }
            }
        } catch (SQLException e) {
            plugin.logError("Could not load objects " + e.getMessage());
        }
        return list;
    }

    @Override
    public T loadObject(String uniqueId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT `json` FROM `");
        sb.append(dataObject.getCanonicalName());
        sb.append("` WHERE uniqueId = ? LIMIT 1");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString())) {
            // UniqueId needs to be placed in quotes
            preparedStatement.setString(1, "\"" + uniqueId + "\"");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    // If there is a result, we only want/need the first one
                    Gson gson = getGSON();
                    return gson.fromJson(resultSet.getString("json"), dataObject);
                }
            }
        } catch (SQLException e) {
            plugin.logError("Could not load object " + uniqueId + " " + e.getMessage());
        }
        return null;
    }

    @Override
    public void saveObject(T instance) {
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        StringBuilder sb = new StringBuilder();
        // Replace into is used so that any data in the table will be replaced with updated data
        sb.append("INSERT INTO ");
        sb.append("`");
        // The table name is the canonical name, so that add-ons can be sure of a unique table in the database
        sb.append(dataObject.getCanonicalName());
        sb.append("` (json) VALUES (?) ON DUPLICATE KEY UPDATE json = ?");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString())) {
            Gson gson = getGSON();
            String toStore = gson.toJson(instance);
            preparedStatement.setString(1, toStore);
            preparedStatement.setString(2, toStore);
            preparedStatement.execute(); 
        } catch (SQLException e) {
            plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(T instance) {
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM `");
        sb.append(dataObject.getCanonicalName());
        sb.append("` WHERE uniqueId = ?");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString())) {
            Method getUniqueId = dataObject.getMethod("getUniqueId");
            String uniqueId = (String) getUniqueId.invoke(instance);
            preparedStatement.setString(1, uniqueId);
            preparedStatement.execute();
        } catch (Exception e) {
            plugin.logError("Could not delete object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#objectExists(java.lang.String)
     */
    @Override
    public boolean objectExists(String key) {
        // Create the query to see if this key exists
        StringBuilder query = new StringBuilder();
        query.append("SELECT IF ( EXISTS( SELECT * FROM `");
        query.append(dataObject.getCanonicalName());
        query.append("` WHERE `uniqueId` = ?), 1, 0)");

        try (PreparedStatement preparedStatement = connection.prepareStatement(query.toString())) {
            preparedStatement.setString(1, key);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            plugin.logError("Could not check if key exists in database! " + key + " " + e.getMessage());
        }
        return false;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.logError("Could not close database for some reason");
            }
        }
    }

}
