/*

 */
package world.bentobox.bentobox.listeners.flags;

import java.util.Arrays;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeTeleportBuilder;

/**
 * Listener for invincible visitor settings. Handles click listening and damage events
 * @author tastybento
 *
 */
public class InvincibleVisitorsListener extends AbstractFlagListener implements ClickHandler {

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!user.inWorld()) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        String reqPerm = getIWM().getPermissionPrefix(Util.getWorld(user.getWorld())) + ".admin.settings.INVINCIBLE_VISITORS";
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", "[permission]", reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        String ivPanelName = user.getTranslation("protection.flags.INVINCIBLE_VISITORS.name");
        if (panel.getName().equals(ivPanelName)) {
            // This is a click on the IV panel
            // Slot relates to the enum
            DamageCause c = Arrays.asList(EntityDamageEvent.DamageCause.values()).get(slot);
            if (getIWM().getIvSettings(user.getWorld()).contains(c.name())) {
                getIWM().getIvSettings(user.getWorld()).remove(c.name());
            } else {
                getIWM().getIvSettings(user.getWorld()).add(c.name());
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
        if (getIWM().getIvSettings(user.getWorld()).contains(c.name())) {
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
        if (!(e.getEntity() instanceof Player)
                || !getIWM().inWorld(e.getEntity().getLocation())
                || !getIWM().getIvSettings(world).contains(e.getCause().name())
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

