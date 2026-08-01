package world.bentobox.bentobox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.BStats.CommandFailure;

/**
 * Tests the accumulation of command failure statistics.
 *
 * @author tastybento
 */
class BStatsTest {

    private BStats bStats;

    @BeforeEach
    public void setUp() {
        bStats = new BStats(mock(BentoBox.class));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> field(String name) throws ReflectiveOperationException {
        Field f = BStats.class.getDeclaredField(name);
        f.setAccessible(true);
        return (Map<String, Integer>) f.get(bStats);
    }

    @Test
    void testRecordCommandFailureCountsByType() throws ReflectiveOperationException {
        bStats.recordCommandFailure(CommandFailure.NO_PERMISSION, "bskyblock.island.team.invite");
        bStats.recordCommandFailure(CommandFailure.NO_PERMISSION, "bskyblock.island.team.kick");
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "bskyblock.island.team.invite");

        Map<String, Integer> failures = field("commandFailures");
        assertEquals(2, failures.size());
        assertEquals(2, failures.get("no_permission"));
        assertEquals(1, failures.get("bad_args"));
    }

    @Test
    void testRecordCommandFailureCountsByCommand() throws ReflectiveOperationException {
        bStats.recordCommandFailure(CommandFailure.NO_PERMISSION, "bskyblock.island.team.invite");
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "bskyblock.island.team.invite");
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "bskyblock.island.go");

        Map<String, Integer> paths = field("commandFailurePaths");
        assertEquals(2, paths.size());
        assertEquals(2, paths.get("bskyblock.island.team.invite"));
        assertEquals(1, paths.get("bskyblock.island.go"));
    }

    /**
     * A badly behaved addon with generated command names must not be able to grow
     * the map without limit between submissions.
     */
    @Test
    void testRecordCommandFailureCapsDistinctCommands() throws ReflectiveOperationException {
        for (int i = 0; i < 500; i++) {
            bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "addon.command" + i);
        }
        Map<String, Integer> paths = field("commandFailurePaths");
        assertEquals(200, paths.size());
        assertNull(paths.get("addon.command200"));

        // Commands already being tracked keep counting once the cap is reached
        bStats.recordCommandFailure(CommandFailure.BAD_ARGS, "addon.command0");
        assertEquals(2, paths.get("addon.command0"));
        assertEquals(200, paths.size());

        // Every failure is still counted by type, capped or not
        assertEquals(501, field("commandFailures").get("bad_args"));
    }

    @Test
    void testRecordCommandFailureNoFailures() throws ReflectiveOperationException {
        assertTrue(field("commandFailures").isEmpty());
        assertTrue(field("commandFailurePaths").isEmpty());
    }
}
