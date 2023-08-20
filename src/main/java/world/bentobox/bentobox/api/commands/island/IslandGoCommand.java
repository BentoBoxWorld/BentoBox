package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.DelayedTeleportCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class IslandGoCommand extends DelayedTeleportCommand {

    public IslandGoCommand(CompositeCommand islandCommand) {
        super(islandCommand, "go", "home", "h");
    }

    @Override
    public void setup() {
        setPermission("island.home");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.go.parameters");
        setDescription("commands.island.go.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check if mid-teleport
        if (getIslands().isGoingHome(user)) {
            // Tell them again that it's in progress
            user.sendMessage("commands.island.go.teleport");
            return false;
        }
        List<Island> islands = getIslands().getIslands(getWorld(), user.getUniqueId());
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check if the island is reserved
        if (checkReserved(user, islands)) {
            return false;
        }
        // Prevent command if player is falling and its not allowed
        if ((getIWM().inWorld(user.getWorld()) && Flags.PREVENT_TELEPORT_WHEN_FALLING.isSetForWorld(user.getWorld()))
                && user.getPlayer().getFallDistance() > 0) {
            // We're sending the "hint" to the player to tell them they cannot teleport while falling.
            user.sendMessage(Flags.PREVENT_TELEPORT_WHEN_FALLING.getHintReference());
            return false;
        }
        // Check if the home is known
        if (!args.isEmpty()) {
            if (!checkHomes(user, islands, args)) {
                // Failed home name check
                user.sendMessage("commands.island.sethome.homes-are");
                islands.forEach(island ->
                island.getHomes().keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s)));
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the args contain a valid home or island name
     * @param user user
     * @param islands user's island list
     * @param args args used
     * @return true if there is a valid home
     */
    private boolean checkHomes(User user, List<Island> islands, List<String> args) {
        boolean result = false;
        String name = String.join(" ", args);
        for (Island island : islands) {
            if ((island.getName() != null && !island.getName().isBlank() && island.getName().equalsIgnoreCase(name)) || getIslands().isHomeLocation(island, name)) {
                result = true;
            }
        }
        if (!result) {
            user.sendMessage("commands.island.go.unknown-home");
        }
        return result;
    }

    private boolean checkReserved(User user, List<Island> islands) {
        for (Island island : islands) {
            if (island.isReserved()) {
                // Send player to create an island
                getParent().getSubCommand("create").ifPresent(createCmd -> createCmd.call(user, createCmd.getLabel(), Collections.emptyList()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer(), String.join(" ", args)));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        List<String> result = new ArrayList<>();
        for (Island island : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            if (island.getName() != null && !island.getName().isBlank()) {
                result.add(island.getName());
            }
            result.addAll(island.getHomes().keySet());
        }
        return Optional.of(Util.tabLimit(result, lastArg));

    }

}
