package world.bentobox.bentobox.listeners;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.util.Vector;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
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
    public void onEndIslandPortal(PlayerPortalEvent e) {
        if (!e.getCause().equals(TeleportCause.END_PORTAL) || !plugin.getIWM().inWorld(e.getFrom())) {
            return;
        }
        World overWorld = Util.getWorld(e.getFrom().getWorld());

        // If entering a portal in the end, teleport home if you have one, else do nothing
        if (e.getFrom().getWorld().getEnvironment().equals(Environment.THE_END)) {
            if (plugin.getIslands().hasIsland(overWorld, e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                plugin.getIslands().homeTeleport(overWorld, e.getPlayer());
            }
            return;
        }
        // Going to the end, then go to the same location in the end world
        if (plugin.getIWM().isEndGenerate(overWorld) && plugin.getIWM().isEndIslands(overWorld)) {
            World endWorld = plugin.getIWM().getEndWorld(overWorld);
            // End exists and end islands are being used
            Location to = plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.THE_END)).orElse(e.getFrom().toVector().toLocation(endWorld));
            e.setCancelled(true);
            new SafeSpotTeleport.Builder(plugin)
            .entity(e.getPlayer())
            .location(to)
            .portal()
            .build();
        }
    }

    /**
     * Handles nether portals
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onNetherPortal(PlayerPortalEvent e) {
        if (e.getFrom() == null) {
            return false;
        }
        World fromWorld = e.getFrom().getWorld();
        if (e.getCause() != TeleportCause.NETHER_PORTAL || !plugin.getIWM().isNetherGenerate(fromWorld)) {
            // Do nothing special
            return false;
        }

        // STANDARD NETHER
        if (plugin.getIWM().isNetherGenerate(fromWorld) && !plugin.getIWM().isNetherIslands(fromWorld)) {
            if (fromWorld.getEnvironment() == Environment.NORMAL) {
                // To Standard Nether
                e.setTo(plugin.getIWM().getNetherWorld(fromWorld).getSpawnLocation());
                e.useTravelAgent(true);
            }
            // From standard nether
            else if (fromWorld.getEnvironment() == Environment.NETHER) {
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