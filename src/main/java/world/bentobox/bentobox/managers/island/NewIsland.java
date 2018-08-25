package world.bentobox.bentobox.managers.island;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Create and paste a new island
 * @author tastybento
 *
 */
public class NewIsland {
    private static final Integer MAX_UNOWNED_ISLANDS = 10;
    private BentoBox plugin;
    private Island island;
    private final User user;
    private final Reason reason;
    private final World world;
    private enum Result {
        ISLAND_FOUND,
        BLOCK_AT_CENTER,
        BLOCKS_IN_AREA,
        FREE
    }

    private NewIsland(Island oldIsland, User user, Reason reason, World world) {
        super();
        plugin = BentoBox.getInstance();
        this.user = user;
        this.reason = reason;
        this.world = world;
        newIsland();
        if (oldIsland != null) {
            // Delete the old island
            plugin.getIslands().deleteIsland(oldIsland, true);
        }
    }

    /**
     * @return the island that was created
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Start building a new island
     * @return New island builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build a new island for a player
     * @author tastybento
     *
     */
    public static class Builder {
        private Island oldIsland2;
        private User user2;
        private Reason reason2;
        private World world2;

        public Builder oldIsland(Island oldIsland) {
            this.oldIsland2 = oldIsland;
            this.world2 = oldIsland.getWorld();
            return this;
        }


        public Builder player(User player) {
            this.user2 = player;
            return this;
        }

        public Builder reason(Reason reason) {
            this.reason2 = reason;
            return this;
        }

        public Builder world(World world) {
            this.world2 = world;
            return this;
        }

        public Island build() throws IOException {
            if (user2 != null) {
                NewIsland newIsland = new NewIsland(oldIsland2, user2, reason2, world2);
                return newIsland.getIsland();
            }
            throw new IOException("Insufficient parameters. Must have a schematic and a player");
        }
    }

    /**
     * Makes an island.
     */
    public void newIsland() {
        Location next = getNextIsland();
        if (next == null) {
            plugin.logError("Failed to make island - no unoccupied spot found");
            return;
        }
        // Add to the grid
        island = plugin.getIslands().createIsland(next, user.getUniqueId());
        // Save the player so that if the server is reset weird things won't happen

        // Clear any old home locations (they should be clear, but just in case)
        plugin.getPlayers().clearHomeLocations(world, user.getUniqueId());

        // Set home location
        plugin.getPlayers().setHomeLocation(user, next, 1);

        // Fire event
        IslandBaseEvent event = IslandEvent.builder()
                .involvedPlayer(user.getUniqueId())
                .reason(reason)
                .island(island)
                .location(island.getCenter())
                .build();
        if (event.isCancelled()) {
            return;
        }
        // Create island
        plugin.getSchemsManager().paste(world, island, () -> {
            // Set initial spawn point if one exists
            if (island.getSpawnPoint(Environment.NORMAL) != null) {
                plugin.getPlayers().setHomeLocation(user, island.getSpawnPoint(Environment.NORMAL), 1);
            }
            // Stop the player from falling or moving if they are
            user.getPlayer().setVelocity(new Vector(0,0,0));
            user.getPlayer().setFallDistance(0F);
            
            // Teleport player after this island is built
            plugin.getIslands().homeTeleport(world, user.getPlayer(), true);
        });
        // Make nether island
        if (plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world) && plugin.getIWM().getNetherWorld(world) != null) {
            plugin.getSchemsManager().paste(plugin.getIWM().getNetherWorld(world), island);
        }

        // Make end island
        if (plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world) && plugin.getIWM().getEndWorld(world) != null) {
            plugin.getSchemsManager().paste(plugin.getIWM().getEndWorld(world), island);
        }

        // Set default settings
        island.setFlagsDefaults();

        // Fire exit event
        Reason reasonDone = Reason.CREATED;
        switch (reason) {
        case CREATE:
            reasonDone = Reason.CREATED;
            break;
        case RESET:
            reasonDone = Reason.RESETTED;
            break;
        default:
            break;
        }
        IslandEvent.builder()
        .involvedPlayer(user.getUniqueId())
        .reason(reasonDone)
        .island(island)
        .location(island.getCenter())
        .build();

    }

    /**
     * Get the location of next free island spot
     * @return Location of island spot or null if one cannot be found
     */
    private Location getNextIsland() {
        Location last = plugin.getIslands().getLast(world);
        if (last == null) {
            last = new Location(world, plugin.getIWM().getIslandXOffset(world) + plugin.getIWM().getIslandStartX(world),
                    plugin.getIWM().getIslandHeight(world), plugin.getIWM().getIslandZOffset(world) + plugin.getIWM().getIslandStartZ(world));
        }
        // Find a free spot
        Map<Result, Integer> result = new EnumMap<>(Result.class);
        Result r = isIsland(last);
        while (!r.equals(Result.FREE) && result.getOrDefault(Result.BLOCK_AT_CENTER, 0) < MAX_UNOWNED_ISLANDS) {
            last = nextGridLocation(last);
            result.merge(r, 1, (k,v) -> v++);
            r = isIsland(last);
        }
        if (!r.equals(Result.FREE)) {
            // We could not find a free spot within the limit required. It's likely this world is not empty
            plugin.logError("Could not find a free spot for islands! Is this world empty?");
            plugin.logError("Blocks at center locations: " + result.getOrDefault(Result.BLOCK_AT_CENTER, 0) + " max " + MAX_UNOWNED_ISLANDS);
            plugin.logError("Blocks around center locations: " + result.getOrDefault(Result.BLOCKS_IN_AREA, 0) + " max " + MAX_UNOWNED_ISLANDS);
            plugin.logError("Known islands: " + result.getOrDefault(Result.ISLAND_FOUND, 0) + " max unlimited.");
            return null;
        }
        plugin.getIslands().setLast(last);
        return last;
    }

    /**
     * Checks if there is an island or blocks at this location
     * @param location - the location
     * @return true if island found, null if blocks found, false if nothing found
     */
    private Result isIsland(Location location){
        location = Util.getClosestIsland(location);
        if (plugin.getIslands().getIslandAt(location).isPresent()) {
            return Result.ISLAND_FOUND;
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
                        if (!location.getWorld().getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ()).isEmpty()
                                && !location.getWorld().getBlockAt(x + location.getBlockX(), y, z + location.getBlockZ()).getType().equals(Material.WATER)) {
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
