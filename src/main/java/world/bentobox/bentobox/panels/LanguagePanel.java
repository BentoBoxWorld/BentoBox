package world.bentobox.bentobox.panels;

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

import java.util.Locale;

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

            localeIcon.name(ChatColor.WHITE + fancyLocaleDisplayName(user, locale))
                    .clickHandler((panel, u, click, slot) -> {
                        BentoBox.getInstance().getPlayers().setLocale(u.getUniqueId(), locale.toLanguageTag());
                        u.sendMessage("language.edited", "[lang]", fancyLocaleDisplayName(u, locale));
                        openPanel(u);
                        return true;
                    });

            if (user.getLocale().toLanguageTag().equals(locale.toLanguageTag())) {
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

    /**
     * Returns a properly capitalized String based on locale's display name from user's current locale.
     * @param user - the User
     * @param locale - the Locale to get the display name from
     * @return properly capitalized String of the locale's display name in user's current locale
     */
    private static String fancyLocaleDisplayName(User user, Locale locale) {
        // Get the display name of the locale based on current user's locale
        String localeDisplayName = locale.getDisplayName(user.getLocale());

        // Set the first letter to an uppercase, to make it nice and fancy :D
        localeDisplayName = localeDisplayName.substring(0,1).toUpperCase() + localeDisplayName.substring(1);

        return localeDisplayName;
    }
}
