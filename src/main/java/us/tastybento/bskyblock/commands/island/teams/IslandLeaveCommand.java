package us.tastybento.bskyblock.commands.island.teams;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

public class IslandLeaveCommand extends AbstractTeamCommand {

    public IslandLeaveCommand(CompositeCommand islandCommand) {
        super(islandCommand, "leave");
        this.setPermission(Settings.PERMPREFIX + "island.team");
        this.setOnlyPlayer(true);

    }

    @Override
    public boolean execute(User user, String[] args) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setup() {
        // TODO Auto-generated method stub
        
    }



}