package us.tastybento.bskyblock.listeners.protection;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands - handles newer events that may not
 *         exist in older servers
 */
public class IslandGuard1_8 implements Listener {
    private final BSkyBlock plugin;
    private final static boolean DEBUG = false;

    public IslandGuard1_8(final BSkyBlock plugin) {
        this.plugin = plugin;

    }

    /**
     * Checks if action is allowed for player in location for flag
     * @param player
     * @param location
     * @param flag
     * @return true if allowed
     */
    private boolean actionAllowed(Player player, Location location, SettingsFlag flag) {
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
     * Handle interaction with armor stands V1.8
     * Note - some armor stand protection is done in IslandGuard.java, e.g. against projectiles.
     *
     * @param e
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerInteract(final PlayerInteractAtEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.8 " + e.getEventName());
        }
        if (!Util.inWorld(e.getPlayer())) {
            return;
        }
        if (e.getRightClicked() != null && e.getRightClicked().getType().equals(EntityType.ARMOR_STAND)) {
            if (actionAllowed(e.getPlayer(), e.getRightClicked().getLocation(), SettingsFlag.ARMOR_STAND)) {
                return;
            }
            e.setCancelled(true);
            Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
        }
    }

    /**
     * Handle V1.8 blocks that need special treatment
     * Tilling of coarse dirt into dirt
     * Usually prevented because it could lead to an endless supply of dirt with gravel
     *
     * @param e
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.8 " + e.getEventName());
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
        ItemStack inHand = e.getPlayer().getItemInHand();
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

    // Armor stand events
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled=true)
    void placeArmorStandEvent(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (DEBUG) {
            plugin.getLogger().info("1.8 " + "Armor stand place " + e.getEventName());
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
            if (inHand.getType().equals(Material.ARMOR_STAND)) {
                // Check island
                Island island = plugin.getIslands().getIslandAt(e.getPlayer().getLocation());
                if (island == null && Settings.defaultWorldSettings.get(SettingsFlag.PLACE_BLOCKS)) {
                    return;
                }
                if (island !=null && (island.getMembers().contains(p.getUniqueId()) || island.getFlag(SettingsFlag.PLACE_BLOCKS))) {
                    //plugin.getLogger().info("1.8 " + "DEBUG: armor stand place check");
                    if (Settings.limitedBlocks.containsKey("ARMOR_STAND") && Settings.limitedBlocks.get("ARMOR_STAND") > -1) {
                        //plugin.getLogger().info("1.8 " + "DEBUG: count armor stands");
                        int count = island.getTileEntityCount(Material.ARMOR_STAND,e.getPlayer().getWorld());
                        //plugin.getLogger().info("1.8 " + "DEBUG: count is " + count + " limit is " + Settings.limitedBlocks.get("ARMOR_STAND"));
                        if (Settings.limitedBlocks.get("ARMOR_STAND") <= count) {
                            Util.sendMessage(e.getPlayer(), ChatColor.RED + (plugin.getLocale(e.getPlayer().getUniqueId()).get("moblimits.entity").replace("[entity]",
                                    Util.prettifyText(Material.ARMOR_STAND.toString()))).replace("[number]", String.valueOf(Settings.limitedBlocks.get("ARMOR_STAND"))));
                            e.setCancelled(true);
                            return;
                        }
                    }
                    return;
                }
                // plugin.getLogger().info("1.8 " + "DEBUG: stand place cancelled");
                e.setCancelled(true);
                Util.sendMessage(e.getPlayer(), ChatColor.RED + plugin.getLocale(e.getPlayer().getUniqueId()).get("island.protected"));
                e.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled=true)
    public void ArmorStandDestroy(EntityDamageByEntityEvent e) {
        if (DEBUG) {
            plugin.getLogger().info("1.8 " + "IslandGuard New " + e.getEventName());
        }
        if (!(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (!Util.inWorld(e.getEntity())) {
            return;
        }
        final LivingEntity livingEntity = (LivingEntity) e.getEntity();
        if (!livingEntity.getType().equals(EntityType.ARMOR_STAND)) {
            return;
        }
        if (e.getDamager() instanceof Player) {
            Player p = (Player) e.getDamager();
            if (p.isOp() || VaultHelper.hasPerm(p, Settings.PERMPREFIX + "mod.bypassprotect")) {
                return;
            }
            // Check if on island
            if (plugin.getIslands().playerIsOnIsland(p)) {
                return;
            }
            // Check island
            Island island = plugin.getIslands().getIslandAt(e.getEntity().getLocation());
            if (island == null && Settings.defaultWorldSettings.get(SettingsFlag.BREAK_BLOCKS)) {
                return;
            }
            if (island != null && island.getFlag(SettingsFlag.BREAK_BLOCKS)) {
                return;
            }
            Util.sendMessage(p, ChatColor.RED + plugin.getLocale(p.getUniqueId()).get("island.protected"));
            e.setCancelled(true);
        }
    }

}