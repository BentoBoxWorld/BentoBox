package world.bentobox.bentobox.api.logs;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents an event that occurred and that is logged.
 * <br/>
 * An {@link world.bentobox.bentobox.database.objects.adapters.AdapterInterface} is provided to be able to save/retrieve
 * a list of instances of this object to/from the database: {@link world.bentobox.bentobox.database.objects.adapters.LogEntryListAdapter}.
 *
 * @author Poslovitch
 */
public class LogEntry {
    private final long timestamp;
    private final String type;
    private final Map<String, Object> data;

    private LogEntry(Builder builder) {
        this.timestamp = builder.timestamp;
        this.type = builder.type;
        this.data = builder.data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public static class Builder {
        private long timestamp;
        private String type;
        private Map<String, Object> data;

        public Builder(String type) {
            this.timestamp = System.currentTimeMillis();
            this.type = type.toUpperCase(Locale.ENGLISH);
            this.data = new LinkedHashMap<>();
        }

        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        /**
         * Puts this key and this value in the currently existing data map.
         * @param key key to set
         * @param value value to set
         * @return the Builder instance
         */
        public Builder data(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
