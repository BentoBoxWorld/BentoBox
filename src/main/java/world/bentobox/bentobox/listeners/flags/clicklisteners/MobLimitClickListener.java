package world.bentobox.bentobox.listeners.flags.clicklisteners;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem.ClickHandler;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.clicklisteners.GeoMobLimitTab.EntityLimitTabType;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

/**
 * Provide limiting of mob types globally
 * @author tastybento
 *
 */
public class MobLimitClickListener implements ClickHandler {

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!user.inWorld()) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        World world = panel.getWorld().orElse(user.getWorld());
        IslandWorldManager iwm = BentoBox.getInstance().getIWM();
        String reqPerm = iwm.getPermissionPrefix(Util.getWorld(world)) + "admin.settings.LIMIT_MOBS";
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", "[permission]", reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        // Open the Sub Settings panel
        openPanel(user, world);

        return true;
    }

    private void openPanel(User user, World world) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel
        new TabbedPanelBuilder()
        .user(user)
        .world(world)
        .tab(1, new GeoMobLimitTab(user, EntityLimitTabType.MOB_LIMIT, world))
        .startingSlot(1)
        .size(54)
        .build().openPanel();
    }



}
