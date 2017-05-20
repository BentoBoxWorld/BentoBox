package us.tastybento.askyblock.util.nms;

import org.bukkit.entity.Player;

public interface NMSAbstraction {

    public void sendActionBar(Player player, String message);
    public void sendTitle(Player player, String message);
    public void sendSubtitle(Player player, String message);
}
