package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

public class IslandUnbanCommand extends CompositeCommand {

    public IslandUnbanCommand(CompositeCommand islandCommand) {
        super(islandCommand, "unban", "pardon");
    }

    @Override
    public void setup() {
        setPermission("island.ban");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.unban.parameters");
        setDescription("commands.island.unban.description");
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
        // Player cannot unban themselves
        if (playerUUID.equals(targetUUID)) {
            user.sendMessage("commands.island.unban.cannot-unban-yourself");
            return false;
        }
        if (!getIslands().getIsland(getWorld(), playerUUID).isBanned(targetUUID)) {
            user.sendMessage("commands.island.unban.player-not-banned");
            return false;
        }
        // Finished error checking - start the unbanning
        User targetUser = User.getInstance(targetUUID);
        return unban(user, targetUser);
    }

    private boolean unban(User issuer, User target) {
        Island island = getIslands().getIsland(getWorld(), issuer.getUniqueId());

        // Run the event
        IslandBaseEvent unbanEvent = IslandEvent.builder()
                .island(island)
                .involvedPlayer(target.getUniqueId())
                .admin(false)
                .reason(IslandEvent.Reason.UNBAN)
                .build();

        // Event is not cancelled
        if (!unbanEvent.isCancelled() && island.unban(issuer.getUniqueId(), target.getUniqueId())) {
            issuer.sendMessage("general.success");
            target.sendMessage("commands.island.unban.you-are-unbanned", TextVariables.NAME, issuer.getName());
            // Set cooldown
            if (getSettings().getBanCooldown() > 0 && getParent() != null) {
                getParent().getSubCommand("ban").ifPresent(subCommand ->
                        subCommand.setCooldown(issuer.getUniqueId(), target.getUniqueId(), getSettings().getBanCooldown() * 60));
            }
            return true;
        }
        // Unbanning was blocked, maybe due to an event cancellation. Fail silently.
        return false;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            List<String> options = island.getBanned().stream().map(getPlayers()::getName).collect(Collectors.toList());
            String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
            return Optional.of(Util.tabLimit(options, lastArg));
        } else {
            return Optional.empty();
        }
    }
}
