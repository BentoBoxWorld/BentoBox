package world.bentobox.bentobox.api.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * Regression test for <a href="https://github.com/BentoBoxWorld/BentoBox/issues/3075">#3075</a>.
 * <p>
 * Walking the typed arguments to find a sub-command used to call
 * {@code setLabel(arg)} on the shared sub-command object. After any player
 * typed {@code /island h}, every other player's tab completion advertised
 * {@code h} in place of {@code go}. The label must stay the primary one however
 * the command is addressed.
 *
 * @author tastybento
 */
class SubCommandAliasLabelTest extends CommonTestSetup {

    private TopCommand top;
    private CommandSender sender;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getCommandsManager()).thenReturn(mock(CommandsManager.class));
        top = new TopCommand();
        sender = mock(CommandSender.class);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testTabCompleteViaAliasKeepsPrimaryLabel() {
        // Brigadier calls tabComplete on every keystroke, so merely typing the alias
        // used to be enough to rename the command for everyone.
        top.tabComplete(sender, "island", new String[] { "h", "" });
        assertEquals("go", top.go.getLabel());

        List<String> options = top.tabComplete(sender, "island", new String[] { "" });
        assertTrue(options.contains("go"), options.toString());
        assertFalse(options.contains("h"), options.toString());
        assertFalse(options.contains("home"), options.toString());
    }

    @Test
    void testExecuteViaAliasKeepsPrimaryLabelButPassesTypedAlias() {
        assertTrue(top.execute(sender, "island", new String[] { "home" }));
        assertEquals("home", top.go.labelUsed, "sub-command should still learn which alias was typed");
        assertEquals("go", top.go.getLabel(), "shared command object must not be renamed");

        assertTrue(top.execute(sender, "island", new String[] { "h" }));
        assertEquals("h", top.go.labelUsed);
        assertEquals("go", top.go.getLabel());

        assertEquals("/island go", top.go.getUsage());
    }

    class TopCommand extends CompositeCommand {

        GoCommand go;

        TopCommand() {
            super("island");
        }

        @Override
        public void setup() {
            go = new GoCommand(this);
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }

    class GoCommand extends CompositeCommand {

        String labelUsed;

        GoCommand(CompositeCommand parent) {
            super(parent, "go", "home", "h");
        }

        @Override
        public void setup() {
            // No permission, so any sender may run it
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            labelUsed = label;
            return true;
        }
    }
}
