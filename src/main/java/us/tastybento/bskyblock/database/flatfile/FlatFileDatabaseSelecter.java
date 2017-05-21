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
 * Class that creates a list of <T>s filled with values from the corresponding
 * database-table.
 * 
 * @author tastybento
 * 
 * @param <T>
 */
public class FlatFileDatabaseSelecter<T> extends AbstractDatabaseHandler<T> {

    public FlatFileDatabaseSelecter(BSkyBlock plugin, Class<T> type,
            DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
    }

    @Override
    protected String createQuery() {
        return "";
    }

    /**
     * Creates a <T> filled with values from the corresponding
     * database file
     * 
     * @return <T> filled with values from the corresponding database file
     * @throws IntrospectionException 
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    public T selectObject() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException  {
        YamlConfiguration config = databaseConnecter.loadYamlFile(type.getSimpleName());
        return createObject(config);
    }

    /**
     * 
     * Creates a list of <T>s filled with values from the provided ResultSet
     * 
     * @param config - YAML config file
     * 
     * @return <T> filled with values
     * 
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IntrospectionException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    private T createObject(YamlConfiguration config) throws InstantiationException, IllegalAccessException,
    IntrospectionException, IllegalArgumentException, InvocationTargetException
             {

        T instance = type.newInstance();

        for (Field field : type.getDeclaredFields()) {

            /* We assume the table-column-names exactly match the variable-names of T */
            // TODO: depending on the data type, it'll need deserializing
            Object value = config.get(field.getName());

            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                    field.getName(), type);

            Method method = propertyDescriptor.getWriteMethod();

            method.invoke(instance, value);
        }

        return instance;
    }
}
