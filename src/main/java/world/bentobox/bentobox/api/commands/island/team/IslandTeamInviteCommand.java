package world.bentobox.bentobox.api.commands.island.team;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.island.team.Invite.Type;
import world.bentobox.bentobox.api.commands.island.team.conversations.InviteNamePrompt;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord.TemplateItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

public class IslandTeamInviteCommand extends CompositeCommand {

    private final IslandTeamCommand itc;
    private @Nullable User invitedPlayer;
    private @Nullable TemplateItem border;
    private @Nullable TemplateItem background;
    private User user;
    private long page = 0; // This number by 35
    private boolean inviteCmd;
    private static final long PER_PAGE = 35;

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

        if (args.size() != 1) {
            this.inviteCmd = true;
            build(user);
            return true;
        }

        Island island = islandsManager.getIsland(getWorld(), user);
        int rank = Objects.requireNonNull(island).getRank(user);

        return checkRankAndInvitePlayer(user, island, rank, args.get(0));
    }

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
        if (getIslands().inTeam(getWorld(), invitedPlayerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.already-on-team");
            return false;
        }

        if (isInvitedByUser(invitedPlayerUUID, playerUUID) && isInviteTypeTeam(invitedPlayerUUID)) {
            user.sendMessage("commands.island.team.invite.errors.you-have-already-invited");
            return false;
        }

        return true;
    }

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

    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Rare case when invited player is null. Could be a race condition.
        if (invitedPlayer == null) return false;
        // If that player already has an invite out then retract it.
        // Players can only have one invite one at a time - interesting
        if (itc.isInvited(invitedPlayer.getUniqueId())) {
            itc.removeInvite(invitedPlayer.getUniqueId());
            user.sendMessage("commands.island.team.invite.removing-invite");
        }
        Island island = getIslands().getIsland(getWorld(), user.getUniqueId());
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Fire event so add-ons can run commands, etc.
        IslandBaseEvent e = TeamEvent.builder()
                .island(island)
                .reason(TeamEvent.Reason.INVITE)
                .involvedPlayer(invitedPlayer.getUniqueId())
                .build();
        if (e.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(e.isCancelled())) {
            return false;
        }
        // Put the invited player (key) onto the list with inviter (value)
        // If someone else has invited a player, then this invite will overwrite the previous invite!
        itc.addInvite(Invite.Type.TEAM, user.getUniqueId(), invitedPlayer.getUniqueId(), island);
        user.sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, invitedPlayer.getName(), TextVariables.DISPLAY_NAME, invitedPlayer.getDisplayName());
        // Send message to online player
        invitedPlayer.sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
        invitedPlayer.sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, getTopLabel());
        if (getIslands().hasIsland(getWorld(), invitedPlayer.getUniqueId())) {
            invitedPlayer.sendMessage("commands.island.team.invite.you-will-lose-your-island");
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (lastArg.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }

    /**
     * Build the invite panel
     * @param user use of the panel
     */
    void build(User user) {
        this.user = user;
        // Start building panel.
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.user(user);
        panelBuilder.world(user.getWorld());

        panelBuilder.template("team_invite_panel", new File(getPlugin().getDataFolder(), "panels"));

        panelBuilder.parameters("[name]", user.getName(), "[display_name]", user.getDisplayName());

        panelBuilder.registerTypeBuilder("PROSPECT", this::createProspectButton);
        panelBuilder.registerTypeBuilder("PREVIOUS", this::createPreviousButton);
        panelBuilder.registerTypeBuilder("NEXT", this::createNextButton);
        panelBuilder.registerTypeBuilder("SEARCH", this::createSearchButton);
        panelBuilder.registerTypeBuilder("BACK", this::createBackButton);
        // Stash the backgrounds for later use
        border = panelBuilder.getPanelTemplate().border();
        background = panelBuilder.getPanelTemplate().background();
        // Register unknown type builder.
        panelBuilder.build();

    }

    private PanelItem createBackButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        checkTemplate(template);
        return new PanelItemBuilder().name(user.getTranslation(template.title())).icon(template.icon())
                .clickHandler((panel, user, clickType, clickSlot) -> {
                    user.closeInventory();
                    if (!inviteCmd) {
                        this.itc.build();
                    }
                    return true;
                }).build();
    }

    private PanelItem createSearchButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        checkTemplate(template);
        return new PanelItemBuilder().name(user.getTranslation(template.title())).icon(template.icon())
                .clickHandler((panel, user, clickType, clickSlot) -> {
                    user.closeInventory();
                    new ConversationFactory(BentoBox.getInstance()).withLocalEcho(false).withTimeout(90)
                            .withModality(false).withFirstPrompt(new InviteNamePrompt(user, this))
                            .buildConversation(user.getPlayer()).begin();
                    return true;
                }).build();
    }

    private PanelItem createNextButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        checkTemplate(template);
        long count = getWorld().getPlayers().stream().filter(player -> user.getPlayer().canSee(player))
                .filter(player -> !player.equals(user.getPlayer())).count();
        if (count > page * PER_PAGE) {
            // We need to show a next button
            return new PanelItemBuilder().name(user.getTranslation(template.title())).icon(template.icon())
                    .clickHandler((panel, user, clickType, clickSlot) -> {
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                        page++;
                        build(user);
                        return true;
                    }).build();
        }
        return getBlankBorder();
    }

    private void checkTemplate(ItemTemplateRecord template) {
        if (template.icon() == null) {
            getPlugin().logError("Icon in template is missing or unknown! " + template.toString());
        }
        if (template.title() == null) {
            getPlugin().logError("Title in template is missing! " + template.toString());
        }

    }

    private PanelItem createPreviousButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        checkTemplate(template);
        if (page > 0) {
            // We need to show a next button
            return new PanelItemBuilder().name(user.getTranslation(template.title())).icon(template.icon())
                    .clickHandler((panel, user, clickType, clickSlot) -> {
                        user.getPlayer().playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                        page--;
                        build(user);
                        return true;
                    }).build();
        }
        return getBlankBorder();
    }

    private PanelItem getBlankBorder() {
        return new PanelItemBuilder().icon(Objects.requireNonNullElse(border.icon(), new ItemStack(Material.BARRIER)))
                .name((Objects.requireNonNullElse(border.title(), ""))).build();
    }

    /**
     * Create member button panel item.
     *
     * @param template the template
     * @param slot     the slot
     * @return the panel item
     */
    private PanelItem createProspectButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        // Player issuing the command must have an island
        Island island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        if (island == null) {
            return this.getBlankBackground();
        }
        if (page < 0) {
            page = 0;
        }
        return getWorld().getPlayers().stream().filter(player -> user.getPlayer().canSee(player))
                .filter(player -> !player.equals(user.getPlayer())).skip(slot.slot() + page * PER_PAGE).findFirst()
                .map(player -> getProspect(player, template)).orElse(this.getBlankBackground());
    }

    private PanelItem getProspect(Player player, ItemTemplateRecord template) {
        // Check if the prospect has already been invited
        if (this.itc.isInvited(player.getUniqueId())
                && user.getUniqueId().equals(this.itc.getInvite(player.getUniqueId()).getInviter())) {
            return new PanelItemBuilder().icon(player.getName()).name(player.getDisplayName())
                    .description(user.getTranslation("commands.island.team.invite.gui.button.already-invited")).build();
        }
        List<String> desc = template.actions().stream().map(ar -> user
                .getTranslation("commands.island.team.invite.gui.tips." + ar.clickType().name() + ".name")
                + " " + user.getTranslation(ar.tooltip())).toList();
        return new PanelItemBuilder().icon(player.getName()).name(player.getDisplayName()).description(desc)
                .clickHandler(
                        (panel, user, clickType, clickSlot) -> clickHandler(panel, user, clickType, clickSlot, player))
                .build();
    }

    private boolean clickHandler(Panel panel, User user, ClickType clickType, int clickSlot, Player player) {
        if (clickType.equals(ClickType.LEFT)) {
            user.closeInventory();
            if (this.canExecute(user, this.getLabel(), List.of(player.getName()))) {
                getPlugin().log("Invite sent to: " + player.getName() + " by " + user.getName() + " to join island in "
                        + getWorld().getName());
                this.execute(user, getLabel(), List.of(player.getName()));
            } else {
                getPlugin().log("Invite failed: " + player.getName() + " by " + user.getName() + " to join island in "
                        + getWorld().getName());
            }
        } else if (clickType.equals(ClickType.RIGHT)) {
            user.closeInventory();
            if (this.itc.getCoopCommand().canExecute(user, this.getLabel(), List.of(player.getName()))) {
                getPlugin().log("Coop: " + player.getName() + " cooped " + user.getName() + " to island in "
                        + getWorld().getName());
                this.itc.getCoopCommand().execute(user, getLabel(), List.of(player.getName()));
            } else {
                getPlugin().log(
                        "Coop failed: " + player.getName() + "'s coop to " + user.getName() + " failed for island in "
                        + getWorld().getName());
            }
        } else if (clickType.equals(ClickType.SHIFT_LEFT)) {
            user.closeInventory();
            if (this.itc.getTrustCommand().canExecute(user, this.getLabel(), List.of(player.getName()))) {
                getPlugin().log("Trust: " + player.getName() + " trusted " + user.getName() + " to island in "
                        + getWorld().getName());
                this.itc.getTrustCommand().execute(user, getLabel(), List.of(player.getName()));
            } else {
                getPlugin().log("Trust failed: " + player.getName() + "'s trust failed for " + user.getName()
                        + " for island in "
                        + getWorld().getName());
            }
        }
        return true;
    }

    private PanelItem getBlankBackground() {
        return new PanelItemBuilder()
                .icon(Objects.requireNonNullElse(background.icon(), new ItemStack(Material.BARRIER)))
                .name((Objects.requireNonNullElse(background.title(), ""))).build();
    }
}
