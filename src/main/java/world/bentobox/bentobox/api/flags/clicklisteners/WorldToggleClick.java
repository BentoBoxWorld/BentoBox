package world.bentobox.bentobox.api.flags.clicklisteners;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.flags.FlagWorldSettingChangeEvent;
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

    private final BentoBox plugin = BentoBox.getInstance();
    private final String id;

    /**
     * @param id - the flag ID that this click listener is associated with
     */
    public WorldToggleClick(String id) {
        this.id = id;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
        if (plugin.onTimeout(user)) {
            return true;
        }
        World world = panel.getWorld().orElseThrow(); // The panel must have a world
        String reqPerm = plugin.getIWM().getPermissionPrefix(world) + "admin.world.settings." + id;
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", TextVariables.PERMISSION, reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }
        // Get flag
        plugin.getFlagsManager().getFlag(id).ifPresent(flag -> {
            if (click.equals(ClickType.SHIFT_LEFT) && user.isOp()) {
                if (!plugin.getIWM().getHiddenFlags(world).contains(flag.getID())) {
                    plugin.getIWM().getHiddenFlags(world).add(flag.getID());
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                } else {
                    plugin.getIWM().getHiddenFlags(world).remove(flag.getID());
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
                }
                // Save changes
                plugin.getIWM().getAddon(world).ifPresent(GameModeAddon::saveWorldSettings);
            } else {
                // Toggle flag
                flag.setSetting(world, !flag.isSetForWorld(world));
                user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                // Fire event
                Bukkit.getPluginManager().callEvent(
                        new FlagWorldSettingChangeEvent(world, user.getUniqueId(), flag, flag.isSetForWorld(world)));

                // Subflag support
                if (flag.hasSubflags()) {
                    // Fire events for all subflags as well
                    flag.getSubflags().forEach(
                            subflag -> Bukkit.getPluginManager().callEvent(new FlagWorldSettingChangeEvent(world,
                                    user.getUniqueId(), subflag, subflag.isSetForWorld(world))));
                }
            }

            // Save world settings
            plugin.getIWM().getAddon(Util.getWorld(world)).ifPresent(GameModeAddon::saveWorldSettings);
        });
        return true;
    }

}
