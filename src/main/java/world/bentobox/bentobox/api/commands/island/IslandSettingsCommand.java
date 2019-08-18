package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class IslandSettingsCommand extends CompositeCommand {

    public IslandSettingsCommand(CompositeCommand islandCommand) {
        super(islandCommand, "settings", "flags", "options");
    }

    @Override
    public void setup() {
        setPermission("island.settings");
        setOnlyPlayer(true);
        setDescription("commands.island.settings.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Settings are only shown if you are in the right world
        if (Util.getWorld(user.getWorld()).equals(getWorld())) {
            return true;
        } else {
            user.sendMessage("general.errors.wrong-world");
            return false;
        }
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        Island island = getIslands().getIslandAt(user.getLocation()).orElse(getIslands().getIsland(user.getWorld(), user.getUniqueId()));
        new TabbedPanelBuilder()
        .user(user)
        .world(getWorld())
        .tab(1, new SettingsTab(getWorld(), user, island, Flag.Type.PROTECTION))
        .tab(2, new SettingsTab(getWorld(), user, island, Flag.Type.SETTING))
        .startingSlot(1)
        .size(54)
        .hideIfEmpty()
        .build().openPanel();
        return true;
    }
}
