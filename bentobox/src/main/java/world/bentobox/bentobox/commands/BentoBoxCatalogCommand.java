package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.CatalogPanel;

/**
 * Displays the Addons Catalog.
 *
 * @since 1.5.0
 * @author Poslovitch
 */
public class BentoBoxCatalogCommand extends CompositeCommand {

    public BentoBoxCatalogCommand(CompositeCommand parent) {
        super(parent, "catalog");
    }

    @Override
    public void setup() {
        setPermission("bentobox.admin.catalog");
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        CatalogPanel.openPanel(user, CatalogPanel.View.GAMEMODES);
        return true;
    }
}
