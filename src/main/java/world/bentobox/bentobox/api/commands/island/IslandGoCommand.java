package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

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

    private Island island;

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
        Set<Island> islands = getIslands().getIslands(getWorld(), user.getUniqueId());
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
            Map<String, Island> names = getNameList(user);
            if (!names.containsKey(String.join(" ", args))) {
                // Failed home name check
                user.sendMessage("commands.island.go.unknown-home");
                user.sendMessage("commands.island.sethome.homes-are");
                names.keySet().forEach(name -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, name));
                return false;
            } else {
                island = names.get(String.join(" ", args));
            }
        }
        return true;
    }

    private boolean checkReserved(User user, Set<Island> islands) {
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
        if (island != null) {
            // This is an island hop
            // Don't use the name, just go home to the new island
            this.delayCommand(user, () -> {
                getIslands().setPrimaryIsland(user.getUniqueId(), island);
                getIslands().homeTeleportAsync(getWorld(), user.getPlayer());
            });
        } else {
            this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer(), String.join(" ", args)));
        }
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";

        return Optional.of(Util.tabLimit(new ArrayList<>(getNameList(user).keySet()), lastArg));

    }

    private Map<String, Island> getNameList(User user) {
        Map<String, Island> islandMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        int index = 1;
        for (Island island : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            if (island.getName() != null && !island.getName().isBlank()) {
                // Name has been set
                islandMap.put(island.getName(), island);
            } else {
                // Name has not been set
                String text = user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
                islandMap.put(text + " " + index++, island);
            }
            // Add homes
            island.getHomes().keySet().forEach(n -> islandMap.put(n, island));
        }
        return islandMap;

    }

}
