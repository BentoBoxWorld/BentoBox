package world.bentobox.bentobox.api.commands.admin;

import org.eclipse.jdt.annotation.Nullable;
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
        if (args.size() == 1)
        {
            UUID playerUUID = Util.getUUID(args.get(0));

            if (playerUUID == null)
            {
                user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
                return false;
            }

            this.island = this.getIslandsManager().getIsland(this.getWorld(), playerUUID);
        }
        else
        {
            this.island = this.getIslandsManager().getIslandAt(user.getLocation()).orElse(null);
        }

        if (this.island == null)
        {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        return true;
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
        if (this.island == null)
        {
            this.showHelp(this, user);
            return true;
        }

        // Resets the island name
        this.island.setName(null);
        user.sendMessage("commands.admin.resetname.success", TextVariables.NAME, this.getPlayers().getName(this.island.getOwner()));
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


    /**
     * Island which name must be changed.
     */
    @Nullable
    private Island island;
}
