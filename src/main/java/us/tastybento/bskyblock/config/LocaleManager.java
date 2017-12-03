package us.tastybento.bskyblock.config;

import org.bukkit.command.CommandSender;
import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.localization.LocaleHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LocaleManager {

    public static final String LOCALE_FOLDER = "localization";

    private BSkyBlock plugin;
    private Map<String, LocaleHandler> handlers = new HashMap<>();

    public LocaleManager(BSkyBlock plugin) {
        this.plugin = plugin;
    }

    public void registerLocaleHandler(LocaleHandler handler) {
        handlers.put(handler.getIdentifier(), handler);
        handler.setupLocales();
        handler.loadLocales();
    }

    public String get(CommandSender sender, String reference) {
        return reference;
    }

    public String get(UUID uuid, String reference) {
        return reference;
    }
}
