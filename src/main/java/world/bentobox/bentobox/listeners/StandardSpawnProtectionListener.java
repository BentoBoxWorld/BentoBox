package world.bentobox.bentobox.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * Handles protection of the standard Nether and/or End spawns.
 *
 * @author tastybento
 */
public class StandardSpawnProtectionListener implements Listener {

    private static final String SPAWN_PROTECTED = "protection.spawn-protected";

    private final BentoBox plugin;

    public StandardSpawnProtectionListener(@NonNull BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevents placing blocks at standard nether or end spawns.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (noAction(e.getPlayer())) {
            return;
        }
        if (atSpawn(e.getBlock().getLocation())) {
            User user = User.getInstance(e.getPlayer());
            user.sendMessage(SPAWN_PROTECTED, TextVariables.DESCRIPTION, user.getTranslation(Flags.PLACE_BLOCKS.getHintReference()));
            e.setCancelled(true);
        }
    }

    /**
     * Prevents blocks from being broken.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (noAction(e.getPlayer())) {
            return;
        }
        if (atSpawn(e.getBlock().getLocation())) {
            User user = User.getInstance(e.getPlayer());
            user.sendMessage(SPAWN_PROTECTED, TextVariables.DESCRIPTION, user.getTranslation(Flags.BREAK_BLOCKS.getHintReference()));
            e.setCancelled(true);
        }
    }

    /**
     * Prevent standard nether or end spawns from being blown up.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent e) {
        if (!plugin.getIWM().inWorld(Util.getWorld(e.getLocation().getWorld()))
                || plugin.getIWM().isIslandNether(e.getLocation().getWorld())
                || plugin.getIWM().isIslandEnd(e.getLocation().getWorld())) {
            // Not used in island worlds
            return;
        }
        e.blockList().removeIf(b -> atSpawn(b.getLocation()));
    }

    /**
     * Protects standard nether or end spawn from bucket abuse.
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (noAction(e.getPlayer())) {
            return;
        }
        if (atSpawn(e.getBlockClicked().getLocation())) {
            User user = User.getInstance(e.getPlayer());
            user.sendMessage(SPAWN_PROTECTED, TextVariables.DESCRIPTION, user.getTranslation(Flags.BUCKET.getHintReference()));
            e.setCancelled(true);
        }
    }

    /**
     * Check proximity to nether or end spawn location.
     * Used when playing with the standard nether or end.
     *
     * @param location - the location
     * @return true if in the spawn area, false if not
     */
    private boolean atSpawn(@NonNull Location location) {
        if (plugin.getIWM().getWorldSettings(location.getWorld()).isMakeNetherPortals()) {
            // If nether portals are active, there is no common spawn
            return false;
        }
        Vector p = location.toVector().multiply(new Vector(1, 0, 1));
        Vector spawn = location.getWorld().getSpawnLocation().toVector().multiply(new Vector(1, 0, 1));
        int radius = plugin.getIWM().getNetherSpawnRadius(location.getWorld());
        Vector diff = p.subtract(spawn);
        return Math.abs(diff.getBlockX()) <= radius && Math.abs(diff.getBlockZ()) <= radius;
    }

    /**
     * If the player is not in the standard nether or standard end or op, do nothing.
     * If portal making is true, then do not protect spawn.
     * Used to protect the standard spawn for nether or end.
     *
     * @param player - the player
     * @return true if nothing needs to be done
     */
    private boolean noAction(@NonNull Player player) {
        return (player.isOp() || player.getWorld().getEnvironment().equals(World.Environment.NORMAL)
                || !plugin.getIWM().inWorld(Util.getWorld(player.getWorld()))
                || (player.getWorld().getEnvironment().equals(World.Environment.NETHER) && plugin.getIWM().isNetherIslands(player.getWorld()))
                || (player.getWorld().getEnvironment().equals(World.Environment.NETHER) && plugin.getIWM().getWorldSettings(player.getWorld()).isMakeNetherPortals())
                || (player.getWorld().getEnvironment().equals(World.Environment.THE_END) && plugin.getIWM().isEndIslands(player.getWorld())));

    }
}
