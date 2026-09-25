package world.bentobox.bentobox.panels.customizable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 */
public abstract class AbstractPanel {

    // ---------------------------------------------------------------------
    // Section: Constants
    // ---------------------------------------------------------------------

    /**
     * This constant is used for button to indicate that it is Language type.
     */
    public static final String LOCALE = "LOCALE";

    /**
     * This constant is used for button to indicate that it is previous page type.
     */
    public static final String PREVIOUS = "PREVIOUS";

    /**
     * This constant is used for button to indicate that it is next page type.
     */
    public static final String NEXT = "NEXT";

    /**
     * This constant is used for indicating that pages should contain numbering.
     */
    public static final String INDEXING = "indexing";

    /**
     * This constant stores value for SELECT action that is used in panels.
     */
    public static final String SELECT_ACTION = "SELECT";

    /**
     * This constant stores value for COMMANDS action that is used in panels.
     */
    public static final String COMMANDS_ACTION = "COMMANDS";

    /**
     * This constant stores value for AUTHORS label that is used in panels.
     */
    public static final String AUTHORS = "[authors]";

    /**
     * This constant stores value for SELECTED label that is used in panels.
     */
    public static final String SELECTED = "[selected]";

    /**
     * This variable allows to access plugin object.
     */
    protected final BentoBox plugin;

    /**
     * This variable stores main command that was triggered.
     */
    protected final CompositeCommand command;

    /**
     * This variable holds user who opens panel. Without it panel cannot be opened.
     */
    protected final User user;

    /**
     * This variable holds world where panel is opened. Without it panel cannot be opened.
     */
    protected String mainLabel;

    /**
     * This variable holds current pageIndex for multipage island choosing.
     */
    protected int pageIndex;

    protected AbstractPanel(CompositeCommand command, User user) {
        plugin = command.getPlugin();
        this.command = command;
        this.user = user;
        this.pageIndex = 0; // Start with the first page by default
    }

    /**
     * Returns the total number of paged items for default next/previous button logic.
     * Subclasses that rely on the default {@link #createNextButton} / {@link #createPreviousButton}
     * implementations must override this.
     */
    protected int getPagedItemCount() {
        return 0;
    }

    /**
     * Returns the item-type key used to look up the per-page amount in
     * {@link TemplatedPanel.ItemSlot#amountMap()}.
     * Subclasses that rely on the default {@link #createNextButton} / {@link #createPreviousButton}
     * implementations must override this.
     */
    protected String getPagedItemType() {
        return "";
    }

    /**
     * Returns how many paged items fit on one page: by default the number of slots in the
     * template that carry the {@link #getPagedItemType() paged item type}. Subclasses whose
     * templates reserve some of those slots for other purposes should override this.
     * @param slot the slot record, which carries the per-type slot counts
     * @return items per page, at least 1
     * @since 3.23.0
     */
    protected int getItemsPerPage(TemplatedPanel.ItemSlot slot) {
        return Math.max(1, slot.amountMap().getOrDefault(getPagedItemType(), 1));
    }

    @Nullable
    protected PanelItem createNextButton(@NonNull ItemTemplateRecord template,
            TemplatedPanel.ItemSlot slot) {
        int size = getPagedItemCount();
        int perPage = getItemsPerPage(slot);
        if (size <= perPage || (double) size / perPage <= pageIndex + 1) {
            return null;
        }

        int nextPage = pageIndex + 2;
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            var clone = template.icon().clone();
            if ((boolean) template.dataMap().getOrDefault(INDEXING, false)) {
                clone.setAmount(nextPage);
            }
            builder.icon(clone);
        }
        if (template.title() != null) {
            builder.name(user.getTranslation(template.title()));
        }
        if (template.description() != null) {
            builder.description(user.getTranslation(template.description(),
                    TextVariables.NUMBER, String.valueOf(nextPage)));
        }

        builder.clickHandler((panel, u, clickType, i) -> {
            template.actions().forEach(action -> {
                if ((clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                        && NEXT.equalsIgnoreCase(action.actionType())) {
                    pageIndex++;
                    onPageChanged();
                }
            });
            return true;
        });

        addTooltips(builder, template);
        return builder.build();
    }

    @Nullable
    protected PanelItem createPreviousButton(@NonNull ItemTemplateRecord template,
            TemplatedPanel.ItemSlot slot) {
        if (pageIndex == 0) {
            return null;
        }

        int prevPage = pageIndex;
        PanelItemBuilder builder = new PanelItemBuilder();

        if (template.icon() != null) {
            var clone = template.icon().clone();
            if ((boolean) template.dataMap().getOrDefault(INDEXING, false)) {
                clone.setAmount(prevPage);
            }
            builder.icon(clone);
        }
        if (template.title() != null) {
            builder.name(user.getTranslation(template.title()));
        }
        if (template.description() != null) {
            builder.description(user.getTranslation(template.description(),
                    TextVariables.NUMBER, String.valueOf(prevPage)));
        }

        builder.clickHandler((panel, u, clickType, i) -> {
            template.actions().forEach(action -> {
                if ((clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                        && PREVIOUS.equalsIgnoreCase(action.actionType())) {
                    pageIndex--;
                    onPageChanged();
                }
            });
            return true;
        });

        addTooltips(builder, template);
        return builder.build();
    }

    protected void addTooltips(@NonNull PanelItemBuilder builder, @NonNull ItemTemplateRecord template) {
        List<String> tooltips = template.actions().stream()
                .filter(a -> a.tooltip() != null)
                .map(a -> user.getTranslation(a.tooltip()))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
        if (!tooltips.isEmpty()) {
            builder.description("");
            builder.description(tooltips);
        }
    }

    // Abstract build method to allow each panel to define its own layout
    protected abstract void build();

    /**
     * Called by the default next/previous handlers once the page index has changed. By default
     * the panel is rebuilt. Panels whose {@link world.bentobox.bentobox.api.panels.PanelListener}
     * already refreshes them after every click should override this to do nothing, so that the
     * panel is not rebuilt twice.
     * @since 3.23.0
     */
    protected void onPageChanged() {
        build();
    }

    // Default method for pagination, can be overridden by subclasses if needed
    protected boolean hasNextPage(int elementListSize, int itemsPerPage) {
        return (pageIndex + 1) * itemsPerPage < elementListSize;
    }

    protected boolean hasPreviousPage() {
        return pageIndex > 0;
    }

    // Method to handle the click event on next/previous buttons
    protected boolean handlePageChange(ItemTemplateRecord.ActionRecords action, ClickType clickType,
            String actionType) {
        if ((clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                && actionType.equalsIgnoreCase(action.actionType())) {
            if (actionType.equalsIgnoreCase("NEXT")) {
                this.pageIndex++;
            } else if (actionType.equalsIgnoreCase(PREVIOUS)) {
                this.pageIndex--;
            }
            onPageChanged();
            return true;
        }
        return false;
    }

    /**
     * This method returns if panel with the requested name is located in GameModeAddon folder.
     * @param addon GameModeAddon that need to be checked.
     * @param name Name of the panel.
     * @return {@code true} if panel exists, {@code false} otherwise.
     */
    protected boolean doesCustomPanelExists(GameModeAddon addon, String name) {
        return addon.getDataFolder().exists() && new File(addon.getDataFolder(), "panels").exists()
                && new File(addon.getDataFolder(), "panels" + File.separator + name + ".yml").exists();
    }

}
