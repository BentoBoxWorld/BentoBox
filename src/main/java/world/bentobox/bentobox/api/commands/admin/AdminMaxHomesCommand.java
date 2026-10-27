package world.bentobox.bentobox.api.commands.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.common.primitives.Ints;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand;
import world.bentobox.bentobox.api.commands.island.IslandGoCommand.IslandInfo;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Sets the maximum number of homes allowed on this island.
 * <p>
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

    /**
     * Resolved once per command run. This command object is shared by every player on
     * the server, so nothing may be kept in instance fields between canExecute and execute.
     */
    private record MaxHomesTarget(Map<String, IslandInfo> islands, int maxHomes) {
    }

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
        return resolveTarget(user, args) != null;
    }

    private @Nullable MaxHomesTarget resolveTarget(User user, List<String> args) {
        if (args.isEmpty()) {
            showHelp(this, user);
            return null;
        }
        return args.size() == 1 ? resolveStandingOnIsland(user, args) : resolveWithTarget(user, args);
    }

    private @Nullable MaxHomesTarget resolveStandingOnIsland(User user, List<String> args) {
        if (!user.isPlayer()) {
            user.sendMessage("general.errors.use-in-game");
            return null;
        }
        if (user.getWorld() != getWorld()) {
            user.sendMessage("general.errors.wrong-world");
            return null;
        }
        Integer maxHomes = Ints.tryParse(args.getFirst());
        if (maxHomes == null || maxHomes < 1) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.getFirst());
            return null;
        }
        Optional<world.bentobox.bentobox.database.objects.Island> island = getIslands().getIslandAt(user.getLocation());
        if (island.isEmpty()) {
            user.sendMessage("general.errors.not-on-island");
            return null;
        }
        return new MaxHomesTarget(Map.of("", new IslandInfo(island.get(), false)), maxHomes);
    }

    private @Nullable MaxHomesTarget resolveWithTarget(User user, List<String> args) {
        UUID targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return null;
        }
        Integer maxHomes = Ints.tryParse(args.get(1));
        if (maxHomes == null || maxHomes < 1) {
            user.sendMessage("general.errors.must-be-positive-number", TextVariables.NUMBER, args.get(1));
            return null;
        }
        Map<String, IslandInfo> islands = new HashMap<>(
                IslandGoCommand.getNameIslandMap(User.getInstance(targetUUID), getWorld()));
        if (islands.isEmpty()) {
            user.sendMessage("general.errors.player-has-no-island");
            return null;
        }
        if (args.size() > 2) {
            final String name = String.join(" ", args.subList(2, args.size()));
            islands.keySet().removeIf(n -> !name.equalsIgnoreCase(n));
            if (islands.isEmpty()) {
                user.sendMessage("commands.admin.maxhomes.errors.unknown-island", TextVariables.NAME, name);
                return null;
            }
        }
        return new MaxHomesTarget(islands, maxHomes);
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        MaxHomesTarget target = resolveTarget(user, args);
        if (target == null) {
            return false;
        }
        target.islands().forEach((name, island) -> {
            island.island().setMaxHomes(target.maxHomes());
            user.sendMessage("commands.admin.maxhomes.max-homes-set", TextVariables.NAME, name, TextVariables.NUMBER,
                    String.valueOf(target.maxHomes()));
        });
        return true;
    }

    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (args.size() == 2) {
            // Suggest player names
            return Optional.of(Util.getOnlinePlayerList(user));
        }
        if (args.size() > 3) {
            // Work out who is in arg 2
            UUID targetUUID = getPlayers().getUUID(args.get(1));
            if (targetUUID != null) {
                User target = User.getInstance(targetUUID);
                return Optional.of(Util.tabLimit(new ArrayList<>(IslandGoCommand.getNameIslandMap(target, getWorld()).keySet()), lastArg));
            }
        }
        return Optional.of(List.of("1"));

    }

}
