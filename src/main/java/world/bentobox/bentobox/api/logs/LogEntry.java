package world.bentobox.bentobox.api.logs;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.annotations.Expose;

/**
 * Represents an event that occurred and that is logged.
 * <br/>
 * An {@link world.bentobox.bentobox.database.objects.adapters.AdapterInterface AdapterInterface} is provided to be able to save/retrieve
 * a list of instances of this object to/from the database: {@link world.bentobox.bentobox.database.objects.adapters.LogEntryListAdapter LogEntryListAdapter}.
 *
 * @author Poslovitch, tastybento
 * 
 */
public class LogEntry {
    @Expose
    private final long timestamp;
    @Expose
    private final LogType type;
    @Expose
    private final Map<String, String> data;

    public enum LogType {
        REMOVE, ADD, UNREGISTER, BAN, UNOWNED, SPAWN, UNBAN, JOINED, NEWOWNER, TRUSTED, UNKNOWN
    }

    private LogEntry(@NonNull Builder builder) {
        this.timestamp = builder.timestamp;
        this.type = builder.type;
        this.data = builder.data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public LogType getType() {
        return type;
    }

    @Nullable
    public Map<String, String> getData() {
        return data;
    }

    public static class Builder {
        private long timestamp;
        private final LogType type;
        private Map<String, String> data;

        public Builder(LogType type) {
            this.timestamp = System.currentTimeMillis();
            this.type = type;
            this.data = new LinkedHashMap<>();
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder data(Map<String, String> data2) {
            this.data = data2;
            return this;
        }

        /**
         * Puts this key and this value in the currently existing data map.
         * @param key key to set
         * @param value value to set
         * @return the Builder instance
         */
        public Builder data(@NonNull String key, @Nullable String value) {
            this.data.put(key, value);
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
