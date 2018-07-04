/*

 */
package us.tastybento.bskyblock.api.flags.clicklisteners;

import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

/**
 * Toggles a worldwide setting on/off
 * @author tastybento
 *
 */
public class WorldToggleClickListener implements ClickHandler {
    
    private BSkyBlock plugin = BSkyBlock.getInstance();
    private String id;
    
    /**
     * @param id - the flag ID that this click listener is associated with
     */
    public WorldToggleClickListener(String id) {
        this.id = id;
    }


    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.panels.PanelItem.ClickHandler#onClick(us.tastybento.bskyblock.api.panels.Panel, us.tastybento.bskyblock.api.user.User, org.bukkit.event.inventory.ClickType, int)
     */
    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!plugin.getIWM().inWorld(user.getLocation())) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        String reqPerm = plugin.getIWM().getPermissionPrefix(Util.getWorld(user.getWorld())) + ".settings." + id;
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission");
            user.sendMessage("general.errors.you-need", "[permission]", reqPerm);
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
