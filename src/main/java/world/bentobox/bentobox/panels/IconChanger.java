package world.bentobox.bentobox.panels;

import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * This class listens to clicks in the inventory and changes the icons of Blueprints and Blueprint Bundles
 * @author tastybento
 *
 */
public class IconChanger implements PanelListener {

    private GameModeAddon addon;
    private BlueprintBundle bb;
    private BlueprintManagementPanel blueprintManagementPanel;
    private BentoBox plugin;

    /**
     * Change the icon of a blueprint bundle or blueprint
     * @param plugin - Bentobox
     * @param addon - the Game Mode Addon
     * @param blueprintManagementPanel - the open Blueprint Management Panel
     * @param bb - the blueprint bundle
     */
    public IconChanger(BentoBox plugin, GameModeAddon addon, BlueprintManagementPanel blueprintManagementPanel, BlueprintBundle bb) {
        this.plugin = plugin;
        this.addon = addon;
        this.blueprintManagementPanel = blueprintManagementPanel;
        this.bb = bb;
    }

    @Override
    public void onInventoryClick(User user, InventoryClickEvent event) {
        // Handle icon changing
        if (event.getCurrentItem() != null && !event.getCurrentItem().getType().equals(Material.AIR) && event.getRawSlot() > 44) {
            Material icon = event.getCurrentItem().getType();
            Entry<Integer, Blueprint> selected = blueprintManagementPanel.getSelected();
            user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
            if (selected == null) {
                // Change the Bundle Icon
                bb.setIcon(icon);
                // Save it
                plugin.getBlueprintsManager().saveBlueprintBundle(addon, bb);

            } else {
                // Change the Blueprint icon
                Blueprint bp = selected.getValue();
                bp.setIcon(icon);
                // Save it
                plugin.getBlueprintsManager().saveBlueprint(addon, bp);
            }
            // Update the view
            blueprintManagementPanel.openBB(bb);
        }
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        // Do nothing

    }

    @Override
    public void setup() {
        // Do nothing
    }

}
