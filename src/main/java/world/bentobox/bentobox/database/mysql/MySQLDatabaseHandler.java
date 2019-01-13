package world.bentobox.bentobox.database.mysql;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.json.AbstractJSONDatabaseHandler;
import world.bentobox.bentobox.database.objects.DataObject;

/**
 *
 * Class that inserts a <T> into the corresponding database-table.
 *
 * @author tastybento
 *
 * @param <T>
 */
public class MySQLDatabaseHandler<T> extends AbstractJSONDatabaseHandler<T> {

    /**
     * Connection to the database
     */
    private Connection connection;

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param dbConnecter - authentication details for the database
     */
    MySQLDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector dbConnecter) {
        super(plugin, type, dbConnecter);
        connection = (Connection)dbConnecter.createConnection();
        // Check if the table exists in the database and if not, create it
        createSchema();
    }

    /**
     * Creates the table in the database if it doesn't exist already
     */
    private void createSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS `" +
                dataObject.getCanonicalName() +
                "` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) )";
        // Prepare and execute the database statements
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.logError("Problem trying to create schema for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
        }
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
                Gson gson = getGson();
                while (resultSet.next()) {
                    String json = resultSet.getString("json");
                    if (json != null) {
                        try {
                            T gsonResult = gson.fromJson(json, dataObject);
                            if (gsonResult != null) {
                                list.add(gsonResult);
                            }
                        } catch (JsonSyntaxException ex) {
                            plugin.logError("Could not load object " + ex.getMessage());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.logError("Could not load objects " + e.getMessage());
        }
        return list;
    }

    @Override
    public T loadObject(String uniqueId) {
        String sb = "SELECT `json` FROM `" +
                dataObject.getCanonicalName() +
                "` WHERE uniqueId = ? LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb)) {
            // UniqueId needs to be placed in quotes
            preparedStatement.setString(1, "\"" + uniqueId + "\"");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    // If there is a result, we only want/need the first one
                    Gson gson = getGson();
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
        // Null check
        if (instance == null) {
            plugin.logError("MySQL database request to store a null. ");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        String sb = "INSERT INTO " +
                "`" +
                dataObject.getCanonicalName() +
                "` (json) VALUES (?) ON DUPLICATE KEY UPDATE json = ?";

        Gson gson = getGson();
        String toStore = gson.toJson(instance);
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> store(instance, toStore, sb));
        } else {
            store(instance, toStore, sb);
        }
    }

    private void store(T instance, String toStore, String sb) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb)) {
            preparedStatement.setString(1, toStore);
            preparedStatement.setString(2, toStore);
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public boolean deleteID(String uniqueId) {
        String sb = "DELETE FROM `" +
                dataObject.getCanonicalName() +
                "` WHERE uniqueId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb)) {
            // UniqueId needs to be placed in quotes
            preparedStatement.setString(1, "\"" + uniqueId + "\"");
            preparedStatement.execute();
            return preparedStatement.getUpdateCount() > 0;
        } catch (Exception e) {
            plugin.logError("Could not delete object " + dataObject.getCanonicalName() + " " + uniqueId + " " + e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteObject(T instance) {
        // Null check
        if (instance == null) {
            plugin.logError("MySQL database request to delete a null. ");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        try {
            Method getUniqueId = dataObject.getMethod("getUniqueId");
            deleteID((String) getUniqueId.invoke(instance));
        } catch (Exception e) {
            plugin.logError("Could not delete object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public boolean objectExists(String uniqueId) {
        // Create the query to see if this key exists
        String query = "SELECT IF ( EXISTS( SELECT * FROM `" +
                dataObject.getCanonicalName() +
                "` WHERE `uniqueId` = ?), 1, 0)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            // UniqueId needs to be placed in quotes
            preparedStatement.setString(1, "\"" + uniqueId + "\"");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            plugin.logError("Could not check if key exists in database! " + uniqueId + " " + e.getMessage());
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
