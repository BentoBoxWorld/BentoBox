/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

/**
 * @author ben
 *
 */
public class IslandSetnameCommand extends CompositeCommand {

    public IslandSetnameCommand(CompositeCommand command) {
        super(command, "resetname");
        this.setPermission(Settings.PERMPREFIX + "island.name");
        this.setOnlyPlayer(true);

    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, String[] args) {
        Player player = user.getPlayer();
        UUID playerUUID = player.getUniqueId();

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

    @Override
    public void setup() {
        // TODO Auto-generated method stub
        
    }

}
