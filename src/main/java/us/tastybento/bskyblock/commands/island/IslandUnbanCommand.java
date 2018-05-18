package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

public class IslandUnbanCommand extends CompositeCommand {

    public IslandUnbanCommand(CompositeCommand islandCommand) {
        super(islandCommand, "unban");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.ban");
        setOnlyPlayer(true);
        setParameters("commands.island.unban.parameters");
        setDescription("commands.island.unban.description");
    }

    @Override
    public boolean execute(User user, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        } 
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island
        if (!getIslands().hasIsland(user.getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!getIslands().isOwner(user.getWorld(), playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player");
            return false;
        }
        // Player cannot unban themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.unban.cannot-unban-yourself");
            return false;
        }
        if (!getIslands().getIsland(user.getWorld(), playerUUID).isBanned(targetUUID)) {
            user.sendMessage("commands.island.unban.player-not-banned");
            return false; 
        }
        // Finished error checking - start the unbanning
        User targetUser = User.getInstance(targetUUID);
        return unban(user, targetUser);
    }

    private boolean unban(User user, User targetUser) {
        if (getIslands().getIsland(user.getWorld(), user.getUniqueId()).removeFromBanList(targetUser.getUniqueId())) {
            user.sendMessage("general.success");
            targetUser.sendMessage("commands.island.unban.you-are-unbanned", "[owner]", user.getName());
            return true;
        }
        // Unbanning was blocked, maybe due to an event cancellation. Fail silently.
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {       
        Island island = getIslands().getIsland(user.getWorld(), user.getUniqueId());
        List<String> options = island.getBanned().stream().map(getPlayers()::getName).collect(Collectors.toList());
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
