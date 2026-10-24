package world.bentobox.bentobox.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/** Thread-safe, write-refreshed expiry without per-set threads or scheduled tasks. */
public class ExpiringSet<E> extends AbstractSet<E> implements AutoCloseable {
    private final Cache<E, Boolean> entries;
    private volatile boolean closed;

    public ExpiringSet(long expirationTime, TimeUnit timeUnit) {
        if (expirationTime <= 0) throw new IllegalArgumentException("Expiration time must be greater than zero.");
        entries = CacheBuilder.newBuilder().expireAfterWrite(expirationTime, Objects.requireNonNull(timeUnit)).build();
    }

    @Override
    public synchronized boolean add(E element) {
        if (closed) throw new java.util.concurrent.RejectedExecutionException("Set is closed");
        return entries.asMap().put(Objects.requireNonNull(element), Boolean.TRUE) == null;
    }

    @Override
    public boolean contains(Object element) {
        return entries.asMap().containsKey(Objects.requireNonNull(element));
    }

    @Override
    public boolean remove(Object element) {
        return entries.asMap().remove(Objects.requireNonNull(element)) != null;
    }

    @Override
    public Iterator<E> iterator() {
        return entries.asMap().keySet().iterator();
    }

    @Override
    public int size() {
        entries.cleanUp();
        return entries.asMap().size();
    }

    @Override
    public void clear() {
        entries.invalidateAll();
    }

    public synchronized void shutdownNow() {
        closed = true;
        clear();
    }

    @Override
    public void close() {
        shutdownNow();
    }
}
