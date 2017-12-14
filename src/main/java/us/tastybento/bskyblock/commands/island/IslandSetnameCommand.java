/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.util.VaultHelper;

/**
 * @author ben
 *
 */
public class IslandSetnameCommand extends CommandArgument {

    /**
     * @param label
     * @param aliases
     */
    public IslandSetnameCommand() {
        super("resetname");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sender.sendMessage(getLocale(sender).get("general.errors.use-in-game"));
            return true;
        }
        Player player = (Player)sender;
        UUID playerUUID = player.getUniqueId();

        if (!VaultHelper.hasPerm(player, Settings.PERMPREFIX + "island.name")) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
            return true;
        }

        if (!getIslands().hasIsland(playerUUID)) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-island"));
            return true;
        }

        if (!getIslands().isOwner(playerUUID)) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.not-leader"));
            return true;
        }
        // Resets the island name
        getIslands().getIsland(playerUUID).setName(null);

        sender.sendMessage(getLocale(sender).get("general.success"));
        return true;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#tabComplete(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public Set<String> tabComplete(CommandSender sender, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }

}
