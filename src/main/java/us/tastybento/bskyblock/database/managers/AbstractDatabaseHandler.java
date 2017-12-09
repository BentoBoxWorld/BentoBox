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
     * The type of the objects that should be created and filled with values
     * from the database or inserted into the database
     */
    protected Class<T>     type;

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
        this.type = type;
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
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws IntrospectionException
     * @throws SecurityException
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public abstract List<T> loadObjects() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, SecurityException, ClassNotFoundException;

    /**
     * Creates a <T> filled with values from the corresponding
     * database file
     * @param uniqueId
     * @return <T>
     * @throws IntrospectionException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws SecurityException
     */
    public abstract T loadObject(String uniqueId) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, SecurityException, ClassNotFoundException;

    /**
     * Save T into the corresponding database
     *
     * @param instance that should be inserted into the database
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws IntrospectionException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws SQLException
     * @throws NoSuchMethodException
     */
    public abstract void saveObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, SecurityException, InstantiationException, NoSuchMethodException;

    /**
     * Deletes the object with the unique id from the database
     * @param instance
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws IntrospectionException
     * @throws SQLException
     * @throws SecurityException
     * @throws NoSuchMethodException
     */
    public abstract void deleteObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, SQLException, NoSuchMethodException, SecurityException;

    /**
     * Checks if a key exists or not
     * @param key
     * @return true if this key exists
     */
    public abstract boolean objectExits(String key);

}
