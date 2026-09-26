package world.bentobox.bentobox.panels.customizable;

import java.io.File;
import java.util.List;

import org.bukkit.World;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.PanelListener;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.panels.reader.PanelTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener;

/**
 * The Command Ranks panel, opened from the {@code COMMAND_RANKS} flag in the settings panel. It
 * lists the commands whose minimum rank an island can set, laid out by
 * {@code panels/command_ranks_panel.yml}. A game mode addon may ship its own copy, which then takes
 * precedence for that game mode.
 * <p>
 * Button types:
 * <ul>
 * <li>{@code COMMAND}: one slot of the paged command list. Left and right clicks cycle the rank.</li>
 * <li>{@code NEXT} / {@code PREVIOUS}: paging.</li>
 * </ul>
 * @since 3.23.3
 */
public class CommandRanksPanel extends AbstractPanel implements PanelListener {

    /** The template file name, without extension. */
    public static final String COMMAND_RANKS_PANEL = "command_ranks_panel";
    /** Button type: one slot of the paged command list. */
    public static final String COMMAND = "COMMAND";

    private final World world;
    private final Island island;
    private final CommandRankClickListener commandRanks;
    /** The commands the viewer may see, in display order. */
    private List<String> commands = List.of();
    @Nullable
    private TemplatedPanel panel;
    private boolean refreshing;
    private boolean closed;

    /**
     * @param command the game mode's player command, which provides the plugin and game mode
     * @param user the viewer
     * @param world the world the island is in
     * @param island the island whose command ranks are shown
     * @param commandRanks the flag's click handler, which lists and draws the commands
     */
    protected CommandRanksPanel(@NonNull CompositeCommand command, @NonNull User user, @NonNull World world,
            @NonNull Island island, @NonNull CommandRankClickListener commandRanks) {
        super(command, user);
        this.world = world;
        this.island = island;
        this.commandRanks = commandRanks;
    }

    /**
     * Opens the Command Ranks panel.
     * @param command the game mode's player command
     * @param user the viewer
     * @param world the world the island is in
     * @param island the island whose command ranks are shown
     * @param commandRanks the flag's click handler, which lists and draws the commands
     * @return {@code false} if the template could not be loaded and nothing was opened
     */
    public static boolean openPanel(@NonNull CompositeCommand command, @NonNull User user, @NonNull World world,
            @NonNull Island island, @NonNull CommandRankClickListener commandRanks) {
        return new CommandRanksPanel(command, user, world, island, commandRanks).open();
    }

    /**
     * Loads the template and opens the panel.
     * @return {@code false} if the template could not be loaded and nothing was opened
     */
    protected boolean open() {
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        if (command.getAddon() instanceof GameModeAddon gma && doesCustomPanelExists(gma, COMMAND_RANKS_PANEL)) {
            // The game mode has its own command ranks panel
            panelBuilder.template(COMMAND_RANKS_PANEL, new File(gma.getDataFolder(), "panels"));
        } else {
            panelBuilder.template(COMMAND_RANKS_PANEL, new File(plugin.getDataFolder(), "panels"));
        }
        PanelTemplateRecord template = panelBuilder.getPanelTemplate();
        if (template == null) {
            return false;
        }
        panelBuilder.user(user).world(world).island(island).listener(this);
        panelBuilder.registerTypeBuilder(COMMAND, this::createCommandButton);
        panelBuilder.registerTypeBuilder(NEXT, this::createNextButton);
        panelBuilder.registerTypeBuilder(PREVIOUS, this::createPreviousButton);
        commands = commandRanks.getCommands(world, user);
        panel = panelBuilder.build();
        return true;
    }

    // ---------------------------------------------------------------------
    // Section: Lifecycle
    // ---------------------------------------------------------------------

    /**
     * Next and previous buttons call this; the panel is refreshed by {@link #refreshPanel()} after
     * every click anyway, so there is nothing to do here.
     */
    @Override
    protected void build() {
        // Refreshed by the listener after the click
    }

    @Override
    protected void onPageChanged() {
        // Refreshed by the listener after the click
    }

    @Override
    public void setup() {
        // Nothing to set up
    }

    @Override
    public void onInventoryClick(User user, InventoryClickEvent event) {
        // Clicks are handled by the buttons
    }

    @Override
    public void refreshPanel() {
        if (closed || panel == null) {
            return;
        }
        commands = commandRanks.getCommands(world, user);
        // Mark as refreshing so that the inventory close fired by a reopen is not treated as a
        // true close
        refreshing = true;
        try {
            panel.regenerate();
        } finally {
            refreshing = false;
        }
        closed = false;
    }

    @Override
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!refreshing) {
            closed = true;
        }
    }

    @Override
    public boolean hasClickCooldown() {
        return true;
    }

    @Override
    public boolean isActionableSlot(int rawSlot) {
        if (panel == null) {
            return false;
        }
        PanelItem item = panel.getItems().get(rawSlot);
        return item != null && item.getClickHandler().isPresent();
    }

    // ---------------------------------------------------------------------
    // Section: Buttons
    // ---------------------------------------------------------------------

    @Override
    protected int getPagedItemCount() {
        return commands.size();
    }

    @Override
    protected String getPagedItemType() {
        return COMMAND;
    }

    /**
     * A command slot: the next command of the page.
     */
    @Nullable
    private PanelItem createCommandButton(@NonNull ItemTemplateRecord template, TemplatedPanel.ItemSlot slot) {
        int index = pageIndex * getItemsPerPage(slot) + slot.slot();
        if (index >= commands.size()) {
            return null;
        }
        PanelItemBuilder builder = commandRanks.getPanelItemBuilder(commands.get(index), user, world, island,
                template);
        addTooltips(builder, template);
        return builder.build();
    }

    // ---------------------------------------------------------------------
    // Section: Getters
    // ---------------------------------------------------------------------

    /**
     * @return the commands the viewer may see, in display order
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * @return the current page, starting at 0
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * @return the panel that is open, or null if none
     */
    @Nullable
    public TemplatedPanel getPanel() {
        return panel;
    }
}
