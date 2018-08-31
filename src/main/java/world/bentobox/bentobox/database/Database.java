package world.bentobox.bentobox.database;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;

/**
 * Handy class to store and load Java POJOs in the Database
 * @author tastybento
 *
 * @param <T>
 */
public class Database<T> {

    private AbstractDatabaseHandler<T> handler;
    private Logger logger;

    /**
     * Construct a database
     * @param plugin - plugin
     * @param type - to store this type
     */
    public Database(BentoBox plugin, Class<T> type)  {
        this.logger = plugin.getLogger();
        handler = DatabaseSetup.getDatabase().getHandler(type);
    }

    /**
     * Construct a database
     * @param addon - addon requesting
     * @param type - to store this type
     */
    public Database(Addon addon, Class<T> type)  {
        this.logger = addon.getLogger();
        handler = DatabaseSetup.getDatabase().getHandler(type);
    }

    /**
     * Load all the config objects and supply them as a list
     * @return list of config objects or an empty list if they cannot be loaded
     */
    public List<T> loadObjects() {
        List<T> result = new ArrayList<>();
        try {
            result = handler.loadObjects();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | ClassNotFoundException | IntrospectionException
                | NoSuchMethodException | SecurityException e) {
            logger.severe(() -> "Could not load objects from database! Error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Loads the config object
     * @param uniqueId - unique id of the object
     * @return the object or null if it cannot be loaded
     */
    public T loadObject(String uniqueId) {
        T result = null;
        try {
            result = handler.loadObject(uniqueId);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | ClassNotFoundException | IntrospectionException | NoSuchMethodException | SecurityException e) {
            logger.severe(() -> "Could not load object from database! " + e.getMessage());
        }
        return result;
    }

    /**
     * Save config object
     * @param instance to save
     * @return true if successfully saved
     */
    public boolean saveObject(T instance) {
        try {
            handler.saveObject(instance);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
                | IntrospectionException e) {
            logger.severe(() -> "Could not save object to database! Error: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Checks if a config object exists or not
     * @param name - unique name of the config object
     * @return true if it exists
     */
    public boolean objectExists(String name) {
        return handler.objectExists(name);
    }

    /**
     * Delete object from database
     * @param object - object to delete
     */
    public void deleteObject(T object) {
        try {
            handler.deleteObject(object);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | IntrospectionException e) {
            logger.severe(() -> "Could not delete config! Error: " + e.getMessage());
        }
    }

    /**
     * Close the database
     */
    public void close() {
        handler.close();
    }

}