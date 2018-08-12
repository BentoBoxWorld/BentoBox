/*

 */
package world.bentobox.bentobox.api.flags.clicklisteners;

import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Toggles a worldwide setting on/off
 * @author tastybento
 *
 */
public class WorldToggleClick implements ClickHandler {

    private BentoBox plugin = BentoBox.getInstance();
    private String id;

    /**
     * @param id - the flag ID that this click listener is associated with
     */
    public WorldToggleClick(String id) {
        this.id = id;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!plugin.getIWM().inWorld(user.getLocation())) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        String reqPerm = plugin.getIWM().getPermissionPrefix(Util.getWorld(user.getWorld())) + ".admin.world.settings." + id;
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }
        // Get flag
        Flag flag = plugin.getFlagsManager().getFlagByID(id);
        // Toggle flag
        flag.setSetting(user.getWorld(), !flag.isSetForWorld(user.getWorld()));
        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
        // Apply change to panel
        panel.getInventory().setItem(slot, flag.toPanelItem(plugin, user).getItem());
        return true;
    }

}
