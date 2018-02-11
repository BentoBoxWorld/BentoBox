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
import us.tastybento.bskyblock.util.SafeTeleportBuilder;

public class NetherPortals implements Listener {
    private final BSkyBlock plugin;
    private World world;
    private World nether;
    private World the_end;

    public NetherPortals(BSkyBlock plugin) {
        this.plugin = plugin;
        world = plugin.getIslandWorldManager().getIslandWorld();
        nether = plugin.getIslandWorldManager().getNetherWorld();
        the_end = plugin.getIslandWorldManager().getEndWorld();
    }

    /**
     * Function to check proximity to nether spawn location
     * 
     * @param location
     * @return true if in the spawn area, false if not
     */
    private boolean awayFromSpawn(Location location) {
        Vector p = location.toVector().multiply(new Vector(1, 0, 1));
        Vector spawn = location.getWorld().getSpawnLocation().toVector().multiply(new Vector(1, 0, 1));
        if (spawn.distanceSquared(p) < (plugin.getSettings().getNetherSpawnRadius() * plugin.getSettings().getNetherSpawnRadius())) {
            plugin.getLogger().info("not away from spawn");
            return false;
        } else {
            return true;
        }
    } 

    private boolean inWorlds(Location from) {
        plugin.getLogger().info("In world = " + (from.getWorld().equals(world) || from.getWorld().equals(nether) || from.getWorld().equals(the_end)));
        return (from.getWorld().equals(world) || from.getWorld().equals(nether) || from.getWorld().equals(the_end)) ? true : false;
    }

    private boolean noAction(Player player) {
        if (player.isOp()
                || (!player.getWorld().equals(nether) && !player.getWorld().equals(the_end)) 
                || (player.getWorld().equals(nether) && plugin.getSettings().isNetherIslands()) 
                || (player.getWorld().equals(the_end) && plugin.getSettings().isEndIslands())) {
            plugin.getLogger().info("No legacy nether or end");
            return true;
        }
        plugin.getLogger().info("Action!");
        return false;
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
            User.getInstance(event.getPlayer()).sendMessage("errors.general.no-permission");
            event.setCancelled(true);
        }
    }

    // Nether portal spawn protection

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (noAction(event.getPlayer())) {
            return;
        }
        if (!awayFromSpawn(event.getBlockClicked().getLocation())) {
            User.getInstance(event.getPlayer()).sendMessage("errors.general.no-permission");
            event.setCancelled(true);
        }
    }

    /**
     * Handle end portals
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndIslandPortal(PlayerPortalEvent event) {
        plugin.getLogger().info("End portal event Is end generated? " + plugin.getSettings().isEndGenerate());
        if (!event.getCause().equals(TeleportCause.END_PORTAL) || !plugin.getSettings().isEndGenerate()) {
            return;
        }
        if (!inWorlds(event.getFrom())) {
            return;
        }
        // If entering a portal in the end, teleport home if you have one, else do nothing
        if (event.getFrom().getWorld().equals(the_end)) { 
            plugin.getLogger().info("In end world");
            if (plugin.getIslands().hasIsland(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                plugin.getIslands().homeTeleport(event.getPlayer());
            } 
            return;
        }
        plugin.getLogger().info("In other world going through end portal");
        // If this is island end, then go to the same location, otherwise try spawn
        Location to = plugin.getSettings().isEndIslands() ? event.getFrom().toVector().toLocation(the_end) : the_end.getSpawnLocation();
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
        plugin.getLogger().info(event.getEventName());
        if (inWorlds(event.getFrom())) {
            // Disable entity portal transfer due to dupe glitching
            event.setCancelled(true);
        }
    }

    /**
     * Prevent spawns from being blown up
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (!inWorlds(event.getLocation())) {
            return;
        }
        if ((event.getLocation().getWorld().equals(nether) && plugin.getSettings().isNetherIslands())
                || (event.getLocation().getWorld().equals(the_end) && plugin.getSettings().isEndIslands())) {
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
        plugin.getLogger().info(event.getEventName() + " " + event.getCause() + " nether");
        if (!event.getCause().equals(TeleportCause.NETHER_PORTAL)) {
            return;
        }
        if (!inWorlds(event.getFrom())) {
            return;
        }
        // If entering a portal in the nether or end, teleport home if you have one, else do nothing
        if (!event.getFrom().getWorld().equals(world)) {  
            plugin.getLogger().info("Entered portal in nether or end");
            if (plugin.getIslands().hasIsland(event.getPlayer().getUniqueId())) {
                plugin.getLogger().info("player has island - teleporting home");
                event.setCancelled(true);
                plugin.getIslands().homeTeleport(event.getPlayer());
            } 
            return;
        }
        plugin.getLogger().info("Entering nether portal in overworld");
        // If this is island nether, then go to the same vector, otherwise try spawn
        Location to = plugin.getSettings().isNetherIslands() ? event.getFrom().toVector().toLocation(nether) : nether.getSpawnLocation();
        plugin.getLogger().info("Going to " + to);
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
     * Prevents placing of blocks
     * 
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerBlockPlace(BlockPlaceEvent event) {
        if (noAction(event.getPlayer())) {
            return;
        }
        if (!awayFromSpawn(event.getBlock().getLocation())) {
            User.getInstance(event.getPlayer()).sendMessage("errors.general.no-permission");
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