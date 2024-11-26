package world.bentobox.bentobox.database.objects.adapters;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.logs.LogEntry.LogType;

/**
 * @author tastybento
 *
 */
public class LogEntryListAdapterTest {

    private LogEntryListAdapter a;
    private YamlConfiguration config;
    private final List<LogEntry> history = new LinkedList<>();
    private UUID target;
    private UUID issuer;
    private List<LogEntry> toLog;

    @Before
    public void setUp() throws Exception {
        config = new YamlConfiguration();
        a = new LogEntryListAdapter();
        target = UUID.randomUUID();
        issuer = UUID.randomUUID();

        toLog = new ArrayList<>();
        toLog.add(new LogEntry.Builder(LogType.BAN).data("player", target.toString()).data("issuer", issuer.toString())
                .build());
        toLog.add(new LogEntry.Builder(LogType.UNBAN).data("player", target.toString())
                .data("issuer", issuer.toString()).build());
        toLog.add(new LogEntry.Builder(LogType.UNOWNED).build());
        history.addAll(toLog);
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
        assertEquals(3, historyCheck.size());
        for (int i = 0; i < historyCheck.size(); i++) {
            assertEquals(toLog.get(i).getTimestamp(), historyCheck.get(i).getTimestamp());
            assertEquals(toLog.get(i).getType(), historyCheck.get(i).getType());
            assertEquals(toLog.get(i).getData().get("player"), historyCheck.get(i).getData().get("player"));
            assertEquals(toLog.get(i).getData().get("issuer"), historyCheck.get(i).getData().get("issuer"));
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.adapters.LogEntryListAdapter#serialize(java.lang.Object)}
     * and {@link world.bentobox.bentobox.database.objects.adapters.LogEntryListAdapter#deserialize(java.lang.Object)}.
     * @throws InvalidConfigurationException 
     */
    @Test
    public void testSerializeDeserializeUnknownHistory() throws InvalidConfigurationException {
        // Make entries using unknown types
        String bad = "test:\n" + "  history:\n" + "  - timestamp: 1731359067207\n" + "    type: WEIRD\n" + "    data:\n"
                + "      player: 3f9d5634-331e-4598-9445-7449d56f7f74\n"
                + "      issuer: b366ba84-adec-42fe-b9dc-2c6a7b26f067\n" + "  - timestamp: 1731359067207\n"
                + "    type: ENTRY\n" + "    data:\n" + "      player: 3f9d5634-331e-4598-9445-7449d56f7f74\n"
                + "      issuer: b366ba84-adec-42fe-b9dc-2c6a7b26f067\n" + "  - timestamp: 1731359067207\n"
                + "    type: SUPER\n" + "    data: {}";
        config.loadFromString(bad);

        // Verify
        List<LogEntry> historyCheck = a.deserialize(config.get("test.history"));
        assertEquals(3, historyCheck.size());
        for (int i = 0; i < historyCheck.size(); i++) {
            assertEquals(LogType.UNKNOWN, historyCheck.get(i).getType());
        }
    }

}
