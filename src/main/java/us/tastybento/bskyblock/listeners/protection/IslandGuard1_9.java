package us.tastybento.bskyblock.listeners.protection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands - handles newer events that may not
 *         exist in older servers
 */
public class IslandGuard1_9 implements Listener {
    private final BSkyBlock plugin;
    private final static boolean DEBUG = false;
    private HashMap<Integer, UUID> thrownPotions;

    public IslandGuard1_9(final BSkyBlock plugin) {
        this.plugin = plugin;
        this.thrownPotions = new HashMap<>();
    }

    /**
     * Handles Frost Walking on visitor's islands
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onBlockForm(EntityBlockFormEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.9 " +e.getEventName());
        }
        if (e.getEntity() instanceof Player && e.getNewState().getType().equals(Material.FROSTED_ICE)) {
            Player player= (Player) e.getEntity();
            if (!Util.inWorld(player)) {
                return;
            }
            if (player.isOp()) {
                return;
            }
            // This permission bypasses protection
            if (VaultHelper.hasPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            // Check island
            Island island = plugin.getIslands().getIslandAt(player.getLocation());
            if (island == null && Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                return;
            }
            if (island !=null) {
                if (island.getMembers().contains(player.getUniqueId()) || island.getFlag(SettingsFlag.PLACE_BLOCKS)) {
                    return;
                }
            }
            // Silently cancel the event
            e.setCancelled(true);
        }
    }


    /**
     * Handle interaction with end crystals 1.9
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onHitEndCrystal(final PlayerInteractAtEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.9 " +e.getEventName());
        }
        if (!Util.inWorld(e.getPlayer())) {
            return;
        }
        if (e.getPlayer().isOp()) {
            return;
        }
        // This permission bypasses protection
        if (VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")) {
            return;
        }
        if (e.getRightClicked() != null && e.getRightClicked().getType().equals(EntityType.ENDER_CRYSTAL)) {
            // Check island
            Island island = plugin.getIslands().getIslandAt(e.getRightClicked().getLocation());
            if (island == null && Settings.defaultWorldSettings.get(SettingsFlag.BREAK_BLOCKS)) {
                return;
            }
            if (island !=null) {
                if (island.getMembers().contains(e.getPlayer().getUniqueId()) || island.getFlag(SettingsFlag.BREAK_BLOCKS)) {
                    return;
                }
            }
            e.setCancelled(true);
            Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
        }
    }

    // End crystal
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    void placeEndCrystalEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (DEBUG) {
            plugin.getLogger().info("1.9 " +"End crystal place " + e.getEventName());
        }
        if (!Util.inWorld(p)) {
            return;
        }
        if (p.isOp() || VaultHelper.hasPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
            // You can do anything if you are Op
            return;
        }

        // Check if they are holding armor stand
        for (ItemStack inHand : Util.getPlayerInHandItems(e.getPlayer())) {
            if (inHand.getType().equals(Material.END_CRYSTAL)) {
                // Check island
                Island island = plugin.getIslands().getIslandAt(e.getPlayer().getLocation());
                if (island == null && Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    return;
                }
                if (island !=null && (island.getMembers().contains(p.getUniqueId()) || island.getFlag(SettingsFlag.PLACE_BLOCKS))) {
                    //plugin.getLogger().info("1.9 " +"DEBUG: armor stand place check");
                    if (Settings.limitedBlocks.containsKey("END_CRYSTAL") && Settings.limitedBlocks.get("END_CRYSTAL") > -1) {
                        //plugin.getLogger().info("1.9 " +"DEBUG: count armor stands");
                        int count = island.getTileEntityCount(Material.END_CRYSTAL,e.getPlayer().getWorld());
                        //plugin.getLogger().info("1.9 " +"DEBUG: count is " + count + " limit is " + Settings.limitedBlocks.get("ARMOR_STAND"));
                        if (Settings.limitedBlocks.get("END_CRYSTAL") <= count) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + (plugin.getLocale(e.getPlayer().getUniqueId()).get("moblimits.entity").replace("[entity]",
                                    Util.prettifyText(Material.END_CRYSTAL.toString()))).replace("[number]", String.valueOf(Settings.limitedBlocks.get("END_CRYSTAL"))));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    return;
                }
                // plugin.getLogger().info("1.9 " +"DEBUG: stand place cancelled");
                e.setCancelled(true);
                Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                e.getPlayer().updateInventory();
            }
        }

    }

    /**
     * Handle end crystal damage by visitors
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void EndCrystalDamage(EntityDamageByEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.9 " +"IslandGuard 1_9 " + e.getEventName());
            plugin.getLogger().info("1.9 " +"Entity is " + e.getEntityType());
        }
        if (e.getEntity() == null || !Util.inWorld(e.getEntity())) {
            return;
        }
        if (!(e.getEntity() instanceof EnderCrystal)) {
            if (DEBUG) {
                plugin.getLogger().info("1.9 Entity is not End crystal it is " + e.getEntityType());
            }
            return;
        }
        if (DEBUG) {
            plugin.getLogger().info("1.9 Damager is " + e.getDamager());
        }
        Player p = null;
        if (e.getDamager() instanceof Player) {
            p = (Player) e.getDamager();
            if (DEBUG) {
                plugin.getLogger().info("1.9 Damager is a player");
            }
        } else if (e.getDamager() instanceof Projectile) {
            // Get the shooter
            Projectile projectile = (Projectile)e.getDamager();
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player) {
                p = (Player)shooter;
            }
            if (DEBUG) {
                plugin.getLogger().info("1.9 " +"Damager is a projectile shot by " + p.getName());
            }
        }
        if (p != null) {
            if (p.isOp() || VaultHelper.hasPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
                if (DEBUG) {
                    plugin.getLogger().info("1.9 " +"Bypassing protection");
                }
                return;
            }
            // Check if on island
            if (plugin.getIslands().playerIsOnIsland(p)) {
                if (DEBUG) {
                    plugin.getLogger().info("1.9 " +"Player is on their own island");
                }
                return;
            }
            // Check island
            Island island = plugin.getIslands().getIslandAt(e.getEntity().getLocation());
            if (island == null && Settings.defaultWorldSettings.get(SettingsFlag.BREAK_BLOCKS)) {
                return;
            }
            if (island != null && island.getFlag(SettingsFlag.BREAK_BLOCKS)) {
                if (DEBUG) {
                    plugin.getLogger().info("1.9 " +"Visitor is allowed to break blocks");
                }
                return;
            }
            Util.sendMessage(p, ChatColor.RED + plugin.getLocale(p.getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }

    }

    /**
     * Handles end crystal explosions
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onExplosion(final EntityExplodeEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.9 " +e.getEventName());
            plugin.getLogger().info("1.9 " +"Entity exploding is " + e.getEntity());
        }
        if (e.getEntity() == null || !e.getEntityType().equals(EntityType.ENDER_CRYSTAL)) {
            if (DEBUG) {
                plugin.getLogger().info("1.9 " +"Entity is not an END CRYSTAL");
            }
            return;
        }

        if (!Util.inWorld(e.getLocation())) {
            return;
        }
        // General settings irrespective of whether this is allowed or not
        if (!Settings.allowTNTDamage) {
            plugin.getLogger().info("1.9 " +"TNT block damage prevented");
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
        // prevent at spawn
        if (plugin.getIslands().isAtSpawn(e.getLocation())) {
            e.blockList().clear();
            e.setCancelled(true);
        }

    }

    /**
     * Handle blocks that need special treatment
     * Tilling of coarse dirt into dirt using off-hand (regular hand is in 1.8)
     * Usually prevented because it could lead to an endless supply of dirt with gravel
     *
     * @param e
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.9 " + e.getEventName());
        }
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        if (!Util.inWorld(e.getPlayer())) {
            return;
        }
        if (e.getPlayer().isOp()) {
            return;
        }
        // This permission bypasses protection
        if (VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "craft.dirt")) {
            return;
        }
        // Prevents tilling of coarse dirt into dirt
        ItemStack inHand = e.getPlayer().getInventory().getItemInOffHand();
        if (inHand.getType() == Material.WOOD_HOE || inHand.getType() == Material.IRON_HOE || inHand.getType() == Material.GOLD_HOE
                || inHand.getType() == Material.DIAMOND_HOE || inHand.getType() == Material.STONE_HOE) {
            // plugin.getLogger().info("1.8 " + "DEBUG: hoe in hand");
            Block block = e.getClickedBlock();
            // plugin.getLogger().info("1.8 " + "DEBUG: block is " + block.getType() +
            // ":" + block.getData());
            // Check if coarse dirt
            if (block.getType() == Material.DIRT && block.getData() == (byte) 1) {
                // plugin.getLogger().info("1.8 " + "DEBUG: hitting coarse dirt!");
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionSplash(final LingeringPotionSplashEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.9 " + e.getEventName());
            plugin.getLogger().info("1.9 entity = " + e.getEntity());
            plugin.getLogger().info("1.9 entity type = " + e.getEntityType());
            plugin.getLogger().info("1.9 radius = " + e.getAreaEffectCloud().getRadius());
            plugin.getLogger().info("1.9 id = " + e.getAreaEffectCloud().getEntityId());
            plugin.getLogger().info("1.9 hit entity = " + e.getHitEntity());
        }
        if (!Util.inWorld(e.getEntity().getLocation())) {
            return;
        }
        // Try to get the shooter
        Projectile projectile = e.getEntity();
        plugin.getLogger().info("shooter = " + projectile.getShooter());
        if (projectile.getShooter() != null && projectile.getShooter() instanceof Player) {
            UUID uuid = ((Player)projectile.getShooter()).getUniqueId();
            // Store it and remove it when the effect is gone
            thrownPotions.put(e.getAreaEffectCloud().getEntityId(), uuid);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> thrownPotions.remove(e.getAreaEffectCloud().getEntityId()), e.getAreaEffectCloud().getDuration());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onLingeringPotionDamage(final EntityDamageByEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.9 lingering potion damage " + e.getEventName());
            plugin.getLogger().info("1.9 lingering potion entity = " + e.getEntity());
            plugin.getLogger().info("1.9 lingering potion entity type = " + e.getEntityType());
            plugin.getLogger().info("1.9 lingering potion cause = " + e.getCause());
            plugin.getLogger().info("1.9 lingering potion damager = " + e.getDamager());
        }
        if (!Util.inWorld(e.getEntity().getLocation())) {
            return;
        }
        if (e.getEntity() == null || e.getEntity().getUniqueId() == null) {
            return;
        }
        if (e.getCause().equals(DamageCause.ENTITY_ATTACK) && thrownPotions.containsKey(e.getDamager().getEntityId())) {
            UUID attacker = thrownPotions.get(e.getDamager().getEntityId());
            // Self damage
            if (attacker.equals(e.getEntity().getUniqueId())) {
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Self damage from lingering potion!");
                return;
            }
            Island island = plugin.getIslands().getIslandAt(e.getEntity().getLocation());
            boolean inNether = false;
            if (e.getEntity().getWorld().equals(IslandWorld.getNetherWorld())) {
                inNether = true;
            }
            // Monsters being hurt
            if (e.getEntity() instanceof Monster || e.getEntity() instanceof Slime || e.getEntity() instanceof Squid) {
                // Normal island check
                if (island != null && island.getMembers().contains(attacker)) {
                    // Members always allowed
                    return;
                }
                if (actionAllowed(attacker, e.getEntity().getLocation(), SettingsFlag.HURT_MONSTERS)) {
                    return;
                }
                // Not allowed
                e.setCancelled(true);
                return;
            }

            // Mobs being hurt
            if (e.getEntity() instanceof Animals || e.getEntity() instanceof IronGolem || e.getEntity() instanceof Snowman
                    || e.getEntity() instanceof Villager) {
                if (island != null && (island.getFlag(SettingsFlag.HURT_ANIMALS) || island.getMembers().contains(attacker))) {
                    return;
                }
                if (DEBUG)
                    plugin.getLogger().info("DEBUG: Mobs not allowed to be hurt. Blocking");
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
                    if (DEBUG) plugin.getLogger().info("DEBUG: PVP not allowed");
                    e.setCancelled(true);
                }
            }
        }
    }

    /**
     * Checks if action is allowed for player in location for flag
     * @param uuid
     * @param location
     * @param flag
     * @return true if allowed
     */
    private boolean actionAllowed(UUID uuid, Location location, SettingsFlag flag) {
        Player player = plugin.getServer().getPlayer(uuid);
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
}