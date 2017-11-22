package us.tastybento.bskyblock.database.flatfile;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler;
import us.tastybento.bskyblock.util.Util;

/**
 * Class that creates a list of <T>s filled with values from the corresponding
 * database-table.
 *
 * @author tastybento
 *
 * @param <T>
 */
public class FlatFileDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    private static final String DATABASE_FOLDER_NAME = "database";
    private static final boolean DEBUG = false;
    public FlatFileDatabaseHandler(BSkyBlock plugin, Class<T> type, DatabaseConnecter databaseConnecter) {
        super(plugin, type, databaseConnecter);
    }

    @Override
    protected String createSelectQuery() {
        return ""; // not used
    }
    @Override
    protected String createInsertQuery() {
        return ""; // not used
    }
    @Override
    protected String createDeleteQuery() {
        return ""; // Not used
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
     * @throws ClassNotFoundException
     */
    @Override
    public T loadObject(String key) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, ClassNotFoundException  {
        YamlConfiguration config = databaseConnecter.loadYamlFile(type.getSimpleName(), key);
        return createObject(config);
    }

    @Override
    public boolean objectExits(String key) {
        return databaseConnecter.uniqueIdExists(type.getSimpleName(), key);
    }

    /**
     * Loads all the records in this table and returns a list of them
     * @return list of <T>
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws IntrospectionException
     * @throws ClassNotFoundException
     */
    @Override
    public List<T> loadObjects() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException, ClassNotFoundException {
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
     * @throws ClassNotFoundException
     */
    private T createObject(YamlConfiguration config) throws InstantiationException, IllegalAccessException, IntrospectionException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException {
        T instance = type.newInstance();

        for (Field field : type.getDeclaredFields()) {

            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), type);
            Method method = propertyDescriptor.getWriteMethod();
            if (DEBUG)
                plugin.getLogger().info("DEBUG: " + field.getName() + ": " + propertyDescriptor.getPropertyType().getTypeName());
            if (config.contains(field.getName())) {
                if (propertyDescriptor.getPropertyType().equals(HashMap.class)) {

                    // Note that we have no idea what type this is
                    List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                    // collectionTypes should be 2 long
                    Type keyType = collectionTypes.get(0);
                    Type valueType = collectionTypes.get(1);
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: is HashMap<" + keyType.getTypeName() + ", " + valueType.getTypeName() + ">");
                    // TODO: this may not work with all keys. Further serialization may be required.
                    HashMap<Object,Object> value = new HashMap<Object, Object>();
                    for (String key : config.getConfigurationSection(field.getName()).getKeys(false)) {
                        Object mapKey = deserialize(key,Class.forName(keyType.getTypeName()));
                        Object mapValue = deserialize(config.get(field.getName() + "." + key), Class.forName(valueType.getTypeName()));
                        if (DEBUG) {
                            plugin.getLogger().info("DEBUG: mapKey = " + mapKey + " (" + mapKey.getClass().getCanonicalName() + ")");
                            plugin.getLogger().info("DEBUG: mapValue = " + mapValue + " (" + mapValue.getClass().getCanonicalName() + ")");
                        }
                        value.put(mapKey, mapValue);
                    }
                    method.invoke(instance, value);
                } else if (propertyDescriptor.getPropertyType().equals(Set.class)) {
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: is Set " + propertyDescriptor.getReadMethod().getGenericReturnType().getTypeName());
                        plugin.getLogger().info("DEBUG: adding a set");
                    }
                    // Loop through the collection resultset 
                    // Note that we have no idea what type this is
                    List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                    // collectionTypes should be only 1 long
                    Type setType = collectionTypes.get(0);
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: is HashSet<" + setType.getTypeName() + ">");
                    Set<Object> value = new HashSet<Object>();
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: collection type argument = " + collectionTypes);
                        plugin.getLogger().info("DEBUG: setType = " + setType.getTypeName());
                    }
                    for (Object listValue: config.getList(field.getName())) {
                        //plugin.getLogger().info("DEBUG: collectionResultSet size = " + collectionResultSet.getFetchSize());
                        ((Set<Object>) value).add(deserialize(listValue,Class.forName(setType.getTypeName())));
                    }

                    // TODO: this may not work with all keys. Further serialization may be required.
                    //Set<Object> value = new HashSet((List<Object>) config.getList(field.getName()));                    
                    method.invoke(instance, value);
                } else if (propertyDescriptor.getPropertyType().equals(ArrayList.class)) {
                    //plugin.getLogger().info("DEBUG: is Set " + propertyDescriptor.getReadMethod().getGenericReturnType().getTypeName());
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: adding a set");
                    // Loop through the collection resultset 
                    // Note that we have no idea what type this is
                    List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                    // collectionTypes should be only 1 long
                    Type setType = collectionTypes.get(0);
                    List<Object> value = new ArrayList<Object>();
                    //plugin.getLogger().info("DEBUG: collection type argument = " + collectionTypes);
                    //plugin.getLogger().info("DEBUG: setType = " + setType.getTypeName());
                    for (Object listValue: config.getList(field.getName())) {
                        //plugin.getLogger().info("DEBUG: collectionResultSet size = " + collectionResultSet.getFetchSize());
                        ((List<Object>) value).add(deserialize(listValue,Class.forName(setType.getTypeName())));
                    }
                    // TODO: this may not work with all keys. Further serialization may be required.
                    //Set<Object> value = new HashSet((List<Object>) config.getList(field.getName()));                    
                    method.invoke(instance, value);
                } else {
                    // Not a collection
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: not a collection");
                    Object value = config.get(field.getName());
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: value = " + value);
                        plugin.getLogger().info("DEBUG: property type = " + propertyDescriptor.getPropertyType());
                        plugin.getLogger().info("DEBUG: " + value.getClass());
                    }
                    if (value != null && !value.getClass().equals(MemorySection.class)) {
                        method.invoke(instance, deserialize(value,propertyDescriptor.getPropertyType()));
                    }
                }
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
    public void saveObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
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
            //plugin.getLogger().info("DEBUG: writing " + field.getName());
            //plugin.getLogger().info("DEBUG: property desc = " + propertyDescriptor.getPropertyType().getTypeName());
            // Depending on the vale type, it'll need serializing differenty
            // Check if this field is the mandatory UniqueId field. This is used to identify this instantiation of the class
            if (method.getName().equals("getUniqueId")) {
                // If the object does not have a unique name assigned to it already, one is created at random
                //plugin.getLogger().info("DEBUG: uniqueId = " + value);
                String id = (String)value;
                if (id.isEmpty()) {
                    id = databaseConnecter.getUniqueId(type.getSimpleName());
                    // Set it in the class so that it will be used next time
                    propertyDescriptor.getWriteMethod().invoke(instance, id);
                }
                // Save the name for when the file is saved
                filename = id;
            }
            // Collections need special serialization
            if (propertyDescriptor.getPropertyType().equals(HashMap.class) || propertyDescriptor.getPropertyType().equals(Map.class)) {
                // Maps need to have keys serialized
                //plugin.getLogger().info("DEBUG: Map for " + field.getName());
                Map<Object, Object> result = new HashMap<Object, Object>();
                for (Entry<Object, Object> object : ((Map<Object,Object>)value).entrySet()) {
                    // Serialize all key types
                    // TODO: also need to serialize values?
                    result.put(serialize(object.getKey()), object.getValue());
                }
                // Save the list in the config file
                config.set(field.getName(), result);
            } else if (propertyDescriptor.getPropertyType().equals(Set.class)) {
                // Sets need to be serialized as string lists
                //plugin.getLogger().info("DEBUG: Set for " + field.getName());
                List<Object> list = new ArrayList<Object>();
                for (Object object : (Set<Object>)value) {
                    list.add(serialize(object));
                }
                // Save the list in the config file
                config.set(field.getName(), list);
            } else {
                // For all other data that doesn't need special serialization
                config.set(field.getName(), serialize(value));
            }
        }
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("No uniqueId in class");
        }
        // Save the file in the right folder
        databaseConnecter.saveYamlFile(config, type.getSimpleName(), filename);

    }

    /**
     * Serialize an object if required
     * @param object
     * @return
     */
    private Object serialize(Object object) {
        if (object == null) {
            return "null";
        }
        //plugin.getLogger().info("DEBUG: serializing " + object.getClass().getTypeName());
        if (object instanceof UUID) {
            return ((UUID)object).toString();
        }
        if (object instanceof World) {
            return ((World)object).getName();
        }
        if (object instanceof Location) {
            return Util.getStringLocation((Location)object);
        }
        if (object instanceof Enum) {
            //Custom enums are a child of the Enum class. Just get the names of each one.
            return ((Enum<?>)object).name();
        }
        return object;
    }

    private Object deserialize(Object value, Class<? extends Object> clazz) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: deserialize - class is " + clazz.getCanonicalName());
            plugin.getLogger().info("DEBUG: value  is " + value);
            if (value != null)
                plugin.getLogger().info("DEBUG: value class  is " + value.getClass().getCanonicalName());
        }
        if (value instanceof String && value.equals("null")) {
            // If the value is null as a string, return null 
            return null;
        }
        // Bukkit may have deserialized the object already
        if (clazz.equals(value.getClass())) {
            return value;
        }
        // Types that need to be deserialized
        if (clazz.equals(Long.class) && value.getClass().equals(Integer.class)) {
            return new Long((Integer)value); 
        }
        if (clazz.equals(UUID.class)) {
            value = UUID.fromString((String)value);
        }
        // Bukkit Types
        if (clazz.equals(Location.class)) {
            // Get Location from String - may be null...
            value = Util.getLocationString(((String)value));
        }
        if (clazz.equals(World.class)) {
            // Get world by name - may be null...
            value = plugin.getServer().getWorld((String)value);
        }
        // Enums
        if (clazz.getSuperclass() != null && clazz.getSuperclass().equals(Enum.class)) {
            //Custom enums are a child of the Enum class.
            // Find out the value
            try {
                Class<Enum> enumClass = (Class<Enum>)clazz;
                value = Enum.valueOf(enumClass, (String)value);
            } catch (Exception e) {
                // Maybe this value does not exist?
                // TODO return something?
                e.printStackTrace();
            }
        }
        return value;
    }

    @Override
    public void deleteObject(T instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
        // The file name of the Yaml file.
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor("uniqueId", type);
        Method method = propertyDescriptor.getReadMethod();
        String fileName = (String) method.invoke(instance);
        if (!fileName.endsWith(".yml")) {
            fileName = fileName + ".yml";
        }
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        File tableFolder = new File(dataFolder, type.getSimpleName());
        if (tableFolder.exists()) {
            File file = new File(tableFolder, fileName);
            file.delete();
        }
    }

}
