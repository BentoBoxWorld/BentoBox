package world.bentobox.bentobox.panels;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.customizable.AbstractPanel;
import world.bentobox.bentobox.util.PlaceholderGrouper;
import world.bentobox.bentobox.util.PlaceholderGrouper.Series;
import world.bentobox.bentobox.util.PlaceholderGrouper.Single;
import world.bentobox.bentobox.util.PlaceholderNode;
import world.bentobox.bentobox.util.PlaceholderNode.NodeType;

/**
 * Per-source Placeholder List panel with hierarchical {@code _}-segment navigation.
 * <p>
 * Displays placeholders for a single source (BentoBox core when {@code addon} is
 * {@code null}, or a specific addon) as a navigable tree whose nodes are formed by
 * splitting placeholder keys on {@code _}. Numeric-suffix series (e.g.
 * {@code island_member_name_1..50}) are collapsed into a single entry.
 * </p>
 * <p>
 * Node types and icons:
 * <ul>
 *   <li><b>LEAF</b> — {@link Material#PAPER} — a single placeholder; click to toggle.</li>
 *   <li><b>FOLDER</b> — {@link Material#CHEST} — a group of sub-placeholders; click to drill in.</li>
 *   <li><b>LEAF_FOLDER</b> — {@link Material#BOOK} — placeholder at this exact path AND sub-entries;
 *       left-click to toggle, right-click to drill in.</li>
 *   <li><b>SERIES</b> — {@link Material#NAME_TAG} — a collapsed numeric series; click to toggle all.</li>
 *   <li><b>LEAF_SERIES</b> — {@link Material#WRITABLE_BOOK} — placeholder at this exact path AND a
 *       numeric series sharing the same stem; left-click to toggle the single placeholder,
 *       right-click to toggle all series members.</li>
 * </ul>
 * </p>
 * <p>
 * The layout is driven by {@code BentoBox/panels/placeholder_list_panel.yml} and
 * can be customised by server admins by editing that file.
 * </p>
 *
 * @since 3.2.0
 */
public class PlaceholderListPanel extends AbstractPanel {

    private static final String PANEL_NAME = "placeholder_list_panel";
    private static final String PLACEHOLDER_TYPE = "PLACEHOLDER";
    private static final String BACK_TYPE = "BACK";

    /** {@code null} means the BentoBox core expansion. */
    @Nullable
    private final Addon addon;

    /** The expansion identifier used in {@code %identifier_placeholder%} format. */
    private final String expansionId;

    /** Root of the placeholder trie, built once in the constructor. */
    private final PlaceholderNode trieRoot;

    /**
     * Navigation breadcrumb. The last element is the current node being viewed.
     * The root element is always {@link #trieRoot}.
     */
    private final List<PlaceholderNode> navigationPath;

    /** Pre-computed compressed children of the current node (what the panel shows). */
    private List<PlaceholderNode> displayItems;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    private PlaceholderListPanel(@NonNull CompositeCommand command, @NonNull User user,
            @Nullable Addon addon) {
        super(command, user);
        this.addon = addon;

        if (addon == null) {
            expansionId = plugin.getName().toLowerCase();
        } else {
            expansionId = addon.getDescription().getName().toLowerCase();
        }

        Set<String> keys = addon == null
                ? plugin.getPlaceholdersManager().getRegisteredBentoBoxPlaceholders()
                : plugin.getPlaceholdersManager().getRegisteredPlaceholders(addon);

        List<PlaceholderGrouper.PlaceholderItem> grouped = PlaceholderGrouper.group(keys,
                key -> addon == null
                        ? plugin.getPlaceholdersManager().getPlaceholderDescription(key)
                        : plugin.getPlaceholdersManager().getPlaceholderDescription(addon, key));

        trieRoot = PlaceholderNode.buildTrie(grouped);
        navigationPath = new ArrayList<>();
        navigationPath.add(trieRoot);
        displayItems = trieRoot.getDisplayChildren();
    }

    /**
     * Opens the Placeholder List panel for the given source.
     * @param command the command that triggered the panel.
     * @param user the player to open the panel for.
     * @param addon the addon whose placeholders to show, or {@code null} for BentoBox core.
     */
    public static void openPanel(@NonNull CompositeCommand command, @NonNull User user,
            @Nullable Addon addon) {
        new PlaceholderListPanel(command, user, addon).build();
    }

    // -------------------------------------------------------------------------
    // Build
    // -------------------------------------------------------------------------

    @Override
    protected void build() {
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.template(PANEL_NAME, new File(plugin.getDataFolder(), "panels"));
        panelBuilder.user(user);
        panelBuilder.world(user.getWorld());

        // Title shows a breadcrumb: "SourceName > segment > segment"
        panelBuilder.parameters("[name]", buildBreadcrumb());

        panelBuilder.registerTypeBuilder(PLACEHOLDER_TYPE, this::createNodeButton);
        panelBuilder.registerTypeBuilder(BACK_TYPE, this::createBackButton);
        panelBuilder.registerTypeBuilder(NEXT, this::createNextButton);
        panelBuilder.registerTypeBuilder(PREVIOUS, this::createPreviousButton);

        panelBuilder.build();
    }

    // -------------------------------------------------------------------------
    // Button creators
    // -------------------------------------------------------------------------

    @Nullable
    private PanelItem createNodeButton(@NonNull ItemTemplateRecord template,
            TemplatedPanel.ItemSlot slot) {
        int perPage = slot.amountMap().getOrDefault(PLACEHOLDER_TYPE, 1);
        int index = pageIndex * perPage + slot.slot();
        if (index >= displayItems.size()) {
            return null;
        }

        PlaceholderNode node = displayItems.get(index);
        NodeType type = node.getType();

        PanelItemBuilder builder = new PanelItemBuilder();

        // Icon: type-specific default, overridden by template if provided
        if (template.icon() != null) {
            builder.icon(template.icon().clone());
        } else {
            builder.icon(iconForType(type));
        }

        // Name, description, and glow depend on the node type
        switch (type) {
            case LEAF       -> buildLeafButton(builder, node);
            case FOLDER     -> buildFolderButton(builder, node);
            case LEAF_FOLDER -> buildLeafFolderButton(builder, node);
            case SERIES     -> buildSeriesButton(builder, node);
            case LEAF_SERIES -> buildLeafSeriesButton(builder, node);
        }

        builder.clickHandler((panel, u, clickType, i) -> {
            handleNodeClick(node, type, clickType);
            return true;
        });

        return builder.build();
    }

    private void buildLeafButton(PanelItemBuilder builder, PlaceholderNode node) {
        Single leaf = node.getLeaf();
        String fullPh = "%" + expansionId + "_" + leaf.key() + "%";
        boolean enabled = isEnabled(leaf.key());

        builder.name(user.getTranslation("panels.placeholder-list.buttons.leaf.name",
                "[placeholder]", fullPh));

        List<String> lore = new ArrayList<>();
        if (!leaf.description().isBlank()) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf.description",
                    "[description]", leaf.description()));
        }
        if (!enabled) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf.disabled"));
        }
        lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf.hint"));
        builder.description(lore);
        builder.glow(!enabled);
    }

    private void buildFolderButton(PanelItemBuilder builder, PlaceholderNode node) {
        int count = node.totalPlaceholderCount();
        builder.name(user.getTranslation("panels.placeholder-list.buttons.folder.name",
                "[label]", node.getLabel()));
        builder.description(
                user.getTranslation("panels.placeholder-list.buttons.folder.description",
                        "[count]", String.valueOf(count)),
                user.getTranslation("panels.placeholder-list.buttons.folder.hint"));
    }

    private void buildLeafFolderButton(PanelItemBuilder builder, PlaceholderNode node) {
        Single leaf = node.getLeaf();
        String fullPh = "%" + expansionId + "_" + leaf.key() + "%";
        boolean enabled = isEnabled(leaf.key());
        int childCount = node.totalPlaceholderCount() - 1; // exclude the leaf itself

        builder.name(user.getTranslation("panels.placeholder-list.buttons.leaf.name",
                "[placeholder]", fullPh));

        List<String> lore = new ArrayList<>();
        if (!leaf.description().isBlank()) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf.description",
                    "[description]", leaf.description()));
        }
        lore.add(user.getTranslation("panels.placeholder-list.buttons.folder.description",
                "[count]", String.valueOf(childCount)));
        if (!enabled) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf.disabled"));
        }
        lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf-folder.hint"));
        builder.description(lore);
        builder.glow(!enabled);
    }

    private void buildSeriesButton(PanelItemBuilder builder, PlaceholderNode node) {
        Series series = node.getSeries();
        String fullPh = "%" + expansionId + "_" + series.displayKey() + "%";
        boolean anyDisabled = series.rawKeys().stream().anyMatch(k -> !isEnabled(k));

        builder.name(user.getTranslation("panels.placeholder-list.buttons.series.name",
                "[placeholder]", fullPh));

        List<String> lore = new ArrayList<>();
        if (!series.description().isBlank()) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.series.description",
                    "[description]", series.description()));
        }
        lore.add(user.getTranslation("panels.placeholder-list.buttons.series.range",
                "[count]", String.valueOf(series.rawKeys().size()),
                "[min]", String.valueOf(series.min()),
                "[max]", String.valueOf(series.max())));
        if (anyDisabled) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.series.disabled"));
        }
        lore.add(user.getTranslation("panels.placeholder-list.buttons.series.hint"));
        builder.description(lore);
        builder.glow(anyDisabled);
    }

    private void buildLeafSeriesButton(PanelItemBuilder builder, PlaceholderNode node) {
        Single leaf = node.getLeaf();
        Series series = node.getSeries();
        String leafPh = "%" + expansionId + "_" + leaf.key() + "%";
        boolean leafEnabled = isEnabled(leaf.key());
        boolean anySeriesDisabled = series.rawKeys().stream().anyMatch(k -> !isEnabled(k));

        builder.name(user.getTranslation("panels.placeholder-list.buttons.series.name",
                "[placeholder]", leafPh + " / " + series.displayKey() + "_{N}"));

        List<String> lore = new ArrayList<>();
        if (!leaf.description().isBlank()) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf.description",
                    "[description]", leaf.description()));
        }
        lore.add(user.getTranslation("panels.placeholder-list.buttons.series.range",
                "[count]", String.valueOf(series.rawKeys().size()),
                "[min]", String.valueOf(series.min()),
                "[max]", String.valueOf(series.max())));
        if (!leafEnabled || anySeriesDisabled) {
            lore.add(user.getTranslation("panels.placeholder-list.buttons.series.disabled"));
        }
        lore.add(user.getTranslation("panels.placeholder-list.buttons.leaf-series.hint"));
        builder.description(lore);
        builder.glow(!leafEnabled || anySeriesDisabled);
    }

    @Nullable
    private PanelItem createBackButton(@NonNull ItemTemplateRecord template,
            TemplatedPanel.ItemSlot slot) {
        PanelItemBuilder builder = new PanelItemBuilder();

        builder.icon(template.icon() != null ? template.icon().clone()
                : new ItemStack(Material.ARROW));

        builder.name(template.title() != null
                ? user.getTranslation(template.title())
                : user.getTranslation("panels.placeholder-list.buttons.back.name"));

        builder.description(template.description() != null
                ? user.getTranslation(template.description())
                : user.getTranslation("panels.placeholder-list.buttons.back.description"));

        builder.clickHandler((panel, u, clickType, i) -> {
            if (navigationPath.size() <= 1) {
                // At root → go back to the source selector
                PlaceholderPanel.openPanel(command, user);
            } else {
                // Go up one level in the tree
                navigationPath.remove(navigationPath.size() - 1);
                PlaceholderNode parent = navigationPath.get(navigationPath.size() - 1);
                displayItems = parent.getDisplayChildren();
                pageIndex = 0;
                build();
            }
            return true;
        });

        addTooltips(builder, template);
        return builder.build();
    }

    @Override
    @Nullable
    protected PanelItem createNextButton(@NonNull ItemTemplateRecord template,
            TemplatedPanel.ItemSlot slot) {
        int size = displayItems.size();
        int perPage = slot.amountMap().getOrDefault(PLACEHOLDER_TYPE, 1);
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
                    "[number]", String.valueOf(nextPage)));
        }

        builder.clickHandler((panel, u, clickType, i) -> {
            template.actions().forEach(action -> {
                if ((clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                        && NEXT.equalsIgnoreCase(action.actionType())) {
                    pageIndex++;
                    build();
                }
            });
            return true;
        });

        addTooltips(builder, template);
        return builder.build();
    }

    @Override
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
                    "[number]", String.valueOf(prevPage)));
        }

        builder.clickHandler((panel, u, clickType, i) -> {
            template.actions().forEach(action -> {
                if ((clickType == action.clickType() || action.clickType() == ClickType.UNKNOWN)
                        && PREVIOUS.equalsIgnoreCase(action.actionType())) {
                    pageIndex--;
                    build();
                }
            });
            return true;
        });

        addTooltips(builder, template);
        return builder.build();
    }

    // -------------------------------------------------------------------------
    // Click handling
    // -------------------------------------------------------------------------

    private void handleNodeClick(PlaceholderNode node, NodeType type, ClickType clickType) {
        switch (type) {
            case LEAF -> {
                toggleLeaf(node.getLeaf().key());
                build();
            }
            case FOLDER -> drillInto(node);
            case LEAF_FOLDER -> {
                if (clickType == ClickType.RIGHT) {
                    drillInto(node);
                } else {
                    toggleLeaf(node.getLeaf().key());
                    build();
                }
            }
            case SERIES -> {
                toggleSeries(node.getSeries());
                build();
            }
            case LEAF_SERIES -> {
                if (clickType == ClickType.RIGHT) {
                    toggleSeries(node.getSeries());
                } else {
                    toggleLeaf(node.getLeaf().key());
                }
                build();
            }
        }
    }

    private void drillInto(PlaceholderNode node) {
        navigationPath.add(node);
        displayItems = node.getDisplayChildren();
        pageIndex = 0;
        build();
    }

    private void toggleLeaf(String key) {
        boolean current = isEnabled(key);
        setEnabled(key, !current);
    }

    private void toggleSeries(Series series) {
        // If all enabled → disable all; otherwise enable all
        boolean allEnabled = series.rawKeys().stream().allMatch(this::isEnabled);
        boolean newState = !allEnabled;
        series.rawKeys().forEach(k -> setEnabled(k, newState));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildBreadcrumb() {
        String sourceName = addon == null
                ? plugin.getName()
                : addon.getDescription().getName();
        if (navigationPath.size() <= 1) {
            return sourceName;
        }
        StringBuilder sb = new StringBuilder(sourceName);
        // Skip index 0 (root has empty label)
        for (int i = 1; i < navigationPath.size(); i++) {
            sb.append(" > ").append(navigationPath.get(i).getLabel());
        }
        return sb.toString();
    }

    @NonNull
    private Material iconForType(NodeType type) {
        return switch (type) {
            case LEAF        -> Material.PAPER;
            case FOLDER      -> Material.CHEST;
            case LEAF_FOLDER -> Material.BOOK;
            case SERIES      -> Material.NAME_TAG;
            case LEAF_SERIES -> Material.WRITABLE_BOOK;
        };
    }

    private boolean isEnabled(String key) {
        return addon == null
                ? plugin.getPlaceholdersManager().isPlaceholderEnabled(key)
                : plugin.getPlaceholdersManager().isPlaceholderEnabled(addon, key);
    }

    private void setEnabled(String key, boolean enabled) {
        if (addon == null) {
            plugin.getPlaceholdersManager().setPlaceholderEnabled(key, enabled);
        } else {
            plugin.getPlaceholdersManager().setPlaceholderEnabled(addon, key, enabled);
        }
    }

    private void addTooltips(@NonNull PanelItemBuilder builder, @NonNull ItemTemplateRecord template) {
        List<String> tooltips = template.actions().stream()
                .filter(a -> a.tooltip() != null)
                .map(a -> user.getTranslation(a.tooltip()))
                .filter(t -> !t.isBlank())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (!tooltips.isEmpty()) {
            builder.description("");
            builder.description(tooltips);
        }
    }
}
