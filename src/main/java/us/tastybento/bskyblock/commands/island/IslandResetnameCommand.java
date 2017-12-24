/**
 * 
 */
package us.tastybento.bskyblock.commands.island;

import java.util.List;
import java.util.UUID;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.config.Settings;

/**
 * @author ben
 *
 */
public class IslandResetnameCommand extends CompositeCommand {

    public IslandResetnameCommand(CompositeCommand islandCommand) {
        super(islandCommand, "resetname");
        this.setPermission(Settings.PERMPREFIX + "island.name");
        this.setOnlyPlayer(true);
        this.setUsage("commands.island.resetname.usage");

    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        UUID playerUUID = user.getUniqueId();

        if (!getIslands().hasIsland(playerUUID)) {
            user.sendMessage("general.errors.no-island");
            return true;
        }

        if (!getIslands().isOwner(playerUUID)) {
            user.sendMessage("general.errors.not-leader");
            return true;
        }
        // Resets the island name
        getIslands().getIsland(playerUUID).setName(null);

        user.sendMessage("general.success");
        return true;
    }

}
