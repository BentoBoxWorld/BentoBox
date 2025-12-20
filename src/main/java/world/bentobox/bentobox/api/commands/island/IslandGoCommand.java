package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
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
        Map<String, IslandInfo> names = getNameIslandMap(user, getWorld());
        // Check if the home is known
        if (!args.isEmpty()) {
            final String name = String.join(" ", args);
            if (!names.containsKey(name)) {
                // Failed home name check
                user.sendMessage("commands.island.go.unknown-home");
                user.sendMessage("commands.island.sethome.homes-are");
                names.keySet().forEach(n -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, n));
                return false;
            } else {
                IslandInfo info = names.get(name);
                getIslands().setPrimaryIsland(user.getUniqueId(), info.island);
                if (!info.islandName) {
                    this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer(), name)
                            .thenAccept((r) -> {
                                if (r) {
                                    // Success
                                    getIslands().setPrimaryIsland(user.getUniqueId(), info.island);
                                } else {
                                    user.sendMessage("commands.island.go.failure");
                                    getPlugin().logError(user.getName() + " could not teleport to their island - async teleport issue");
                                }
                            }));
                    return true;
                }
            }
        }
        this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer()));
        return true;
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
    public record IslandInfo(Island island, boolean islandName) {
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
