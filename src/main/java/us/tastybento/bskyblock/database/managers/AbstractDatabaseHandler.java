package us.tastybento.bskyblock.database.managers;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;

import org.bukkit.plugin.Plugin;

import us.tastybento.bskyblock.database.DatabaseConnecter;

/**
 * An abstract class that handles insert/select-operations into/from a database
 *
 * @author tastybento
 *
 * @param <T>
 */
public abstract class AbstractDatabaseHandler<T> {

    /**
     * The data object that should be created and filled with values
     * from the database or inserted into the database
     */
    protected Class<T>     dataObject;

    /**
     * Contains the settings to create a connection to the database like
     * host/port/database/user/password
     */
    protected DatabaseConnecter     databaseConnecter;

    /** The SQL-select- and insert query */
    protected final String     selectQuery;
    protected final String     insertQuery;
    protected final String     deleteQuery;


    protected Plugin plugin;


    /**
     * Constructor
     *
     * @param type
     *            The type of the objects that should be created and filled with
     *            values from the database or inserted into the database
     * @param databaseConnecter
     *            Contains the settings to create a connection to the database
     *            like host/port/database/user/password
     */
    protected AbstractDatabaseHandler(Plugin plugin, Class<T> type, DatabaseConnecter databaseConnecter) {
        this.plugin = plugin;
        this.databaseConnecter = databaseConnecter;
        this.dataObject = type;
        this.selectQuery = createSelectQuery();
        this.insertQuery = createInsertQuery();
        this.deleteQuery = createDeleteQuery();
    }

    /**
     * Create the SQL-String to insert into / select / delete from the database
     * Not used in the flat file database
     * @return the SQL-String
     */
    protected abstract String createSelectQuery();
    protected abstract String createInsertQuery();
    protected abstract String createDeleteQuery();

    /**
     * Loads all the records in this table and returns a list of them
     * @return list of <T>
     */
    public abstract List<T> loadObjects() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, SecurityException, ClassNotFoundException;

    /**
     * Creates a <T> filled with values from the corresponding
     * database file
     * @param uniqueId - unique ID
     * @return <T>
     */
    public abstract T loadObject(String uniqueId) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, SecurityException, ClassNotFoundException;

    /**
     * Save T into the corresponding database
     *
     * @param instance that should be inserted into the database
     */
    public abstract void saveObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, SecurityException, InstantiationException, NoSuchMethodException;

    /**
     * Deletes the object with the unique id from the database
     * @param instance
     */
    public abstract void deleteObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, NoSuchMethodException, SecurityException;

    /**
     * Checks if a key exists or not
     * @param key
     * @return true if this key exists
     */
    public abstract boolean objectExists(String key);

    /**
     * Saves a file as settings
     * @param instance
     */
    public abstract void saveSettings(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException;

    /**
     * Loads a file as settings
     * @param uniqueId - unique ID
     * @param dbConfig - the database mirror of this object. It will be checked against what is loaded to see if any significant changes have been made
     * @return Settings object
     */
    public abstract T loadSettings(String uniqueId, T dbConfig) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, IntrospectionException;

}
