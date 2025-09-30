package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island settings command (/island settings).
 * <p>
 * This command opens a tabbed GUI panel that allows players to configure
 * their island's protection and general settings flags.
 * <p>
 * Features:
 * <ul>
 *   <li>Tabbed interface separating protection and settings flags</li>
 *   <li>Cross-world settings access</li>
 *   <li>Location-based island detection</li>
 *   <li>54-slot GUI panel</li>
 * </ul>
 * <p>
 * Permission: {@code island.settings}
 * Aliases: settings, flags, options
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandSettingsCommand extends CompositeCommand {

    /**
     * Cached island instance for the command execution.
     * Set during canExecute and used in execute.
     */
    private Island island;

    public IslandSettingsCommand(CompositeCommand islandCommand) {
        super(islandCommand, "settings", "flags", "options");
    }

    @Override
    public void setup() {
        setPermission("island.settings");
        setOnlyPlayer(true);
        setDescription("commands.island.settings.description");
    }

    /**
     * Validates command execution conditions and determines the target island.
     * <p>
     * Logic:
     * <ul>
     *   <li>If player is in the game world: Uses location to find island</li>
     *   <li>If player is in different world: Uses player's owned island</li>
     *   <li>Fails if no island is found</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (Util.getWorld(user.getWorld()).equals(getWorld())) {
            // Player is in same world
            island = getIslands().getIslandAt(user.getLocation()).orElseGet(() -> getIslands().getIsland(user.getWorld(), user.getUniqueId()));
        } else {
            island = getIslands().getIsland(getWorld(), user);
        }
        if (island == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        return true;
    }

    /**
     * Opens the settings GUI panel.
     * <p>
     * Creates a tabbed panel with:
     * <ul>
     *   <li>Tab 1: Protection flags</li>
     *   <li>Tab 2: Settings flags</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        new TabbedPanelBuilder()
        .user(user)
                .island(island)
        .world(island.getWorld())
                .tab(1, new SettingsTab(getWorld(), user, Flag.Type.PROTECTION))
                .tab(2, new SettingsTab(getWorld(), user, Flag.Type.SETTING))
        .startingSlot(1)
        .size(54)
        .build().openPanel();
        return true;
    }
}
