package world.bentobox.bentobox.api.flags.clicklisteners;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Left-clicks increase the default island protection rank, right-clicks decrease it.
 * This modifies the default rank that new islands will receive for a protection flag.
 * @author tastybento
 * @since 3.14.0
 */
public class IslandDefaultCycleClick implements PanelItem.ClickHandler {

    private final BentoBox plugin = BentoBox.getInstance();
    private final String id;
    private int minRank = RanksManager.VISITOR_RANK;
    private int maxRank = RanksManager.OWNER_RANK;

    /**
     * Construct default cycle clicker with min rank of {@link RanksManager#VISITOR_RANK}
     * and max rank of {@link RanksManager#OWNER_RANK}
     * @param id - the flag id that will be adjusted by this click
     */
    public IslandDefaultCycleClick(String id) {
        this.id = id;
    }

    /**
     * Construct a cycle clicker with a min and max rank
     * @param id flag id
     * @param minRank minimum rank value
     * @param maxRank maximum rank value
     */
    public IslandDefaultCycleClick(String id, int minRank, int maxRank) {
        this.id = id;
        this.minRank = minRank;
        this.maxRank = maxRank;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
        if (panel.getWorld().isEmpty()) {
            plugin.logError("Panel " + panel.getName()
                    + " has no world associated with it. Please report this bug to the author.");
            return true;
        }
        World world = panel.getWorld().get();
        // Permission check
        String reqPerm = plugin.getIWM().getPermissionPrefix(world) + "admin.set-world-defaults";
        if (!user.hasPermission(reqPerm) && !user.isOp()) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }
        // Get flag
        plugin.getFlagsManager().getFlag(id).ifPresent(flag -> {
            // Get current default rank from world settings
            int currentRank = plugin.getIWM().getWorldSettings(world)
                    .getDefaultIslandFlagNames().getOrDefault(id, flag.getDefaultRank());

            int newRank;
            if (click.equals(ClickType.LEFT)) {
                newRank = currentRank >= maxRank ? minRank
                        : RanksManager.getInstance().getRankUpValue(currentRank);
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1F, 1F);
            } else if (click.equals(ClickType.RIGHT)) {
                newRank = currentRank <= minRank ? maxRank
                        : RanksManager.getInstance().getRankDownValue(currentRank);
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
            } else {
                return;
            }

            // Update default island flag rank in world settings
            plugin.getIWM().getWorldSettings(world).getDefaultIslandFlagNames().put(id, newRank);

            // Save world settings
            plugin.getIWM().getAddon(Util.getWorld(world)).ifPresent(GameModeAddon::saveWorldSettings);
        });
        return true;
    }

}
