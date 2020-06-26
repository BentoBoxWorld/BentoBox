package world.bentobox.bentobox.panels;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.web.catalog.CatalogEntry;

/**
 * @since 1.5.0
 * @author Poslovitch
 */
public class CatalogPanel {

    private static final String LOCALE_REF = "catalog.panel.";
    private static final int[] PANES = {0, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};

    private CatalogPanel() {}

    public static void openPanel(@NonNull User user, @NonNull View view) {
        BentoBox plugin = BentoBox.getInstance();

        PanelBuilder builder = new PanelBuilder()
                .name(user.getTranslation(LOCALE_REF + view.name() + ".title"))
                .size(45);

        // Setup header and corners
        for (int i : PANES) {
            builder.item(i, new PanelItemBuilder().icon(plugin.getSettings().getPanelFillerMaterial()).name(" ").build());
        }

        PanelItemBuilder gamemodesButton = new PanelItemBuilder()
                .icon(Material.COMMAND_BLOCK)
                .name(user.getTranslation(LOCALE_REF + "views.gamemodes.name"))
                .description(user.getTranslation(LOCALE_REF + "views.gamemodes.description"));

        PanelItemBuilder addonsButton = new PanelItemBuilder()
                .icon(Material.BOOK)
                .name(user.getTranslation(LOCALE_REF + "views.addons.name"))
                .description(user.getTranslation(LOCALE_REF + "views.addons.description"));

        List<CatalogEntry> catalog;
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
            for (CatalogEntry addon : catalog) {
                PanelItemBuilder itemBuilder = new PanelItemBuilder();

                String name = ChatColor.WHITE + addon.getName();
                if (addon.getTag() != null) {
                    name += " " + ChatColor.AQUA + "" + ChatColor.BOLD + user.getTranslation("catalog.tags." + addon.getTag());
                }

                itemBuilder.icon(addon.getIcon()).name(name);

                // If the addon is already installed, then tell the user it's already installed
                String install;
                if (plugin.getAddonsManager().getAddonByName(addon.getName()).isPresent()) {
                    itemBuilder.glow(true);
                    install = user.getTranslation(LOCALE_REF + "icon.already-installed");
                } else {
                    install = user.getTranslation(LOCALE_REF + "icon.install-now");
                }

                itemBuilder.description(user.getTranslation(LOCALE_REF + "icon.description-template",
                        "[topic]", StringUtils.capitalize(user.getTranslation("catalog.topics." + addon.getTopic())),
                        "[install]", install,
                        "[description]", addon.getDescription()));

                // Send the link to the releases tab on click
                itemBuilder.clickHandler((panel, user1, clickType, slot) -> {
                    user1.sendRawMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "https://github.com/" + addon.getRepository() + "/releases");
                    return true;
                });

                builder.item(addon.getSlot(), itemBuilder.build());
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
