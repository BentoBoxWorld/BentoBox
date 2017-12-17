/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.managers.island.NewIsland;

/**
 * @author ben
 *
 */
public class IslandCreateCommand extends CommandArgument {

    public IslandCreateCommand() {
        super("create", "auto");
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

        if (!player.hasPermission(Settings.PERMPREFIX + "island.create")) {
            user.sendMessage(ChatColor.RED + "general.errors.no-permission");
        }
        if (getIslands().hasIsland(player.getUniqueId())) {
            user.sendMessage(ChatColor.RED + "general.errors.already-have-island");
        }
        if (inTeam(player)) {
            return false; 
        }
        createIsland(player);
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
    
    /**
     * Creates an island for player
     *
     * @param player
     */
    protected void createIsland(Player player) {
        //TODO: Add panels, make a selection.
        try {
            NewIsland.builder()
            .player(player)
            .reason(Reason.CREATE)
            .build();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create island for player.");
            player.sendMessage(ChatColor.RED + plugin.getLocale(player).get("general.errors.general"));
            e.printStackTrace();
        }
    }

}
