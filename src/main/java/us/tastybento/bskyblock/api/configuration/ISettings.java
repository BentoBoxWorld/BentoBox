package us.tastybento.bskyblock.api.configuration;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.database.flatfile.FlatFileDatabase;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;

/**
 * Simple interface for tagging all classes containing ConfigEntries.
 *
 * @author Poslovitch
 * @param <T>
 */
public interface ISettings<T> {
    
    // ----------------Saver-------------------
    @SuppressWarnings("unchecked")
    default void saveConfig(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException, InstantiationException, NoSuchMethodException, IntrospectionException, SQLException {
        // Get the handler
        AbstractDatabaseHandler<T> configHandler = (AbstractDatabaseHandler<T>) new FlatFileDatabase().getHandler(BSkyBlock.getInstance(), instance.getClass());
        // Load every field in the config class
        configHandler.saveObject(instance); // The string parameter can be anything
    }
    // --------------- Loader ------------------
    @SuppressWarnings("unchecked")
    default Settings loadSettings() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, SecurityException, ClassNotFoundException, IntrospectionException, SQLException {
        // Get the handler
        AbstractDatabaseHandler<Settings> configHandler = (AbstractDatabaseHandler<Settings>) new FlatFileDatabase().getHandler(BSkyBlock.getInstance(), Settings.class);
        // Load every field in the config class
        return configHandler.loadObject("config");
    }
}
