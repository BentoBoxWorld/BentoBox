/**
 *
 */
package world.bentobox.bentobox.database.objects.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.api.logs.LogEntry;

/**
 * @author tastybento
 *
 */
public class LogEntryListAdapterTest {

    private LogEntryListAdapter a;
    private YamlConfiguration config;
    private List<LogEntry> history = new LinkedList<>();
    private UUID target;
    private UUID issuer;
    private List<LogEntry> toLog;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        config = new YamlConfiguration();
        a = new LogEntryListAdapter();
        target = UUID.randomUUID();
        issuer = UUID.randomUUID();

        toLog = new ArrayList<>();
        toLog.add(new LogEntry.Builder("BAN").data("player", target.toString()).data("issuer", issuer.toString()).build());
        toLog.add(new LogEntry.Builder("UNBAN").data("player", target.toString()).data("issuer", issuer.toString()).build());
        toLog.add(new LogEntry.Builder("UNOWNED").build());
        toLog.forEach(history::add);
    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.adapters.LogEntryListAdapter#serialize(java.lang.Object)}
     * and {@link world.bentobox.bentobox.database.objects.adapters.LogEntryListAdapter#deserialize(java.lang.Object)}.
     */
    @Test
    public void testSerializeDeserialize() {
        config.set("test.history", a.serialize(history));
        // Verify
        List<LogEntry> historyCheck = a.deserialize(config.get("test.history"));
        assertTrue(historyCheck.size() == 3);
        for (int i = 0; i < historyCheck.size(); i++) {
            assertEquals(toLog.get(i).getTimestamp(), historyCheck.get(i).getTimestamp());
            assertEquals(toLog.get(i).getType(), historyCheck.get(i).getType());
            assertEquals(toLog.get(i).getData().get("player"), historyCheck.get(i).getData().get("player"));
            assertEquals(toLog.get(i).getData().get("issuer"), historyCheck.get(i).getData().get("issuer"));
        }
    }

}
