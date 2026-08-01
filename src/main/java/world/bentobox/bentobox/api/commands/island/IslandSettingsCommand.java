package world.bentobox.bentobox.api.commands.island;

import java.util.List;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.builders.TabbedPanelBuilder;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.panels.settings.WorldProtectionInfoTab;
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
     *   <li>If no island is found, the panel still opens and shows the
     *       protection flags that apply out in the world - useful when standing
     *       in the wilderness and wondering what rules apply there</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (getWorld().equals(Util.getWorld(user.getWorld()))) {
            // Player is in same world
            island = getIslands().getIslandAt(user.getLocation()).orElseGet(() -> getIslands().getIsland(user.getWorld(), user.getUniqueId()));
        } else {
            island = getIslands().getIsland(getWorld(), user);
        }
        // A null island is fine: the panel falls back to the world's protection
        // flags, which are what applies off-island
        return true;
    }

    /**
     * Opens the settings GUI panel.
     * <p>
     * With an island, creates a tabbed panel with:
     * <ul>
     *   <li>Tab 1: Protection flags</li>
     *   <li>Tab 2: Settings flags</li>
     * </ul>
     * Without an island there is nothing to configure, so a single read-only tab
     * of the world's protection flags is shown instead. That answers the only
     * question a player can ask out in the world: what am I allowed to do here?
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        buildPanel(user).build().openPanel();
        return true;
    }

    /**
     * Builds the panel shown by this command.
     * @param user user to show it to
     * @return the panel builder for the island the player is on, or for the world
     *         if they are not on one
     * @since 3.22.0
     */
    protected TabbedPanelBuilder buildPanel(User user) {
        if (island == null) {
            return new TabbedPanelBuilder()
                    .user(user)
                    .world(getWorld())
                    .tab(1, new WorldProtectionInfoTab(getWorld(), user))
                    .startingSlot(1)
                    .size(54);
        }
        return new TabbedPanelBuilder()
                .user(user)
                .island(island)
                .world(island.getWorld())
                .tab(1, new SettingsTab(getWorld(), user, Flag.Type.PROTECTION))
                .tab(2, new SettingsTab(getWorld(), user, Flag.Type.SETTING))
                .startingSlot(1)
                .size(54);
    }
}
