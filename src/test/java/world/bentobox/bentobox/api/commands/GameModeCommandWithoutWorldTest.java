package world.bentobox.bentobox.api.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * A game mode addon's commands are created in its {@code onLoad()} but only
 * given their world when the addon <i>enables</i>. An addon that never enables -
 * ChunkBlock with the Level addon it depends on missing, in the report behind
 * #3059 - therefore leaves live commands with no world behind them, and every
 * player who ran one got an "unexpected error" and a
 * {@code NullPointerException} in the console.
 * <p>
 * The commands are now taken back out when the addon is given up on, but a
 * command that does slip through must fail politely rather than throw.
 *
 * @author tastybento
 */
class GameModeCommandWithoutWorldTest extends CommonTestSetup {

    @Mock
    private User user;
    @Mock
    private GameModeAddon gameMode;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        AddonDescription description = mock(AddonDescription.class);
        when(description.getName()).thenReturn("ChunkBlock");
        when(gameMode.getDescription()).thenReturn(description);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.isPlayer()).thenReturn(true);
        when(user.isOp()).thenReturn(true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testWorldlessGameModeCommandIsRefusedRatherThanThrowing() {
        PlayerCommand command = new PlayerCommand(gameMode);
        assertNull(command.getWorld(), "Precondition: the addon never enabled, so no world was set");

        assertFalse(command.call(user, "ch", List.of()));
        assertFalse(command.hasRun, "The command body must not run with no world behind it");
    }

    @Test
    void testWorldlessGameModeCommandTellsThePlayer() {
        PlayerCommand command = new PlayerCommand(gameMode);

        command.call(user, "ch", List.of());

        verify(user).sendMessage("general.errors.general");
    }

    /**
     * A sub-command inherits its world from the top of the tree, so it is just as
     * exposed as the top level command is.
     */
    @Test
    void testWorldlessGameModeSubCommandIsRefused() {
        PlayerCommand command = new PlayerCommand(gameMode);
        CompositeCommand create = command.getSubCommand("create").orElseThrow();

        assertFalse(create.call(user, "create", List.of()));
    }

    /**
     * BentoBox's own commands legitimately have no world - {@code /bentobox} is not
     * tied to one - and must not be caught by the guard.
     */
    @Test
    void testWorldlessNonGameModeCommandStillRuns() {
        PlainCommand command = new PlainCommand();

        assertTrue(command.call(user, "bentobox", List.of()));
    }

    private static class PlayerCommand extends CompositeCommand {
        boolean hasRun;

        PlayerCommand(GameModeAddon addon) {
            super(addon, "ch");
        }

        @Override
        public void setup() {
            new CreateCommand(this);
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            hasRun = true;
            return true;
        }
    }

    private static class CreateCommand extends CompositeCommand {
        CreateCommand(CompositeCommand parent) {
            super(parent, "create");
        }

        @Override
        public void setup() {
            // Nothing to set up
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }

    private static class PlainCommand extends CompositeCommand {
        PlainCommand() {
            super("bentobox");
        }

        @Override
        public void setup() {
            // Nothing to set up
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }
}
