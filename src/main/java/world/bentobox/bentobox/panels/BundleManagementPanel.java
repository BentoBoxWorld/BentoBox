package world.bentobox.bentobox.panels;

import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.user.User;

public class BundleManagementPanel {
    private BundleManagementPanel() {}

    public static boolean openPanel(Panel panel, User user, ClickType clickType, int slot ) {
        return true;
    }
}
