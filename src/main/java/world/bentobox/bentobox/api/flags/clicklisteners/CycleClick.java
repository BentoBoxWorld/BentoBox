package world.bentobox.bentobox.api.flags.clicklisteners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.flags.FlagProtectionChangeEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * Left Clicks increase rank, right clicks lower rank
 * @author tastybento
 *
 */
public class CycleClick implements PanelItem.ClickHandler {

    protected BentoBox plugin = BentoBox.getInstance();
    protected Island island;
    protected User user;
    protected boolean changeOccurred;
    private final String id;
    private int minRank = RanksManager.VISITOR_RANK;
    private int maxRank = RanksManager.OWNER_RANK;

    /**
     * Construct default cycle clicker with min rank of {@link RanksManager#VISITOR_RANK}
     * and max rank of {@link RanksManager#OWNER_RANK}
     * @param id - the flag id that will be adjusted by this click
     */
    public CycleClick(String id) {
        this.id = id;
    }

    /**
     * Construct a cycle clicker with a min and max rank
     * @param id flag id
     * @param minRank minimum rank value
     * @param maxRank maximum rank value
     */
    public CycleClick(String id, int minRank, int maxRank) {
        this.id = id;
        this.minRank = minRank;
        this.maxRank = maxRank;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
        // This click listener is used with TabbedPanel and SettingsTabs only
        TabbedPanel tp = (TabbedPanel)panel;
        SettingsTab st = (SettingsTab)tp.getActiveTab();
        // Get the island for this tab
        island = st.getIsland();
        this.user = user;
        changeOccurred = false;
        // Permission prefix
        String prefix = plugin.getIWM().getPermissionPrefix(Util.getWorld(user.getWorld()));
        String reqPerm = prefix + "settings." + id;
        String allPerms = prefix + "settings.*";
        if (!user.hasPermission(reqPerm) && !user.hasPermission(allPerms)
                && !user.isOp() && !user.hasPermission(prefix + "admin.settings")) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }
        // Left clicking increases the rank required
        // Right clicking decreases the rank required
        // Shift Left Click toggles player visibility
        if (island != null && (user.isOp() || user.getUniqueId().equals(island.getOwner()) || user.hasPermission(prefix + "admin.settings"))) {
            changeOccurred = true;
            RanksManager rm = plugin.getRanksManager();
            plugin.getFlagsManager().getFlag(id).ifPresent(flag -> {
                // Rank
                int currentRank = island.getFlag(flag);
                if (click.equals(ClickType.LEFT)) {
                    if (currentRank >= maxRank) {
                        island.setFlag(flag, minRank);
                    } else {
                        island.setFlag(flag, rm.getRankUpValue(currentRank));
                    }
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_OFF, 1F, 1F);
                    // Fire event
                    Bukkit.getPluginManager().callEvent(new FlagProtectionChangeEvent(island, user.getUniqueId(), flag, island.getFlag(flag)));
                } else if (click.equals(ClickType.RIGHT)) {
                    if (currentRank <= minRank) {
                        island.setFlag(flag, maxRank);
                    } else {
                        island.setFlag(flag, rm.getRankDownValue(currentRank));
                    }
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                    // Fire event
                    Bukkit.getPluginManager().callEvent(new FlagProtectionChangeEvent(island, user.getUniqueId(), flag, island.getFlag(flag)));
                } else if (click.equals(ClickType.SHIFT_LEFT) && user.isOp()) {
                    if (!plugin.getIWM().getHiddenFlags(user.getWorld()).contains(flag.getID())) {
                        plugin.getIWM().getHiddenFlags(user.getWorld()).add(flag.getID());
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                    } else {
                        plugin.getIWM().getHiddenFlags(user.getWorld()).remove(flag.getID());
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
                    }
                    // Save changes
                    plugin.getIWM().getAddon(user.getWorld()).ifPresent(GameModeAddon::saveWorldSettings);
                }
            });
        } else {
            // Player is not the owner of the island.
            user.sendMessage("general.errors.not-owner");
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
        }
        return true;
    }

    /**
     * @param minRank the minRank to set
     */
    public void setMinRank(int minRank) {
        this.minRank = minRank;
    }

    /**
     * @param maxRank the maxRank to set
     */
    public void setMaxRank(int maxRank) {
        this.maxRank = maxRank;
    }

}
