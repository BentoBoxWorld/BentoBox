package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.primitives.Ints;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * Sets the maximum number of homes allowed on this island.
 * 
 * Commands:
 * <ul>
 *     <li><b>/bsb maxhomes &lt;player&gt; &lt;number&gt;</b> - Sets the maximum number of homes for each island where the player is the owner. This could apply to multiple islands.</li>
 *     <li><b>/bsb maxhomes &lt;player&gt; &lt;number&gt; [island name]</b> - Sets the maximum number of homes for a specific named island where the player is the owner.</li>
 *     <li><b>/bsb maxhomes &lt;number&gt;</b> - Sets the maximum number of homes for the island you are standing on (in-game only).</li>
 * </ul>
 * 
 * @author tastybento
 * @since 2.6.0
 */

public class AdminMaxHomesCommand extends ConfirmableCommand {

    Integer maxHomes;
    Map<String, Island> islands = new HashMap<>();

    public AdminMaxHomesCommand(CompositeCommand parent) {
        super(parent, "setmaxhomes");
    }

    @Override
    public void setup() {
        setPermission("mod.maxhomes");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.maxhomes.parameters");
        setDescription("commands.admin.maxhomes.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        islands.clear();
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }
        // Check arguments
        if (args.size() == 1) {
            // Player must be in game
            if (!user.isPlayer()) {
                user.sendMessage("general.errors.use-in-game");
                return false;
            }
            // Check world
            if (user.getWorld() != getWorld()) {
                user.sendMessage("general.errors.wrong-world");
                return false;
            }
            // Arg must be an integer to return true, otherwise false
            maxHomes = Ints.tryParse(args.get(0));
            if (maxHomes == null || maxHomes < 1) {
                user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(0));
                return false;
            }
            // Get the island the user is standing on
            boolean onIsland = getIslands().getIslandAt(user.getLocation()).map(is -> {
                islands.put("", is);
                return true;
            }).orElse(false);
            if (!onIsland) {
                user.sendMessage("general.errors.not-on-island");
                return false;
            }
            return true;
        }
        // More than one argument
        // First arg must be a valid player name
        UUID targetUUID = getPlayers().getUUID(args.get(0));
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.get(0));
            return false;
        }
        // Second arg must be the max homes number
        maxHomes = Ints.tryParse(args.get(1));
        if (maxHomes == null) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return false;
        }
        // Get islands
        islands = this.getNameIslandMap(User.getInstance(targetUUID));
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        if (args.size() > 2) {
            // A specific island is mentioned. Parse which one it is and remove the others
            final String name = String.join(" ", args.subList(2, args.size())); // Join all the args from here with spaces

            islands.keySet().removeIf(n -> !name.equalsIgnoreCase(n));

            if (islands.isEmpty()) {
                // Failed name check - there are either
                user.sendMessage("commands.admin.maxhomes.errors.unknown-island", TextVariables.NAME, name);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (islands.isEmpty() || maxHomes < 1) {
            // Sanity check
            return false;
        }
        islands.forEach((name, island) -> {
            island.setMaxHomes(maxHomes);
            user.sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, name, TextVariables.NUMBER,
                    String.valueOf(maxHomes));
        });
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.get(args.size()-1) : "";
        if (args.size() == 2) {
            // Suggest player names
            return Optional.of(Util.getOnlinePlayerList(user));
        }
        if (args.size() > 3) {
        return Optional.of(Util.tabLimit(new ArrayList<>(getNameIslandMap(user).keySet()), lastArg));
        }
        return Optional.of(List.of("1"));

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
