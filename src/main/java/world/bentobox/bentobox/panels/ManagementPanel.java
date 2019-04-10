package world.bentobox.bentobox.panels;

import org.bukkit.Material;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Poslovitch
 * @since 1.5.0
 */
public class ManagementPanel {

    private static final String LOCALE_REF = "management.panel.";
    private static final int[] PANES = {0, 4, 7, 8, 9, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};

    private ManagementPanel() {}

    /**
     * Dynamically creates the panel.
     * @param user the User to show the panel to
     */
    public static void openPanel(User user, View view) {
        BentoBox plugin = BentoBox.getInstance();

        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + "title"))
                .size(45);

        // Setup header and corner
        setupHeader(builder, user, view);
        for (int i : PANES) {
            builder.item(i, new PanelItemBuilder().icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name(" ").build());
        }

        // Setup the views
        int startSlot = 10;
        int i = 0;
        List<? extends Addon> addons;
        switch (view) {
            case GAMEMODES:
                addons = plugin.getAddonsManager().getGameModeAddons();
                if (addons.isEmpty()) {
                    looksEmpty(builder, user);
                    break;
                }
                for (Addon addon : addons) {
                    PanelItem addonItem = new PanelItemBuilder()
                            .icon(addon.getDescription().getIcon())
                            .name(addon.getDescription().getName())
                            .build();

                    builder.item(startSlot + i, addonItem);

                    PanelItem schems = new PanelItemBuilder()
                            .icon(Material.STRUCTURE_BLOCK)
                            .name(user.getTranslation(LOCALE_REF + "views.gamemodes.schems.name"))
                            .description(user.getTranslation(LOCALE_REF + "views.gamemodes.schems.description"))
                            .clickHandler((panel, user1, clickType, slot) -> {
                                user1.sendRawMessage("opening the admin schems menu (not implemented yet)");
                                return true;
                            })
                            .build();

                    builder.item(startSlot + i + 9, schems);
                    i++;
                }
                break;
            case ADDONS:
                addons = plugin.getAddonsManager().getEnabledAddons().stream().filter(addon -> !(addon instanceof GameModeAddon)).collect(Collectors.toList());
                if (addons.isEmpty()) {
                    looksEmpty(builder, user);
                    break;
                }
                for (Addon addon : addons) {
                    PanelItem addonItem = new PanelItemBuilder()
                            .icon(addon.getDescription().getIcon())
                            .name(addon.getDescription().getName())
                            .build();

                    builder.item(startSlot + i, addonItem);
                    i++;
                }
                break;
            case HOOKS:
                looksEmpty(builder, user);

                break;
        }

        // Setup a few more buttons
        // Catalog
        PanelItem catalog = new PanelItemBuilder()
                .icon(Material.ENCHANTED_BOOK)
                .name(user.getTranslation(LOCALE_REF + "buttons.catalog.name"))
                .description(user.getTranslation(LOCALE_REF + "buttons.catalog.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    user1.sendRawMessage("opening the catalog... (not implemented yet)");
                    return true;
                })
                .build();

        builder.item(17, catalog);

        // Show it to the user
        builder.build().open(user);
    }

    private static void setupHeader(PanelBuilder builder, User user, View view) {
        // Navigation buttons
        PanelItemBuilder gamemodesIconBuilder = new PanelItemBuilder()
                .icon(Material.COMMAND_BLOCK)
                .name(user.getTranslation(LOCALE_REF + "views.gamemodes.name"))
                .description(user.getTranslation(LOCALE_REF + "views.gamemodes.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    openPanel(user, View.GAMEMODES);
                    return true;
                });

        PanelItemBuilder addonsIconBuilder = new PanelItemBuilder()
                .icon(Material.BOOK)
                .name(user.getTranslation(LOCALE_REF + "views.addons.name"))
                .description(user.getTranslation(LOCALE_REF + "views.addons.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    openPanel(user, View.ADDONS);
                    return true;
                });

        PanelItemBuilder hooksIconBuilder = new PanelItemBuilder()
                .icon(Material.TRIPWIRE_HOOK)
                .name(user.getTranslation(LOCALE_REF + "views.hooks.name"))
                .description(user.getTranslation(LOCALE_REF + "views.hooks.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    openPanel(user, View.HOOKS);
                    return true;
                });

        switch (view) {
            case GAMEMODES:
                gamemodesIconBuilder.glow(true);
                break;
            case ADDONS:
                addonsIconBuilder.glow(true);
                break;
            case HOOKS:
                hooksIconBuilder.glow(true);
                break;
        }

        builder.item(1, gamemodesIconBuilder.build());
        builder.item(2, addonsIconBuilder.build());
        builder.item(3, hooksIconBuilder.build());

        // Global action buttons
        PanelItem checkUpdatesItem = new PanelItemBuilder()
                .icon(Material.OBSERVER)
                .name(user.getTranslation(LOCALE_REF + "actions.check-updates.name"))
                .description(user.getTranslation(LOCALE_REF + "actions.check-updates.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    user1.sendRawMessage("checking for updates (not implemented yet)");
                    return true;
                })
                .build();

        PanelItem reloadItem = new PanelItemBuilder()
                .icon(Material.REDSTONE_TORCH)
                .name(user.getTranslation(LOCALE_REF + "actions.reload.name"))
                .description(user.getTranslation(LOCALE_REF + "actions.reload.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    user1.performCommand("bentobox reload");
                    return true;
                })
                .build();

        builder.item(5, checkUpdatesItem);
        builder.item(6, reloadItem);
    }

    private static void looksEmpty(PanelBuilder builder, User user) {
        PanelItem emptyHere = new PanelItemBuilder()
                .icon(Material.STRUCTURE_VOID)
                .name(user.getTranslation(LOCALE_REF + "buttons.empty-here.name"))
                .description(user.getTranslation(LOCALE_REF + "buttons.empty-here.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    user1.sendRawMessage("opening the catalog... (not implemented yet)");
                    return true;
                })
                .build();

        builder.item(22, emptyHere);
    }

    public enum View {
        GAMEMODES,
        ADDONS,
        HOOKS
    }
}
