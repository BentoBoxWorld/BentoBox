package us.tastybento.bskyblock.database.flatfile;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.DatabaseConnecter;

/**
 * 
 * Class that inserts a list of <T>s into the corresponding YAML file.
 * 
 * @author tastybento
 * 
 * @param <T>
 */
public class FlatFileDatabaseInserter<T> extends AbstractDatabaseHandler<T> {


    public FlatFileDatabaseInserter(BSkyBlock plugin, Class<T> type, DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
    }

    @Override
    protected String createQuery() {
        // Not used for flat file
        return "";
    }

    /**
     * Inserts T into the corresponding database-table
     * 
     * @param instance that should be inserted into the database
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws IntrospectionException
     */
    public void insertObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
        YamlConfiguration config = databaseConnecter.loadYamlFile(type.getSimpleName());
        for (Field field : type.getDeclaredFields()) {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);

            Method method = propertyDescriptor.getReadMethod();

            Object value = method.invoke(instance);

            // TODO: depending on the type, it'll need serializing
            config.set(field.getName(), value);

        }
        databaseConnecter.saveYamlFile(config, type.getSimpleName());

    }


}
