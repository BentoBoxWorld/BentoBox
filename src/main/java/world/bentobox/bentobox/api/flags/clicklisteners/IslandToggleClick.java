package world.bentobox.bentobox.api.flags.clicklisteners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.flags.FlagSettingChangeEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.panels.settings.SettingsTab;
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
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
        // This click listener is used with TabbedPanel and SettingsTabs only
        TabbedPanel tp = (TabbedPanel)panel;
        SettingsTab st = (SettingsTab)tp.getActiveTab();

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
        // Get the island for this tab
        Island island = st.getIsland();
        if (island != null && (user.isOp() || user.getUniqueId().equals(island.getOwner()) || user.hasPermission(prefix + "admin.settings"))) {
            plugin.getFlagsManager().getFlag(id).ifPresent(flag -> {
                if (click.equals(ClickType.SHIFT_LEFT) && user.isOp()) {
                    if (!plugin.getIWM().getHiddenFlags(user.getWorld()).contains(flag.getID())) {
                        plugin.getIWM().getHiddenFlags(user.getWorld()).add(flag.getID());
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                    } else {
                        plugin.getIWM().getHiddenFlags(user.getWorld()).remove(flag.getID());
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
                    }
                    // Save changes
                    plugin.getIWM().getAddon(user.getWorld()).ifPresent(GameModeAddon::saveWorldSettings);
                } else {
                    // Check cooldown
                    if (!user.isOp() && island.isCooldown(flag)) {
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1F, 1F);
                        user.notify("protection.panel.flag-item.setting-cooldown");
                        return;
                    }
                    // Toggle flag
                    island.toggleFlag(flag);
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                    // Set cooldown
                    island.setCooldown(flag);
                    // Fire event
                    Bukkit.getPluginManager().callEvent(new FlagSettingChangeEvent(island, user.getUniqueId(), flag, island.isAllowed(flag)));
                }
            });
        } else {
            // Player is not the owner of the island.
            user.sendMessage("general.errors.not-owner");
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
        }
        return true;
    }
}
