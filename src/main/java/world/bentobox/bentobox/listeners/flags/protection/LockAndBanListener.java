package world.bentobox.bentobox.listeners.flags.protection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
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
     * Draws the border of a locked island for players who are turned away from it.
     */
    private final LockedIslandBorder lockedBorder = new LockedIslandBorder();

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
        Optional<Island> island = getIslands().getProtectedIslandAt(e.getTo());
        CheckResult result = checkAndNotify(e.getPlayer(), e.getTo(), island);
        if (!result.isAllowed()) {
            e.setCancelled(true);
            showLockedBorder(e.getPlayer(), result, island, e.getTo());
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
    /**
     * Keeps locked-out and banned players from riding a vehicle onto an island.
     *
     * <p>{@link VehicleMoveEvent} cannot be cancelled, so the equivalent of
     * cancelling a player's move is to put the vehicle back where it came from:
     * <ul>
     *   <li>If anybody aboard may not enter, the whole vehicle is turned back with
     *       everyone still in it, including riders who could have entered on their
     *       own. Nothing is left behind on territory a rider cannot reach, and
     *       nobody is dropped into whatever the vehicle was crossing (#2958).</li>
     *   <li>A rider who is already somewhere they may not be (the island was
     *       locked around them) is sent home, as with walking players.</li>
     * </ul>
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent e) {
        // Ignore only vertical movement
        if (e.getFrom().getBlockX() - e.getTo().getBlockX() == 0 && e.getFrom().getBlockZ() - e.getTo().getBlockZ() == 0) {
            return;
        }
        // For each Player in the vehicle. Resolve the island lazily on the first Player
        // passenger so unmanned/mob-ridden vehicles pay no grid lookup, and reuse it for
        // any further player passengers (the rare multi-player case).
        Optional<Island> island = Optional.empty();
        boolean islandResolved = false;
        List<Player> riders = new ArrayList<>();
        List<Player> denied = new ArrayList<>();
        for (Entity passenger : e.getVehicle().getPassengers()) {
            if (passenger instanceof Player p) {
                if (!islandResolved) {
                    island = getIslands().getProtectedIslandAt(e.getTo());
                    islandResolved = true;
                }
                riders.add(p);
                CheckResult result = checkAndNotify(p, e.getTo(), island);
                if (!result.isAllowed()) {
                    denied.add(p);
                    showLockedBorder(p, result, island, e.getTo());
                }
            }
        }
        if (denied.isEmpty()) {
            return;
        }
        Vehicle vehicle = e.getVehicle();
        vehicle.getWorld().playSound(e.getFrom(), Sound.BLOCK_ANVIL_HIT, 1F, 1F);

        // Riders who are already somewhere they may not be are sent home.
        List<Player> trapped = denied.stream().filter(p -> !check(p, e.getFrom()).isAllowed()).toList();
        if (!trapped.isEmpty()) {
            trapped.forEach(p -> {
                p.leaveVehicle();
                eject(p);
            });
            return;
        }
        // Otherwise the whole vehicle is turned back, whoever else is aboard.
        vehicle.setVelocity(new Vector(0, 0, 0));
        // Riders stay aboard: a teleported vehicle keeps its passengers
        if (vehicle.teleport(e.getFrom())) {
            return;
        }
        // The server refused to move the loaded vehicle. Take the riders off and
        // bring the empty vehicle back out so it can still be recovered.
        riders.forEach(p -> {
            p.leaveVehicle();
            p.teleport(e.getFrom());
        });
        vehicle.teleport(e.getFrom());
    }

    /**
     * Shows the border of the island a player has just been turned away from, if it is locked.
     */
    private void showLockedBorder(Player player, CheckResult result, Optional<Island> island, Location to) {
        if (result == CheckResult.LOCKED && to.getWorld() != null) {
            island.ifPresent(is -> lockedBorder.show(player, is, to.getWorld()));
        }
    }

    // Login check
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerLogin(PlayerJoinEvent e) {
        if (!checkAndNotify(e.getPlayer(), e.getPlayer().getLocation()).isAllowed()) {
            eject(e.getPlayer());
        }
    }

    // Quit cleanup — prevent unbounded growth of notification tracking sets
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        notifiedPlayers.remove(uuid);
        deletableNotified.remove(uuid);
        lockedBorder.forget(uuid);
    }

    /**
     * Check if a player is banned or the island is locked
     * @param player - player
     * @param loc - location to check
     * @return CheckResult LOCKED, BANNED or OPEN. If an island is locked, that will take priority over banned
     */
    private CheckResult check(@NonNull Player player, Location loc)
    {
        return check(player, loc, this.getIslands().getProtectedIslandAt(loc));
    }

    /**
     * Check if a player is banned or the island is locked, using an already-resolved island
     * to avoid repeated island lookups for the same location.
     * @param player - player
     * @param loc - location to check
     * @param island - island at the location, if any
     * @return CheckResult LOCKED, BANNED or OPEN. If an island is locked, that will take priority over banned
     */
    private CheckResult check(@NonNull Player player, Location loc, Optional<Island> island)
    {
        // Ops or NPC's are allowed everywhere
        if (player.isOp() || player.hasMetadata("NPC"))
        {
            return CheckResult.OPEN;
        }

        // See if the island is locked to non-members or player is banned
        return island.
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
        return checkAndNotify(player, loc, this.getIslands().getProtectedIslandAt(loc));
    }

    /**
     * Same as {@link #checkAndNotify(Player, Location)} but reuses an already-resolved island.
     * @param player - player
     * @param loc - location to check
     * @param island - island at the location, if any
     * @return CheckResult
     */
    private CheckResult checkAndNotify(@NonNull Player player, Location loc, Optional<Island> island)
    {
        CheckResult result = this.check(player, loc, island);
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
        notifyIfDeletable(player, island);
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
    private void notifyIfDeletable(@NonNull Player player, Optional<Island> island) {
        if (!player.isOp()) {
            deletableNotified.remove(player.getUniqueId());
            return;
        }
        boolean deletable = island.map(Island::isDeletable).orElse(false);
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
