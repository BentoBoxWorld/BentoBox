package world.bentobox.bentobox.database.transition;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    private AbstractDatabaseHandler<T> fromHandler;
    private AbstractDatabaseHandler<T> toHandler;

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
        // Load all objects from both databases
        List<T> listFrom = fromHandler.loadObjects();
        List<T> listTo = toHandler.loadObjects();
        // If source database has objects, then delete and save them in the destination database
        for (T object : listFrom) {
            toHandler.saveObject(object);
            fromHandler.deleteObject(object);
        }
        // Merge results
        listTo.addAll(listFrom);
        return listTo;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObject(java.lang.String)
     */
    @Override
    public T loadObject(String uniqueId) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        // Try destination database
        @Nullable
        T object = toHandler.loadObject(uniqueId);
        if (object == null) {
            // Try source database
            object = fromHandler.loadObject(uniqueId);
            if (object != null) {
                // Save the object in the new database and delete it from the old one
                toHandler.saveObject(object);
                fromHandler.deleteObject(object);
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
