package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.island.NewIsland;

/**
 * /island create - Create an island.
 *
 * @author Tastybento
 */
public class IslandCreateCommand extends CompositeCommand {

    /**
     * Command to create an island
     * @param islandCommand - parent command
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

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (getIslands().hasIsland(getWorld(), user.getUniqueId())
                || getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.already-have-island");
            return false;
        }
        if (getIWM().getMaxIslands(getWorld()) > 0
                && getIslands().getIslandCount(getWorld()) >= getIWM().getMaxIslands(getWorld())) {
            // There is too many islands in the world :(
            user.sendMessage("commands.island.create.too-many-islands");
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
