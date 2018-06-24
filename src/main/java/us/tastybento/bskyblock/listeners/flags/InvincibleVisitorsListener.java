/*

 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.Arrays;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.ClickType;

import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;
import us.tastybento.bskyblock.util.teleport.SafeTeleportBuilder;

/**
 * @author tastybento
 *
 */
public class InvincibleVisitorsListener extends AbstractFlagListener implements ClickHandler {

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        String ivPanelName = user.getTranslation("protection.flags.INVINCIBLE_VISITORS.name");
        if (panel.getName().equals(ivPanelName)) {
            // This is a click on the IV panel
            // Slot relates to the enum
            DamageCause c = Arrays.asList(EntityDamageEvent.DamageCause.values()).get(slot);
            if (getPlugin().getSettings().getIvSettings().contains(c.name())) {
                getPlugin().getSettings().getIvSettings().remove(c.name());
            } else {
                getPlugin().getSettings().getIvSettings().add(c.name());
            }
            // Apply change to panel
            panel.getInventory().setItem(slot, getPanelItem(c, user).getItem());
        } else {
            // Open the IV Settings panel
            openPanel(user, ivPanelName);
        }
        return true;
    }

    private void openPanel(User user, String ivPanelName) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel for visitor protection
        PanelBuilder pb = new PanelBuilder();
        pb.user(user).name(ivPanelName);
        // Make panel items
        Arrays.stream(EntityDamageEvent.DamageCause.values()).forEach(c -> pb.item(getPanelItem(c, user)));
        pb.build();

    }
    
    private PanelItem getPanelItem(DamageCause c, User user) {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.name(Util.prettifyText(c.toString()));
        pib.clickHandler(this);
        if (getPlugin().getSettings().getIvSettings().contains(c.name())) {
            pib.icon(Material.GREEN_SHULKER_BOX);
            pib.description(user.getTranslation("protection.panel.flag-item.setting-active"));
        } else {
            pib.icon(Material.RED_SHULKER_BOX);
            pib.description(user.getTranslation("protection.panel.flag-item.setting-disabled")); 
        } 
        return pib.build();
    }

    /**
     * Prevents visitors from getting damage if a particular damage type is listed in the config
     * @param e - event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVisitorGetDamage(EntityDamageEvent e) {
        World world = e.getEntity().getWorld();
        if (!getPlugin().getIWM().inWorld(e.getEntity().getLocation())
                || !getPlugin().getIWM().getIvSettings(world).contains(e.getCause().name())
                || !(e.getEntity() instanceof Player) 
                || e.getCause().equals(DamageCause.ENTITY_ATTACK)
                || getIslands().userIsOnIsland(world, User.getInstance(e.getEntity()))) {
            return;
        }
        // Player is a visitor and should be protected from damage
        e.setCancelled(true);
        Player p = (Player) e.getEntity();
        // Handle the void - teleport player back to island in a safe spot
        if(e.getCause().equals(DamageCause.VOID)) {
            // Will be set back after the teleport
            p.setGameMode(GameMode.SPECTATOR);
            getIslands().getIslandAt(p.getLocation()).ifPresent(i -> new SafeTeleportBuilder(getPlugin()).entity(p).island(i).build());
        }
    }



}

