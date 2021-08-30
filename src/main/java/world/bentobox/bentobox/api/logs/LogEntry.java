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
 * @author Poslovitch
 */
public class LogEntry {
    @Expose
    private final long timestamp;
    @Expose
    private final String type;
    @Expose
    private final Map<String, String> data;

    private LogEntry(@NonNull Builder builder) {
        this.timestamp = builder.timestamp;
        this.type = builder.type;
        this.data = builder.data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    public String getType() {
        return type;
    }

    @Nullable
    public Map<String, String> getData() {
        return data;
    }

    public static class Builder {
        private long timestamp;
        private final String type;
        private Map<String, String> data;

        public Builder(@NonNull String type) {
            this.timestamp = System.currentTimeMillis();
            this.type = type.toUpperCase(Locale.ENGLISH);
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
