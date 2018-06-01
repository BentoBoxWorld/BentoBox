package us.tastybento.bskyblock.commands.island;

import java.io.IOException;
import java.util.List;

import us.tastybento.bskyblock.api.commands.CompositeCommand;
import us.tastybento.bskyblock.api.events.island.IslandEvent.Reason;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.managers.island.NewIsland;

/**
 * /island create - Create an island.
 *
 * @author Tastybento
 */
public class IslandCreateCommand extends CompositeCommand {

    /**
     * Command to create an island
     * @param islandCommand
     */
    public IslandCreateCommand(CompositeCommand islandCommand) {
        super(islandCommand, "create");
    }

    @Override
    public void setup() {
        setPermission("island.create");
        setOnlyPlayer(true);
        setDescription("commands.island.create.description");
    }

    /* (non-Javadoc)
     * @see us.tastybento.bskyblock.api.commands.CommandArgument#execute(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public boolean execute(User user, List<String> args) {
        if (getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        if (getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        user.sendMessage("commands.island.create.creating-island");
        try {
            NewIsland.builder()
            .player(user)
            .world(getWorld())
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
