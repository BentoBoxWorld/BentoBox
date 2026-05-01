package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Handles respawning back on island
 * 
 * @author tastybento
 *
 */
public class IslandRespawnListener extends FlagListener {

    private final Map<UUID, UUID> respawn = new HashMap<>();

    /**
     * Tag players who die in island space and have an island
     * 
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent e) {
        World world = Util.getWorld(e.getEntity().getWorld());
        if (world == null || !getIWM().inWorld(world)) {
            return; // not in the island world
        }
        if (!Flags.ISLAND_RESPAWN.isSetForWorld(world)) {
            return; // world doesn't have the island respawn flag
        }
        if (!getIslands().hasIsland(world, e.getEntity().getUniqueId())
                && !getIslands().inTeam(world, e.getEntity().getUniqueId())) {
            return; // doesn't have an island in this world
        }
        respawn.put(e.getEntity().getUniqueId(), world.getUID());
    }

    /**
     * Place players back on their island if respawn on island is true and active
     * 
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        final UUID worldUUID = respawn.remove(e.getPlayer().getUniqueId());
        if (worldUUID == null) {
            return; // no respawn world set
        }

        final World world = Bukkit.getWorld(worldUUID);
        if (world == null) {
            return; // world no longer available
        }
        World w = Util.getWorld(world);
        String ownerName = e.getPlayer().getName();
        if (w != null) {
            Location respawnLocation = getIslands().getHomeLocation(world, e.getPlayer().getUniqueId());
            if (respawnLocation != null && !getIslands().isSafeLocation(respawnLocation)) {
                // Home location is not safe (e.g. the block was removed).
                // Try one block above first (covers slabs, stairs, etc.)
                Location lPlusOne = respawnLocation.clone().add(new Vector(0, 1, 0));
                if (getIslands().isSafeLocation(lPlusOne)) {
                    respawnLocation = lPlusOne;
                } else {
                    // Fall back to a safe spot anywhere on the island
                    respawnLocation = getSafeIslandLocation(world, e.getPlayer().getUniqueId());
                }
            }
            if (respawnLocation != null && getIslands().isSafeLocation(respawnLocation)) {
                e.setRespawnLocation(respawnLocation);
                // Get the island owner name
                Island island = BentoBox.getInstance().getIslands().getIsland(w, User.getInstance(e.getPlayer()));
                if (island != null) {
                    ownerName = BentoBox.getInstance().getPlayers().getName(island.getOwner());
                }
            }
        }
        // Run respawn commands, if any
        Util.runCommands(User.getInstance(e.getPlayer()), ownerName, getIWM().getOnRespawnCommands(world), "respawn");
    }

    /**
     * Tries to find a safe respawn location on the player's island by scanning from
     * the island center upward. Used as a fallback when the player's home location
     * is not safe (e.g. the home block was removed).
     *
     * @param world - the island world
     * @param uuid  - the player's UUID
     * @return a safe location on the island, or {@code null} if none can be found
     */
    @Nullable
    private Location getSafeIslandLocation(@NonNull World world, @NonNull UUID uuid) {
        Location islandLoc = getIslands().getIslandLocation(world, uuid);
        if (islandLoc == null) {
            return null;
        }
        // Try a default offset from the island center (same offsets used by getAsyncSafeHomeLocation)
        Location dl = islandLoc.clone().add(new Vector(0.5D, 5D, 2.5D));
        if (getIslands().isSafeLocation(dl)) {
            return dl;
        }
        // Try directly above the island center at a safe height
        dl = islandLoc.clone().add(new Vector(0.5D, 5D, 0.5D));
        if (getIslands().isSafeLocation(dl)) {
            return dl;
        }
        // Scan upward from the island center
        for (int y = islandLoc.getBlockY(); y < world.getMaxHeight(); y++) {
            dl = new Location(world, islandLoc.getX() + 0.5D, y, islandLoc.getZ() + 0.5D);
            if (getIslands().isSafeLocation(dl)) {
                return dl;
            }
        }
        return null;
    }

}
