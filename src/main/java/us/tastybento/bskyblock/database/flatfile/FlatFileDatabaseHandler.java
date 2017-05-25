package us.tastybento.bskyblock.database.flatfile;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
public class FlatFileDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    private static final String DATABASE_FOLDER_NAME = "database";
    public FlatFileDatabaseHandler(BSkyBlock plugin, Class<T> type,
            DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
    }

    @Override
    protected String createSelectQuery() {
        // not used
        return "";
    }
    @Override
    protected String createInsertQuery() {
        // not used
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
    @Override
    public T selectObject(String key) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException  {
        YamlConfiguration config = databaseConnecter.loadYamlFile(type.getSimpleName(), key);
        return createObject(config);
    }

    /**
     * Loads all the records in this table and returns a list of them
     * @return list of <T>
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws IntrospectionException
     */
    @Override
    public List<T> selectObjects() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
        List<T> list = new ArrayList<T>();
        FilenameFilter ymlFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".yml")) {
                    return true;
                } else {
                    return false;
                }
            }
        };
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        File tableFolder = new File(dataFolder, type.getSimpleName());
        if (!tableFolder.exists()) {
            // Nothing there...
            tableFolder.mkdirs();
        }
        for (File file: tableFolder.listFiles(ymlFilter)) {
            YamlConfiguration config = databaseConnecter.loadYamlFile(type.getSimpleName(), file.getName());
            list.add(createObject(config));
        }
        return list;
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
            try {

                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
                Method method = propertyDescriptor.getWriteMethod();
                plugin.getLogger().info("DEBUG: " + field.getName() + ": " + propertyDescriptor.getPropertyType().getTypeName());
                if (propertyDescriptor.getPropertyType().equals(HashMap.class)) {
                    plugin.getLogger().info("DEBUG: is HashMap");
                    // TODO: this may not work with all keys. Further serialization may be required.
                    HashMap<Object,Object> value = new HashMap<Object, Object>();
                    for (String key : config.getConfigurationSection(field.getName()).getKeys(false)) {
                        value.put(key, config.get(field.getName() + "." + key));
                    }
                    method.invoke(instance, value);
                } else if (propertyDescriptor.getPropertyType().equals(Set.class)) {
                    plugin.getLogger().info("DEBUG: is Set " + propertyDescriptor.getReadMethod().getGenericReturnType().getTypeName());

                    // TODO: this may not work with all keys. Further serialization may be required.
                    Set<Object> value = new HashSet((List<Object>) config.getList(field.getName()));
                    
                    method.invoke(instance, value);
                } else if (propertyDescriptor.getPropertyType().equals(UUID.class)) {
                    plugin.getLogger().info("DEBUG: is UUID");
                    String uuid = (String)config.get(field.getName());
                    if (uuid == null || uuid.equals("null")) {
                        method.invoke(instance, (Object)null); 
                    } else {
                        Object value = UUID.fromString(uuid);
                        method.invoke(instance, value);
                    }
                } else {
                    Object value = config.get(field.getName());
                    method.invoke(instance, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return instance;
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
    @SuppressWarnings("unchecked")
    @Override
    public void insertObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
        // This is the Yaml Configuration that will be used and saved at the end
        YamlConfiguration config = new YamlConfiguration();
        // The file name of the Yaml file.
        String filename = "";
        // Run through all the fields in the class that is being stored. EVERY field must have a get and set method
        for (Field field : type.getDeclaredFields()) {
            // Get the property descriptor for this field
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
            // Get the read method, i.e., getXXXX();
            Method method = propertyDescriptor.getReadMethod();
            // Invoke the read method to get the value. We have no idea what type of value it is.
            Object value = method.invoke(instance);
            plugin.getLogger().info("DEBUG: writing " + field.getName());
            plugin.getLogger().info("DEBUG: property desc = " + propertyDescriptor.getPropertyType().getTypeName());
            // Depending on the vale type, it'll need serializing differenty
            // Check if this field is the mandatory UniqueId field. This is used to identify this instantiation of the class
            if (method.getName().equals("getUniqueId")) {
                // If the object does not have a unique name assigned to it already, one is created at random
                plugin.getLogger().info("DEBUG: uniqueId = " + value);
                String id = (String)value;
                if (id.isEmpty()) {
                    id = databaseConnecter.getUniqueId(type.getSimpleName());
                    // Set it in the class so that it will be used next time
                    propertyDescriptor.getWriteMethod().invoke(instance, id);
                }
                // Save the name for when the file is saved
                filename = id;
            }
            // UUID's need special serialization
            if (propertyDescriptor.getPropertyType().equals(UUID.class)) {
                plugin.getLogger().info("DEBUG: writing UUID for " + field.getName());
                if (value != null) {
                    config.set(field.getName(), ((UUID)value).toString());
                } else {
                    // UUID's can be null, so they need to be saved as the string "null"
                    config.set(field.getName(), "null");
                }
            } else if (propertyDescriptor.getPropertyType().equals(Set.class)) {
                // Sets need to be serialized as string lists
                plugin.getLogger().info("DEBUG: Set for " + field.getName());

                List<Object> list = new ArrayList<Object>();
                for (Object object : (Set<Object>)value) {
                    if (object instanceof UUID) {
                        list.add(((UUID)object).toString());
                    }
                }
                // Save the list in the config file
                config.set(field.getName(), list);
            } else {
                // For all other data that doesn't need special serialization
                config.set(field.getName(), value);
            }
        }
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("No UUID in class");
        }
        // Save the file in the right folder
        databaseConnecter.saveYamlFile(config, type.getSimpleName(), filename);

    }

}
