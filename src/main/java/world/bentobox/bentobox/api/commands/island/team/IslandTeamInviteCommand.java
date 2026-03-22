package world.bentobox.bentobox.api.commands.island.team;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord.TemplateItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * Handles team invitation commands in BentoBox.
 * <p>
 * Features:
 * <ul>
 *   <li>Invite players to join island teams</li>
 *   <li>GUI-based player selection</li>
 *   <li>Invitation cooldowns</li>
 *   <li>Rank-based permission control</li>
 *   <li>Team size limits</li>
 * </ul>
 * <p>
 * Restrictions:
 * <ul>
 *   <li>Players must have sufficient rank to invite</li>
 *   <li>Team must have space for new members</li>
 *   <li>Cannot invite players already on teams (configurable)</li>
 *   <li>Cannot invite offline players</li>
 *   <li>Cannot invite self</li>
 *   <li>Only one active invite per player</li>
 * </ul>
 */
public class IslandTeamInviteCommand extends CompositeCommand {

    /** Parent team command reference */
    private final IslandTeamCommand itc;
    
    /** Currently invited player - used between canExecute and execute */
    private @Nullable User invitedPlayer;
    
    /** GUI template items */
    private @Nullable TemplateItem border;
    private @Nullable TemplateItem background;

    public IslandTeamInviteCommand(IslandTeamCommand parent) {
        super(parent, "invite");
        itc = parent;
    }

    @Override
    public void setup() {
        setPermission("island.team.invite");
        setOnlyPlayer(true);
        setDescription("commands.island.team.invite.description");
        setConfigurableRankCommand();
        // Panels
        if (!new File(getPlugin().getDataFolder() + File.separator + "panels", "team_invite_panel.yml").exists()) {
            getPlugin().saveResource("panels/team_invite_panel.yml", false);
        }
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        UUID playerUUID = user.getUniqueId();
        IslandsManager islandsManager = getIslands();

        // Player issuing the command must have an island or be in a team
        if (!islandsManager.inTeam(getWorld(), playerUUID) && !islandsManager.hasIsland(getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        Island island = islandsManager.getIsland(getWorld(), user);

        if (args.size() != 1) {
            new IslandTeamInviteGUI(itc, true, island).build(user);
            return false;
        }

        int rank = Objects.requireNonNull(island).getRank(user);

        return checkRankAndInvitePlayer(user, island, rank, args.getFirst());
    }

    /**
     * Validates invitation requirements:
     * - User has required rank
     * - Team has space
     * - Target player exists and is online
     * - No existing invite
     * - Team membership restrictions
     * 
     * @param user command issuer
     * @param island user's island
     * @param rank user's rank
     * @param playerName target player name
     * @return true if invite can proceed, false if requirements not met
     */
    private boolean checkRankAndInvitePlayer(User user, Island island, int rank, String playerName) {
        PlayersManager playersManager = getPlayers();
        UUID playerUUID = user.getUniqueId();

        // Check rank to use command
        int requiredRank = island.getRankCommand(getUsage());
        if (rank < requiredRank) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }

        // Check for space on team
        int maxMembers = getIslands().getMaxMembers(island, RanksManager.MEMBER_RANK);
        if (island.getMemberSet().size() >= maxMembers) {
            user.sendMessage("commands.island.team.invite.errors.island-is-full");
            return false;
        }

        UUID invitedPlayerUUID = playersManager.getUUID(playerName);
        if (invitedPlayerUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, playerName);
            return false;
        }
        // Write to field as this is used by execute method
        invitedPlayer = User.getInstance(invitedPlayerUUID);
        if (!canInvitePlayer(user, invitedPlayer)) {
            return false;
        }

        // Check cooldown
        if (this.getSettings().getInviteCooldown() > 0 && checkCooldown(user, island.getUniqueId(), invitedPlayerUUID.toString())) {
            return false;
        }

        // Player cannot invite someone already on a team
        if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()
                && getIslands().inTeam(getWorld(), invitedPlayerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.already-on-team");
            return false;
        }

        if (isInvitedByUser(invitedPlayerUUID, playerUUID) && isInviteTypeTeam(invitedPlayerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.you-have-already-invited");
            return false;
        }

        return true;
    }

    /**
     * Validates player-specific invite conditions:
     * - Player is online and visible
     * - Not inviting self
     */
    private boolean canInvitePlayer(User user, User invitedPlayer) {
        UUID playerUUID = user.getUniqueId();
        if (!invitedPlayer.isOnline() || !user.getPlayer().canSee(invitedPlayer.getPlayer())) {
            user.sendMessage("general.errors.offline-player");
            return false;
        }
        if (playerUUID.equals(invitedPlayer.getUniqueId())) {
            user.sendMessage("commands.island.team.invite.errors.cannot-invite-self");
            return false;
        }
        return true;
    }

    private boolean isInvitedByUser(UUID invitedPlayerUUID, UUID inviterUUID) {
        return itc.isInvited(invitedPlayerUUID) && itc.getInviter(invitedPlayerUUID).equals(inviterUUID);
    }

    private boolean isInviteTypeTeam(UUID invitedPlayerUUID) {
        return Objects.requireNonNull(itc.getInvite(invitedPlayerUUID)).getType().equals(Type.TEAM);
    }

    /**
     * Process the invite:
     * - Cancels any existing invite
     * - Fires team invite event
     * - Sends invite messages
     * - Warns about island loss if applicable
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Rare case when invited player is null. Could be a race condition.
        if (invitedPlayer == null) return false;
        // If that player already has an invitation out then retract it.
        // Players can only have one invite one at a time - interesting
        if (itc.isInvited(invitedPlayer.getUniqueId())) {
            itc.removeInvite(invitedPlayer.getUniqueId());
            user.sendMessage("commands.island.team.invite.removing-invite");
        }
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            invitedPlayer = null;
            return false;
        }
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent e = TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.INVITE)
                .involvedPlayer(invitedPlayer.getUniqueId())
                .build();
        if (e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled())) {
            invitedPlayer = null;
            return false;
        }
        // Put the invited player (key) onto the list with inviter (value)
        // If someone else has invited a player, then this invite will overwrite the previous invite!
        itc.addInvite(Type.TEAM, user.getUniqueId(), invitedPlayer.getUniqueId(), island);
        user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, invitedPlayer.getName(), TextVariables.DISPLAY_NAME, invitedPlayer.getDisplayName());
        // Send message to online player
        invitedPlayer.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
        invitedPlayer.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, getTopLabel());
        if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()
                && getIslands().hasIsland(getWorld(), invitedPlayer.getUniqueId())) {
            invitedPlayer.sendMessage("commands.island.team.invite.you-will-lose-your-island");
        }
        invitedPlayer = null;
        return true;
    }

    /**
     * Provides tab completion for online player names.
     * Requires at least first letter to avoid showing all players.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (lastArg.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

}
