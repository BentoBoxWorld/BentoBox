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
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

/**
 * Toggles a island setting on/off
 * @author tastybento
 *
 */
public class IslandToggleClickListener implements ClickHandler {

    private BSkyBlock plugin = BSkyBlock.getInstance();
    private String id;

    /**
     * @param id - the flag ID that this click listener is associated with
     */
    public IslandToggleClickListener(String id) {
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
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }
        // Get the user's island
        Island island = plugin.getIslands().getIsland(user.getWorld(), user);
        if (island != null && island.getOwner().equals(user.getUniqueId())) {
            Flag flag = plugin.getFlagsManager().getFlagByID(id);
            // Toggle flag
            island.toggleFlag(flag);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
            // Apply change to panel
            panel.getInventory().setItem(slot, flag.toPanelItem(plugin, user).getItem());
        } else {
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
        }
        return true;
    }

}
