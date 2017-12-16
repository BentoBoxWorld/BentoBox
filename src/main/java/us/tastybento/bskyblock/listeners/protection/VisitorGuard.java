package us.tastybento.bskyblock.listeners.protection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Island.SettingsFlag;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * @author tastybento
 *         Provides protection to islands
 */
public class VisitorGuard implements Listener {
    private final BSkyBlock plugin;
    private static final boolean DEBUG = false;

    public VisitorGuard(final BSkyBlock plugin) {
        this.plugin = plugin;
    }

    /*
     * Prevent dropping items if player dies on another island
     * This option helps reduce the down side of dying due to traps, etc.
     * Also handles muting of death messages
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorDeath(final PlayerDeathEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!Util.inWorld(e.getEntity())) {
            return;
        }
        // Mute death messages
        if (Settings.muteDeathMessages) {
            e.setDeathMessage(null);
        }
        // If visitors will keep items and their level on death. This overrides any global settings.
        // If the player is not a visitor then they die and lose everything - sorry :-(
        Island island = plugin.getIslands().getProtectedIslandAt(e.getEntity().getLocation());
        if (island != null && !island.getMembers().contains(e.getEntity().getUniqueId()) && island.getFlag(SettingsFlag.KEEP_INVENTORY)) {
            // They are a visitor
            InventorySave.getInstance().savePlayerInventory(e.getEntity());
            e.getDrops().clear();
            e.setKeepLevel(true);
            e.setDroppedExp(0);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorSpawn(final PlayerRespawnEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        // If the player died on an island and his inventory has been saved, give it him back. This will override any global settings.
        if (InventorySave.isStored(e.getPlayer().getUniqueId())) {
            InventorySave.getInstance().loadPlayerInventory(e.getPlayer());
            InventorySave.getInstance().clearSavedInventory(e.getPlayer());
        }
    }

    /*
     * Prevent item drop by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorDrop(final PlayerDropItemEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (!Util.inWorld(e.getPlayer())) {
            return;
        }
        Island island = plugin.getIslands().getIslandAt(e.getItemDrop().getLocation());
        if ((island != null && island.getFlag(SettingsFlag.ITEM_DROP)) 
                || e.getPlayer().isOp() || VaultHelper.hasPerm(e.getPlayer(), Settings.PERMPREFIX + "mod.bypassprotect")
                || plugin.getIslands().locationIsOnIsland(e.getPlayer(), e.getItemDrop().getLocation())) {
            return;
        }
        e.getPlayer().sendMessage(plugin.getLocale(e.getPlayer()).get("island.protected"));
        e.setCancelled(true);
    }

    /**
     * Prevents visitors from getting damage if invinciblevisitors option is set to TRUE
     * @param e
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVisitorReceiveDamage(EntityDamageEvent e){
        if(!Settings.invincibleVisitor) return;
        if(!(e.getEntity() instanceof Player)) return;

        Player p = (Player) e.getEntity();
        if (!Util.inWorld(p) || plugin.getIslands().locationIsOnIsland(p, p.getLocation())) return;

        if (Settings.invincibleVisitorOptions.contains(e.getCause())) e.setCancelled(true);

        else if(e.getCause().equals(DamageCause.VOID)) {
            if(plugin.getPlayers().hasIsland(p.getUniqueId())) {
                Location safePlace = plugin.getIslands().getSafeHomeLocation(p.getUniqueId(), 1);
                if (safePlace != null) {
                    p.teleport(safePlace);
                    // Set their fall distance to zero otherwise they crash onto their island and die
                    p.setFallDistance(0);
                    e.setCancelled(true);
                    return;
                } 
            }
            // No island, or no safe spot on island
            if (plugin.getIslands().getSpawn() != null) {
                p.teleport(plugin.getIslands().getSpawnPoint());
                // Set their fall distance to zero otherwise they crash onto their island and die
                p.setFallDistance(0);
                e.setCancelled(true);
                return;
            }
            // No island spawn, try regular spawn
            if (!p.performCommand("spawn")) {
                // If this command doesn't work, let them die otherwise they may get trapped in the void forever
                return;
            }
            // Set their fall distance to zero otherwise they crash onto their island and die
            p.setFallDistance(0);
            e.setCancelled(true);
        }
    }

    /*
     * Prevent item pickup by visitors
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onVisitorPickup(final EntityPickupItemEvent e) {
        if (DEBUG) {
            plugin.getLogger().info(e.getEventName());
        }
        if (e.getEntity() instanceof Player) {
            Player player = (Player)e.getEntity();
            if (!Util.inWorld(player)) {
                return;
            }
            Island island = plugin.getIslands().getIslandAt(e.getItem().getLocation());
            if ((island != null && island.getFlag(SettingsFlag.ITEM_PICKUP)) 
                    || player.isOp() || VaultHelper.hasPerm(player, Settings.PERMPREFIX + "mod.bypassprotect")
                    || plugin.getIslands().locationIsOnIsland(player, e.getItem().getLocation())) {
                return;
            }
            e.setCancelled(true);
        }
    }

}
