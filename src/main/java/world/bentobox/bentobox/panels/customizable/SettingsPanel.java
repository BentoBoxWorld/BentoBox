/**
 * 
 */
package world.bentobox.bentobox.panels.customizable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.panels.TemplatedPanel.ItemSlot;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

/**
 * 
 */
public class SettingsPanel extends AbstractPanel {

    protected static final String PROTECTION_PANEL = "protection.panel.";
    private static final String CLICK_TO_SWITCH = PROTECTION_PANEL + "mode.click-to-switch";
    protected Flag.Type type;
    protected World world;
    protected Island island;
    protected TabbedPanel parent;

    private Map<UUID, Flag.Mode> currentMode = new HashMap<>();

    public SettingsPanel(CompositeCommand command, User user) {
        super(command, user);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected void build() {
        // TODO Auto-generated method stub

    }

    @Override
    protected PanelItem createNextButton(ItemTemplateRecord arg0, ItemSlot arg1) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected PanelItem createPreviousButton(ItemTemplateRecord arg0, ItemSlot arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
