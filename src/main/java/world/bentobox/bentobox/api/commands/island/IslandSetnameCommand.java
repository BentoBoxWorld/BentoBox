package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;

import org.bukkit.ChatColor;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;


/**
 * Handles the island set name command (/island setname).
 * <p>
 * This command allows players to set or change their island's display name.
 * Names can include color codes if the player has the appropriate permission.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable name length limits</li>
 *   <li>Color code support (with permission)</li>
 *   <li>Configurable rank requirement</li>
 *   <li>Name change event firing</li>
 * </ul>
 * <p>
 * Permissions:
 * <ul>
 *   <li>{@code island.name} - Base permission</li>
 *   <li>{@code island.name.format} - Allows use of color codes</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandSetnameCommand extends CompositeCommand {

    public IslandSetnameCommand(CompositeCommand islandCommand) {
        super(islandCommand, "setname");
    }

    @Override
    public void setup() {
        setPermission("island.name");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.setname.parameters");
        setDescription("commands.island.setname.description");
        setConfigurableRankCommand();
    }


    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Name argument provided</li>
     *   <li>Player has an island</li>
     *   <li>Player has sufficient rank</li>
     *   <li>Name length within limits</li>
     *   <li>Name not empty after color stripping</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args)
    {
        // Explain command
        if (args.isEmpty()) {
            showHelp(this, user);
            return false;
        }

        Island island = getIslands().getIsland(getWorld(), user);

        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }

        // Check command rank.
        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK,
                    user.getTranslation(RanksManager.getInstance().getRank(rank)));
            return false;
        }

        // Naming the island - join all the arguments with spaces.
        String name = String.join(" ", args);

        // Check if the name isn't too short or too long
        if (name.length() < getSettings().getNameMinLength() || Util.stripColor(name).isEmpty()) {
            user.sendMessage("commands.island.setname.name-too-short", TextVariables.NUMBER, String.valueOf(getSettings().getNameMinLength()));
            return false;
        }
        if (name.length() > getSettings().getNameMaxLength()) {
            user.sendMessage("commands.island.setname.name-too-long", TextVariables.NUMBER, String.valueOf(getSettings().getNameMaxLength()));
            return false;
        }

        // Apply colors
        if (user.hasPermission(getPermissionPrefix() + "island.name.format")) {
            name = ChatColor.translateAlternateColorCodes('&', name);
        }

        return true;
    }


    /**
     * Sets the island name and fires the name change event.
     * <p>
     * Process:
     * <ul>
     *   <li>Joins arguments into name string</li>
     *   <li>Applies color codes if permitted</li>
     *   <li>Updates island name</li>
     *   <li>Fires IslandEvent with NAME reason</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Naming the island - join all the arguments with spaces.
        String name = String.join(" ", args);

        // Apply colors
        if (user.hasPermission(getPermissionPrefix() + "island.name.format")) {
            name = ChatColor.translateAlternateColorCodes('&', name);
        }

        // Everything's good!
        Island island = Objects.requireNonNull(getIslands().getIsland(getWorld(), user));
        String previousName = island.getName();
        island.setName(name);
        user.sendMessage("commands.island.setname.success", TextVariables.NAME, name);
        // Fire the IslandNameEvent
        new IslandEvent.IslandEventBuilder()
        .island(island)
        .involvedPlayer(user.getUniqueId())
        .reason(IslandEvent.Reason.NAME)
        .previousName(previousName)
        .admin(false)
        .build();
        return true;
    }
}
