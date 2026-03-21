package world.bentobox.bentobox.commands;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.PlaceholderPanel;

/**
 * Opens the Placeholder Browser GUI.
 * <p>
 * Usage: {@code /bbox placeholders}
 * </p>
 *
 * @since 3.2.0
 */
public class BentoBoxPlaceholderCommand extends CompositeCommand {

    public BentoBoxPlaceholderCommand(CompositeCommand parent) {
        super(parent, "placeholders", "ph");
    }

    @Override
    public void setup() {
        setPermission("bentobox.admin.placeholders");
        setOnlyPlayer(true);
        setDescription("commands.bentobox.placeholders.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        PlaceholderPanel.openPanel(this, user);
        return true;
    }
}
