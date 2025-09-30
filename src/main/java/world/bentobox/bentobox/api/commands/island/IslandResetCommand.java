package world.bentobox.bentobox.api.commands.island;

import java.io.IOException;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.island.NewIsland.Builder;
import world.bentobox.bentobox.panels.customizable.IslandCreationPanel;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the island reset command (/island reset).
 * <p>
 * This command allows players to reset their island, optionally with a new blueprint.
 * The process includes:
 * <ul>
 *   <li>Cooldown management</li>
 *   <li>Reset limit tracking</li>
 *   <li>Team member removal</li>
 *   <li>Old island deletion</li>
 *   <li>New island creation</li>
 * </ul>
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable cooldown period</li>
 *   <li>Limited or unlimited resets</li>
 *   <li>Multiple blueprint support</li>
 *   <li>Optional confirmation dialog</li>
 *   <li>Blueprint selection GUI</li>
 * </ul>
 * <p>
 * Permission: {@code island.reset}
 * Aliases: reset, restart
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandResetCommand extends ConfirmableCommand {

    /**
     * If true, skips pasting a new island after reset.
     * Used for special game modes or testing.
     */
    private boolean noPaste;

    public IslandResetCommand(CompositeCommand islandCommand) {
        super(islandCommand, "reset", "restart");
    }

    /**
     * Creates the island reset command
     * 
     * @param islandCommand - parent command
     * @param noPaste       - true if resetting should not paste a new island
     */
    public IslandResetCommand(CompositeCommand islandCommand, boolean noPaste) {
        super(islandCommand, "reset", "restart");
        this.noPaste = noPaste;
    }

    @Override
    public void setup() {
        setPermission("island.reset");
        setOnlyPlayer(true);
        setParametersHelp("commands.island.reset.parameters");
        setDescription("commands.island.reset.description");
    }

    /**
     * Validates command execution conditions.
     * <p>
     * Checks:
     * <ul>
     *   <li>Reset cooldown period</li>
     *   <li>Island ownership</li>
     *   <li>Remaining reset count</li>
     * </ul>
     */
    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        // Check cooldown
        if (getSettings().getResetCooldown() > 0 && checkCooldown(user)) {
            return false;
        }
        if (!getIslands().hasIsland(getWorld(), user.getUniqueId())) {
            user.sendMessage("general.errors.not-owner");
            return false;
        }
        int resetsLeft = getPlayers().getResetsLeft(getWorld(), user.getUniqueId());
        if (resetsLeft != -1) {
            // Resets are not unlimited here
            if (resetsLeft == 0) {
                // No resets allowed
                user.sendMessage("commands.island.reset.none-left");
                return false;
            } else {
                // Still some resets left
                // Notify how many resets are left
                user.sendMessage("commands.island.reset.resets-left", TextVariables.NUMBER, String.valueOf(resetsLeft));
            }
        }

        return true;
    }

    /**
     * Handles the reset process based on arguments.
     * <p>
     * Flow:
     * <ul>
     *   <li>With args: Directly reset with specified blueprint</li>
     *   <li>Without args: Show confirmation and/or blueprint selection</li>
     * </ul>
     */
    @Override
    public boolean execute(User user, String label, List<String> args) {
        // Permission check if the name is not the default one
        if (!args.isEmpty()) {
            String name = getPlugin().getBlueprintsManager().validate(getAddon(), Util.sanitizeInput(args.get(0)));
            if (name == null || name.isEmpty()) {
                // The blueprint name is not valid.
                user.sendMessage("commands.island.create.unknown-blueprint");
                return false;
            }
            if (!getPlugin().getBlueprintsManager().checkPerm(getAddon(), user, Util.sanitizeInput(args.get(0)))) {
                return false;
            }
            return resetIsland(user, name);
        } else {
            // Show panel after confirmation
            if (getPlugin().getSettings().isResetConfirmation()) {
                this.askConfirmation(user, user.getTranslation("commands.island.reset.confirmation"),
                        () -> selectBundle(user, label));
            } else {
                selectBundle(user, label);
            }
            return true;
        }
    }

    /**
     * Manages blueprint selection process.
     * Shows GUI if multiple blueprints are available,
     * otherwise uses default blueprint.
     * 
     * @param user The user resetting their island
     * @param label The command label used
     */
    private void selectBundle(@NonNull User user, @NonNull String label) {
        // Show panel only if there are multiple bundles available
        if (getPlugin().getBlueprintsManager().getBlueprintBundles(getAddon()).size() > 1) {
            // Show panel - once the player selected a bundle, this will re-run this command
            IslandCreationPanel.openPanel(this, user, label, true);
        } else {
            resetIsland(user, BlueprintsManager.DEFAULT_BUNDLE_NAME);
        }
    }

    /**
     * Performs the actual island reset process.
     * <p>
     * Process:
     * <ul>
     *   <li>Delete old island</li>
     *   <li>Create new island</li>
     *   <li>Apply cooldown</li>
     * </ul>
     * 
     * @param user The user resetting their island
     * @param name The blueprint bundle name to use
     * @return true if reset was successful
     */
    private boolean resetIsland(User user, String name) {
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(getWorld(), user);
        deleteOldIsland(user, oldIsland);

        user.sendMessage("commands.island.create.creating-island");
        // Create new island and then delete the old one
        try {
            Builder builder = NewIsland.builder().player(user).reason(Reason.RESET).addon(getAddon())
                    .oldIsland(oldIsland).name(name);
            if (noPaste)
                builder.noPaste();
            builder.build();
        } catch (IOException e) {
            getPlugin().logError("Could not create island for player. " + e.getMessage());
            user.sendMessage(e.getMessage());
            return false;
        }
        setCooldown(user.getUniqueId(), getSettings().getResetCooldown());
        return true;
    }

    /**
     * Handles old island cleanup before reset.
     * Fires preclear event and removes all team members.
     * 
     * @param user The user resetting their island
     * @param oldIsland The island being reset
     */
    private void deleteOldIsland(User user, Island oldIsland) {
        // Fire island preclear event
        IslandEvent.builder().involvedPlayer(user.getUniqueId()).reason(Reason.PRECLEAR).island(oldIsland)
                .oldIsland(oldIsland).location(oldIsland.getCenter()).build();

        // Reset the island

        // Kick all island members (including the owner)
        kickMembers(oldIsland);

        // Add a reset
        getPlayers().addReset(getWorld(), user.getUniqueId());
    }

    /**
     * Removes all members from an island.
     * Handles member cleanup and event firing.
     * Does not use team kick command to avoid permission issues.
     * 
     * @param island The island to remove members from
     * @since 1.7.0
     */
    private void kickMembers(Island island) {
        /*
         * We cannot assume the island owner can run /[cmd] team kick (it might be
         * disabled, or there could be permission restrictions...) Therefore, we need to
         * do it manually. Plus, a more specific team event (TeamDeleteEvent) is called
         * by this method.
         */
        island.getMemberSet().forEach(memberUUID -> {

            User member = User.getInstance(memberUUID);
            // Send a "you're kicked" message if the member is not the island owner (send
            // before removing!)
            if (!memberUUID.equals(island.getOwner())) {
                member.sendMessage("commands.island.reset.kicked-from-island", TextVariables.GAMEMODE,
                        getAddon().getDescription().getName());
            }
            // Remove player
            getIslands().removePlayer(island, memberUUID);

            // Clean player
            getPlayers().cleanLeavingPlayer(getWorld(), member, false, island);

            // Fire event
            TeamEvent.builder().island(island).reason(TeamEvent.Reason.DELETE).involvedPlayer(memberUUID).build();
        });
    }
}
