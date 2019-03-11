package world.bentobox.bentobox.database.json;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;

public class JSONDatabaseHandler<T> extends AbstractJSONDatabaseHandler<T> {

    private static final String JSON = ".json";

    /**
     * Constructor
     *
     * @param plugin
     * @param type              The type of the objects that should be created and filled with
     *                          values from the database or inserted into the database
     * @param databaseConnector Contains the settings to create a connection to the database
     */
    JSONDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector);
    }

    @Override
    public List<T> loadObjects() {
        // In this case, all the objects of a specific type are being loaded.
        List<T> list = new ArrayList<>();

        // The path is the simple name of the class
        String path = dataObject.getSimpleName();

        // The database folder name is in the plugin's data folder
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        // The folder for the objects (tables in database terminology) is here
        File tableFolder = new File(dataFolder, path);
        if (!tableFolder.exists()) {
            // Nothing there...
            tableFolder.mkdirs();
        }
        // Load each object from the file system, filtered, non-null
        for (File file: Objects.requireNonNull(tableFolder.listFiles((dir, name) ->  name.toLowerCase(Locale.ENGLISH).endsWith(JSON)))) {
            try {
                list.add(getGson().fromJson(new FileReader(file), dataObject));
            } catch (FileNotFoundException e) {
                plugin.logError("Could not load file '" + file.getName() + "': File not found.");
            } catch (Exception e) {
                plugin.logError("Could not load objects " + file.getName() + " " + e.getMessage());
            }
        }
        return list;
    }

    @Override
    public T loadObject(String uniqueId) {
        // Objects are loaded from a folder named after the simple name of the class being stored
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();

        String fileName = path + File.separator + uniqueId;
        if (!fileName.endsWith(JSON)) {
            fileName = fileName + JSON;
        }

        T result = null;
        try {
            result = getGson().fromJson(new FileReader(new File(plugin.getDataFolder(), fileName)), dataObject);
        } catch (FileNotFoundException e) {
            plugin.logError("Could not load file '" + fileName + "': File not found.");
        } catch (Exception e) {
            plugin.logError("Could not load objects " + fileName + " " + e.getMessage());
        }

        return result;
    }

    @Override
    public void saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Null check
        if (instance == null) {
            plugin.logError("JSON database request to store a null. ");
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

        String toStore = getGson().toJson(instance);

        try (FileWriter fileWriter = new FileWriter(file)) {
            File tmpFile = new File(tableFolder, fileName + ".bak");
            if (file.exists()) {
                // Make a backup of file
                Files.copy(file.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            fileWriter.write(toStore);
            Files.deleteIfExists(tmpFile.toPath());
        } catch (IOException e) {
            plugin.logError("Could not save json file: " + path + " " + fileName + " " + e.getMessage());
        }
    }

    @Override
    public void deleteID(String uniqueId) {
        // The filename of the JSON file is the value of uniqueId field plus .json. Sometimes the .json is already appended.
        if (!uniqueId.endsWith(JSON)) {
            uniqueId = uniqueId + JSON;
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
                plugin.logError("Could not delete json database object! " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    @Override
    public void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Null check
        if (instance == null) {
            plugin.logError("JSON database request to delete a null. ");
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
    public boolean objectExists(String uniqueId) {
        // Check if the uniqueId (key) exists in the file system
        return databaseConnector.uniqueIdExists(dataObject.getSimpleName(), uniqueId);
    }

    @Override
    public void close() {
        // Not used
    }
}
