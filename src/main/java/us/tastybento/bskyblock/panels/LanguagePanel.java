package us.tastybento.bskyblock.panels;

import java.util.Locale;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;

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

        for (Locale locale : BSkyBlock.getInstance().getLocalesManager().getAvailableLocales()) {
            PanelItemBuilder localeIcon = new PanelItemBuilder().icon(BSkyBlock.getInstance().getLocalesManager().getLanguages().get(locale).getBanner())
                    .name(fancyLocaleDisplayName(user, locale))
                    .clickHandler((panel, u, click, slot) -> {
                        BSkyBlock.getInstance().getPlayers().setLocale(u.getUniqueId(), locale.toLanguageTag());
                        u.sendMessage("language.edited", "[lang]", fancyLocaleDisplayName(u, locale));
                        openPanel(u);
                        return true;
                    });

            if (user.getLocale().toLanguageTag().equals(locale.toLanguageTag())) {
                localeIcon.description(user.getTranslation("language.selected"));
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
