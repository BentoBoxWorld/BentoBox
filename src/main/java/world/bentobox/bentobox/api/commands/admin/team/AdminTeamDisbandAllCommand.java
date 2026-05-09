package world.bentobox.bentobox.api.commands.admin.team;

import java.util.List;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Strips every member and sub-owner from every island in the current world. Trust and
 * coop ranks are not touched. Pre-existing islands keep their owner.
 * <p>
 * Intended migration tool when toggling on
 * {@link world.bentobox.bentobox.api.configuration.WorldSettings#isTeamsDisabled()}: legacy
 * team membership cannot be cleaned up by users any more, so an admin runs this once.
 *
 * @since 3.16.0
 */
public class AdminTeamDisbandAllCommand extends ConfirmableCommand {

    public AdminTeamDisbandAllCommand(CompositeCommand parent) {
        super(parent, "disbandall");
    }

    @Override
    public void setup() {
        setPermission("mod.team.disbandall");
        setDescription("commands.admin.team.disbandall.description");
        setOnlyPlayer(false);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        askConfirmation(user, user.getTranslation("commands.admin.team.disbandall.confirmation"),
                () -> disbandAll(user));
        return true;
    }

    private void disbandAll(User user) {
        int islandsAffected = 0;
        int playersRemoved = 0;
        for (Island island : getIslands().getIslands(getWorld())) {
            // Snapshot UUIDs first because removePlayer mutates the member map.
            List<UUID> toRemove = island
                    .getMemberSet(RanksManager.MEMBER_RANK, true)
                    .stream()
                    .filter(uuid -> island.getRank(uuid) < RanksManager.OWNER_RANK)
                    .toList();
            if (toRemove.isEmpty()) {
                continue;
            }
            islandsAffected++;
            for (UUID uuid : toRemove) {
                int previousRank = island.getRank(uuid);
                getIslands().removePlayer(island, uuid);
                playersRemoved++;
                TeamEvent.builder().island(island).reason(TeamEvent.Reason.KICK).involvedPlayer(uuid).admin(true)
                        .build();
                IslandEvent.builder().island(island).involvedPlayer(uuid).admin(true)
                        .reason(IslandEvent.Reason.RANK_CHANGE)
                        .rankChange(previousRank, RanksManager.VISITOR_RANK).build();
            }
        }
        user.sendMessage("commands.admin.team.disbandall.success",
                TextVariables.NUMBER, String.valueOf(playersRemoved),
                "[islands]", String.valueOf(islandsAffected));
    }
}
