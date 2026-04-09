package world.bentobox.bentobox.listeners.flags.protection;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Listener for the lock flag
 * Also handles ban protection
 *
 * @author tastybento
 */
public class LockAndBanListener extends FlagListener {

    /**
     * Tracks players who have already been notified about a locked or banned island,
     * to avoid spamming the same message on every move event.
     */
    private final Set<UUID> notifiedPlayers = new HashSet<>();

    /**
     * Tracks ops who have already been notified that they are standing on an
     * island flagged for deletion (awaiting region purge), to avoid spamming
     * the notice on every move event. Cleared when the op leaves a deletable
     * island.
     */
    private final Set<UUID> deletableNotified = new HashSet<>();

    /**
     * Result of checking the island for locked state or player bans
     *
     */
    private enum CheckResult {
        /**
         * player is banned from island
         */
        BANNED,
        /**
         * Island is locked
         */
        LOCKED,
        /**
         * Island is open for teleporting
         */
        OPEN,
        /**
         * Island is locked but player has bypass permission
         */
        BYPASS_LOCK;

        /**
         * @return true if the player is allowed to enter the island
         */
        boolean isAllowed() {
            return this != BANNED && this != LOCKED;
        }
    }

    // Teleport check
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        e.setCancelled(!checkAndNotify(e.getPlayer(), e.getTo()).isAllowed());
    }

    // Movement check
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        // Ignore only vertical movement
        if (e.getFrom().getBlockX() - e.getTo().getBlockX() == 0 && e.getFrom().getBlockZ() - e.getTo().getBlockZ() == 0) {
            return;
        }
        if (!checkAndNotify(e.getPlayer(), e.getTo()).isAllowed()) {
            e.setCancelled(true);
            e.getPlayer().playSound(e.getFrom(), Sound.BLOCK_ANVIL_HIT, 1F, 1F);
            e.getPlayer().setVelocity(new Vector(0,0,0));
            e.getPlayer().setGliding(false);
        }
        // Check from - just in case the player is inside the island
        if (!check(e.getPlayer(), e.getFrom()).isAllowed()) {
            // Has to be done 1 tick later otherwise it doesn't happen for some reason...
            Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> eject(e.getPlayer()));
        }
    }

    // Vehicle move check
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent e) {
        // Ignore only vertical movement
        if (e.getFrom().getBlockX() - e.getTo().getBlockX() == 0 && e.getFrom().getBlockZ() - e.getTo().getBlockZ() == 0) {
            return;
        }
        // For each Player in the vehicle
        e.getVehicle().getPassengers().stream().filter(Player.class::isInstance).map(Player.class::cast).forEach(p -> {
            if (!checkAndNotify(p, e.getTo()).isAllowed()) {
                p.leaveVehicle();
                p.teleport(e.getFrom());
                e.getVehicle().getWorld().playSound(e.getFrom(), Sound.BLOCK_ANVIL_HIT, 1F, 1F);
                eject(p);
            }
        });
    }

    // Login check
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent e) {
        if (!checkAndNotify(e.getPlayer(), e.getPlayer().getLocation()).isAllowed()) {
            eject(e.getPlayer());
        }
    }

    /**
     * Check if a player is banned or the island is locked
     * @param player - player
     * @param loc - location to check
     * @return CheckResult LOCKED, BANNED or OPEN. If an island is locked, that will take priority over banned
     */
    private CheckResult check(@NonNull Player player, Location loc)
    {
        // Ops or NPC's are allowed everywhere
        if (player.isOp() || player.hasMetadata("NPC"))
        {
            return CheckResult.OPEN;
        }

        // See if the island is locked to non-members or player is banned
        return this.getIslands().getProtectedIslandAt(loc).
                map(is ->
                {
                    if (is.isBanned(player.getUniqueId()))
                    {
                        return player.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + "mod.bypassban") ?
                                CheckResult.OPEN : CheckResult.BANNED;
                    }
                    if (!is.isAllowed(User.getInstance(player), Flags.LOCK))
                    {
                        return player.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + "mod.bypasslock") ?
                                CheckResult.BYPASS_LOCK : CheckResult.LOCKED;
                    }
                    return CheckResult.OPEN;
                }).
                orElse(CheckResult.OPEN);
    }

    /**
     * Checks if a player is banned from this location and notifies them if so.
     * Notifications are only sent once per entry; subsequent checks for the same
     * player will not repeat the message until the player moves to an open area
     * and re-enters.
     * @param player - player
     * @param loc - location to check
     * @return CheckResult
     */
    private CheckResult checkAndNotify(@NonNull Player player, Location loc)
    {
        CheckResult result = this.check(player, loc);
        if (result == CheckResult.OPEN) {
            // Player is in an open area, clear notification state
            notifiedPlayers.remove(player.getUniqueId());
        } else if (notifiedPlayers.add(player.getUniqueId())) {
            // Player was not previously notified — send the notification
            if (result == CheckResult.BANNED) {
                User.getInstance(player).notify("commands.island.ban.you-are-banned");
            } else if (result == CheckResult.LOCKED) {
                User.getInstance(player).notify("protection.locked");
            } else if (result == CheckResult.BYPASS_LOCK) {
                User.getInstance(player).notify("protection.locked-island-bypass");
            }
        }
        notifyIfDeletable(player, loc);
        return result;
    }

    /**
     * Notify ops that the island they just entered is flagged for deletion
     * and awaiting the region purge. Regular players see nothing — this is
     * an admin-only heads-up so server staff know the visible chunks will
     * be reaped the next time housekeeping runs.
     *
     * <p>Fires at most once per entry, using the same "move out to reset"
     * pattern as the lock notification.
     */
    private void notifyIfDeletable(@NonNull Player player, Location loc) {
        if (!player.isOp()) {
            deletableNotified.remove(player.getUniqueId());
            return;
        }
        boolean deletable = getIslands().getProtectedIslandAt(loc)
                .map(i -> i.isDeletable()).orElse(false);
        if (deletable) {
            if (deletableNotified.add(player.getUniqueId())) {
                User.getInstance(player).notify("protection.deletable-island-admin");
            }
        } else {
            deletableNotified.remove(player.getUniqueId());
        }
    }

    /**
     * Sends player home
     * @param player - player
     */
    private void eject(Player player) {
        // Teleport player to their home
        if (getIslands().hasIsland(player.getWorld(), player.getUniqueId()) || getIslands().inTeam(player.getWorld(), player.getUniqueId())) {
            getIslands().homeTeleportAsync(player.getWorld(), player);
        } else if (getIslands().getSpawn(player.getWorld()).isPresent()) {
            // Else, try to teleport him to the world spawn
            getIslands().spawnTeleport(player.getWorld(), player);
        } else {
            // There's nothing much we can do.
            // We'll try to teleport him to the spawn...
            Location l = player.getWorld().getSpawnLocation();
            Util.teleportAsync(player, l);

            // Switch him back to the default gamemode. He may die, sorry :(
            player.setGameMode(getIWM().getDefaultGameMode(player.getWorld()));

            // Log
            getPlugin().logWarning("Could not teleport '" + player.getName() + "' back to his island or the spawn.");
            getPlugin().logWarning("Please consider setting a spawn for this world using the admin setspawn command.");
        }
    }
}
