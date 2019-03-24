package world.bentobox.bentobox.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;

/**
 * Handles teleportation via the Nether/End portals to the Nether and End dimensions of the worlds added by the GameModeAddons.
 *
 * @author tastybento
 */
public class PortalTeleportationListener implements Listener {

    private final BentoBox plugin;

    public PortalTeleportationListener(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles non-player portal use.
     * Currently disables portal use by entities to prevent dupe glitching.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent e) {
        if (plugin.getIWM().inWorld(e.getFrom())) {
            // Disable entity portal transfer due to dupe glitching
            e.setCancelled(true);
        }
    }

    /**
     * Handles end portals
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onEndIslandPortal(PlayerPortalEvent e) {
        if (e.getCause() != TeleportCause.END_PORTAL) {
            return false;
        }
        World fromWorld = e.getFrom().getWorld();
        if (fromWorld == null || !plugin.getIWM().inWorld(fromWorld) || !plugin.getIWM().isEndGenerate(fromWorld)) {
            // Do nothing special
            return false;
        }

        // STANDARD END
        if (!plugin.getIWM().isEndIslands(fromWorld)) {
            if (fromWorld.getEnvironment() != Environment.THE_END) {
                // To Standard end
                e.setTo(plugin.getIWM().getEndWorld(fromWorld).getSpawnLocation());
            }
            // From standard end - check if player has an island to go to
            else if (plugin.getIslands().hasIsland(Util.getWorld(fromWorld), e.getPlayer().getUniqueId())
                    || plugin.getIslands().inTeam(Util.getWorld(fromWorld), e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                plugin.getIslands().homeTeleport(Util.getWorld(fromWorld), e.getPlayer());
            }
            // No island, so just do nothing
            return false;
        }

        // FROM END
        World overWorld = Util.getWorld(fromWorld);

        // If entering an ender portal in the End.
        if (fromWorld.getEnvironment() == Environment.THE_END) {
            // If this is from the island nether, then go to the same vector, otherwise try island home location
            Location to = plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.NORMAL)).orElse(e.getFrom().toVector().toLocation(overWorld));
            e.setCancelled(true);
            // Else other worlds teleport to the overworld
            new SafeSpotTeleport.Builder(plugin)
            .entity(e.getPlayer())
            .location(to)
            .portal()
            .build();
            return true;
        }
        // TO END
        World endWorld = plugin.getIWM().getEndWorld(overWorld);
        // If this is to island End, then go to the same vector, otherwise try spawn
        Location to = plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.THE_END)).orElse(e.getFrom().toVector().toLocation(endWorld));
        e.setCancelled(true);
        // Else other worlds teleport to the nether
        new SafeSpotTeleport.Builder(plugin)
        .entity(e.getPlayer())
        .location(to)
        .portal()
        .build();
        return true;
    }

    /**
     * Handles nether portals
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onNetherPortal(PlayerPortalEvent e) {
        if (e.getCause() != TeleportCause.NETHER_PORTAL) {
            return false;
        }
        World fromWorld = e.getFrom().getWorld();
        if (fromWorld == null || !plugin.getIWM().inWorld(fromWorld) || !plugin.getIWM().isNetherGenerate(fromWorld)) {
            // Do nothing special
            return false;
        }

        // STANDARD NETHER
        if (!plugin.getIWM().isNetherIslands(fromWorld)) {
            if (fromWorld.getEnvironment() != Environment.NETHER) {
                // To Standard Nether
                e.setTo(plugin.getIWM().getNetherWorld(fromWorld).getSpawnLocation());
                e.useTravelAgent(true);
            }
            // From standard nether
            else {
                e.setCancelled(true);
                plugin.getIslands().homeTeleport(Util.getWorld(fromWorld), e.getPlayer());
            }
            return false;
        }

        // FROM NETHER
        World overWorld = Util.getWorld(fromWorld);
        // If entering a nether portal in the nether, teleport to portal in overworld if there is one
        if (fromWorld.getEnvironment() == Environment.NETHER) {
            // If this is from the island nether, then go to the same vector, otherwise try island home location
            Location to = plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.NORMAL)).orElse(e.getFrom().toVector().toLocation(overWorld));
            e.setCancelled(true);
            // Else other worlds teleport to the nether
            new SafeSpotTeleport.Builder(plugin)
            .entity(e.getPlayer())
            .location(to)
            .portal()
            .build();
            return true;
        }
        // TO NETHER
        World nether = plugin.getIWM().getNetherWorld(overWorld);
        // If this is to island nether, then go to the same vector, otherwise try spawn
        Location to = plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.NETHER)).orElse(e.getFrom().toVector().toLocation(nether));
        e.setCancelled(true);
        // Else other worlds teleport to the nether
        new SafeSpotTeleport.Builder(plugin)
        .entity(e.getPlayer())
        .location(to)
        .portal()
        .build();
        return true;
    }
}
