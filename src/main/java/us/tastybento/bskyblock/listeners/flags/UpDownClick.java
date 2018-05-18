/**
 * 
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.RanksManager;

/**
 * Left Clicks increase rank, right clicks lower rank
 * @author tastybento
 *
 */
public class UpDownClick implements PanelItem.ClickHandler {

    private BSkyBlock plugin = BSkyBlock.getInstance();
    private final String id;


    /**
     * @param id - the flag id that will be adjusted by this click
     */
    public UpDownClick(String id) {
        this.id = id;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
        // Left clicking increases the rank required
        // Right clicking decreases the rank required
        // Get the user's island
        Island island = plugin.getIslands().getIsland(user.getWorld(), user.getUniqueId());
        if (island != null && island.getOwner().equals(user.getUniqueId())) {
            RanksManager rm = plugin.getRanksManager();
            Flag flag = plugin.getFlagsManager().getFlagByID(id);
            int currentRank = island.getFlag(flag);
            if (click.equals(ClickType.LEFT)) {
                int nextRank = rm.getRankUpValue(currentRank);
                island.setFlag(flag, nextRank);
                user.getWorld().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
            } else if (click.equals(ClickType.RIGHT)) {
                int nextRank = rm.getRankDownValue(currentRank);
                island.setFlag(flag, nextRank);
                user.getWorld().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1F, 1F);
            }
            // Apply change to panel
            panel.getInventory().setItem(slot, flag.toPanelItem(plugin, user).getItem());
        }
        return true;
    }

}
