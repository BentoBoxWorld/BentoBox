package world.bentobox.bentobox.api.commands.island.team;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord.ActionRecords;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord.TemplateItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * GUI for managing island teams in BentoBox.
 * <p>
 * Features:
 * <ul>
 *   <li>Display team members with their ranks</li>
 *   <li>Rank filtering and management</li>
 *   <li>Team invites handling</li>
 *   <li>Member management (kick, promote, demote)</li>
 *   <li>Owner transfer</li>
 *   <li>Team status display</li>
 * </ul>
 * <p>
 * The GUI uses a template system for layout and styling, loaded from team_panel.yml.
 * It supports various click actions for different team management operations.
 * 
 * @since 1.0
 */
public class IslandTeamGUI {

    /**
     * Ordered list of ranks from highest (OWNER) to lowest (COOP).
     * Used for displaying members in rank order and filtering.
     */
    private static final List<Integer> RANKS = List.of(RanksManager.OWNER_RANK, RanksManager.SUB_OWNER_RANK,
            RanksManager.MEMBER_RANK, RanksManager.TRUSTED_RANK, RanksManager.COOP_RANK);

    private static final String NAME = ".name";
    private static final String TIPS = "commands.island.team.gui.tips.";

    /** The user viewing the GUI */
    private final User user;

    /** The island being managed */
    private final Island island;

    /**
     * Current rank being viewed in the filter.
     * Defaults to OWNER_RANK which shows all ranks.
     */
    private int rankView = RanksManager.OWNER_RANK;

    /** Template items for panel styling */
    private @Nullable TemplateItem border;
    private @Nullable TemplateItem background;

    private final IslandTeamCommand parent;
    private final BentoBox plugin;


    /**
     * Displays the team management GUI
     * @param plugin BentoBox
     * @param parent IslandTeamCommand object
     * @param user user who is opening the GUI
     * @param island island that the GUI is managing
     */
    public IslandTeamGUI(BentoBox plugin, IslandTeamCommand parent, User user, Island island) {
        this.parent = parent;
        this.plugin = plugin;
        this.user = user;
        this.island = island;
     // Panels
        if (!new File(plugin.getDataFolder() + File.separator + "panels", "team_panel.yml").exists()) {
            plugin.saveResource("panels/team_panel.yml", false);
        }
    }

    /**
     * This method builds this GUI.
     */
    public void build() {
        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.user(user);
        panelBuilder.world(user.getWorld());

        panelBuilder.template("team_panel", new File(plugin.getDataFolder(), "panels"));

        panelBuilder.parameters("[name]", user.getName(), "[display_name]", user.getDisplayName());

        panelBuilder.registerTypeBuilder("STATUS", this::createStatusButton);
        panelBuilder.registerTypeBuilder("MEMBER", this::createMemberButton);
        panelBuilder.registerTypeBuilder("INVITED", this::createInvitedButton);
        panelBuilder.registerTypeBuilder("RANK", this::createRankButton);
        panelBuilder.registerTypeBuilder("INVITE", this::createInviteButton);
        border = panelBuilder.getPanelTemplate().border();
        background = panelBuilder.getPanelTemplate().background();
        // Register unknown type builder.
        panelBuilder.build();
    }

    private PanelItem createInviteButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        if (island == null || !user.hasPermission(this.parent.getInviteCommand().getPermission())
                || island.getRank(user) < island.getRankCommand(parent.getLabel() + " invite")) {
            return this.getBlankBorder();
        }
        PanelItemBuilder builder = new PanelItemBuilder();
        builder.icon(Material.PLAYER_HEAD);
        builder.name(user.getTranslation("commands.island.team.gui.buttons.invite.name"));
        builder.description(user.getTranslation("commands.island.team.gui.buttons.invite.description"));
        builder.clickHandler((panel, user, clickType, clickSlot) -> {
            if (template.actions().stream().noneMatch(ar -> clickType.equals(ar.clickType()))) {
                // If the click type is not in the template, don't do anything
                return true;
            }
            if (clickType.equals(ClickType.LEFT)) {
                user.closeInventory();
                new IslandTeamInviteGUI(parent, false, island).build(user);
            }
            return true;
        });
        return builder.build();
    }

    private PanelItem createRankButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        // If there is no island, then do not show this icon
        if (island == null) {
            return this.getBlankBorder();
        }
        PanelItemBuilder builder = new PanelItemBuilder();
        builder.name(user.getTranslation("commands.island.team.gui.buttons.rank-filter.name"));
        builder.icon(Material.AMETHYST_SHARD);
        // Create description
        createDescription(builder);
        createClickHandler(builder, template.actions());

        return builder.build();
    }

    /**
     * Creates button to filter members by rank.
     * Updates the display when clicked:
     * - Left click decreases rank
     * - Right click increases rank 
     * - Wraps around at min/max ranks
     */
    private void createClickHandler(PanelItemBuilder builder, @NonNull List<ActionRecords> actions) {
        builder.clickHandler((panel, user, clickType, clickSlot) -> {
            if (actions.stream().noneMatch(ar -> clickType.equals(ar.clickType()))) {
                // If the click type is not in the template, don't do anything
                return true;
            }
            if (clickType.equals(ClickType.LEFT)) {
                rankView = RanksManager.getInstance().getRankDownValue(rankView);
                if (rankView <= RanksManager.VISITOR_RANK) {
                    rankView = RanksManager.OWNER_RANK;
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
                } else {
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                }
            }
            if (clickType.equals(ClickType.RIGHT)) {
                rankView = RanksManager.getInstance().getRankUpValue(rankView);
                if (rankView >= RanksManager.OWNER_RANK) {
                    rankView = RanksManager.getInstance().getRankUpValue(RanksManager.VISITOR_RANK);
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
                } else {
                    user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                }
            }

            // Update panel after click
            build();
            return true;
        });


    }

    /**
     * Creates the description showing which ranks are currently visible.
     * Ranks above and below the selected rank are shown as blocked.
     */
    private void createDescription(PanelItemBuilder builder) {
        RanksManager.getInstance().getRanks().forEach((reference, score) -> {
            if (rankView == RanksManager.OWNER_RANK && score > RanksManager.VISITOR_RANK
                    && score <= RanksManager.OWNER_RANK) {
                builder.description(user.getTranslation("protection.panel.flag-item.allowed-rank")
                        + user.getTranslation(reference));
            } else if (score > RanksManager.VISITOR_RANK && score < rankView) {
                builder.description(user.getTranslation("protection.panel.flag-item.blocked-rank")
                        + user.getTranslation(reference));
            } else if (score <= RanksManager.OWNER_RANK && score > rankView) {
                builder.description(user.getTranslation("protection.panel.flag-item.blocked-rank")
                        + user.getTranslation(reference));
            } else if (score == rankView) {
                builder.description(user.getTranslation("protection.panel.flag-item.allowed-rank")
                        + user.getTranslation(reference));
            }
        });
        builder.description(user.getTranslation("commands.island.team.gui.buttons.rank-filter.description"));

    }

    /**
     * Create invited button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createInvitedButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        PanelItemBuilder builder = new PanelItemBuilder();
        if (parent.isInvited(user.getUniqueId()) && user.hasPermission(parent.getAcceptCommand().getPermission())) {
            TeamInvite invite = parent.getInvite(user.getUniqueId());
            if (invite == null) {
                return this.getBlankBorder();
            }
            User inviter = User.getInstance(invite.getInviter());
            String name = inviter.getName();
            builder.icon(inviter.getName());
            builder.name(user.getTranslation("commands.island.team.gui.buttons.invitation"));
            createInviteDescription(builder, invite.getType(), name, template.actions());
            createInviteClickHandler(builder, invite, template.actions());
        } else {
            return this.getBlankBorder();
        }
        return builder.build();
    }

    private void createInviteClickHandler(PanelItemBuilder builder, TeamInvite invite,
            @NonNull List<ActionRecords> list) {
        Type type = invite.getType();
        builder.clickHandler((panel, user, clickType, clickSlot) -> {
            if (list.stream().noneMatch(ar -> clickType.equals(ar.clickType()))) {
                // If the click type is not in the template, don't do anything
                return true;
            }
            if (clickType.equals(ClickType.SHIFT_LEFT)
                    && user.hasPermission(parent.getAcceptCommand().getPermission())) {
                plugin.log("Invite accepted: " + user.getName() + " accepted " + type);
                // Accept
                switch (type) {
                case COOP -> parent.getAcceptCommand().acceptCoopInvite(user, invite);
                case TRUST -> parent.getAcceptCommand().acceptTrustInvite(user, invite);
                default -> parent.getAcceptCommand().acceptTeamInvite(user, invite);
                }
                user.closeInventory();
            }
            if (clickType.equals(ClickType.SHIFT_RIGHT)
                    && user.hasPermission(parent.getRejectCommand().getPermission())) {
                // Reject
                plugin.log("Invite rejected: " + user.getName() + " rejected " + type + " invite.");
                parent.getRejectCommand().execute(user, "", List.of());
                user.closeInventory();
            }
            return true;
        });

    }

    private void createInviteDescription(PanelItemBuilder builder, Type type, String name,
            @NonNull List<ActionRecords> list) {
        builder.description(switch (type) {
        case COOP -> List.of(
                user.getTranslation("commands.island.team.invite.name-has-invited-you.coop", TextVariables.NAME, name));
        case TRUST -> List.of(user.getTranslation("commands.island.team.invite.name-has-invited-you.trust",
                TextVariables.NAME, name));
        default ->
            List.of(user.getTranslation("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, name),
                    user.getTranslation("commands.island.team.invite.accept.confirmation"));
        });
        // Add all the tool tips
        builder.description(list.stream()
                .map(ar -> user.getTranslation(TIPS + ar.clickType().name() + NAME) + " "
                        + user.getTranslation(ar.tooltip()))
                .toList());

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
        Island is = plugin.getIslands().getPrimaryIsland(parent.getWorld(), user.getUniqueId());
        if (is == null) {
            return getBlankBorder();
        }

        return builder.icon(user.getName()).name(user.getTranslation("commands.island.team.gui.buttons.status.name"))
                .description(showMembers()).build();
    }

    private PanelItem getBlankBorder() {
        assert border != null;
        return new PanelItemBuilder().icon(Objects.requireNonNullElse(border.icon(), new ItemStack(Material.BARRIER)))
                .name((Objects.requireNonNullElse(border.title(), ""))).build();
    }

    private PanelItem getBlankBackground() {
        assert background != null;
        return new PanelItemBuilder()
                .icon(Objects.requireNonNullElse(background.icon(), new ItemStack(Material.BARRIER)))
                .name((Objects.requireNonNullElse(background.title(), ""))).build();
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
        Island is = plugin.getIslands().getPrimaryIsland(parent.getWorld(), user.getUniqueId());
        if (is == null) {
            return this.getBlankBackground();
        }
        int minimumRank = RanksManager.getInstance().getRankUpValue(RanksManager.VISITOR_RANK); // Get the rank above Visitor.
        Optional<User> opMember = is.getMemberSet(minimumRank).stream().map(User::getInstance)
                .filter((User usr) -> rankView == RanksManager.OWNER_RANK || is.getRank(usr) == rankView) // If rankView is owner then show all ranks
                .sorted(Comparator.comparingInt((User usr) -> is.getRank(usr)).reversed()) // Show owner on left, then descending ranks
                .skip(slot.slot()) // Get the head for this slot
                .limit(1L).findFirst(); // Get just one head
        if (opMember.isEmpty()) {
            return this.getBlankBackground();
        }
        User member = opMember.get();
        int rank = is.getRank(member);
        String rankRef = RanksManager.getInstance().getRank(rank);
        @NonNull
        List<ActionRecords> actions = template.actions();
        // Make button description depending on viewer
        List<String> desc = new ArrayList<>();
        int userRank = Objects.requireNonNull(is).getRank(user);
        // Add the tooltip for kicking
        if (user.hasPermission(parent.getKickCommand().getPermission())
                && userRank >= is.getRankCommand(parent.getLabel() + " kick") && !user.equals(member)) {
            actions.stream().filter(ar -> ar.actionType().equalsIgnoreCase("kick"))
                    .map(ar -> user.getTranslation(TIPS + ar.clickType().name() + NAME)
                            + " " + user.getTranslation(ar.tooltip()))
                    .findFirst().ifPresent(desc::add);
        }
        // Set Owner
        if (user.hasPermission(parent.getSetOwnerCommand().getPermission()) && !user.equals(member)
                && userRank >= RanksManager.OWNER_RANK && rank >= RanksManager.MEMBER_RANK) {
            // Add the tooltip for setowner
            actions.stream().filter(ar -> ar.actionType().equalsIgnoreCase("setowner"))
                    .map(ar -> user.getTranslation(TIPS + ar.clickType().name() + NAME)
                            + " " + user.getTranslation(ar.tooltip()))
                    .findFirst().ifPresent(desc::add);
        }
        // Leave
        if (user.hasPermission(parent.getLeaveCommand().getPermission()) && user.equals(member)
                && userRank < RanksManager.OWNER_RANK) {
            // Add the tooltip for leave
            actions.stream().filter(ar -> ar.actionType().equalsIgnoreCase("leave"))
                    .map(ar -> user.getTranslation(TIPS + ar.clickType().name() + NAME)
                            + " " + user.getTranslation(ar.tooltip()))
                    .findFirst().ifPresent(desc::add);
        }
        if (member.isOnline()) {
            desc.addFirst(user.getTranslation(rankRef));
            return new PanelItemBuilder().icon(member.getName()).name(member.getDisplayName()).description(desc)
                    .clickHandler(
                            (panel, user, clickType, i) -> clickListener(panel, user, clickType, i, member, actions))
                    .build();
        } else {
            // Offline player
            desc.addFirst(user.getTranslation(rankRef));
            return new PanelItemBuilder().icon(member.getName())
                    .name(offlinePlayerStatus(Bukkit.getOfflinePlayer(member.getUniqueId()))).description(desc)
                    .clickHandler(
                            (panel, user, clickType, i) -> clickListener(panel, user, clickType, i, member, actions))
                    .build();
        }
    }

    /**
     * Click listener
     * @param panel panel
     * @param clickingUser clicking user
     * @param clickType click type
     * @param i slot
     * @param target target user
     * @param actions actions
     * @return true if the inventory item should not be removed - always true
     */
    private boolean clickListener(Panel panel, User clickingUser, ClickType clickType, int i, User target,
            List<ActionRecords> actions) {
        if (actions.stream().noneMatch(ar -> clickType.equals(ar.clickType()))) {
            // If the click type is not in the template, don't do anything
            return true;
        }
        int rank = Objects.requireNonNull(island).getRank(clickingUser);
        for (ItemTemplateRecord.ActionRecords action : actions) {
            if (clickType.equals(action.clickType())) {
                switch (action.actionType().toUpperCase(Locale.ENGLISH)) {
                case "KICK" -> kickPlayer(clickingUser, target, rank);
                case "SETOWNER" -> setOwner(clickingUser, target);
                case "LEAVE" -> leave(clickingUser, target);
                default -> {
                    // Do nothing
                }
                }
            }
        }
        return true;
    }

    private void leave(User clickingUser, User target) {
        if (clickingUser.hasPermission(parent.getLeaveCommand().getPermission()) && target.equals(clickingUser)
                && !clickingUser.getUniqueId().equals(island.getOwner())) {
            plugin.log("Leave: " + clickingUser.getName() + " trying to leave island at " + island.getCenter());
            clickingUser.closeInventory();
            if (parent.getLeaveCommand().leave(clickingUser)) {
                plugin.log("Leave: success");
            } else {
                plugin.log("Leave: failed");
            }
        }
    }

    private void setOwner(User clickingUser, User target) {
        // Make the player the leader of the island
        if (clickingUser.hasPermission(parent.getSetOwnerCommand().getPermission()) && !target.equals(clickingUser)
                && clickingUser.getUniqueId().equals(island.getOwner())
                && island.getRank(target) >= RanksManager.MEMBER_RANK) {
            plugin.log("Set Owner: " + clickingUser.getName() + " trying to make " + target.getName()
                    + " owner of island at " + island.getCenter());
            clickingUser.closeInventory();
            if (parent.getSetOwnerCommand().setOwner(clickingUser, target.getUniqueId())) {
                plugin.log("Set Owner: success");
            } else {
                plugin.log("Set Owner: failed");
                }
        }
    }

    private void kickPlayer(User clickingUser, User target, int rank) {
        // Kick the player, or uncoop, or untrust
        if (clickingUser.hasPermission(parent.getKickCommand().getPermission()) && !target.equals(clickingUser)
                && rank >= island.getRankCommand(parent.getLabel() + " kick")) {
            plugin.log("Kick: " + clickingUser.getName() + " kicked " + target.getName() + " from island at "
                    + island.getCenter());
            clickingUser.closeInventory();
            if (removePlayer(clickingUser, target)) {
                clickingUser.getPlayer().playSound(clickingUser.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
                plugin.log("Kick: success");
            } else {
                plugin.log("Kick: failed");
                }
            }
    }

    private boolean removePlayer(User clicker, User member) {
        // If member then kick, if coop, uncoop, if trusted, then untrust
        return switch (island.getRank(member)) {
        case RanksManager.COOP_RANK -> parent.getUncoopCommand().unCoopCmd(user, member.getUniqueId());
        case RanksManager.TRUSTED_RANK -> parent.getUnTrustCommand().unTrustCmd(user, member.getUniqueId());
        default -> {
            if (parent.getKickCommand().canExecute(user, parent.getKickCommand().getLabel(),
                    List.of(member.getName()))) {
                yield parent.getKickCommand().kick(clicker, member.getUniqueId());
            } else {
                yield false;
            }
        }
        };

    }

    private List<String> showMembers() {
        List<String> message = new ArrayList<>();
        // Gather online members
        long onlineMemberCount = island.getMemberSet(RanksManager.MEMBER_RANK).stream()
                .filter(uuid -> Util.getOnlinePlayerList(user).contains(Bukkit.getOfflinePlayer(uuid).getName()))
                .count();

        // Show header:
        message.add(user.getTranslation("commands.island.team.info.header", "[max]",
                String.valueOf(plugin.getIslands().getMaxMembers(island, RanksManager.MEMBER_RANK)), "[total]",
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
                message.addAll(displayOnOffline(rank, island, onlineMembers));
            }
        }
        return message;
    }

    private List<String> displayOnOffline(int rank, Island island, List<UUID> onlineMembers) {
        List<String> message = new ArrayList<>();
        for (UUID member : island.getMemberSet(rank, false)) {
            message.add(getMemberStatus(member, onlineMembers.contains(member)));

        }
        return message;
    }

    private String getMemberStatus(UUID member, boolean online) {
        OfflinePlayer offlineMember = Bukkit.getOfflinePlayer(member);
        if (online) {
            return user.getTranslation("commands.island.team.info.member-layout.online", TextVariables.NAME,
                    offlineMember.getName());
        } else {
            return offlinePlayerStatus(offlineMember);
        }
    }

    /**
     * Creates text to describe the status of the player
     * @param offlineMember member of the team
     * @return string
     */
    private String offlinePlayerStatus(OfflinePlayer offlineMember) {
        String lastSeen = lastSeen(offlineMember);
        if (island.getMemberSet(RanksManager.MEMBER_RANK, true).contains(offlineMember.getUniqueId())) {
            return user.getTranslation("commands.island.team.info.member-layout.offline", TextVariables.NAME,
                    offlineMember.getName(), "[last_seen]", lastSeen);
        } else {
            // This will prevent anyone that is trusted or below to not have a last-seen status
            return user.getTranslation("commands.island.team.info.member-layout.offline-not-last-seen",
                    TextVariables.NAME, offlineMember.getName());
        }
    }

    /**
     * Formats the last seen time for offline players.
     * Shows time in most appropriate unit: <br>
     * - Minutes if &lt 1 hour <br>
     * - Hours if &lt 1 day <br>
     * - Days otherwise
     */
    private String lastSeen(OfflinePlayer offlineMember) {
        // A bit of handling for the last joined date
        Instant lastJoined = Instant.ofEpochMilli(offlineMember.getLastSeen());
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
        return lastSeen;
    }


}