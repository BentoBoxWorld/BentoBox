package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.panels.customizable.IslandCreationPanel;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island creation command (/island create).
 * <p>
 * Features:
 * <ul>
 *   <li>Blueprint-based island creation</li>
 *   <li>Multiple island support with permission-based limits</li>
 *   <li>Blueprint bundle selection via GUI panel</li>
 *   <li>Usage tracking for limited-use blueprints</li>
 *   <li>Team member restrictions</li>
 *   <li>World island limits</li>
 * </ul>
 * <p>
 * Permission nodes:
 * <ul>
 *   <li>{@code island.create} - Base permission</li>
 *   <li>{@code [gamemode].island.number.[number]} - Max concurrent islands</li>
 *   <li>{@code [gamemode].island.create.[blueprintname]} - Blueprint-specific permission</li>
 * </ul>
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandCreateCommand extends CompositeCommand {

    /**
     * Command to create an island
     * 
     * @param islandCommand - parent command
     */
    public IslandCreateCommand(CompositeCommand islandCommand) {
        super(islandCommand, "create", "new");
    }

    @Override
    public void setup() {
        setPermission("island.create");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.create.parameters");
        setDescription("commands.island.create.description");
    }

    /**
     * Checks if the command can be executed by this user.
     * <p>
     * Validation checks:
     * <ul>
     *   <li>Reserved island status</li>
     *   <li>Team member restrictions</li>
     *   <li>Concurrent island limits</li>
     *   <li>World island limits</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check if the island is reserved
        @Nullable
        Island island = getIslands().getPrimaryIsland(getWorld(), user.getUniqueId());
        if (island != null) {
            // Reserved islands can be made
            if (island.isReserved()) {
                return true;
            }
        }
        // Check if this player is on a team in this world
        if (getIWM().getWorldSettings(getWorld()).isDisallowTeamMemberIslands()
                && getIslands().inTeam(getWorld(), user.getUniqueId()) && island != null
                && !user.getUniqueId().equals(island.getOwner())) {
            // Team members who are not owners cannot make additional islands
            user.sendMessage("commands.island.create.you-cannot-make-team");
            return false;
        }
        // Get how many islands this player has
        int num = this.getIslands().getNumberOfConcurrentIslands(user.getUniqueId(), getWorld());
        int max = user.getPermissionValue(
                this.getIWM().getAddon(getWorld()).map(GameModeAddon::getPermissionPrefix).orElse("") + "island.number",
                this.getIWM().getWorldSettings(getWorld()).getConcurrentIslands());
        if (num >= max) {
            // You cannot make an island
            user.sendMessage("commands.island.create.you-cannot-make");
            return false;
        }
        if (getIWM().getMaxIslands(getWorld()) > 0
                && getIslands().getIslandCount(getWorld()) >= getIWM().getMaxIslands(getWorld())) {
            // There is too many islands in the world :(
            user.sendMessage("commands.island.create.too-many-islands");
            return false;
        }
        return true;
    }

    /**
     * Handles the island creation process.
     * <p>
     * Flow:
     * <ul>
     *   <li>With args: Creates island with specified blueprint (if valid and permitted)</li>
     *   <li>Without args:
     *     <ul>
     *       <li>Shows blueprint selection panel if multiple bundles available</li>
     *       <li>Creates island with default blueprint if only one available</li>
     *     </ul>
     *   </li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Permission check if the name is not the default one
        if (!args.isEmpty()) {
            String name = getPlugin().getBlueprintsManager().validate(getAddon(), Util.sanitizeInput(args.getFirst()));
            if (name == null) {
                // The blueprint name is not valid.
                user.sendMessage("commands.island.create.unknown-blueprint");
                return false;
            }
            // Check perm
            if (!getPlugin().getBlueprintsManager().checkPerm(getAddon(), user, Util.sanitizeInput(args.getFirst()))) {
                return false;
            }
            // Check maximum uses
            if (checkMaxUses(user, name)) {
                return false;
            }
            // Make island
            return makeIsland(user, name);
        } else {
            if (getPlugin().getSettings().getIslandNumber() > 1
                    && checkMaxUses(user, BlueprintsManager.DEFAULT_BUNDLE_NAME)) {
                return false;
            }
            // Show panel only if there are multiple bundles available
            if (getPlugin().getBlueprintsManager().getBlueprintBundles(getAddon()).size() > 1) {
                // Show panel
                IslandCreationPanel.openPanel(this, user, label, false);
                return true;
            }
            return makeIsland(user, BlueprintsManager.DEFAULT_BUNDLE_NAME);
        }
    }

    /**
     * Checks if a user has reached the maximum uses for a blueprint bundle.
     * 
     * @param user The user to check
     * @param name The blueprint bundle name
     * @return true if max uses reached, false otherwise
     */
    private boolean checkMaxUses(User user, String name) {
        if (getPlugin().getBlueprintsManager().getBlueprintBundles(getAddon()).containsKey(name)) {
            int maxTimes = getPlugin().getBlueprintsManager().getBlueprintBundles(getAddon()).get(name).getTimes();
            // Check how many times this player has used this bundle
            if (maxTimes > 0 && getBundleUses(user, name) >= maxTimes) {
                user.sendMessage("commands.island.create.max-uses");
                return true;
            }
        }
        return false;
    }

    /**
     * Counts how many times a user has used a specific blueprint bundle.
     * Checks island metadata for the "bundle" key matching the given name.
     * 
     * @param user The user to check
     * @param name The blueprint bundle name
     * @return Number of times the bundle has been used
     */
    private long getBundleUses(User user, String name) {
        return getIslands().getIslands(getWorld(), user).stream()
                .filter(is -> is.getMetaData("bundle").map(mdv -> name.equalsIgnoreCase(mdv.asString())).orElse(false))
                .count();
    }

    /**
     * Creates a new island for the user using the specified blueprint.
     * Also handles reset cooldown if configured.
     * 
     * @param user The user getting the new island
     * @param name The blueprint bundle name to use
     * @return true if island creation was successful
     */
    private boolean makeIsland(User user, String name) {
        user.sendMessage("commands.island.create.creating-island");
        try {
            NewIsland.builder().player(user).addon(getAddon()).reason(Reason.CREATE).name(name).build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage(e.getMessage());
            return false;
        }
        if (getSettings().isResetCooldownOnCreate()) {
            getParent().getSubCommand("reset").ifPresent(
                    resetCommand -> resetCommand.setCooldown(user.getUniqueId(), getSettings().getResetCooldown()));
        }
        return true;
    }
}
