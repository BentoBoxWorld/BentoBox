package world.bentobox.bentobox.listeners.flags.clicklisteners;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
public class CommandCycleClick implements ClickHandler {

    protected static final String COMMAND_RANK_PREFIX = "COMMAND_RANK:";
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
        if (island != null && island.getOwner() != null && island.isAllowed(user, Flags.CHANGE_SETTINGS)) {
            int currentRank = island.getRankCommand(command);
            if (click.equals(ClickType.LEFT)) {
                if (currentRank == RanksManager.OWNER_RANK) {
                    island.setRankCommand(command, RanksManager.MEMBER_RANK);
                } else {
                    island.setRankCommand(command, RanksManager.getInstance().getRankUpValue(currentRank));
                }
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
            } else if (click.equals(ClickType.RIGHT)) {
                if (currentRank == RanksManager.MEMBER_RANK) {
                    island.setRankCommand(command, RanksManager.OWNER_RANK);
                } else {
                    island.setRankCommand(command, RanksManager.getInstance().getRankDownValue(currentRank));
                }
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
            } else if (click.equals(ClickType.SHIFT_LEFT) && user.isOp()) {
                leftShiftClick(user);
            }
            // Apply change to panel
            panel.getInventory().setItem(slot, commandRankClickListener.getPanelItem(command, user, world).getItem());
        } else {
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
        }
        return true;
    }

    /**
     * Adds or removes the command rank from visibility by non-Ops
     * @param user - the Op requesting the change
     */
    private void leftShiftClick(User user) {
        String configSetting = COMMAND_RANK_PREFIX + command;
        if (!plugin.getIWM().getHiddenFlags(user.getWorld()).contains(configSetting)) {
            plugin.getIWM().getHiddenFlags(user.getWorld()).add(configSetting);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
        } else {
            plugin.getIWM().getHiddenFlags(user.getWorld()).remove(configSetting);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
        }
        // Save changes
        plugin.getIWM().getAddon(user.getWorld()).ifPresent(GameModeAddon::saveWorldSettings);

    }

}
