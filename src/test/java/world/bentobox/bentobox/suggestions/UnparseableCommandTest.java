package world.bentobox.bentobox.suggestions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.event.command.UnknownCommandEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * Regression tests for Border issue 179: {@code /oneblock border color green}
 * threw an unhandled {@code CommandException} out of a BentoBox task.
 * <p>
 * Brigadier hides nodes the sender may not use, and hiding is not passive: the
 * parse walks into the {@code color} literal, finds the sender cannot use it and
 * dead-ends there rather than falling back to the catch-all argument beside it.
 * Paper reports that as an unknown command, so before this fix:
 * <ol>
 * <li>the player saw Brigadier's "Incorrect argument for command" instead of the
 * command's own "you do not have permission" message, and nothing ran;</li>
 * <li>the did-you-mean matcher re-attached the tokens it could not match and
 * suggested the very line that had just failed;</li>
 * <li>accepting that suggestion re-dispatched it through
 * {@code Player#performCommand}, where the same failure escaped the scheduled
 * task as an unhandled exception.</li>
 * </ol>
 *
 * @author tastybento
 */
class UnparseableCommandTest extends CommonTestSetup {

    private static final String LINE = "oneblock border color green";

    private SuggestionsManager suggestionsManager;
    private DidYouMeanListener listener;
    private GameModeCommand oneblock;
    private Map<String, CompositeCommand> registeredCommands;
    @Mock
    private CommandSourceStack commandSourceStack;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        registeredCommands = new HashMap<>();
        when(cm.getCommands()).thenReturn(registeredCommands);
        mockedUtil.when(() -> Util.getWorld(any())).thenAnswer(invocation -> invocation.getArgument(0));
        oneblock = new GameModeCommand("oneblock", "ob");
        oneblock.setWorld(world);
        registeredCommands.put("oneblock", oneblock);
        when(cm.resolveCommand("oneblock")).thenReturn(oneblock);
        suggestionsManager = new SuggestionsManager(plugin);
        when(plugin.getSuggestionsManager()).thenReturn(suggestionsManager);
        listener = new DidYouMeanListener(plugin);
        when(commandSourceStack.getSender()).thenReturn(mockPlayer);
        when(lm.get(any(), eq("general.did-you-mean.suggestion"))).thenReturn("Did you mean [command]?");
        when(lm.get(any(), eq("general.did-you-mean.header"))).thenReturn("Did you mean one of these?");
        when(lm.get(any(), eq("general.did-you-mean.option"))).thenReturn("  [command]");
        // The player may toggle their border but has no colour permission
        when(mockPlayer.hasPermission(anyString())).thenReturn(false);
        when(mockPlayer.hasPermission("aoneblock.border.toggle")).thenReturn(true);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * The command line reaches its command, which reports the missing permission
     * itself, and Brigadier's error is dropped.
     */
    @Test
    void testCommandRunsAndReportsTheMissingPermission() {
        UnknownCommandEvent e = unknownCommand(LINE);

        listener.onUnknownCommand(e);

        assertNull(e.message(), "Brigadier's parse error must not be shown as well");
        checkSpigotMessage("general.errors.no-permission");
    }

    /**
     * Nothing is suggested, because there is nothing better to suggest than what
     * was typed - which is what used to be offered.
     */
    @Test
    void testTheTypedLineIsNotSuggestedBack() {
        listener.onUnknownCommand(unknownCommand(LINE));

        checkSpigotMessage("Did you mean", 0);
        assertFalse(listener.acceptPending(uuid), "Nothing may be left pending for 'yes' to run");
    }

    /**
     * The matcher must never offer the line the player just typed, whatever the
     * reason the parse failed.
     */
    @Test
    void testMatcherDropsAMatchIdenticalToTheInput() {
        User user = User.getInstance(mockPlayer);

        assertFalse(suggestionsManager.suggestCommand(user, LINE));
    }

    /**
     * A genuinely mistyped command still gets its suggestion - the fallback only
     * takes lines that start with one of our own labels.
     */
    @Test
    void testAnUnknownCommandStillGetsASuggestion() {
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);
        UnknownCommandEvent e = unknownCommand("teams");

        listener.onUnknownCommand(e);

        assertNull(e.message());
        checkSpigotMessage("Did you mean /oneblock team?");
    }

    /**
     * Something else entirely is left to the server to report on.
     */
    @Test
    void testAForeignCommandIsLeftAlone() {
        UnknownCommandEvent e = unknownCommand("xyzzy");

        listener.onUnknownCommand(e);

        assertNotNull(e.message());
    }

    /**
     * Whatever the dispatch of an accepted suggestion throws, it must not escape
     * the scheduled task.
     */
    @Test
    void testAcceptedSuggestionSwallowsDispatchFailures() {
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);
        listener.onUnknownCommand(unknownCommand("teams"));
        assertTrue(listener.acceptPending(uuid));
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTask(eq(plugin), task.capture());
        mockedBukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(mockPlayer);
        doThrow(new CommandException("Unhandled exception executing 'oneblock team'"))
                .when(mockPlayer).performCommand(anyString());

        task.getValue().run();

        verify(plugin).logError(anyString());
    }

    private UnknownCommandEvent unknownCommand(String commandLine) {
        return new UnknownCommandEvent(commandSourceStack, commandLine, Component.text("Unknown command"));
    }

    /**
     * A game mode player command with the Border addon's sub-commands hung off
     * it, as Border registers them: {@code /oneblock border} with
     * {@code type} and {@code color} below it.
     */
    private static class GameModeCommand extends CompositeCommand {

        GameModeCommand(String label, String... aliases) {
            super(label, aliases);
        }

        @Override
        public void setup() {
            new SubCommand(this, "go", "");
            CompositeCommand team = new SubCommand(this, "team", "");
            new SubCommand(team, "invite", "");
            CompositeCommand border = new SubCommand(this, "border", "aoneblock.border.toggle");
            new SubCommand(border, "type", "aoneblock.border.type");
            new SubCommand(border, "color", "aoneblock.border.color");
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }

    private static class SubCommand extends CompositeCommand {

        private final String perm;

        SubCommand(CompositeCommand parent, String label, String perm) {
            super(parent, label);
            this.perm = perm;
            // setup() has already run, so the permission is applied here
            setPermission(perm);
        }

        @Override
        public void setup() {
            // Permissions are set by the constructor, which runs after this
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }

        @Override
        public String toString() {
            return getLabel() + "(" + perm + ")";
        }
    }
}
