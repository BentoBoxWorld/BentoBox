package us.tastybento.bskyblock.managers.island;

import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.World;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.island.builders.IslandBuilder;
import us.tastybento.bskyblock.island.builders.IslandBuilder.IslandType;

/**
 * Create and paste a new island
 * @author tastybento
 *
 */
public class NewIsland {
    private BSkyBlock plugin;
    private Island island;
    private final User user;
    private final Reason reason;
    private final World world;

    private NewIsland(Island oldIsland, User user, Reason reason, World world) {
        super();
        plugin = BSkyBlock.getInstance();
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
        if (!event.isCancelled()) {
            // Create island
            new IslandBuilder(plugin, island)
            .setPlayer(user.getPlayer())
            .setChestItems(plugin.getSettings().getChestItems())
            .setType(IslandType.ISLAND)
            .build();
            if (plugin.getSettings().isNetherGenerate() && plugin.getSettings().isNetherIslands() && plugin.getIWM().getNetherWorld() != null) {
                new IslandBuilder(plugin,island)
                .setPlayer(user.getPlayer())
                .setChestItems(plugin.getSettings().getChestItems())
                .setType(IslandType.NETHER)
                .build();
            }
            if (plugin.getSettings().isEndGenerate() && plugin.getSettings().isEndIslands() && plugin.getIWM().getEndWorld() != null) {
                new IslandBuilder(plugin,island)
                .setPlayer(user.getPlayer())
                .setChestItems(plugin.getSettings().getChestItems())
                .setType(IslandType.END)
                .build();
            }
            // Teleport player to their island
            plugin.getIslands().homeTeleport(world, user.getPlayer());
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
    }

    /**
     * Get the location of next free island spot
     * @return Location of island spot
     */
    private Location getNextIsland() {
        Location last = plugin.getIslands().getLast(world);
        if (last == null) {
            last = new Location(world, plugin.getIWM().getIslandXOffset(world) + plugin.getIWM().getIslandStartX(world),
                    plugin.getIWM().getIslandHeight(world), plugin.getIWM().getIslandZOffset(world) + plugin.getIWM().getIslandStartZ(world));
        }
        Location next = last.clone();
        while (plugin.getIslands().isIsland(next)) {
            next = nextGridLocation(next);
        }
        return next;
    }

    /**
     * Finds the next free island spot based off the last known island Uses
     * island_distance setting from the config file Builds up in a grid fashion
     *
     * @param lastIsland
     * @return Location of next free island
     */
    private Location nextGridLocation(final Location lastIsland) {
        int x = lastIsland.getBlockX();
        int z = lastIsland.getBlockZ();
        int d = plugin.getIWM().getIslandDistance(lastIsland.getWorld()) * 2;
        Location nextPos = lastIsland;
        if (x < z) {
            if (-1 * x < z) {
                nextPos.setX(nextPos.getX() + d);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() + d);
            return nextPos;
        }
        if (x > z) {
            if (-1 * x >= z) {
                nextPos.setX(nextPos.getX() - d);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() - d);
            return nextPos;
        }
        if (x <= 0) {
            nextPos.setZ(nextPos.getZ() + d);
            return nextPos;
        }
        nextPos.setZ(nextPos.getZ() - d);
        return nextPos;
    }
}
