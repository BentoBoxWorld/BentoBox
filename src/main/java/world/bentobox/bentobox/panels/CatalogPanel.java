package world.bentobox.bentobox.panels;

import com.google.gson.JsonObject;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;

import java.util.List;

/**
 * @since 1.5.0
 * @author Poslovitch
 */
public class CatalogPanel {

    private static final String LOCALE_REF = "catalog.panel.";
    private static final int[] PANES = {0, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,};

    private CatalogPanel() {}

    public static void openPanel(@NonNull User user, @NonNull View view) {
        BentoBox plugin = BentoBox.getInstance();

        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + view.name() + ".title"))
                .size(45);

        // Setup header and corners
        for (int i : PANES) {
            builder.item(i, new PanelItemBuilder().icon(Material.LIGHT_BLUE_STAINED_GLASS_PANE).name(" ").build());
        }

        PanelItemBuilder gamemodesButton = new PanelItemBuilder()
                .icon(Material.COMMAND_BLOCK)
                .name(user.getTranslation(LOCALE_REF + "views.gamemodes.name"))
                .description(user.getTranslation(LOCALE_REF + "views.gamemodes.description"));

        PanelItemBuilder addonsButton = new PanelItemBuilder()
                .icon(Material.BOOK)
                .name(user.getTranslation(LOCALE_REF + "views.addons.name"))
                .description(user.getTranslation(LOCALE_REF + "views.addons.description"));

        List<JsonObject> catalog;
        if (view == View.GAMEMODES) {
            catalog = plugin.getWebManager().getGamemodesCatalog();
            // Make the gamemodes button glow
            gamemodesButton.glow(true);
            // Make the addons button move to the addons view
            addonsButton.clickHandler((panel, user1, clickType, slot) -> {
                openPanel(user, View.ADDONS);
                return true;
            });
        } else {
            catalog = plugin.getWebManager().getAddonsCatalog();
            // Make the addons button glow
            addonsButton.glow(true);
            // Make the gamemodes button move to the gamemodes view
            gamemodesButton.clickHandler((panel, user1, clickType, slot) -> {
                openPanel(user, View.GAMEMODES);
                return true;
            });
        }

        builder.item(1, gamemodesButton.build());
        builder.item(2, addonsButton.build());

        // Populate with the addons from the catalog we're actually viewing.
        if (catalog.isEmpty()) {
            looksEmpty(builder, user);
        } else {
            for (JsonObject addon : catalog) {
                PanelItemBuilder itemBuilder = new PanelItemBuilder();

                Material icon = Material.getMaterial(addon.get("icon").getAsString());
                if (icon == null) {
                    icon = Material.PAPER;
                }

                String name = addon.get("name").getAsString();
                itemBuilder.icon(icon)
                        .name(ChatColor.WHITE + name);

                // If the addon is already installed, then tell the user it's already installed
                plugin.getAddonsManager().getAddonByName(name).ifPresent(addon1 -> itemBuilder.glow(true).description(user.getTranslation(LOCALE_REF + "already-installed")));

                builder.item(addon.get("slot").getAsInt(), itemBuilder.build());
            }
        }

        builder.build().open(user);
    }

    private static void looksEmpty(@NonNull PanelBuilder builder, @NonNull User user) {
        PanelItem emptyHere = new PanelItemBuilder()
                .icon(Material.STRUCTURE_VOID)
                .name(user.getTranslation(LOCALE_REF + "empty-here.name"))
                .description(user.getTranslation(LOCALE_REF + "empty-here.description"))
                .build();

        builder.item(22, emptyHere);
    }

    public enum View {
        GAMEMODES,
        ADDONS
    }
}
