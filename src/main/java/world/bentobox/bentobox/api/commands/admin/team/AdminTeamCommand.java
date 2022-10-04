//
// Created by BONNe
// Copyright - 2022
//


package world.bentobox.bentobox.api.commands.admin.team;


import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;


/**
 * Parent command for all Admin Team commands.
 */
public class AdminTeamCommand extends CompositeCommand
{
    public AdminTeamCommand(CompositeCommand parent)
    {
        super(parent, "team");
    }


    @Override
    public void setup()
    {
        this.setPermission("mod.team");
        this.setDescription("commands.admin.team.description");

        new AdminTeamAddCommand(this);
        new AdminTeamDisbandCommand(this);
        new AdminTeamFixCommand(this);
        new AdminTeamKickCommand(this);
        new AdminTeamSetownerCommand(this);
    }


    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        this.showHelp(this, user);
        return true;
    }
}
