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
import world.bentobox.bentobox.panels.settings.WorldDefaultSettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 * @since 1.6.0
 */
public class AdminSettingsCommand extends CompositeCommand {

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
        if (args.size() > 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        if (args.isEmpty()) {
            // World settings
            return true;
        }
        // Get target player
        @Nullable UUID targetUUID = Util.getUUID(args.get(0));
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
        if (args.isEmpty()) {
            new TabbedPanelBuilder()
            .user(user)
            .world(getWorld())
            .tab(1, new SettingsTab(getWorld(), user, Flag.Type.WORLD_SETTING))
            .tab(2, new WorldDefaultSettingsTab(getWorld(), user))
            .startingSlot(1)
            .size(54)
            .build().openPanel();
            return true;
        }
        // Player settings
        new TabbedPanelBuilder()
        .user(user)
        .world(getWorld())
        .tab(1, new SettingsTab(getWorld(), user, island, Flag.Type.PROTECTION))
        .tab(2, new SettingsTab(getWorld(), user, island, Flag.Type.SETTING))
        .startingSlot(1)
        .size(54)
        .build().openPanel();
        return true;
    }
}
