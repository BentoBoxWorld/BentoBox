package world.bentobox.bentobox.database;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;

/**
 * An abstract class that handles insert/select-operations into/from a database
 *
 * @author tastybento
 *
 * @param <T>
 */
public abstract class AbstractDatabaseHandler<T> {

    /**
     * FIFO queue for saves or deletions. Note that the assumption here is that most database objects will be held
     * in memory because loading is not handled with this queue. That means that it is theoretically
     * possible to load something before it has been saved. So, in general, load your objects and then
     * save them async only when you do not need the data again immediately.
     */
    protected Queue<Runnable> processQueue;

    /**
     * Async save task that runs repeatedly
     */
    private BukkitTask asyncSaveTask;

    protected boolean shutdown;

    /**
     * Name of the folder where databases using files will live
     */
    protected static final String DATABASE_FOLDER_NAME = "database";

    /**
     * The data object that should be created and filled with values
     * from the database or inserted into the database
     */
    protected Class<T> dataObject;

    /**
     * Contains the settings to create a connection to the database like
     * host/port/database/user/password
     */
    protected DatabaseConnector databaseConnector;

    protected BentoBox plugin;

    /**
     * The addon that is accessing the database, if any.
     */
    @Nullable
    private Addon addon;

    /**
     * Get the addon that is accessing the database, if any. May be null.
     * @return the addon
     */
    @Nullable
    public Addon getAddon() {
        return addon;
    }

    /**
     * Set the addon that is accessing the database, if any.
     * @param addon the addon to set
     */
    public void setAddon(@Nullable Addon addon) {
        this.addon = addon;
    }

    /**
     * Constructor
     *
     * @param type
     *            The type of the objects that should be created and filled with
     *            values from the database or inserted into the database
     * @param databaseConnector
     *            Contains the settings to create a connection to the database
     *            like host/port/database/user/password
     */
    protected AbstractDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        this.plugin = plugin;
        this.databaseConnector = databaseConnector;
        this.dataObject = type;

        // Return if plugin disabled
        if (!plugin.isEnabled()) return;
        // Run async queue
        processQueue = new ConcurrentLinkedQueue<>();
        asyncSaveTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            // Loop continuously
            while (!shutdown || !processQueue.isEmpty()) {
                while (!processQueue.isEmpty()) {
                    processQueue.poll().run();
                }
                // Shutdown flag
                shutdown = plugin.isShutdown();
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
            databaseConnector.closeConnection(dataObject);
        });
    }

    protected AbstractDatabaseHandler() {}

    /**
     * Loads all the records in this table and returns a list of them
     * @return list of <T>
     */
    public abstract List<T> loadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException;

    /**
     * Creates a <T> filled with values from the corresponding
     * database file
     * @param uniqueId - unique ID
     * @return <T>
     */
    @Nullable
    public abstract T loadObject(@NonNull String uniqueId) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException;

    /**
     * Save T into the corresponding database
     *
     * @param instance that should be inserted into the database
     */
    public abstract CompletableFuture<Boolean> saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException ;

    /**
     * Deletes the object with the unique id from the database. If the object does not exist, it will fail silently.
     * Use {@link #objectExists(String)} if you need to know if the object is in the database beforehand.
     * @param instance - object instance
     */
    public abstract void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException ;

    /**
     * Checks if a unique id exists or not
     * @param uniqueId - uniqueId to check
     * @return true if this uniqueId exists
     */
    public abstract boolean objectExists(String uniqueId);

    /**
     * Closes the database
     */
    public abstract void close();

    /**
     * Attempts to delete the object with the uniqueId. If the object does not exist, it will fail silently.
     * Use {@link #objectExists(String)} if you need to know if the object is in the database beforehand.
     * @param uniqueId - uniqueId of object
     * @since 1.1
     */
    public abstract void deleteID(String uniqueId);

}
