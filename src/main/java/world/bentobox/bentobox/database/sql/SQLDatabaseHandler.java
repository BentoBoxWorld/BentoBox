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

import javax.sql.DataSource;

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
public class SQLDatabaseHandler<T> extends AbstractJSONDatabaseHandler<T>
{
    protected static final String COULD_NOT_LOAD_OBJECTS = "Could not load objects ";
    protected static final String COULD_NOT_LOAD_OBJECT = "Could not load object ";

    /**
     * DataSource of database
     */
    protected DataSource dataSource;

    /**
     * SQL configuration
     */
    private SQLConfiguration sqlConfig;


    /**
     * Handles the connection to the database and creation of the initial database schema (tables) for
     * the class that will be stored.
     * @param plugin - plugin object
     * @param type - the type of class to be stored in the database. Must inherit DataObject
     * @param databaseConnector - authentication details for the database
     * @param sqlConfiguration - SQL configuration
     */
    protected SQLDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector, SQLConfiguration sqlConfiguration)
    {
        super(plugin, type, databaseConnector);
        this.sqlConfig = sqlConfiguration;

        if (this.setDataSource((DataSource) this.databaseConnector.createConnection(type)))
        {
            // Check if the table exists in the database and if not, create it
            this.createSchema();
        }
    }


    /**
     * @return the sqlConfig
     */
    public SQLConfiguration getSqlConfig()
    {
        return sqlConfig;
    }


    /**
     * @param sqlConfig the sqlConfig to set
     */
    public void setSqlConfig(SQLConfiguration sqlConfig)
    {
        this.sqlConfig = sqlConfig;
    }


    /**
     * Creates the table in the database if it doesn't exist already
     */
    protected void createSchema()
    {
        if (this.sqlConfig.renameRequired())
        {
            // Transition from the old table name
            String sql = this.sqlConfig.getRenameTableSQL().
                    replace("[oldTableName]", this.sqlConfig.getOldTableName()).
                    replace("[tableName]", this.sqlConfig.getTableName());

            try (Connection connection = this.dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(sql))
            {
                preparedStatement.execute();
            }
            catch (SQLException e)
            {
                this.plugin.logError("Could not rename " + this.sqlConfig.getOldTableName() + " for data object " +
                        this.dataObject.getCanonicalName() + " " + e.getMessage());
            }
        }

        // Prepare and execute the database statements
        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(this.sqlConfig.getSchemaSQL()))
        {
            preparedStatement.execute();
        }
        catch (SQLException e)
        {
            this.plugin.logError("Problem trying to create schema for data object " +
                    this.dataObject.getCanonicalName() + " " + e.getMessage());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<T> loadObjects()
    {
        try (Connection connection = this.dataSource.getConnection();
                Statement preparedStatement = connection.createStatement())
        {
            return this.loadIt(preparedStatement);
        }
        catch (SQLException e)
        {
            this.plugin.logError(COULD_NOT_LOAD_OBJECTS + e.getMessage());
        }

        return Collections.emptyList();
    }


    /**
     * This method loads objects based on results provided by prepared statement.
     * @param preparedStatement Statement from database.
     * @return List of object <T> from database.
     */
    private List<T> loadIt(Statement preparedStatement)
    {
        List<T> list = new ArrayList<>();

        try (ResultSet resultSet = preparedStatement.executeQuery(this.sqlConfig.getLoadObjectsSQL()))
        {
            // Load all the results
            Gson gson = this.getGson();

            while (resultSet.next())
            {
                String json = resultSet.getString("json");

                if (json != null)
                {
                    getGsonResultSet(gson, json, list);
                }
            }
        }
        catch (Exception e)
        {
            this.plugin.logError(COULD_NOT_LOAD_OBJECTS + e.getMessage());
        }

        return list;
    }


    private void getGsonResultSet(Gson gson, String json, List<T> list) {
        try
        {
            T gsonResult = gson.fromJson(json, this.dataObject);

            if (gsonResult != null)
            {
                list.add(gsonResult);
            }
        }
        catch (JsonSyntaxException ex)
        {
            this.plugin.logError(COULD_NOT_LOAD_OBJECT + ex.getMessage());
            this.plugin.logError(json);
        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public T loadObject(@NonNull String uniqueId)
    {
        T result = null;
        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(this.sqlConfig.getLoadObjectSQL()))
        {
            // UniqueId needs to be placed in quotes?
            preparedStatement.setString(1, this.sqlConfig.isUseQuotes() ? "\"" + uniqueId + "\"" : uniqueId);
            result = getObject(uniqueId, preparedStatement);
        }
        catch (SQLException e)
        {
            this.plugin.logError(COULD_NOT_LOAD_OBJECT + uniqueId + " " + e.getMessage());
        }

        return result;
    }


    /**
     * Return the object decoded from JSON or null if there is an error
     * @param uniqueId - unique Id of object used in error reporting
     * @param preparedStatement - database statement to execute
     * @return T
     */
    private T getObject(@NonNull String uniqueId, PreparedStatement preparedStatement) {
        try (ResultSet resultSet = preparedStatement.executeQuery())
        {
            if (resultSet.next())
            {
                // If there is a result, we only want/need the first one
                Gson gson = this.getGson();
                return gson.fromJson(resultSet.getString("json"), this.dataObject);
            }
        }
        catch (Exception e)
        {
            this.plugin.logError(COULD_NOT_LOAD_OBJECT + uniqueId + " " + e.getMessage());
        }
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Boolean> saveObject(T instance)
    {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        // Null check
        if (instance == null)
        {
            this.plugin.logError("SQL database request to store a null. ");
            completableFuture.complete(false);
            return completableFuture;
        }

        if (!(instance instanceof DataObject))
        {
            this.plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            completableFuture.complete(false);
            return completableFuture;
        }

        // This has to be on the main thread to avoid concurrent modification errors
        String toStore = this.getGson().toJson(instance);

        if (this.plugin.isEnabled())
        {
            // Async
            this.processQueue.add(() -> store(completableFuture,
                    instance.getClass().getName(),
                    toStore,
                    this.sqlConfig.getSaveObjectSQL(),
                    true));
        }
        else
        {
            // Sync
            this.store(completableFuture, instance.getClass().getName(), toStore, this.sqlConfig.getSaveObjectSQL(), false);
        }

        return completableFuture;
    }


    /**
     * This method is called to save data into database based on given parameters.
     * @param completableFuture Failsafe on saving data.
     * @param name Name of the class that is saved.
     * @param toStore data that is stored.
     * @param storeSQL SQL command for saving.
     * @param async boolean that indicates if saving is async or not.
     */
    private void store(CompletableFuture<Boolean> completableFuture, String name, String toStore, String storeSQL, boolean async)
    {
        // Do not save anything if plug is disabled and this was an async request
        if (async && !this.plugin.isEnabled())
        {
            return;
        }

        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(storeSQL))
        {
            preparedStatement.setString(1, toStore);
            preparedStatement.setString(2, toStore);
            preparedStatement.execute();
            completableFuture.complete(true);
        }
        catch (SQLException e)
        {
            this.plugin.logError("Could not save object " + name + " " + e.getMessage());
            completableFuture.complete(false);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteID(String uniqueId)
    {
        this.processQueue.add(() -> this.delete(uniqueId));
    }


    /**
     * This method triggers object deletion from the database.
     * @param uniqueId Object unique id.
     */
    private void delete(String uniqueId)
    {
        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(this.sqlConfig.getDeleteObjectSQL()))
        {
            // UniqueId needs to be placed in quotes?
            preparedStatement.setString(1, this.sqlConfig.isUseQuotes() ? "\"" + uniqueId + "\"" : uniqueId);
            preparedStatement.execute();
        }
        catch (Exception e)
        {
            this.plugin.logError("Could not delete object " + this.plugin.getSettings().getDatabasePrefix() +
                    this.dataObject.getCanonicalName() + " " + uniqueId + " " + e.getMessage());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteObject(T instance)
    {
        // Null check
        if (instance == null)
        {
            this.plugin.logError("SQL database request to delete a null.");
            return;
        }

        if (!(instance instanceof DataObject))
        {
            this.plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }

        try
        {
            Method getUniqueId = this.dataObject.getMethod("getUniqueId");
            this.deleteID((String) getUniqueId.invoke(instance));
        }
        catch (Exception e)
        {
            this.plugin.logError("Could not delete object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean objectExists(String uniqueId)
    {
        // Query to see if this key exists
        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(this.sqlConfig.getObjectExistsSQL()))
        {
            // UniqueId needs to be placed in quotes?
            preparedStatement.setString(1, this.sqlConfig.isUseQuotes() ? "\"" + uniqueId + "\"" : uniqueId);

            try (ResultSet resultSet = preparedStatement.executeQuery())
            {
                if (resultSet.next())
                {
                    return resultSet.getBoolean(1);
                }
            }
        }
        catch (SQLException e)
        {
            this.plugin.logError("Could not check if key exists in database! " + uniqueId + " " + e.getMessage());
        }

        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close()
    {
        this.shutdown = true;
    }


    /**
     * Sets data source of database.
     *
     * @param dataSource the data source
     * @return {@code true} if data source is set, {@code false} otherwise.
     */
    public boolean setDataSource(DataSource dataSource)
    {
        if (dataSource == null)
        {
            this.plugin.logError("Could not connect to the database. Are the credentials in the config.yml file correct?");
            this.plugin.logWarning("Disabling the plugin...");
            Bukkit.getPluginManager().disablePlugin(this.plugin);
            return false;
        }
        this.dataSource = dataSource;
        return true;
    }
}
