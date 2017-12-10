package us.tastybento.bskyblock.util.nms;

import org.bukkit.entity.Player;

public interface NMSAbstraction {

    /**
     * Send an action bar message to player
     * @param player
     * @param message
     */
    void sendActionBar(Player player, String message);


    /**
     * Send a title to a player
     * @param player
     * @param message
     */
    void sendTitle(Player player, String message);


    /**
     * Send a subtitle to a player
     * @param player
     * @param message
     */
    void sendSubtitle(Player player, String message);
}
