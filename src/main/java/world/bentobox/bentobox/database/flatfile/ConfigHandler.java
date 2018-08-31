package world.bentobox.bentobox.database.flatfile;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;

/**
 * Handles config settings saving and loading.
 *
 * @author tastybento
 *
 * @param <T> Handles config files for Class <T>
 */

public class ConfigHandler<T> extends FlatFileDatabaseHandler<T> {

    public ConfigHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector);
    }

    public void saveSettings(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        configFlag = true;
        saveObject(instance);
    }

    public T loadSettings(String uniqueId, T dbConfig) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, IllegalArgumentException, NoSuchMethodException, SecurityException {
        if (dbConfig == null) {
            return loadObject(uniqueId);
        }

        // TODO: compare the loaded with the database copy

        return loadObject(uniqueId);
    }

}
