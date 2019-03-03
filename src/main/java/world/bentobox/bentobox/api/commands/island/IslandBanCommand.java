package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class IslandBanCommand extends CompositeCommand {

    public IslandBanCommand(CompositeCommand islandCommand) {
        super(islandCommand, "ban");
    }

    @Override
    public void setup() {
        setPermission("island.ban");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.ban.parameters");
        setDescription("commands.island.ban.description");
        setConfigurableRankCommand();
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() != 1) {
            // Show help
            showHelp(this, user);
            return false;
        }
        UUID playerUUID = user.getUniqueId();
        // Player issuing the command must have an island or be in a team
        if (!getIslands().inTeam(getWorld(), user.getUniqueId()) && !getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        if (getIslands().getIsland(getWorld(), user).getRank(user) < getPlugin().getSettings().getRankCommand(getUsage())) {
            user.sendMessage("general.errors.no-permission");
            return false;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Player cannot ban themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.ban.cannot-ban-yourself");
            return false;
        }
        if (getIslands().getMembers(getWorld(), user.getUniqueId()).contains(targetUUID)) {
            user.sendMessage("commands.island.ban.cannot-ban-member");
            return false;
        }
        if (getIslands().getIsland(getWorld(), playerUUID).isBanned(targetUUID)) {
            user.sendMessage("commands.island.ban.player-already-banned");
            return false;
        }
        if (getSettings().getBanCooldown() > 0 && checkCooldown(user, targetUUID)) {
            return false;
        }
        User target = User.getInstance(targetUUID);
        // Cannot ban ops
        if (target.hasPermission("admin.noban")) {
            user.sendMessage("commands.island.ban.cannot-ban");
            return false;
        }
        // Finished error checking - start the banning
        return ban(user, target);
    }

    private boolean ban(User issuer, User target) {
        Island island = getIslands().getIsland(getWorld(), issuer.getUniqueId());

        // Check if player can ban any more players
        int banLimit = issuer.getPermissionValue(getPermissionPrefix() + "ban.maxlimit", getIWM().getBanLimit(getWorld()));
        if (banLimit <= -1 || island.getBanned().size() < banLimit) {
            // Run the event
            IslandBaseEvent banEvent = IslandEvent.builder()
                    .island(island)
                    .involvedPlayer(target.getUniqueId())
                    .admin(false)
                    .reason(IslandEvent.Reason.BAN)
                    .build();

            // Event is not cancelled
            if (!banEvent.isCancelled() && island.ban(issuer.getUniqueId(), target.getUniqueId())) {
                issuer.sendMessage("general.success");
                target.sendMessage("commands.island.ban.owner-banned-you", TextVariables.NAME, issuer.getName());
                // If the player is online, has an island and on the banned island, move them home immediately
                if (target.isOnline() && getIslands().hasIsland(getWorld(), target.getUniqueId()) && island.onIsland(target.getLocation())) {
                    getIslands().homeTeleport(getWorld(), target.getPlayer());
                    island.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
                }
                return true;
            }
        } else {
            issuer.sendMessage("commands.island.ban.cannot-ban-more-players");
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
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(user.getUniqueId()))
                    .filter(p -> !island.isBanned(p.getUniqueId()))
                    .filter(p -> user.getPlayer().canSee(p))
                    .map(Player::getName).collect(Collectors.toList());
            String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
