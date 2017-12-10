package us.tastybento.bskyblock.util.nms;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

import java.util.Map;

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

    /**
     * Returns the active {@link org.bukkit.command.CommandMap} of the Server.
     * It is used by the {@link us.tastybento.bskyblock.api.commands.CompositeCommand} to register itself.
     * @return the active CommandMap of the server
     */
    CommandMap getServerCommandMap();
}
