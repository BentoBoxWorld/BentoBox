package us.tastybento.bskyblock.util.nms.fallback;

import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.util.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    @Override
    public void sendActionBar(Player player, String message) {
        //TODO use /title command
    }

    @Override
    public void sendTitle(Player player, String message) {
        //TODO use /title command
    }

    @Override
    public void sendSubtitle(Player player, String message) {
        //TODO use /title command
    }

    @Override
    public CommandMap getServerCommandMap() {
        return null;
    }
}
