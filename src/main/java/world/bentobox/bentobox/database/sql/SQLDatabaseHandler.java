package world.bentobox.bentobox.database.sql;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.json.AbstractJSONDatabaseHandler;
import world.bentobox.bentobox.database.objects.DataObject;

/**
 *
 * Abstract class that covers SQL style databases
 * Class that inserts a <T> into the corresponding database-table.
 *
 * @author tastybento
 *
 * @param <T>
 */
public class SQLDatabaseHandler<T> extends AbstractJSONDatabaseHandler<T> {

    protected static final String COULD_NOT_LOAD_OBJECTS = "Could not load objects ";
    protected static final String COULD_NOT_LOAD_OBJECT = "Could not load object ";

    /**
     * Connection to the database
     */
    private Connection connection;

    /**
     * SQL configuration
     */
    private SQLConfiguration sqlConfig;

    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param dbConnecter - authentication details for the database
     * @param sqlConfiguration - SQL configuration
     */
    protected SQLDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector dbConnecter, SQLConfiguration sqlConfiguration) {
        super(plugin, type, dbConnecter);
        this.sqlConfig = sqlConfiguration;
        if (setConnection((Connection)databaseConnector.createConnection(type))) {
            // Check if the table exists in the database and if not, create it
            createSchema();
        }
    }

    /**
     * @return the sqlConfig
     */
    public SQLConfiguration getSqlConfig() {
        return sqlConfig;
    }

    /**
     * @param sqlConfig the sqlConfig to set
     */
    public void setSqlConfig(SQLConfiguration sqlConfig) {
        this.sqlConfig = sqlConfig;
    }

    /**
     * Creates the table in the database if it doesn't exist already
     */
    protected void createSchema() {
        if (sqlConfig.renameRequired()) {
            // Transition from the old table name
            try (PreparedStatement pstmt = connection.prepareStatement(sqlConfig.getRenameTableSQL())) {
                pstmt.execute();
            } catch (SQLException e) {
                plugin.logError("Could not rename " + sqlConfig.getOldTableName() + " for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
            }
        }
        // Prepare and execute the database statements
        try (PreparedStatement pstmt = connection.prepareStatement(sqlConfig.getSchemaSQL())) {
            pstmt.execute();
        } catch (SQLException e) {
            plugin.logError("Problem trying to create schema for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
        }
    }

    @Override
    public List<T> loadObjects() {
        try (Statement preparedStatement = connection.createStatement()) {
            return loadIt(preparedStatement);
        } catch (SQLException e) {
            plugin.logError(COULD_NOT_LOAD_OBJECTS + e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<T> loadIt(Statement preparedStatement) {
        List<T> list = new ArrayList<>();
        try (ResultSet resultSet = preparedStatement.executeQuery(sqlConfig.getLoadObjectsSQL())) {
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
                        plugin.logError(COULD_NOT_LOAD_OBJECT + ex.getMessage());
                        plugin.logError(json);
                    }
                }
            }
        } catch (Exception e) {
            plugin.logError(COULD_NOT_LOAD_OBJECTS + e.getMessage());
        }
        return list;
    }

    @Override
    public T loadObject(@NonNull String uniqueId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlConfig.getLoadObjectSQL())) {
            // UniqueId needs to be placed in quotes?
            preparedStatement.setString(1, this.sqlConfig.isUseQuotes() ? "\"" + uniqueId + "\"" : uniqueId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    // If there is a result, we only want/need the first one
                    Gson gson = getGson();
                    return gson.fromJson(resultSet.getString("json"), dataObject);
                }
            } catch (Exception e) {
                plugin.logError(COULD_NOT_LOAD_OBJECT + uniqueId + " " + e.getMessage());
            }
        } catch (SQLException e) {
            plugin.logError(COULD_NOT_LOAD_OBJECT + uniqueId + " " + e.getMessage());
        }
        return null;
    }

    @Override
    public CompletableFuture<Boolean> saveObject(T instance) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        // Null check
        if (instance == null) {
            plugin.logError("SQL database request to store a null. ");
            completableFuture.complete(false);
            return completableFuture;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            completableFuture.complete(false);
            return completableFuture;
        }
        // This has to be on the main thread to avoid concurrent modification errors
        String toStore = getGson().toJson(instance);
        // Async
        processQueue.add(() -> store(completableFuture, instance.getClass().getName(), toStore, sqlConfig.getSaveObjectSQL()));
        return completableFuture;
    }

    private void store(CompletableFuture<Boolean> completableFuture, String name, String toStore, String sb) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sb)) {
            preparedStatement.setString(1, toStore);
            preparedStatement.setString(2, toStore);
            preparedStatement.execute();
            completableFuture.complete(true);
        } catch (SQLException e) {
            plugin.logError("Could not save object " + name + " " + e.getMessage());
            completableFuture.complete(false);
        }
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteID(java.lang.String)
     */
    @Override
    public void deleteID(String uniqueId) {
        processQueue.add(() -> delete(uniqueId));
    }

    private void delete(String uniqueId) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlConfig.getDeleteObjectSQL())) {
            // UniqueId needs to be placed in quotes?
            preparedStatement.setString(1, this.sqlConfig.isUseQuotes() ? "\"" + uniqueId + "\"" : uniqueId);
            preparedStatement.execute();
        } catch (Exception e) {
            plugin.logError("Could not delete object " + plugin.getSettings().getDatabasePrefix() + dataObject.getCanonicalName() + " " + uniqueId + " " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(T instance) {
        // Null check
        if (instance == null) {
            plugin.logError("SQL database request to delete a null.");
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
        // Query to see if this key exists
        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlConfig.getObjectExistsSQL())) {
            // UniqueId needs to be placed in quotes?
            preparedStatement.setString(1, this.sqlConfig.isUseQuotes() ? "\"" + uniqueId + "\"" : uniqueId);
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
        shutdown = true;
    }

    /**
     * @return the connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * @param connection the connection to set
     * @return true if connection is not null
     */
    public boolean setConnection(Connection connection) {
        if (connection == null) {
            plugin.logError("Could not connect to the database. Are the credentials in the config.yml file correct?");
            plugin.logWarning("Disabling the plugin...");
            Bukkit.getPluginManager().disablePlugin(plugin);
            return false;
        }
        this.connection = connection;
        return true;
    }
}
