package world.bentobox.bentobox.panels;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.versions.ServerCompatibility;
import world.bentobox.bentobox.versions.ServerCompatibility.ServerSoftware;

/**
 * @author Poslovitch
 * @since 1.5.0
 */
public class ManagementPanel {

    private static final String LOCALE_REF = "management.panel.";
    private static final int[] PANES = {0, 4, 5, 8, 9, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};

    private ManagementPanel() {}

    /**
     * Dynamically creates the panel.
     * @param user the User to show the panel to
     */
    public static void openPanel(@NonNull User user, View view) {
        BentoBox plugin = BentoBox.getInstance();

        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + "title"))
                .size(45);

        // Setup header and corner
        setupHeader(builder, user, view);
        for (int i : PANES) {
            builder.item(i, new PanelItemBuilder().icon(plugin.getSettings().getPanelFillerMaterial()).name(" ").build());
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
                GameModeAddon gameModeAddon = (GameModeAddon) addon;
                PanelItem addonItem = new PanelItemBuilder()
                        .icon(addon.getDescription().getIcon())
                        .name(user.getTranslation(LOCALE_REF + "views.gamemodes.gamemode.name", TextVariables.NAME, addon.getDescription().getName()))
                        .description(user.getTranslation(LOCALE_REF + "views.gamemodes.gamemode.description",
                                "[islands]", String.valueOf(addon.getIslands().getIslandCount(gameModeAddon.getOverWorld()))))
                        .clickHandler((panel, user1, clickType, slot) -> {
                            if (clickType.equals(ClickType.MIDDLE)) {
                                CreditsPanel.openPanel(user, addon);
                            }
                            return true;
                        })
                        .build();

                builder.item(startSlot + i, addonItem);

                PanelItem blueprints = new PanelItemBuilder()
                        .icon(Material.STRUCTURE_BLOCK)
                        .name(user.getTranslation(LOCALE_REF + "views.gamemodes.blueprints.name"))
                        .description(user.getTranslation(LOCALE_REF + "views.gamemodes.blueprints.description"))
                        .clickHandler((panel, user1, clickType, slot) -> {
                            new BlueprintManagementPanel(plugin, user, gameModeAddon).openPanel();
                            return true;
                        })
                        .build();

                builder.item(startSlot + i + 9, blueprints);
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
                        .name(ChatColor.WHITE + addon.getDescription().getName())
                        .clickHandler((panel, user1, clickType, slot) -> {
                            if (clickType.equals(ClickType.MIDDLE)) {
                                CreditsPanel.openPanel(user, addon);
                            }
                            return true;
                        })
                        .build();

                builder.item(startSlot + i, addonItem);
                i++;
                if (builder.slotOccupied(startSlot + i)) {
                    i = i+2;
                }
            }
            break;
        case HOOKS:
            if (plugin.getHooks().getHooks().isEmpty()) {
                looksEmpty(builder, user);
                break;
            }
            for (Hook hook : plugin.getHooks().getHooks()) {
                PanelItem hookItem = new PanelItemBuilder()
                        .icon(hook.getIcon())
                        .name(ChatColor.WHITE + hook.getPluginName())
                        .build();

                builder.item(startSlot + i, hookItem);
                i++;
                if (builder.slotOccupied(startSlot + i)) {
                    i = i+2;
                }
            }
            break;
        }

        // Setup a few more buttons
        // Catalog
        PanelItem catalog = new PanelItemBuilder()
                .icon(Material.ENCHANTED_BOOK)
                .name(user.getTranslation(LOCALE_REF + "buttons.catalog.name"))
                .description(user.getTranslation(LOCALE_REF + "buttons.catalog.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    CatalogPanel.openPanel(user, CatalogPanel.View.GAMEMODES);
                    return true;
                })
                .build();

        builder.item(17, catalog);

        // Credits
        PanelItem credits = new PanelItemBuilder()
                .icon(Material.KNOWLEDGE_BOOK)
                .name(user.getTranslation(LOCALE_REF + "buttons.credits.name"))
                .description(user.getTranslation(LOCALE_REF + "buttons.credits.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    CreditsPanel.openPanel(user, "BentoBoxWorld/BentoBox");
                    return true;
                })
                .build();

        builder.item(26, credits);

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

        PanelItem reloadItem = new PanelItemBuilder()
                .icon(Material.REDSTONE_TORCH)
                .name(user.getTranslation(LOCALE_REF + "actions.reload.name"))
                .description(user.getTranslation(LOCALE_REF + "actions.reload.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    user1.performCommand("bentobox reload");
                    return true;
                })
                .build();

        builder.item(6, reloadItem);

        // BentoBox state icon
        ServerCompatibility.Compatibility compatibility = ServerCompatibility.getInstance().checkCompatibility();
        ServerCompatibility.ServerSoftware serverSoftware = ServerCompatibility.getInstance().getServerSoftware();
        ServerCompatibility.ServerVersion serverVersion = ServerCompatibility.getInstance().getServerVersion();

        PanelItemBuilder compatibilityItemBuilder = new PanelItemBuilder()
                .name(user.getTranslation(LOCALE_REF + "information.state.name"))
                .description(user.getTranslation(LOCALE_REF + "information.state.description." + compatibility,
                        TextVariables.NAME, serverSoftware.equals(ServerSoftware.UNKNOWN) ? serverSoftware.getName() : serverSoftware.toString(),
                                TextVariables.VERSION, serverVersion != null ? serverVersion.toString() : user.getTranslation("general.invalid")));

        switch (compatibility) {
        case COMPATIBLE:
        case SUPPORTED:
            compatibilityItemBuilder.icon(Material.GREEN_CONCRETE);
            break;
        case NOT_SUPPORTED:
            compatibilityItemBuilder.icon(Material.ORANGE_CONCRETE);
            break;
        case INCOMPATIBLE:
            compatibilityItemBuilder.icon(Material.RED_CONCRETE);
            break;
        }

        builder.item(7, compatibilityItemBuilder.build());
    }

    private static void looksEmpty(@NonNull PanelBuilder builder, @NonNull User user) {
        PanelItem emptyHere = new PanelItemBuilder()
                .icon(Material.STRUCTURE_VOID)
                .name(user.getTranslation(LOCALE_REF + "buttons.empty-here.name"))
                .description(user.getTranslation(LOCALE_REF + "buttons.empty-here.description"))
                .clickHandler((panel, user1, clickType, slot) -> {
                    CatalogPanel.openPanel(user, CatalogPanel.View.GAMEMODES);
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
