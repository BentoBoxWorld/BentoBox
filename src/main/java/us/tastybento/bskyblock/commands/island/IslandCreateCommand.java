/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CommandArgument;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.managers.island.NewIsland;
import us.tastybento.bskyblock.util.VaultHelper;

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
    public boolean execute(CommandSender sender, String[] args) {
        if (!isPlayer(sender)) {
            sender.sendMessage(getLocale(sender).get("general.errors.use-in-game"));
            return true;
        }
        Player player = (Player)sender;

        if (!player.hasPermission(Settings.PERMPREFIX + "island.create")) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.no-permission"));
        }
        if (getIslands().hasIsland(player.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + getLocale(sender).get("general.errors.already-have-island"));
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
    public Set<String> tabComplete(CommandSender sender, String[] args) {
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
