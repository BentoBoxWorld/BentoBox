package us.tastybento.bskyblock.commands.admin;

import java.util.List;
import java.util.UUID;

import org.bukkit.World;

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
        // TODO: fix world
        World world = getPlugin().getIWM().getIslandWorld();

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
        if (!getIslands().hasIsland(world, targetUUID)) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (getIslands().inTeam(world, targetUUID)) {
            user.sendMessage("commands.admin.unregister.cannot-unregister-team-player");
            return false;
        }
        // Unregister island
        user.sendMessage("commands.admin.unregister.unregistered-island", "[xyz]", Util.xyz(getIslands().getIsland(world, targetUUID).getCenter().toVector()));
        getIslands().removePlayer(world, targetUUID);
        user.sendMessage("general.success");
        return true;
    }
}