package us.tastybento.bskyblock.config;

import java.util.UUID;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Handles the BSkyBlock locale
 * @author ben
 *
 */
public class LocaleManager extends AbstractLocaleManager {
    
    private BSkyBlock plugin;

    public LocaleManager(BSkyBlock plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    /**
     * Returns the locale for the specified player
     * @param player - Player to get the locale
     * @return the locale for this player
     */
    public BSBLocale getLocale(UUID player){
        //getLogger().info("DEBUG: " + player);
        //getLogger().info("DEBUG: " + getPlayers() == null ? "Players is null":"Players in not null");
        //getLogger().info("DEBUG: " + getPlayers().getPlayer(player));
        //getLogger().info("DEBUG: " + getPlayers().getPlayer(player).getLocale());
        String locale = plugin.getPlayers().getPlayer(player).getLocale();
        if(locale.isEmpty() || !getLocales().containsKey(locale)) return getLocales().get(Settings.defaultLanguage);

        return getLocales().get(locale);
    }
}
