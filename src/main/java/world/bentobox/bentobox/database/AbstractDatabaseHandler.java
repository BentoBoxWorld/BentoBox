package world.bentobox.bentobox.database;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import world.bentobox.bentobox.BentoBox;

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
    protected Class<T> dataObject;

    /**
     * Contains the settings to create a connection to the database like
     * host/port/database/user/password
     */
    protected DatabaseConnector databaseConnector;

    protected BentoBox plugin;

    /**
     * Constructor
     *
     * @param type
     *            The type of the objects that should be created and filled with
     *            values from the database or inserted into the database
     * @param databaseConnector
     *            Contains the settings to create a connection to the database
     *            like host/port/database/user/password
     */
    protected AbstractDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        this.plugin = plugin;
        this.databaseConnector = databaseConnector;
        this.dataObject = type;
    }

    /**
     * Loads all the records in this table and returns a list of them
     * @return list of <T>
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     */
    public abstract List<T> loadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, IllegalArgumentException, NoSuchMethodException, SecurityException;

    /**
     * Creates a <T> filled with values from the corresponding
     * database file
     * @param uniqueId - unique ID
     * @return <T>
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     */
    public abstract T loadObject(String uniqueId) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, IllegalArgumentException, NoSuchMethodException, SecurityException;

    /**
     * Save T into the corresponding database
     *
     * @param instance that should be inserted into the database
     */
    public abstract void saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException ;

    /**
     * Deletes the object with the unique id from the database
     * @param instance - object instance
     */
    public abstract void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException ;

    /**
     * Checks if a unique id exists or not
     * @param uniqueId - uniqueId to check
     * @return true if this uniqueId exists
     */
    public abstract boolean objectExists(String uniqueId);

    /**
     * Closes the database
     */
    public abstract void close();
}
