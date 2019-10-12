package world.bentobox.bentobox.managers.island;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

/**
 * The default strategy for generating locations for island
 * @author tastybento
 *
 */
public class DefaultNewIslandLocationStrategy implements NewIslandLocationStrategy {

    /**
     * The amount times to tolerate island check returning blocks without kwnon
     * island.
     */
    protected static final Integer MAX_UNOWNED_ISLANDS = 10;

    protected enum Result {
        ISLAND_FOUND, BLOCK_AT_CENTER, BLOCKS_IN_AREA, FREE
    }

    protected BentoBox plugin = BentoBox.getInstance();

    public Location getNextLocation(World world) {
        Location last = plugin.getIslands().getLast(world);
        if (last == null) {
            last = new Location(world,
                    (double) plugin.getIWM().getIslandXOffset(world) + plugin.getIWM().getIslandStartX(world),
                    plugin.getIWM().getIslandHeight(world),
                    (double) plugin.getIWM().getIslandZOffset(world) + plugin.getIWM().getIslandStartZ(world));
        }
        // Find a free spot
        Map<Result, Integer> result = new EnumMap<>(Result.class);
        // Check center
        last = Util.getClosestIsland(last);
        Result r = isIsland(last);

        while (!r.equals(Result.FREE) && result.getOrDefault(Result.BLOCK_AT_CENTER, 0) < MAX_UNOWNED_ISLANDS) {
            nextGridLocation(last);
            last = Util.getClosestIsland(last);
            result.merge(r, 1, (k, v) -> v++);
            r = isIsland(last);
        }

        if (!r.equals(Result.FREE)) {
            // We could not find a free spot within the limit required. It's likely this
            // world is not empty
            plugin.logError("Could not find a free spot for islands! Is this world empty?");
            plugin.logError("Blocks at center locations: " + result.getOrDefault(Result.BLOCK_AT_CENTER, 0) + " max "
                    + MAX_UNOWNED_ISLANDS);
            plugin.logError("Blocks around center locations: " + result.getOrDefault(Result.BLOCKS_IN_AREA, 0) + " max "
                    + MAX_UNOWNED_ISLANDS);
            plugin.logError("Known islands: " + result.getOrDefault(Result.ISLAND_FOUND, 0) + " max unlimited.");
            return null;
        }
        plugin.getIslands().setLast(last);
        return last;
    }

    /**
     * Checks if there is an island or blocks at this location
     * 
     * @param location - the location
     * @return true if island found, null if blocks found, false if nothing found
     */
    protected Result isIsland(Location location) {

        // Check 4 corners
        int dist = plugin.getIWM().getIslandDistance(location.getWorld());
        Set<Location> locs = new HashSet<>();
        locs.add(location);

        locs.add(new Location(location.getWorld(), location.getX() - dist, 0, location.getZ() - dist));
        locs.add(new Location(location.getWorld(), location.getX() - dist, 0, location.getZ() + dist - 1));
        locs.add(new Location(location.getWorld(), location.getX() + dist - 1, 0, location.getZ() - dist));
        locs.add(new Location(location.getWorld(), location.getX() + dist - 1, 0, location.getZ() + dist - 1));

        for (Location l : locs) {
            if (plugin.getIslands().getIslandAt(l).isPresent() || plugin.getIslandDeletionManager().inDeletion(l)) {
                return Result.ISLAND_FOUND;
            }
        }

        if (!plugin.getIWM().isUseOwnGenerator(location.getWorld())) {
            // Block check
            if (!location.getBlock().isEmpty() && !location.getBlock().getType().equals(Material.WATER)) {
                plugin.getIslands().createIsland(location);
                return Result.BLOCK_AT_CENTER;
            }
            // Look around
            for (int x = -5; x <= 5; x++) {
                for (int y = 10; y < location.getWorld().getMaxHeight(); y++) {
                    for (int z = -5; z <= 5; z++) {
                        if (!location.getWorld().getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ())
                                .isEmpty()
                                && !location.getWorld()
                                        .getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ()).getType()
                                        .equals(Material.WATER)) {
                            plugin.getIslands().createIsland(location);
                            return Result.BLOCKS_IN_AREA;
                        }
                    }
                }
            }
        }
        return Result.FREE;
    }

    /**
     * Finds the next free island spot based off the last known island Uses
     * island_distance setting from the config file Builds up in a grid fashion
     *
     * @param lastIsland - last island location
     * @return Location of next free island
     */
    private Location nextGridLocation(final Location lastIsland) {
        int x = lastIsland.getBlockX();
        int z = lastIsland.getBlockZ();
        int d = plugin.getIWM().getIslandDistance(lastIsland.getWorld()) * 2;
        if (x < z) {
            if (-1 * x < z) {
                lastIsland.setX(lastIsland.getX() + d);
                return lastIsland;
            }
            lastIsland.setZ(lastIsland.getZ() + d);
            return lastIsland;
        }
        if (x > z) {
            if (-1 * x >= z) {
                lastIsland.setX(lastIsland.getX() - d);
                return lastIsland;
            }
            lastIsland.setZ(lastIsland.getZ() - d);
            return lastIsland;
        }
        if (x <= 0) {
            lastIsland.setZ(lastIsland.getZ() + d);
            return lastIsland;
        }
        lastIsland.setZ(lastIsland.getZ() - d);
        return lastIsland;
    }
}