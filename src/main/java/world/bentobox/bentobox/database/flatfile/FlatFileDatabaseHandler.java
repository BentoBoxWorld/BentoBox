package world.bentobox.bentobox.database.flatfile;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.StoreAt;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.adapters.Adapter;
import world.bentobox.bentobox.database.objects.adapters.AdapterInterface;
import world.bentobox.bentobox.util.Util;

/**
 * Class that creates a list of <T>s filled with values from the corresponding
 * database-table.
 *
 * @author tastybento
 *
 * @param <T> Handles flat files for Class <T>
 */

public class FlatFileDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    /**
     * This is the name of the folder where the flat file databases will live
     */
    private static final String DATABASE_FOLDER_NAME = "database";
    /**
     * Flag to indicate if this is a config or a pure object database (difference is in comments and annotations)
     */
    protected boolean configFlag;

    /**
     * Constructor
     * @param plugin - plugin
     * @param type - class to store in the database
     * @param databaseConnector - the database credentials, in this case, just the YAML functions
     */
    public FlatFileDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObject(java.lang.String)
     */
    @Override
    public T loadObject(String key) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, IllegalArgumentException, NoSuchMethodException, SecurityException {
        // Objects are loaded from a folder named after the simple name of the class being stored
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();
        // This path and key can be overridden by the StoreAt annotation in the code
        StoreAt storeAt = dataObject.getAnnotation(StoreAt.class);
        if (storeAt != null) {
            path = storeAt.path();
            key = storeAt.filename();
        }
        // Load the YAML file at the location.
        YamlConfiguration config = databaseConnector.loadYamlFile(path, key);
        // Use the createObject method to turn a YAML config into an Java object
        return createObject(config);
    }

    @Override
    public boolean objectExists(String uniqueId) {
        // Check if the uniqueId (key) exists in the file system
        return databaseConnector.uniqueIdExists(dataObject.getSimpleName(), uniqueId);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObjects()
     */
    @Override
    public List<T> loadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, IllegalArgumentException, NoSuchMethodException, SecurityException {
        // In this case, all the objects of a specific type are being loaded.
        List<T> list = new ArrayList<>();
        // Look for any files that end in .yml in the folder
        FilenameFilter ymlFilter = (dir, name) ->  name.toLowerCase(java.util.Locale.ENGLISH).endsWith(".yml");
        // The path is the simple name of the class
        String path = dataObject.getSimpleName();
        // The storeAt annotation may override the path
        StoreAt storeAt = dataObject.getAnnotation(StoreAt.class);
        if (storeAt != null) {
            path = storeAt.path();
        }
        // The database folder name is in the plugin's data folder
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        // The folder for the objects (tables in database terminology) is here
        File tableFolder = new File(dataFolder, path);
        if (!tableFolder.exists()) {
            // Nothing there...
            tableFolder.mkdirs();
        }
        // Load each object from the file system, filtered, non-null
        for (File file: Objects.requireNonNull(tableFolder.listFiles(ymlFilter))) {
            String fileName = file.getName();
            if (storeAt != null) {
                fileName = storeAt.filename();
            }
            YamlConfiguration config = databaseConnector.loadYamlFile(DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName(), fileName);
            list.add(createObject(config));
        }
        return list;
    }

    /**
     * Creates a list of <T>s filled with values from the provided YamlConfiguration
     *
     * @param config - YAML config file
     *
     * @return <T> filled with values
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     */
    private T createObject(YamlConfiguration config) throws InstantiationException, IllegalAccessException, IntrospectionException, InvocationTargetException, ClassNotFoundException, IllegalArgumentException, NoSuchMethodException, SecurityException {
        // Create a new instance of the dataObject of type T (which can be any class)
        T instance = dataObject.getDeclaredConstructor().newInstance();

        // Run through all the fields in the object
        for (Field field : dataObject.getDeclaredFields()) {
            // Gets the getter and setters for this field using the JavaBeans system
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
            // Get the write method
            Method method = propertyDescriptor.getWriteMethod();

            // Information about the field
            String storageLocation = field.getName();
            /*
             * Field annotation checks
             */
            // Check if there is a ConfigEntry annotation on the field
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);

            // If there is a config annotation then do something
            if (configEntry != null && !configEntry.path().isEmpty()) {
                storageLocation = configEntry.path();
            }
            // Some fields need custom handling to serialize or deserialize and the programmer will need to
            // define them herself. She can add an annotation to do that.
            Adapter adapterNotation = field.getAnnotation(Adapter.class);
            if (adapterNotation != null && AdapterInterface.class.isAssignableFrom(adapterNotation.value())) {
                // A conversion adapter has been defined
                // Get the original value to be stored
                Object value = config.get(storageLocation);
                // Invoke the deserialization on this value
                method.invoke(instance, ((AdapterInterface<?,?>)adapterNotation.value().getDeclaredConstructor().newInstance()).deserialize(value));
                // We are done here. If a custom adapter was defined, the rest of this method does not need to be run
                continue;
            }
            /*
             * What follows is general deserialization code
             */
            // Look in the YAML Config to see if this field exists (it should)
            if (config.contains(storageLocation)) {
                // Check for null values
                if (config.get(storageLocation) == null) {
                    method.invoke(instance, (Object)null);
                    continue;
                }
                // Handle storage of maps. Check if this field type is a Map
                if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Note that we have no idea what type of map this is, so we need to find out
                    List<Type> collectionTypes = getCollectionParameterTypes(method);
                    // collectionTypes should be 2 long because there are two parameters in a Map (key, value)
                    Type keyType = collectionTypes.get(0);
                    Type valueType = collectionTypes.get(1);
                    // Create a map that we'll put the values into
                    Map<Object,Object> value = new HashMap<>();
                    // Map values are stored in a configuration section in the YAML. Check that it exists
                    if (config.getConfigurationSection(storageLocation) != null) {
                        // Run through the values stored
                        for (String key : config.getConfigurationSection(storageLocation).getKeys(false)) {
                            // Map values can be null - it is allowed here
                            Object mapValue = deserialize(config.get(storageLocation + "." + key), Class.forName(valueType.getTypeName()));
                            // Keys cannot be null - skip if they exist
                            // Convert any serialized dots back to dots
                            // In YAML dots . cause a lot of problems, so I serialize them as :dot:
                            // There may be a better way to do this.
                            key = key.replaceAll(":dot:", ".");
                            Object mapKey = deserialize(key,Class.forName(keyType.getTypeName()));
                            if (mapKey == null) {
                                continue;
                            }
                            // Put the value in the map
                            value.put(mapKey, mapValue);
                        }
                    }
                    // Invoke the setter in the class (this is why JavaBeans requires getters and setters for every field)
                    method.invoke(instance, value);
                } else if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Note that we have no idea what type this set is
                    List<Type> collectionTypes = getCollectionParameterTypes(method);
                    // collectionTypes should be only 1 long
                    Type setType = collectionTypes.get(0);
                    // Create an empty set to fill
                    Set<Object> value = new HashSet<>();
                    // Sets are stored as a list in YAML
                    if (config.getList(storageLocation) != null) {
                        for (Object listValue: config.getList(storageLocation)) {
                            value.add(deserialize(listValue,Class.forName(setType.getTypeName())));
                        }
                    }
                    // Store the set using the setter in the class
                    method.invoke(instance, value);
                } else if (List.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Note that we have no idea what type of List this is
                    List<Type> collectionTypes = getCollectionParameterTypes(method);
                    // collectionTypes should be only 1 long
                    Type setType = collectionTypes.get(0);
                    // Create an empty list
                    List<Object> value = new ArrayList<>();
                    // Lists are stored as lists in YAML
                    if (config.getList(storageLocation) != null) {
                        for (Object listValue: config.getList(storageLocation)) {
                            value.add(deserialize(listValue,Class.forName(setType.getTypeName())));
                        }
                    }
                    // Store the list using the setting
                    method.invoke(instance, value);
                } else {
                    // Not a collection. Get the value and rely on YAML to supply it
                    Object value = config.get(storageLocation);
                    // If the value is a yaml MemorySection then something is wrong, so ignore it. Maybe an admin did some bad editing
                    if (value != null && !value.getClass().equals(MemorySection.class)) {
                        method.invoke(instance, deserialize(value,propertyDescriptor.getPropertyType()));
                    }
                }
            }
        }
        // After deserialization is complete, return the instance of the class we have created
        return instance;
    }

    /**
     * Get a list of parameter types for the collection argument in this method
     * @param writeMethod - write method
     * @return a list of parameter types for the collection argument in this method
     */
    private List<Type> getCollectionParameterTypes(Method writeMethod) {
        List<Type> result = new ArrayList<>();
        // Get the return type
        // This uses a trick to extract what the arguments are of the writeMethod of the field.
        // In this way, we can deduce what type needs to be written at runtime.
        Type[] genericParameterTypes = writeMethod.getGenericParameterTypes();
        // There could be more than one argument, so step through them
        for (Type genericParameterType : genericParameterTypes) {
            // If the argument is a parameter, then do something - this should always be true if the parameter is a collection
            if( genericParameterType instanceof ParameterizedType ) {
                // Get the actual type arguments of the parameter
                Type[] parameters = ((ParameterizedType)genericParameterType).getActualTypeArguments();
                result.addAll(Arrays.asList(parameters));
            }
        }
        return result;
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
        StoreAt storeAt = instance.getClass().getAnnotation(StoreAt.class);
        if (storeAt != null) {
            path = storeAt.path();
            filename = storeAt.filename();
        }
        // See if there are any top-level comments
        // See if there are multiple comments
        ConfigComment.Line comments = instance.getClass().getAnnotation(ConfigComment.Line.class);
        if (comments != null) {
            for (ConfigComment comment : comments.value()) {
                setComment(comment, config, yamlComments, "");
            }
        }
        // Handle single line comments
        ConfigComment comment = instance.getClass().getAnnotation(ConfigComment.class);
        if (comment != null) {
            setComment(comment, config, yamlComments, "");
        }

        // Run through all the fields in the class that is being stored. EVERY field must have a get and set method
        for (Field field : dataObject.getDeclaredFields()) {
            // Get the property descriptor for this field
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
            // Get the read method
            Method method = propertyDescriptor.getReadMethod();
            // Invoke the read method to get the value. We have no idea what type of value it is.
            Object value = method.invoke(instance);
            String storageLocation = field.getName();
            // Check if there is an annotation on the field
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);
            // If there is a config path annotation then do something
            if (configEntry != null && !configEntry.path().isEmpty()) {
                storageLocation = configEntry.path();
            }

            // Get path for comments
            String parent = "";
            if (storageLocation.contains(".")) {
                parent = storageLocation.substring(0, storageLocation.lastIndexOf('.')) + ".";
            }
            // See if there are multiple comments
            comments = field.getAnnotation(ConfigComment.Line.class);
            if (comments != null) {
                for (ConfigComment bodyComment : comments.value()) {
                    setComment(bodyComment, config, yamlComments, parent);
                }
            }
            // Handle single line comments
            comment = field.getAnnotation(ConfigComment.class);
            if (comment != null) {
                setComment(comment, config, yamlComments, parent);
            }

            // Adapter
            Adapter adapterNotation = field.getAnnotation(Adapter.class);
            if (adapterNotation != null && AdapterInterface.class.isAssignableFrom(adapterNotation.value())) {
                // A conversion adapter has been defined
                try {
                    config.set(storageLocation, ((AdapterInterface<?,?>)adapterNotation.value().getDeclaredConstructor().newInstance()).serialize(value));
                } catch (InstantiationException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
                    plugin.logError("Could not instatiate adapter " + adapterNotation.value().getName() + " " + e.getMessage());
                }
                // We are done here
                continue;
            }

            // Depending on the value type, it'll need serializing differently
            // Check if this field is the mandatory UniqueId field. This is used to identify this instantiation of the class
            if (method.getName().equals("getUniqueId")) {
                // If the object does not have a unique name assigned to it already, one is created at random
                String id = (String)value;
                if (value == null || id.isEmpty()) {
                    id = databaseConnector.getUniqueId(dataObject.getSimpleName());
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
                        String key = (String)serialize(object.getKey());
                        key = key.replaceAll("\\.", ":dot:");
                        result.put(key, serialize(object.getValue()));
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

        databaseConnector.saveYamlFile(config, path, filename, yamlComments);
    }

    private void setComment(ConfigComment comment, YamlConfiguration config, Map<String, String> yamlComments, String parent) {
        String random = "comment-" + UUID.randomUUID().toString();
        // Store placeholder
        config.set(parent + random, " ");
        // Create comment
        yamlComments.put(random, "# " + comment.value().replace(TextVariables.VERSION, plugin.getDescription().getVersion()));
    }

    /**
     * Serialize an object if required. This means that an object will be turned into text to store in YAML
     * @param object - object to serialize
     * @return - serialized object
     */
    private Object serialize(Object object) {
        // Null is a value object and is serialized as the string "null"
        if (object == null) {
            return "null";
        }
        // UUID has it's own serialization, that is not picked up automatically
        if (object instanceof UUID) {
            return ((UUID)object).toString();
        }
        // Only the world name is needed for worlds
        if (object instanceof World) {
            return ((World)object).getName();
        }
        // Location
        if (object instanceof Location) {
            return Util.getStringLocation((Location)object);
        }
        // Enums
        if (object instanceof Enum) {
            //Custom enums are a child of the Enum class. Just get the names of each one.
            return ((Enum<?>)object).name();
        }
        return object;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Object deserialize(Object value, Class<?> clazz) {
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

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteObject(java.lang.Object)
     */
    @Override
    public void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Obtain the value of uniqueId within the instance (which must be a DataObject)
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor("uniqueId", dataObject);
        Method method = propertyDescriptor.getReadMethod();
        String fileName = (String) method.invoke(instance);
        // The filename of the YAML file is the value of uniqueId field plus .yml. Sometimes the .yml is already appended.
        if (!fileName.endsWith(".yml")) {
            fileName = fileName + ".yml";
        }
        // Get the database and table folders
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        File tableFolder = new File(dataFolder, dataObject.getSimpleName());
        if (tableFolder.exists()) {
            // Obtain the file and delete it
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
