package world.bentobox.bentobox.api.commands.island.team;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.prompts.Prompts;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord.ActionRecords;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord.TemplateItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

public class IslandTeamInviteGUI {

    /** Locale key for the (customisable) prospect button name. Placeholders: [name], [display_name]. */
    private static final String PROSPECT_NAME_REF = "commands.island.team.invite.gui.buttons.member.name";
    /** Locale key for an optional (customisable) prospect description line. Placeholders: [name], [display_name]. */
    private static final String PROSPECT_DESC_REF = "commands.island.team.invite.gui.buttons.member.description";

    private final IslandTeamInviteCommand itic;
    private final IslandTeamCommand itc;
    private @Nullable TemplateItem border;
    private @Nullable TemplateItem background;
    private User user;
    private long page = 0; // This number by 35
    private final boolean inviteCmd;
    private static final long PER_PAGE = 35;
    private String searchName = "";
    private final BentoBox plugin;
    private final Island island;
    /** What a left click on a player does. Changed with the MODE button (#3137). */
    private Mode mode = Mode.INVITE;

    /**
     * What a left click on a player in the list does. Coop and trust also have their own right and
     * shift click, but a touch-screen player can only tap, so the mode makes them reachable.
     */
    enum Mode {
        INVITE("commands.island.team.invite.gui.tips.LEFT.invite"),
        COOP("commands.island.team.invite.gui.tips.RIGHT.coop"),
        TRUST("commands.island.team.invite.gui.tips.SHIFT_LEFT.trust");

        /** Tooltip used when the template has no action named after this mode. */
        private final String defaultTooltip;

        Mode(String defaultTooltip) {
            this.defaultTooltip = defaultTooltip;
        }

        String key() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public IslandTeamInviteGUI(IslandTeamCommand itc, boolean invitedCmd, Island island) {
        this.island = island;
        this.plugin = itc.getPlugin();
        this.inviteCmd = invitedCmd;
        itic = itc.getInviteCommand();
        this.itc = itc;
        // Panels
        if (!new File(plugin.getDataFolder() + File.separator + "panels", "team_invite_panel.yml")
                .exists()) {
            plugin.saveResource("panels/team_invite_panel.yml", false);
        }
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

        panelBuilder.template("team_invite_panel", new File(plugin.getDataFolder(), "panels"));

        panelBuilder.parameters("[name]", user.getName(), "[display_name]", user.getDisplayName());

        panelBuilder.registerTypeBuilder("PROSPECT", this::createProspectButton);
        panelBuilder.registerTypeBuilder("PREVIOUS", this::createPreviousButton);
        panelBuilder.registerTypeBuilder("NEXT", this::createNextButton);
        panelBuilder.registerTypeBuilder("SEARCH", this::createSearchButton);
        panelBuilder.registerTypeBuilder("BACK", this::createBackButton);
        panelBuilder.registerTypeBuilder("MODE", this::createModeButton);
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
                        new IslandTeamGUI(plugin, itc, user, island).build();
                    }
                    return true;
                }).build();
    }

    /**
     * The modes this viewer can use: inviting always, coop and trust if those ranks exist and the
     * viewer has the permission and rank for the command.
     */
    private List<Mode> availableModes() {
        List<Mode> modes = new ArrayList<>();
        modes.add(Mode.INVITE);
        if (canUse(itc.getCoopCommand(), Mode.COOP)) {
            modes.add(Mode.COOP);
        }
        if (canUse(itc.getTrustCommand(), Mode.TRUST)) {
            modes.add(Mode.TRUST);
        }
        return modes;
    }

    private boolean canUse(@Nullable CompositeCommand command, Mode commandMode) {
        if (command == null || island == null) {
            return false;
        }
        return user.hasPermission(Objects.requireNonNullElse(command.getPermission(), ""))
                && island.getRank(user) >= island.getRankCommand(itc.getLabel() + " " + commandMode.key());
    }

    /**
     * A button that changes what a left click on a player does: invite, coop or trust. Any click
     * moves to the next mode. It is left out when inviting is the only thing the viewer can do.
     */
    private PanelItem createModeButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        List<Mode> modes = availableModes();
        if (modes.size() < 2) {
            return getBlankBorder();
        }
        if (!modes.contains(mode)) {
            mode = Mode.INVITE;
        }
        final String ref = "commands.island.team.invite.gui.mode.";
        return new PanelItemBuilder()
                .icon(template.icon() != null ? template.icon().clone() : new ItemStack(Material.COMPARATOR))
                .name(user.getTranslation(ref + "name"))
                .description(user.getTranslation(ref + mode.key()))
                .description(user.getTranslation(ref + "change"))
                .clickHandler((panel, clicker, clickType, clickSlot) -> {
                    mode = modes.get((modes.indexOf(mode) + 1) % modes.size());
                    clicker.getPlayer().playSound(clicker.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
                    build(clicker);
                    return true;
                }).build();
    }

    private PanelItem createSearchButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        checkTemplate(template);
        PanelItemBuilder pib = new PanelItemBuilder().name(user.getTranslation(template.title())).icon(template.icon())
                .clickHandler((panel, user, clickType, clickSlot) -> {
                    user.closeInventory();
                    Prompts.text(user).title("commands.island.team.invite.gui.enter-name")
                            .initial(searchName).optional().onSubmit(this::nameEntered).onCancel(() -> build(user)).open();
                    return true;
                });
        if (!this.searchName.isBlank()) {
            pib.description(user.getTranslation(Objects
                    .requireNonNullElse(template.description(),
                            "commands.island.team.invite.gui.button.searching"),
                    TextVariables.NAME, searchName));
        }
        return pib.build();
    }

    private PanelItem createNextButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        checkTemplate(template);
        long count = itc.getWorld().getPlayers().stream().filter(player -> user.getPlayer().canSee(player))
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
            plugin.logError("Icon in template is missing or unknown! " + template);
        }
        if (template.title() == null) {
            plugin.logError("Title in template is missing! " + template);
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
        Island is = plugin.getIslands().getPrimaryIsland(itc.getWorld(), user.getUniqueId());
        if (is == null) {
            return this.getBlankBackground();
        }
        if (page < 0) {
            page = 0;
        }
        // Stream of all players that the user can see
        Stream<Player> visiblePlayers = itc.getWorld().getPlayers().stream().filter(user.getPlayer()::canSee);

        // Filter players based on searchName if it's not blank, and ensure they're not the user
        Stream<Player> filteredPlayers = visiblePlayers
                .filter(player -> this.searchName.isBlank()
                        || player.getName().toLowerCase().contains(searchName.toLowerCase()))
                .filter(player -> !player.equals(user.getPlayer()));

        // Skipping to the correct pagination slot, then finding the first player
        Optional<Player> playerOptional = filteredPlayers.skip(slot.slot() + page * PER_PAGE).findFirst();

        // Map the player to a prospect or return a blank background if not found
        return playerOptional.map(player -> getProspect(player, template)).orElse(this.getBlankBackground());
    }

    private PanelItem getProspect(Player player, ItemTemplateRecord template) {
        // Customisable prospect name ([name] / [display_name] placeholders; defaults to the display name).
        // getTranslation echoes the reference back when the key is missing, so fall back to the display name.
        String name = user.getTranslation(PROSPECT_NAME_REF, TextVariables.NAME, player.getName(),
                TextVariables.DISPLAY_NAME, player.getDisplayName());
        if (name.equals(PROSPECT_NAME_REF)) {
            name = player.getDisplayName();
        }
        // Check if the prospect has already been invited
        if (this.itc.isInvited(player.getUniqueId())
                && user.getUniqueId().equals(this.itc.getInvite(player.getUniqueId()).getInviter())) {
            return new PanelItemBuilder().icon(player.getName()).name(name)
                    .description(user.getTranslation("commands.island.team.invite.gui.button.already-invited")).build();
        }
        List<String> desc = new ArrayList<>();
        // Optional customisable description line, shown before the action tips when set (blank by default,
        // and skipped when the key is missing so an older locale file does not leak the raw key).
        String descLine = user.getTranslation(PROSPECT_DESC_REF, TextVariables.NAME, player.getName(),
                TextVariables.DISPLAY_NAME, player.getDisplayName());
        if (!descLine.isBlank() && !descLine.equals(PROSPECT_DESC_REF)) {
            desc.add(descLine);
        }
        template.actions().stream().map(ar -> user
                .getTranslation("commands.island.team.invite.gui.tips." + ar.clickType().name() + ".name")
                + " " + user.getTranslation(tooltipFor(ar, template.actions()))).forEach(desc::add);
        return new PanelItemBuilder().icon(player.getName()).name(name).description(desc)
                .clickHandler(
                        (panel, user, clickType, clickSlot) -> clickHandler(user, clickType, player,
                                template.actions()))
                .build();
    }

    /**
     * The tooltip to show for an action. The left click does whatever the current mode is, so in
     * coop or trust mode its tooltip is the one that belongs to that action.
     */
    private String tooltipFor(ActionRecords action, @NonNull List<ActionRecords> actions) {
        if (mode == Mode.INVITE || !ClickType.LEFT.equals(action.clickType())) {
            return action.tooltip();
        }
        return actions.stream().filter(ar -> mode.key().equalsIgnoreCase(ar.actionType()))
                .map(ActionRecords::tooltip).filter(Objects::nonNull).findFirst().orElse(mode.defaultTooltip);
    }

    private boolean clickHandler(User user, ClickType clickType, Player player, @NonNull List<ActionRecords> list) {
        if (list.stream().noneMatch(ar -> clickType.equals(ar.clickType()))) {
            // If the click type is not in the template, don't do anything
            return true;
        }
        if (clickType.equals(ClickType.LEFT)) {
            switch (mode) {
            case COOP -> coop(user, player);
            case TRUST -> trust(user, player);
            default -> invite(user, player);
            }
        } else if (clickType.equals(ClickType.RIGHT)) {
            coop(user, player);
        } else if (clickType.equals(ClickType.SHIFT_LEFT)) {
            trust(user, player);
        }
        return true;
    }

    private void invite(User user, Player player) {
        // Close inventory after one tick to allow the no pickup click return to occur
        Bukkit.getScheduler().runTask(plugin, user::closeInventory);
        if (itic.canExecute(user, itic.getLabel(), List.of(player.getName()))) {
            plugin.log("Invite sent to: " + player.getName() + " by " + user.getName() + " to join island in "
                    + itc.getWorld().getName());
            itic.execute(user, itic.getLabel(), List.of(player.getName()));
        } else {
            plugin.log("Invite failed: " + player.getName() + " by " + user.getName() + " to join island in "
                    + itc.getWorld().getName());
        }
    }

    private void coop(User user, Player player) {
        if (itc.getCoopCommand() == null) {
            // The coop rank does not exist on this server
            return;
        }
        // Close inventory after one tick to allow the no pickup click return to occur
        Bukkit.getScheduler().runTask(plugin, user::closeInventory);
        if (this.itc.getCoopCommand().canExecute(user, itic.getLabel(), List.of(player.getName()))) {
            plugin.log("Coop: " + player.getName() + " cooped " + user.getName() + " to island in "
                    + itc.getWorld().getName());
            this.itc.getCoopCommand().execute(user, itic.getLabel(), List.of(player.getName()));
        } else {
            plugin.log("Coop failed: " + player.getName() + "'s coop to " + user.getName()
                    + " failed for island in " + itc.getWorld().getName());
        }
    }

    private void trust(User user, Player player) {
        if (itc.getTrustCommand() == null) {
            // The trusted rank does not exist on this server
            return;
        }
        // Close inventory after one tick to allow the no pickup click return to occur
        Bukkit.getScheduler().runTask(plugin, user::closeInventory);
        if (this.itc.getTrustCommand().canExecute(user, itic.getLabel(), List.of(player.getName()))) {
            plugin.log("Trust: " + player.getName() + " trusted " + user.getName() + " to island in "
                    + itc.getWorld().getName());
            this.itc.getTrustCommand().execute(user, itic.getLabel(), List.of(player.getName()));
        } else {
            plugin.log("Trust failed: " + player.getName() + "'s trust failed for " + user.getName()
                    + " for island in " + itc.getWorld().getName());
        }
    }

    private PanelItem getBlankBackground() {
        return new PanelItemBuilder()
                .icon(Objects.requireNonNullElse(background.icon(), new ItemStack(Material.BARRIER)))
                .name((Objects.requireNonNullElse(background.title(), ""))).build();
    }

    /**
     * Acts on a name typed into the search prompt. If it names a player who can be invited,
     * they are invited. Otherwise it becomes the search text and the panel reopens. Nothing
     * typed clears the search.
     *
     * @param input what the player typed
     */
    void nameEntered(String input) {
        if (input.isBlank()) {
            searchName = "";
            build(user);
            return;
        }
        if (itic.canExecute(user, itic.getLabel(), List.of(input))
                && itic.execute(user, itic.getLabel(), List.of(input))) {
            return;
        }
        // Set the search item to what was entered
        searchName = input;
        // Return to the GUI but give a second for the error to show
        Bukkit.getScheduler().runTaskLater(BentoBox.getInstance(), () -> build(user), 20L);
    }
}
