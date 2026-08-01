package world.bentobox.bentobox.commands.brigadier;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;

/**
 * Tests the raw command line handling of {@link BrigadierCommandRegistrar}.
 * <p>
 * These are the parts that have to agree exactly with what
 * {@code CompositeCommand} expects: Brigadier hands over one greedy string, and
 * everything downstream depends on it being split the same way the legacy
 * command map split it. They need no server, so they run as plain unit tests.
 *
 * @author tastybento
 */
class BrigadierCommandRegistrarTest {

    @Test
    void tokenizeSplitsLabelAndArguments() {
        assertArrayEquals(new String[] { "island", "team", "invite" },
                BrigadierCommandRegistrar.tokenize("island team invite"));
    }

    @Test
    void tokenizeDropsLeadingSlash() {
        assertArrayEquals(new String[] { "island", "go" }, BrigadierCommandRegistrar.tokenize("/island go"));
    }

    @Test
    void tokenizeStripsThePluginNamespace() {
        // Paper offers every command as plugin:label as well as label
        assertArrayEquals(new String[] { "island", "go" },
                BrigadierCommandRegistrar.tokenize("/bskyblock:island go"));
        assertArrayEquals(new String[] { "acid" }, BrigadierCommandRegistrar.tokenize("acidisland:acid"));
    }

    @Test
    void tokenizeLeavesColonsInArgumentsAlone() {
        assertArrayEquals(new String[] { "island", "a:b" }, BrigadierCommandRegistrar.tokenize("island a:b"));
    }

    @Test
    void argsOfDropsTheLabel() {
        assertArrayEquals(new String[] { "team", "invite" },
                BrigadierCommandRegistrar.argsOf(new String[] { "island", "team", "invite" }, false));
    }

    @Test
    void argsOfIsEmptyForABareCommand() {
        assertArrayEquals(new String[0], BrigadierCommandRegistrar.argsOf(new String[] { "island" }, false));
    }

    @Test
    void argsOfAddsAnEmptyElementAfterATrailingSpace() {
        // "/island team " must complete the argument after team, not team itself
        assertArrayEquals(new String[] { "team", "" },
                BrigadierCommandRegistrar.argsOf(new String[] { "island", "team" }, true));
        assertArrayEquals(new String[] { "" }, BrigadierCommandRegistrar.argsOf(new String[] { "island" }, true));
    }

    @Test
    void suggestionsReplaceOnlyTheTokenBeingTyped() {
        // Greedy argument starts at index 7 of "island team inv"
        SuggestionsBuilder builder = new SuggestionsBuilder("island team inv", 7);
        assertEquals("team inv", builder.getRemaining());
        // Only "inv" should be replaced, i.e. the offset starts at 12
        assertEquals(12, BrigadierCommandRegistrar.atCurrentToken(builder).getStart());
    }

    @Test
    void suggestionsAfterATrailingSpaceInsertAtTheCursor() {
        SuggestionsBuilder builder = new SuggestionsBuilder("island team ", 7);
        assertEquals(12, BrigadierCommandRegistrar.atCurrentToken(builder).getStart());
    }

    @Test
    void suggestionsOnTheFirstArgumentKeepTheOriginalBuilder() {
        SuggestionsBuilder builder = new SuggestionsBuilder("island tea", 7);
        assertEquals(7, BrigadierCommandRegistrar.atCurrentToken(builder).getStart());
    }
}
