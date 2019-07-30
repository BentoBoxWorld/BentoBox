package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.panels.settings.SettingsTab;

/**
 * @author tastybento
 */
public class AdminSettingsCommand extends CompositeCommand {

    private @Nullable UUID targetUUID;
    private Island island;

    public AdminSettingsCommand(CompositeCommand islandCommand) {
        super(islandCommand, "settings", "flags", "options");
    }

    @Override
    public void setup() {
        setPermission("admin.settings");
        setOnlyPlayer(true);
        setParametersHelp("commands.admin.settings.parameters");
        setDescription("commands.admin.settings.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Get target player
        targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        island = getIslands().getIsland(getWorld(), targetUUID);
        if (island == null || !getPlugin().getIslands().hasIsland(getWorld(), targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        new TabbedPanelBuilder()
        .user(user)
        .world(getWorld())
        .tab(2, new SettingsTab(getWorld(), user, island, Flag.Type.PROTECTION))
        .tab(6, new SettingsTab(getWorld(), user, island, Flag.Type.SETTING))
        .startingSlot(1)
        .build().openPanel();
        return true;
    }
}
