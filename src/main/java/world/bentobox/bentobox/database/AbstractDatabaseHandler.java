package world.bentobox.bentobox.database;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
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
     * Every handler created in this JVM, so that queued writes can be flushed on shutdown.
     * <p>
     * A handler is created per {@link Database} instance, each with its own {@link #processQueue},
     * and nothing else keeps a list of them. Weakly held so handlers belonging to a discarded
     * {@code Database} can still be collected.
     * @since 3.22.0
     */
    private static final Set<AbstractDatabaseHandler<?>> HANDLERS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    /**
     * Async save task that runs repeatedly
     */
    private BukkitTask asyncSaveTask;
    private boolean inSave;

    protected boolean shutdown;

    /**
     * True while {@link #flush()} is draining the queue on the calling thread. Queued writes
     * normally refuse to run once BentoBox is disabled; during a flush they must be allowed to
     * complete, because that is the whole point of the flush.
     * @since 3.22.0
     */
    protected volatile boolean draining;

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
        asyncSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            // Check shutdown
            if(shutdown || plugin.isShutdown()) {
                // Cancel - this will only get called if the plugin is shutdown separately to the server
                databaseConnector.closeConnection(dataObject);
                asyncSaveTask.cancel();
            } else if (!inSave && !processQueue.isEmpty()) {
                inSave = true;
                while(!processQueue.isEmpty()) {
                    processQueue.poll().run();
                }
                inSave = false;
            }
        }, 0L, 1L);
        HANDLERS.add(this);
    }

    protected AbstractDatabaseHandler() {}

    /**
     * Runs every write still sitting in this handler's queue, on the calling thread.
     * <p>
     * Queued writes are normally drained by an asynchronous repeating task. That task stops once
     * BentoBox is disabled, so anything queued during shutdown is never written. Pladdons are
     * disabled by the server <i>before</i> BentoBox, which means everything they save from
     * {@code onDisable()} lands in a queue that is about to be abandoned.
     *
     * @since 3.22.0
     */
    public void flush() {
        if (processQueue == null || processQueue.isEmpty()) {
            return;
        }
        draining = true;
        try {
            while (!processQueue.isEmpty()) {
                try {
                    processQueue.poll().run();
                } catch (Exception e) {
                    plugin.logError("Could not flush a queued database write: " + e.getMessage());
                }
            }
        } finally {
            draining = false;
        }
    }

    /**
     * Flushes every handler's outstanding writes on the calling thread. Called during BentoBox
     * shutdown, once all addons have been disabled and before any database is closed, so that
     * writes queued by addons on their way out are not discarded.
     *
     * @since 3.22.0
     */
    public static void flushAll() {
        List<AbstractDatabaseHandler<?>> handlers;
        synchronized (HANDLERS) {
            handlers = new ArrayList<>(HANDLERS);
        }
        handlers.forEach(AbstractDatabaseHandler::flush);
    }

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
     * Loads all the records in this table and returns a list of them async
     * @return CompletableFuture List of <T>
     * @since 2.7.0
     */
    public CompletableFuture<List<T>> loadObjectsASync() {
        CompletableFuture<List<T>> completableFuture = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(BentoBox.getInstance(), () -> {
            try {
                completableFuture.complete(loadObjects()); // Complete the future with the result
            } catch (Exception e) {
                completableFuture.completeExceptionally(e); // Complete exceptionally if an error occurs
                plugin.logError("Failed to load objects asynchronously: " + e.getMessage());
            }
        });

        return completableFuture;
    }

    /**
     * Save T into the corresponding database
     *
     * @param instance that should be inserted into the database
     * @return completable future that is true if saved
     */
    public abstract CompletableFuture<Boolean> saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException ;

    /**
     * Save T into the corresponding database on the calling thread, bypassing the asynchronous save
     * queue. The returned future is already complete when this method returns.
     * <p>
     * Normal saves are queued and written by an asynchronous task. That task stops running once
     * BentoBox is disabled, and any write still sitting in the queue at that point is discarded, so
     * a save issued while the server is shutting down can be lost silently. This is not
     * hypothetical: Pladdons are disabled by the server <i>before</i> BentoBox itself, so anything a
     * Pladdon persists from {@code onDisable()} is queued and never drained.
     * <p>
     * Use this only where a write must not be lost and there is no later chance to retry - shutdown
     * paths, essentially. Everywhere else prefer {@link #saveObject(Object)}, as this blocks the
     * calling thread for the duration of the write.
     * <p>
     * The default implementation delegates to {@link #saveObject(Object)}, which is already correct
     * for handlers that write synchronously. Handlers that queue writes must override it.
     *
     * @param instance that should be inserted into the database
     * @return completable future, already complete, that is true if saved
     * @since 3.22.0
     */
    public CompletableFuture<Boolean> saveObjectNow(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        return saveObject(instance);
    }

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
