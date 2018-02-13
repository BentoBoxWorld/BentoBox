/**
 *
 */
package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.List;

import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.commands.IslandCommand;
import us.tastybento.bskyblock.database.managers.island.NewIsland;

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
        if (getIslands().hasIsland(user.getUniqueId())) {
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        if (getPlayers().inTeam(user.getUniqueId())) {
            return false;
        }
        user.sendMessage("commands.island.create.creating-island");
        createIsland(user);
        return true;
    }

    /**
     * Creates an island for player
     *
     * @param user
     */
    protected void createIsland(User user) {
        try {
            NewIsland.builder()
            .player(user.getPlayer())
            .reason(Reason.CREATE)
            .build();
        } catch (IOException e) {
            getPlugin().getLogger().severe("Could not create island for player. " + e.getMessage());
            user.sendMessage("commands.island.create.unable-create-island");
        }
    }
}
