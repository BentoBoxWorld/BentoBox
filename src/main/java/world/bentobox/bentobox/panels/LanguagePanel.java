package world.bentobox.bentobox.panels;

import java.util.Locale;

import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.BentoBoxLocale;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.builders.PanelBuilder;
import world.bentobox.bentobox.api.panels.builders.PanelItemBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.LocalesManager;

/**
 * @author Poslovitch
 */
public class LanguagePanel {

    private LanguagePanel() {}

    /**
     * Dynamically creates the panel.
     * @param user the User to show the panel to
     */
    public static void openPanel(User user) {
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation("language.panel-title"));

        LocalesManager localesManager = BentoBox.getInstance().getLocalesManager();

        for (Locale locale : localesManager.getAvailableLocales(true)) {
            PanelItemBuilder localeIcon = new PanelItemBuilder();

            BentoBoxLocale language = localesManager.getLanguages().get(locale);

            ItemStack localeBanner = language.getBanner();
            if (localeBanner != null) {
                localeIcon.icon(localeBanner);
            } else {
                localeIcon.icon(new ItemStack(Material.WHITE_BANNER, 1)); // Set to a blank banner.
            }
            localeIcon.name(ChatColor.WHITE + WordUtils.capitalize(locale.getDisplayName(user.getLocale())))
            .clickHandler((panel, u, click, slot) -> {
                BentoBox.getInstance().getPlayers().setLocale(u.getUniqueId(), locale.toLanguageTag());
                u.sendMessage("language.edited", "[lang]", WordUtils.capitalize(locale.getDisplayName(user.getLocale())));
                openPanel(u);
                return true;
            });
            if (user.getLocale().equals(locale)) {
                localeIcon.description(user.getTranslation("language.description.selected"), "");
            } else {
                localeIcon.description(user.getTranslation("language.description.click-to-select"), "");
            }

            localeIcon.description(user.getTranslation("language.description.authors"));
            for (String author : language.getAuthors()) {
                localeIcon.description(user.getTranslation("language.description.author", TextVariables.NAME, author));
            }

            panelBuilder.item(localeIcon.build());
        }

        panelBuilder.build().open(user);
    }

}
