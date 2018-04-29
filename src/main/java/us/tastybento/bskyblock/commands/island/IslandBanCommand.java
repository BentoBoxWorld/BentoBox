package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

public class IslandBanCommand extends CompositeCommand {

    public IslandBanCommand(CompositeCommand islandCommand) {
        super(islandCommand, "ban");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.ban");
        setOnlyPlayer(true);
        setParameters("command.island.ban.parameters");
        setDescription("commands.island.ban.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island
        if (!getIslands().hasIsland(playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().isOwner(playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        if (args.isEmpty() || args.size() > 1) {
            // Show help
            showHelp(this, user);
            return false;
        } else  {
            // Get target player
            UUID targetUUID = getPlayers().getUUID(args.get(0));
            if (targetUUID == null) {
                user.sendMessage("general.errors.unknown-player");
                return false;
            }
            // Player cannot ban themselves
            if (playerUUID.equals(targetUUID)) {
                user.sendMessage("commands.island.ban.cannot-ban-yourself");
                return false;
            }
            if (getIslands().getMembers(user.getUniqueId()).contains(targetUUID)) {
                user.sendMessage("commands.island.ban.cannot-ban-member");
                return false; 
            }
            if (getIslands().getIsland(playerUUID).isBanned(targetUUID)) {
                user.sendMessage("commands.island.ban.player-already-banned");
                return false; 
            }
            User target = User.getInstance(targetUUID);
            // Cannot ban ops
            if (!target.isPlayer() || target.isOp()) {
                user.sendMessage("commands.island.ban.cannot-ban");
                return false; 
            }
            
            User targetUser = User.getInstance(targetUUID);
            // Finished error checking - start the banning
            if (getIslands().getIsland(playerUUID).addToBanList(targetUUID)) {
                user.sendMessage("general.success");
                targetUser.sendMessage("commands.island.ban.you-are-banned", "[owner]", user.getName());
                if (target.isOnline()) {
                    // Remove from island
                    if (getPlayers().hasIsland(targetUUID)) {
                        getIslands().homeTeleport(target.getPlayer());
                    }
                    // TODO else if there is a spawn, send them there
                }
                return true;
            }
            // Banning was blocked, maybe due to an event cancellation. Fail silently.
        }
        return false;
    }

}
