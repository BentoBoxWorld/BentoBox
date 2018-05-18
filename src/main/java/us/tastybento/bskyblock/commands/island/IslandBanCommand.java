package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

public class IslandBanCommand extends CompositeCommand {

    public IslandBanCommand(CompositeCommand islandCommand) {
        super(islandCommand, "ban");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.ban");
        setOnlyPlayer(true);
        setParameters("commands.island.ban.parameters");
        setDescription("commands.island.ban.description");
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
        // Player cannot ban themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.ban.cannot-ban-yourself");
            return false;
        }
        if (getIslands().getMembers(user.getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("commands.island.ban.cannot-ban-member");
            return false; 
        }
        if (getIslands().getIsland(user.getWorld(), playerUUID).isBanned(targetUUID)) {
            user.sendMessage("commands.island.ban.player-already-banned");
            return false; 
        }        
        User target = User.getInstance(targetUUID);
        // Cannot ban ops
        if (target.isOp()) {
            user.sendMessage("commands.island.ban.cannot-ban");
            return false; 
        }
        // Finished error checking - start the banning
        return ban(user, target);
    }

    private boolean ban(User user, User targetUser) {
        Island island = getIslands().getIsland(user.getWorld(), user.getUniqueId());
        if (island.addToBanList(targetUser.getUniqueId())) {
            user.sendMessage("general.success");
            targetUser.sendMessage("commands.island.ban.owner-banned-you", "[owner]", user.getName());
            // If the player is online, has an island and on the banned island, move them home immediately
            if (targetUser.isOnline() && getIslands().hasIsland(user.getWorld(), targetUser.getUniqueId()) && island.onIsland(targetUser.getLocation())) {
                getIslands().homeTeleport(user.getWorld(), targetUser.getPlayer());
                island.getWorld().playSound(targetUser.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
            }            
            return true;
        }
        // Banning was blocked, maybe due to an event cancellation. Fail silently.
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        if (args.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        Island island = getIslands().getIsland(user.getWorld(), user.getUniqueId());
        List<String> options = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !p.getUniqueId().equals(user.getUniqueId()))
                .filter(p -> !island.isBanned(p.getUniqueId()))
                .filter(p -> user.getPlayer().canSee(p))
                .map(Player::getName).collect(Collectors.toList());
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
