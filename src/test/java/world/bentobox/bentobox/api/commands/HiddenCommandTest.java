package world.bentobox.bentobox.api.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.command.CommandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, CommandEvent.class})
public class HiddenCommandTest {

    @Mock
    private BentoBox plugin;

    @Mock
    private User user;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Translation
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.CompositeCommand#tabComplete(org.bukkit.command.CommandSender, java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testTabCompleteCommandSenderStringStringArrayVisible() {
        TopLevelCommand tlc = new TopLevelCommand();
        CommandSender sender = mock(CommandSender.class);
        String[] args = {"v"};
        List<String> opList = tlc.tabComplete(sender, "top", args);
        assertFalse(opList.isEmpty());
        assertEquals("visible", opList.get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.CompositeCommand#tabComplete(org.bukkit.command.CommandSender, java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testTabCompleteCommandSenderStringStringArrayHidden() {
        TopLevelCommand tlc = new TopLevelCommand();
        CommandSender sender = mock(CommandSender.class);
        String[] args = {"h"};
        List<String> opList = tlc.tabComplete(sender, "top", args);
        assertEquals(1, opList.size());
        assertEquals("help", opList.get(0)); // Only help
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.CompositeCommand#tabComplete(org.bukkit.command.CommandSender, java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testTabCompleteCommandSenderStringStringArrayInvisible() {
        TopLevelCommand tlc = new TopLevelCommand();
        CommandSender sender = mock(CommandSender.class);
        String[] args = {"i"};
        List<String> opList = tlc.tabComplete(sender, "top", args);
        assertTrue(opList.isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.CompositeCommand#showHelp(world.bentobox.bentobox.api.commands.CompositeCommand, world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testShowHelp() {
        // Only the visible command should show in help
        TopLevelCommand tlc = new TopLevelCommand();
        tlc.showHelp(tlc, user);
        verify(user).sendMessage(eq("commands.help.header"), eq(TextVariables.LABEL), eq("commands.help.console"));
        verify(user).sendMessage(eq("commands.help.syntax-no-parameters"), eq("[usage]"), eq("/top"), eq(TextVariables.DESCRIPTION), eq("top.description"));
        verify(user).sendMessage(eq("commands.help.syntax-no-parameters"), eq("[usage]"), eq("/top visible"), eq(TextVariables.DESCRIPTION), eq("visible.description"));
        verify(user, never()).sendMessage(eq("commands.help.syntax-no-parameters"), eq("[usage]"), eq("/top hidden"), eq(TextVariables.DESCRIPTION), anyString());
        verify(user, never()).sendMessage(eq("commands.help.syntax-no-parameters"), eq("[usage]"), eq("/top hidden2"), eq(TextVariables.DESCRIPTION), anyString());
        verify(user).sendMessage(eq("commands.help.end"));

    }

    class TopLevelCommand extends CompositeCommand {

        public TopLevelCommand() {
            super("top");
        }

        @Override
        public void setup() {
            this.setParametersHelp("top.parameters");
            this.setDescription("top.description");
            new VisibleCommand(this);
            new HiddenCommand(this);
            new Hidden2Command(this);
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }

    }

    class VisibleCommand extends CompositeCommand {

        public VisibleCommand(CompositeCommand parent) {
            super(parent, "visible");
        }

        @Override
        public void setup() {
            this.setParametersHelp("visible.parameters");
            this.setDescription("visible.description");
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }

    }

    class HiddenCommand extends CompositeCommand {

        public HiddenCommand(CompositeCommand parent) {
            super(parent, "hidden");
        }

        @Override
        public void setup() {
            this.setHidden(true);
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }

    }

    class Hidden2Command extends CompositeCommand {

        public Hidden2Command(CompositeCommand parent) {
            super(parent, "invisible");
        }

        @Override
        public void setup() {
            this.setHidden(true);
            this.setParametersHelp("invisible.parameters");
            this.setDescription("invisible.description");

        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }

    }
}
