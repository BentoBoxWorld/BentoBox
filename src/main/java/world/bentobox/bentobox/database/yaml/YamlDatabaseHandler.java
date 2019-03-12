package world.bentobox.bentobox.database.yaml;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
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
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.StoreAt;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;
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

public class YamlDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    private static final String YML = ".yml";

    /**
     * FIFO queue for saves. Note that the assumption here is that most database objects will be held
     * in memory because loading is not handled with this queue. That means that it is theoretically
     * possible to load something before it has been saved. So, in general, load your objects and then
     * save them async only when you do not need the data again immediately.
     */
    private Queue<Runnable> processQueue;

    /**
     * Async save task that runs repeatedly
     */
    private BukkitTask asyncSaveTask;

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
    YamlDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector);
        processQueue = new ConcurrentLinkedQueue<>();
        if (plugin.isEnabled()) {
            asyncSaveTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                // Loop continuously
                while (plugin.isEnabled() || !processQueue.isEmpty()) {
                    while (!processQueue.isEmpty()) {
                        processQueue.poll().run();
                    }
                    // Clear the queue and then sleep
                    try {
                        Thread.sleep(25);
                    } catch (InterruptedException e) {
                        plugin.logError("Thread sleep error " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
                // Cancel
                asyncSaveTask.cancel();
            });
        }
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObject(java.lang.String)
     */
    @Override
    public T loadObject(String key) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        // Objects are loaded from a folder named after the simple name of the class being stored
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();
        // This path and key can be overridden by the StoreAt annotation in the code
        StoreAt storeAt = dataObject.getAnnotation(StoreAt.class);
        if (storeAt != null) {
            path = storeAt.path();
            key = storeAt.filename();
        }
        // Load the YAML file at the location.
        YamlConfiguration config = ((YamlDatabaseConnector)databaseConnector).loadYamlFile(path, key);
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
    public List<T> loadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        // In this case, all the objects of a specific type are being loaded.
        List<T> list = new ArrayList<>();
        // Look for any files that end in .yml in the folder
        FilenameFilter ymlFilter = (dir, name) ->  name.toLowerCase(java.util.Locale.ENGLISH).endsWith(YML);
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
            YamlConfiguration config = ((YamlDatabaseConnector)databaseConnector).loadYamlFile(DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName(), fileName);
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
    private T createObject(YamlConfiguration config) throws InstantiationException, IllegalAccessException, IntrospectionException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException {
        // Create a new instance of the dataObject of type T (which can be any class)
        T instance = dataObject.getDeclaredConstructor().newInstance();

        // Run through all the fields in the object
        for (Field field : dataObject.getDeclaredFields()) {
            // Ignore synthetic fields, such as those added by Jacoco or the compiler
            if (field.isSynthetic()) {
                continue;
            }
            // Get the getter and setters for this field using the JavaBeans system
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
            // Get the write method
            Method method = propertyDescriptor.getWriteMethod();
            /*
             * Field annotation checks
             */
            // Check if there is a ConfigEntry annotation on the field
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);

            // Determine the storage location
            String storageLocation = (configEntry != null && !configEntry.path().isEmpty()) ? configEntry.path() : field.getName();

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
            } else if (config.contains(storageLocation)) { // Look in the YAML Config to see if this field exists (it should)
                /*
                 * What follows is general deserialization code
                 */
                if (config.get(storageLocation) == null) { // Check for null values
                    method.invoke(instance, (Object)null);
                } else if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Maps
                    deserializeMap(method, instance, storageLocation, config);
                } else if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Sets
                    deserializeSet(method, instance, storageLocation, config);
                } else if (List.class.isAssignableFrom(propertyDescriptor.getPropertyType())) {
                    // Lists
                    deserializeLists(method, instance, storageLocation, config);
                } else {
                    // Non-collections
                    deserializeValue(method, instance, propertyDescriptor, storageLocation, config);
                }
            }
        }
        // After deserialization is complete, return the instance of the class we have created
        return instance;
    }

    private void deserializeValue(Method method, T instance, PropertyDescriptor propertyDescriptor, String storageLocation, YamlConfiguration config) throws IllegalAccessException, InvocationTargetException {
        // Not a collection. Get the value and rely on YAML to supply it
        Object value = config.get(storageLocation);
        // If the value is a yml MemorySection then something is wrong, so ignore it. Maybe an admin did some bad editing
        if (value != null && !value.getClass().equals(MemorySection.class)) {
            Object setTo = deserialize(value,propertyDescriptor.getPropertyType());
            if (!(Enum.class.isAssignableFrom(propertyDescriptor.getPropertyType()) && setTo == null)) {
                // Do not invoke null on Enums
                method.invoke(instance, setTo);
            } else {
                plugin.logError("Default setting value will be used: " + propertyDescriptor.getReadMethod().invoke(instance));
            }
        }
    }

    private void deserializeLists(Method method, T instance, String storageLocation, YamlConfiguration config) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException {
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
    }

    private void deserializeSet(Method method, T instance, String storageLocation, YamlConfiguration config) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException {
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
    }

    private void deserializeMap(Method method, T instance, String storageLocation, YamlConfiguration config) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException {
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
            if(genericParameterType instanceof ParameterizedType ) {
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
        // Null check
        if (instance == null) {
            plugin.logError("YAML database request to store a null.");
            return;
        }
        // DataObject check
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        // This is the Yaml Configuration that will be used and saved at the end
        YamlConfiguration config = new YamlConfiguration();

        // Comments for the file
        Map<String, String> yamlComments = new HashMap<>();

        // Only allow storing in an arbitrary place if it is a config object. Otherwise it is in the database
        StoreAt storeAt = instance.getClass().getAnnotation(StoreAt.class);
        String path = storeAt == null ? DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName() : storeAt.path();
        String filename = storeAt == null ? "" : storeAt.filename();

        // See if there are any top-level comments
        handleComments(instance.getClass(), config, yamlComments, "");

        // Run through all the fields in the class that is being stored. EVERY field must have a get and set method
        for (Field field : dataObject.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
            // Get the property descriptor for this field
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), dataObject);
            // Get the read method
            Method method = propertyDescriptor.getReadMethod();
            // Invoke the read method to get the value. We have no idea what type of value it is.
            Object value = method.invoke(instance);

            String storageLocation = field.getName();

            // Check if there is an annotation on the field
            ConfigEntry configEntry = field.getAnnotation(ConfigEntry.class);

            // If there is a config path annotation or adapter then deal with them
            if (configEntry != null && !configEntry.path().isEmpty()) {
                if (configEntry.hidden()) {
                    // If the annotation tells us to not print the config entry, then we won't.
                    continue;
                }

                // Get the storage location
                storageLocation = configEntry.path();

                // Get path for comments
                String parent = "";
                if (storageLocation.contains(".")) {
                    parent = storageLocation.substring(0, storageLocation.lastIndexOf('.')) + ".";
                }
                handleComments(field, config, yamlComments, parent);
                handleConfigEntryComments(configEntry, config, yamlComments, parent);
            }

            if (checkAdapter(field, config, storageLocation, value)) {
                continue;
            }
            // Set the filename if it has not be set already
            if (filename.isEmpty() && method.getName().equals("getUniqueId")) {
                // Save the name for when the file is saved
                filename = getFilename(propertyDescriptor, instance, (String)value);
            }
            // Collections need special serialization
            if (Map.class.isAssignableFrom(propertyDescriptor.getPropertyType()) && value != null) {
                serializeMap((Map<Object,Object>)value, config, storageLocation);
            } else if (Set.class.isAssignableFrom(propertyDescriptor.getPropertyType()) && value != null) {
                serializeSet((Set<Object>)value, config, storageLocation);
            } else {
                // For all other data that doesn't need special serialization
                config.set(storageLocation, serialize(value));
            }
        }
        // If the filename has not been set by now then we have a problem
        if (filename.isEmpty()) {
            throw new IllegalArgumentException("No uniqueId in class");
        }

        // Save
        save(filename, config.saveToString(), path, yamlComments);
    }

    private void save(String name, String data, String path, Map<String, String> yamlComments) {
        if (plugin.isEnabled()) {
            // Async
            processQueue.add(() -> ((YamlDatabaseConnector)databaseConnector).saveYamlFile(data, path, name, yamlComments));
        } else {
            // Sync for shutdown
            ((YamlDatabaseConnector)databaseConnector).saveYamlFile(data, path, name, yamlComments);
        }
    }

    private void serializeSet(Set<Object> value, YamlConfiguration config, String storageLocation) {
        // Sets need to be serialized as string lists
        List<Object> list = new ArrayList<>();
        for (Object object : value) {
            list.add(serialize(object));
        }
        // Save the list in the config file
        config.set(storageLocation, list);
    }

    private void serializeMap(Map<Object, Object> value, YamlConfiguration config, String storageLocation) {
        // Maps need to have keys serialized
        Map<Object, Object> result = new HashMap<>();
        for (Entry<Object, Object> object : value.entrySet()) {
            // Serialize all key and values
            String key = (String)serialize(object.getKey());
            key = key.replaceAll("\\.", ":dot:");
            result.put(key, serialize(object.getValue()));
        }
        // Save the list in the config file
        config.set(storageLocation, result);
    }

    private String getFilename(PropertyDescriptor propertyDescriptor, T instance, String id) throws IllegalAccessException, InvocationTargetException {
        // If the object does not have a unique name assigned to it already, one is created at random
        if (id == null || id.isEmpty()) {
            id = databaseConnector.getUniqueId(dataObject.getSimpleName());
            // Set it in the class so that it will be used next time
            propertyDescriptor.getWriteMethod().invoke(instance, id);
        }
        return id;
    }

    private boolean checkAdapter(Field field, YamlConfiguration config, String storageLocation, Object value) throws IllegalAccessException, InvocationTargetException {
        Adapter adapterNotation = field.getAnnotation(Adapter.class);
        if (adapterNotation != null && AdapterInterface.class.isAssignableFrom(adapterNotation.value())) {
            // A conversion adapter has been defined
            try {
                config.set(storageLocation, ((AdapterInterface<?,?>)adapterNotation.value().getDeclaredConstructor().newInstance()).serialize(value));
            } catch (InstantiationException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
                plugin.logError("Could not instantiate adapter " + adapterNotation.value().getName() + " " + e.getMessage());
            }
            // We are done here
            return true;
        }
        return false;
    }

    /**
     * Handles comments that are set on a Field or a Class using the {@link ConfigComment} annotation.
     * @since 1.3.0
     */
    private void handleComments(@NonNull AnnotatedElement annotatedElement, @NonNull YamlConfiguration config, @NonNull Map<String, String> yamlComments, @NonNull String parent) {
        // See if there are multiple comments
        ConfigComment.Line comments = annotatedElement.getAnnotation(ConfigComment.Line.class);
        if (comments != null) {
            for (ConfigComment comment : comments.value()) {
                setComment(comment.value(), config, yamlComments, parent);
            }
        }
        // Handle single line comments
        ConfigComment comment = annotatedElement.getAnnotation(ConfigComment.class);
        if (comment != null) {
            setComment(comment.value(), config, yamlComments, parent);
        }
    }

    /**
     * Handles comments that should be added according to the values set in the {@link ConfigEntry} annotation of a Field.
     * @since 1.3.0
     */
    private void handleConfigEntryComments(@NonNull ConfigEntry configEntry, @NonNull YamlConfiguration config, @NonNull Map<String, String> yamlComments, @NonNull String parent) {
        // Tell when the configEntry has been added (if it's not "1.0")
        if (!configEntry.since().equals("1.0")) {
            setComment("Added since " + configEntry.since() + ".", config, yamlComments, parent);
        }

        // Tell if the configEntry is experimental
        if (configEntry.experimental()) {
            setComment("/!\\ This feature is experimental and might not work as expected or might not work at all.", config, yamlComments, parent);
        }

        // Tell if the configEntry needs a reset.
        if (configEntry.needsReset()) {
            setComment("/!\\ BentoBox currently does not support changing this value mid-game. If you do need to change it, do a full reset of your databases and worlds.", config, yamlComments, parent);
        }
    }

    private void setComment(@NonNull String comment, @NonNull YamlConfiguration config, @NonNull Map<String, String> yamlComments, @NonNull String parent) {
        String random = "comment-" + UUID.randomUUID().toString();
        // Store placeholder
        config.set(parent + random, " ");
        // Create comment
        yamlComments.put(random, "# " + comment.replace(TextVariables.VERSION, Objects.isNull(getAddon()) ? plugin.getDescription().getVersion() : getAddon().getDescription().getVersion()));
    }

    /**
     * Serialize an object if required. This means that an object will be turned into text to store in YAML
     * @param object - object to serialize
     * @return - serialized object
     */
    @NonNull
    private Object serialize(@Nullable Object object) {
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
    @Nullable
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
        if (value.getClass().equals(String.class)) {
            if (clazz.equals(Integer.class)) {
                return Integer.valueOf((String) value);
            }
            if (clazz.equals(Long.class)) {
                return Long.valueOf((String) value);
            }
            if (clazz.equals(Double.class)) {
                return Double.valueOf((String) value);
            }
            if (clazz.equals(Float.class)) {
                return Float.valueOf((String) value);
            }
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
            value = Bukkit.getServer().getWorld((String)value);
        }
        // Enums
        if (Enum.class.isAssignableFrom(clazz)) {
            //Custom enums are a child of the Enum class.
            // Find out the value
            Class<Enum> enumClass = (Class<Enum>)clazz;
            try {
                value = Enum.valueOf(enumClass, ((String)value).toUpperCase());
            } catch (Exception e) {
                // This value does not exist - probably admin typed it wrongly
                // Show what is available and pick one at random
                plugin.logError("Error in YML file: " + value + " is not a valid value in the enum " + clazz.getCanonicalName() + "!");
                plugin.logError("Options are : ");
                for (Field fields : enumClass.getFields()) {
                    plugin.logError(fields.getName());
                }
                value = null;
            }
        }
        return value;
    }

    @Override
    public void deleteID(String uniqueId) {
        if (plugin.isEnabled()) {
            processQueue.add(() -> delete(uniqueId));
        } else {
            delete(uniqueId);
        }
    }

    private void delete(String uniqueId) {
        // The filename of the YAML file is the value of uniqueId field plus .yml. Sometimes the .yml is already appended.
        if (!uniqueId.endsWith(YML)) {
            uniqueId = uniqueId + YML;
        }
        // Get the database and table folders
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        File tableFolder = new File(dataFolder, dataObject.getSimpleName());
        if (tableFolder.exists()) {
            // Obtain the file and delete it
            File file = new File(tableFolder, uniqueId);
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                plugin.logError("Could not delete yml database object! " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteObject(java.lang.Object)
     */
    @Override
    public void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Null check
        if (instance == null) {
            plugin.logError("YAML database request to delete a null.");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }

        // Obtain the value of uniqueId within the instance (which must be a DataObject)
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor("uniqueId", dataObject);
        Method method = propertyDescriptor.getReadMethod();
        deleteID((String) method.invoke(instance));

    }

    @Override
    public void close() {
        // Not used
    }
}
