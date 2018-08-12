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
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Toggles a island setting on/off
 * @author tastybento
 *
 */
public class IslandToggleClick implements ClickHandler {

    private BentoBox plugin = BentoBox.getInstance();
    private String id;

    /**
     * @param id - the flag ID that this click listener is associated with
     */
    public IslandToggleClick(String id) {
        this.id = id;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!plugin.getIWM().inWorld(user.getLocation())) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        String reqPerm = plugin.getIWM().getPermissionPrefix(Util.getWorld(user.getWorld())) + ".settings." + id;
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, reqPerm);
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
