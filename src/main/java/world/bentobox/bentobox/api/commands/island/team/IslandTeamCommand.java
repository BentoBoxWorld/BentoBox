package world.bentobox.bentobox.api.commands.island.team;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.TemplatedPanel.ItemSlot;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord.ActionRecords;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class IslandTeamCommand extends CompositeCommand {

    /**
     * List of ranks that we will loop through in order
     */
    private static final List<Integer> RANKS = List.of(RanksManager.OWNER_RANK, RanksManager.SUB_OWNER_RANK,
            RanksManager.MEMBER_RANK, RanksManager.TRUSTED_RANK, RanksManager.COOP_RANK);

    /**
     * Invited list. Key is the invited party, value is the invite.
     * @since 1.8.0
     */
    private final Map<UUID, Invite> inviteMap;

    private User user;

    private Island island;

    private int rank = RanksManager.OWNER_RANK;

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
        new IslandTeamInviteCommand(this);
        new IslandTeamLeaveCommand(this);
        new IslandTeamSetownerCommand(this);
        new IslandTeamKickCommand(this);
        new IslandTeamInviteAcceptCommand(this);
        new IslandTeamInviteRejectCommand(this);
        if (RanksManager.getInstance().rankExists(RanksManager.COOP_RANK_REF)) {
            new IslandTeamCoopCommand(this);
            new IslandTeamUncoopCommand(this);
        }
        if (RanksManager.getInstance().rankExists(RanksManager.TRUSTED_RANK_REF)) {
            new IslandTeamTrustCommand(this);
            new IslandTeamUntrustCommand(this);
        }
        new IslandTeamPromoteCommand(this, "promote");
        new IslandTeamPromoteCommand(this, "demote");

        // Panels
        getPlugin().saveResource("panels/team_panel.yml", false);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.user = user;
        // Player issuing the command must have an island
        island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
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
        // Show members of island
        showMembers().forEach(user::sendRawMessage);
        build();
        return true;
    }

    /**
     * This method builds this GUI.
     */
    private void build() {
        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.user(user);
        panelBuilder.world(user.getWorld());

        panelBuilder.template("team_panel", new File(getPlugin().getDataFolder(), "panels"));

        panelBuilder.parameters("[name]", user.getName(), "[display_name]", user.getDisplayName());

        panelBuilder.registerTypeBuilder("STATUS", this::createStatusButton);
        panelBuilder.registerTypeBuilder("MEMBER", this::createMemberButton);
        panelBuilder.registerTypeBuilder("INVITE", this::createInviteButton);
        panelBuilder.registerTypeBuilder("RANK", this::createRankButton);
        //panelBuilder.registerTypeBuilder("KICK", this::createKickButton);

        // Register unknown type builder.
        panelBuilder.build();
    }

    private PanelItem createRankButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        PanelItemBuilder builder = new PanelItemBuilder();
        builder.name("Rank");
        builder.icon(Material.AMETHYST_SHARD);
        builder.description("Rank shown = " + user.getTranslation(RanksManager.getInstance().getRank(rank)));
        builder.clickHandler((panel, user, clickType, clickSlot) -> {
            BentoBox.getInstance().logDebug("Rank = " + rank);
            if (clickType.equals(ClickType.RIGHT)) {
                rank = RanksManager.getInstance().getRankDownValue(rank);

            } else {
                rank = RanksManager.getInstance().getRankUpValue(rank);
            }
            BentoBox.getInstance().logDebug("New Rank = " + rank);
            // Update panel after click
            build();
            return true;
        });

        return builder.build();
    }
    /**
     * Create invite button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createInviteButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        PanelItemBuilder builder = new PanelItemBuilder();
        // Player issuing the command must have an island
        Island island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        if (island == null) {
            return builder.icon(Material.BARRIER).name(user.getTranslation("general.errors.no-island")).build();
        }
        // The player must be able to invite a player

        return builder.icon(user.getName()).name(user.getTranslation("commands.island.team.gui.buttons.status.name"))
                .description(showMembers()).build();
    }

    /**
     * Create status button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createStatusButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        PanelItemBuilder builder = new PanelItemBuilder();
        // Player issuing the command must have an island
        Island island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        if (island == null) {
            return builder.icon(Material.BARRIER).name(user.getTranslation("general.errors.no-island")).build();
        }

        return builder.icon(user.getName()).name(user.getTranslation("commands.island.team.gui.buttons.status.name"))
                .description(showMembers()).build();
    }

    /**
     * Create member button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createMemberButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        // Player issuing the command must have an island
        Island island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        if (island == null) {
            return new PanelItemBuilder().icon(Material.BARRIER).name(user.getTranslation("general.errors.no-island"))
                    .build();
        }
        return switch (rank) {
        case RanksManager.OWNER_RANK -> ownerView(template, slot);
        default -> getMemberButton(rank, slot.slot(), template.actions());
        };
    }

    /**
     * The owner view shows all the ranks, in order
     * @param template template reference
     * @param slot slot to show
     * @return panel item
     */
    private PanelItem ownerView(ItemTemplateRecord template, ItemSlot slot) {
        if (slot.slot() == 0 && island.getOwner() != null) {
            // Owner
            PanelItem item = getMemberButton(RanksManager.OWNER_RANK, 1, template.actions());
            if (item != null) {
                return item;
            }
        }
        long subOwnerCount = island.getMemberSet(RanksManager.SUB_OWNER_RANK, false).stream().count();
        long memberCount = island.getMemberSet(RanksManager.MEMBER_RANK, false).stream().count();
        long coopCount = island.getMemberSet(RanksManager.COOP_RANK, false).stream().count();
        long trustedCount = island.getMemberSet(RanksManager.TRUSTED_RANK, false).stream().count();

        if (slot.slot() > 0 && slot.slot() < subOwnerCount + 1) {
            // Show sub owners
            PanelItem item = getMemberButton(RanksManager.SUB_OWNER_RANK, slot.slot(), template.actions());
            if (item != null) {
                return item;
            }

        }
        if (slot.slot() > subOwnerCount && slot.slot() < subOwnerCount + memberCount + 1) {
            // Show members 
            PanelItem item = getMemberButton(RanksManager.MEMBER_RANK, slot.slot(), template.actions());
            if (item != null) {
                return item;
            }
        }
        if (slot.slot() > subOwnerCount + memberCount && slot.slot() < subOwnerCount + memberCount + trustedCount + 1) {
            // Show trusted
            PanelItem item = getMemberButton(RanksManager.TRUSTED_RANK, slot.slot(), template.actions());
            if (item != null) {
                return item;
            }

        }
        if (slot.slot() > subOwnerCount + memberCount + trustedCount
                && slot.slot() < subOwnerCount + memberCount + trustedCount + coopCount + 1) {
            // Show coops
            PanelItem item = getMemberButton(RanksManager.COOP_RANK, slot.slot(), template.actions());
            if (item != null) {
                return item;
            }

        }
        return new PanelItemBuilder().icon(Material.BLACK_STAINED_GLASS_PANE).name("&b&r").build();

    }

    /**
     * Shows a member's head
     * @param rank - the rank to show
     * @param slot - the slot number
     * @param actions - actions that need to apply to this member button as provided by the template
     * @return panel item
     */
    private PanelItem getMemberButton(int rank, int slot, List<ActionRecords> actions) {
        if (slot == 0 && island.getOwner() != null) {
            // Owner
            return getMemberButton(RanksManager.OWNER_RANK, 1, actions);
        }
        long count = island.getMemberSet(rank, false).size();
        String ref = RanksManager.getInstance().getRank(rank);
        User player = island.getMemberSet(rank, false).stream().sorted().skip(slot - 1L).limit(1L)
                .map(User::getInstance).findFirst().orElse(null);
        if (player != null) {
            if (player.isOnline()) {
                return new PanelItemBuilder().icon(player.getName()).name(player.getDisplayName())
                        .description(
                                user.getTranslation("commands.island.team.info.rank-layout.generic", TextVariables.RANK,
                                        user.getTranslation(ref), TextVariables.NUMBER, String.valueOf(count)))
                        .clickHandler((panel, user, clickType, i) -> clickListener(panel, user, clickType, i, player,
                                actions))
                        .build();
            } else {
                // Offline player
                return new PanelItemBuilder().icon(player.getName()).name(player.getDisplayName())
                        .description(offlinePlayerStatus(user, Bukkit.getOfflinePlayer(player.getUniqueId())))
                        .clickHandler((panel, user, clickType, i) -> clickListener(panel, user, clickType, i, player,
                                actions))
                        .build();
            }
        }
        return null;
    }

    private boolean clickListener(Panel panel, User user, ClickType clickType, int i, User player,
            List<ActionRecords> actions) {
        for (ItemTemplateRecord.ActionRecords action : actions) {
            if (clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN) {
                switch (action.actionType().toUpperCase(Locale.ENGLISH)) {
                case "KICK" -> {
                    // Kick the player
                    if (!player.equals(user)) {
                        this.user.closeInventory();
                        BentoBox.getInstance()
                                .logDebug(this.getTopLabel() + " " + this.getLabel() + " kick " + player.getName());
                        user.performCommand(this.getTopLabel() + " " + this.getLabel() + " kick " + player.getName());
                    }

                }
                }
            }
        }
        return true;
    }

    private List<String> showMembers() {
        List<String> message = new ArrayList<>();
        // Gather online members
        long onlineMemberCount = island.getMemberSet(RanksManager.MEMBER_RANK).stream()
                .filter(uuid -> Util.getOnlinePlayerList(user).contains(Bukkit.getOfflinePlayer(uuid).getName()))
                .count();

        // Show header:
        message.add(user.getTranslation("commands.island.team.info.header", "[max]",
                String.valueOf(getIslands().getMaxMembers(island, RanksManager.MEMBER_RANK)), "[total]",
                String.valueOf(island.getMemberSet().size()), "[online]", String.valueOf(onlineMemberCount)));

        // We now need to get all online "members" of the island - incl. Trusted and coop
        List<UUID> onlineMembers = island.getMemberSet(RanksManager.COOP_RANK).stream()
                .filter(uuid -> Util.getOnlinePlayerList(user).contains(Bukkit.getOfflinePlayer(uuid).getName()))
                .toList();

        for (int rank : RANKS) {
            Set<UUID> players = island.getMemberSet(rank, false);
            if (!players.isEmpty()) {
                if (rank == RanksManager.OWNER_RANK) {
                    // Slightly special handling for the owner rank
                    message.add(user.getTranslation("commands.island.team.info.rank-layout.owner", TextVariables.RANK,
                            user.getTranslation(RanksManager.OWNER_RANK_REF)));
                } else {
                    message.add(user.getTranslation("commands.island.team.info.rank-layout.generic", TextVariables.RANK,
                            user.getTranslation(RanksManager.getInstance().getRank(rank)), TextVariables.NUMBER,
                            String.valueOf(island.getMemberSet(rank, false).size())));
                }
                message.addAll(displayOnOffline(user, rank, island, onlineMembers));
            }
        }
        return message;
    }

    private List<String> displayOnOffline(User user, int rank, Island island, List<UUID> onlineMembers) {
        List<String> message = new ArrayList<>();
        for (UUID member : island.getMemberSet(rank, false)) {
            message.add(getMemberStatus(user, member, onlineMembers.contains(member)));

        }
        return message;
    }

    private String getMemberStatus(User user2, UUID member, boolean online) {
        OfflinePlayer offlineMember = Bukkit.getOfflinePlayer(member);
        if (online) {
            return user.getTranslation("commands.island.team.info.member-layout.online", TextVariables.NAME,
                    offlineMember.getName());
        } else {
            return offlinePlayerStatus(user, offlineMember);
        }
    }

    /**
     * Creates text to describe the status of the player
     * @param user2 user asking to see the status
     * @param offlineMember member of the team
     * @return string
     */
    private String offlinePlayerStatus(User user2, OfflinePlayer offlineMember) {
        // A bit of handling for the last joined date
        Instant lastJoined = Instant.ofEpochMilli(offlineMember.getLastPlayed());
        Instant now = Instant.now();

        Duration duration = Duration.between(lastJoined, now);
        String lastSeen;
        final String reference = "commands.island.team.info.last-seen.layout";
        if (duration.toMinutes() < 60L) {
            lastSeen = user.getTranslation(reference, TextVariables.NUMBER, String.valueOf(duration.toMinutes()),
                    TextVariables.UNIT, user.getTranslation("commands.island.team.info.last-seen.minutes"));
        } else if (duration.toHours() < 24L) {
            lastSeen = user.getTranslation(reference, TextVariables.NUMBER, String.valueOf(duration.toHours()),
                    TextVariables.UNIT, user.getTranslation("commands.island.team.info.last-seen.hours"));
        } else {
            lastSeen = user.getTranslation(reference, TextVariables.NUMBER, String.valueOf(duration.toDays()),
                    TextVariables.UNIT, user.getTranslation("commands.island.team.info.last-seen.days"));
        }

        if (island.getMemberSet(RanksManager.MEMBER_RANK, true).contains(offlineMember.getUniqueId())) {
            return user.getTranslation("commands.island.team.info.member-layout.offline", TextVariables.NAME,
                    offlineMember.getName(), "[last_seen]", lastSeen);
        } else {
            // This will prevent anyone that is trusted or below to not have a last-seen status
            return user.getTranslation("commands.island.team.info.member-layout.offline-not-last-seen",
                    TextVariables.NAME, offlineMember.getName());
        }
    }

    private boolean fireEvent(User user, Island island) {
        IslandBaseEvent e = TeamEvent.builder().island(island).reason(TeamEvent.Reason.INFO)
                .involvedPlayer(user.getUniqueId()).build();
        return e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled());
    }

    /**
     * Add an invite
     * @param type - type of invite
     * @param inviter - uuid of inviter
     * @param invitee - uuid of invitee
     * @since 1.8.0
     */
    public void addInvite(Invite.Type type, @NonNull UUID inviter, @NonNull UUID invitee, @NonNull Island island) {
        inviteMap.put(invitee, new Invite(type, inviter, invitee, island));
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
