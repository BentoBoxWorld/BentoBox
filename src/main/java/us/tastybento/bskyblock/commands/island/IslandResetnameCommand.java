/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;

/**
 * @author tastybento
 *
 */
public class IslandResetnameCommand extends CompositeCommand {

    public IslandResetnameCommand(CompositeCommand islandCommand) {
        super(islandCommand, "resetname");
    }
    
    @Override
    public void setup() {
        this.setPermission(Constants.PERMPREFIX + "island.name");
        this.setOnlyPlayer(true);
        this.setDescription("commands.island.resetname.description");

    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();

        if (!getIslands().hasIsland(playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        if (!getIslands().isOwner(playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        // Resets the island name
        getIslands().getIsland(playerUUID).setName(null);

        user.sendMessage("general.success");
        return true;
    }

}
