package world.bentobox.bentobox.database.yaml;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.database.DatabaseConnector;

/**
 * Handles config settings saving and loading.
 *
 * @author tastybento
 *
 * @param <T> Handles config files for Class <T>
 */

public class ConfigHandler<T> extends YamlDatabaseHandler<T> {

    public ConfigHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector);
        if (!ConfigObject.class.isAssignableFrom(type)) {
            throw new java.lang.ClassFormatError("Config classes must implement ConfigObject");
        }
    }

    public void saveSettings(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // ConfigObject check
        if (!(instance instanceof ConfigObject)) {
            throw new java.lang.ClassFormatError("Config classes must implement ConfigObject");
        }
        configFlag = true;
        saveObject(instance);
    }

    public T loadSettings(String uniqueId, T dbConfig) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {

        // TODO: compare the loaded with the database copy

        return loadObject(uniqueId);
    }

}
