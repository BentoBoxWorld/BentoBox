package us.tastybento.bskyblock.database;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.addons.Addon;

/**
 * Handy class to store and load Java POJOs in the BSkyBlock Database
 * @author tastybento
 *
 * @param <T>
 */
public class BSBDatabase<T> {

    private AbstractDatabaseHandler<T> handler;
    private Logger logger;

    @SuppressWarnings("unchecked")
    public BSBDatabase(BSkyBlock plugin, Class<T> type)  {
        this.logger = plugin.getLogger();
        handler = (AbstractDatabaseHandler<T>) BSBDbSetup.getDatabase().getHandler(type);
    }

    @SuppressWarnings("unchecked")
    public BSBDatabase(Addon addon, Class<T> type)  {
        this.logger = addon.getLogger();
        handler = (AbstractDatabaseHandler<T>) BSBDbSetup.getDatabase().getHandler(type);
       
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
                | InvocationTargetException | ClassNotFoundException | IntrospectionException e) {
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

        try {
            return handler.loadObject(uniqueId);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | ClassNotFoundException | IntrospectionException e) {
            logger.severe(() -> "Could not load object from database! " + e.getMessage());
        }

        return null;
    }

    /**
     * Save config object
     * @param instance to save
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