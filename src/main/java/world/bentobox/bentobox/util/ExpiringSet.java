package world.bentobox.bentobox.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * An {@code ExpiringSet} is a set implementation that automatically removes elements after a
 * specified period of time. The expiration time is specified when the set is created and
 * applies to all elements added to the set. It is thread-safe and provides similar
 * functionality to {@code HashSet} with the added feature of automatic expiration of elements.
 *
 * <p>This class manages a background thread resource, so it implements {@code AutoCloseable}
 * to ensure resources are released reliably using try-with-resources.
 *
 * @param <E> the type of elements maintained by this set
 */
public class ExpiringSet<E> implements Set<E>, AutoCloseable {
    // Maps the element (E) to its scheduled removal task (ScheduledFuture).
    private final ConcurrentMap<E, ScheduledFuture<?>> scheduledTasks;
    private final ScheduledExecutorService scheduler;
    private final long expirationTime;

    /**
     * Constructs an empty {@code ExpiringSet} with the specified expiration time for elements.
     *
     * @param expirationTime the time after which elements should expire, in the specified time unit
     * @param timeUnit the time unit for the {@code expirationTime} parameter
     * @throws IllegalArgumentException if {@code expirationTime} is less than or equal to zero
     * @throws NullPointerException if {@code timeUnit} is null
     */
    public ExpiringSet(long expirationTime, TimeUnit timeUnit) {
        if (expirationTime <= 0) {
            throw new IllegalArgumentException("Expiration time must be greater than zero.");
        }
        if (timeUnit == null) {
            throw new NullPointerException("TimeUnit cannot be null.");
        }
        
        this.scheduledTasks = new ConcurrentHashMap<>();
        // Use a single thread for scheduling removals.
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.expirationTime = timeUnit.toMillis(expirationTime);
    }

    /**
     * Shuts down the {@code ScheduledExecutorService} immediately. This attempts to stop
     * all actively executing tasks and halts the processing of waiting tasks.
     */
    public void shutdownNow() {
        scheduler.shutdownNow();
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is called automatically when the resource is used in a try-with-resources statement.
     */
    @Override
    public void close() {
        shutdownNow();
    }

    // --- Set Interface Implementations ---

    /**
     * Returns the number of elements in this set (its cardinality).
     */
    @Override
    public int size() {
        return scheduledTasks.size();
    }

    /**
     * Returns {@code true} if this set contains no elements.
     */
    @Override
    public boolean isEmpty() {
        return scheduledTasks.isEmpty();
    }

    /**
     * Returns {@code true} if this set contains the specified element.
     *
     * @param o element whose presence in this set is to be tested
     * @return {@code true} if this set contains the specified element
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean contains(Object o) {
        if (o == null) {
            throw new NullPointerException("Element cannot be null.");
        }
        return scheduledTasks.containsKey(o);
    }

    /**
     * Returns an iterator over the elements in this set.
     *
     * @return an iterator over the elements in this set
     */
    @Override
    public Iterator<E> iterator() {
        // Iterate over the keys of the map
        return scheduledTasks.keySet().iterator();
    }

    /**
     * Returns an array containing all of the elements in this set.
     */
    @Override
    public Object[] toArray() {
        return scheduledTasks.keySet().toArray();
    }

    /**
     * Returns an array containing all of the elements in this set; the runtime type of
     * the returned array is that of the specified array.
     *
     * @param a the array into which the elements of this set are to be stored
     * @param <T> the runtime type of the array to contain the collection
     * @return an array containing all of the elements in this set
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return scheduledTasks.keySet().toArray(a);
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * The element will automatically be removed after the specified expiration time.
     * If the element is already present, its expiration time is refreshed.
     *
     * @param e element to be added to this set
     * @throws NullPointerException if the specified element is null
     * @return {@code true} if this set did not already contain the specified element
     */
    @Override
    public boolean add(E e) {
        if (e == null) {
            throw new NullPointerException("Element cannot be null.");
        }

        // The lambda captures the 'futureHolder' array reference, which is final.
        final ScheduledFuture<?>[] futureHolder = new ScheduledFuture<?>[1];
        
        // 1. Create and schedule the new task. The lambda uses the reference in the array.
        futureHolder[0] = scheduler.schedule(() -> {
            // Self-removal: Try to remove the element only if its associated future is still the one 
            // referenced by the holder at the time of execution.
            scheduledTasks.remove(e, futureHolder[0]);
        }, expirationTime, TimeUnit.MILLISECONDS);

        // 2. Atomically replace the old future with the new one.
        // We use the value stored in the array immediately after scheduling.
        ScheduledFuture<?> oldFuture = scheduledTasks.put(e, futureHolder[0]);

        if (oldFuture != null) {
            // Element was already present (refresh). Cancel the old task.
            // false means do not interrupt the thread if the task is already running.
            oldFuture.cancel(false); 
            return false; // Not a new element
        }

        return true; // New element added
    }

    /**
     * Removes the specified element from this set if it is present.
     *
     * @param o object to be removed from this set, if present
     * @return {@code true} if this set contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    @Override
    public boolean remove(Object o) {
        if (o == null) {
            throw new NullPointerException("Element cannot be null.");
        }

        // Atomically remove the element and retrieve its associated future
        ScheduledFuture<?> future = scheduledTasks.remove(o);
        
        if (future != null) {
            // Cancel the future to prevent the removal task from running later
            future.cancel(false);
            return true;
        }

        return false;
    }

    /**
     * Returns {@code true} if this set contains all of the elements of the specified collection.
     *
     * @param c collection to be checked for containment in this set
     * @return {@code true} if this set contains all of the elements of the specified collection
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException("Collection cannot be null.");
        }
        return scheduledTasks.keySet().containsAll(c);
    }

    /**
     * Adds all of the elements in the specified collection to this set if they're not
     * already present. Each element's expiration time is set/refreshed.
     *
     * @param c collection containing elements to be added to this set
     * @return {@code true} if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is null or contains a null element
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException("The specified collection cannot be null.");
        }
        boolean modified = false;
        for (E element : c) {
            if (element == null) {
                throw new NullPointerException("Collection cannot contain null elements.");
            }
            // Use the custom add method to trigger expiration scheduling and cancellation
            if (add(element)) {
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Retains only the elements in this set that are contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this set
     * @return {@code true} if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException("Collection cannot be null.");
        }
        // Since we cannot atomically retain/cancel, we rely on the ConcurrentMap's keySet retainAll.
        // This will remove entries but leave the futures running (a minor resource leak).
        return scheduledTasks.keySet().retainAll(c);
    }

    /**
     * Removes from this set all of its elements that are contained in the specified collection.
     *
     * @param c collection containing elements to be removed from this set
     * @return {@code true} if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        if (c == null) {
            throw new NullPointerException("Collection cannot be null.");
        }
        boolean modified = false;
        // Iterate through the collection and use the custom remove(Object o) method
        for (Object element : c) {
            if (remove(element)) {
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Removes all of the elements from this set. The set will be empty after this call returns.
     */
    @Override
    public void clear() {
        // Cancel all pending futures
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            future.cancel(false);
        }
        scheduledTasks.clear();
    }

    /**
     * Compares the specified object with this set for equality.
     *
     * @param obj object to be compared for equality with this set
     * @return {@code true} if the specified object is equal to this set
     */
    @Override
    public boolean equals(Object obj) {
        return scheduledTasks.keySet().equals(obj);
    }

    /**
     * Returns the hash code value for this set.
     *
     * @return the hash code value for this set
     */
    @Override
    public int hashCode() {
        return scheduledTasks.keySet().hashCode();
    }
}
