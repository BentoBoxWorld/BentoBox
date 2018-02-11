package us.tastybento.bskyblock.listeners;

import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
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

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.util.teleport.SafeTeleportBuilder;

public class NetherPortals implements Listener {
    private static final String ERROR_NO_PERMISSION = "errors.general.no-permission";
    private final BSkyBlock plugin;
    private World world;
    private World nether;
    private World theEnd;

    public NetherPortals(BSkyBlock plugin) {
        this.plugin = plugin;
        world = plugin.getIslandWorldManager().getIslandWorld();
        nether = plugin.getIslandWorldManager().getNetherWorld();
        theEnd = plugin.getIslandWorldManager().getEndWorld();
    }

    /**
     * Function to check proximity to nether or end spawn location.
     * Used when playing with the standard nether or end.
     * 
     * @param location
     * @return true if in the spawn area, false if not
     */
    private boolean awayFromSpawn(Location location) {
        Vector p = location.toVector().multiply(new Vector(1, 0, 1));
        Vector spawn = location.getWorld().getSpawnLocation().toVector().multiply(new Vector(1, 0, 1));
        return (spawn.distanceSquared(p) < (plugin.getSettings().getNetherSpawnRadius() * plugin.getSettings().getNetherSpawnRadius())) ? false : true;
    } 

    /**
     * Check if the player is in the island worlds
     * @param location
     * @return true if the player is
     */
    private boolean inWorlds(Location location) {
        return (location.getWorld().equals(world) || location.getWorld().equals(nether) || location.getWorld().equals(theEnd)) ? true : false;
    }

    /**
     * If the player is in the standard nether or standard end or op, do nothing.
     * Used to protect the standard spawn for nether or end
     * @param player
     * @return true if nothing needs to be done
     */
    private boolean noAction(Player player) {
        return (player.isOp()
                || (!player.getWorld().equals(nether) && !player.getWorld().equals(theEnd)) 
                || (player.getWorld().equals(nether) && plugin.getSettings().isNetherIslands()) 
                || (player.getWorld().equals(theEnd) && plugin.getSettings().isEndIslands())) ? true: false;
    }

    /**
     * Prevents blocks from being broken
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (noAction(event.getPlayer())) {
            return;
        }
        if (!awayFromSpawn(event.getBlock().getLocation())) {
            User.getInstance(event.getPlayer()).sendMessage(ERROR_NO_PERMISSION);
            event.setCancelled(true);
        }
    }

    /**
     * Protects standard nether or end spawn from bucket abuse
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (noAction(event.getPlayer())) {
            return;
        }
        if (!awayFromSpawn(event.getBlockClicked().getLocation())) {
            User.getInstance(event.getPlayer()).sendMessage(ERROR_NO_PERMISSION);
            event.setCancelled(true);
        }
    }

    /**
     * Handle end portals
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndIslandPortal(PlayerPortalEvent event) {
        if (!event.getCause().equals(TeleportCause.END_PORTAL) || !plugin.getSettings().isEndGenerate()) {
            return;
        }
        if (!inWorlds(event.getFrom())) {
            return;
        }
        // If entering a portal in the end, teleport home if you have one, else do nothing
        if (event.getFrom().getWorld().equals(theEnd)) { 
            if (plugin.getIslands().hasIsland(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                plugin.getIslands().homeTeleport(event.getPlayer());
            } 
            return;
        }
        // If this is island end, then go to the same location, otherwise try spawn
        Location to = plugin.getSettings().isEndIslands() ? event.getFrom().toVector().toLocation(theEnd) : theEnd.getSpawnLocation();
        // Else other worlds teleport to the end
        event.setCancelled(true);
        new SafeTeleportBuilder(plugin)
        .entity(event.getPlayer())
        .location(to)
        .build();
        return;
    }

    /**
     * This handles non-player portal use
     * Currently disables portal use by entities
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (inWorlds(event.getFrom())) {
            // Disable entity portal transfer due to dupe glitching
            event.setCancelled(true);
        }
    }

    /**
     * Prevent standard nether or end spawns from being blown up
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!inWorlds(event.getLocation())) {
            return;
        }
        if ((event.getLocation().getWorld().equals(nether) && plugin.getSettings().isNetherIslands())
                || (event.getLocation().getWorld().equals(theEnd) && plugin.getSettings().isEndIslands())) {
            // Not used in island worlds
            return;
        }
        // Find out what is exploding
        Entity expl = event.getEntity();
        if (expl == null) {
            return;
        }
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            if (!awayFromSpawn(b.getLocation())) {
                it.remove();
            }
        }
    }

    /**
     * Handle nether portals
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onNetherPortal(PlayerPortalEvent event) {
        if (!event.getCause().equals(TeleportCause.NETHER_PORTAL)) {
            return;
        }
        if (!inWorlds(event.getFrom())) {
            return;
        }
        // If entering a portal in the nether, teleport to portal in overworld if there is one
        if (event.getFrom().getWorld().equals(nether)) {
            // If this is island nether, then go to the same vector, otherwise try spawn
            Location to = plugin.getSettings().isNetherIslands() ? event.getFrom().toVector().toLocation(world) : plugin.getIslands().getIslandLocation(event.getPlayer().getUniqueId());
            event.setCancelled(true);
            // Else other worlds teleport to the nether
            new SafeTeleportBuilder(plugin)
            .entity(event.getPlayer())
            .location(to)
            .portal()
            .build();
            return;
        }
        // If this is island nether, then go to the same vector, otherwise try spawn
        Location to = plugin.getSettings().isNetherIslands() ? event.getFrom().toVector().toLocation(nether) : nether.getSpawnLocation();
        event.setCancelled(true);
        // Else other worlds teleport to the nether
        new SafeTeleportBuilder(plugin)
        .entity(event.getPlayer())
        .location(to)
        .portal()
        .build();
        return;
    }

    /**
     * Prevents placing of blocks at standard nether or end spawns
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBlockPlace(BlockPlaceEvent event) {
        if (noAction(event.getPlayer())) {
            return;
        }
        if (!awayFromSpawn(event.getBlock().getLocation())) {
            User.getInstance(event.getPlayer()).sendMessage(ERROR_NO_PERMISSION);
            event.setCancelled(true);
        }
    }

    /**
     * Converts trees to gravel and glowstone
     * 
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent event) {
        if (!plugin.getSettings().isNetherTrees() || !event.getWorld().equals(nether)) {
            return;
        }
        for (BlockState b : event.getBlocks()) {
            if (b.getType() == Material.LOG || b.getType() == Material.LOG_2) {
                b.setType(Material.GRAVEL);
            } else if (b.getType() == Material.LEAVES || b.getType() == Material.LEAVES_2) {
                b.setType(Material.GLOWSTONE);
            }
        }
    }
}