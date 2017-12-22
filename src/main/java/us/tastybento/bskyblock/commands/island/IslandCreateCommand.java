/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.managers.island.NewIsland;

/**
 * @author ben
 *
 */
public class IslandCreateCommand extends CompositeCommand {

    public IslandCreateCommand(IslandCommand islandCommand) {
        super(islandCommand, "create", "auto");
        this.setPermission(Settings.PERMPREFIX + "island.create");
        this.setOnlyPlayer(true);
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, String[] args) {
        if (getIslands().hasIsland(user.getUniqueId())) {
            user.sendMessage(ChatColor.RED + "general.errors.already-have-island");
        }
        if (getPlayers().inTeam(user.getUniqueId())) {
            return false; 
        }
        createIsland(user.getPlayer());
        return true;
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

    @Override
    public void setup() {
        // TODO Auto-generated method stub
        
    }

}
