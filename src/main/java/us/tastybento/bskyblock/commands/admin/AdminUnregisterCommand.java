package us.tastybento.bskyblock.commands.admin;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.util.Util;

public class AdminUnregisterCommand extends CompositeCommand {

    public AdminUnregisterCommand(CompositeCommand parent) {
        super(parent, "unregister");
    }
    
    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.unregister");
        setParameters("commands.admin.unregister.parameters");
        setDescription("commands.admin.unregister.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        // Get target
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        if (!getIslands().hasIsland(targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (getIslands().inTeam(targetUUID)) {
            user.sendMessage("commands.admin.unregister.cannot-unregister-team-player");
            return false;
        }
        // Unregister island
        user.sendMessage("commands.admin.unregister.unregistered-island", "[xyz]", Util.xyz(getIslands().getIsland(targetUUID).getCenter().toVector()));
        getIslands().removePlayer(targetUUID);
        user.sendMessage("general.success");
        return true;
    }
}