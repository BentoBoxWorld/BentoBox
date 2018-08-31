package world.bentobox.bentobox.api.configuration;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.flatfile.FlatFileDatabase;

/**
 * Handy config class to store and load Java POJOs as YAML configs
 * @author tastybento
 *
 * @param <T>
 */
public class Config<T> {

    private AbstractDatabaseHandler<T> handler;
    private Logger logger;

    public Config(BentoBox plugin, Class<T> type)  {
        this.logger = plugin.getLogger();
        handler = new FlatFileDatabase().getConfig(type);
    }

    public Config(Addon addon, Class<T> type)  {
        this.logger = addon.getLogger();
        handler = new FlatFileDatabase().getConfig(type);
    }

    /**
     * Load all the config objects and supply them as a list
     * @return list of config objects or an empty list if they cannot be loaded
     */
    public List<T> loadConfigObjects() {
        List<T> result = new ArrayList<>();
        try {
            result = handler.loadObjects();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | ClassNotFoundException | IntrospectionException
                | NoSuchMethodException | SecurityException e) {
            logger.severe(() -> "Could not load config! Error: " + e.getMessage());
        }
        return result;
    }

    /**
     * Loads the config object
     * @param uniqueId - unique id of the object
     * @return the object or null if it cannot be loaded
     */
    public T loadConfigObject(String uniqueId) {
        try {
            return handler.loadObject(uniqueId);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | ClassNotFoundException | IntrospectionException | NoSuchMethodException | SecurityException e) {
            logger.severe(() -> "Could not load config object! " + e.getMessage());
        }

        return null;
    }

    /**
     * Loads a config object
     * @return the object or null if it cannot be loaded
     */
    public T loadConfigObject() {
        return loadConfigObject("");
    }

    /**
     * Save config object
     * @param instance to save
     */
    public boolean saveConfigObject(T instance) {
        try {
            handler.saveObject(instance);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
                | IntrospectionException e) {
            logger.severe(() -> "Could not save config! Error: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Checks if a config object exists or not
     * @param name - unique name of the config object
     * @return true if it exists
     */
    public boolean configObjectExists(String name) {
        return handler.objectExists(name);
    }

}