package world.bentobox.bentobox.api.configuration;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.yaml.YamlDatabase;

/**
 * Handy config class to store and load Java POJOs as YAML configs
 * @author tastybento
 *
 * @param <T>
 */
public class Config<T> {

    private AbstractDatabaseHandler<T> handler;
    private Logger logger;
    private Addon addon;

    public Config(BentoBox plugin, Class<T> type)  {
        this.logger = plugin.getLogger();
        handler = new YamlDatabase().getConfig(type);
    }

    public Config(Addon addon, Class<T> type)  {
        this.logger = addon.getLogger();
        this.addon = addon;
        handler = new YamlDatabase().getConfig(type);
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
    @Nullable
    public T loadConfigObject(String uniqueId) {
        try {
            return handler.loadObject(uniqueId);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | ClassNotFoundException | IntrospectionException | NoSuchMethodException | SecurityException e) {
            logger.severe(() -> "Could not load config object! " + e.getMessage());
            // Required for debugging
            logger.severe(ExceptionUtils.getStackTrace(e));
        }

        return null;
    }

    /**
     * Loads a config object
     * @return the object or null if it cannot be loaded
     */
    @Nullable
    public T loadConfigObject() {
        return loadConfigObject("");
    }

    /**
     * Save config object
     * @param instance to save
     */
    public boolean saveConfigObject(T instance) {
        // Set the addon (may be null)
        handler.setAddon(addon);
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