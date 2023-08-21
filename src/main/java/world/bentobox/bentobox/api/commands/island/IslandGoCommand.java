package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import world.bentobox.bentobox.BentoBox;
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
            List<String> names = getNameList(user);
            if (!names.contains(String.join(" ", args))) {
                // Failed home name check
                user.sendMessage("commands.island.go.unknown-home");
                user.sendMessage("commands.island.sethome.homes-are");
                names.forEach(name -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, name));
                return false;
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
        this.delayCommand(user, () -> getIslands().homeTeleportAsync(getWorld(), user.getPlayer(), String.join(" ", args)));
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        return Optional.of(Util.tabLimit(getNameList(user), lastArg));

    }

    private List<String> getNameList(User user) {
        BentoBox.getInstance().logDebug("getNameList ");
        List<String> result = new ArrayList<>();
        int index = 1;
        for (Island island : getIslands().getIslands(getWorld(), user.getUniqueId())) {
            BentoBox.getInstance().logDebug("island ");
            // TODO DEBUG THIS. WHY IS THERE REPEATED ITEMS IN THIS LIST?
            if (island.getName() != null && !island.getName().isBlank()) {
                // Name has been set
                result.add(island.getName());
            } else {
                // Name has not been set
                String text = user.getTranslation("protection.flags.ENTER_EXIT_MESSAGES.island", TextVariables.NAME, user.getName(), TextVariables.DISPLAY_NAME, user.getDisplayName());
                result.add(text + " " + index++);
            }
            // Add homes
            BentoBox.getInstance().logDebug("There are " + result.size() + " island names and " + island.getHomes().size() + " homes");
            result.addAll(island.getHomes().keySet());

        }
        return result;

    }

}
