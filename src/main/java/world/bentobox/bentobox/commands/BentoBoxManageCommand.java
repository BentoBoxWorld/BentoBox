package world.bentobox.bentobox.commands;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.ManagementPanel;

import java.util.List;

/**
 * Displays the Management panel.
 *
 * @author Poslovitch
 * @since 1.5.0
 */
public class BentoBoxManageCommand extends CompositeCommand {

    public BentoBoxManageCommand(CompositeCommand parent) {
        super(parent, "manage", "overview");
    }

    @Override
    public void setup() {
        setOnlyPlayer(true);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        ManagementPanel.openPanel(user, ManagementPanel.View.GAMEMODES);
        return true;
    }
}
