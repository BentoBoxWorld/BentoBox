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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;

public class JSONDatabaseHandler<T> extends AbstractJSONDatabaseHandler<T> {

    private static final String JSON = ".json";

    /**
     * FIFO queue for saves or deletions. Note that the assumption here is that most database objects will be held
     * in memory because loading is not handled with this queue. That means that it is theoretically
     * possible to load something before it has been saved. So, in general, load your objects and then
     * save them async only when you do not need the data again immediately.
     */
    private Queue<Runnable> processQueue;

    /**
     * Async save task that runs repeatedly
     */
    private BukkitTask asyncSaveTask;

    private boolean shutdown;

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
        processQueue = new ConcurrentLinkedQueue<>();
        if (plugin.isEnabled()) {
            asyncSaveTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                // Loop continuously
                while (!shutdown || !processQueue.isEmpty()) {
                    // This catches any databases that are not explicitly closed
                    if (!plugin.isEnabled()) {
                        shutdown = true;
                    }
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
            try (FileReader reader = new FileReader(file)){
                T object = getGson().fromJson(reader, dataObject);
                if (object == null) {
                    reader.close();
                    throw new IOException("JSON file created a null object: " + file.getPath());
                }
                list.add(object);
            } catch (FileNotFoundException e) {
                plugin.logError("Could not load file '" + file.getName() + "': File not found.");

            } catch (Exception e) {
                plugin.logError("Could not load objects " + file.getName() + " " + e.getMessage());
            }
        }
        return list;
    }

    @Override
    public T loadObject(@NonNull String uniqueId) {
        // Objects are loaded from a folder named after the simple name of the class being stored
        String path = DATABASE_FOLDER_NAME + File.separator + dataObject.getSimpleName();

        String fileName = path + File.separator + uniqueId;
        if (!fileName.endsWith(JSON)) {
            fileName = fileName + JSON;
        }

        T result = null;
        try (FileReader reader = new FileReader(new File(plugin.getDataFolder(), fileName))) {
            result = getGson().fromJson(reader, dataObject);
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
        String fileName = method.invoke(instance) + JSON;

        File tableFolder = new File(plugin.getDataFolder(), path);
        File file = new File(tableFolder, fileName);
        if (!tableFolder.exists()) {
            tableFolder.mkdirs();
        }

        String toStore = getGson().toJson(instance);
        if (plugin.isEnabled()) {
            // Async
            processQueue.add(() -> store(toStore, file, tableFolder, fileName));
        } else {
            // Sync
            store(toStore, file, tableFolder, fileName);
        }
    }

    private void store(String toStore, File file, File tableFolder, String fileName) {
        try (FileWriter fileWriter = new FileWriter(file)) {
            File tmpFile = new File(tableFolder, fileName + ".bak");
            if (file.exists()) {
                // Make a backup of file
                Files.copy(file.toPath(), tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            fileWriter.write(toStore);
            Files.deleteIfExists(tmpFile.toPath());
        } catch (IOException e) {
            plugin.logError("Could not save JSON file: " + tableFolder.getName() + " " + fileName + " " + e.getMessage());
        }
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteID(java.lang.String)
     */
    @Override
    public void deleteID(String uniqueId) {
        if (plugin.isEnabled()) {
            processQueue.add(() -> delete(uniqueId));
        } else {
            delete(uniqueId);
        }
    }

    private void delete(String uniqueId) {
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
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                plugin.logError("Could not delete JSON database object! " + file.getName() + " - " + e.getMessage());
            }
        }
    }

    @Override
    public void deleteObject(T instance) {
        // Null check
        if (instance == null) {
            plugin.logError("JSON database request to delete a null.");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        try {
            Method getUniqueId = dataObject.getMethod("getUniqueId");
            deleteID((String) getUniqueId.invoke(instance));
        } catch (Exception e) {
            plugin.logError("Could not delete object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public boolean objectExists(String uniqueId) {
        // Check if the uniqueId (key) exists in the file system
        return databaseConnector.uniqueIdExists(dataObject.getSimpleName(), uniqueId);
    }

    @Override
    public void close() {
        shutdown = true;
    }
}
