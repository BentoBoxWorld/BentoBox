package us.tastybento.bskyblock.managers.island;

import java.io.IOException;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.IslandBaseEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
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
    private final Player player;
    private final Reason reason;

    private NewIsland(Island oldIsland, Player player, Reason reason) {
        super();
        plugin = BSkyBlock.getInstance();
        this.player = player;
        this.reason = reason;
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
     * Build a new island for a player using a schematic
     * @author tastybento
     *
     */
    public static class Builder {
        private Island oldIsland;
        private Player player;
        private Reason reason;

        public Builder oldIsland(Island oldIsland) {
            this.oldIsland = oldIsland;
            return this;
        }


        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        public Builder reason(Reason reason) {
            this.reason = reason;
            return this;
        }

        public Island build() throws IOException {
            if (player != null) {
                NewIsland newIsland = new NewIsland(oldIsland, player, reason);
                return newIsland.getIsland();
            }
            throw new IOException("Insufficient parameters. Must have a schematic and a player");
        }
    }

    /**
     * Makes an island.
     */
    public void newIsland() {
        final UUID playerUUID = player.getUniqueId();
        Location next = getNextIsland();
        // Add to the grid
        island = plugin.getIslands().createIsland(next, playerUUID);
        // Save the player so that if the server is reset weird things won't happen

        // Clear any old home locations (they should be clear, but just in case)
        plugin.getPlayers().clearHomeLocations(playerUUID);

        // Set home loction
        plugin.getPlayers().setHomeLocation(playerUUID, next, 1);

        // Fire event
        IslandBaseEvent event = IslandEvent.builder()
                .involvedPlayer(player.getUniqueId())
                .reason(reason)
                .island(island)
                .location(island.getCenter())
                .build();
        if (!event.isCancelled()) {
            // Create island
            new IslandBuilder(plugin, island)
            .setPlayer(player)
            .setChestItems(plugin.getSettings().getChestItems())
            .setType(IslandType.ISLAND)
            .build();
            if (plugin.getSettings().isNetherGenerate() && plugin.getSettings().isNetherIslands() && plugin.getIslandWorldManager().getNetherWorld() != null) {
                new IslandBuilder(plugin,island)
                .setPlayer(player)
                .setChestItems(plugin.getSettings().getChestItems())
                .setType(IslandType.NETHER)
                .build();
            }
            if (plugin.getSettings().isEndGenerate() && plugin.getSettings().isEndIslands() && plugin.getIslandWorldManager().getEndWorld() != null) {
                new IslandBuilder(plugin,island)
                .setPlayer(player)
                .setChestItems(plugin.getSettings().getChestItems())
                .setType(IslandType.END)
                .build();
            }
            // Teleport player to their island
            plugin.getIslands().homeTeleport(player);
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
                    .involvedPlayer(player.getUniqueId())
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
        Location last = plugin.getIslands().getLast();
        if (last == null) {
            last = new Location(plugin.getIslandWorldManager().getIslandWorld(), plugin.getSettings().getIslandXOffset() + plugin.getSettings().getIslandStartX(),
                    plugin.getSettings().getIslandHeight(), plugin.getSettings().getIslandZOffset() + plugin.getSettings().getIslandStartZ());
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
        Location nextPos = lastIsland;
        if (x < z) {
            if (-1 * x < z) {
                nextPos.setX(nextPos.getX() + plugin.getSettings().getIslandDistance()*2);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() + plugin.getSettings().getIslandDistance()*2);
            return nextPos;
        }
        if (x > z) {
            if (-1 * x >= z) {
                nextPos.setX(nextPos.getX() - plugin.getSettings().getIslandDistance()*2);
                return nextPos;
            }
            nextPos.setZ(nextPos.getZ() - plugin.getSettings().getIslandDistance()*2);
            return nextPos;
        }
        if (x <= 0) {
            nextPos.setZ(nextPos.getZ() + plugin.getSettings().getIslandDistance()*2);
            return nextPos;
        }
        nextPos.setZ(nextPos.getZ() - plugin.getSettings().getIslandDistance()*2);
        return nextPos;
    }
}
