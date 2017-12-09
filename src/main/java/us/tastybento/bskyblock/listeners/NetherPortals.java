package us.tastybento.bskyblock.listeners;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.StructureGrowEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.managers.island.IslandsManager;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.island.builders.IslandBuilder;
import us.tastybento.bskyblock.island.builders.IslandBuilder.IslandType;
import us.tastybento.bskyblock.util.SafeSpotTeleport;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

public class NetherPortals implements Listener {
    private final BSkyBlock plugin;
    private final static boolean DEBUG = true;

    public NetherPortals(BSkyBlock plugin) {
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

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Player portal event - reason =" + event.getCause());
        UUID playerUUID = event.getPlayer().getUniqueId();
        // If the nether is disabled then quit immediately
        if (!Settings.netherGenerate || IslandWorld.getNetherWorld() == null) {
            return;
        }
        Location currentLocation = event.getFrom().clone();
        String currentWorld = currentLocation.getWorld().getName();
        if (!currentWorld.equalsIgnoreCase(Settings.worldName) && !currentWorld.equalsIgnoreCase(Settings.worldName + "_nether")
                && !currentWorld.equalsIgnoreCase(Settings.worldName + "_the_end")) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: not right world");
            return;
        }
        // Check if player has permission
        Island island = plugin.getIslands().getIslandAt(currentLocation);
        // TODO: if ((island == null && !Settings.defaultWorldSettings.get(SettingsFlag.PORTAL)) 
        if (island == null
                || (island != null && !(island.getFlag(SettingsFlag.PORTAL) || island.getMembers().contains(event.getPlayer().getUniqueId())))) {
            // Portals use is not allowed
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Portal use not allowed");
            if (!event.getPlayer().isOp() && !VaultHelper.hasPerm(event.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                Util.sendMessage(event.getPlayer(), plugin.getLocale(event.getPlayer().getUniqueId()).get("island.protected"));
                event.setCancelled(true);
                return;
            }
        }
        // Determine what portal it is
        switch (event.getCause()) {
            case END_PORTAL:
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: End portal");
                // Same action for all worlds except the end itself
                if (!event.getFrom().getWorld().getEnvironment().equals(Environment.THE_END)) {
                    if (plugin.getServer().getWorld(Settings.worldName + "_the_end") != null) {
                        // The end exists
                        event.setCancelled(true);
                        Location end_place = plugin.getServer().getWorld(Settings.worldName + "_the_end").getSpawnLocation();
                        if (IslandsManager.isSafeLocation(end_place)) {
                            event.getPlayer().teleport(end_place);
                            // event.getPlayer().sendBlockChange(end_place,
                            // end_place.getBlock().getType(),end_place.getBlock().getData());
                            return;
                        } else {
                            Util.sendMessage(event.getPlayer(), plugin.getLocale(event.getPlayer().getUniqueId()).get("warps.error.NotSafe"));
                            plugin.getIslands().homeTeleport(event.getPlayer());
                            return;
                        }
                    }
                } else {
                    event.setCancelled(true);
                    plugin.getIslands().homeTeleport(event.getPlayer());
                }
                break;
            case NETHER_PORTAL:
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: nether portal");
                // Get the home world of this player
                World homeWorld = IslandWorld.getIslandWorld();
                Location home = plugin.getPlayers().getHomeLocation(event.getPlayer().getUniqueId());
                if (home != null) {
                    homeWorld = home.getWorld();
                }
                if (!Settings.netherIslands) {
                    // Legacy action
                    if (event.getFrom().getWorld().getEnvironment().equals(Environment.NORMAL)) {
                        // Going to Nether
                        if (homeWorld.getEnvironment().equals(Environment.NORMAL)) {
                            // Home world is over world
                            event.setTo(IslandWorld.getNetherWorld().getSpawnLocation());
                            event.useTravelAgent(true);
                        } else {
                            // Home world is nether - going home
                            event.useTravelAgent(false);
                            Location dest = plugin.getIslands().getSafeHomeLocation(playerUUID,1);
                            if (dest != null) {
                                event.setTo(dest);
                            } else {
                                event.setCancelled(true);
                                new SafeSpotTeleport(plugin, event.getPlayer(), plugin.getIslands().getIslandLocation(playerUUID), 1);
                            }
                        }
                    } else {
                        // Going to Over world
                        if (homeWorld.getEnvironment().equals(Environment.NORMAL)) {
                            // Home world is over world
                            event.useTravelAgent(false);
                            Location dest = plugin.getIslands().getSafeHomeLocation(playerUUID,1);
                            if (dest != null) {
                                event.setTo(dest);
                            } else {
                                event.setCancelled(true);
                                new SafeSpotTeleport(plugin, event.getPlayer(), plugin.getIslands().getIslandLocation(playerUUID), 1);
                            }
                        } else {
                            // Home world is nether
                            event.setTo(IslandWorld.getIslandWorld().getSpawnLocation());
                            event.useTravelAgent(true);
                        }
                    }
                } else {
                    // Island Nether
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Island nether");
                    // Get location of the island where the player is at
                    if (island == null) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: island is null");
                        event.setCancelled(true);
                        return;
                    }
                    // Can go both ways now
                    Location overworldIsland = island.getCenter().toVector().toLocation(IslandWorld.getIslandWorld());
                    Location netherIsland = island.getCenter().toVector().toLocation(IslandWorld.getNetherWorld());
                    //Location dest = event.getFrom().toVector().toLocation(IslandWorld.getIslandWorld());
                    if (event.getFrom().getWorld().getEnvironment().equals(Environment.NORMAL)) {
                        // Going to Nether
                        // Check that there is a nether island there. Due to legacy reasons it may not exist
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: island center = " + island.getCenter());
                        if (netherIsland.getBlock().getType() != Material.BEDROCK) {
                            // Check to see if there is anything there
                            if (plugin.getIslands().bigScan(netherIsland, 20) == null) {
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: big scan is null");
                                plugin.getLogger().warning("Creating nether island for " + event.getPlayer().getName());
                                new IslandBuilder(island)
                                    .setPlayer(event.getPlayer())
                                    .setChestItems(Settings.chestItems)
                                    .setType(IslandType.NETHER)
                                    .build();
                            }
                        }
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: Teleporting to " + event.getFrom().toVector().toLocation(IslandWorld.getNetherWorld()));
                        event.setCancelled(true);
                        // Teleport using the new safeSpot teleport
                        new SafeSpotTeleport(plugin, event.getPlayer(), netherIsland);
                        return;
                    }
                    // Going to the over world - if there isn't an island, do nothing
                    event.setCancelled(true);
                    // Teleport using the new safeSpot teleport
                    new SafeSpotTeleport(plugin, event.getPlayer(), overworldIsland);
                }
                break;
            default:
                break;
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