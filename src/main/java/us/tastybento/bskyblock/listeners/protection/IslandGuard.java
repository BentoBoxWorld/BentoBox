package us.tastybento.bskyblock.listeners.protection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Golem;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.Potion;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.events.island.IslandEvent;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands
 */
@SuppressWarnings("deprecation")
public class IslandGuard implements Listener {
    private final BSkyBlock plugin;
    private final static boolean DEBUG = false;
    private static final boolean DEBUG2 = false;
    private HashMap<UUID,Vector> onPlate = new HashMap<>();
    private Set<Location> tntBlocks = new HashSet<>();
    private Set<UUID> litCreeper = new HashSet<>();

    public IslandGuard(final BSkyBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevents visitors picking items from riding horses or other inventories
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHorseInventoryClick(InventoryClickEvent event) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: horse and llama inventory click");
        if (event.getInventory().getHolder() == null) {
            return;
        }
        // World check
        if (!Util.inWorld(event.getWhoClicked())) {
            return;
        }
        if (event.getInventory().getHolder() instanceof Animals) {
            if (actionAllowed((Player)event.getWhoClicked(), event.getWhoClicked().getLocation(), SettingsFlag.MOUNT_INVENTORY)) {
                return;
            }
            // Elsewhere - not allowed
            Util.sendMessage(event.getWhoClicked(), "island.protected");
            event.setCancelled(true);
        }
    }

    /**
     * Checks if action is allowed for player in location for flag
     * @param player
     * @param location
     * @param flag
     * @return true if allowed
     */
    private boolean actionAllowed(Player player, Location location, SettingsFlag flag) {
        if (player == null) {
            return actionAllowed(location, flag);
        }
        // This permission bypasses protection
        if (player.isOp() || VaultHelper.hasPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
            return true;
        }
        Island island = plugin.getIslands().getProtectedIslandAt(location);
        if (island != null && (island.getFlag(flag) || island.getMembers().contains(player.getUniqueId()))){
            return true;
        }
        if (island == null && Settings.defaultWorldSettings.get(flag)) {
            return true;
        }
        return false;
    }

    /**
     * Action allowed in this location
     * @param location
     * @param flag
     * @return true if allowed
     */
    private boolean actionAllowed(Location location, SettingsFlag flag) {
        Island island = plugin.getIslands().getProtectedIslandAt(location);
        if (island != null && island.getFlag(flag)){
            return true;
        }
        if (island == null && Settings.defaultWorldSettings.get(flag)) {
            return true;
        }
        return false;
    }

    // Vehicle damage
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleDamageEvent(VehicleDamageEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info(e.getAttacker().getType().toString());
        }
        if (Util.inWorld(e.getVehicle())) {
            if (!(e.getAttacker() instanceof Player)) {
                return;
            }
            Player p = (Player) e.getAttacker();
            if (actionAllowed(p, e.getVehicle().getLocation(), SettingsFlag.BREAK_BLOCKS)) {
                return;
            }
            // Not allowed
            Util.sendMessage(p, plugin.getLocale(p.getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onVehicleMove(final VehicleMoveEvent e) {
        if (DEBUG)
            plugin.getLogger().info("DEBUG: vehicle move = " + e.getVehicle());
        if (!Util.inWorld(e.getVehicle())) {
            return;
        }

        Entity passenger = e.getVehicle().getPassenger();
        if (passenger == null || !(passenger instanceof Player)) {
            return;
        }

        Player player = (Player)passenger;
        if (plugin.getIslands() == null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: grid = null");
            return;
        }
        Island islandTo = plugin.getIslands().getProtectedIslandAt(e.getTo());
        // Announcement entering
        Island islandFrom = plugin.getIslands().getProtectedIslandAt(e.getFrom());
        // Only says something if there is a change in islands
        /*
         * Situations:
         * islandTo == null && islandFrom != null - exit
         * islandTo == null && islandFrom == null - nothing
         * islandTo != null && islandFrom == null - enter
         * islandTo != null && islandFrom != null - same PlayerIsland or teleport?
         * islandTo == islandFrom
         */
        // plugin.getLogger().info("islandTo = " + islandTo);
        // plugin.getLogger().info("islandFrom = " + islandFrom);

        if (islandTo != null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
            // Lock check
            if (islandTo.isLocked() || plugin.getPlayers().isBanned(islandTo.getOwner(),player.getUniqueId())) {
                if (!islandTo.getMembers().contains(player.getUniqueId()) && !player.isOp()
                        && !VaultHelper.hasPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")
                        && !VaultHelper.hasPerm(player, Settings.PERMPREFIX + "mod.bypasslock")) {
                    Util.sendMessage(player, plugin.getLocale(player.getUniqueId()).get("lock.islandlocked"));
                    // Get the vector away from this island
                    Vector v = e.getVehicle().getLocation().toVector().subtract(islandTo.getCenter().toVector()).normalize().multiply(new Vector(1.2,0,1.2));
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: direction vector = " + v);
                    e.getVehicle().setVelocity(v);
                    return;
                }
            }
        }
        if (islandTo !=null && islandFrom == null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
            // Entering
            if (islandTo.isSpawn()) {
                if (!plugin.getLocale(player.getUniqueId()).get("lock.enteringspawn").isEmpty()) {
                    if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.enteringspawn"));
                    }
                }
            } else {
                if (!plugin.getLocale(player.getUniqueId()).get("lock.nowentering").isEmpty()) {
                    if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.nowentering").replace("[name]", plugin.getIslands().getIslandName(islandTo.getOwner())));
                    }
                }
            }
        } else if (islandTo == null && islandFrom != null && (islandFrom.getOwner() != null || islandFrom.isSpawn())) {
            // Leaving
            if (islandFrom.isSpawn()) {
                // Leaving
                if (!plugin.getLocale(player.getUniqueId()).get("lock.leavingspawn").isEmpty()) {
                    if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.leavingspawn"));
                    }
                }
            } else {
                if (!plugin.getLocale(player.getUniqueId()).get("lock.nowleaving").isEmpty()) {
                    if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.nowleaving").replace("[name]", plugin.getIslands().getIslandName(islandFrom.getOwner())));
                    }
                }
            }
        } else if (islandTo != null && islandFrom !=null && !islandTo.equals(islandFrom)) {
            // Adjacent islands or overlapping protections
            if (islandFrom.isSpawn()) {
                // Leaving
                if (!plugin.getLocale(player.getUniqueId()).get("lock.leavingspawn").isEmpty()) {
                    if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.leavingspawn"));
                    }
                }
            } else if (islandFrom.getOwner() != null){
                if (!plugin.getLocale(player.getUniqueId()).get("lock.nowleaving").isEmpty()) {
                    if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.nowleaving").replace("[name]", plugin.getIslands().getIslandName(islandFrom.getOwner())));
                    }
                }
            }
            if (islandTo.isSpawn()) {
                if (!plugin.getLocale(player.getUniqueId()).get("lock.enteringspawn").isEmpty()) {
                    if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.enteringspawn"));
                    }
                }
            } else if (islandTo.getOwner() != null) {
                if (!plugin.getLocale(player.getUniqueId()).get("lock.nowentering").isEmpty()) {
                    if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(player, plugin.getLocale(player.getUniqueId()).get("lock.nowentering").replace("[name]", plugin.getIslands().getIslandName(islandTo.getOwner())));
                    }
                }
            }
        }
    }


    /**
     * Adds island lock function
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(final PlayerMoveEvent e) {
        if (e.getPlayer().isDead()) {
            return;
        }
        if (!Util.inWorld(e.getPlayer())) {
            return;
        }
        if (plugin.getIslands() == null) {
            return;
        }
        // Only do something if there is a definite x or z movement
        if (e.getTo().getBlockX() - e.getFrom().getBlockX() == 0 && e.getTo().getBlockZ() - e.getFrom().getBlockZ() == 0) {
            return;
        }
        final Island islandTo = plugin.getIslands().getProtectedIslandAt(e.getTo());
        // Announcement entering
        final Island islandFrom = plugin.getIslands().getProtectedIslandAt(e.getFrom());
        // Only says something if there is a change in islands
        /*
         * Situations:
         * islandTo == null && islandFrom != null - exit
         * islandTo == null && islandFrom == null - nothing
         * islandTo != null && islandFrom == null - enter
         * islandTo != null && islandFrom != null - same PlayerIsland or teleport?
         * islandTo == islandFrom
         */
        // plugin.getLogger().info("islandTo = " + islandTo);
        // plugin.getLogger().info("islandFrom = " + islandFrom);
        if (islandTo != null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
            // Lock check
            if (islandTo.isLocked() || plugin.getPlayers().isBanned(islandTo.getOwner(),e.getPlayer().getUniqueId())) {
                if (!islandTo.getMembers().contains(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()
                        && !VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                        && !VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypasslock")) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.islandlocked"));

                    // Get the vector away from this island
                    if (e.getPlayer().isInsideVehicle()) {
                        if (e.getPlayer().getVehicle() instanceof LivingEntity) {
                            // Dismount
                            e.getPlayer().leaveVehicle();
                            e.setCancelled(true);
                        }

                    } else {
                        Vector v = e.getPlayer().getLocation().toVector().subtract(islandTo.getCenter().toVector()).normalize().multiply(new Vector(1.2,0,1.2));
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: direction vector = " + v);
                        e.getPlayer().setVelocity(v);
                    }
                    return;
                }
            }
        }

        if (islandTo != null && islandFrom == null && (islandTo.getOwner() != null || islandTo.isSpawn())) {
            // Entering
            if (islandTo.isLocked() || plugin.getPlayers().isBanned(islandTo.getOwner(),e.getPlayer().getUniqueId())) {
                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.islandlocked"));
            }
            if (islandTo.isSpawn()) {
                if (!plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.enteringspawn").isEmpty()) {
                    if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.enteringspawn"));
                    }
                }
            } else {
                if (!plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.nowentering").isEmpty()) {
                    if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                        Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.nowentering").replace("[name]", plugin.getIslands().getIslandName(islandTo.getOwner())));
                    }
                }
            }
            // Fire entry event
            final IslandEvent event = IslandEvent.builder()
                    .island(islandTo)
                    .reason(Reason.ENTER)
                    .involvedPlayer(e.getPlayer().getUniqueId())
                    .location(e.getTo())
                    .build();
            plugin.getServer().getPluginManager().callEvent(event);
        } else if (islandTo == null && islandFrom != null && (islandFrom.getOwner() != null || islandFrom.isSpawn())) {
            // Leaving
            if (islandFrom.isSpawn()) {
                // Leaving
                if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                    Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.leavingspawn"));
                }
            } else {
                if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                    Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.nowleaving").replace("[name]", plugin.getIslands().getIslandName(islandFrom.getOwner())));                    }
            }
            // Fire exit event
            final IslandEvent event = IslandEvent.builder()
                    .island(islandTo)
                    .reason(Reason.EXIT)
                    .involvedPlayer(e.getPlayer().getUniqueId())
                    .location(e.getFrom())
                    .build();
            plugin.getServer().getPluginManager().callEvent(event);
        } else if (islandTo != null && islandFrom != null && !islandTo.equals(islandFrom)) {
            // Adjacent islands or overlapping protections
            if (islandFrom.isSpawn()) {
                // Leaving
                if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                    Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.leavingspawn"));
                }
            } else if (islandFrom.getOwner() != null) {
                if(islandFrom.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                    Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.nowleaving").replace("[name]", plugin.getIslands().getIslandName(islandFrom.getOwner())));
                }
            }
            if (islandTo.isSpawn()) {
                if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                    Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.enteringspawn"));
                }
            } else if (islandTo.getOwner() != null) {
                if(islandTo.getFlag(SettingsFlag.ENTER_EXIT_MESSAGES)) {
                    Util.sendEnterExit(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("lock.nowentering").replace("[name]", plugin.getIslands().getIslandName(islandTo.getOwner())));
                }
            }
            // Fire exit event
            final IslandEvent event = IslandEvent.builder()
                    .island(islandTo)
                    .reason(Reason.EXIT)
                    .involvedPlayer(e.getPlayer().getUniqueId())
                    .location(e.getFrom())
                    .build();
            plugin.getServer().getPluginManager().callEvent(event);
            // Fire entry event
            final IslandEvent event2 = IslandEvent.builder()
                    .island(islandTo)
                    .reason(Reason.ENTER)
                    .involvedPlayer(e.getPlayer().getUniqueId())
                    .location(e.getTo())
                    .build();
            plugin.getServer().getPluginManager().callEvent(event2);
        }
    }


    /**
     * Prevents mobs spawning at spawn or in an island
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobSpawn(final CreatureSpawnEvent e) {
        if (DEBUG2) {
            plugin.getLogger().info("on Mob spawn" + e.getEventName());
        }
        // if grid is not loaded yet, return.
        if (plugin.getIslands() == null) {
            return;
        }
        // If not in the right world, return
        if (!Util.inWorld(e.getEntity())) {
            return;
        }
        // Deal with natural spawning
        if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime) {
            if (!actionAllowed(e.getLocation(), SettingsFlag.MONSTER_SPAWN)) {
                if (DEBUG2)
                    plugin.getLogger().info("Natural monster spawn cancelled.");
                // Mobs not allowed to spawn
                e.setCancelled(true);
                return;
            }
        }
        if (e.getEntity() instanceof Animals) {
            if (!actionAllowed(e.getLocation(), SettingsFlag.ANIMAL_SPAWN)) {
                // Animals are not allowed to spawn
                if (DEBUG2)
                    plugin.getLogger().info("Natural animal spawn cancelled.");
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (DEBUG) {
            // Commented out due to Ender dragon spam in Paper Spigot
            //plugin.getLogger().info(e.getEventName());
            //plugin.getLogger().info("Entity exploding is " + e.getEntity());
        }
        if (!Util.inWorld(e.getLocation())) {
            return;
        }
        // Find out what is exploding
        Entity expl = e.getEntity();
        if (expl == null) {
            // This allows beds to explode or other null entities, but still curtail the damage
            // Note player can still die from beds exploding in the nether.
            if (!Settings.allowTNTDamage) {
                //plugin.getLogger().info("TNT block damage prevented");
                e.blockList().clear();
            } else {
                if (!Settings.allowChestDamage) {
                    List<Block> toberemoved = new ArrayList<>();
                    // Save the chest blocks in a list
                    for (Block b : e.blockList()) {
                        switch (b.getType()) {
                            case CHEST:
                            case ENDER_CHEST:
                            case STORAGE_MINECART:
                            case TRAPPED_CHEST:
                                toberemoved.add(b);
                                break;
                            default:
                                break;
                        }
                    }
                    // Now delete them
                    for (Block b : toberemoved) {
                        e.blockList().remove(b);
                    }
                }
            }
            return;
        }
        // prevent at spawn
        if (plugin.getIslands().isAtSpawn(e.getLocation())) {
            e.setCancelled(true);
        }
        // Find out what is exploding
        EntityType exploding = e.getEntityType();
        if (exploding == null) {
            return;
        }
        switch (exploding) {
            case CREEPER:
                if (!Settings.allowCreeperDamage) {
                    // plugin.getLogger().info("Creeper block damage prevented");
                    e.blockList().clear();
                } else {
                    // Check if creeper griefing is allowed
                    if (!Settings.allowCreeperGriefing) {
                        // Find out who the creeper was targeting
                        Creeper creeper = (Creeper)e.getEntity();
                        if (creeper.getTarget() instanceof Player) {
                            Player target = (Player)creeper.getTarget();
                            // Check if the target is on their own island or not
                            if (!plugin.getIslands().locationIsOnIsland(target, e.getLocation())) {
                                // They are a visitor tsk tsk
                                // Stop the blocks from being damaged, but allow hurt still
                                e.blockList().clear();
                            }
                        }
                        // Check if this creeper was lit by a visitor
                        if (litCreeper.contains(creeper.getUniqueId())) {
                            if (DEBUG) {
                                plugin.getLogger().info("DBEUG: preventing creeper from damaging");
                            }
                            litCreeper.remove(creeper.getUniqueId());
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!Settings.allowChestDamage) {
                        List<Block> toberemoved = new ArrayList<Block>();
                        // Save the chest blocks in a list
                        for (Block b : e.blockList()) {
                            switch (b.getType()) {
                                case CHEST:
                                case ENDER_CHEST:
                                case STORAGE_MINECART:
                                case TRAPPED_CHEST:
                                    toberemoved.add(b);
                                    break;
                                default:
                                    break;
                            }
                        }
                        // Now delete them
                        for (Block b : toberemoved) {
                            e.blockList().remove(b);
                        }
                    }
                }
                break;
            case PRIMED_TNT:
            case MINECART_TNT:
                if (!Settings.allowTNTDamage) {
                    // plugin.getLogger().info("TNT block damage prevented");
                    e.blockList().clear();
                } else {
                    if (!Settings.allowChestDamage) {
                        List<Block> toberemoved = new ArrayList<>();
                        // Save the chest blocks in a list
                        for (Block b : e.blockList()) {
                            switch (b.getType()) {
                                case CHEST:
                                case ENDER_CHEST:
                                case STORAGE_MINECART:
                                case TRAPPED_CHEST:
                                    toberemoved.add(b);
                                    break;
                                default:
                                    break;
                            }
                        }
                        // Now delete them
                        for (Block b : toberemoved) {
                            e.blockList().remove(b);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Allows or prevents enderman griefing
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndermanGrief(final EntityChangeBlockEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!(e.getEntity() instanceof Enderman)) {
            return;
        }
        if (!Util.inWorld(e.getEntity())) {
            return;
        }
        // Prevent Enderman griefing at spawn
        if (plugin.getIslands() != null && plugin.getIslands().isAtSpawn(e.getEntity().getLocation())) {
            e.setCancelled(true);
        }
        if (Settings.allowEndermanGriefing)
            return;
        // Stop the Enderman from griefing
        // plugin.getLogger().info("Enderman stopped from griefing);
        e.setCancelled(true);
    }

    /**
     * Drops the Enderman's block when he dies if he has one
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEndermanDeath(final EntityDeathEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!Settings.endermanDeathDrop)
            return;
        if (!Util.inWorld(e.getEntity())) {
            return;
        }
        if (!(e.getEntity() instanceof Enderman)) {
            // plugin.getLogger().info("Not an Enderman!");
            return;
        }
        // Get the block the enderman is holding
        Enderman ender = (Enderman) e.getEntity();
        MaterialData m = ender.getCarriedMaterial();
        if (m != null && !m.getItemType().equals(Material.AIR)) {
            // Drop the item
            // plugin.getLogger().info("Dropping item " + m.toString());
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), m.toItemStack(1));
        }
    }

    /*
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onBlockBreakCheck(final BlockPhysicsEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: check: " + e.getEventName());
            plugin.getLogger().info("DEBUG: block is " + e.getBlock());
        }
    }
     */

    /**
     * Prevents blocks from being broken
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (Settings.allowAutoActivator && e.getPlayer().getName().equals("[CoFH]")) {
            return;
        }
        if (Util.inWorld(e.getPlayer())) {
            if (actionAllowed(e.getPlayer(), e.getBlock().getLocation(), SettingsFlag.BREAK_BLOCKS)) {
                return;
            }
            // Everyone else - not allowed
            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemFrameDamage(final EntityDamageByEntityEvent e) {
        // Check world
        if (!Util.inWorld(e.getEntity()) || !(e.getEntity() instanceof ItemFrame)) {
            return;
        }
        if (e.getDamager() instanceof Projectile) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Projectile damage to itemframe");
            // Find out who fired the arrow
            Projectile p = (Projectile) e.getDamager();
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Shooter is " + p.getShooter().toString());
            if (p.getShooter() instanceof Skeleton || p.getShooter() instanceof Golem) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Shooter is mob");
                if (!Settings.allowMobDamageToItemFrames) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Damage not allowed, cancelling");
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemFrameDamage(final HangingBreakByEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: Hanging break by entity event");
            plugin.getLogger().info("DEBUG: cause = " + e.getCause());
            plugin.getLogger().info("DEBUG: entity = " + e.getEntity());
            plugin.getLogger().info("DEBUG: remover = " + e.getRemover());
        }
        // Check world
        if (!Util.inWorld(e.getEntity()) || !(e.getEntity() instanceof ItemFrame)) {
            return;
        }
        if (e.getRemover() instanceof Skeleton || e.getRemover() instanceof Golem) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Remover is mob");
            if (!Settings.allowMobDamageToItemFrames) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Damage not allowed, cancelling");
                e.setCancelled(true);
            }
        }
    }
    /**
     * This method protects players from PVP if it is not allowed and from
     * arrows fired by other players
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("DEBUG: Damager = " + e.getDamager().toString());
            plugin.getLogger().info("DEBUG: Entitytype = " + e.getEntityType());
            plugin.getLogger().info("DEBUG: Entity = " + e.getEntity());
        }
        // Check world
        if (!Util.inWorld(e.getEntity())) {
            return;
        }
        // Get the island where the damage is occurring
        Island island = plugin.getIslands().getProtectedIslandAt(e.getEntity().getLocation());
        boolean inNether = false;
        if (e.getEntity().getWorld().equals(IslandWorld.getNetherWorld())) {
            inNether = true;
        }
        // Stop TNT damage if it is disallowed
        if (!Settings.allowTNTDamage && (e.getDamager().getType().equals(EntityType.PRIMED_TNT)
                || e.getDamager().getType().equals(EntityType.FIREWORK)
                || e.getDamager().getType().equals(EntityType.FIREBALL))) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: cancelling firework or fireball damage");
            e.setCancelled(true);
            return;
        }
        // Check for creeper damage at spawn
        if (island != null && island.isSpawn() && e.getDamager().getType().equals(EntityType.CREEPER) && island.getFlag(SettingsFlag.CREEPER_HURT)) {
            return;
        }
        // Stop Creeper damager if it is disallowed
        if (!Settings.allowCreeperDamage && e.getDamager().getType().equals(EntityType.CREEPER) && !(e.getEntity() instanceof Player)) {
            e.setCancelled(true);
            return;
        }
        // Stop Creeper griefing if it is disallowed
        if (!Settings.allowCreeperGriefing && e.getDamager().getType().equals(EntityType.CREEPER)) {
            // Now we have to check what the target was
            Creeper creeper = (Creeper)e.getDamager();
            //plugin.getLogger().info("DEBUG: creeper is damager");
            //plugin.getLogger().info("DEBUG: entity being damaged is " + e.getEntity());
            if (creeper.getTarget() instanceof Player) {
                //plugin.getLogger().info("DEBUG: target is a player");
                Player target = (Player)creeper.getTarget();
                //plugin.getLogger().info("DEBUG: player = " + target.getName());
                // Check if the target is on their own island or not
                if (!plugin.getIslands().locationIsOnIsland(target, e.getEntity().getLocation())) {
                    // They are a visitor tsk tsk
                    //plugin.getLogger().info("DEBUG: player is a visitor");
                    e.setCancelled(true);
                    return;
                }
            }
            // Check if this creeper was lit by a visitor
            if (litCreeper.contains(creeper.getUniqueId())) {
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: preventing creeeper from damaging");
                }
                e.setCancelled(true);
                return;
            }
        }
        // Ops can do anything
        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if (p.isOp() || VaultHelper.hasPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
        }
        // Get the real attacker
        boolean flamingArrow = false;
        boolean projectile = false;
        Player attacker = null;
        if (e.getDamager() instanceof Player) {
            attacker = (Player)e.getDamager();
        } else if (e.getDamager() instanceof Projectile) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Projectile damage");
            projectile = true;
            // Find out who fired the arrow
            Projectile p = (Projectile) e.getDamager();
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Shooter is " + p.getShooter().toString());
            if (p.getShooter() instanceof Player) {
                attacker = (Player) p.getShooter();
                if (p.getFireTicks() > 0) {
                    flamingArrow = true;
                }
                // Check if this is a flaming arrow
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: fire ticks = " + p.getFireTicks() + " max = " + p.getMaxFireTicks());
            }
        }
        if (attacker == null) {
            // Not a player
            return;
        }
        // Self damage
        if (e.getEntity() instanceof Player && attacker.equals((Player)e.getEntity())) {
            if (DEBUG) plugin.getLogger().info("Self damage!");
            return;
        }
        if (DEBUG)
            plugin.getLogger().info("DEBUG: Another player");

        // ITEM FRAME ENTITY DAMAGE or Armor Stand
        // Check to see if it's an item frame
        if (e.getEntity() instanceof ItemFrame || e.getEntityType().toString().endsWith("STAND")) {
            if (island != null && (island.getFlag(SettingsFlag.BREAK_BLOCKS) || island.getMembers().contains(attacker.getUniqueId()))) {
                return;
            }
            // Else not allowed
            Util.sendMessage(attacker, plugin.getLocale(attacker.getUniqueId()).get("island.protected"));
            if (flamingArrow)
                e.getEntity().setFireTicks(0);
            if (projectile)
                e.getDamager().remove();
            e.setCancelled(true);
            return;
        }
        // Monsters being hurt
        if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime || e.getEntity() instanceof Squid) {
            // Normal island check
            if (island != null && island.getMembers().contains(attacker.getUniqueId())) {
                // Members always allowed
                return;
            }
            if (actionAllowed(attacker, e.getEntity().getLocation(), SettingsFlag.HURT_MONSTERS)) {
                // Check for visitors setting creepers alight using flint steel
                if (!Settings.allowCreeperGriefing && e.getEntity() instanceof Creeper) {
                    for (ItemStack holding : Util.getPlayerInHandItems(attacker)) {
                        if (holding.getType().equals(Material.FLINT_AND_STEEL)) {
                            // Save this creeper for later when any damage caused by its explosion will be nullified
                            litCreeper.add(e.getEntity().getUniqueId());
                            if (DEBUG) {
                                plugin.getLogger().info("DEBUG: adding to lit creeper set");
                            }
                        }
                    }
                }
                return;
            }
            // Not allowed
            Util.sendMessage(attacker, plugin.getLocale(attacker.getUniqueId()).get("island.protected"));
            if (flamingArrow)
                e.getEntity().setFireTicks(0);
            if (projectile)
                e.getDamager().remove();
            e.setCancelled(true);
            return;
        }

        // Mobs being hurt
        if (e.getEntity() instanceof Animals || e.getEntity() instanceof IronGolem || e.getEntity() instanceof Snowman
                || e.getEntity() instanceof Villager) {
            if (island != null && (island.getFlag(SettingsFlag.HURT_ANIMALS) || island.getMembers().contains(attacker.getUniqueId()))) {
                return;
            }
            if (DEBUG)
                plugin.getLogger().info("DEBUG: Mobs not allowed to be hurt. Blocking");
            // Else not allowed
            Util.sendMessage(attacker, plugin.getLocale(attacker.getUniqueId()).get("island.protected"));
            if (flamingArrow)
                e.getEntity().setFireTicks(0);
            if (projectile)
                e.getDamager().remove();
            e.setCancelled(true);
            return;
        }

        // Establish whether PVP is allowed or not.
        boolean pvp = false;
        if ((inNether && island != null && island.getFlag(SettingsFlag.PVP_NETHER) || (!inNether && island != null && island.getFlag(SettingsFlag.PVP_OVERWORLD)))) {
            if (DEBUG) plugin.getLogger().info("DEBUG: PVP allowed");
            pvp = true;
        }

        // Players being hurt PvP
        if (e.getEntity() instanceof Player) {
            if (!pvp) {
                Util.sendMessage(attacker, plugin.getLocale(attacker.getUniqueId()).get("targetInPVPArea"));
                if (flamingArrow)
                    e.getEntity().setFireTicks(0);
                if (projectile)
                    e.getDamager().remove();
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents placing of blocks
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBlockPlace(final BlockPlaceEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: " + e.getEventName());
            if (e.getPlayer() == null) {
                plugin.getLogger().info("DEBUG: player is null");
            } else {
                plugin.getLogger().info("DEBUG: block placed by " + e.getPlayer().getName());
            }
            plugin.getLogger().info("DEBUG: Block is " + e.getBlock().toString());
        }
        if (Settings.allowAutoActivator && e.getPlayer().getName().equals("[CoFH]")) {
            return;
        }
        // plugin.getLogger().info(e.getEventName());
        if (Util.inWorld(e.getPlayer())) {
            // This permission bypasses protection
            if (e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            //plugin.getLogger().info("DEBUG: checking is inside protection area");
            Island island = plugin.getIslands().getProtectedIslandAt(e.getBlock().getLocation());
            // Outside of island protection zone
            if (island == null) {
                if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
                return;
            }
            if (actionAllowed(e.getPlayer(), e.getBlock().getLocation(), SettingsFlag.PLACE_BLOCKS))  {
                // Check how many placed
                //plugin.getLogger().info("DEBUG: block placed " + e.getBlock().getType());
                String type = e.getBlock().getType().toString();
                if (!e.getBlock().getState().getClass().getName().endsWith("CraftBlockState")
                        // Not all blocks have that type of class, so we have to do some explicit checking...
                        || e.getBlock().getType().equals(Material.REDSTONE_COMPARATOR_OFF)
                        || type.endsWith("BANNER") // Avoids V1.7 issues
                        || e.getBlock().getType().equals(Material.ENDER_CHEST)
                        || e.getBlock().getType().equals(Material.ENCHANTMENT_TABLE)
                        || e.getBlock().getType().equals(Material.DAYLIGHT_DETECTOR)
                        || e.getBlock().getType().equals(Material.FLOWER_POT)){
                    // tile entity placed
                    if (Settings.limitedBlocks.containsKey(type) && Settings.limitedBlocks.get(type) > -1) {
                        int count = island.getTileEntityCount(e.getBlock().getType(),e.getBlock().getWorld());
                        //plugin.getLogger().info("DEBUG: count is "+ count);
                        if (Settings.limitedBlocks.get(type) <= count) {
                            Util.sendMessage(e.getPlayer(), (plugin.getLocale(e.getPlayer().getUniqueId()).get("moblimits.entity").replace("[entity]",
                                    Util.prettifyText(type))).replace("[number]", String.valueOf(Settings.limitedBlocks.get(type))));
                            e.setCancelled(true);
                        }
                    }
                }
            } else {
                // Visitor
                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBlockPlace(final BlockMultiPlaceEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: " + e.getEventName());
            if (e.getPlayer() == null) {
                plugin.getLogger().info("DEBUG: player is null");
            } else {
                plugin.getLogger().info("DEBUG: block placed by " + e.getPlayer().getName());
            }
            plugin.getLogger().info("DEBUG: Block is " + e.getBlock().toString());
        }
        if (Settings.allowAutoActivator && e.getPlayer().getName().equals("[CoFH]")) {
            return;
        }
        // plugin.getLogger().info(e.getEventName());
        if (Util.inWorld(e.getPlayer())) {
            // This permission bypasses protection
            if (e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            Island island = plugin.getIslands().getProtectedIslandAt(e.getBlock().getLocation());
            if (island == null) {
                if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
                return;
            }
            // Island exists
            if (island.getFlag(SettingsFlag.PLACE_BLOCKS) || island.getMembers().contains(e.getPlayer().getUniqueId()))  {
                // Check how many placed
                //plugin.getLogger().info("DEBUG: block placed " + e.getBlock().getType());
                String type = e.getBlock().getType().toString();
                if (!e.getBlock().getState().getClass().getName().endsWith("CraftBlockState")
                        // Not all blocks have that type of class, so we have to do some explicit checking...
                        || e.getBlock().getType().equals(Material.REDSTONE_COMPARATOR_OFF)
                        || type.endsWith("BANNER") // Avoids V1.7 issues
                        || e.getBlock().getType().equals(Material.ENDER_CHEST)
                        || e.getBlock().getType().equals(Material.ENCHANTMENT_TABLE)
                        || e.getBlock().getType().equals(Material.DAYLIGHT_DETECTOR)
                        || e.getBlock().getType().equals(Material.FLOWER_POT)){
                    // tile entity placed
                    if (Settings.limitedBlocks.containsKey(type) && Settings.limitedBlocks.get(type) > -1) {
                        int count = island.getTileEntityCount(e.getBlock().getType(),e.getBlock().getWorld());
                        if (Settings.limitedBlocks.get(type) <= count) {
                            Util.sendMessage(e.getPlayer(), (plugin.getLocale(e.getPlayer().getUniqueId()).get("moblimits.entity").replace("[entity]",
                                    Util.prettifyText(type))).replace("[number]", String.valueOf(Settings.limitedBlocks.get(type))));
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
                return;
            }
            // Outside of protection area or visitor
            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerLeashHitch(final HangingPlaceEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("DEBUG: block placed " + e.getBlock().getType());
            plugin.getLogger().info("DEBUG: entity " + e.getEntity().getType());
        }
        if (Settings.allowAutoActivator && e.getPlayer().getName().equals("[CoFH]")) {
            return;
        }
        // plugin.getLogger().info(e.getEventName());
        if (Util.inWorld(e.getPlayer())) {
            if (e.getEntity() != null && e.getEntity().getType().equals(EntityType.LEASH_HITCH)) {
                if (!actionAllowed(e.getPlayer(), e.getBlock().getLocation(), SettingsFlag.LEASH)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerBlockPlace(final HangingPlaceEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("DEBUG: block placed " + e.getBlock().getType());
            plugin.getLogger().info("DEBUG: entity " + e.getEntity().getType());
        }
        if (Settings.allowAutoActivator && e.getPlayer().getName().equals("[CoFH]")) {
            return;
        }
        // plugin.getLogger().info(e.getEventName());
        if (Util.inWorld(e.getPlayer())) {
            // This permission bypasses protection
            if (e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            Island island = plugin.getIslands().getProtectedIslandAt(e.getBlock().getLocation());
            // Outside of island protection zone
            if (island == null) {
                if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
                return;
            }
            if (island.getFlag(SettingsFlag.PLACE_BLOCKS) || island.getMembers().contains(e.getPlayer().getUniqueId()))  {
                // Check how many placed
                String type = e.getEntity().getType().toString();
                if (e.getEntity().getType().equals(EntityType.ITEM_FRAME) || e.getEntity().getType().equals(EntityType.PAINTING)) {
                    // tile entity placed
                    if (Settings.limitedBlocks.containsKey(type) && Settings.limitedBlocks.get(type) > -1) {
                        // Convert from EntityType to Material via string - ugh
                        int count = island.getTileEntityCount(Material.valueOf(type),e.getEntity().getWorld());
                        if (Settings.limitedBlocks.get(type) <= count) {
                            Util.sendMessage(e.getPlayer(), (plugin.getLocale(e.getPlayer().getUniqueId()).get("moblimits.entity").replace("[entity]",
                                    Util.prettifyText(type))).replace("[number]", String.valueOf(Settings.limitedBlocks.get(type))));
                            e.setCancelled(true);
                        }
                    }
                }
            } else {
                // Visitor
                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                e.setCancelled(true);
            }
        }
    }

    // Prevent sleeping in other beds
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBedEnter(final PlayerBedEnterEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Check world
        if (Util.inWorld(e.getPlayer())) {
            if (actionAllowed(e.getPlayer(),e.getBed().getLocation(), SettingsFlag.BED)) {
                return;
            }
            // Not allowed
            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }
    }

    /**
     * Prevents the breakage of hanging items
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onBreakHanging(final HangingBreakByEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info(e.getRemover().toString());
        }
        if (Util.inWorld(e.getEntity())) {
            if ((e.getRemover() instanceof Creeper) && !Settings.allowCreeperDamage) {
                e.setCancelled(true);
                return;
            }
            // Check if creeper griefing is allowed
            if ((e.getRemover() instanceof Creeper) && !Settings.allowCreeperGriefing) {
                // Find out who the creeper was targeting
                Creeper creeper = (Creeper)e.getRemover();
                if (creeper.getTarget() instanceof Player) {
                    Player target = (Player)creeper.getTarget();
                    // Check if the target is on their own island or not
                    if (!plugin.getIslands().locationIsOnIsland(target, e.getEntity().getLocation())) {
                        // They are a visitor tsk tsk
                        e.setCancelled(true);
                        return;
                    }
                }
                // Check if this creeper was lit by a visitor
                if (litCreeper.contains(creeper.getUniqueId())) {
                    if (DEBUG) {
                        plugin.getLogger().info("DBEUG: preventing creeper from damaging");
                    }
                    e.setCancelled(true);
                    return;
                }
            }
            if (e.getRemover() instanceof Player) {
                Player p = (Player) e.getRemover();
                // This permission bypasses protection
                if (p.isOp() || VaultHelper.hasPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
                    return;
                }
                if (actionAllowed(p,e.getEntity().getLocation(),SettingsFlag.BREAK_BLOCKS)) {
                    return;
                }
                // Not allowed
                Util.sendMessage(p, plugin.getLocale(p.getUniqueId()).get("island.protected"));
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents the leash use
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLeashUse(final PlayerLeashEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (Util.inWorld(e.getEntity())) {
            if (e.getPlayer() != null) {
                Player player = e.getPlayer();
                if (actionAllowed(player, e.getEntity().getLocation(),SettingsFlag.LEASH)) {
                    return;
                }
                Util.sendMessage(player, plugin.getLocale(player.getUniqueId()).get("island.protected"));
                e.setCancelled(true);
            }
        }
    }


    /**
     * Prevents the leash use
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onLeashUse(final PlayerUnleashEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (Util.inWorld(e.getEntity())) {
            if (e.getPlayer() != null) {
                Player player = e.getPlayer();
                if (actionAllowed(player, e.getEntity().getLocation(),SettingsFlag.LEASH)) {
                    return;
                }
                Util.sendMessage(player, plugin.getLocale(player.getUniqueId()).get("island.protected"));
                e.setCancelled(true);
            }
        }
    }
    /*
     * Not going to implement this right now, but it could be used if required
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onLiquidFlow(final BlockFromToEvent e) {
	// Ignore non-island worlds
	if (!Util.inWorld(e.getBlock())) {
	    return;
	}
	// Only check lateral movement
	if (e.getBlock().getLocation().getBlockX() == e.getToBlock().getLocation().getBlockX()
		&& e.getBlock().getLocation().getBlockZ() == e.getToBlock().getLocation().getBlockZ()) {
	    return;
	}
	// Ignore flows within flow
	if (e.getBlock().getType().equals(e.getToBlock().getType())) {
	    return;
	}
	// Ignore stationary to non-stationary
	if (e.getBlock().getType().equals(Material.STATIONARY_WATER) && e.getToBlock().getType().equals(Material.WATER) ) {
	    return;
	}
	if (e.getBlock().getType().equals(Material.STATIONARY_LAVA) && e.getToBlock().getType().equals(Material.LAVA) ) {
	    return;
	}
	// Check if flow leaving island protection range
	// Check home island
	Island from = plugin.getIslands().getProtectedIslandAt(e.getBlock().getLocation());
	Island to = plugin.getIslands().getProtectedIslandAt(e.getToBlock().getLocation());
	// Scenarios
	// 1. inside district or outside - always ok
	// 2. inside to outside - not allowed
	// 3. outside to inside - allowed
	if (to == null && from == null) {
	    return;
	}
	if (to !=null && from != null && to.equals(from)) {
	    return;
	}
	// Flows into an island space is allowed, e.g., sea
	if (to != null) {
	    return;
	}
	// Otherwise cancel - the flow is not allowed
	e.setCancelled(true);
    }
     */

    /**
     * Prevents emptying of buckets outside of island space
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (Util.inWorld(e.getPlayer())) {
            Player p = e.getPlayer();
            if (e.getBlockClicked() != null) {
                // This is where the water or lava actually will be dumped
                Block dumpBlock = e.getBlockClicked().getRelative(e.getBlockFace());
                if (actionAllowed(p, dumpBlock.getLocation(), SettingsFlag.BUCKET)) {
                    // Check if biome is Nether and then allow water placement but fizz the water
                    if (e.getBlockClicked().getBiome().equals(Biome.HELL)) {
                        if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                            if (e.getPlayer().getItemInHand().getType().equals(Material.WATER_BUCKET)) {
                                e.setCancelled(true);
                                e.getPlayer().getItemInHand().setType(Material.BUCKET);
                                e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.valueOf("FIZZ"), 1F, 2F);
                                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("biome.set").replace("[biome]", "Nether"));
                            }
                        } else {
                            if (Util.playerIsHolding(e.getPlayer(), Material.WATER_BUCKET)) {
                                e.setCancelled(true);
                                if (e.getPlayer().getInventory().getItemInMainHand().getType() == Material.WATER_BUCKET) {
                                    e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
                                } else if (e.getPlayer().getInventory().getItemInOffHand().getType() == Material.WATER_BUCKET) {
                                    e.getPlayer().getInventory().setItemInOffHand(new ItemStack(Material.BUCKET));
                                }
                                e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1F, 2F);
                                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("biome.set").replace("[biome]", "Nether"));
                            }
                        }
                    }
                    return;
                }
                // Not allowed
                Util.sendMessage(p, plugin.getLocale(p.getUniqueId()).get("island.protected"));
                e.setCancelled(true);
            }
        }
    }

    /**
     * Prevents water from being dispensed in hell biomes
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onNetherDispenser(final BlockDispenseEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!Util.inWorld(e.getBlock().getLocation()) || !e.getBlock().getBiome().equals(Biome.HELL)) {
            return;
        }
        // plugin.getLogger().info("DEBUG: Item being dispensed is " +
        // e.getItem().getType().toString());
        if (e.getItem().getType().equals(Material.WATER_BUCKET)) {
            e.setCancelled(true);
            if (plugin.getServer().getVersion().contains("(MC: 1.8") || plugin.getServer().getVersion().contains("(MC: 1.7")) {
                e.getBlock().getWorld().playSound(e.getBlock().getLocation(), Sound.valueOf("FIZZ"), 1F, 2F);
            } else {
                e.getBlock().getWorld().playSound(e.getBlock().getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1F, 2F);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBucketFill(final PlayerBucketFillEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (Util.inWorld(e.getPlayer())) {
            // This permission bypasses protection
            if (VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            Island island = plugin.getIslands().getProtectedIslandAt(e.getBlockClicked().getLocation());
            if (island != null) {
                if (island.getMembers().contains(e.getPlayer().getUniqueId())) {
                    return;
                }
                if (island.getFlag(SettingsFlag.COLLECT_LAVA) && e.getItemStack().getType().equals(Material.LAVA_BUCKET)) {
                    return;
                }
                if (island.getFlag(SettingsFlag.COLLECT_WATER) && e.getItemStack().getType().equals(Material.WATER_BUCKET)) {
                    return;
                }
                if (island.getFlag(SettingsFlag.MILKING) && e.getItemStack().getType().equals(Material.MILK_BUCKET)) {
                    return;
                }
                if (island.getFlag(SettingsFlag.BUCKET)) {
                    return;
                }
            } else {
                // Null
                if (Settings.defaultWorldSettings.get(SettingsFlag.BUCKET)) {
                    return;
                }
            }
            // Not allowed
            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }
    }

    // Protect sheep
    @EventHandler(priority = EventPriority.LOW)
    public void onShear(final PlayerShearEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (Util.inWorld(e.getPlayer())) {
            if (actionAllowed(e.getPlayer(), e.getEntity().getLocation(), SettingsFlag.SHEARING)) {
                return;
            }
            // Not allowed
            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }
    }

    /**
     * Handles interaction with objects
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!Util.inWorld(e.getPlayer())) {
            return;
        }
        if (e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
            return;
        }
        if ((e.getClickedBlock() != null && plugin.getIslands().locationIsOnIsland(e.getPlayer(), e.getClickedBlock().getLocation()))) {
            // You can do anything on your island
            return;
        }
        // Player is not clicking a block, they are clicking a material so this
        // is driven by where the player is
        if (e.getClickedBlock() == null && (e.getMaterial() != null && plugin.getIslands().playerIsOnIsland(e.getPlayer()))) {
            return;
        }
        // Get island
        Island island = plugin.getIslands().getProtectedIslandAt(e.getPlayer().getLocation());
        if (DEBUG) {
            if (island == null) {
                plugin.getLogger().info("DEBUG: Player is not on a known island.");
            } else {
                plugin.getLogger().info("DEBUG: Player is on a known island.");
            }
        }
        // Check for disallowed clicked blocks
        if (e.getClickedBlock() != null) {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: clicked block " + e.getClickedBlock());
                plugin.getLogger().info("DEBUG: Material " + e.getMaterial());
            }
            // Look along player's sight line to see if any blocks are fire
            try {
                BlockIterator iter = new BlockIterator(e.getPlayer(), 10);
                Block lastBlock = iter.next();
                while (iter.hasNext()) {
                    lastBlock = iter.next();
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: lastBlock = " + lastBlock.toString());
                    if (lastBlock.equals(e.getClickedBlock())) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: found clicked block");
                        break;
                    }
                    if (lastBlock.getType().equals(Material.FIRE)) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: fire found");
                        if (island != null) {
                            if (!island.getFlag(SettingsFlag.FIRE_EXTINGUISH)) {
                                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                                e.setCancelled(true);
                                return;
                            }
                        } else {
                            if (Settings.defaultWorldSettings.get(SettingsFlag.FIRE_EXTINGUISH)) {
                                if (DEBUG)
                                    plugin.getLogger().info("DEBUG: extinguishing is allowed");
                                continue;
                            } else {
                                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                // To catch at block iterator exceptions that can happen in the void or at the very top of blocks
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: block iterator error");
                    ex.printStackTrace();
                }
            }
            // Handle Shulker Boxes
            if (e.getClickedBlock().getType().toString().contains("SHULKER_BOX")) {
                if (island == null) {
                    if (!Settings.defaultWorldSettings.get(SettingsFlag.OPEN_CHESTS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                } else if (!island.getFlag(SettingsFlag.OPEN_CHESTS)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
                return;
            }
            // Handle fireworks
            if (e.getMaterial() != null && e.getMaterial().equals(Material.FIREWORK)) {
                if (island == null) {
                    if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                } else if (!island.getFlag(SettingsFlag.PLACE_BLOCKS)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
                return;
            }
            if (DEBUG)
                plugin.getLogger().info("DEBUG: clicked block = " + e.getClickedBlock().getType());
            switch (e.getClickedBlock().getType()) {
                case WOODEN_DOOR:
                case SPRUCE_DOOR:
                case ACACIA_DOOR:
                case DARK_OAK_DOOR:
                case BIRCH_DOOR:
                case JUNGLE_DOOR:
                case TRAP_DOOR:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.DOOR)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.DOOR)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case FENCE_GATE:
                case SPRUCE_FENCE_GATE:
                case ACACIA_FENCE_GATE:
                case DARK_OAK_FENCE_GATE:
                case BIRCH_FENCE_GATE:
                case JUNGLE_FENCE_GATE:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.GATE)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.GATE)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ENDER_CHEST:
                    break;
                case CHEST:
                case TRAPPED_CHEST:
                case DISPENSER:
                case DROPPER:
                case HOPPER:
                case HOPPER_MINECART:
                case STORAGE_MINECART:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.OPEN_CHESTS)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.OPEN_CHESTS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case SOIL:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.CROP_TRAMPLE)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.CROP_TRAMPLE)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case BREWING_STAND:
                case CAULDRON:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.BREWING)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.BREWING)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case DIODE:
                case DIODE_BLOCK_OFF:
                case DIODE_BLOCK_ON:
                case REDSTONE_COMPARATOR_ON:
                case REDSTONE_COMPARATOR_OFF:
                case DAYLIGHT_DETECTOR:
                case DAYLIGHT_DETECTOR_INVERTED:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.REDSTONE)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.REDSTONE)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ENCHANTMENT_TABLE:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.ENCHANTING)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.ENCHANTING)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case FURNACE:
                case BURNING_FURNACE:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.FURNACE)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.FURNACE)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ICE:
                    break;
                case ITEM_FRAME:
                    break;
                case JUKEBOX:
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Jukebox");
                case NOTE_BLOCK:
                    if (island == null) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: Jukebox island = null");
                        if (Settings.defaultWorldSettings.get(SettingsFlag.MUSIC)) {
                            return;
                        } else {
                            if (DEBUG)
                                plugin.getLogger().info("DEBUG: Jukebox not allowed");
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.MUSIC)) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: Jukebox not allowed");
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case PACKED_ICE:
                    break;
                case STONE_BUTTON:
                case WOOD_BUTTON:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.BUTTON)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.LEVER)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case LEVER:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.LEVER)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.LEVER)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case TNT:
                    break;
                case WORKBENCH:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.CRAFTING)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.CRAFTING)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case ANVIL:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.ANVIL)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.ANVIL)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case RAILS:
                case POWERED_RAIL:
                case DETECTOR_RAIL:
                case ACTIVATOR_RAIL:
                    // If they are not on an island, it's protected
                    if (island == null) {
                        if (!Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                        }
                        return;
                    }
                    if (!island.getFlag(SettingsFlag.PLACE_BLOCKS)) {
                        if (e.getMaterial() == Material.MINECART || e.getMaterial() == Material.STORAGE_MINECART || e.getMaterial() == Material.HOPPER_MINECART
                                || e.getMaterial() == Material.EXPLOSIVE_MINECART || e.getMaterial() == Material.POWERED_MINECART) {
                            e.setCancelled(true);
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.getPlayer().updateInventory();
                            return;
                        }
                    }
                case BEACON:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.BEACON)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.BEACON)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case CAKE_BLOCK:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.BREAK_BLOCKS)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.getPlayer().setFoodLevel(e.getPlayer().getFoodLevel() - 2);
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.BREAK_BLOCKS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.getPlayer().setFoodLevel(e.getPlayer().getFoodLevel() - 2);
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case DRAGON_EGG:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.BREAK_BLOCKS)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.BREAK_BLOCKS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case MOB_SPAWNER:
                    if (island == null) {
                        if (Settings.defaultWorldSettings.get(SettingsFlag.BREAK_BLOCKS)) {
                            return;
                        } else {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    if (!island.getFlag(SettingsFlag.BREAK_BLOCKS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                    break;
                case BED_BLOCK:
                    if (e.getPlayer().getWorld().getEnvironment().equals(Environment.NETHER)) {
                        // Prevent explosions
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                default:
                    break;
            }
        }
        // Check for disallowed in-hand items
        if (DEBUG) {
            plugin.getLogger().info("Material = " + e.getMaterial());
            plugin.getLogger().info("in hand = " + Util.getPlayerInHandItems(e.getPlayer()));
        }
        if (e.getMaterial() != null) {
            // This check protects against an exploit in 1.7.9 against cactus
            // and sugar cane
            if (e.getMaterial() == Material.WOOD_DOOR || e.getMaterial() == Material.CHEST
                    || e.getMaterial() == Material.TRAPPED_CHEST || e.getMaterial() == Material.IRON_DOOR) {
                if ((island == null && Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS))
                        || (island !=null && !island.getFlag(SettingsFlag.PLACE_BLOCKS))) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                    e.getPlayer().updateInventory();
                }
            } else if (e.getMaterial().name().contains("BOAT") && (e.getClickedBlock() != null && !e.getClickedBlock().isLiquid())) {
                // Trying to put a boat on non-liquid
                if ((island == null && Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS))
                        || (island !=null && !island.getFlag(SettingsFlag.PLACE_BLOCKS))) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
            } else if (e.getMaterial().equals(Material.ENDER_PEARL)) {
                if ((island == null && Settings.defaultWorldSettings.get(SettingsFlag.THROW_ENDERPEARLS))
                        || (island !=null && !island.getFlag(SettingsFlag.THROW_ENDERPEARLS))) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
            } else if (e.getMaterial().equals(Material.FLINT_AND_STEEL)) {
                plugin.getLogger().info("DEBUG: flint & steel");
                if (e.getClickedBlock() != null) {
                    if (e.getMaterial().equals(Material.OBSIDIAN)) {
                        plugin.getLogger().info("DEBUG: flint & steel on obsidian");
                        //return;
                    }
                    if (!actionAllowed(e.getPlayer(), e.getClickedBlock().getLocation(), SettingsFlag.FIRE)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                }
            } else if (e.getMaterial().equals(Material.MONSTER_EGG)) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: allowMonsterEggs = " + island.getFlag(SettingsFlag.SPAWN_EGGS));
                if (!actionAllowed(e.getPlayer(),e.getClickedBlock().getLocation(),SettingsFlag.SPAWN_EGGS)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                }
            } else if (e.getMaterial().equals(Material.POTION) && e.getItem().getDurability() != 0) {
                // Potion
                // plugin.getLogger().info("DEBUG: potion");
                try {
                    Potion p = Potion.fromItemStack(e.getItem());
                    if (p.isSplash()) {
                        // Splash potions are allowed only if PVP is allowed
                        boolean inNether = false;
                        if (e.getPlayer().getWorld().equals(IslandWorld.getNetherWorld())) {
                            inNether = true;
                        }
                        // Check PVP
                        if (island == null) {
                            if ((inNether && Settings.defaultWorldSettings.get(SettingsFlag.PVP_NETHER)
                                    || (!inNether && Settings.defaultWorldSettings.get(SettingsFlag.PVP_OVERWORLD)))) {
                                return;
                            }
                        } else {
                            if ((inNether && island.getFlag(SettingsFlag.PVP_NETHER) || (!inNether && island.getFlag(SettingsFlag.PVP_OVERWORLD)))) {
                                return;
                            }
                        }
                        // Not allowed
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                } catch (Exception ex) {
                }
            }
            // Everything else is okay
        }
    }

    /**
     * Prevents crafting of EnderChest unless the player has permission
     *
     * @param event
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onCraft(CraftItemEvent event) {
        if (DEBUG) {
            plugin.getLogger().info(event.getEventName());
        }
        Player player = (Player) event.getWhoClicked();
        if (Util.inWorld(player) || player.getWorld().equals(IslandWorld.getNetherWorld())) {
            if (event.getRecipe().getResult().getType() == Material.ENDER_CHEST) {
                if (!(player.hasPermission(Settings.PERMPREFIX + "craft.enderchest"))) {
                    Util.sendMessage(player, plugin.getLocale(player.getUniqueId()).get("general.errors.no-permission"));
                    event.setCancelled(true);
                }
            }
        }
    }

    /**
     * Prevents usage of an Ender Chest
     *
     * @param event
     */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnderChestEvent(PlayerInteractEvent event) {
        if (DEBUG) {
            plugin.getLogger().info("Ender chest " + event.getEventName());
        }
        Player player = (Player) event.getPlayer();
        if (Util.inWorld(player) || player.getWorld().equals(IslandWorld.getNetherWorld())) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (event.getClickedBlock().getType() == Material.ENDER_CHEST) {
                    if (!(event.getPlayer().hasPermission(Settings.PERMPREFIX + "craft.enderchest"))) {
                        Util.sendMessage(player, plugin.getLocale(player.getUniqueId()).get("general.errors.no-permission"));
                        event.setCancelled(true);
                    }
                }
            }
        }
    }


    /**
     * Handles hitting minecarts or feeding animals
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerHitEntity(PlayerInteractEntityEvent e) {
        Player p = e.getPlayer();
        if (DEBUG) {
            plugin.getLogger().info("Hit entity event " + e.getEventName());
        }
        if (!Util.inWorld(p)) {
            return;
        }
        if (p.isOp() || VaultHelper.hasPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
            // You can do anything if you are Op of have the bypass
            return;
        }
        // Leashes are dealt with elsewhere
        if (Util.playerIsHolding(p, Material.LEASH)) {
            return;
        }
        Island island = plugin.getIslands().getProtectedIslandAt(e.getPlayer().getLocation());
        if (!plugin.getIslands().playerIsOnIsland(e.getPlayer())) {
            // Not on island
            // Minecarts and other storage entities
            //plugin.getLogger().info("DEBUG: " + e.getRightClicked().getType().toString());
            //plugin.getLogger().info("DEBUG: " + p.getItemInHand());
            // Handle village trading
            if (e.getRightClicked() != null && e.getRightClicked().getType().equals(EntityType.VILLAGER)) {
                if (island != null) {
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: island is not null");
                        plugin.getLogger().info("DEBUG: island is not spawn");
                        plugin.getLogger().info("DEBUG: villager trading is " + island.getFlag(SettingsFlag.VILLAGER_TRADING));
                    }
                    if ((!island.getFlag(SettingsFlag.VILLAGER_TRADING) && !island.getMembers().contains(p.getUniqueId()))) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            // Handle name tags and dyes
            if (Util.playerIsHolding(p, Material.NAME_TAG) || Util.playerIsHolding(p, Material.INK_SACK)) {
                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                e.setCancelled(true);
                e.getPlayer().updateInventory();
                return;
            }
            // Handle cookies (to animals)
            if (Util.playerIsHolding(p, Material.COOKIE) && e.getRightClicked() instanceof Animals) {
                if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.HURT_ANIMALS)) {
                    Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                    return;
                }
                if (island != null) {
                    if ((!island.getFlag(SettingsFlag.HURT_ANIMALS) && !island.getMembers().contains(p.getUniqueId()))) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            // Handle breeding
            if (e.getRightClicked() instanceof Animals) {
                for (ItemStack item : Util.getPlayerInHandItems(p)) {
                    Material type = item.getType();
                    if (type == Material.EGG || type == Material.WHEAT || type == Material.CARROT_ITEM || type == Material.SEEDS) {
                        if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.BREEDING)) {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                            return;
                        }
                        if (island != null) {
                            if ((!island.getFlag(SettingsFlag.BREEDING) && !island.getMembers().contains(p.getUniqueId()))) {
                                Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                                e.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }
            switch (e.getRightClicked().getType()) {
                case CREEPER:
                    // This seems to be called when the player is in Creative mode...
                    if (!Settings.allowCreeperGriefing) {
                        for (ItemStack item : Util.getPlayerInHandItems(e.getPlayer())) {
                            if (item != null && item.getType().equals(Material.FLINT_AND_STEEL)) {
                                if (!island.getMembers().contains(e.getPlayer().getUniqueId())) {
                                    // Visitor
                                    litCreeper.add(e.getRightClicked().getUniqueId());
                                    if (DEBUG) {
                                        plugin.getLogger().info("DEBUG: visitor lit creeper");
                                    }
                                }
                            }
                        }
                    }
                    break;
                case LLAMA:
                case SKELETON_HORSE:
                case ZOMBIE_HORSE:
                case HORSE:
                    //plugin.getLogger().info("Horse riding");
                    if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.MOUNT_RIDING)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                    if (island != null && !island.getFlag(SettingsFlag.MOUNT_RIDING)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                    break;
                case ITEM_FRAME:
                    // This is to place items in an item frame
                    if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                    if (island != null) {
                        if (!island.getFlag(SettingsFlag.PLACE_BLOCKS)) {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                        }
                    }
                    break;
                case MINECART_CHEST:
                case MINECART_FURNACE:
                case MINECART_HOPPER:
                    //plugin.getLogger().info("Minecarts");
                    if (island == null && !Settings.defaultWorldSettings.get(SettingsFlag.OPEN_CHESTS)) {
                        Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                        e.setCancelled(true);
                    }
                    if (island != null) {
                        if (!island.getFlag(SettingsFlag.OPEN_CHESTS)) {
                            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                            e.setCancelled(true);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Prevents fire spread
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!Util.inWorld(e.getBlock())) {
            //plugin.getLogger().info("DEBUG: Not in world");
            return;
        }
        if (actionAllowed(e.getBlock().getLocation(), SettingsFlag.FIRE_SPREAD)) {
            return;
        }
        e.setCancelled(true);
    }

    /**
     * Prevent fire spread
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info(e.getSource().getType().toString());
        }
        if (e.getSource().getType() == Material.FIRE) {
            if (!Util.inWorld(e.getBlock())) {
                //plugin.getLogger().info("DEBUG: Not in world");
                return;
            }
            if (actionAllowed(e.getBlock().getLocation(), SettingsFlag.FIRE_SPREAD)) {
                return;
            }
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(final BlockIgniteEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info(e.getCause().name());
        }
        if (!Util.inWorld(e.getBlock())) {
            //plugin.getLogger().info("DEBUG: Not in world");
            return;
        }
        // Check if this is a portal lighting
        if (e.getBlock() != null && e.getBlock().getType().equals(Material.OBSIDIAN)) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: portal lighting");
            return;
        }
        if (e.getCause() != null) {
            if (DEBUG)
                plugin.getLogger().info("DEBUG: ignite cause = " + e.getCause());
            switch (e.getCause()) {
                case ENDER_CRYSTAL:
                case EXPLOSION:
                case FIREBALL:
                case LIGHTNING:
                    if (!actionAllowed(e.getBlock().getLocation(), SettingsFlag.FIRE)) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: canceling fire");
                        e.setCancelled(true);
                    }
                    break;
                case FLINT_AND_STEEL:
                    Set<Material> transparent = new HashSet<>();
                    transparent.add(Material.AIR);
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: block = " + e.getBlock());
                        //plugin.getLogger().info("DEBUG: target block = " + e.getPlayer().getTargetBlock(transparent, 10));
                    }
                    // Check if this is allowed
                    if (e.getPlayer() != null && (e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypass"))) {
                        return;
                    }
                    if (!actionAllowed(e.getBlock().getLocation(), SettingsFlag.FIRE)) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: canceling fire");
                        // If this was not a player, just stop it
                        if (e.getPlayer() == null) {
                            e.setCancelled(true);
                            break;
                        }
                        // Get target block
                        Block targetBlock = e.getPlayer().getTargetBlock(transparent, 10);
                        if (targetBlock.getType().equals(Material.OBSIDIAN)) {
                            final MaterialData md = new MaterialData(e.getBlock().getType(), e.getBlock().getData());
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                if (e.getBlock().getType().equals(Material.FIRE)) {
                                    e.getBlock().setType(md.getItemType());
                                    e.getBlock().setData(md.getData());
                                }
                            });
                        } else {
                            e.setCancelled(true);
                        }
                    }
                    break;

                case LAVA:
                case SPREAD:
                    // Check if this is a portal lighting
                    if (e.getBlock() != null && e.getBlock().getType().equals(Material.OBSIDIAN)) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: obsidian lighting");
                        return;
                    }
                    if (!actionAllowed(e.getBlock().getLocation(), SettingsFlag.FIRE_SPREAD)) {
                        if (DEBUG)
                            plugin.getLogger().info("DEBUG: canceling fire spread");
                        e.setCancelled(true);
                    }
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * Pressure plates
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlateStep(PlayerInteractEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("pressure plate = " + e.getEventName());
            plugin.getLogger().info("action = " + e.getAction());
        }
        if (!Util.inWorld(e.getPlayer()) || !e.getAction().equals(Action.PHYSICAL)
                || e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                || plugin.getIslands().playerIsOnIsland(e.getPlayer())) {
            //plugin.getLogger().info("DEBUG: Not in world");
            return;
        }
        // Check island
        Island island = plugin.getIslands().getProtectedIslandAt(e.getPlayer().getLocation());
        if ((island == null && Settings.defaultWorldSettings.get(SettingsFlag.PRESSURE_PLATE))) {
            return;
        }
        if (island != null && island.getFlag(SettingsFlag.PRESSURE_PLATE)) {
            return;
        }
        // Block action
        UUID playerUUID = e.getPlayer().getUniqueId();
        if (!onPlate.containsKey(playerUUID)) {
            Util.sendMessage(e.getPlayer(), plugin.getLocale(playerUUID).get("island.protected"));
            Vector v = e.getPlayer().getLocation().toVector();
            onPlate.put(playerUUID, new Vector(v.getBlockX(), v.getBlockY(), v.getBlockZ()));
        }
        e.setCancelled(true);
    }

    /**
     * Removes the player from the plate map
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onStepOffPlate(PlayerMoveEvent e) {
        if (!onPlate.containsKey(e.getPlayer().getUniqueId())) {
            return;
        }
        Vector v = e.getPlayer().getLocation().toVector();
        if (!(new Vector(v.getBlockX(), v.getBlockY(), v.getBlockZ())).equals(onPlate.get(e.getPlayer().getUniqueId()))) {
            onPlate.remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        Location pistonLoc = e.getBlock().getLocation();
        if (Settings.allowPistonPush || !Util.inWorld(pistonLoc)) {
            //plugin.getLogger().info("DEBUG: Not in world");
            return;
        }
        Island island = plugin.getIslands().getProtectedIslandAt(pistonLoc);
        if (island == null || !island.onIsland(pistonLoc)) {
            //plugin.getLogger().info("DEBUG: Not on is island protection zone");
            return;
        }
        // We need to check where the blocks are going to go, not where they are
        for (Block b : e.getBlocks()) {
            if (!island.onIsland(b.getRelative(e.getDirection()).getLocation())) {
                //plugin.getLogger().info("DEBUG: Block is outside protected area");
                e.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Handle visitor chicken egg throwing
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEggThrow(PlayerEggThrowEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("egg throwing = " + e.getEventName());
        }
        if (!Util.inWorld(e.getPlayer()) || e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                || plugin.getIslands().playerIsOnIsland(e.getPlayer()) || plugin.getIslands().isAtSpawn(e.getPlayer().getLocation())) {
            return;
        }
        // Check island
        Island island = plugin.getIslands().getProtectedIslandAt(e.getPlayer().getLocation());
        if (island == null) {
            return;
        }
        if (!island.getFlag(SettingsFlag.THROW_EGGS)) {
            e.setHatching(false);
            Util.sendMessage(e.getPlayer(), plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
            //e.getPlayer().updateInventory();
        }
    }

    /**
     * Prevents trees from growing outside of the protected area.
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTreeGrow(final StructureGrowEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // Check world
        if (!Util.inWorld(e.getLocation())) {
            return;
        }
        // Check if this is on an island
        Island island = plugin.getIslands().getIslandAt(e.getLocation());
        if (island == null || island.isSpawn()) {
            return;
        }
        Iterator<BlockState> it = e.getBlocks().iterator();
        while (it.hasNext()) {
            BlockState b = it.next();
            if (b.getType() == Material.LOG || b.getType() == Material.LOG_2
                    || b.getType() == Material.LEAVES || b.getType() == Material.LEAVES_2) {
                if (!island.onIsland(b.getLocation())) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Trap TNT being primed by flaming arrows
     * @param e
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTNTPrimed(final EntityChangeBlockEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("DEBUG: block = " + e.getBlock().getType());
            plugin.getLogger().info("DEBUG: entity = " + e.getEntityType());
            plugin.getLogger().info("DEBUG: material changing to " + e.getTo());
        }
        if (actionAllowed(e.getEntity().getLocation(), SettingsFlag.FIRE)) {
            return;
        }
        if (e.getBlock() == null) {
            return;
        }
        // Check for TNT
        if (!e.getBlock().getType().equals(Material.TNT)) {
            //plugin.getLogger().info("DEBUG: not tnt");
            return;
        }
        // Check world
        if (!Util.inWorld(e.getBlock())) {
            return;
        }
        // Check if this is on an island
        Island island = plugin.getIslands().getIslandAt(e.getBlock().getLocation());
        if (island == null || island.isSpawn()) {
            return;
        }
        // Stop TNT from being damaged if it is being caused by a visitor with a flaming arrow
        if (e.getEntity() instanceof Projectile) {
            //plugin.getLogger().info("DEBUG: projectile");
            Projectile projectile = (Projectile) e.getEntity();
            // Find out who fired it
            if (projectile.getShooter() instanceof Player) {
                //plugin.getLogger().info("DEBUG: player shot arrow. Fire ticks = " + projectile.getFireTicks());
                if (projectile.getFireTicks() > 0) {
                    //plugin.getLogger().info("DEBUG: arrow on fire");
                    Player shooter = (Player)projectile.getShooter();
                    if (!plugin.getIslands().locationIsAtHome(shooter, true, e.getBlock().getLocation())) {
                        //plugin.getLogger().info("DEBUG: shooter is not at home");
                        // Only say it once a second
                        // Debounce event (it can be called twice for the same action)
                        if (!tntBlocks.contains(e.getBlock().getLocation())) {
                            Util.sendMessage(shooter, plugin.getLocale(shooter.getUniqueId()).get("island.protected"));
                            tntBlocks.add(e.getBlock().getLocation());
                            plugin.getServer().getScheduler().runTaskLater(plugin, () -> tntBlocks.remove(e.getBlock().getLocation()), 20L);
                        }
                        // Remove the arrow
                        projectile.remove();
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.LOW)
    public void onEvent(BlockPistonExtendEvent event) {
        if (!Settings.allowTNTPushing) {
            // Check world
            if (!Util.inWorld(event.getBlock())) return;

            if (event.getBlocks().stream().anyMatch(it -> it.getType() == Material.TNT))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOW)
    public void onEvent(BlockPistonRetractEvent event) {
        if (!Settings.allowTNTPushing) {
            // Check world
            if (!Util.inWorld(event.getBlock())) return;

            if (event.getBlocks().stream().anyMatch(it -> it.getType() == Material.TNT))
                event.setCancelled(true);
        }
    }

    /**
     * Checks for splash damage. If there is any to any affected entity and it's not allowed, it won't work on any of them.
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onSplashPotionSplash(final PotionSplashEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
            plugin.getLogger().info("splash entity = " + e.getEntity());
            plugin.getLogger().info("splash entity type = " + e.getEntityType());
            plugin.getLogger().info("splash affected entities = " + e.getAffectedEntities());
            //plugin.getLogger().info("splash hit entity = " + e.getHitEntity());
        }
        if (!Util.inWorld(e.getEntity().getLocation())) {
            return;
        }
        // Try to get the shooter
        Projectile projectile = (Projectile) e.getEntity();
        if (DEBUG)
            plugin.getLogger().info("splash shooter = " + projectile.getShooter());
        if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
            Player attacker = (Player)projectile.getShooter();
            // Run through all the affected entities
            for (LivingEntity entity: e.getAffectedEntities()) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: affected splash entity = " + entity);
                // Self damage
                if (attacker.equals(entity)) {
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Self damage from splash potion!");
                    continue;
                }
                Island island = plugin.getIslands().getIslandAt(entity.getLocation());
                boolean inNether = false;
                if (entity.getWorld().equals(IslandWorld.getNetherWorld())) {
                    inNether = true;
                }
                // Monsters being hurt
                if (entity instanceof Monster || entity instanceof Slime || entity instanceof Squid) {
                    // Normal island check
                    if (island != null && island.getMembers().contains(attacker.getUniqueId())) {
                        // Members always allowed
                        continue;
                    }
                    if (actionAllowed(attacker, entity.getLocation(), SettingsFlag.HURT_MONSTERS)) {
                        continue;
                    }
                    // Not allowed
                    Util.sendMessage(attacker, plugin.getLocale(attacker.getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                    return;
                }

                // Mobs being hurt
                if (entity instanceof Animals || entity instanceof IronGolem || entity instanceof Snowman
                        || entity instanceof Villager) {
                    if (island != null && (island.getFlag(SettingsFlag.HURT_ANIMALS) || island.getMembers().contains(attacker.getUniqueId()))) {
                        continue;
                    }
                    if (DEBUG)
                        plugin.getLogger().info("DEBUG: Mobs not allowed to be hurt. Blocking");
                    Util.sendMessage(attacker, plugin.getLocale(attacker.getUniqueId()).get("island.protected"));
                    e.setCancelled(true);
                    return;
                }

                // Establish whether PVP is allowed or not.
                boolean pvp = false;
                if ((inNether && island != null && island.getFlag(SettingsFlag.PVP_NETHER) || (!inNether && island != null && island.getFlag(SettingsFlag.PVP_OVERWORLD)))) {
                    if (DEBUG) plugin.getLogger().info("DEBUG: PVP allowed");
                    pvp = true;
                }

                // Players being hurt PvP
                if (entity instanceof Player) {
                    if (!pvp) {
                        if (DEBUG) plugin.getLogger().info("DEBUG: PVP not allowed");
                        Util.sendMessage(attacker, plugin.getLocale(attacker.getUniqueId()).get("targetInPVPArea"));
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}
