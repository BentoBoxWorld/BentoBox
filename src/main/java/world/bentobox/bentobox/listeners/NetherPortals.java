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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeTeleportBuilder;

public class NetherPortals implements Listener {
    private static final String ERROR_NO_PERMISSION = "general.errors.no-permission";
    private final BentoBox plugin;

    public NetherPortals(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Function to check proximity to nether or end spawn location.
     * Used when playing with the standard nether or end.
     *
     * @param location - the location
     * @return true if in the spawn area, false if not
     */
    private boolean atSpawn(Location location) {
        Vector p = location.toVector().multiply(new Vector(1, 0, 1));
        Vector spawn = location.getWorld().getSpawnLocation().toVector().multiply(new Vector(1, 0, 1));
        int radiusSquared = plugin.getIWM().getNetherSpawnRadius(location.getWorld()) ^ 2;
        return (spawn.distanceSquared(p) < radiusSquared);
    }

    /**
     * If the player is not in the standard nether or standard end or op, do nothing.
     * Used to protect the standard spawn for nether or end
     * @param player - the player
     * @return true if nothing needs to be done
     */
    private boolean noAction(Player player) {
        if (player.isOp()
                || player.getWorld().getEnvironment().equals(Environment.NORMAL)
                || !plugin.getIWM().inWorld(player.getLocation())) {
            return true;
        }
        // Player is in an island world and in a nether or end
        return (player.getWorld().getEnvironment().equals(Environment.NETHER) && plugin.getIWM().isNetherIslands(player.getWorld()))
                || (player.getWorld().getEnvironment().equals(Environment.THE_END) && plugin.getIWM().isEndIslands(player.getWorld()));
    }

    /**
     * Prevents blocks from being broken
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (noAction(e.getPlayer())) {
            return;
        }
        if (atSpawn(e.getBlock().getLocation())) {
            User.getInstance(e.getPlayer()).sendMessage(ERROR_NO_PERMISSION);
            e.setCancelled(true);
        }
    }

    /**
     * Protects standard nether or end spawn from bucket abuse
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (noAction(e.getPlayer())) {
            return;
        }
        if (atSpawn(e.getBlockClicked().getLocation())) {
            User.getInstance(e.getPlayer()).sendMessage(ERROR_NO_PERMISSION);
            e.setCancelled(true);
        }
    }

    /**
     * Handle end portals
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
            new SafeTeleportBuilder(plugin)
            .entity(e.getPlayer())
            .location(to)
            .build();
        }
    }

    /**
     * This handles non-player portal use
     * Currently disables portal use by entities
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
     * Prevent standard nether or end spawns from being blown up
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onExplosion(EntityExplodeEvent e) {
        if (!plugin.getIWM().inWorld(e.getLocation())
                || plugin.getIWM().isIslandNether(e.getLocation().getWorld())
                || plugin.getIWM().isIslandEnd(e.getLocation().getWorld())) {
            // Not used in island worlds
            return false;
        }
        // Find out what is exploding
        Entity expl = e.getEntity();
        if (expl == null) {
            return false;
        }
        e.blockList().removeIf(b -> atSpawn(b.getLocation()));
        return true;
    }

    /**
     * Handle nether portals
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onNetherPortal(PlayerPortalEvent e) {
        if (!e.getCause().equals(TeleportCause.NETHER_PORTAL) || !plugin.getIWM().inWorld(e.getFrom())) {
            return false;
        }
        // Get the overworld, which may be the same world
        World overWorld = Util.getWorld(e.getFrom().getWorld());
        // If entering a portal in the nether, teleport to portal in overworld if there is one
        if (e.getFrom().getWorld().getEnvironment().equals(Environment.NETHER)) {
            // If this is from the island nether, then go to the same vector, otherwise try island home location
            Location to = plugin.getIWM().isNetherIslands(overWorld)
                    ? plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.NORMAL)).orElse(e.getFrom().toVector().toLocation(overWorld))
                            : plugin.getIslands().getIslandLocation(overWorld, e.getPlayer().getUniqueId());

                    e.setCancelled(true);
                    // Else other worlds teleport to the nether
                    new SafeTeleportBuilder(plugin)
                    .entity(e.getPlayer())
                    .location(to)
                    .portal()
                    .build();
                    return true;
        }
        World nether = plugin.getIWM().getNetherWorld(overWorld);
        // If this is to island nether, then go to the same vector, otherwise try spawn
        Location to = (plugin.getIWM().isNetherIslands(overWorld) && plugin.getIWM().isNetherGenerate(overWorld))
                ? plugin.getIslands().getIslandAt(e.getFrom()).map(i -> i.getSpawnPoint(Environment.NETHER)).orElse(e.getFrom().toVector().toLocation(nether))
                        : nether.getSpawnLocation();
                e.setCancelled(true);
                // Else other worlds teleport to the nether
                new SafeTeleportBuilder(plugin)
                .entity(e.getPlayer())
                .location(to)
                .portal()
                .build();
                return true;
    }

    /**
     * Prevents placing of blocks at standard nether or end spawns
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBlockPlace(BlockPlaceEvent e) {
        if (noAction(e.getPlayer())) {
            return;
        }
        if (atSpawn(e.getBlock().getLocation())) {
            User.getInstance(e.getPlayer()).sendMessage(ERROR_NO_PERMISSION);
            e.setCancelled(true);
        }
    }

    /**
     * Converts trees to gravel and glowstone
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public boolean onTreeGrow(StructureGrowEvent e) {
        if (!plugin.getIWM().isNetherTrees(e.getWorld()) || !e.getWorld().getEnvironment().equals(Environment.NETHER)) {
            return false;
        }
        for (BlockState b : e.getBlocks()) {
            if (Tag.LOGS.isTagged(b.getType())) {
                b.setType(Material.GRAVEL);
            } else if (Tag.LEAVES.isTagged(b.getType())) {
                b.setType(Material.GLOWSTONE);
            }
        }
        return true;
    }
}