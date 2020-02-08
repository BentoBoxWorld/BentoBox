package world.bentobox.bentobox.api.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;

/**
 * BentoBox Delayed Teleport Command
 * Adds ability to require the player stays still for a period of time before a command is executed
 * @author tastybento
 */
public abstract class DelayedTeleportCommand extends CompositeCommand implements Listener {

    /**
     * User monitor map
     */
    private static Map<UUID, DelayedCommand> toBeMonitored = new HashMap<>();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // Only check x,y,z
        if (toBeMonitored.containsKey(uuid) && !e.getTo().toVector().equals(toBeMonitored.get(uuid).getLocation().toVector())) {
            // Player moved
            toBeMonitored.get(uuid).getTask().cancel();
            toBeMonitored.remove(uuid);
            // Player has another outstanding confirmation request that will now be cancelled
            User.getInstance(e.getPlayer()).notify("commands.delay.moved-so-command-cancelled");
        }
    }

    /**
     * Top level command
     * @param addon - addon creating the command
     * @param label - string for this command
     * @param aliases - aliases
     */
    public DelayedTeleportCommand(Addon addon, String label, String... aliases) {
        super(addon, label, aliases);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    /**
     * Command to register a command from an addon under a parent command (that could be from another addon)
     * @param addon - this command's addon
     * @param parent - parent command
     * @param aliases - aliases for this command
     */
    public DelayedTeleportCommand(Addon addon, CompositeCommand parent, String label, String... aliases ) {
        super(addon, parent, label, aliases);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }


    public DelayedTeleportCommand(CompositeCommand parent, String label, String... aliases) {
        super(parent, label, aliases);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    /**
     * Tells user to stand still for a period of time before teleporting
     * @param user User to tell
     * @param message Optional message to send to the user to give them a bit more context. It must already be translated.
     * @param confirmed Runnable to be executed if successfully delayed.
     */
    public void delayCommand(User user, String message, Runnable confirmed) {
        if (getSettings().getDelayTime() < 1 || user.isOp() || user.hasPermission(getPermissionPrefix() + "mod.bypasscooldowns")
                || user.hasPermission(getPermissionPrefix() + "mod.bypassdelays")) {
            Bukkit.getScheduler().runTask(getPlugin(), confirmed);
            return;
        }
        // Check for pending delays
        UUID uuid = user.getUniqueId();
        if (toBeMonitored.containsKey(uuid)) {
            // A double request - clear out the old one
            toBeMonitored.get(uuid).getTask().cancel();
            toBeMonitored.remove(uuid);
            // Player has another outstanding confirmation request that will now be cancelled
            user.sendMessage("commands.delay.previous-command-cancelled");
        }
        // Send user the context message if it is not empty
        if (!message.trim().isEmpty()) {
            user.sendRawMessage(message);
        }
        // Tell user that they need to stand still
        user.sendMessage("commands.delay.stand-still", "[seconds]", String.valueOf(getSettings().getDelayTime()));
        // Set up the run task
        BukkitTask task = Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            Bukkit.getScheduler().runTask(getPlugin(), toBeMonitored.get(uuid).getRunnable());
            toBeMonitored.remove(uuid);
        }, getPlugin().getSettings().getDelayTime() * 20L);

        // Add to the monitor
        toBeMonitored.put(uuid, new DelayedCommand(confirmed, task, user.getLocation()));
    }

    /**
     * Tells user to stand still for a period of time before teleporting
     * @param user User to monitor.
     * @param command Runnable to be executed if player does not move.
     */
    public void delayCommand(User user, Runnable command) {
        delayCommand(user, "", command);
    }

    /**
     * Holds the data to run once the confirmation is given
     * @author tastybento
     *
     */
    private class DelayedCommand {
        private final Runnable runnable;
        private final BukkitTask task;
        private final Location location;

        /**
         * @param runnable - runnable to run when confirmed
         * @param task - task ID to cancel when confirmed
         * @param location - location
         */
        DelayedCommand(Runnable runnable, BukkitTask task, Location location) {
            this.runnable = runnable;
            this.task = task;
            this.location = location;
        }
        /**
         * @return the runnable
         */
        public Runnable getRunnable() {
            return runnable;
        }
        /**
         * @return the task
         */
        public BukkitTask getTask() {
            return task;
        }
        /**
         * @return the location
         */
        public Location getLocation() {
            return location;
        }
    }

}
