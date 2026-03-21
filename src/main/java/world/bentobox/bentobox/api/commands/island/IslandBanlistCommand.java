package world.bentobox.bentobox.api.commands.island;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Handles the island banlist command, which displays a formatted list of
 * players banned from an island.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable rank requirement</li>
 *   <li>Formatted output with line wrapping</li>
 *   <li>Shows remaining ban slots if applicable</li>
 *   <li>Multiple command aliases (banlist, banned, bans)</li>
 * </ul>
 * <p>
 * Permission: {@code island.ban}
 * <br>
 * Sub-permissions:
 * <ul>
 *   <li>{@code island.ban.maxlimit.[number]} - Sets maximum number of allowed bans</li>
 * </ul>
 * 
 * @author tastybento
 * @since 1.0
 */
public class IslandBanlistCommand extends CompositeCommand {

    /**
     * Cached island instance to avoid multiple database lookups.
     * Set during canExecute and used in execute.
     */
    private Island island;

    public IslandBanlistCommand(CompositeCommand islandCommand) {
        super(islandCommand, "banlist", "banned", "bans");
    }

    @Override
    public void setup() {
        setPermission("island.ban");
        setOnlyPlayer(true);
        setDescription("commands.island.banlist.description");
    }

    /**
     * Checks if the command can be executed by this user.
     * <p>
     * Validation checks:
     * <ul>
     *   <li>No arguments provided (pure banlist command)</li>
     *   <li>Player has an island or is in a team</li>
     *   <li>Player has sufficient rank</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (!args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        // Player issuing the command must have an island
        if (!getIslands().hasIsland(getWorld(), user.getUniqueId()) && !getIslands().inTeam(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        // Check rank to use command
        island = getIslands().getIsland(getWorld(), user.getUniqueId());
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }
        return true;
    }

    /**
     * Displays the list of banned players in a formatted manner.
     * <p>
     * The output is formatted as follows:
     * <ul>
     *   <li>Message if no players are banned</li>
     *   <li>Title followed by names if players are banned</li>
     *   <li>Names are sorted alphabetically</li>
     *   <li>Names are wrapped to ~40 characters per line</li>
     *   <li>Shows number of remaining ban slots if applicable</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Show all the players banned on the island
        if (island.getBanned().isEmpty()) {
            user.sendMessage("commands.island.banlist.noone");
            return true;
        }
        // Title
        user.sendMessage("commands.island.banlist.the-following");
        
        // Create a nicely formatted list with names sorted alphabetically
        List<String> names = island.getBanned().stream()
                .map(u -> getPlayers().getName(u))
                .sorted()
                .toList();
                
        // Format the list into lines of max 40 characters
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        
        // Build lines, ensuring each is no longer than 40 characters
        names.forEach(n -> {
            if (line.length() + n.length() >= 41) {
                lines.add(line.toString().trim());
                line.setLength(0);
            }
            line.append(n);
            line.append(", ");
        });
        // Remove trailing comma
        line.setLength(line.length() - 2);
        // Add the final line if it is not empty
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        
        // Display the formatted lines
        lines.forEach(l -> user.sendMessage("commands.island.banlist.names", "[line]", l));

        // Show remaining ban slots if there's a limit
        int banLimit = user.getPermissionValue(getPermissionPrefix() + "ban.maxlimit", 
                getIWM().getBanLimit(getWorld()));
        if (banLimit > -1 && island.getBanned().size() < banLimit) {
            user.sendMessage("commands.island.banlist.you-can-ban", 
                    TextVariables.NUMBER, 
                    String.valueOf(banLimit - island.getBanned().size()));
        }
        return true;
    }

}
