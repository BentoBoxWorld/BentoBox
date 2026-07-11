package world.bentobox.bentobox.suggestions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Predicate;

import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.suggestions.CommandMatcher.Match;
import world.bentobox.bentobox.util.Util;

/**
 * Tests the pure matching engine behind the did-you-mean feature (#3027).
 *
 * @author tastybento
 */
class CommandMatcherTest extends CommonTestSetup {

    private static final Predicate<CompositeCommand> ALL = c -> true;

    private GameModeCommand oneblock;
    private GameModeCommand island;
    @Mock
    private World oneblockWorld;
    @Mock
    private World islandWorld;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);
        // Identity world normalization so distinct mock worlds compare correctly
        mockedUtil.when(() -> Util.getWorld(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // Two game modes with the standard command shape
        oneblock = new GameModeCommand("oneblock", "ob");
        oneblock.setWorld(oneblockWorld);
        island = new GameModeCommand("island", "is");
        island.setWorld(islandWorld);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * The headline case: a player types the subcommand as if it were the whole
     * command, with a plural on top. {@code /teams} → {@code /oneblock team}.
     */
    @Test
    void testSubcommandTypedAsRootCommand() {
        List<Match> matches = CommandMatcher.matchCommandLine(List.of("teams"), List.of(oneblock), null, ALL);
        assertFalse(matches.isEmpty());
        assertEquals("/oneblock team", matches.get(0).commandString());
    }

    /**
     * A whole subcommand path typed as a root command keeps its arguments:
     * {@code /team invite Floris} → {@code /oneblock team invite Floris}.
     */
    @Test
    void testSubcommandPathTypedAsRootCommandKeepsArgs() {
        List<Match> matches = CommandMatcher.matchCommandLine(List.of("team", "invite", "Floris"),
                List.of(oneblock), null, ALL);
        assertFalse(matches.isEmpty());
        assertEquals("/oneblock team invite Floris", matches.get(0).commandString());
    }

    /**
     * A typo in the root command itself: {@code /oneblok} → {@code /oneblock}.
     */
    @Test
    void testTypoInRootCommand() {
        List<Match> matches = CommandMatcher.matchCommandLine(List.of("oneblok"), List.of(oneblock), null, ALL);
        assertFalse(matches.isEmpty());
        assertEquals("/oneblock", matches.get(0).commandString());
    }

    /**
     * In-tree matching: dispatch stopped at /oneblock with args "invit Floris".
     * The mistyped grandchild is found: {@code /oneblock team invite Floris}.
     */
    @Test
    void testMistypedSubcommandInSubtree() {
        List<Match> matches = CommandMatcher.matchSubtree(List.of("invit", "Floris"), oneblock, "/oneblock", ALL);
        assertFalse(matches.isEmpty());
        assertEquals("/oneblock team invite Floris", matches.get(0).commandString());
    }

    /**
     * Gibberish must not produce false positives.
     */
    @Test
    void testGibberishMatchesNothing() {
        assertTrue(CommandMatcher.matchCommandLine(List.of("xyzzy"), List.of(oneblock, island), null, ALL).isEmpty());
        assertTrue(CommandMatcher.matchSubtree(List.of("xyzzy"), oneblock, "/oneblock", ALL).isEmpty());
    }

    /**
     * When several game modes offer the same subcommand, the world the player is
     * standing in is decisive: the gap to the runner-up is at least the
     * confident-suggestion threshold.
     */
    @Test
    void testWorldContextOutranksOtherGameModes() {
        List<Match> matches = CommandMatcher.matchCommandLine(List.of("teams"), List.of(island, oneblock),
                oneblockWorld, ALL);
        assertTrue(matches.size() >= 2);
        assertEquals("/oneblock team", matches.get(0).commandString());
        assertEquals("/island team", matches.get(1).commandString());
        assertTrue(matches.get(0).score() - matches.get(1).score() >= SuggestionsManager.CONFIDENT_GAP - 1e-9);
    }

    /**
     * Commands the player cannot access are never suggested.
     */
    @Test
    void testInaccessibleCommandsAreFiltered() {
        Predicate<CompositeCommand> onlyOneblock = c -> {
            CompositeCommand root = c;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            return root == oneblock;
        };
        List<Match> matches = CommandMatcher.matchCommandLine(List.of("teams"), List.of(island, oneblock), null,
                onlyOneblock);
        assertFalse(matches.isEmpty());
        assertTrue(matches.stream().allMatch(m -> m.commandString().startsWith("/oneblock")));
    }

    /**
     * Root aliases are matched too.
     */
    @Test
    void testRootAliasMatch() {
        List<Match> matches = CommandMatcher.matchCommandLine(List.of("obb"), List.of(oneblock), null, ALL);
        assertFalse(matches.isEmpty());
        assertEquals("/oneblock", matches.get(0).commandString());
    }

    /**
     * The token similarity heuristics themselves.
     */
    @Test
    void testQualityHeuristics() {
        assertEquals(1.0, CommandMatcher.quality("team", "team"), 1e-9);
        // Plural: one edit away
        assertEquals(0.8, CommandMatcher.quality("teams", "team"), 1e-9);
        // Prefix typing scores well
        assertTrue(CommandMatcher.quality("invit", "invite") > 0.8);
        // Unrelated words score zero
        assertEquals(0.0, CommandMatcher.quality("xyz", "team"), 1e-9);
        assertEquals(0.0, CommandMatcher.quality("spawn", "setname"), 1e-9);
    }

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
