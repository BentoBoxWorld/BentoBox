package world.bentobox.bentobox.suggestions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.suggestions.CommandMatcher.Match;
import world.bentobox.bentobox.util.Util;

/**
 * Turns {@link CommandMatcher} results into player-facing "did you mean?"
 * suggestions and tracks the pending suggestion each player can accept by
 * typing {@code yes}.
 * <p>
 * Suggestion messages are sent through the normal locale system and use the
 * {@code [run_command: ...]} inline tag, so clicking the message runs the
 * suggested command directly - no re-typing needed.
 *
 * @author tastybento
 * @since 3.20.0
 */
public class SuggestionsManager {

    /** How long a player has to type "yes" after receiving a suggestion. */
    private static final long PENDING_EXPIRY_MS = 30_000;
    /** Matches below this score are never shown. */
    private static final double MIN_SCORE = 0.6;
    /**
     * Lead the best match needs over the runner-up to be presented as a single
     * confident suggestion rather than a list of options.
     */
    static final double CONFIDENT_GAP = 0.2;
    /** Maximum number of options shown when several matches are plausible. */
    private static final int MAX_OPTIONS = 3;
    /** Tolerance for floating-point score comparisons. */
    private static final double EPSILON = 1e-9;
    /** Longest command line worth analysing. */
    private static final int MAX_COMMAND_LINE_LENGTH = 256;

    private static final String COMMAND_VAR = "[command]";

    private final BentoBox plugin;
    private final Map<UUID, PendingSuggestion> pending = new ConcurrentHashMap<>();

    private record PendingSuggestion(String command, long expiry) {
    }

    public SuggestionsManager(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles a command line no plugin knows, e.g. a player typing
     * {@code /teams} when they meant {@code /oneblock team}. Matches it against
     * every registered BentoBox command tree.
     *
     * @param user        the player who typed the command
     * @param commandLine the full command line without the leading slash
     * @return true if a suggestion was sent to the player
     */
    public boolean suggestCommand(@NonNull User user, @Nullable String commandLine) {
        if (commandLine == null || commandLine.isBlank() || commandLine.length() > MAX_COMMAND_LINE_LENGTH) {
            return false;
        }
        List<String> tokens = Arrays.asList(commandLine.trim().split("\\s+"));
        World contextWorld = user.isPlayer() ? Util.getWorld(user.getWorld()) : null;
        List<Match> matches = CommandMatcher.matchCommandLine(tokens,
                plugin.getCommandsManager().getCommands().values(), contextWorld, cmd -> isAccessible(user, cmd));
        return sendSuggestions(user, matches);
    }

    /**
     * Handles arguments that matched none of a command's subcommands, e.g.
     * {@code /island invit Bob}. Matches them against the subcommand tree below
     * the command the dispatch walk stopped at.
     *
     * @param user        the player who typed the command
     * @param command     the command the dispatch walk stopped at
     * @param typedPrefix what the player typed to reach {@code command},
     *                    starting with '/' (e.g. {@code "/island"})
     * @param args        the unconsumed arguments
     * @return true if a suggestion was sent to the player
     */
    public boolean suggestSubcommand(@NonNull User user, @NonNull CompositeCommand command,
            @NonNull String typedPrefix, @NonNull List<String> args) {
        List<Match> matches = CommandMatcher.matchSubtree(args, command, typedPrefix,
                cmd -> isAccessible(user, cmd));
        return sendSuggestions(user, matches);
    }

    /**
     * Consumes the player's pending suggestion if there is one and it has not
     * expired.
     *
     * @param uuid the player's UUID
     * @return the suggested command (starting with '/') if one was pending
     */
    public Optional<String> acceptPending(@NonNull UUID uuid) {
        PendingSuggestion suggestion = pending.remove(uuid);
        return suggestion != null && System.currentTimeMillis() <= suggestion.expiry()
                ? Optional.of(suggestion.command())
                : Optional.empty();
    }

    /**
     * Drops any pending suggestion for this player, e.g. because they ran some
     * other command and have moved on.
     *
     * @param uuid the player's UUID
     */
    public void clearPending(@NonNull UUID uuid) {
        pending.remove(uuid);
    }

    /**
     * Never suggest a command the player would not be allowed to run. Walks the
     * permission chain up through the parents.
     */
    private boolean isAccessible(User user, CompositeCommand command) {
        CompositeCommand node = command;
        while (node != null) {
            if (!user.hasPermission(node.getPermission())) {
                return false;
            }
            node = node.getParent();
        }
        return true;
    }

    private boolean sendSuggestions(User user, List<Match> matches) {
        List<Match> plausible = matches.stream().filter(m -> m.score() >= MIN_SCORE).limit(MAX_OPTIONS).toList();
        if (plausible.isEmpty()) {
            return false;
        }
        if (plausible.size() == 1 || plausible.get(0).score() - plausible.get(1).score() >= CONFIDENT_GAP - EPSILON) {
            // One clear winner: suggest it and let the player accept by typing "yes"
            String command = plausible.getFirst().commandString();
            user.sendMessage("general.did-you-mean.suggestion", COMMAND_VAR, command);
            pending.put(user.getUniqueId(), new PendingSuggestion(command, System.currentTimeMillis() + PENDING_EXPIRY_MS));
        } else {
            // Several plausible options: list them, each clickable
            user.sendMessage("general.did-you-mean.header");
            plausible.forEach(m -> user.sendMessage("general.did-you-mean.option", COMMAND_VAR, m.commandString()));
        }
        return true;
    }
}
