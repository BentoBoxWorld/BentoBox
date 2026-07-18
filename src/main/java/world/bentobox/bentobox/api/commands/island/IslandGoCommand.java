package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.bukkit.World;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.dialogs.DialogBuilder;
import world.bentobox.bentobox.api.dialogs.DialogButton;
import world.bentobox.bentobox.api.dialogs.Dialogs;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island teleport command (/island go).
 * <p>
 * This command teleports players to their islands or specific island homes.
 * Extends {@link DelayedTeleportCommand} to provide a configurable delay
 * before teleporting to prevent abuse.
 * <p>
 * Features:
 * <ul>
 *   <li>Multiple home locations support</li>
 *   <li>Named homes and islands</li>
 *   <li>Teleport delay and cancellation on movement</li>
 *   <li>Fall protection (prevents teleporting while falling)</li>
 *   <li>Reserved island handling</li>
 * </ul>
 * <p>
 * Aliases: go, home, h
 * Permission: {@code island.home}
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandGoCommand extends DelayedTeleportCommand {

    public IslandGoCommand(CompositeCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
    }

    @Override
    public void setup() {
        setPermission("island.home");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.go.parameters");
        setDescription("commands.island.go.description");
    }

    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Not already in teleport process</li>
     *   <li>Has at least one island</li>
     *   <li>Island is not reserved</li>
     *   <li>Not falling (if PREVENT_TELEPORT_WHEN_FALLING flag is set)</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check if mid-teleport
        if (getIslands().isGoingHome(user)) {
            // Tell them again that it's in progress
            user.sendMessage("commands.island.go.in-progress");
            return false;
        }
        List<Island> islands = getIslands().getIslands(getWorld(), user.getUniqueId());
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check if the island is reserved
        if (checkReserved(user, islands)) {
            return false;
        }
        // Prevent command if player is falling and it's not allowed
        if ((getIWM().inWorld(user.getWorld()) && Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()))
                && user.getPlayer().getFallDistance() > 0) {
            // We're sending the "hint" to the player to tell them they cannot teleport while falling.
            user.sendMessage(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            return false;
        }
        return true;
    }

    /**
     * Handles the teleport process.
     * <p>
     * If no arguments are provided, teleports to the default home.
     * If arguments are provided, attempts to teleport to the named home or island.
     * Sets the teleported island as the primary island for the user.
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get a map of potential names - this includes island names and home names
        Map<String, IslandInfo> names = getNameIslandMap(user, getWorld());
        // Check if the name is known if one is given
        if (!args.isEmpty()) {
            // Assemble the arguments into one string
            final String typed = String.join(" ", args);
            // Forgiving lookup: exact, then case/space-insensitive, then unique prefix
            final String name = resolveName(typed, names.keySet());
            // If the name could not be resolved to a destination
            if (name == null) {
                // Failed home name check
                user.sendMessage("commands.island.go.unknown-home");
                user.sendMessage("commands.island.sethome.homes-are");
                names.keySet().forEach(n -> user.sendRawMessage(
                        user.getTranslation("commands.island.sethome.home-list-syntax", TextVariables.NAME, n)
                                + "[run_command: /" + getTopLabel() + " go " + n + "]"
                                + "[hover: " + user.getTranslation("commands.island.sethome.click-to-teleport") + "]"));
                return false;
            } else {
                // We know where this location is. Teleport there.
                teleportToNamed(user, name, names.get(name));
                return true;
            }
        }

        // No name given. If the player has several destinations, offer a picker dialog
        if (showGoPicker(user, names)) {
            return true;
        }

        this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer()));
        return true;
    }

    /**
     * Teleports the user to a resolved destination, whether it is a named home or an
     * island name.
     *
     * @param user the user to teleport
     * @param name the resolved (canonical) destination name
     * @param info the island/home the name refers to
     */
    private void teleportToNamed(User user, String name, IslandInfo info) {
        getIslands().setPrimaryIsland(user.getUniqueId(), info.island());
        if (!info.islandName()) {
            // This is a home name, not an island name
            this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer(), name)
                    .thenAccept(r -> {
                        if (Boolean.TRUE.equals(r)) {
                            getIslands().setPrimaryIsland(user.getUniqueId(), info.island());
                        } else {
                            user.sendMessage("commands.island.go.failure");
                            getPlugin().logError(
                                    user.getName() + " could not teleport to their island - async teleport issue");
                        }
                    }));
        } else {
            // An island name, so teleport to the island
            this.delayCommand(user, () -> getIslands().homeTeleportAsync(info.island(), user));
        }
    }

    /**
     * Shows a button-per-destination picker dialog when the player has more than one
     * island or home. Each button teleports to that destination.
     *
     * @param user  the user
     * @param names the destination map
     * @return true if the picker was shown; false to fall through to the default teleport
     */
    private boolean showGoPicker(User user, Map<String, IslandInfo> names) {
        if (!user.isPlayer() || names.size() < 2 || !Dialogs.isSupported()
                || !getPlugin().getSettings().isDialogGoPicker()) {
            return false;
        }
        try {
            DialogBuilder builder = new DialogBuilder().title(user, "commands.island.go.picker.title");
            // Stable, alphabetical button order
            names.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(en -> {
                String name = en.getKey();
                IslandInfo info = en.getValue();
                builder.button(new DialogButton(Component.text(name), u -> teleportToNamed(u, name, info)));
            });
            builder.build().show(user);
            return true;
        } catch (Exception e) {
            getPlugin().logError("Could not show go picker dialog, falling back to teleport: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if any of the user's islands are reserved.
     * If a reserved island is found, redirects the user to island creation.
     *
     * @param user The user to check
     * @param islands List of islands to check
     * @return true if any island is reserved
     */
    private boolean checkReserved(User user, List<Island> islands) {
        for (Island island : islands) {
            if (island.isReserved()) {
                // Send player to create an island
                getParent().getSubCommand("create").ifPresent(createCmd -> createCmd.call(user, createCmd.getLabel(), Collections.emptyList()));
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";

        return Optional.of(Util.tabLimit(new ArrayList<>(getNameIslandMap(user, getWorld()).keySet()), lastArg));

    }

    /**
     * Record to store island information and whether the name refers to
     * an island name or a home location.
     */
    public record IslandInfo(
            /**
             * The island
             */
            Island island, 
            /**
             * True if this is an island name as opposed to a home name
             */
            boolean islandName) {
    }

    /**
     * Resolves the text a player typed to one of the valid destination names using
     * forgiving matching, so small mistakes still teleport them instead of dumping a
     * list. Matching is tried in decreasing order of confidence:
     * <ol>
     *   <li>exact match (existing behaviour, always wins);</li>
     *   <li>case-, colour- and whitespace-insensitive exact match;</li>
     *   <li>a unique case-insensitive prefix (e.g. {@code hom} for {@code Home}).</li>
     * </ol>
     * Any step that is ambiguous (more than one candidate) is skipped, so an unclear
     * input falls through to the normal unknown-home list rather than guessing.
     *
     * @param typed the raw string the player typed
     * @param names the valid destination names
     * @return the canonical destination name to use, or {@code null} if none matched confidently
     */
    static String resolveName(String typed, Set<String> names) {
        // 1. Exact match wins and preserves the original behaviour
        if (names.contains(typed)) {
            return typed;
        }
        String norm = normalize(typed);
        if (norm.isEmpty()) {
            return null;
        }
        // 2. Case/colour/whitespace-insensitive exact match, if unambiguous
        List<String> exact = names.stream().filter(n -> normalize(n).equals(norm)).toList();
        if (exact.size() == 1) {
            return exact.get(0);
        }
        if (!exact.isEmpty()) {
            // Several names collapse to the same normalized form - too ambiguous to guess
            return null;
        }
        // 3. Unique prefix match
        List<String> prefix = names.stream().filter(n -> normalize(n).startsWith(norm)).toList();
        return prefix.size() == 1 ? prefix.get(0) : null;
    }

    /**
     * Normalizes a name for forgiving comparison: strips colour codes, lower-cases and
     * collapses runs of whitespace to a single space.
     *
     * @param s the string to normalize
     * @return the normalized form
     */
    private static String normalize(String s) {
        return Util.stripColor(s).toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ").trim();
    }

    /**
     * Creates a mapping of valid teleport destination names for a user.
     * Includes:
     * <ul>
     *   <li>Island names (with index if unnamed)</li>
     *   <li>Home location names</li>
     * </ul>
     *
     * @param user The user whose destinations to map
     * @param world The world to check
     * @return Map of destination names to island information
     */
    public static Map<String, IslandInfo> getNameIslandMap(User user, World world) {
        Map<String, IslandInfo> islandMap = new HashMap<>();
        int index = 0;
        for (Island island : BentoBox.getInstance().getIslands().getIslands(world, user.getUniqueId())) {
            index++;
            if (island.getName() != null && !island.getName().isBlank()) {
                // Name has been set
                // Color codes need to be stripped because they are not allowed in chat
                islandMap.put(Util.stripColor(island.getName()), new IslandInfo(island, true));
            } else {
                // Name has not been set
                String text = Util.stripColor(
                        user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME,
                                user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName()) + " " + index);
                islandMap.put(text, new IslandInfo(island, true));
            }
            // Add homes. Homes do not need an island specified
            island.getHomes().keySet().stream().filter(n -> !n.isBlank())
            .forEach(n -> islandMap.put(n, new IslandInfo(island, false)));
        }

        return islandMap;

    }

}
