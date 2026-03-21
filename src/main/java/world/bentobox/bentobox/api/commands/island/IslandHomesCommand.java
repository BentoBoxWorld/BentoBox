package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.panels.customizable.IslandHomesPanel;

/**
 * Handles the island homes command (/island homes).
 * <p>
 * This command opens a GUI panel displaying all available home locations
 * for a player's islands. Players can view and teleport to their homes
 * through this interface.
 * <p>
 * Features:
 * <ul>
 *   <li>GUI-based home management</li>
 *   <li>Support for multiple islands</li>
 *   <li>Visual representation of home locations</li>
 *   <li>One-click teleportation</li>
 * </ul>
 * <p>
 * Permission: {@code island.homes}
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandHomesCommand extends CompositeCommand {

    public IslandHomesCommand(CompositeCommand islandCommand) {
        super(islandCommand, "homes");
    }

    @Override
    public void setup() {
        setPermission("island.homes");
        setOnlyPlayer(true);
        setDescription("commands.island.homes.description");
    }

    /**
     * Validates command execution conditions.
     * <p>
     * Checks if the player has at least one island in the world.
     * Without an island, they cannot have any homes to display.
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check island
        if (getIslands().getIslands(getWorld(), user).isEmpty()) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    /**
     * Opens the homes GUI panel for the user.
     * Panel display is handled by {@link IslandHomesPanel}.
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        IslandHomesPanel.openPanel(this, user);
        return true;
    }

}
