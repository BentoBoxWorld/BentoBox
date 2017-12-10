package us.tastybento.bskyblock.listeners.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.util.Vector;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.SafeSpotTeleport;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class NetherEvents implements Listener {
    private final BSkyBlock plugin;
    private final static boolean DEBUG = false;

    public NetherEvents(BSkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * This handles non-player portal use
     * Currently disables portal use by entities
     *
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: nether portal entity " + event.getFrom().getBlock().getType());
        // If the nether is disabled then quit immediately
        if (!Settings.netherGenerate || IslandWorld.getNetherWorld() == null) {
            return;
        }
        if (event.getEntity() == null) {
            return;
        }
        if (event.getFrom() != null && event.getFrom().getBlock().getType().equals(Material.ENDER_PORTAL)) {
            event.setCancelled(true);
            // Same action for all worlds except the end itself
            if (!event.getFrom().getWorld().getEnvironment().equals(Environment.THE_END)) {
                if (plugin.getServer().getWorld(Settings.worldName + "_the_end") != null) {
                    // The end exists
                    Location end_place = plugin.getServer().getWorld(Settings.worldName + "_the_end").getSpawnLocation();
                    event.getEntity().teleport(end_place);
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Result teleported " + event.getEntityType() + " to " + end_place);
                    return;
                }
            }
            return;
        }
        Location currentLocation = event.getFrom().clone();
        String currentWorld = currentLocation.getWorld().getName();
        // Only operate if this is Island territory
        if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")) {
            return;
        }
        // No entities may pass with the old nether
        if (!Settings.netherIslands) {
            event.setCancelled(true);
            return;
        }
        // New nether
        // Entities can pass only if there are adjoining portals
        Location dest = event.getFrom().toVector().toLocation(IslandWorld.getIslandWorld());
        if (event.getFrom().getWorld().getEnvironment().equals(Environment.NORMAL)) {
            dest = event.getFrom().toVector().toLocation(IslandWorld.getNetherWorld());
        }
        // Vehicles
        if (event.getEntity() instanceof Vehicle) {
            Vehicle vehicle = (Vehicle)event.getEntity();
            vehicle.eject();
        }
        new SafeSpotTeleport(plugin, event.getEntity(), dest);
        event.setCancelled(true);
    }

    // Nether portal spawn protection

    /**
     * Function to check proximity to nether spawn location
     *
     * @param player
     * @return true if in the spawn area, false if not
     */
    private boolean awayFromSpawn(Player player) {
        Vector p = player.getLocation().toVector().multiply(new Vector(1, 0, 1));
        Vector spawn = player.getWorld().getSpawnLocation().toVector().multiply(new Vector(1, 0, 1));
        return spawn.distanceSquared(p) < (Settings.netherSpawnRadius * Settings.netherSpawnRadius);
    }

    /**
     * Prevents blocks from being broken
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());
        // plugin.getLogger().info("Block break");
        if ((e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether") && !Settings.netherIslands)
                || e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
            if (VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            if (DEBUG)
                plugin.getLogger().info("Block break in island nether");
            if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
                e.getPlayer().sendMessage(plugin.getLocale(e.getPlayer()).get("nether.spawnisprotected"));
                e.setCancelled(true);
            }
        }

    }

    /**
     * Prevents placing of blocks
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());
        if (!Settings.netherIslands) {
            if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
                    || e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
                if (VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                    return;
                }
                if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());
        if (!Settings.netherIslands) {
            if (e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
                    || e.getPlayer().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
                if (VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                    return;
                }
                if (!awayFromSpawn(e.getPlayer()) && !e.getPlayer().isOp()) {
                    e.setCancelled(true);
                }
            }
        }
    }

    /**
     * Prevent the Nether spawn from being blown up
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (Settings.netherIslands) {
            // Not used in the new nether
            return;
        }
        // Find out what is exploding
        Entity expl = e.getEntity();
        if (expl == null) {
            return;
        }
        // Check world
        if (!e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_nether")
                || e.getEntity().getWorld().getName().equalsIgnoreCase(Settings.worldName + "_the_end")) {
            return;
        }
        Location spawn = e.getLocation().getWorld().getSpawnLocation();
        Location loc = e.getLocation();
        if (spawn.distance(loc) < Settings.netherSpawnRadius) {
            e.blockList().clear();
        }
    }

    /**
     * Converts trees to gravel and glowstone
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTreeGrow(final StructureGrowEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: " + e.getEventName());

        if (!Settings.netherTrees) {
            return;
        }
        if (!Settings.netherGenerate || IslandWorld.getNetherWorld() == null) {
            return;
        }
        // Check world
        if (!e.getLocation().getWorld().equals(IslandWorld.getNetherWorld())) {
            return;
        }
        for (BlockState b : e.getBlocks()) {
            if (b.getType() == Material.LOG || b.getType() == Material.LOG_2) {
                b.setType(Material.GRAVEL);
            } else if (b.getType() == Material.LEAVES || b.getType() == Material.LEAVES_2) {
                b.setType(Material.GLOWSTONE);
            }
        }
    }
}