package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class AdminResetHomeCommand extends CompositeCommand
{
    Map<String, Island> islands = new HashMap<>();

    /**
     * Default constructor.
     * @param command Parent command.
     */
    public AdminResetHomeCommand(CompositeCommand command)
    {
        super(command, "resethome");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setup()
    {
        this.setPermission("mod.resethome");
        this.setDescription("commands.admin.resethome.description");
        this.setParametersHelp("commands.admin.resethome.parameters");
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
        islands.clear();
        if (args.isEmpty()) {
            this.showHelp(this, user);
            return false;
        }
        // First arg must be a valid player name
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Get islands
        islands = this.getNameIslandMap(User.getInstance(targetUUID));
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }

        // Second optional arg must be the name of the island
        if (args.size() == 1) {
            return true;
        }

        // A specific island is mentioned. Parse which one it is and remove the others
        final String name = String.join(" ", args.subList(1, args.size())); // Join all the args from here with spaces

        islands.keySet().removeIf(n -> !name.equalsIgnoreCase(n));

        if (islands.isEmpty()) {
            // Failed name check - there are either
            user.sendMessage("commands.admin.maxhomes.errors.unknown-island", TextVariables.NAME, name);
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
        if (islands.isEmpty()) {
            // Sanity check
            return false;
        }
        islands.forEach((name, island) -> {
            island.getHomes().keySet().removeIf(String::isEmpty); // Remove the default home
            user.sendMessage("commands.admin.resethome.cleared", TextVariables.NAME, name);
        });

        user.sendMessage("general.success");
        return true;
    }


    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size() - 1) : "";
        if (args.size() == 2) {
            // Suggest player names
            return Optional.of(Util.getOnlinePlayerList(user));
        }
        if (args.size() > 2) {
            // Work out who is in arg 2
            UUID targetUUID = getPlayers().getUUID(args.get(0));
            if (targetUUID != null) {
                User target = User.getInstance(targetUUID);
                return Optional.of(Util.tabLimit(new ArrayList<>(getNameIslandMap(target).keySet()), lastArg));
            }
        }
        return Optional.empty();

    }

    Map<String, Island> getNameIslandMap(User user) {
        Map<String, Island> islandMap = new HashMap<>();
        int index = 0;
        for (Island island : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            index++;
            if (island.getName() != null && !island.getName().isBlank()) {
                // Name has been set
                islandMap.put(island.getName(), island);
            } else {
                // Name has not been set
                String text = user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME,
                        user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName()) + " " + index;
                islandMap.put(text, island);
            }
        }

        return islandMap;

    }

}
