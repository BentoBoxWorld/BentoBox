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
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
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
    /** Locale keys for the action menu, opened by a plain click for players who cannot shift or right click (#3137). */
    private static final String ACTIONS = "commands.island.team.gui.actions.";
    private static final String KICK = "kick";
    private static final String SETOWNER = "setowner";
    private static final String LEAVE = "leave";
    /** Slots in the one-row action menu. */
    private static final int ACTION_MENU_SIZE = 9;
    /** Locale key for the (customisable) member button name. Placeholders: [name], [display_name]. */
    private static final String MEMBER_NAME_REF = "commands.island.team.gui.buttons.member.name";
    /** Locale key for the (customisable) member button rank line. Placeholder: [rank]. */
    private static final String MEMBER_DESC_REF = "commands.island.team.gui.buttons.member.description";
    /** Locale key for the (customisable) suffix appended to an offline member's name. Placeholder: [last_seen]. */
    private static final String MEMBER_LAST_SEEN_REF = "commands.island.team.gui.buttons.member.last-seen";
    /** Placeholder replaced with the formatted last-seen suffix in the member button name. */
    private static final String LAST_SEEN = "[last_seen]";

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
        applyIcon(builder, template, Material.PLAYER_HEAD);
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
        applyIcon(builder, template, Material.AMETHYST_SHARD);
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
            String rankName = user.getTranslation(reference);
            if (rankView == RanksManager.OWNER_RANK && score > RanksManager.VISITOR_RANK
                    && score <= RanksManager.OWNER_RANK) {
                builder.description(getRankTranslation("protection.panel.flag-item.allowed-rank", rankName));
            } else if (score > RanksManager.VISITOR_RANK && score < rankView) {
                builder.description(getRankTranslation("protection.panel.flag-item.blocked-rank", rankName));
            } else if (score <= RanksManager.OWNER_RANK && score > rankView) {
                builder.description(getRankTranslation("protection.panel.flag-item.blocked-rank", rankName));
            } else if (score == rankView) {
                builder.description(getRankTranslation("protection.panel.flag-item.allowed-rank", rankName));
            }
        });
        builder.description(user.getTranslation("commands.island.team.gui.buttons.rank-filter.description"));

    }

    /**
     * @see Flag#getRankLine(User, String, String)
     */
    private String getRankTranslation(String key, String rankName) {
        return Flag.getRankLine(user, key, rankName);
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
                // Not a click the template uses: offer the same actions as buttons instead
                if (opensActionMenu(clickType)) {
                    openInviteActions(invite);
                }
                return true;
            }
            if (clickType.equals(ClickType.SHIFT_LEFT)) {
                acceptInvite(user, invite);
            }
            if (clickType.equals(ClickType.SHIFT_RIGHT)) {
                rejectInvite(user, type);
            }
            return true;
        });

    }

    private void acceptInvite(User clickingUser, TeamInvite invite) {
        if (!clickingUser.hasPermission(parent.getAcceptCommand().getPermission())) {
            return;
        }
        plugin.log("Invite accepted: " + clickingUser.getName() + " accepted " + invite.getType());
        switch (invite.getType()) {
        case COOP -> parent.getAcceptCommand().acceptCoopInvite(clickingUser, invite);
        case TRUST -> parent.getAcceptCommand().acceptTrustInvite(clickingUser, invite);
        default -> parent.getAcceptCommand().acceptTeamInvite(clickingUser, invite);
        }
        clickingUser.closeInventory();
    }

    private void rejectInvite(User clickingUser, Type type) {
        if (!clickingUser.hasPermission(parent.getRejectCommand().getPermission())) {
            return;
        }
        plugin.log("Invite rejected: " + clickingUser.getName() + " rejected " + type + " invite.");
        parent.getRejectCommand().execute(clickingUser, "", List.of());
        clickingUser.closeInventory();
    }

    /**
     * Opens the action menu for a pending invitation: Accept and Reject as buttons.
     */
    private void openInviteActions(TeamInvite invite) {
        List<PanelItem> items = new ArrayList<>();
        if (user.hasPermission(parent.getAcceptCommand().getPermission())) {
            items.add(actionButton("accept", Material.LIME_CONCRETE, () -> acceptInvite(user, invite)));
        }
        if (user.hasPermission(parent.getRejectCommand().getPermission())) {
            items.add(actionButton("reject", Material.RED_CONCRETE, () -> rejectInvite(user, invite.getType())));
        }
        openActionMenu(user.getTranslation("commands.island.team.gui.buttons.invitation"), items);
    }

    /**
     * A plain left or right click that the template gives no action to opens the action menu.
     * Touch-screen Bedrock players (through Geyser) can only tap, which arrives as a left click,
     * so every shift or right click action has to be reachable this way (#3137).
     */
    private static boolean opensActionMenu(ClickType clickType) {
        return clickType == ClickType.LEFT || clickType == ClickType.RIGHT;
    }

    /**
     * The "click for actions" line for a button's description, or empty if the template already
     * uses the left click for something, or the locale has no such line.
     */
    private Optional<String> actionsTip(@NonNull List<ActionRecords> actions) {
        if (actions.stream().anyMatch(ar -> ClickType.LEFT.equals(ar.clickType()))) {
            return Optional.empty();
        }
        String tip = translateOr(ACTIONS + "tip", "");
        return tip.isBlank() ? Optional.empty() : Optional.of(tip);
    }

    private PanelItem actionButton(String action, Material icon, Runnable onClick) {
        return new PanelItemBuilder().icon(icon).name(user.getTranslation(ACTIONS + action))
                .clickHandler((panel, clicker, clickType, clickSlot) -> {
                    onClick.run();
                    return true;
                }).build();
    }

    /**
     * Opens a one-row menu with a button per action and a Back button that returns to the team panel.
     * Does nothing if there are no actions to show.
     */
    private void openActionMenu(String title, List<PanelItem> items) {
        if (items.isEmpty()) {
            return;
        }
        PanelBuilder menu = new PanelBuilder().user(user).world(user.getWorld()).name(title).size(ACTION_MENU_SIZE);
        // The last slot is kept for Back
        for (int i = 0; i < Math.min(items.size(), ACTION_MENU_SIZE - 1); i++) {
            menu.item(i, items.get(i));
        }
        menu.item(ACTION_MENU_SIZE - 1, actionButton("back", Material.ARROW, this::build));
        menu.build();
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
        actionsTip(list).ifPresent(builder::description);

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

        // Use the template icon if the admin has set one, otherwise show the viewer's head.
        if (template.icon() != null) {
            builder.icon(template.icon().clone());
        } else {
            builder.icon(user.getName());
        }
        return builder.name(user.getTranslation("commands.island.team.gui.buttons.status.name"))
                .description(showMembers()).build();
    }

    /**
     * Applies the icon for a button. If the panel template defines an {@code icon:}
     * for this button it is used (allowing server admins to customise it via
     * {@code team_panel.yml}); otherwise the supplied default material is used.
     *
     * @param builder  the panel item builder
     * @param template the template record for this button
     * @param fallback the default material to use when the template has no icon
     */
    private void applyIcon(PanelItemBuilder builder, ItemTemplateRecord template, Material fallback) {
        builder.icon(template.icon() != null ? template.icon().clone() : new ItemStack(fallback));
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
        // Add the tooltip for kicking
        if (canKick(is, member)) {
            actions.stream().filter(ar -> ar.actionType().equalsIgnoreCase(KICK))
                    .map(ar -> user.getTranslation(TIPS + ar.clickType().name() + NAME)
                            + " " + user.getTranslation(ar.tooltip()))
                    .findFirst().ifPresent(desc::add);
        }
        // Set Owner
        if (canSetOwner(is, member)) {
            // Add the tooltip for setowner
            actions.stream().filter(ar -> ar.actionType().equalsIgnoreCase(SETOWNER))
                    .map(ar -> user.getTranslation(TIPS + ar.clickType().name() + NAME)
                            + " " + user.getTranslation(ar.tooltip()))
                    .findFirst().ifPresent(desc::add);
        }
        // Leave
        if (canLeave(is, member)) {
            // Add the tooltip for leave
            actions.stream().filter(ar -> ar.actionType().equalsIgnoreCase(LEAVE))
                    .map(ar -> user.getTranslation(TIPS + ar.clickType().name() + NAME)
                            + " " + user.getTranslation(ar.tooltip()))
                    .findFirst().ifPresent(desc::add);
        }
        // Tell the viewer that a plain click lists the same actions as buttons
        if (canKick(is, member) || canSetOwner(is, member) || canLeave(is, member)) {
            actionsTip(actions).ifPresent(desc::add);
        }
        // Rank line, styled through a customisable locale key ([rank] placeholder), shown first.
        // Fall back to the raw rank name if the key is absent (e.g. an older custom locale file).
        String rankName = user.getTranslation(rankRef);
        desc.addFirst(translateOr(MEMBER_DESC_REF, rankName, TextVariables.RANK, rankName));
        // The name is styled through the same customisable member.name key whether the member is
        // online or offline, so admins get one consistent format. Offline members (member rank or
        // above) get a last-seen suffix via the [last_seen] placeholder; online members and
        // trusted-or-below members get an empty suffix.
        String lastSeenSuffix = lastSeenSuffix(is, member);
        return new PanelItemBuilder().icon(member.getName())
                .name(translateOr(MEMBER_NAME_REF, member.getDisplayName() + lastSeenSuffix, TextVariables.NAME,
                        member.getName(), TextVariables.DISPLAY_NAME, member.getDisplayName(), LAST_SEEN,
                        lastSeenSuffix))
                .description(desc)
                .clickHandler(
                        (panel, user, clickType, i) -> clickListener(user, clickType, member, actions))
                .build();
    }

    /** @return true if this member is the player looking at the panel */
    private boolean isViewer(User member) {
        return Objects.equals(user.getUniqueId(), member.getUniqueId());
    }

    /** @return true if the viewer may kick (or uncoop, or untrust) this member */
    private boolean canKick(Island is, User member) {
        return user.hasPermission(parent.getKickCommand().getPermission()) && !isViewer(member)
                && is.getRank(user) >= is.getRankCommand(parent.getLabel() + " kick");
    }

    /** @return true if the viewer owns the island and may hand it to this member */
    private boolean canSetOwner(Island is, User member) {
        return user.hasPermission(parent.getSetOwnerCommand().getPermission()) && !isViewer(member)
                && is.getRank(user) >= RanksManager.OWNER_RANK && is.getRank(member) >= RanksManager.MEMBER_RANK;
    }

    /** @return true if this member is the viewer and the viewer may leave the team */
    private boolean canLeave(Island is, User member) {
        return user.hasPermission(parent.getLeaveCommand().getPermission()) && isViewer(member)
                && is.getRank(user) < RanksManager.OWNER_RANK;
    }

    /**
     * Opens the action menu for a team member: the actions the viewer may take on them, as buttons.
     */
    private void openMemberActions(User target) {
        Island is = plugin.getIslands().getPrimaryIsland(parent.getWorld(), user.getUniqueId());
        if (is == null) {
            return;
        }
        List<PanelItem> items = new ArrayList<>();
        if (canKick(is, target)) {
            items.add(actionButton(KICK, Material.IRON_BOOTS, () -> kickPlayer(user, target, island.getRank(user))));
        }
        if (canSetOwner(is, target)) {
            items.add(actionButton(SETOWNER, Material.GOLDEN_HELMET, () -> setOwner(user, target)));
        }
        if (canLeave(is, target)) {
            items.add(actionButton(LEAVE, Material.OAK_DOOR, () -> leave(user, target)));
        }
        openActionMenu(target.getName(), items);
    }

    /**
     * Builds the last-seen suffix shown after an offline member's name in the team GUI. Returns an
     * empty string for online members and for members ranked trusted or below (who have no last-seen
     * status), otherwise the formatted, customisable {@link #MEMBER_LAST_SEEN_REF} suffix.
     *
     * @param is     the island being viewed
     * @param member the member whose button is being built
     * @return the suffix to append to the member's name, or an empty string
     */
    private String lastSeenSuffix(Island is, User member) {
        if (member.isOnline() || !is.getMemberSet(RanksManager.MEMBER_RANK, true).contains(member.getUniqueId())) {
            return "";
        }
        String lastSeen = lastSeen(Bukkit.getOfflinePlayer(member.getUniqueId()));
        return translateOr(MEMBER_LAST_SEEN_REF, "", LAST_SEEN, lastSeen);
    }

    /**
     * Translates {@code reference}, falling back to {@code fallback} when the key is
     * missing from the locale. {@link User#getTranslation} echoes the reference back
     * when a key is absent, so an older or customised locale file that lacks the new
     * keys would otherwise show the raw key string in the GUI.
     *
     * @param reference the locale key
     * @param fallback  value to use when the key is missing
     * @param variables placeholder replacement pairs
     * @return the translated (and substituted) string, or the fallback
     */
    private String translateOr(String reference, String fallback, String... variables) {
        String translation = user.getTranslation(reference, variables);
        return translation.equals(reference) ? fallback : translation;
    }

    /**
     * Click listener
     * @param clickingUser clicking user
     * @param clickType click type
     * @param target target user
     * @param actions actions
     * @return true if the inventory item should not be removed - always true
     */
    private boolean clickListener(User clickingUser, ClickType clickType, User target, List<ActionRecords> actions) {
        if (actions.stream().noneMatch(ar -> clickType.equals(ar.clickType()))) {
            // Not a click the template uses: offer the same actions as buttons instead
            if (opensActionMenu(clickType)) {
                openMemberActions(target);
            }
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
        // Objects.equals: getUniqueId() is null for non-player users and must not NPE
        if (clickingUser.hasPermission(parent.getLeaveCommand().getPermission()) && target.equals(clickingUser)
                && !Objects.equals(clickingUser.getUniqueId(), island.getOwner())) {
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
                    offlineMember.getName(), LAST_SEEN, lastSeen);
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