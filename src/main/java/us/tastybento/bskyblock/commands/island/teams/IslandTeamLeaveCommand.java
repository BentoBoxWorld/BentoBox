package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.User;

public class IslandTeamLeaveCommand extends AbstractIslandTeamCommand {

    public IslandTeamLeaveCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "leave");
    }
    
    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setDescription("commands.island.team.leave.description");

    }

    @Override
    public boolean execute(User user, List<String> args) {
        // TODO Auto-generated method stub
        return false;
    }

}