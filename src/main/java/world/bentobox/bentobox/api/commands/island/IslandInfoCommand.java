package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.IslandInfo;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island info command (/island info).
 * <p>
 * This command displays detailed information about islands.
 * It can show info for:
 * <ul>
 *   <li>The island at the player's current location (no arguments)</li>
 *   <li>A specific player's island (player name as argument)</li>
 * </ul>
 * <p>
 * Features:
 * <ul>
 *   <li>Console support</li>
 *   <li>Tab completion for player names</li>
 *   <li>Location-based island lookup</li>
 *   <li>Player-based island lookup</li>
 * </ul>
 * <p>
 * Permission: {@code island.info}
 * Aliases: info, who
 *
 * @author Poslovitch
 * @since 1.0
 */
public class IslandInfoCommand extends CompositeCommand {

    public IslandInfoCommand(CompositeCommand parent) {
        super(parent, "info", "who");
    }

    @Override
    public void setup() {
        setPermission("island.info");
        setOnlyPlayer(false);
        setParametersHelp("commands.island.info.parameters");
        setDescription("commands.island.info.description");
    }

    /**
     * Displays island information based on command arguments.
     * <p>
     * Behavior:
     * <ul>
     *   <li>No args + Player: Shows info for island at current location</li>
     *   <li>No args + Console: Shows help</li>
     *   <li>Player name arg: Shows info for that player's island</li>
     *   <li>Invalid args: Shows help</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        if (args.size() > 1 || (args.isEmpty() && !user.isPlayer())) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // If there are no args, then the player wants info on the island at this location
        if (args.isEmpty()) {
            if (!getIslands().getIslandAt(user.getLocation()).map(i -> new IslandInfo(i).showInfo(user)).orElse(false)) {
                user.sendMessage("commands.admin.info.no-island");
                return false;
            }
            return true;
        }
        // Get target player
        UUID targetUUID = getPlayers().getUUID(args.getFirst());
        if (targetUUID == null) {
            user.sendMessage("general.errors.unknown-player", TextVariables.NAME, args.getFirst());
            return false;
        }
        // Get island
        Island island = getIslands().getIsland(getWorld(), targetUUID);
        if (island == null) {
            user.sendMessage("general.errors.player-has-no-island");
            return false;
        }
        // Show info for this player
        new IslandInfo(island).showInfo(user);
        return true;
    }

    /**
     * Provides tab completion for online player names.
     * Requires at least one character to be typed to prevent
     * showing the full player list.
     */
    @Override
    public Optional<List<String>> tabComplete(User user, String alias, List<String> args) {
        String lastArg = !args.isEmpty() ? args.getLast() : "";
        if (lastArg.isEmpty()) {
            // Don't show every player on the server. Require at least the first letter
            return Optional.empty();
        }
        List<String> options = new ArrayList<>(Util.getOnlinePlayerList(user));
        return Optional.of(Util.tabLimit(options, lastArg));
    }
}
