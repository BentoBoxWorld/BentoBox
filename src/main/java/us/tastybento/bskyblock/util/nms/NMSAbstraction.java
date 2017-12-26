package us.tastybento.bskyblock.util.nms;

import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

public interface NMSAbstraction {

    /**
     * Send an action bar message to player
     * @param player
     * @param message
     */
    void sendActionBar(Player player, String message);

    /**
     * Returns the active {@link org.bukkit.command.CommandMap} of the Server.
     * It is used by the {@link us.tastybento.bskyblock.api.commands.CompositeCommand} to register itself.
     * @return the active CommandMap of the server
     */
    CommandMap getServerCommandMap();
}
