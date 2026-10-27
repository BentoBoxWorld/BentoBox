package world.bentobox.bentobox.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.util.NumberConversions;
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
        Block block = e.getBlock();
        if (block == null) {
            return;
        }
        if (atSpawn(block.getLocation())) {
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
        // Use the overworld to look up game settings, as sub-worlds (nether/end) may not be
        // directly registered in the game mode map when using standard (non-island) worlds.
        World gameWorld = Util.getWorld(location.getWorld());
        if (gameWorld == null || !plugin.getIWM().inWorld(gameWorld)) {
            return false;
        }
        World.Environment env = location.getWorld().getEnvironment();
        if (env == World.Environment.NETHER && plugin.getIWM().getWorldSettings(gameWorld).isMakeNetherPortals()) {
            // If nether portals are active, there is no common spawn
            return false;
        }
        if (env == World.Environment.THE_END && plugin.getIWM().getWorldSettings(gameWorld).isMakeEndPortals()) {
            // If end portals are active, there is no common spawn
            return false;
        }
        int radius = env == World.Environment.THE_END
                ? plugin.getIWM().getEndSpawnRadius(gameWorld)
                : plugin.getIWM().getNetherSpawnRadius(gameWorld);
        // Plain coordinate math runs per block event, so it avoid vector allocations.
        // floor() keeps the exact semantics of the previous Vector#getBlockX comparison.
        Location spawn = location.getWorld().getSpawnLocation();
        int diffX = NumberConversions.floor(location.getX() - spawn.getX());
        int diffZ = NumberConversions.floor(location.getZ() - spawn.getZ());
        return Math.abs(diffX) <= radius && Math.abs(diffZ) <= radius;
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
        if (player.isOp() || player.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
            return true;
        }
        // Use the game-mode overworld: getWorldSettings() is keyed by it, and the player's
        // sub-world may no longer exist or be registered.
        World gameWorld = Util.getWorld(player.getWorld());
        if (!plugin.getIWM().inWorld(gameWorld)) {
            return true;
        }
        if (player.getWorld().getEnvironment().equals(World.Environment.NETHER)) {
            return plugin.getIWM().isNetherIslands(gameWorld)
                    || plugin.getIWM().getWorldSettings(gameWorld).isMakeNetherPortals();
        }
        if (player.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
            return plugin.getIWM().isEndIslands(gameWorld)
                    || plugin.getIWM().getWorldSettings(gameWorld).isMakeEndPortals();
        }
        return false;

    }
}
