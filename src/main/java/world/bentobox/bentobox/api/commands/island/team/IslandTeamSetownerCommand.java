package world.bentobox.bentobox.api.commands.island.team;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class IslandTeamSetownerCommand extends CompositeCommand {

    public IslandTeamSetownerCommand(CompositeCommand islandTeamCommand) {
        super(islandTeamCommand, "setowner");
    }

    @Override
    public void setup() {
        setPermission("island.team.setowner");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.team.setowner.parameters");
        setDescription("commands.island.team.setowner.description");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        // Can use if in a team
        boolean inTeam = getPlugin().getIslands().inTeam(getWorld(), playerUUID);
        UUID ownerUUID = getOwner(getWorld(), user);
        if (!(inTeam && ownerUUID.equals(playerUUID))) {
            user.sendMessage("general.errors.not-owner");
            return false;
        }
        // If args are not right, show help
        if (args.size() != 1) {
            showHelp(this, user);
            return false;
        }
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        if (!getIslands().inTeam(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-team");
            return false;
        }
        if (targetUUID.equals(playerUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.cant-transfer-to-yourself");
            return false;
        }
        if (!getPlugin().getIslands().getMembers(getWorld(), playerUUID).contains(targetUUID)) {
            user.sendMessage("commands.island.team.setowner.errors.target-is-not-member");
            return false;
        }
        // Fire event so add-ons can run commands, etc.
        Island island = getIslands().getIsland(getWorld(), playerUUID);
        if (TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.SETOWNER)
                .involvedPlayer(targetUUID)
                .build()
                .isCancelled()) {
            return false;
        }
        getIslands().setOwner(getWorld(), user, targetUUID);
        // Call the event for the new owner
        IslandEvent.builder()
                .island(island)
                .involvedPlayer(targetUUID)
                .admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(island.getRank(User.getInstance(targetUUID)), RanksManager.OWNER_RANK)
                .build();
        // Call the event for the previous owner
        IslandEvent.builder()
                .island(island)
                .involvedPlayer(playerUUID)
                .admin(false)
                .reason(IslandEvent.Reason.RANK_CHANGE)
                .rankChange(RanksManager.OWNER_RANK, island.getRank(user))
                .build();
        getIslands().save(island);
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        List<String> options = new ArrayList<>();
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        for (UUID member : getPlugin().getIslands().getMembers(getWorld(), user.getUniqueId())) {
            options.add(Bukkit.getServer().getOfflinePlayer(member).getName());
        }
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}