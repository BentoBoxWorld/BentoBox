package world.bentobox.bentobox.database.transition;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;

/**
 * Class that transitions from one database type to another
 *
 * @author tastybento
 *
 * @param <T> Class <T> that is to be handled
 * @since 1.5.0
 */
public class TransitionDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    private final AbstractDatabaseHandler<T> fromHandler;
    private final AbstractDatabaseHandler<T> toHandler;

    /**
     * Constructor
     * @param type - class to store in the database
     * @param fromHandler - the database being moved away from
     * @param toHandler - the database being moved to
     */
    TransitionDatabaseHandler(Class<T> type, AbstractDatabaseHandler<T> fromHandler, AbstractDatabaseHandler<T> toHandler) {
        this.fromHandler = fromHandler;
        this.toHandler = toHandler;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObjects()
     */
    @Override
    public List<T> loadObjects() throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        // Load all objects from both databases. The destination wins on a shared uniqueId so a
        // previously interrupted migration cannot overwrite newer data with an old snapshot.
        List<T> listFrom = fromHandler.loadObjects();
        List<T> listTo = toHandler.loadObjects();
        Map<String, T> merged = new java.util.LinkedHashMap<>();
        for (T object : listTo) {
            merged.put(((world.bentobox.bentobox.database.objects.DataObject) object).getUniqueId(), object);
        }
        for (T object : listFrom) {
            String id = ((world.bentobox.bentobox.database.objects.DataObject) object).getUniqueId();
            if (merged.containsKey(id)) {
                continue; // already migrated (or newer): do not copy the source snapshot over it
            }
            merged.put(id, object);
            toHandler.saveObject(object).thenAccept(b -> {
                // Only delete the source once the destination write has succeeded.
                if (Boolean.TRUE.equals(b)) {
                    try {
                        fromHandler.deleteObject(object);
                    } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
                        plugin.logStacktrace(e);
                    }
                }
            });
        }
        return new java.util.ArrayList<>(merged.values());
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObject(java.lang.String)
     */
    @Override
    public T loadObject(@NonNull String uniqueId) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        // Try destination database
        @Nullable
        T object = toHandler.loadObject(uniqueId);
        if (object == null) {
            // Try source database
            object = fromHandler.loadObject(uniqueId);
            if (object != null) {
                // Save first and only delete the source row after the destination write succeeded.
                // Both handlers queue writes, so the previous fire-and-forget order could lose the
                // island permanently when the target write failed.
                final T migrated = object;
                toHandler.saveObject(migrated).thenAccept(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        try {
                            fromHandler.deleteObject(migrated);
                        } catch (IllegalAccessException | InvocationTargetException | IntrospectionException e) {
                            plugin.logStacktrace(e);
                        }
                    }
                });
            }
        }
        return object;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#objectExists(java.lang.String)
     */
    @Override
    public boolean objectExists(String uniqueId) {
        // True if this object is in either database
        return toHandler.objectExists(uniqueId) || fromHandler.objectExists(uniqueId);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#saveObject(java.lang.Object)
     */
    @Override
    public CompletableFuture<Boolean> saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Save only in the destination database
        return toHandler.saveObject(instance);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#saveObjectNow(java.lang.Object)
     */
    @Override
    public CompletableFuture<Boolean> saveObjectNow(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Save only in the destination database
        return toHandler.saveObjectNow(instance);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteID(java.lang.String)
     */
    @Override
    public void deleteID(String uniqueId) {
        // Delete in both databases if the object exists
        toHandler.deleteID(uniqueId);
        fromHandler.deleteID(uniqueId);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteObject(java.lang.Object)
     */
    @Override
    public void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        // Delete in both databases if the object exists
        toHandler.deleteObject(instance);
        fromHandler.deleteObject(instance);
    }

    @Override
    public void close() {
        // Not used
    }
}
