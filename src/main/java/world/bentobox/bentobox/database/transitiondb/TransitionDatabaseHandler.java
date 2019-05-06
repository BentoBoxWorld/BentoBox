package world.bentobox.bentobox.database.transitiondb;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.database.AbstractDatabaseHandler;

/**
 * Class that creates a list of <T>s filled with values from the corresponding
 * database-table.
 *
 * @author tastybento
 *
 * @param <T> Handles flat files for Class <T>
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
        // Try JSON first
        List<T> list = toHandler.loadObjects();
        if (list == null || list.isEmpty()) {
            list = fromHandler.loadObjects();
            // If YAML has objects, then delete and save them as JSON
            if (list != null && !list.isEmpty()) {
                for (T object : list) {
                    toHandler.saveObject(object);
                    fromHandler.deleteObject(object);
                }
            }
        }
        return list;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#loadObject(java.lang.String)
     */
    @Override
    public T loadObject(String uniqueId) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, IntrospectionException, NoSuchMethodException {
        @Nullable
        T object = toHandler.loadObject(uniqueId);
        if (object == null) {
            // Try YAML
            object = fromHandler.loadObject(uniqueId);
            if (object != null) {
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
        return toHandler.objectExists(uniqueId) || fromHandler.objectExists(uniqueId);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#saveObject(java.lang.Object)
     */
    @Override
    public void saveObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        toHandler.saveObject(instance);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteID(java.lang.String)
     */
    @Override
    public void deleteID(String uniqueId) {
        toHandler.deleteID(uniqueId);
        fromHandler.deleteID(uniqueId);
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.AbstractDatabaseHandler#deleteObject(java.lang.Object)
     */
    @Override
    public void deleteObject(T instance) throws IllegalAccessException, InvocationTargetException, IntrospectionException {
        toHandler.deleteObject(instance);
        fromHandler.deleteObject(instance);
    }

    @Override
    public void close() {
        // Not used
    }
}
