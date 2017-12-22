package us.tastybento.bskyblock.commands.admin;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;

public class AdminVersionCommand extends CompositeCommand {

    public AdminVersionCommand(CompositeCommand adminCommand) {
        super(adminCommand, "version");
    }

    @Override
    public boolean execute(User user, String[] args) {
        return false;
    }

    @Override
    public void setup() {
        // TODO Auto-generated method stub
        
    }
}
