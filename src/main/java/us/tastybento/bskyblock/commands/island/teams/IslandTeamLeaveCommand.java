package us.tastybento.bskyblock.commands.island.teams;

import java.util.List;

import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class IslandTeamLeaveCommand extends AbstractIslandTeamCommand {

    public IslandTeamLeaveCommand(IslandTeamCommand islandTeamCommand) {
        super(islandTeamCommand, "leave");
    }
    
    @Override
    public void setup() {
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);
        this.setDescription("commands.island.team.leave.description");

    }

    @Override
    public boolean execute(User user, List<String> args) {
        // TODO Auto-generated method stub
        return false;
    }

}