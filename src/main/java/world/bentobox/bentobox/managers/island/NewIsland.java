package world.bentobox.bentobox.managers.island;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BStats;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
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
    private String name;
    private final boolean noPaste;
    private GameModeAddon addon;

    private enum Result {
        ISLAND_FOUND,
        BLOCK_AT_CENTER,
        BLOCKS_IN_AREA,
        FREE
    }

    public NewIsland(Builder builder) {
        plugin = BentoBox.getInstance();
        this.user = builder.user2;
        this.reason = builder.reason2;
        this.world = builder.world2;
        this.name = builder.name2;
        this.noPaste = builder.noPaste2;
        this.addon = builder.addon2;
        newIsland(builder.oldIsland2);
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
     */
    public static class Builder {
        private Island oldIsland2;
        private User user2;
        private Reason reason2;
        private World world2;
        private String name2 = BlueprintsManager.DEFAULT_BUNDLE_NAME;
        private boolean noPaste2;
        private GameModeAddon addon2;

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

        /**
         * @param world world where the island will go
         * @deprecated use {@link #addon} instead
         */
        @Deprecated
        public Builder world(World world) {
            this.world2 = world;
            return this;
        }

        /**
         * Set the addon
         * @param addon a game mode addon
         */
        public Builder addon(GameModeAddon addon) {
            this.addon2 = addon;
            this.world2 = addon.getOverWorld();
            return this;
        }

        /**
         * No blocks will be pasted
         */
        public Builder noPaste() {
            this.noPaste2 = true;
            return this;
        }

        /**
         * @param name - name of Blueprint bundle
         */
        public Builder name(String name) {
            this.name2 = name;
            return this;
        }

        /**
         * @return Island
         * @throws IOException - if there are insufficient parameters defined
         */
        public Island build() throws IOException {
            if (user2 != null) {
                NewIsland newIsland = new NewIsland(this);
                return newIsland.getIsland();
            }
            throw new IOException("Insufficient parameters. Must have a user!");
        }
    }

    /**
     * Makes an island.
     * @param oldIsland old island that is being replaced, if any
     */
    public void newIsland(Island oldIsland) {
        Location next = null;
        if (plugin.getIslands().hasIsland(world, user)) {
            // Island exists, it just needs pasting
            island = plugin.getIslands().getIsland(world, user);
            if (island != null && island.isReserved()) {
                next = island.getCenter();
                // Clear the reservation
                island.setReserved(false);
            } else {
                // This should never happen unless we allow another way to paste over islands without reserving
                plugin.logError("New island for user " + user.getName() + " was not reserved!");
            }
        }
        // If the reservation fails, then we need to make a new island anyway
        if (next == null) {
            next = getNextIsland();
            if (next == null) {
                plugin.logError("Failed to make island - no unoccupied spot found");
                return;
            }
            // Add to the grid
            island = plugin.getIslands().createIsland(next, user.getUniqueId());
            if (island == null) {
                plugin.logError("Failed to make island! Island could not be added to the grid.");
                return;
            }
        }

        // Clear any old home locations (they should be clear, but just in case)
        plugin.getPlayers().clearHomeLocations(world, user.getUniqueId());

        // Set home location
        plugin.getPlayers().setHomeLocation(user, new Location(next.getWorld(), next.getX() + 0.5D, next.getY(), next.getZ() + 0.5D), 1);

        // Reset deaths
        if (plugin.getIWM().isDeathsResetOnNewIsland(world)) {
            plugin.getPlayers().setDeaths(world, user.getUniqueId(), 0);
        }

        // Save the player so that if the server crashes weird things won't happen
        plugin.getPlayers().save(user.getUniqueId());

        // Fire event
        IslandBaseEvent event = IslandEvent.builder()
                .involvedPlayer(user.getUniqueId())
                .reason(reason)
                .island(island)
                .location(island.getCenter())
                .blueprintBundle(plugin.getBlueprintsManager().getBlueprintBundles(addon).get(name))
                .build();
        if (event.isCancelled()) {
            return;
        }

        // Get the new BlueprintBundle if it was changed
        switch (reason) {
        case CREATE:
            name = ((IslandEvent.IslandCreateEvent) event).getBlueprintBundle().getUniqueId();
            break;
        case RESET:
            name = ((IslandEvent.IslandResetEvent) event).getBlueprintBundle().getUniqueId();
            break;
        default:
            break;
        }

        // Task to run after creating the island
        Runnable task = () -> {
            // Set initial spawn point if one exists
            if (island.getSpawnPoint(Environment.NORMAL) != null) {
                plugin.getPlayers().setHomeLocation(user, island.getSpawnPoint(Environment.NORMAL), 1);
            }
            // Stop the player from falling or moving if they are
            if (user.isOnline()) {
                user.getPlayer().setVelocity(new Vector(0,0,0));
                user.getPlayer().setFallDistance(0F);

                // Teleport player after this island is built
                plugin.getIslands().homeTeleport(world, user.getPlayer(), true);
            } else {
                // Remove the player again to completely clear the data
                User.removePlayer(user.getPlayer());
            }
            // Delete old island
            if (oldIsland != null) {
                // Delete the old island
                plugin.getIslands().deleteIsland(oldIsland, true, user.getUniqueId());
            }

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
        };
        if (noPaste) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            // Create islands
            plugin.getBlueprintsManager().paste(addon, island, name, task);
        }
        // Set default settings
        island.setFlagsDefaults();
        if (!reason.equals(Reason.RESERVED)) {
            plugin.getMetrics().ifPresent(BStats::increaseIslandsCreatedCount);
        }
        // Save island
        plugin.getIslands().save(island);
    }

    /**
     * Get the location of next free island spot
     * @return Location of island spot or null if one cannot be found
     */
    @Nullable
    private Location getNextIsland() {
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
        Result r = isIsland(last);

        while (!r.equals(Result.FREE) && result.getOrDefault(Result.BLOCK_AT_CENTER, 0) < MAX_UNOWNED_ISLANDS) {
            nextGridLocation(last);
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
