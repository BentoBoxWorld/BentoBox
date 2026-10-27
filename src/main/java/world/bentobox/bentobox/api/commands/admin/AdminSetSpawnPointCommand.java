package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * This command sets spawn point for island at admin location for island on which admin is located.
 * This command is only for player entity.
 * @author BONNe
 * @since 1.13.0
 */
public class AdminSetSpawnPointCommand extends ConfirmableCommand {
    /**
     * Sub-command constructor
     *
     * @param parent - the parent composite command
     */
    public AdminSetSpawnPointCommand(CompositeCommand parent) {
        super(parent, "setspawnpoint");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setup() {
        this.setPermission("admin.setspawnpoint");
        this.setOnlyPlayer(true);
        this.setDescription("commands.admin.setspawnpoint.description");
    }

    /**
     * This method finds an island in user location and asks confirmation if spawn point
     * must be changed to that location.
     * {@inheritDoc}
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        Optional<Island> optionalIsland = this.getIslands().getIslandAt(user.getLocation());

        if (optionalIsland.isPresent()) {
            Island island = optionalIsland.get();
            var location = user.getLocation().clone();
            var exists = switch (location.getWorld().getEnvironment()) {
                case NETHER -> island.hasNetherIslandAsync();
                case THE_END -> island.hasEndIslandAsync();
                default -> java.util.concurrent.CompletableFuture.completedFuture(true);
            };
            exists.thenCompose(found -> TaskScheduler.userFuture(getPlugin(), user, () -> {
                if (!found || !sameBlock(location, user.getLocation())) {
                    user.sendMessage("commands.admin.setspawnpoint.no-island-here");
                    return;
                }
                this.askConfirmation(user, user.getTranslation("commands.admin.setspawnpoint.confirmation"),
                    () -> {
                        if (sameBlock(location, user.getLocation()) && !island.isDeleted()) setSpawnPoint(user, island);
                        else user.sendMessage("commands.admin.setspawnpoint.no-island-here");
                    });
            })).exceptionally(error -> { getPlugin().logStacktrace(error); return null; });
            return true;
        } else {
            user.sendMessage("commands.admin.setspawnpoint.no-island-here");
            return false;
        }
    }

    /**
     * Compares positions by block, not yaw/pitch: turning around must not invalidate the
     * spot the admin picked.
     */
    private static boolean sameBlock(Location first, Location second) {
        return first != null && second != null && Objects.equals(first.getWorld(), second.getWorld())
                && first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ();
    }

    /**
     * This method changes spawn point for island at given user location.
     * @param user User who initiate spawn point change.
     * @param island Island which is targeted by user.
     */
    private void setSpawnPoint(User user, Island island) {
        island.setSpawnPoint(Objects.requireNonNull(user.getLocation().getWorld()).getEnvironment(),
                user.getLocation());
        user.sendMessage("commands.admin.setspawnpoint.success");

        if (!island.isSpawn()) {
            island.getPlayersOnIsland().forEach(player -> User.getInstance(player)
                    .sendMessage("commands.admin.setspawnpoint.island-spawnpoint-changed", "[user]", user.getName()));
        }
    }
}
