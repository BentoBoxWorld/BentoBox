/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.io.IOException;

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
            user.sendMessage("general.errors.already-have-island");
        }
        if (getPlayers().inTeam(user.getUniqueId())) {
            return false; 
        }
        user.sendLegacyMessage("Creating island...");
        createIsland(user);
        return true;
    }

    /**
     * Creates an island for player
     *
     * @param user
     */
    protected void createIsland(User user) {
        //TODO: Add panels, make a selection.
        try {
            NewIsland.builder()
            .player(user.getPlayer())
            .reason(Reason.CREATE)
            .build();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not create island for player.");
            user.sendMessage("general.errors.general");
            e.printStackTrace();
        }
    }

    @Override
    public void setup() {
        // TODO Auto-generated method stub
        
    }

}
