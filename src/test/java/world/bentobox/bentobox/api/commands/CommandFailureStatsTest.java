package world.bentobox.bentobox.api.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.BStats;
import world.bentobox.bentobox.BStats.CommandFailure;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * Tests that command failures are reported to bStats, and that they are keyed by
 * something that means the same thing on every server.
 *
 * @author tastybento
 */
class CommandFailureStatsTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private BStats bStats;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        when(plugin.getMetrics()).thenReturn(Optional.of(bStats));
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.isPlayer()).thenReturn(true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testNoPermissionIsRecorded() {
        TopLevelCommand tlc = new TopLevelCommand();
        CompositeCommand sub = tlc.getSubCommand("restricted").orElseThrow();
        when(user.hasPermission(anyString())).thenReturn(false);

        assertFalse(sub.call(user, "restricted", List.of()));
        verify(bStats).recordCommandFailure(eq(CommandFailure.NO_PERMISSION), eq("bentobox.restricted"));
    }

    @Test
    void testWrongContextIsRecorded() {
        TopLevelCommand tlc = new TopLevelCommand();
        CompositeCommand sub = tlc.getSubCommand("playeronly").orElseThrow();
        when(user.isPlayer()).thenReturn(false);

        assertFalse(sub.call(user, "playeronly", List.of()));
        verify(bStats).recordCommandFailure(eq(CommandFailure.WRONG_CONTEXT), anyString());
    }

    @Test
    void testCannotExecuteIsRecorded() {
        TopLevelCommand tlc = new TopLevelCommand();
        CompositeCommand sub = tlc.getSubCommand("blocked").orElseThrow();
        when(user.isOp()).thenReturn(true);

        assertFalse(sub.call(user, "blocked", List.of()));
        verify(bStats).recordCommandFailure(eq(CommandFailure.CANNOT_EXECUTE), eq("bentobox.blocked"));
    }

    @Test
    void testBadArgsIsRecorded() {
        TopLevelCommand tlc = new TopLevelCommand();
        CompositeCommand sub = tlc.getSubCommand("failing").orElseThrow();
        when(user.isOp()).thenReturn(true);

        assertFalse(sub.call(user, "failing", List.of()));
        verify(bStats).recordCommandFailure(eq(CommandFailure.BAD_ARGS), eq("bentobox.failing"));
    }

    @Test
    void testSuccessIsNotRecorded() {
        TopLevelCommand tlc = new TopLevelCommand();
        CompositeCommand sub = tlc.getSubCommand("working").orElseThrow();
        when(user.isOp()).thenReturn(true);

        assertTrue(sub.call(user, "working", List.of()));
        verify(bStats, never()).recordCommandFailure(org.mockito.ArgumentMatchers.any(), anyString());
    }

    /**
     * The permission is hard coded in setup(), so it is the same on every server
     * even when the top level command has been renamed in the config.
     */
    @Test
    void testGetStatsKeyUsesPermission() {
        TopLevelCommand tlc = new TopLevelCommand();
        assertEquals("bentobox.restricted", tlc.getSubCommand("restricted").orElseThrow().getStatsKey());
    }

    /**
     * Commands without a permission fall back to their class name.
     */
    @Test
    void testGetStatsKeyFallsBackToClassName() {
        TopLevelCommand tlc = new TopLevelCommand();
        assertEquals("bentobox.WorkingCommand", tlc.getSubCommand("working").orElseThrow().getStatsKey());
    }

    private static class TopLevelCommand extends CompositeCommand {
        TopLevelCommand() {
            super("top");
        }

        @Override
        public void setup() {
            new RestrictedCommand(this);
            new PlayerOnlyCommand(this);
            new BlockedCommand(this);
            new FailingCommand(this);
            new WorkingCommand(this);
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }

    private static class RestrictedCommand extends CompositeCommand {
        RestrictedCommand(CompositeCommand parent) {
            super(parent, "restricted");
        }

        @Override
        public void setup() {
            setPermission("restricted");
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }

    private static class PlayerOnlyCommand extends CompositeCommand {
        PlayerOnlyCommand(CompositeCommand parent) {
            super(parent, "playeronly");
        }

        @Override
        public void setup() {
            setOnlyPlayer(true);
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }

    private static class BlockedCommand extends CompositeCommand {
        BlockedCommand(CompositeCommand parent) {
            super(parent, "blocked");
        }

        @Override
        public void setup() {
            setPermission("blocked");
        }

        @Override
        public boolean canExecute(User user, String label, List<String> args) {
            return false;
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }

    private static class FailingCommand extends CompositeCommand {
        FailingCommand(CompositeCommand parent) {
            super(parent, "failing");
        }

        @Override
        public void setup() {
            setPermission("failing");
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return false;
        }
    }

    private static class WorkingCommand extends CompositeCommand {
        WorkingCommand(CompositeCommand parent) {
            super(parent, "working");
        }

        @Override
        public void setup() {
            // No permission set on purpose
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }
}
