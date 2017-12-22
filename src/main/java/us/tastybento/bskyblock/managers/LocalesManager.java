package us.tastybento.bskyblock.managers;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.BSModule;
import us.tastybento.bskyblock.api.localization.BSLocale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Poslovitch
 */
public final class LocalesManager {

    private BSkyBlock plugin;
    private Map<BSModule, List<BSLocale>> locales;

    public LocalesManager(BSkyBlock plugin) {
        this.plugin = plugin;
        this.locales = new HashMap<>();
    }

    public String get(CommandSender sender, String reference) {
        if (sender instanceof Player) {
            return get(((Player)sender).getUniqueId(), reference);
        }
        return reference; //TODO
    }

    public String get(UUID uuid, String reference) {
        return reference; //TODO
    }

    public Map<BSModule, List<BSLocale>> getLocales() {
        return locales;
    }

    public List<BSLocale> getLocales(BSModule module) {
        return locales.get(module);
    }

    public BSLocale getLocale(BSModule module, String languageTag) {
        for (BSLocale locale : locales.get(module)) {
            if (locale.toLanguageTag().equals(languageTag)) return locale;
        }
        return null;
    }

    public void registerLocales(BSModule module) {
        //TODO
    }

    public void registerLocale(BSModule module, String languageTag) {
        //TODO
    }

    public void registerExternalLocale(BSModule originModule, BSModule targetModule, String languageTag) {
        //TODO
    }
}
