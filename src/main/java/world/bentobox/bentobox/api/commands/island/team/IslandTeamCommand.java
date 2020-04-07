package world.bentobox.bentobox.api.commands.island.team;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

public class IslandTeamCommand extends CompositeCommand {

    /**
     * Invited list. Key is the invited party, value is the invite.
     * @since 1.8.0
     */
    private Map<UUID, Invite> inviteMap;

    private IslandTeamInviteCommand inviteCommand;

    public IslandTeamCommand(CompositeCommand parent) {
        super(parent, "team");
        inviteMap = new HashMap<>();
    }

    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.description");
        // Register commands
        inviteCommand = new IslandTeamInviteCommand(this);
        new IslandTeamLeaveCommand(this);
        new IslandTeamSetownerCommand(this);
        new IslandTeamKickCommand(this);
        new IslandTeamInviteAcceptCommand(this);
        new IslandTeamInviteRejectCommand(this);
        new IslandTeamCoopCommand(this);
        new IslandTeamUncoopCommand(this);
        new IslandTeamTrustCommand(this);
        new IslandTeamUntrustCommand(this);
        new IslandTeamPromoteCommand(this, "promote");
        new IslandTeamPromoteCommand(this, "demote");
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Player issuing the command must have an island
        UUID ownerUUID = getOwner(getWorld(), user);
        if (ownerUUID == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        UUID playerUUID = user.getUniqueId();
        // Fire event so add-ons can run commands, etc.
        if (fireEvent(user)) {
            // Cancelled
            return false;
        }
        Set<UUID> teamMembers = getMembers(getWorld(), user);
        if (ownerUUID.equals(playerUUID)) {
            int maxSize = inviteCommand.getMaxTeamSize(user);
            if (teamMembers.size() < maxSize) {
                user.sendMessage("commands.island.team.invite.you-can-invite", TextVariables.NUMBER, String.valueOf(maxSize - teamMembers.size()));
            } else {
                user.sendMessage("commands.island.team.invite.errors.island-is-full");
            }
        }
        // Show members of island
        showMembers(getIslands().getIsland(getWorld(), playerUUID), user);
        return true;
    }

    private void showMembers(Island island, User user) {
        // Gather online members
        List<UUID> onlineMembers = island
                .getMemberSet(RanksManager.MEMBER_RANK)
                .stream()
                .filter(uuid -> Bukkit.getOfflinePlayer(uuid)
                        .isOnline())
                .collect(Collectors.toList());

        // List of ranks that we will loop through
        Integer[] ranks = new Integer[]{RanksManager.OWNER_RANK, RanksManager.SUB_OWNER_RANK, RanksManager.MEMBER_RANK, RanksManager.TRUSTED_RANK, RanksManager.COOP_RANK};

        // Show header:
        user.sendMessage("commands.island.team.info.header",
                "[max]", String.valueOf(inviteCommand.getMaxTeamSize(user)),
                "[total]", String.valueOf(island.getMemberSet().size()),
                "[online]", String.valueOf(onlineMembers.size()));

        // We now need to get all online "members" of the island - incl. Trusted and coop
        onlineMembers = island.getMemberSet(RanksManager.COOP_RANK).stream()
                .filter(uuid -> Bukkit.getOfflinePlayer(uuid).isOnline()).collect(Collectors.toList());

        for (int rank : ranks) {
            Set<UUID> players = island.getMemberSet(rank, false);
            if (!players.isEmpty()) {
                if (rank == RanksManager.OWNER_RANK) {
                    // Slightly special handling for the owner rank
                    user.sendMessage("commands.island.team.info.rank-layout.owner",
                            TextVariables.RANK, user.getTranslation(RanksManager.OWNER_RANK_REF));
                } else {
                    user.sendMessage("commands.island.team.info.rank-layout.generic",
                            TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)),
                            TextVariables.NUMBER, String.valueOf(island.getMemberSet(rank, false).size()));
                }
                for (UUID member : island.getMemberSet(rank, false)) {
                    OfflinePlayer offlineMember = Bukkit.getOfflinePlayer(member);
                    if (onlineMembers.contains(member)) {
                        // the player is online
                        user.sendMessage("commands.island.team.info.member-layout.online",
                                TextVariables.NAME, offlineMember.getName());
                    } else {
                        // A bit of handling for the last joined date
                        Instant lastJoined = Instant.ofEpochMilli(offlineMember.getLastPlayed());
                        Instant now = Instant.now();

                        Duration duration = Duration.between(lastJoined, now);
                        String lastSeen;
                        final String reference = "commands.island.team.info.last-seen.layout";
                        if (duration.toMinutes() < 60L) {
                            lastSeen = user.getTranslation(reference,
                                    TextVariables.NUMBER, String.valueOf(duration.toMinutes()),
                                    TextVariables.UNIT, user.getTranslation("commands.island.team.info.last-seen.minutes"));
                        } else if (duration.toHours() < 24L) {
                            lastSeen = user.getTranslation(reference,
                                    TextVariables.NUMBER, String.valueOf(duration.toHours()),
                                    TextVariables.UNIT, user.getTranslation("commands.island.team.info.last-seen.hours"));
                        } else {
                            lastSeen = user.getTranslation(reference,
                                    TextVariables.NUMBER, String.valueOf(duration.toDays()),
                                    TextVariables.UNIT, user.getTranslation("commands.island.team.info.last-seen.days"));
                        }

                        user.sendMessage("commands.island.team.info.member-layout.offline",
                                TextVariables.NAME, offlineMember.getName(),
                                "[last_seen]", lastSeen);
                    }
                }
            }
        }
    }

    private boolean fireEvent(User user) {
        return TeamEvent.builder()
                .island(getIslands()
                        .getIsland(getWorld(), user.getUniqueId()))
                .reason(TeamEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId())
                .build()
                .isCancelled();
    }

    /**
     * Add an invite
     * @param type - type of invite
     * @param inviter - uuid of inviter
     * @param invitee - uuid of invitee
     * @since 1.8.0
     */
    public void addInvite(Invite.Type type, @NonNull UUID inviter, @NonNull UUID invitee) {
        inviteMap.put(invitee, new Invite(type, inviter, invitee));
    }

    /**
     * Check if a player has been invited
     * @param invitee - UUID of invitee to check
     * @return true if invited, false if not
     * @since 1.8.0
     */
    public boolean isInvited(@NonNull UUID invitee) {
        return inviteMap.containsKey(invitee);
    }

    /**
     * Get whoever invited invitee
     * @param invitee - uuid
     * @return UUID of inviter, or null if invitee has not been invited
     * @since 1.8.0
     */
    @Nullable
    public UUID getInviter(UUID invitee) {
        return isInvited(invitee) ? inviteMap.get(invitee).getInviter() : null;
    }

    /**
     * Gets the invite for an invitee.
     * @param invitee - UUID of invitee
     * @return invite or null if none
     * @since 1.8.0
     */
    @Nullable
    public Invite getInvite(UUID invitee) {
        return inviteMap.get(invitee);
    }

    /**
     * Removes a pending invite.
     * @param invitee - UUID of invited user
     * @since 1.8.0
     */
    public void removeInvite(@NonNull UUID invitee) {
        inviteMap.remove(invitee);
    }
}
