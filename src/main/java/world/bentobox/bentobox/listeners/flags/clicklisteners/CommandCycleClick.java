package world.bentobox.bentobox.listeners.flags.clicklisteners;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class CommandCycleClick implements ClickHandler {

    private final BentoBox plugin = BentoBox.getInstance();
    private final String command;
    private final CommandRankClickListener commandRankClickListener;

    public CommandCycleClick(CommandRankClickListener commandRankClickListener, String c) {
        this.commandRankClickListener = commandRankClickListener;
        this.command = c;
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.api.panels.PanelItem.ClickHandler#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)
     */
    @Override
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
        // Left clicking increases the rank required
        // Right clicking decreases the rank required
        // Get the user's island for the game world
        World world = panel.getWorld().orElse(user.getWorld());
        Island island = plugin.getIslands().getIsland(world, user.getUniqueId());
        if (island != null && island.getOwner().equals(user.getUniqueId())) {
            RanksManager rm = plugin.getRanksManager();
            int currentRank = island.getRankCommand(command);
            if (click.equals(ClickType.LEFT)) {
                if (currentRank == RanksManager.OWNER_RANK) {
                    island.setRankCommand(command, RanksManager.MEMBER_RANK);
                } else {
                    island.setRankCommand(command, rm.getRankUpValue(currentRank));
                }
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
            } else if (click.equals(ClickType.RIGHT)) {
                if (currentRank == RanksManager.MEMBER_RANK) {
                    island.setRankCommand(command, RanksManager.OWNER_RANK);
                } else {
                    island.setRankCommand(command, rm.getRankDownValue(currentRank));
                }
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
            }
            // Apply change to panel
            panel.getInventory().setItem(slot, commandRankClickListener.getPanelItem(command, user, world).getItem());
            // Save island
            plugin.getIslands().save(island);

        } else {
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
        }
        return true;
    }

}
