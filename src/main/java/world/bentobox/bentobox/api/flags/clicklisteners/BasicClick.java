package world.bentobox.bentobox.api.flags.clicklisteners;

import org.bukkit.event.inventory.ClickType;

import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;

/**
 * Basic click WIP
 * @author tastybento
 *
 */
public class BasicClick implements PanelItem.ClickHandler {

    private final String id;

    public BasicClick(String id) {
        this.id = id;
    }

    @Override
    public boolean onClick(Panel panel, User user, ClickType click, int slot) {
        // TODO
        return true;
    }

}
