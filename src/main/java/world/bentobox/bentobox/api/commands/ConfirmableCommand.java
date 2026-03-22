package world.bentobox.bentobox.api.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;

/**
 * An extension of {@link CompositeCommand} that adds a confirmation step for
 * potentially destructive or significant actions.
 * <p>
 * When a command needs confirmation, it calls {@link #askConfirmation(User, Runnable)}
 * or {@link #askConfirmation(User, String, Runnable)}. The user will then be prompted
 * to execute the same command again within a configurable time limit to confirm.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * askConfirmation(user, () -> {
 *     // Code to run after confirmation
 *     island.delete();
 *     user.sendMessage("island.deleted");
 * });
 * }
 * </pre>
 * 
 * @author tastybento
 * @author Poslovitch
 * @since 1.0
 */
public abstract class ConfirmableCommand extends CompositeCommand {

    /**
     * Tracks pending confirmation requests across all confirmable commands.
     * Key: The user who needs to confirm an action
     * Value: A {@link Confirmer} record containing the confirmation details
     */
    private static final Map<User, Confirmer> toBeConfirmed = new HashMap<>();

    /**
     * Creates a top-level confirmable command registered by an addon.
     *
     * @param addon   The addon registering the command.
     * @param label   The primary label for the command.
     * @param aliases Optional aliases for the command.
     */
    protected ConfirmableCommand(Addon addon, String label, String... aliases) {
        super(addon, label, aliases);
    }

    /**
     * Creates a confirmable sub-command registered by an addon, attached to a parent command.
     *
     * @param addon   The addon registering this sub-command.
     * @param parent  The parent command.
     * @param label   The label for this sub-command.
     * @param aliases Optional aliases for this sub-command.
     */
    protected ConfirmableCommand(Addon addon, CompositeCommand parent, String label, String... aliases ) {
        super(addon, parent, label, aliases);
    }

    /**
     * Creates a confirmable sub-command that belongs to a parent command.
     *
     * @param parent  The parent command.
     * @param label   The label for this sub-command.
     * @param aliases Optional aliases for this sub-command.
     */
    protected ConfirmableCommand(CompositeCommand parent, String label, String... aliases) {
        super(parent, label, aliases);
    }

    /**
     * Prompts a user to confirm an action by re-executing the command.
     * <p>
     * This method initiates a confirmation workflow:
     * <ol>
     *   <li>If there's already a pending confirmation for this user:
     *     <ul>
     *       <li>If it's for the same command, execute the confirmed action</li>
     *       <li>If it's for a different command, cancel the previous request</li>
     *     </ul>
     *   </li>
     *   <li>Displays the optional context message if provided</li>
     *   <li>Tells the user to confirm by re-executing the command</li>
     *   <li>Sets up an automatic cancellation task based on the confirmation timeout</li>
     * </ol>
     *
     * @param user      The user to ask for confirmation.
     * @param message   A pre-translated message to provide context to the user.
     * @param confirmed The action to execute upon successful confirmation.
     */
    public void askConfirmation(User user, String message, Runnable confirmed) {
        // Check for pending confirmations
        if (toBeConfirmed.containsKey(user)) {
            if (toBeConfirmed.get(user).topLabel().equals(getTopLabel()) && toBeConfirmed.get(user).label().equalsIgnoreCase(getLabel())) {
                toBeConfirmed.get(user).task().cancel();
                Bukkit.getScheduler().runTask(getPlugin(), toBeConfirmed.get(user).runnable());
                toBeConfirmed.remove(user);
                return;
            } else {
                // Player has another outstanding confirmation request that will now be cancelled
                user.sendMessage("commands.confirmation.previous-request-cancelled");
            }
        }
        // Send user the context message if it is not empty
        if (!message.trim().isEmpty()) {
            user.sendRawMessage(message);
        }
        // Tell user that they need to confirm
        user.sendMessage("commands.confirmation.confirm", "[seconds]", String.valueOf(getSettings().getConfirmationTime()));
        // Set up a cancellation task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            user.sendMessage("commands.confirmation.request-cancelled");
            toBeConfirmed.remove(user);
        }, getPlugin().getSettings().getConfirmationTime() * 20L);

        // Add to the global confirmation map
        toBeConfirmed.put(user, new Confirmer(getTopLabel(), getLabel(), confirmed, task));
    }

    /**
     * A convenience method that calls {@link #askConfirmation(User, String, Runnable)}
     * with an empty message.
     *
     * @param user      The user to ask for confirmation.
     * @param confirmed The action to execute upon successful confirmation.
     */
    public void askConfirmation(User user, Runnable confirmed) {
        askConfirmation(user, "", confirmed);
    }

    /**
     * A record that holds the details of a pending confirmation request.
     * 
     * @param topLabel The top-level command label (e.g., "island" in "/island delete")
     * @param label    The specific command label (e.g., "delete" in "/island delete")
     * @param runnable The action to execute when confirmed
     * @param task    The cancellation task that will run if confirmation times out
     */
    private record Confirmer(String topLabel, String label, Runnable runnable, BukkitTask task) { }
}
