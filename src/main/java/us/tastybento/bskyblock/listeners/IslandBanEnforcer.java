/**
 * 
 */
package us.tastybento.bskyblock.listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.managers.IslandsManager;

/**
 * Enforces island bans. Checks for teleporting, entry via flying and due to logging in
 * @author tastybento
 *
 */

public class IslandBanEnforcer implements Listener {

    private IslandsManager im;
    private Set<UUID> inTeleport;

    /**
     * Enforces island bans
     * @param plugin
     */
    public IslandBanEnforcer(BSkyBlock plugin) {
        this.im = plugin.getIslands();
        inTeleport = new HashSet<>();
    }

    // Teleport check
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        // Ignore players who are being ejected
        if (inTeleport.contains(e.getPlayer().getUniqueId())) {
            // Remove them
            inTeleport.remove(e.getPlayer().getUniqueId());
            return;
        }
        e.setCancelled(checkAndNotify(e.getPlayer(), e.getTo()));
        // Check from - just in case the player is inside the island
        if (check(e.getPlayer(), e.getFrom())) {
            eject(e.getPlayer());
        }
    }

    // Movement check
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        // Ignore only vertical movement
        if (e.getFrom().getBlockX() - e.getTo().getBlockX() == 0 && e.getFrom().getBlockZ() - e.getTo().getBlockZ() == 0) {
            return;
        }
        e.setCancelled(checkAndNotify(e.getPlayer(), e.getTo()));
        // Check from - just in case the player is inside the island
        if (check(e.getPlayer(), e.getFrom())) {
            eject(e.getPlayer());
        }
    }

    // Vehicle move check
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent e) {
        // Ignore only vertical movement
        if (e.getFrom().getBlockX() - e.getTo().getBlockX() == 0 && e.getFrom().getBlockZ() - e.getTo().getBlockZ() == 0) {
            return;
        }
        // For each Player in the vehicle
        e.getVehicle().getPassengers().stream().filter(en -> en instanceof Player).map(en -> (Player)en).forEach(p -> {
            if (checkAndNotify(p, e.getTo())) {
                p.leaveVehicle();
                p.teleport(e.getFrom());
                eject(p);
            }
        });
    }  

    // Login check
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent e) {
        if (checkAndNotify(e.getPlayer(), e.getPlayer().getLocation())) {
            eject(e.getPlayer());
        }
    }

    /**
     * Check if a player is banned from this location
     * @param player - player
     * @param loc - location to check
     * @return true if banned
     */
    private boolean check(Player player, Location loc) {
        // See if player is banned
        return im.getProtectedIslandAt(loc).map(is -> is.isBanned(player.getUniqueId())).orElse(false);
    }
    
    /**
     * Checks if a player is banned from this location and notifies them if so
     * @param player - player
     * @param loc - location to check
     * @return true if banned
     */
    private boolean checkAndNotify(Player player, Location loc) {
        if (check(player, loc)) {
            User.getInstance(player).notify("commands.island.ban.you-are-banned");
            return true;
        }
        return false;
    }

    /**
     * Sends player home
     * @param player
     */
    private void eject(Player player) {
        // Teleport player to their home
        inTeleport.add(player.getUniqueId());
        if (im.hasIsland(player.getUniqueId())) {
            im.homeTeleport(player);
        } // else, TODO: teleport somewhere else?   
    }

}
