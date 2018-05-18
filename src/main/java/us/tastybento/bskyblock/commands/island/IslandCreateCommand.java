/**
 *
 */
package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.List;

import org.bukkit.World;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.managers.island.NewIsland;

/**
 * /island create - Create an island.
 *
 * @author Tastybento
 */
public class IslandCreateCommand extends CompositeCommand {

    public IslandCreateCommand(IslandCommand islandCommand) {
        super(islandCommand, "create", "auto");
    }

    @Override
    public void setup() {
        setPermission(Constants.PERMPREFIX + "island.create");
        setOnlyPlayer(true);
        setDescription("commands.island.create.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        // TODO: fix world
        World world = getPlugin().getIslandWorldManager().getIslandWorld();
        if (!args.isEmpty()) {
            // Get game types
            // TODO
        }
        if (getIslands().hasIsland(world, user.getUniqueId())) {
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        if (getIslands().inTeam(world, user.getUniqueId())) {
            return false;
        }
        user.sendMessage("commands.island.create.creating-island");
        try {
            NewIsland.builder()
            .player(user)
            .world(world)
            .reason(Reason.CREATE)
            .build();
            return true;
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage("commands.island.create.unable-create-island");
            return false;
        }
    }
}
