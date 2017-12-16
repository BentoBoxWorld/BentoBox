package us.tastybento.bskyblock.util.nms.v1_12_R1;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.util.nms.NMSAbstraction;

public class NMSHandler implements NMSAbstraction {

    @Override
    public void sendActionBar(Player player, String message) {

    }

    @Override
    public void sendTitle(Player player, String message) {

    }

    @Override
    public void sendSubtitle(Player player, String message) {

    }

    @Override
    public CommandMap getServerCommandMap() {
        return ((CraftServer) Bukkit.getServer()).getCommandMap();
    }
}
