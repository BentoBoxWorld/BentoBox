package us.tastybento.bskyblock.database.flatfile;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.file.Files;
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
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.Constants.GameType;
import us.tastybento.bskyblock.api.configuration.ConfigComment;
import us.tastybento.bskyblock.api.configuration.ConfigEntry;
import us.tastybento.bskyblock.api.configuration.StoreAt;
import us.tastybento.bskyblock.database.AbstractDatabaseHandler;
import us.tastybento.bskyblock.database.DatabaseConnecter;
import us.tastybento.bskyblock.database.objects.adapters.Adapter;
import us.tastybento.bskyblock.database.objects.adapters.AdapterInterface;
import us.tastybento.bskyblock.util.Util;

/**
 * Class that creates a list of <T>s filled with values from the corresponding
 * database-table.
 *
 * @author tastybento
 *
 * @param <T> Handles flat files for Class <T>
 */

public class FlatFileDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    private static final String DATABASE_FOLDER_NAME = "database";
    protected boolean configFlag;

    public FlatFileDatabaseHandler(BSkyBlock plugin, Class<T> type, DatabaseConnecter dbConnecter) {
        super(plugin, type, dbConnecter);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#loadObject(java.lang.String)
     */
    @Override
    public T loadObject(String key) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException {
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();
        String fileName = key;
        StoreAt storeAt = dataObject.getAnnotation(StoreAt.class);
        if (storeAt != null) {
            path = storeAt.path();
            fileName = storeAt.filename();
        }
        YamlConfiguration config = databaseConnecter.loadYamlFile(path, fileName);
        return createObject(config);
    }

    @Override
    public boolean objectExists(String key) {
        return databaseConnecter.uniqueIdExists(dataObject.getSimpleName(), key);
    }


    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.database.managers.AbstractDatabaseHandler#loadObjects()
     */
    @Override
    public List<T> loadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException {
        List<T> list = new ArrayList<>();
        FilenameFilter ymlFilter = (dir, name) ->  name.toLowerCase().endsWith(".yml");
        String path = dataObject.getSimpleName();
        StoreAt storeAt = dataObject.getAnnotation(StoreAt.class);
        if (storeAt != null) {
            path = storeAt.path();
        }
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        File tableFolder = new File(dataFolder, path);
        if (!tableFolder.exists()) {
            // Nothing there...
            tableFolder.mkdirs();
        }
        for (File file: tableFolder.listFiles(ymlFilter)) {
            String fileName = file.getName();
            if (storeAt != null) {
                fileName = storeAt.filename();
            }
            YamlConfiguration config = databaseConnecter.loadYamlFile(DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName(), fileName);
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
     */
    private T createObject(YamlConfiguration config) throws InstantiationException, IllegalAccessException, IntrospectionException, InvocationTargetException, ClassNotFoundException {
        T instance = dataObject.newInstance();

        // Run through all the fields in the object
        for (Field field : dataObject.getDeclaredFields()) {
            // Gets the getter and setters for this field
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
            // Get the write method
            Method method = propertyDescriptor.getWriteMethod();
            String storageLocation = field.getName();
            // Check if there is an annotation on the field
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
            // If there is a config annotation then do something
            if (configEntry != null) {
                if (!configEntry.path().isEmpty()) {
                    storageLocation = configEntry.path();
                }
                if (!configEntry.specificTo().equals(GameType.BOTH) && !configEntry.specificTo().equals(Constants.GAMETYPE)) {
                    continue;
                }
                // TODO: Add handling of other ConfigEntry elements
            }
            Adapter adapterNotation = field.getAnnotation(Adapter.class);
            if (adapterNotation != null && AdapterInterface.class.isAssignableFrom(adapterNotation.value())) {
                // A conversion adapter has been defined
                Object value = config.get(storageLocation);
                method.invoke(instance, ((AdapterInterface<?,?>)adapterNotation.value().newInstance()).serialize(value));
                if (value != null && !value.getClass().equals(MemorySection.class)) {
                    method.invoke(instance, deserialize(value,propertyDescriptor.getPropertyType()));
                }
                // We are done here
                continue;
            }

            // Look in the YAML Config to see if this field exists (it should)
            if (config.contains(storageLocation)) {
                // Check for null values
                if (config.get(storageLocation) == null) {
                    method.invoke(instance, (Object)null);
                    continue;
                }
                // Handle storage of maps. Check if this type is a Map
                if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Note that we have no idea what type this is
                    List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                    // collectionTypes should be 2 long
                    Type keyType = collectionTypes.get(0);
                    Type valueType = collectionTypes.get(1);
                    // TODO: this may not work with all keys. Further serialization may be required.
                    Map<Object,Object> value = new HashMap<>();
                    if (config.getConfigurationSection(storageLocation) != null) {
                        for (String key : config.getConfigurationSection(storageLocation).getKeys(false)) {
                            // Keys cannot be null - skip if they exist
                            Object mapKey = deserialize(key,Class.forName(keyType.getTypeName()));
                            if (mapKey == null) {
                                continue;
                            }
                            // Map values can be null - it is allowed here
                            Object mapValue = deserialize(config.get(storageLocation + "." + key), Class.forName(valueType.getTypeName()));
                            value.put(mapKey, mapValue);
                        }
                    }
                    method.invoke(instance, value);
                } else if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Loop through the collection resultset
                    // Note that we have no idea what type this is
                    List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                    // collectionTypes should be only 1 long
                    Type setType = collectionTypes.get(0);
                    Set<Object> value = new HashSet<>();
                    for (Object listValue: config.getList(storageLocation)) {
                        value.add(deserialize(listValue,Class.forName(setType.getTypeName())));
                    }

                    // TODO: this may not work with all keys. Further serialization may be required.
                    method.invoke(instance, value);
                } else if (List.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Loop through the collection resultset
                    // Note that we have no idea what type this is
                    List<Type> collectionTypes = Util.getCollectionParameterTypes(method);
                    // collectionTypes should be only 1 long
                    Type setType = collectionTypes.get(0);
                    List<Object> value = new ArrayList<>();
                    if (config.getList(storageLocation) != null) {
                        for (Object listValue: config.getList(storageLocation)) {
                            value.add(deserialize(listValue,Class.forName(setType.getTypeName())));
                        }
                    }
                    // TODO: this may not work with all keys. Further serialization may be required.
                    method.invoke(instance, value);
                } else {
                    // Not a collection
                    Object value = config.get(storageLocation);
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
     */
    @SuppressWarnings("unchecked")
    @Override
    public void saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {


        // This is the Yaml Configuration that will be used and saved at the end
        YamlConfiguration config = new YamlConfiguration();

        // The file name of the Yaml file.
        String filename = "";
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();
        // Comments for the file
        Map<String, String> yamlComments = new HashMap<>();

        // Only allow storing in an arbitrary place if it is a config object. Otherwise it is in the database
        if (configFlag) {
            StoreAt storeAt = instance.getClass().getAnnotation(StoreAt.class);
            if (storeAt != null) {
                path = storeAt.path();
                filename = storeAt.filename();
            }
        }

        // Run through all the fields in the class that is being stored. EVERY field must have a get and set method
        fields:
            for (Field field : dataObject.getDeclaredFields()) {

                // Get the property descriptor for this field
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
                // Get the read method, i.e., getXXXX();
                Method method = propertyDescriptor.getReadMethod();
                // Invoke the read method to get the value. We have no idea what type of value it is.
                Object value = method.invoke(instance);
                String storageLocation = field.getName();
                // Check if there is an annotation on the field
                ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
                // If there is a config path annotation then do something
                if (configEntry != null) {
                    if (!configEntry.specificTo().equals(GameType.BOTH) && !configEntry.specificTo().equals(Constants.GAMETYPE)) {
                        continue fields;
                    }
                    if (!configEntry.path().isEmpty()) {
                        storageLocation = configEntry.path();
                    }
                    // TODO: add in game-specific saving

                }
                
                // Comments          
                ConfigComment comment = field.getAnnotation(ConfigComment.class);
                if (comment != null) {
                    // Create a random placeholder string
                    String random = "comment-" + UUID.randomUUID().toString();
                    // Store placeholder
                    config.set(random, " ");
                    // Create comment
                    yamlComments.put(random, "# " + comment.value());
                }

                // Adapter
                Adapter adapterNotation = field.getAnnotation(Adapter.class);
                if (adapterNotation != null && AdapterInterface.class.isAssignableFrom(adapterNotation.value())) {
                    // A conversion adapter has been defined
                    try {
                        config.set(storageLocation, ((AdapterInterface<?,?>)adapterNotation.value().newInstance()).deserialize(value));
                    } catch (InstantiationException e) {
                        plugin.logError("Could not instatiate adapter " + adapterNotation.value().getName() + " " + e.getMessage());
                    }
                    // We are done here
                    continue fields;
                }
                
                // Depending on the vale type, it'll need serializing differently
                // Check if this field is the mandatory UniqueId field. This is used to identify this instantiation of the class
                if (method.getName().equals("getUniqueId")) {
                    // If the object does not have a unique name assigned to it already, one is created at random
                    String id = (String)value;
                    if (value == null || id.isEmpty()) {
                        id = databaseConnecter.getUniqueId(dataObject.getSimpleName());
                        // Set it in the class so that it will be used next time
                        propertyDescriptor.getWriteMethod().invoke(instance, id);
                    }
                    // Save the name for when the file is saved
                    if (filename.isEmpty()) {
                        filename = id;
                    }
                }
                // Collections need special serialization
                if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Maps need to have keys serialized
                    if (value != null) {
                        Map<Object, Object> result = new HashMap<>();
                        for (Entry<Object, Object> object : ((Map<Object,Object>)value).entrySet()) {
                            // Serialize all key and values
                            result.put(serialize(object.getKey()), serialize(object.getValue()));
                        }
                        // Save the list in the config file
                        config.set(storageLocation, result);
                    }
                } else if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Sets need to be serialized as string lists
                    if (value != null) {
                        List<Object> list = new ArrayList<>();
                        for (Object object : (Set<Object>)value) {
                            list.add(serialize(object));
                        }
                        // Save the list in the config file
                        config.set(storageLocation, list);
                    }
                } else {
                    // For all other data that doesn't need special serialization
                    config.set(storageLocation, serialize(value));
                }
            }
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("No uniqueId in class");
        }
        
        databaseConnecter.saveYamlFile(config, path, filename, yamlComments);
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object deserialize(Object value, Class<? extends Object> clazz) {
        // If value is already null, then it can be nothing else
        if (value == null) {
            return null;
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
            return Long.valueOf((Integer) value);
        }
        if (clazz.equals(Integer.class) && value.getClass().equals(String.class)) {
            return Integer.valueOf((String)value);
        }
        if (clazz.equals(Long.class) && value.getClass().equals(String.class)) {
            return Long.valueOf((String)value);
        }
        if (clazz.equals(Double.class) && value.getClass().equals(String.class)) {
            return Double.valueOf((String)value);
        }
        if (clazz.equals(Float.class) && value.getClass().equals(String.class)) {
            return Float.valueOf((String)value);
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
        if (Enum.class.isAssignableFrom(clazz)) {
            //Custom enums are a child of the Enum class.
            // Find out the value
            Class<Enum> enumClass = (Class<Enum>)clazz;
            try {
                value = Enum.valueOf(enumClass, (String)value);
            } catch (Exception e) {
                // This value does not exist - probably admin typed it wrongly
                // Show what is available and pick one at random
                plugin.logError("Error in YML file: " + value + " is not a valid value in the enum " + clazz.getCanonicalName() + "!");
                plugin.logError("Options are : ");
                boolean isSet = false;
                for (Field fields : enumClass.getFields()) {
                    plugin.logError(fields.getName());
                    if (!isSet && !((String)value).isEmpty() && fields.getName().substring(0, 1).equals(((String)value).substring(0, 1))) {
                        value = Enum.valueOf(enumClass, fields.getName());
                        plugin.logError("Setting to " + fields.getName() + " because it starts with the same letter");
                        isSet = true;
                    }
                }
            }
        }
        return value;
    }

    @Override
    public void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // The file name of the Yaml file.
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor("uniqueId", dataObject);
        Method method = propertyDescriptor.getReadMethod();
        String fileName = (String) method.invoke(instance);
        if (!fileName.endsWith(".yml")) {
            fileName = fileName + ".yml";
        }
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        File tableFolder = new File(dataFolder, dataObject.getSimpleName());
        if (tableFolder.exists()) {

            File file = new File(tableFolder, fileName);
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                plugin.logError("Could not delete yaml database object! " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        // Not used
        
    }

}
