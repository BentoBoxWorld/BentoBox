/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import java.util.Arrays;

import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.ClickType;

import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

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
        }
        // Open the IV Settings panel
        openPanel(user, ivPanelName);
        return true;
    }

    private void openPanel(User user, String ivPanelName) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel for visitor protection
        PanelBuilder pb = new PanelBuilder();
        pb.user(user).name(ivPanelName);
        // Make panel items
        Arrays.stream(EntityDamageEvent.DamageCause.values()).forEach(c -> {
            PanelItemBuilder pib = new PanelItemBuilder();
            pib.name(Util.prettifyText(c.toString()));
            pib.clickHandler(this);
            if (getPlugin().getSettings().getIvSettings().contains(c.name())) {
                pib.icon(Material.GREEN_GLAZED_TERRACOTTA);
                pib.description(user.getTranslation("protection.panel.flag-item.setting-active"));
            } else {
                pib.icon(Material.RED_GLAZED_TERRACOTTA);
                pib.description(user.getTranslation("protection.panel.flag-item.setting-disabled")); 
            }
            pb.item(pib.build());
        });
        pb.build();
        
    }



}

