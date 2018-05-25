package us.tastybento.bskyblock.commands.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

public class AdminRegisterCommand extends CompositeCommand {

    public AdminRegisterCommand(CompositeCommand parent) {
        super(parent, "unregister");
    }
    
    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "admin.unregister");
        setOnlyPlayer(true);
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
        if (getIslands().hasIsland(world, targetUUID)) {
            user.sendMessage("general.errors.player-has-island");
            return false;
        }
        if (getIslands().inTeam(world, targetUUID)) {
            user.sendMessage("commands.admin.register.cannot-register-team-player");
            return false;
        }
        // Check if island is owned
        Optional<Island> island = getIslands().getIslandAt(user.getLocation());
        if (island.map(i -> i.getOwner() != null).orElse(false)) {
            user.sendMessage("commands.admin.register.already-owned");
            return false;
        }
        // Register island if it exists
        return island.map(i -> {
            // Island exists
            getIslands().makeLeader(user, targetUUID, i);
            user.sendMessage("commands.admin.register.registered-island", "[xyz]", Util.xyz(i.getCenter().toVector()));
            user.sendMessage("general.success");
            return true;
        }).orElse(false); // Island does not exist
    }
}