package world.bentobox.bentobox.database.yaml2json;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.json.AbstractJSONDatabaseHandler;
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

public class Yaml2JsonDatabaseHandler<T> extends AbstractJSONDatabaseHandler<T> {

    private static final String YML = ".yml";
    private static final String JSON = ".json";

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
    Yaml2JsonDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
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
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObjects()
     */
    @Override
    public List<T> loadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        // In this case, all the objects of a specific type are being loaded.
        List<T> list = new ArrayList<>();
        // The database folder name is in the plugin's data folder
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        // The folder for the objects (tables in database terminology) is here
        File tableFolder = new File(dataFolder, dataObject.getSimpleName());
        if (!tableFolder.exists()) {
            // Nothing there...
            tableFolder.mkdirs();
        }
        // Load each object from the file system, filtered, non-null
        // Try JSON first
        for (File file: Objects.requireNonNull(tableFolder.listFiles((dir, name) ->  name.toLowerCase(Locale.ENGLISH).endsWith(JSON)))) {
            try {
                list.add(getGson().fromJson(new FileReader(file), dataObject));
            } catch (Exception e) {
                plugin.logError("Could not load object " + file.getName() + " " + e.getMessage());
            }
        }
        if (list.isEmpty()) {
            // Try YAML
            // Load each object from the file system, filtered, non-null
            for (File file: Objects.requireNonNull(tableFolder.listFiles((dir, name) ->  name.toLowerCase(java.util.Locale.ENGLISH).endsWith(YML)))) {
                YamlConfiguration config = ((Yaml2JsonDatabaseConnector)databaseConnector).loadYamlFile(DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName(), file.getName());
                T object = createObject(config);
                list.add(object);
                // Delete the YAML file and save the JSON one - this is done async
                this.deleteObject(object);
                this.saveObject(object);
            }
        }
        return list;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObject(java.lang.String)
     */
    @Override
    public T loadObject(String uniqueId) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        // Objects are loaded from a folder named after the simple name of the class being stored
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();

        // Try JSON first
        String fileName = path + File.separator + uniqueId;
        if (!fileName.endsWith(JSON)) {
            fileName = fileName + JSON;
        }
        File file = new File(plugin.getDataFolder(), fileName);
        if (file.exists()) {
            try {
                return getGson().fromJson(new FileReader(file), dataObject);
            } catch (Exception e) {
                plugin.logError("Could not load object " + fileName + " " + e.getMessage());
            }
        }
        // Load the YAML file at the location.
        YamlConfiguration config = ((Yaml2JsonDatabaseConnector)databaseConnector).loadYamlFile(path, uniqueId);
        // Use the createObject method to turn a YAML config into an Java object
        T object = createObject(config);
        // Delete the YAML file and save the JSON one
        this.deleteID(uniqueId);
        this.saveObject(object);
        return object;
    }

    @Override
    public boolean objectExists(String uniqueId) {
        // Check if the uniqueId (key) exists in the file system
        return databaseConnector.uniqueIdExists(dataObject.getSimpleName(), uniqueId);
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

    @Override
    public void saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Null check
        if (instance == null) {
            plugin.logError("Database request to store a null. ");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();

        // Obtain the value of uniqueId within the instance (which must be a DataObject)
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor("uniqueId", dataObject);
        Method method = propertyDescriptor.getReadMethod();
        String fileName = (String) method.invoke(instance) + JSON;

        File tableFolder = new File(plugin.getDataFolder(), path);
        File file = new File(tableFolder, fileName);
        if (!tableFolder.exists()) {
            tableFolder.mkdirs();
        }
        if (plugin.isEnabled()) {
            // Async
            processQueue.add(() -> save(getGson().toJson(instance), tableFolder, file, fileName));
        } else {
            // Sync for shutdown
            save(getGson().toJson(instance), tableFolder, file, fileName);
        }
    }

    private void save(String toStore, File tableFolder, File file, String fileName) {
        try (FileWriter fileWriter = new FileWriter(file)) {
            File tmpFile = new File(tableFolder, fileName + ".bak");
            if (file.exists()) {
                // Make a backup of file
                Files.copy(file.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            fileWriter.write(toStore);
            Files.deleteIfExists(tmpFile.toPath());
        } catch (IOException e) {
            plugin.logError("Could not save json file: " + file.getAbsolutePath() + " " + e.getMessage());
        }
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
        // Get the database and table folders
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        File tableFolder = new File(dataFolder, dataObject.getSimpleName());
        if (tableFolder.exists()) {
            // Obtain the file and delete it
            File file = new File(tableFolder, uniqueId);
            File file2 = new File(tableFolder, uniqueId + YML);
            File file3 = new File(tableFolder, uniqueId + JSON);
            try {
                Files.deleteIfExists(file.toPath());
                Files.deleteIfExists(file2.toPath());
                Files.deleteIfExists(file3.toPath());
            } catch (IOException e) {
                plugin.logError("Could not delete database object! " + uniqueId + " - " + e.getMessage());
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
            plugin.logError("Database request to delete a null.");
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
