package world.bentobox.bentobox.api.flags.clicklisteners;

import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
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
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
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
        // Get the user's island or where they are standing
        Island island = plugin.getIslands().getIslandAt(user.getLocation()).orElse(plugin.getIslands().getIsland(user.getWorld(), user.getUniqueId()));
        if (island != null && (user.isOp() || user.getUniqueId().equals(island.getOwner()))) {
            plugin.getFlagsManager().getFlag(id).ifPresent(flag -> {

                // Visibility
                boolean invisible = false;
                if (click.equals(ClickType.SHIFT_LEFT) && user.isOp()) {
                    if (!plugin.getIWM().getHiddenFlags(user.getWorld()).contains(flag.getID())) {
                        invisible = true;
                        plugin.getIWM().getHiddenFlags(user.getWorld()).add(flag.getID());
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                    } else {
                        plugin.getIWM().getHiddenFlags(user.getWorld()).remove(flag.getID());
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
                    }
                    // Save changes
                    plugin.getIWM().getAddon(user.getWorld()).ifPresent(GameModeAddon::saveWorldSettings);
                } else {
                    // Toggle flag
                    island.toggleFlag(flag);
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                }
                // Apply change to panel
                panel.getInventory().setItem(slot, flag.toPanelItem(plugin, user, invisible).getItem());
            });
        } else {
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
        }
        return true;
    }

}
