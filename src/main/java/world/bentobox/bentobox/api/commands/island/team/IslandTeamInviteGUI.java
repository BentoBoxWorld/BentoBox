package world.bentobox.bentobox.api.commands.island.team;

import java.io.File;
import java.util.List;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.Player;
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

public class IslandTeamInviteGUI {

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

    private PanelItem createSearchButton(ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        checkTemplate(template);
        PanelItemBuilder pib = new PanelItemBuilder().name(user.getTranslation(template.title())).icon(template.icon())
                .clickHandler((panel, user, clickType, clickSlot) -> {
                    user.closeInventory();
                    new ConversationFactory(BentoBox.getInstance()).withLocalEcho(false).withTimeout(90)
                            .withModality(false).withFirstPrompt(new InviteNamePrompt())
                            .buildConversation(user.getPlayer()).begin();
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
        long count = itic.getWorld().getPlayers().stream().filter(player -> user.getPlayer().canSee(player))
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
            plugin.logError("Icon in template is missing or unknown! " + template.toString());
        }
        if (template.title() == null) {
            plugin.logError("Title in template is missing! " + template.toString());
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
        Island island = plugin.getIslands().getPrimaryIsland(itic.getWorld(), user.getUniqueId());
        if (island == null) {
            return this.getBlankBackground();
        }
        if (page < 0) {
            page = 0;
        }
        return itic.getWorld().getPlayers().stream().filter(player -> user.getPlayer().canSee(player))
                .filter(player -> this.searchName.isBlank() ? true
                        : player.getName().toLowerCase().contains(searchName.toLowerCase()))
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
                        (panel, user, clickType, clickSlot) -> clickHandler(panel, user, clickType, clickSlot, player,
                                template.actions()))
                .build();
    }

    private boolean clickHandler(Panel panel, User user, ClickType clickType, int clickSlot, Player player,
            @NonNull List<ActionRecords> list) {
        if (!list.stream().anyMatch(ar -> clickType.equals(ar.clickType()))) {
            // If the click type is not in the template, don't do anything
            return true;
        }
        if (clickType.equals(ClickType.LEFT)) {
            user.closeInventory();
            if (itic.canExecute(user, itic.getLabel(), List.of(player.getName()))) {
                plugin.log("Invite sent to: " + player.getName() + " by " + user.getName() + " to join island in "
                        + itic.getWorld().getName());
                itic.execute(user, itic.getLabel(), List.of(player.getName()));
            } else {
                plugin.log("Invite failed: " + player.getName() + " by " + user.getName() + " to join island in "
                        + itic.getWorld().getName());
            }
        } else if (clickType.equals(ClickType.RIGHT)) {
            user.closeInventory();
            if (this.itc.getCoopCommand().canExecute(user, itic.getLabel(), List.of(player.getName()))) {
                plugin.log("Coop: " + player.getName() + " cooped " + user.getName() + " to island in "
                        + itic.getWorld().getName());
                this.itc.getCoopCommand().execute(user, itic.getLabel(), List.of(player.getName()));
            } else {
                plugin.log(
                        "Coop failed: " + player.getName() + "'s coop to " + user.getName() + " failed for island in "
                                + itic.getWorld().getName());
            }
        } else if (clickType.equals(ClickType.SHIFT_LEFT)) {
            user.closeInventory();
            if (this.itc.getTrustCommand().canExecute(user, itic.getLabel(), List.of(player.getName()))) {
                plugin.log("Trust: " + player.getName() + " trusted " + user.getName() + " to island in "
                        + itic.getWorld().getName());
                this.itc.getTrustCommand().execute(user, itic.getLabel(), List.of(player.getName()));
            } else {
                plugin.log("Trust failed: " + player.getName() + "'s trust failed for " + user.getName()
                        + " for island in "
                        + itic.getWorld().getName());
            }
        }
        return true;
    }

    private PanelItem getBlankBackground() {
        return new PanelItemBuilder()
                .icon(Objects.requireNonNullElse(background.icon(), new ItemStack(Material.BARRIER)))
                .name((Objects.requireNonNullElse(background.title(), ""))).build();
    }

    class InviteNamePrompt extends StringPrompt {

        @Override
        @NonNull
        public String getPromptText(@NonNull ConversationContext context) {
            return user.getTranslation("commands.island.team.invite.gui.enter-name");
        }

        @Override
        public Prompt acceptInput(@NonNull ConversationContext context, String input) {
            // TODO remove this and pass the options back to the GUI
            if (itic.canExecute(user, itic.getLabel(), List.of(input))) {
                if (itic.execute(user, itic.getLabel(), List.of(input))) {
                    return Prompt.END_OF_CONVERSATION;
                }
            }
            // Set the search item to what was entered
            searchName = input;
            // Return to the GUI but give a second for the error to show
            // TODO: return the failed input and display the options in the GUI.
            Bukkit.getScheduler().runTaskLater(BentoBox.getInstance(), () -> build(user), 20L);
            return Prompt.END_OF_CONVERSATION;
        }

    }
}
