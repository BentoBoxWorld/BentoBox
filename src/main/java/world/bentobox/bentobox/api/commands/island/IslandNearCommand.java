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
 * Tells the players which islands are near to them
 * @author tastybento
 * @since 1.5.0
 */
public class IslandNearCommand extends CompositeCommand {

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

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Get coordinates around island
        user.sendMessage("commands.island.near.the-following-islands");
        Island island = getIslands().getIsland(getWorld(), user);
        int dist = getIWM().getIslandDistance(getWorld()) * 2;
        boolean noNeighbors = true;
        for (BlockFace face : COMPASS_POINTS) {
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
