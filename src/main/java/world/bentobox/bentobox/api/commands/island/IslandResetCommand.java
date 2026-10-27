package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
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
 *   <li>New island creation and teleport</li>
 *   <li>Team member removal and old island deletion</li>
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
        if (NewIsland.isCreating(user)) {
            user.sendMessage("commands.island.create.creating-island");
            return false;
        }
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
            String name = getPlugin().getBlueprintsManager().validate(getAddon(), Util.sanitizeInput(args.getFirst()));
            if (name == null || name.isEmpty()) {
                // The blueprint name is not valid.
                user.sendMessage("commands.island.create.unknown-blueprint");
                return false;
            }
            if (!getPlugin().getBlueprintsManager().checkPerm(getAddon(), user, Util.sanitizeInput(args.getFirst()))) {
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
    private boolean resetIsland(@NonNull User user, String name) {
        if (NewIsland.isCreating(user)) {
            user.sendMessage("commands.island.create.creating-island");
            return false;
        }
        // Get the player's old island
        Island oldIsland = getIslands().getIsland(getWorld(), user);
        if (oldIsland == null) {
            // Should not happen, but the player has no island to reset
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!preclearAllowed(user, oldIsland)) return false;
        user.sendMessage("commands.island.create.creating-island");
        // Charge the cooldown as soon as the attempt starts. A failed reset must not be free to
        // retry, otherwise a player can pile up islands faster than the cleanup can reap them.
        setCooldown(user.getUniqueId(), getSettings().getResetCooldown());
        // Create new island and then delete the old one
            Builder builder = NewIsland.builder().player(user).reason(Reason.RESET).addon(getAddon())
                    .oldIsland(oldIsland).name(name);
            if (noPaste)
                builder.noPaste();
            boolean started = BlueprintCostHelper.build(getPlugin(), getAddon(), user, name, getWorld(), builder,
                    getPlugin().getSettings().isChargeForBlueprintOnReset(),
                    () -> CompletableFuture.completedFuture(null),
                    () -> getPlayers().addReset(getWorld(), user.getUniqueId()),
                    () -> setCooldown(user.getUniqueId(), 0));
            if (!started) {
                // Nothing was created (cannot afford it, another creation in progress, ...), so
                // the attempt must not cost the player their cooldown.
                setCooldown(user.getUniqueId(), 0);
            }
            return started;
    }

    private boolean preclearAllowed(@NonNull User user, @NonNull Island oldIsland) {
        // Reserve the old island until the replacement has been built. The event
        // still gives addons a chance to veto the reset before any mutation.
        IslandBaseEvent event = IslandEvent.builder().involvedPlayer(user.getUniqueId()).reason(Reason.PRECLEAR).island(oldIsland)
                .oldIsland(oldIsland).location(oldIsland.getCenter()).build();
        return !event.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(event.isCancelled());
    }
}
