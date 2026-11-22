package world.bentobox.bentobox.api.commands.island.team;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord.TemplateItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Handles the island team command system (/island team).
 * <p>
 * This is the parent command for all team-related operations including:
 * <ul>
 *   <li>Inviting players (invite, accept, reject)</li>
 *   <li>Team management (kick, leave, setowner)</li>
 *   <li>Role management (promote, demote, coop, trust)</li>
 *   <li>Team GUI panel system</li>
 * </ul>
 * <p>
 * Features:
 * <ul>
 *   <li>Persistent invite system</li>
 *   <li>Team size limits</li>
 *   <li>Role-based permissions</li>
 *   <li>Customizable GUI panel</li>
 * </ul>
 */
public class IslandTeamCommand extends CompositeCommand {

    /** Sub-commands for team management */
    private IslandTeamKickCommand kickCommand;

    private IslandTeamLeaveCommand leaveCommand;

    private IslandTeamSetownerCommand setOwnerCommand;

    /** Sub-commands for role management */
    private IslandTeamUncoopCommand uncoopCommand;

    private IslandTeamUntrustCommand unTrustCommand;

    private IslandTeamCoopCommand coopCommand;

    private IslandTeamTrustCommand trustCommand;

    /** Sub-commands for invite system */
    private IslandTeamInviteAcceptCommand acceptCommand;

    private IslandTeamInviteRejectCommand rejectCommand;

    private IslandTeamInviteCommand inviteCommand;

    /** GUI panel template items */
    private @Nullable TemplateItem border;

    private @Nullable TemplateItem background;

    /** Database handler for team invites */
    private final Database<TeamInvite> handler;

    public IslandTeamCommand(CompositeCommand parent) {
        super(parent, "team");
        handler = new Database<>(parent.getAddon(), TeamInvite.class);
    }

    /**
     * Sets up the team command system.
     * <p>
     * Initializes:
     * <ul>
     *   <li>Core team commands (invite, leave, setowner, kick)</li>
     *   <li>Invite management commands (accept, reject)</li>
     *   <li>Role commands if enabled (coop, trust)</li>
     *   <li>Rank commands (promote, demote)</li>
     *   <li>GUI panel template</li>
     * </ul>
     */
    @Override
    public void setup() {
        setPermission("island.team");
        setOnlyPlayer(true);
        setDescription("commands.island.team.description");
        // Register commands
        inviteCommand = new IslandTeamInviteCommand(this);
        leaveCommand = new IslandTeamLeaveCommand(this);
        setOwnerCommand = new IslandTeamSetownerCommand(this);
        kickCommand = new IslandTeamKickCommand(this);
        acceptCommand = new IslandTeamInviteAcceptCommand(this);
        rejectCommand = new IslandTeamInviteRejectCommand(this);
        new IslandTeamUninviteCommand(this);
        if (RanksManager.getInstance().rankExists(RanksManager.COOP_RANK_REF)) {
            coopCommand = new IslandTeamCoopCommand(this);
            uncoopCommand = new IslandTeamUncoopCommand(this);
        }
        if (RanksManager.getInstance().rankExists(RanksManager.TRUSTED_RANK_REF)) {
            trustCommand = new IslandTeamTrustCommand(this);
            unTrustCommand = new IslandTeamUntrustCommand(this);
        }
        new IslandTeamPromoteCommand(this, "promote");
        new IslandTeamPromoteCommand(this, "demote");

        // Panels
        if (!new File(getPlugin().getDataFolder() + File.separator + "panels", "team_panel.yml").exists()) {
            getPlugin().saveResource("panels/team_panel.yml", false);
        }
    }

    /**
     * Validates team command execution.
     * <p>
     * Checks:
     * <ul>
     *   <li>Player has an island or pending invite</li>
     *   <li>Team event isn't cancelled</li>
     *   <li>Team size limits for owners</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Player issuing the command must have an island
        Island island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        UUID playerUUID = user.getUniqueId();
        // Fire event so add-ons can run commands, etc.
        if (fireEvent(user, island)) {
            // Cancelled
            return false;
        }
        Set<UUID> teamMembers = getMembers(getWorld(), user);
        if (playerUUID.equals(island.getOwner())) {
            int maxSize = getIslands().getMaxMembers(island, RanksManager.MEMBER_RANK);
            if (teamMembers.size() < maxSize) {
                user.sendMessage("commands.island.team.invite.you-can-invite", TextVariables.NUMBER,
                        String.valueOf(maxSize - teamMembers.size()));
            } else {
                user.sendMessage("commands.island.team.invite.errors.island-is-full");
            }
        }
        return true;
    }

    /**
     * Opens the team management GUI panel.
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Show the panel
        new IslandTeamGUI(getPlugin(), this, user, getIslands().getPrimaryIsland(getWorld(), user.getUniqueId()))
                .build();
        return true;
    }

    private boolean fireEvent(User user, Island island) {
        IslandBaseEvent e = TeamEvent.builder().island(island).reason(TeamEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId()).build();
        return e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled());
    }

    /**
     * Manages the invite system in the database.
     * Methods handle:
     * <ul>
     *   <li>Adding new invites</li>
     *   <li>Validating existing invites</li>
     *   <li>Retrieving invite information</li>
     *   <li>Removing expired/accepted/rejected invites</li>
     * </ul>
     * @param type - type of invite
     * @param inviter - uuid of inviter
     * @param invitee - uuid of invitee
     * @since 1.8.0
     */
    public void addInvite(TeamInvite.Type type, @NonNull UUID inviter, @NonNull UUID invitee, @NonNull Island island) {
        handler.saveObjectAsync(new TeamInvite(type, inviter, invitee, island.getUniqueId()));
    }

    /**
     * Check if a player has been invited - validates any invite that may be in the system
     * @param invitee - UUID of invitee to check
     * @return true if invited, false if not
     * @since 1.8.0
     */
    public boolean isInvited(@NonNull UUID invitee) {
        boolean valid = false;
        if (handler.objectExists(invitee.toString())) {
            @Nullable
            TeamInvite invite = getInvite(invitee);
            valid = getIslands().getIslandById(invite.getIslandID()).map(island -> island.isOwned() // Still owned by someone
                    && !island.isDeleted() // Not deleted
                    && !island.isDeletable() // Not deletable
                    && island.getMemberSet().contains(invite.getInviter()) // the inviter is still a member of the island
            ).orElse(false) && !isExpired(invite);
            if (!valid) {
                // Remove invite
                handler.deleteObject(invite);
            }
        }
        return valid;
    }

    /**
     * Whether an invite has been open for longer than the configured
     * {@code island.invite-expiry-hours}. An invite stored before invites were
     * timestamped is stamped now, so it gets one full expiry period from the
     * first time it is looked at instead of lingering forever.
     *
     * @param invite the invite to check
     * @return true if the invite has run out
     */
    private boolean isExpired(@NonNull TeamInvite invite) {
        int hours = getSettings().getInviteExpiryHours();
        if (hours <= 0) {
            return false;
        }
        if (invite.getCreated() <= 0) {
            invite.setCreated(System.currentTimeMillis());
            handler.saveObjectAsync(invite);
            return false;
        }
        return hasExpired(invite.getCreated(), hours, System.currentTimeMillis());
    }

    /**
     * @param created when the invite was made, epoch milliseconds
     * @param hours how long invites stay open; 0 or less means forever
     * @param now the current time, epoch milliseconds
     * @return true if an invite made at {@code created} has run out by {@code now}
     */
    static boolean hasExpired(long created, int hours, long now) {
        return hours > 0 && now - created > TimeUnit.HOURS.toMillis(hours);
    }

    /**
     * Get whoever invited invitee.
     * @param invitee - uuid
     * @return UUID of inviter, or null if invitee has not been invited
     * @since 1.8.0
     */
    @Nullable
    public UUID getInviter(UUID invitee) {
        return isInvited(invitee) ? handler.loadObject(invitee.toString()).getInviter() : null;
    }

    /**
     * Gets the invite for an invitee.
     * @param invitee - UUID of invitee
     * @return invite or null if none
     * @since 1.8.0
     */
    @Nullable
    public TeamInvite getInvite(UUID invitee) {
        return handler.loadObject(invitee.toString());
    }

    /**
     * Gets the players who have an open invite to an island.
     * @param island the island
     * @return UUIDs of everyone with a valid, unexpired invite to the island
     * @since 3.24.0
     */
    @NonNull
    public List<UUID> getInvitees(@NonNull Island island) {
        return handler.loadObjects().stream()
                .filter(invite -> island.getUniqueId().equals(invite.getIslandID()))
                .map(TeamInvite::getInvitee)
                .filter(Objects::nonNull)
                .filter(this::isInvited)
                .toList();
    }

    /**
     * Removes a pending invite.
     * @param invitee - UUID of invited user
     * @since 1.8.0
     */
    public void removeInvite(@NonNull UUID invitee) {
        handler.deleteID(invitee.toString());
    }

    /**
     * @return the coopCommand
     */
    protected IslandTeamCoopCommand getCoopCommand() {
        return coopCommand;
    }

    /**
     * @return the trustCommand
     */
    protected IslandTeamTrustCommand getTrustCommand() {
        return trustCommand;
    }

    public IslandTeamInviteCommand getInviteCommand() {
        return inviteCommand;
    }

    public IslandTeamInviteAcceptCommand getAcceptCommand() {
        return acceptCommand;
    }

    public IslandTeamInviteRejectCommand getRejectCommand() {
        return rejectCommand;
    }

    /**
     * @return the kickCommand
     */
    public IslandTeamKickCommand getKickCommand() {
        return kickCommand;
    }

    /**
     * @return the leaveCommand
     */
    public IslandTeamLeaveCommand getLeaveCommand() {
        return leaveCommand;
    }

    /**
     * @return the setOwnerCommand
     */
    public IslandTeamSetownerCommand getSetOwnerCommand() {
        return setOwnerCommand;
    }

    /**
     * @return the uncoopCommand
     */
    public IslandTeamUncoopCommand getUncoopCommand() {
        return uncoopCommand;
    }

    /**
     * @return the unTrustCommand
     */
    public IslandTeamUntrustCommand getUnTrustCommand() {
        return unTrustCommand;
    }

}
