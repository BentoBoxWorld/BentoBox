package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;


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
        setPermission("island.name");
        setOnlyPlayer(true);
        setDescription("commands.island.resetname.description");
    }


    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        Island island = getIslands().getIsland(getWorld(), user);

        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        // Check command rank.
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank",
                TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        return true;
    }


    @Override
    public boolean execute(User user, String label, List<String> args) {

        // Resets the island name
        Objects.requireNonNull(getIslands().getIsland(getWorld(), user)).setName(null);
        user.sendMessage("commands.island.resetname.success");
        return true;
    }

}
