package world.bentobox.bentobox.api.logs;

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
    private long timestamp;
    private String type;
    private Map<String, String> data;

    public LogEntry(String type, Map<String, String> data) {
        this.timestamp = System.currentTimeMillis();
        this.type = type;
        this.data = data;
    }

    public LogEntry(long timestamp, String type, Map<String, String> data) {
        this.timestamp = timestamp;
        this.type = type;
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getData() {
        return data;
    }
}
