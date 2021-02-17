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

    private Island island;

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
        if (Util.getWorld(user.getWorld()).equals(getWorld())) {
            // Player is in same world
            island = getIslands().getIslandAt(user.getLocation()).orElseGet(() -> getIslands().getIsland(user.getWorld(), user.getUniqueId()));
        } else {
            island = getIslands().getIsland(getWorld(), user);
        }
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        new TabbedPanelBuilder()
        .user(user)
        .world(island.getWorld())
        .tab(1, new SettingsTab(user, island, Flag.Type.PROTECTION))
        .tab(2, new SettingsTab(user, island, Flag.Type.SETTING))
        .startingSlot(1)
        .size(54)
        .hideIfEmpty()
        .build().openPanel();
        return true;
    }
}
