package us.tastybento.bskyblock.database.mysqljson;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

/**
 *
 * Class that inserts a <T> into the corresponding database-table.
 *
 * @author tastybento
 *
 * @param <T>
 */
public class MySQLDatabaseHandlerJ<T> extends AbstractDatabaseHandler<T> {

    /**
     * Connection to the database
     */
    private Connection connection = null;

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - BSkyBlock plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param databaseConnecter - authentication details for the database
     */
    public MySQLDatabaseHandlerJ(Plugin plugin, Class<T> type, DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
        connection = databaseConnecter.createConnection();
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
        sql.append("` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), INDEX i (uniqueId) );");
        // Prepare and execute the database statements
        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "Problem trying to create schema for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
        }
    }

    @Override
    public List<T> loadObjects() {
        List<T> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT `json`  FROM `");
        sb.append(dataObject.getCanonicalName());
        sb.append("`");
        try (Statement preparedStatement = connection.createStatement()) {
            try (ResultSet resultSet = preparedStatement.executeQuery(sb.toString())) {
                // Load all the results
                GsonBuilder builder = new GsonBuilder();
                builder.registerTypeAdapter(Location.class, new LocationAdapter()) ;
                Gson gson = builder.create();
                while (resultSet.next()) {
                    list.add(gson.fromJson(resultSet.getString("json"), dataObject));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "Could not load objects " + e.getMessage());
        }
        return list;
    }


    @Override
    public T loadObject(String uniqueId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT `json`  FROM `");
        sb.append(dataObject.getCanonicalName());
        sb.append("` WHERE uniqueId = ? LIMIT 1");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString())) {
            preparedStatement.setString(1, uniqueId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    // If there is a result, we only want/need the first one
                    GsonBuilder builder = new GsonBuilder();
                    builder.registerTypeAdapter(Location.class, new LocationAdapter()) ;
                    Gson gson = builder.create();
                    return gson.fromJson(resultSet.getString("json"), dataObject);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "Could not load object " + uniqueId + " " + e.getMessage());
        }
        return null;
    }

    @Override
    public void saveObject(T instance) {
        Bukkit.getLogger().severe(() -> "Saving object " + instance.getClass().getName());
        StringBuilder sb = new StringBuilder();
        // Replace into is used so that any data in the table will be replaced with updated data
        sb.append("REPLACE INTO ");
        sb.append("`");
        // The table name is the canonical name, so that add-ons can be sure of a unique table in the database
        sb.append(dataObject.getCanonicalName());
        sb.append("` (json) VALUES (?)");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString())) {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Location.class, new LocationAdapter()) ;
            Gson gson = builder.create();
            preparedStatement.setString(1, gson.toJson(instance));
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "Could not save object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(T instance) {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM `");
        sb.append(dataObject.getCanonicalName());
        sb.append("` WHERE uniqueId = ?");
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb.toString())) {
            Method getUniqueId = dataObject.getMethod("getUniqueId");
            String uniqueId = (String) getUniqueId.invoke(instance);
            preparedStatement.setString(1, uniqueId);
            preparedStatement.executeQuery();
        } catch (Exception e) {
            plugin.getLogger().severe(() -> "Could not delete object " + instance.getClass().getName() + " " + e.getMessage());
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
            plugin.getLogger().severe("Could not check if key exists in database! " + key + " " + e.getMessage());
        }
        return false;
    }

    @Override
    public void saveSettings(T instance)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
        // This method should not be used because configs are not stored in MySQL

    }

    @Override
    public T loadSettings(String uniqueId, T dbConfig) {
        // This method should not be used because configs are not stored in MySQL
        return null;
    }



}
