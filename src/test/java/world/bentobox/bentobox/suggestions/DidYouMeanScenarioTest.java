package world.bentobox.bentobox.suggestions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * Demonstrates the did-you-mean feature (#3027) end to end, following a real
 * support transcript from a production server:
 *
 * <pre>
 * Admin:  type /oneblock team, then invite your friend
 * Player: /teams
 * Admin:  not /teams — /oneblock team
 * Player: /team invite
 * Admin:  no, type /oneblock team and pick the name from the list
 * Player: /spawn
 * </pre>
 *
 * Every one of those wrong inputs now produces a correct, clickable
 * suggestion instead of "Unknown command".
 *
 * @author tastybento
 */
class DidYouMeanScenarioTest extends CommonTestSetup {

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
        // Command manager with the /oneblock tree registered
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        registeredCommands = new HashMap<>();
        when(cm.getCommands()).thenReturn(registeredCommands);
        // Identity world normalization so distinct mock worlds compare correctly
        mockedUtil.when(() -> Util.getWorld(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // The OneBlock game mode command tree; the mock player stands in its world
        oneblock = new GameModeCommand("oneblock", "ob");
        oneblock.setWorld(world);
        registeredCommands.put("oneblock", oneblock);
        // The engine under test
        suggestionsManager = new SuggestionsManager(plugin);
        when(plugin.getSuggestionsManager()).thenReturn(suggestionsManager);
        listener = new DidYouMeanListener(plugin);
        when(commandSourceStack.getSender()).thenReturn(mockPlayer);
        // Real en-US locale strings so the demo shows actual player-facing text
        when(lm.get(any(), eq("general.did-you-mean.suggestion"))).thenReturn(
                "<gray>Did you mean </gray><yellow>[command]</yellow><gray>? Click here or type </gray><green>yes</green><gray> to run it.</gray>[run_command: [command]][hover: Click to run [command]]");
        when(lm.get(any(), eq("general.did-you-mean.header")))
                .thenReturn("<gray>Did you mean one of these? Click one to run it:</gray>");
        when(lm.get(any(), eq("general.did-you-mean.option")))
                .thenReturn("<yellow>  [command]</yellow>[run_command: [command]][hover: Click to run [command]]");
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Player types {@code /teams}. Instead of "Unknown command" they get a
     * clickable "Did you mean /oneblock team?" they can also accept by typing
     * yes.
     */
    @Test
    void testPlayerTypesTeams() {
        UnknownCommandEvent e = unknownCommand("teams");
        listener.onUnknownCommand(e);
        // The vanilla "Unknown command" message is replaced...
        assertNull(e.message());
        // ...by a single confident, clickable suggestion
        checkSpigotMessage("Did you mean ");
        checkSpigotMessage("/oneblock team");
        assertClickRuns("/oneblock team");
    }

    /**
     * Player types {@code /team invite Floris}. The argument survives:
     * {@code /oneblock team invite Floris}.
     */
    @Test
    void testPlayerTypesTeamInvite() {
        UnknownCommandEvent e = unknownCommand("team invite Floris");
        listener.onUnknownCommand(e);
        assertNull(e.message());
        checkSpigotMessage("/oneblock team invite Floris");
        assertClickRuns("/oneblock team invite Floris");
    }

    /**
     * Player types {@code /spawn}. On a server without a /spawn command, the
     * game mode's own spawn subcommand is the right guess.
     */
    @Test
    void testPlayerTypesSpawn() {
        UnknownCommandEvent e = unknownCommand("spawn");
        listener.onUnknownCommand(e);
        assertNull(e.message());
        checkSpigotMessage("/oneblock spawn");
        assertClickRuns("/oneblock spawn");
    }

    /**
     * After a suggestion, the player can just type "yes" in chat and the
     * suggested command runs for them.
     */
    @Test
    void testPlayerAcceptsByTypingYes() {
        listener.onUnknownCommand(unknownCommand("teams"));
        // Player answers yes (this is what the chat listener calls)
        assertTrue(listener.acceptPending(uuid));
        // The suggested command is run for the player on the main thread
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTask(eq(plugin), task.capture());
        mockedBukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(mockPlayer);
        task.getValue().run();
        verify(mockPlayer).performCommand("oneblock team");
    }

    /**
     * A pending suggestion is one-shot and only exists after a suggestion.
     */
    @Test
    void testYesWithNothingPendingDoesNothing() {
        assertFalse(listener.acceptPending(uuid));
    }

    /**
     * Running any other command drops the pending suggestion — the player has
     * moved on.
     */
    @Test
    void testRunningAnotherCommandClearsPending() {
        listener.onUnknownCommand(unknownCommand("teams"));
        listener.onCommandPreprocess(new PlayerCommandPreprocessEvent(mockPlayer, "/somethingelse", new HashSet<>()));
        assertFalse(listener.acceptPending(uuid));
    }

    /**
     * Gibberish is not force-matched: the vanilla unknown-command message stays
     * and nothing is sent.
     */
    @Test
    void testGibberishLeavesVanillaMessage() {
        UnknownCommandEvent e = unknownCommand("xyzzy");
        listener.onUnknownCommand(e);
        assertNotNull(e.message());
        verify(mockPlayer, never()).sendMessage(any(Component.class));
    }

    /**
     * Two game modes both have a team command. The player is standing in the
     * OneBlock world, so that context is decisive: one confident suggestion for
     * /oneblock team, not a list.
     */
    @Test
    void testWorldContextPicksTheRightGameMode() {
        GameModeCommand island = new GameModeCommand("island", "is");
        island.setWorld(mock(World.class));
        registeredCommands.put("island", island);
        listener.onUnknownCommand(unknownCommand("teams"));
        checkSpigotMessage("or type ");
        checkSpigotMessage("/oneblock team");
        assertClickRuns("/oneblock team");
    }

    /**
     * The same input from a neutral world (e.g. spawn) is genuinely ambiguous,
     * so the player gets a short list of clickable options instead.
     */
    @Test
    void testAmbiguousInputOffersClickableOptions() {
        GameModeCommand island = new GameModeCommand("island", "is");
        island.setWorld(mock(World.class));
        registeredCommands.put("island", island);
        // Player stands in a world that belongs to neither game mode
        when(mockPlayer.getWorld()).thenReturn(mock(World.class));
        listener.onUnknownCommand(unknownCommand("teams"));
        checkSpigotMessage("Did you mean one of these");
        checkSpigotMessage("/oneblock team");
        checkSpigotMessage("/island team");
        assertClickRuns("/oneblock team");
        assertClickRuns("/island team");
    }

    /**
     * The in-tree interception: {@code /oneblock invit Floris} dispatched
     * through the real command executor suggests
     * {@code /oneblock team invite Floris} instead of the unknown-command
     * error.
     */
    @Test
    void testMistypedSubcommandThroughDispatch() {
        assertTrue(oneblock.execute(mockPlayer, "oneblock", new String[] { "invit", "Floris" }));
        checkSpigotMessage("/oneblock team invite Floris");
        assertClickRuns("/oneblock team invite Floris");
        // The unknown-command error was never shown
        checkSpigotMessage("general.errors.unknown-command", 0);
    }

    /**
     * The suggestion respects the alias the player actually typed:
     * {@code /ob team invit} suggests {@code /ob team invite}.
     */
    @Test
    void testMistypedSubcommandKeepsTypedAlias() {
        assertTrue(oneblock.execute(mockPlayer, "ob", new String[] { "team", "invit", "Floris" }));
        checkSpigotMessage("/ob team invite Floris");
        assertClickRuns("/ob team invite Floris");
    }

    /**
     * The whole feature can be turned off in the config.
     */
    @Test
    void testConfigToggleOff() {
        plugin.getSettings().setDidYouMeanUnknownCommands(false);
        plugin.getSettings().setDidYouMeanSubcommands(false);
        UnknownCommandEvent e = unknownCommand("teams");
        listener.onUnknownCommand(e);
        assertNotNull(e.message());
        oneblock.execute(mockPlayer, "oneblock", new String[] { "invit", "Floris" });
        // The old unknown-command error is back, and no suggestion was made
        checkSpigotMessage("general.errors.unknown-command");
        checkSpigotMessage("Did you mean", 0);
    }

    private UnknownCommandEvent unknownCommand(String commandLine) {
        return new UnknownCommandEvent(commandSourceStack, commandLine, Component.text("Unknown command"));
    }

    /**
     * Asserts that some message sent to the player carries a click event that
     * runs the given command.
     */
    private void assertClickRuns(String command) {
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(mockPlayer, atLeast(1)).sendMessage(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(c -> hasClickRunning(c, command)),
                "No message carries a click event running " + command);
    }

    private boolean hasClickRunning(Component component, String command) {
        ClickEvent click = component.clickEvent();
        if (click != null && click.action() == ClickEvent.Action.RUN_COMMAND && command.equals(click.value())) {
            return true;
        }
        return component.children().stream().anyMatch(child -> hasClickRunning(child, command));
    }

    /**
     * A minimal stand-in for a game mode's player command (e.g. AOneBlock's
     * /oneblock): same shape, same unknown-command error on unmatched args.
     */
    private static class GameModeCommand extends CompositeCommand {

        GameModeCommand(String label, String... aliases) {
            super(label, aliases);
        }

        @Override
        public void setup() {
            new SubCommand(this, "go");
            new SubCommand(this, "spawn");
            new SubCommand(this, "create");
            CompositeCommand team = new SubCommand(this, "team");
            new SubCommand(team, "invite");
            new SubCommand(team, "accept");
            new SubCommand(team, "reject");
            new SubCommand(team, "kick");
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            if (!args.isEmpty()) {
                // Mirrors DefaultPlayerCommand's behavior for unmatched arguments
                user.sendMessage("general.errors.unknown-command", TextVariables.LABEL, getTopLabel());
                return false;
            }
            return true;
        }
    }

    private static class SubCommand extends CompositeCommand {

        SubCommand(CompositeCommand parent, String label, String... aliases) {
            super(parent, label, aliases);
        }

        @Override
        public void setup() {
            // Nothing to do
        }

        @Override
        public boolean execute(User user, String label, List<String> args) {
            return true;
        }
    }
}
