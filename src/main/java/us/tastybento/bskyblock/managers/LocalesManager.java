package us.tastybento.bskyblock.managers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.BSBModule;
import us.tastybento.bskyblock.api.localization.BSBLocale;
import us.tastybento.bskyblock.config.Settings;

/**
 * @author Poslovitch
 */
public final class LocalesManager {

    private BSkyBlock plugin;
    private Map<BSBModule, List<BSBLocale>> locales;

    public LocalesManager(BSkyBlock plugin) {
        this.plugin = plugin;
        this.locales = new HashMap<>();
    }

    /**
     *
     * @param sender
     * @param reference
     * @return the translation found for the provided reference, or the reference if nothing has been found
     */
    public String get(CommandSender sender, String reference) {
        if (sender instanceof Player) {
            return get(((Player)sender).getUniqueId(), reference);
        }

        if (reference.contains(":")) { // if reference addresses directly the module like "bskyblock:general.errors.use-in-game"
            String[] path = reference.split(":", 1);
            for (BSBModule module : locales.keySet()) {
                if (module.getIdentifier().toLowerCase().equals(path[0].toLowerCase())) {
                    // CommandSender doesnt have any data stored, so we have to get the default language
                    BSBLocale locale = getLocale(module, Settings.defaultLanguage);
                    String translation = path[1];

                    if (locale != null) translation = locale.get(path[1]);

                    if (!Settings.defaultLanguage.equals("en-US") && translation.equals(path[1])) {
                        // If the default language is not en-US and no translation has been found (aka reference has been returned)
                        // then check in the en-US locale, which should always exists
                        locale = getLocale(module, "en-US");
                        if (locale != null) translation = locale.get(path[1]);
                    }
                    return translation; // translation can be found, or can be the reference
                }
            }
        } else {
            // Run through each module's locales to try find the reference
            for (BSBModule module : locales.keySet()) {
                // CommandSender doesnt have any data stored, so we have to get the default language
                BSBLocale locale = getLocale(module, Settings.defaultLanguage);
                String translation = reference;

                if (locale != null) translation = locale.get(reference);

                if (!Settings.defaultLanguage.equals("en-US") && translation.equals(reference)) {
                    // If the default language is not en-US and no translation has been found (aka reference has been returned)
                    // then check in the en-US locale, which should always exists
                    locale = getLocale(module, "en-US");
                    if (locale != null) translation = locale.get(reference);
                }

                if (!translation.equals(reference)) return translation; // if a translation has been found, return it. Otherwise continue.
            }
        }
        return reference; // Return reference to tell the user that no translation has been found
    }

    /**
     *
     * @param uuid
     * @param reference
     * @return the translation found for the provided reference, or the reference if nothing has been found
     */
    public String get(UUID uuid, String reference) {
        if (reference.contains(":")) { // if reference addresses directly the module like "bskyblock:general.errors.use-in-game"
            String[] path = reference.split(":", 1);
            for (BSBModule module : locales.keySet()) {
                if (module.getIdentifier().toLowerCase().equals(path[0].toLowerCase())) {
                    // Firstly try to find the translation in player's locale
                    BSBLocale locale = getLocale(module, plugin.getPlayers().getLocale(uuid));
                    String translation = path[1];

                    if (locale != null) translation = locale.get(path[1]);

                    if (!Settings.defaultLanguage.equals(plugin.getPlayers().getLocale(uuid)) && translation.equals(path[1])) {
                        // If the default language is not the same than the player's one and no translation has been found (aka reference has been returned)
                        // then check in the default language locale, which should always exist
                        locale = getLocale(module, Settings.defaultLanguage);
                        if (locale != null) translation = locale.get(path[1]);
                    }

                    if (!plugin.getPlayers().getLocale(uuid).equals("en-US") && !Settings.defaultLanguage.equals("en-US") && translation.equals(path[1])) {
                        // If the player's locale is not en-US and the default language is not en-US and no translation has been found (aka reference has been returned)
                        // then check in the en-US locale, which should always exists
                        locale = getLocale(module, "en-US");
                        if (locale != null) translation = locale.get(path[1]);
                    }
                    return translation; // translation can be found, or can be the reference
                }
            }
        } else {
            // Run through each module's locales to try find the reference
            for (BSBModule module : locales.keySet()) {
                // Firstly try to find the translation in player's locale
                BSBLocale locale = getLocale(module, plugin.getPlayers().getLocale(uuid));
                String translation = reference;

                if (locale != null) translation = locale.get(reference);

                if (!Settings.defaultLanguage.equals(plugin.getPlayers().getLocale(uuid)) && translation.equals(reference)) {
                    // If the default language is not the same than the player's one and no translation has been found (aka reference has been returned)
                    // then check in the default language locale, which should always exist
                    locale = getLocale(module, Settings.defaultLanguage);
                    if (locale != null) translation = locale.get(reference);
                }

                if (!plugin.getPlayers().getLocale(uuid).equals("en-US") && !Settings.defaultLanguage.equals("en-US") && translation.equals(reference)) {
                    // If the player's locale is not en-US and the default language is not en-US and no translation has been found (aka reference has been returned)
                    // then check in the en-US locale, which should always exists
                    locale = getLocale(module, "en-US");
                    if (locale != null) translation = locale.get(reference);
                }

                if (!translation.equals(reference)) return translation; // if a translation has been found, return it. Otherwise continue.
            }
        }
        return reference; // Return reference to tell the user that no translation has been found
    }

    public Map<BSBModule, List<BSBLocale>> getLocales() {
        return locales;
    }

    public List<BSBLocale> getLocales(BSBModule module) {
        return locales.get(module);
    }

    public BSBLocale getLocale(BSBModule module, String languageTag) {
        for (BSBLocale locale : locales.get(module)) {
            if (locale.toLanguageTag().equals(languageTag)) return locale;
        }
        return null;
    }

    public void registerLocales(BSBModule module) {
        // Check if the folder exists and contains the locales

        // for each languageTag found, do registerLocale(module, languageTag)
        //TODO
    }

    public void registerLocale(BSBModule module, String languageTag) {
        //TODO
    }

    public void registerExternalLocale(BSBModule originModule, BSBModule targetModule, String languageTag) {
        //TODO
    }
}
