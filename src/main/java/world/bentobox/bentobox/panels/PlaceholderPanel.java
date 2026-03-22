package world.bentobox.bentobox.panels;

import java.io.File;
import java.util.Comparator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TemplatedPanel;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.panels.builders.TemplatedPanelBuilder;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.customizable.AbstractPanel;

/**
 * Top-level Placeholder Browser panel.
 * <p>
 * Shows a "BentoBox" entry for core placeholders plus one entry per addon that has
 * registered placeholders. Clicking an entry opens {@link PlaceholderListPanel}.
 * </p>
 * <p>
 * The layout is driven by {@code BentoBox/panels/placeholder_panel.yml} and can be
 * customised by server admins by editing that file.
 * </p>
 *
 * @since 3.2.0
 */
public class PlaceholderPanel extends AbstractPanel {

    private static final String PANEL_NAME = "placeholder_panel";
    private static final String BENTOBOX_TYPE = "BENTOBOX";
    private static final String ADDON_TYPE = "ADDON";

    /** Sorted list of addons that have at least one registered placeholder. */
    private final List<Addon> addonList;

    private PlaceholderPanel(@NonNull CompositeCommand command, @NonNull User user) {
        super(command, user);
        this.addonList = plugin.getPlaceholdersManager()
                .getAddonsWithPlaceholders().stream()
                .sorted(Comparator.comparing(a -> a.getDescription().getName()))
                .toList();
    }

    /**
     * Opens the Placeholder Browser panel for the given user.
     * @param command the command that triggered the panel.
     * @param user the player to open the panel for.
     */
    public static void openPanel(@NonNull CompositeCommand command, @NonNull User user) {
        new PlaceholderPanel(command, user).build();
    }

    @Override
    protected void build() {
        TemplatedPanelBuilder panelBuilder = new TemplatedPanelBuilder();
        panelBuilder.template(PANEL_NAME, new File(plugin.getDataFolder(), "panels"));
        panelBuilder.user(user);
        panelBuilder.world(user.getWorld());

        panelBuilder.registerTypeBuilder(BENTOBOX_TYPE, this::createBentoBoxButton);
        panelBuilder.registerTypeBuilder(ADDON_TYPE, this::createAddonButton);
        panelBuilder.registerTypeBuilder(NEXT, this::createNextButton);
        panelBuilder.registerTypeBuilder(PREVIOUS, this::createPreviousButton);

        panelBuilder.build();
    }

    // -------------------------------------------------------------------------
    // Button creators
    // -------------------------------------------------------------------------

    @Nullable
    private PanelItem createBentoBoxButton(@NonNull ItemTemplateRecord template,
            TemplatedPanel.ItemSlot slot) {
        int count = plugin.getPlaceholdersManager().getRegisteredBentoBoxPlaceholders().size();
        if (count == 0) {
            return null;
        }

        PanelItemBuilder builder = new PanelItemBuilder();
        builder.icon(template.icon() != null ? template.icon().clone() : new org.bukkit.inventory.ItemStack(Material.BOOK));

        if (template.title() != null) {
            builder.name(user.getTranslation(template.title()));
        } else {
            builder.name(user.getTranslation("panels.placeholder.buttons.bentobox.name"));
        }

        if (template.description() != null) {
            builder.description(user.getTranslation(template.description(),
                    TextVariables.NUMBER, String.valueOf(count)));
        } else {
            builder.description(user.getTranslation("panels.placeholder.buttons.bentobox.description",
                    TextVariables.NUMBER, String.valueOf(count)));
        }

        builder.clickHandler((panel, u, clickType, i) -> {
            template.actions().stream()
                    .filter(a -> a.clickType() == clickType || a.clickType() == ClickType.UNKNOWN)
                    .findFirst()
                    .ifPresent(a -> PlaceholderListPanel.openPanel(command, user, null));
            return true;
        });

        addTooltips(builder, template);
        return builder.build();
    }

    @Nullable
    private PanelItem createAddonButton(@NonNull ItemTemplateRecord template,
            TemplatedPanel.ItemSlot slot) {
        int index = pageIndex * slot.amountMap().getOrDefault(ADDON_TYPE, 1) + slot.slot();
        if (index >= addonList.size()) {
            return null;
        }
        Addon addon = addonList.get(index);
        int count = plugin.getPlaceholdersManager().getRegisteredPlaceholders(addon).size();

        PanelItemBuilder builder = new PanelItemBuilder();
        if (template.icon() != null) {
            builder.icon(template.icon().clone());
        } else {
            builder.icon(addon.getDescription().getIcon());
        }

        if (template.title() != null) {
            builder.name(user.getTranslation(template.title(),
                    TextVariables.NAME, addon.getDescription().getName()));
        } else {
            builder.name(user.getTranslation("panels.placeholder.buttons.addon.name",
                    TextVariables.NAME, addon.getDescription().getName()));
        }

        if (template.description() != null) {
            builder.description(user.getTranslation(template.description(),
                    TextVariables.NAME, addon.getDescription().getName(),
                    TextVariables.NUMBER, String.valueOf(count)));
        } else {
            builder.description(user.getTranslation("panels.placeholder.buttons.addon.description",
                    TextVariables.NAME, addon.getDescription().getName(),
                    TextVariables.NUMBER, String.valueOf(count)));
        }

        builder.clickHandler((panel, u, clickType, i) -> {
            template.actions().stream()
                    .filter(a -> a.clickType() == clickType || a.clickType() == ClickType.UNKNOWN)
                    .findFirst()
                    .ifPresent(a -> PlaceholderListPanel.openPanel(command, user, addon));
            return true;
        });

        addTooltips(builder, template);
        return builder.build();
    }

    @Override
    protected int getPagedItemCount() {
        return addonList.size();
    }

    @Override
    protected String getPagedItemType() {
        return ADDON_TYPE;
    }
}
