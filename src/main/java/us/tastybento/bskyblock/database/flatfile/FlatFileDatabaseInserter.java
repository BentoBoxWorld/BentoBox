package us.tastybento.bskyblock.database.flatfile;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
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
            plugin.getLogger().info("DEBUG: writing " + field.getName());
            plugin.getLogger().info("DEBUG: property desc = " + propertyDescriptor.getPropertyType().getTypeName());
            // TODO: depending on the type, it'll need serializing
            if (propertyDescriptor.getPropertyType().equals(UUID.class)) {
                plugin.getLogger().info("DEBUG: writing UUID for " + field.getName());
                if (value != null) {
                    config.set(field.getName(), ((UUID)value).toString());
                } else {
                    config.set(field.getName(), "null");
                }
            } else if (propertyDescriptor.getPropertyType().equals(Set.class)) {
                plugin.getLogger().info("DEBUG: Hashset for " + field.getName());
                
                List<Object> list = new ArrayList<Object>();
                for (Object object : (Set<Object>)value) {
                    if (object instanceof UUID) {
                        list.add(((UUID)object).toString());
                    }
                }
                config.set(field.getName(), list);
            } else {
                config.set(field.getName(), value);
            }


        }
        databaseConnecter.saveYamlFile(config, type.getSimpleName());

    }
    /**
     * Saves a Map at the specified ConfigurationSection.
     * @param section the ConfigurationSection
     * @param map the Map, note that the String parameter in the map refers to the keys the objects are saved in
     * @throws IllegalArgumentException when either the ConfigurationSection or the Map is null
     */
    @SuppressWarnings("unchecked")
    public static void saveMap(final ConfigurationSection section, final Map<String, Object> map) throws IllegalArgumentException {
        if (section == null || map == null)
            throw new IllegalArgumentException("Both the configuration section and the map to save must not be null");

        final Iterator<Entry<String, Object>> iter = map.entrySet().iterator();

        while (iter.hasNext()) {
            final Entry<String, Object> entry = iter.next();
            final Object value = entry.getValue();
            final String key = entry.getKey();

            if (value instanceof Map) {
                saveMap(section.createSection(key), (Map<String, Object>) value);
            } else if (value instanceof Collection) {
                saveCollection(section, (Collection<Object>) value);
            } else {
                section.set(key, value);
            }
        }
    }

    /**
     * Saves a Collection at the specified ConfigurationSection.
     * @param section the ConfigurationSection
     * @param collection the Collection
     * @throws IllegalArgumentException when either the ConfigurationSection or the Collection is null
     */
    public static void saveCollection(final ConfigurationSection section, final Collection<Object> collection) throws IllegalArgumentException {
        if (section == null || collection == null)
            throw new IllegalArgumentException("Both the configuration section and the iterable object to save must not be null");

        final Iterator<Object> iter = collection.iterator();
        final String currentSectionPath = section.getCurrentPath();

        while (iter.hasNext()) {
            final Object value = iter.next();

            section.set(currentSectionPath, value);
        }
    }

}
