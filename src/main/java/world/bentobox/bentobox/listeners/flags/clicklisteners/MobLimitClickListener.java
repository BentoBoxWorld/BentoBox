package world.bentobox.bentobox.listeners.flags.clicklisteners;

import java.util.Objects;

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

    private final EntityLimitTabType type;

    /**
     * Opens the mob block list ({@link EntityLimitTabType#MOB_LIMIT})
     */
    public MobLimitClickListener() {
        this(EntityLimitTabType.MOB_LIMIT);
    }

    /**
     * @param type - which list this click handler opens
     * @since 3.23.6
     */
    public MobLimitClickListener(EntityLimitTabType type) {
        this.type = type;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType clickType, int slot) {
        // Get the world
        if (!user.inWorld()) {
            user.sendMessage("general.errors.wrong-world");
            return true;
        }
        World world = panel.getWorld().orElse(user.getWorld());
        IslandWorldManager iwm = BentoBox.getInstance().getIWM();
        World w = Objects.requireNonNull(Util.getWorld(world));
        GeoMobLimitTab tab = new GeoMobLimitTab(user, type, world);
        String reqPerm = iwm.getPermissionPrefix(w) + "admin.settings." + tab.getFlagKey();
        if (!user.hasPermission(reqPerm)) {
            user.sendMessage("general.errors.no-permission", "[permission]", reqPerm);
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            return true;
        }

        // Open the Sub Settings panel
        openPanel(user, world, tab);

        return true;
    }

    private void openPanel(User user, World world, GeoMobLimitTab tab) {
        // Close the current panel
        user.closeInventory();
        // Open a new panel
        new TabbedPanelBuilder()
        .user(user)
        .world(world)
        .tab(1, tab)
        .startingSlot(1)
        .size(54)
        .build().openPanel();
    }



}
