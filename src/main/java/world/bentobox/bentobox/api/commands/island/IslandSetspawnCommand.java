package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;

import org.bukkit.Location;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

public class IslandSetspawnCommand extends ConfirmableCommand {
    private @Nullable Island island;

    public IslandSetspawnCommand(CompositeCommand islandCommand) {
        super(islandCommand, "setspawn");
    }

    @Override
    public void setup() {
        setPermission("island.setspawn");
        setOnlyPlayer(true);
        setDescription("commands.island.setspawn.description");
        setConfigurableRankCommand();
        setDefaultCommandRank(RanksManager.SUB_OWNER_RANK);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null || island.getOwner() == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!island.onIsland(user.getLocation())) {
            user.sendMessage("commands.island.setspawn.must-be-on-your-island");
            return false;
        }

        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.askConfirmation(user, user.getTranslation("commands.island.reset.confirmation"), () -> doSetSpawn(user));
        return true;
    }

    private void doSetSpawn(User user) {
        Location userLocation = user.getLocation();
        userLocation.setX(Math.floor(userLocation.getX())+0.5D);
        userLocation.setZ(Math.floor(userLocation.getZ())+0.5D);
        island.setSpawnPoint(userLocation.getWorld().getEnvironment(), userLocation);
        user.sendMessage("commands.island.setspawn.spawn-set");
    }
}
