package world.bentobox.bentobox.api.commands.admin;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.World;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;


/**
 * This command sets spawn point for island at admin location for island on which admin is located.
 * This command is only for player entity.
 * @author BONNe
 * @since 1.13.0
 */
public class AdminSetSpawnPointCommand extends ConfirmableCommand
{
    /**
     * Sub-command constructor
     *
     * @param parent - the parent composite command
     */
    public AdminSetSpawnPointCommand(CompositeCommand parent)
    {
        super(parent, "setspawnpoint");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setup()
    {
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
    public boolean execute(User user, String label, List<String> args)
    {
        Optional<Island> optionalIsland = this.getIslands().getIslandAt(user.getLocation());

        if (optionalIsland.isPresent() &&
            (optionalIsland.get().hasNetherIsland() ||
            !World.Environment.NETHER.equals(user.getLocation().getWorld().getEnvironment())) &&
            (optionalIsland.get().hasEndIsland() ||
            !World.Environment.THE_END.equals(user.getLocation().getWorld().getEnvironment())))
        {
            // Everything's fine, we can set the location as spawn point for island :)
            this.askConfirmation(user, user.getTranslation("commands.admin.setspawnpoint.confirmation"),
                () -> this.setSpawnPoint(user, optionalIsland.get()));

            return true;
        }
        else
        {
            user.sendMessage("commands.admin.setspawnpoint.no-island-here");
            return false;
        }
    }


    /**
     * This method changes spawn point for island at given user location.
     * @param user User who initiate spawn point change.
     * @param island Island which is targeted by user.
     */
    private void setSpawnPoint(User user, Island island)
    {
        island.setSpawnPoint(Objects.requireNonNull(user.getLocation().getWorld()).getEnvironment(),
            user.getLocation());
        user.sendMessage("commands.admin.setspawnpoint.success");

        if (!island.isSpawn())
        {
            island.getPlayersOnIsland().forEach(player ->
                User.getInstance(player).sendMessage("commands.admin.setspawnpoint.island-spawnpoint-changed",
                    "[user]", user.getName()));
        }
    }
}
