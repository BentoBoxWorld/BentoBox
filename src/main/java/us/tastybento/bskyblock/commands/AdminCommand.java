package us.tastybento.bskyblock.commands;

import java.util.List;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.admin.AdminGetRankCommand;
import us.tastybento.bskyblock.commands.admin.AdminInfoCommand;
import us.tastybento.bskyblock.commands.admin.AdminReloadCommand;
import us.tastybento.bskyblock.commands.admin.AdminSchemCommand;
import us.tastybento.bskyblock.commands.admin.AdminSetRankCommand;
import us.tastybento.bskyblock.commands.admin.AdminTeleportCommand;
import us.tastybento.bskyblock.commands.admin.AdminVersionCommand;
import us.tastybento.bskyblock.commands.admin.teams.AdminTeamAddCommand;
import us.tastybento.bskyblock.commands.admin.teams.AdminTeamDisbandCommand;
import us.tastybento.bskyblock.commands.admin.teams.AdminTeamKickCommand;
import us.tastybento.bskyblock.commands.admin.teams.AdminTeamMakeLeaderCommand;

public class AdminCommand extends CompositeCommand {

    public AdminCommand() {
        super(Constants.ADMINCOMMAND, "bsb");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.*");
        setOnlyPlayer(false);
        setParameters("commands.admin.help.parameters");
        setDescription("commands.admin.help.description");
        new AdminVersionCommand(this);
        new AdminReloadCommand(this);
        new AdminTeleportCommand(this, "tp");
        new AdminTeleportCommand(this, "tpnether");
        new AdminTeleportCommand(this, "tpend");
        new AdminGetRankCommand(this);
        new AdminSetRankCommand(this);
        new AdminInfoCommand(this);
        // Team commands
        new AdminTeamAddCommand(this);
        new AdminTeamKickCommand(this);
        new AdminTeamDisbandCommand(this);
        new AdminTeamMakeLeaderCommand(this);
        // Schems
        new AdminSchemCommand(this);
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (!args.isEmpty()) {
            user.sendMessage("general.errors.unknown-command", "[label]", Constants.ADMINCOMMAND);
            return false;
        }
        // By default run the attached help command, if it exists (it should)
        return showHelp(this, user);
    }

}
