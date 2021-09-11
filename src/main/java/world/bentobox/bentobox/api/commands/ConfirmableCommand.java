package world.bentobox.bentobox.api.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;

/**
 * BentoBox Confirmable Command
 * Adds ability to confirm a command before execution
 * @author tastybento
 * @author Poslovitch
 */
public abstract class ConfirmableCommand extends CompositeCommand {

    /**
     * Confirmation tracker
     */
    private static final Map<User, Confirmer> toBeConfirmed = new HashMap<>();

    /**
     * Top level command
     * @param addon - addon creating the command
     * @param label - string for this command
     * @param aliases - aliases
     */
    protected ConfirmableCommand(Addon addon, String label, String... aliases) {
        super(addon, label, aliases);
    }

    /**
     * Command to register a command from an addon under a parent command (that could be from another addon)
     * @param addon - this command's addon
     * @param parent - parent command
     * @param aliases - aliases for this command
     */
    protected ConfirmableCommand(Addon addon, CompositeCommand parent, String label, String... aliases ) {
        super(addon, parent, label, aliases);
    }

    /**
     *
     * @param parent - parent command
     * @param label - command label
     * @param aliases - command aliases
     */
    protected ConfirmableCommand(CompositeCommand parent, String label, String... aliases) {
        super(parent, label, aliases);
    }

    /**
     * Tells user to confirm command by retyping it.
     * @param user User to ask confirmation to.
     * @param message Optional message to send to the user to give them a bit more context. It must already be translated.
     * @param confirmed Runnable to be executed if successfully confirmed.
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
     * Tells user to confirm command by retyping it.
     * @param user User to ask confirmation to.
     * @param confirmed Runnable to be executed if successfully confirmed.
     */
    public void askConfirmation(User user, Runnable confirmed) {
        askConfirmation(user, "", confirmed);
    }

    /**
     * Record to hold the data to run once the confirmation is given
     *
     */
    private record Confirmer (String topLabel, String label, Runnable runnable, BukkitTask task) { }

}
