package us.tastybento.bskyblock.panels;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.panels.builders.PanelBuilder;
import us.tastybento.bskyblock.api.panels.builders.PanelItemBuilder;
import us.tastybento.bskyblock.api.user.User;

import java.util.Locale;

/**
 * @author Poslovitch
 */
public class LanguagePanel {

    /**
     * Dynamically creates the panel.
     * @param user the User to show the panel to
     */
    public static void openPanel(User user) {
        PanelBuilder panelBuilder = new PanelBuilder()
                .name(user.getTranslation("languages.panel.title"));

        for (Locale locale : BSkyBlock.getInstance().getLocalesManager().getAvailableLocales()) {
            PanelItemBuilder localeIcon = new PanelItemBuilder().icon(BSkyBlock.getInstance().getLocalesManager().getLanguages().get(locale).getBanner())
                    .name("languages." + locale.toLanguageTag() + ".name")
                    .clickHandler((u, click) -> {
                        BSkyBlock.getInstance().getPlayers().setLocale(u.getUniqueId(), locale.toLanguageTag());
                        u.sendMessage("language.changed");
                        u.closeInventory();
                        return true;
                    });

            if (user.getLocale().toLanguageTag().equals(locale.toLanguageTag())) {
                localeIcon.description("language.already-selected");
            }

            panelBuilder.item(localeIcon.build());
        }

        panelBuilder.build().open(user);
    }
}
