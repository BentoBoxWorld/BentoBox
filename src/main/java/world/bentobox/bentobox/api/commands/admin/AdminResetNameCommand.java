package world.bentobox.bentobox.api.commands.admin;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;


/**
 * This command resets players island name.
 * @author BONNe
 */
public class AdminResetNameCommand extends CompositeCommand
{
    /**
     * Default constructor.
     * @param command Parent command.
     */
    public AdminResetNameCommand(CompositeCommand command)
    {
        super(command, "resetname");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setup()
    {
        this.setPermission("mod.resetname");
        this.setOnlyPlayer(true);
        this.setDescription("commands.admin.resetname.description");
    }


    /**
     * @param user the {@link User} who is executing this command.
     * @param label the label which has been used to execute this command.
     *              It can be {@link CompositeCommand#getLabel()} or an alias.
     * @param args the command arguments.
     * @return {@code true} if name can be reset, {@code false} otherwise.
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        return findIsland(user, args) != null;
    }

    private Island findIsland(User user, List<String> args)
    {
        Island island;
        if (args.size() == 1)
        {
            UUID playerUUID = Util.getUUID(args.getFirst());

            if (playerUUID == null)
            {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
                return null;
            }

            island = this.getIslandsManager().getIsland(this.getWorld(), playerUUID);
        }
        else
        {
            // Location-based lookup needs a player: the console has no location
            if (!user.isPlayer())
            {
                user.sendMessage("general.errors.use-in-game");
                return null;
            }
            island = this.getIslandsManager().getIslandAt(user.getLocation()).orElse(null);
        }

        if (island == null)
        {
            user.sendMessage("general.errors.player-has-no-island");
            return null;
        }

        return island;
    }


    /**
     * @param user the {@link User} who is executing this command.
     * @param label the label which has been used to execute this command.
     *              It can be {@link CompositeCommand#getLabel()} or an alias.
     * @param args the command arguments.
     * @return {@code true}
     */
    @Override
    public boolean execute(User user, String label, List<String> args)
    {
        Island island = findIsland(user, args);
        if (island == null)
        {
            return false;
        }

        // Resets the island name
        island.setName(null);
        user.sendMessage("commands.admin.resetname.success", TextVariables.NAME, this.getPlayers().getName(island.getOwner()));
        return true;
    }


    /**
     * @param user the {@link User} who is executing this command.
     * @param alias alias for command
     * @param args command arguments
     * @return Optional of possible values.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        // Return the player names

        if (args.size() == 1)
        {
            return Optional.of(Util.getOnlinePlayerList(user));
        }
        else
        {
            return Optional.empty();
        }
    }


}
