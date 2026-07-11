package world.bentobox.bentobox.suggestions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.util.Util;

/**
 * Pure matching engine for the "did you mean?" feature.
 * <p>
 * New players routinely type a subcommand as if it were a whole command
 * ({@code /teams} or {@code /team invite} instead of {@code /oneblock team}),
 * or mistype a subcommand ({@code /island invit}). This class matches what was
 * typed against <b>every node</b> of the given command trees - labels and
 * aliases, at any depth - and returns ranked candidate commands.
 * <p>
 * This class is a pure function of its inputs: it sends no messages, reads no
 * configuration and holds no state, which keeps it directly unit-testable.
 * Presentation and pending-intent handling live in {@link SuggestionsManager}.
 *
 * @author tastybento
 * @since 3.20.0
 */
public final class CommandMatcher {

    /** Minimum per-token quality for a token to be considered a match at all. */
    private static final double MIN_TOKEN_QUALITY = 0.6;
    /**
     * Score bonus when the command tree belongs to the world the player is in.
     * Deliberately equal to {@link SuggestionsManager#CONFIDENT_GAP} so that being
     * in a game mode's world is decisive when several game modes offer the same
     * subcommand (e.g. {@code team}).
     */
    private static final double WORLD_CONTEXT_BONUS = 0.2;
    /** Score penalty per level of implicit path the player did not actually type. */
    private static final double IMPLICIT_DEPTH_PENALTY = 0.05;
    /** How deep below each starting point the matcher looks for an entry node. */
    private static final int MAX_IMPLICIT_DEPTH = 2;
    /** Longest first token that is worth matching; anything longer is noise. */
    private static final int MAX_TOKEN_LENGTH = 30;

    /**
     * A candidate command for what the player probably meant.
     *
     * @param command       the command node the match ends on
     * @param commandString the full runnable command, starting with '/', with any
     *                      unmatched trailing tokens re-attached as arguments
     * @param score         ranking score; higher is more likely
     */
    public record Match(@NonNull CompositeCommand command, @NonNull String commandString, double score) {
    }

    private CommandMatcher() {
        // Static use only
    }

    /**
     * Matches a complete typed command line (as delivered by an unknown-command
     * event, without the leading slash) against a set of command trees.
     *
     * @param tokens       the typed command line, split on whitespace
     * @param roots        top-level commands to search
     * @param contextWorld the world the player is in, for context ranking; may be null
     * @param accessible   filter for commands the player is allowed to see/run
     * @return matches sorted best-first; empty if nothing plausible was found
     */
    public static List<Match> matchCommandLine(@NonNull List<String> tokens,
            @NonNull Collection<CompositeCommand> roots, @Nullable World contextWorld,
            @NonNull Predicate<CompositeCommand> accessible) {
        List<Match> results = new ArrayList<>();
        if (validTokens(tokens)) {
            for (CompositeCommand root : roots) {
                if (accessible.test(root)) {
                    double worldBonus = worldBonus(root, contextWorld);
                    collectEntries(tokens, root, "/" + root.getLabel(), 0, worldBonus, accessible, results);
                }
            }
        }
        return rank(results);
    }

    /**
     * Matches leftover arguments against the subcommand tree below a node. Used
     * when command dispatch stopped at {@code node} with arguments that matched
     * none of its subcommands (e.g. {@code /island invit Bob} stopping at
     * {@code /island}).
     *
     * @param tokens      the unconsumed arguments; the first is the presumed
     *                    mistyped subcommand
     * @param node        the command the dispatch walk stopped at
     * @param typedPrefix what the player actually typed to reach {@code node},
     *                    starting with '/' (e.g. {@code "/island"})
     * @param accessible  filter for commands the player is allowed to see/run
     * @return matches sorted best-first; empty if nothing plausible was found
     */
    public static List<Match> matchSubtree(@NonNull List<String> tokens, @NonNull CompositeCommand node,
            @NonNull String typedPrefix, @NonNull Predicate<CompositeCommand> accessible) {
        List<Match> results = new ArrayList<>();
        if (validTokens(tokens)) {
            for (CompositeCommand child : node.getSubCommands().values()) {
                if (isCandidate(child) && accessible.test(child)) {
                    collectEntries(tokens, child, typedPrefix + " " + child.getLabel(), 0, 0, accessible, results);
                }
            }
        }
        return rank(results);
    }

    private static boolean validTokens(List<String> tokens) {
        return !tokens.isEmpty() && !tokens.get(0).isBlank() && tokens.get(0).length() <= MAX_TOKEN_LENGTH;
    }

    /**
     * Considers {@code node} itself as the entry point the first typed token was
     * aiming at, then recurses into its subcommands (up to
     * {@link #MAX_IMPLICIT_DEPTH}) so that a subcommand typed as a root command
     * is found too.
     *
     * @param canonicalPath full runnable path up to and including {@code node}'s label
     * @param implicitDepth how many path elements the player did not type
     */
    private static void collectEntries(List<String> tokens, CompositeCommand node, String canonicalPath,
            int implicitDepth, double worldBonus, Predicate<CompositeCommand> accessible, List<Match> results) {
        tryMatch(tokens, node, canonicalPath, implicitDepth, worldBonus, accessible, results);
        if (implicitDepth < MAX_IMPLICIT_DEPTH) {
            for (CompositeCommand child : node.getSubCommands().values()) {
                if (isCandidate(child) && accessible.test(child)) {
                    collectEntries(tokens, child, canonicalPath + " " + child.getLabel(), implicitDepth + 1,
                            worldBonus, accessible, results);
                }
            }
        }
    }

    /**
     * Attempts to match the token list starting at {@code entry}: the first token
     * against the entry node itself, then following tokens greedily down its
     * subcommands. Unmatched trailing tokens are kept as command arguments.
     */
    private static void tryMatch(List<String> tokens, CompositeCommand entry, String canonicalPath,
            int implicitDepth, double worldBonus, Predicate<CompositeCommand> accessible, List<Match> results) {
        double quality = tokenQuality(tokens.get(0), entry);
        if (quality < MIN_TOKEN_QUALITY) {
            return;
        }
        double qualitySum = quality;
        int matchedTokens = 1;
        CompositeCommand node = entry;
        StringBuilder commandString = new StringBuilder(canonicalPath);
        int i = 1;
        while (i < tokens.size()) {
            CompositeCommand best = null;
            double bestQuality = MIN_TOKEN_QUALITY;
            for (CompositeCommand child : node.getSubCommands().values()) {
                if (isCandidate(child) && accessible.test(child)) {
                    double q = tokenQuality(tokens.get(i), child);
                    if (q >= bestQuality) {
                        bestQuality = q;
                        best = child;
                    }
                }
            }
            if (best == null) {
                break;
            }
            node = best;
            qualitySum += bestQuality;
            matchedTokens++;
            commandString.append(' ').append(best.getLabel());
            i++;
        }
        // Any leftover tokens are treated as arguments and re-attached verbatim
        while (i < tokens.size()) {
            commandString.append(' ').append(tokens.get(i));
            i++;
        }
        double score = qualitySum / matchedTokens + worldBonus - implicitDepth * IMPLICIT_DEPTH_PENALTY;
        results.add(new Match(node, commandString.toString(), score));
    }

    /**
     * Commands that should never be suggested: hidden ones, console-only ones and
     * the ubiquitous help command.
     */
    private static boolean isCandidate(CompositeCommand command) {
        return !command.isHidden() && !command.isOnlyConsole() && !"help".equals(command.getLabel());
    }

    private static double worldBonus(CompositeCommand root, @Nullable World contextWorld) {
        if (contextWorld == null || root.getWorld() == null) {
            return 0;
        }
        World commandWorld = Util.getWorld(root.getWorld());
        return contextWorld.equals(commandWorld) ? WORLD_CONTEXT_BONUS : 0;
    }

    /**
     * Best match quality of a typed token against a command's label and aliases.
     */
    private static double tokenQuality(String token, CompositeCommand command) {
        String typed = token.toLowerCase(Locale.ENGLISH);
        double best = quality(typed, command.getLabel().toLowerCase(Locale.ENGLISH));
        for (String alias : command.getAliases()) {
            best = Math.max(best, quality(typed, alias.toLowerCase(Locale.ENGLISH)));
        }
        return best;
    }

    /**
     * Similarity of a typed token to a candidate label in [0, 1]: 1.0 for an exact
     * match, otherwise the better of a prefix match and a length-scaled edit
     * distance, or 0 if they are not plausibly the same word.
     * Package-private for unit testing.
     */
    static double quality(String typed, String candidate) {
        if (typed.equals(candidate)) {
            return 1.0;
        }
        double result = 0;
        // Prefix typing: "invit" for "invite". Too-short prefixes are ambiguous.
        if (typed.length() >= 3 && candidate.startsWith(typed)) {
            result = 0.7 + 0.2 * typed.length() / candidate.length();
        }
        // Typos and plurals: "teams" for "team", "tem" for "team"
        int maxLength = Math.max(typed.length(), candidate.length());
        int allowed = maxLength <= 4 ? 1 : maxLength <= 7 ? 2 : 3;
        int distance = levenshtein(typed, candidate);
        if (distance <= allowed) {
            result = Math.max(result, 1.0 - (double) distance / maxLength);
        }
        return result;
    }

    /** Plain Levenshtein edit distance; inputs are short command labels. */
    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    /**
     * Sorts best-first and keeps only the best-scoring match per distinct command
     * string.
     */
    private static List<Match> rank(List<Match> results) {
        Map<String, Match> best = new LinkedHashMap<>();
        for (Match match : results) {
            best.merge(match.commandString(), match, (a, b) -> a.score() >= b.score() ? a : b);
        }
        return best.values().stream().sorted(Comparator.comparingDouble(Match::score).reversed()).toList();
    }
}
