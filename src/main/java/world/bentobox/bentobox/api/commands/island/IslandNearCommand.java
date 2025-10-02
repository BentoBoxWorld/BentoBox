package world.bentobox.bentobox.api.commands.island;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.block.BlockFace;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * Handles the island near command (/island near).
 * <p>
 * This command shows information about neighboring islands in the four cardinal directions
 * (North, East, South, West) at a distance of twice the island spacing.
 * <p>
 * Features:
 * <ul>
 *   <li>Compass-based neighbor detection</li>
 *   <li>Shows island names or owner names</li>
 *   <li>Handles unowned islands</li>
 *   <li>Supports localization for directions</li>
 * </ul>
 * <p>
 * Permission: {@code island.near}
 *
 * @author tastybento
 * @since 1.5.0
 */
public class IslandNearCommand extends CompositeCommand {

    /**
     * The cardinal directions to check for neighboring islands.
     * Order determines the display order in the command output.
     */
    private static final List<BlockFace> COMPASS_POINTS = Arrays.asList(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

    public IslandNearCommand(CompositeCommand islandCommand) {
        super(islandCommand, "near");
    }

    @Override
    public void setup() {
        setPermission("island.near");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.near.parameters");
        setDescription("commands.island.near.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Explain command
        if (!args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        UUID playerUUID = user.getUniqueId();
        if (!getIslands().hasIsland(getWorld(), playerUUID) && !getIslands().inTeam(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    /**
     * Checks neighboring islands in all cardinal directions.
     * <p>
     * For each direction:
     * <ul>
     *   <li>Checks a location at 2x island distance</li>
     *   <li>Gets island at that location</li>
     *   <li>Displays name or owner of found islands</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get coordinates around island
        user.sendMessage("commands.island.near.the-following-islands");
        Island island = getIslands().getIsland(getWorld(), user);
        int dist = getIWM().getIslandDistance(getWorld()) * 2;
        boolean noNeighbors = true;
        for (BlockFace face : COMPASS_POINTS) {
            assert island != null;
            String name = getIslands().getIslandAt(
                    island
                    .getCenter()
                    .getBlock()
                    .getRelative(face, dist)
                    .getLocation())
                    .map(i -> getName(user, i)).orElse("");
            if (!name.isEmpty()) {
                noNeighbors = false;
                user.sendMessage("commands.island.near.syntax",
                        "[direction]", user.getTranslation("commands.island.near." + face.name().toLowerCase(Locale.ENGLISH)),
                        TextVariables.NAME, name);
            }
        }
        if (noNeighbors) {
            user.sendMessage("commands.island.near.no-neighbors");
        }
        return true;
    }

    /**
     * Gets the display name for an island.
     * Priority:
     * 1. Custom island name
     * 2. "Unowned" for unowned islands
     * 3. Owner's name
     *
     * @param user The user executing the command
     * @param island The island to get the name for
     * @return The display name for the island
     */
    private String getName(User user, Island island) {
        if (island.getName() != null && !island.getName().isEmpty()) {
            return island.getName();
        }
        if (island.isUnowned()) {
            return user.getTranslation("commands.admin.info.unowned");
        }
        return getPlayers().getName(island.getOwner());
    }
}
