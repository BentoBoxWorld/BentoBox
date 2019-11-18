package world.bentobox.bentobox.listeners.flags.protection;

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

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Listener for the lock flag
 * Also handles ban protection
 *
 * @author tastybento
 */
public class LockAndBanListener extends FlagListener {

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
        OPEN
    }

    // Teleport check
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        e.setCancelled(!checkAndNotify(e.getPlayer(), e.getTo()).equals(CheckResult.OPEN));
    }

    // Movement check
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        // Ignore only vertical movement
        if (e.getFrom().getBlockX() - e.getTo().getBlockX() == 0 && e.getFrom().getBlockZ() - e.getTo().getBlockZ() == 0) {
            return;
        }
        if (!checkAndNotify(e.getPlayer(), e.getTo()).equals(CheckResult.OPEN)) {
            e.setCancelled(true);
            e.getPlayer().playSound(e.getFrom(), Sound.BLOCK_ANVIL_HIT, 1F, 1F);
            e.getPlayer().setVelocity(new Vector(0,0,0));
            e.getPlayer().setGliding(false);
        }
        // Check from - just in case the player is inside the island
        if (!check(e.getPlayer(), e.getFrom()).equals(CheckResult.OPEN)) {
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
        e.getVehicle().getPassengers().stream().filter(en -> en instanceof Player).map(en -> (Player)en).forEach(p -> {
            if (!checkAndNotify(p, e.getTo()).equals(CheckResult.OPEN)) {
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
        if (!checkAndNotify(e.getPlayer(), e.getPlayer().getLocation()).equals(CheckResult.OPEN)) {
            eject(e.getPlayer());
        }
    }

    /**
     * Check if a player is banned or the island is locked
     * @param player - player
     * @param loc - location to check
     * @return CheckResult LOCKED, BANNED or OPEN. If an island is locked, that will take priority over banned
     */
    private CheckResult check(Player player, Location loc) {
        // Ops are allowed everywhere
        if (player.isOp()) {
            return CheckResult.OPEN;
        }
        // See if the island is locked to non-members or player is banned
        return getIslands().getProtectedIslandAt(loc)
                .map(is -> {
                    if (is.isBanned(player.getUniqueId())) {
                        return player.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + "mod.bypassban") ? CheckResult.OPEN : CheckResult.BANNED;
                    }
                    if (!is.isAllowed(User.getInstance(player), Flags.LOCK)) {
                        return player.hasPermission(getIWM().getPermissionPrefix(loc.getWorld()) + "mod.bypasslock") ? CheckResult.OPEN : CheckResult.LOCKED;
                    }
                    return CheckResult.OPEN;
                }).orElse(CheckResult.OPEN);
    }

    /**
     * Checks if a player is banned from this location and notifies them if so
     * @param player - player
     * @param loc - location to check
     * @return true if banned
     */
    private CheckResult checkAndNotify(Player player, Location loc) {
        CheckResult r = check(player,loc);
        switch (r) {
        case BANNED:
            User.getInstance(player).notify("commands.island.ban.you-are-banned");
            break;
        case LOCKED:
            User.getInstance(player).notify("protection.locked");
            break;
        default:
            break;
        }
        return r;
    }

    /**
     * Sends player home
     * @param player - player
     */
    private void eject(Player player) {
        // Teleport player to their home
        if (getIslands().hasIsland(player.getWorld(), player.getUniqueId()) || getIslands().inTeam(player.getWorld(), player.getUniqueId())) {
            getIslands().homeTeleport(player.getWorld(), player);
        } else if (getIslands().getSpawn(player.getWorld()).isPresent()) {
            // Else, try to teleport him to the world spawn
            getIslands().spawnTeleport(player.getWorld(), player);
        } else {
            // There's nothing much we can do.
            // We'll try to teleport him to the spawn...
            PaperLib.teleportAsync(player, player.getWorld().getSpawnLocation());

            // Switch him back to the default gamemode. He may die, sorry :(
            player.setGameMode(getIWM().getDefaultGameMode(player.getWorld()));

            // Log
            getPlugin().logWarning("Could not teleport '" + player.getName() + "' back to his island or the spawn.");
            getPlugin().logWarning("Please consider setting a spawn for this world using the admin setspawn command.");
        }
    }
}
