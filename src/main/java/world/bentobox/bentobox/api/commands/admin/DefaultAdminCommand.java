package world.bentobox.bentobox.api.commands.admin;

import java.util.List;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.admin.blueprints.AdminBlueprintCommand;
import world.bentobox.bentobox.api.commands.admin.deaths.AdminDeathsCommand;
import world.bentobox.bentobox.api.commands.admin.purge.AdminPurgeCommand;
import world.bentobox.bentobox.api.commands.admin.range.AdminRangeCommand;
import world.bentobox.bentobox.api.commands.admin.resets.AdminResetsCommand;
import world.bentobox.bentobox.api.commands.admin.team.AdminTeamAddCommand;
import world.bentobox.bentobox.api.commands.admin.team.AdminTeamDisbandCommand;
import world.bentobox.bentobox.api.commands.admin.team.AdminTeamKickCommand;
import world.bentobox.bentobox.api.commands.admin.team.AdminTeamSetownerCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * This is default Admin command for console and op. It contains all necessary parts that
 * for main command.
 * @since 1.13.0
 * @author BONNe
 */
public abstract class DefaultAdminCommand extends CompositeCommand {
    /**
     * This is the top-level command constructor for commands that have no parent.
     *
     * @param addon   - GameMode addon
     */
    public DefaultAdminCommand(GameModeAddon addon) {
        // Register command with alias from config.
        super(addon,
                addon.getWorldSettings().getAdminCommandAliases().split(" ")[0],
                addon.getWorldSettings().getAdminCommandAliases().split(" "));
    }

    /**
     * Setups anything that is necessary for default main admin command.
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#setup()
     */
    @Override
    public void setup() {
        this.setPermission("admin.*");
        this.setOnlyPlayer(false);

        this.setParametersHelp("commands.admin.help.parameters");
        this.setDescription("commands.admin.help.description");

        new AdminVersionCommand(this);
        new AdminTeleportCommand(this, "tp");
        new AdminTeleportCommand(this, "tpnether");
        new AdminTeleportCommand(this, "tpend");
        new AdminGetrankCommand(this);
        new AdminSetrankCommand(this);
        new AdminInfoCommand(this);
        // Team commands
        new AdminTeamAddCommand(this);
        new AdminTeamKickCommand(this);
        new AdminTeamDisbandCommand(this);
        new AdminTeamSetownerCommand(this);
        // Schems
        new AdminBlueprintCommand(this);
        // Register/unregister islands
        new AdminRegisterCommand(this);
        new AdminUnregisterCommand(this);
        // Range
        new AdminRangeCommand(this);
        // Resets
        new AdminResetsCommand(this);
        // Delete
        new AdminDeleteCommand(this);
        // Why
        new AdminWhyCommand(this);
        // Deaths
        new AdminDeathsCommand(this);
        // Reload
        new AdminReloadCommand(this);
        // Spawn
        new AdminSetspawnCommand(this);
        // Spawn Point command
        new AdminSetSpawnPointCommand(this);
        // Reset flags
        new AdminResetFlagsCommand(this);
        // Switch
        new AdminSwitchCommand(this);
        // Purge
        new AdminPurgeCommand(this);
        // Settings
        new AdminSettingsCommand(this);
    }

    /**
     * Defines what will be executed when this command is run.
     * @see world.bentobox.bentobox.api.commands.BentoBoxCommand#execute(User, String, List&lt;String&gt;)
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (user != null && !args.isEmpty()) {
            user.sendMessage("general.errors.unknown-command", TextVariables.LABEL, getTopLabel());
            return false;
        }

        // By default run the attached help command, if it exists (it should)
        return this.showHelp(this, user);
    }
}
