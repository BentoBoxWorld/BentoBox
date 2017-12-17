/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.Set;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

/**
 * @author ben
 *
 */
public class IslandSetnameCommand extends CommandArgument {

    public IslandSetnameCommand() {
        super("resetname");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, String[] args) {
        if (!isPlayer(user)) {
            user.sendMessage("general.errors.use-in-game");
            return true;
        }
        Player player = (Player)user;
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPermission(Settings.PERMPREFIX + "island.name")) {
            user.sendMessage(ChatColor.RED + "general.errors.no-permission");
            return true;
        }

        if (!getIslands().hasIsland(playerUUID)) {
            user.sendMessage(ChatColor.RED + "general.errors.no-island");
            return true;
        }

        if (!getIslands().isOwner(playerUUID)) {
            user.sendMessage(ChatColor.RED + "general.errors.not-leader");
            return true;
        }
        // Resets the island name
        getIslands().getIsland(playerUUID).setName(null);

        user.sendMessage("general.success");
        return true;
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#tabComplete(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public Set<String> tabComplete(User user, String[] args) {
        // TODO Auto-generated method stub
        return null;
    }

}
