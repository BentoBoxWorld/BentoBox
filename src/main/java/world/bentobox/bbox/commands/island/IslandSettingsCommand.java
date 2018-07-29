package world.bentobox.bbox.commands.island;

import java.util.List;

import world.bentobox.bbox.api.commands.CompositeCommand;
import world.bentobox.bbox.api.flags.Flag;
import world.bentobox.bbox.api.user.User;
import world.bentobox.bbox.panels.SettingsPanel;
import world.bentobox.bbox.util.Util;

/**
 * @author Poslovitch
 */
public class IslandSettingsCommand extends CompositeCommand {

    public IslandSettingsCommand(CompositeCommand islandCommand) {
        super(islandCommand, "settings", "flags");
    }

    @Override
    public void setup() {
        setPermission("island.settings");
        setOnlyPlayer(true);
        setDescription("commands.island.settings.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Settings are only shown if you are in the right world
        if (Util.getWorld(user.getWorld()).equals(getWorld())) {
            SettingsPanel.openPanel(getPlugin(), user, Flag.Type.PROTECTION, getWorld()); //TODO keep track of history?
            return true;
        } else {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
    }
}
