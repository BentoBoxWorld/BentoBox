/**
 *
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.user.User;

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
        setPermission(Constants.PERMPREFIX + "island.name");
        setOnlyPlayer(true);
        setDescription("commands.island.resetname.description");

    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();

        if (!getIslands().hasIsland(user.getWorld(), playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        if (!getIslands().isOwner(user.getWorld(), playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return false;
        }
        // Resets the island name
        getIslands().getIsland(user.getWorld(), playerUUID).setName(null);

        user.sendMessage("general.success");
        return true;
    }

}
