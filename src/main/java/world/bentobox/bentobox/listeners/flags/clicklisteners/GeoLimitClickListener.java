/**
 *
 */
package world.bentobox.bentobox.listeners.flags.clicklisteners;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

/**
 * Provide geo limiting to mobs - removed them if they go outside island bounds
 * @author tastybento
 *
 */
public class GeoLimitClickListener implements ClickHandler {

    /**
     * A list of all living entity types, minus some
     */
    private final List<EntityType> livingEntityTypes = Arrays.stream(EntityType.values())
            .filter(EntityType::isAlive)
            .filter(t -> !(t.equals(EntityType.PLAYER) || t.equals(EntityType.GIANT) || t.equals(EntityType.ARMOR_STAND)))
            .sorted(Comparator.comparing(EntityType::name))
            .collect(Collectors.toList());

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!user.inWorld()) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        IslandWorldManager iwm = BentoBox.getInstance().getIWM();
        String reqPerm = iwm.getPermissionPrefix(Util.getWorld(user.getWorld())) + ".admin.settings.GEO_LIMIT_MOBS";
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", "[permission]", reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        String panelName = user.getTranslation("protection.flags.GEO_LIMIT_MOBS.name");
        if (panel.getName().equals(panelName)) {
            // This is a click on the geo limit panel
            // Slot relates to the enum
            EntityType c = livingEntityTypes.get(slot);
            if (iwm.getGeoLimitSettings(user.getWorld()).contains(c.name())) {
                iwm.getGeoLimitSettings(user.getWorld()).remove(c.name());
            } else {
                iwm.getGeoLimitSettings(user.getWorld()).add(c.name());
            }
            // Apply change to panel
            panel.getInventory().setItem(slot, getPanelItem(c, user).getItem());
        } else {
            // Open the Sub Settings panel
            openPanel(user, panelName);
        }
        return true;
    }

    private void openPanel(User user, String panelName) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel
        PanelBuilder pb = new PanelBuilder();
        pb.user(user).name(panelName);
        // Make panel items
        livingEntityTypes.forEach(c -> pb.item(getPanelItem(c, user)));
        pb.build();

    }

    private PanelItem getPanelItem(EntityType c, User user) {
        PanelItemBuilder pib = new PanelItemBuilder();
        pib.name(Util.prettifyText(c.toString()));
        pib.clickHandler(this);
        if (BentoBox.getInstance().getIWM().getGeoLimitSettings(user.getWorld()).contains(c.name())) {
            pib.icon(Material.GREEN_SHULKER_BOX);
            pib.description(user.getTranslation("protection.panel.flag-item.setting-active"));
        } else {
            pib.icon(Material.RED_SHULKER_BOX);
            pib.description(user.getTranslation("protection.panel.flag-item.setting-disabled"));
        }
        return pib.build();
    }

}
