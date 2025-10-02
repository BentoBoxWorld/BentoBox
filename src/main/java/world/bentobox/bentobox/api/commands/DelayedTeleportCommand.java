package world.bentobox.bentobox.api.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.user.User;

/**
 * A command type that requires players to stand still for a configurable period
 * before the command executes. This is typically used for teleport commands to
 * prevent abuse during combat or escape situations.
 * <p>
 * Features:
 * <ul>
 *   <li>Configurable delay time</li>
 *   <li>Movement cancellation</li>
 *   <li>Teleport cancellation</li>
 *   <li>Permission-based bypass ({@code mod.bypassdelays})</li>
 *   <li>OP bypass</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>
 * delayCommand(user, () -> {
 *     // Code to run after delay if player doesn't move
 *     player.teleport(location);
 *     user.sendMessage("teleported");
 * });
 * </pre>
 * 
 * @author tastybento
 * @since 1.0
 */
public abstract class DelayedTeleportCommand extends CompositeCommand implements Listener {

    /**
     * Tracks pending delayed commands.
     * Key: Player UUID
     * Value: DelayedCommand containing the command to run, cancellation task, and original location
     */
    private static final Map<UUID, DelayedCommand> toBeMonitored = new HashMap<>();

    /**
     * Monitors player movement and cancels delayed commands if the player moves.
     * Only checks x,y,z coordinates, not head rotation.
     * 
     * @param e Player move event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // Only check x,y,z
        if (toBeMonitored.containsKey(uuid) && !e.getTo().toVector().equals(toBeMonitored.get(uuid).location().toVector())) {
            moved(uuid);
        }
    }

    /**
     * Handles command cancellation when a player moves.
     * Cancels the scheduled task and notifies the player.
     * 
     * @param uuid UUID of the player who moved
     */
    private void moved(UUID uuid) {
        // Player moved
        toBeMonitored.get(uuid).task().cancel();
        toBeMonitored.remove(uuid);
        // Player has another outstanding confirmation request that will now be cancelled
        User.getInstance(uuid).notify("commands.delay.moved-so-command-cancelled");
    }

    /**
     * Monitors player teleports and cancels delayed commands if the player teleports.
     * This prevents players from using other teleport methods to bypass the delay.
     * 
     * @param e Player teleport event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (toBeMonitored.containsKey(uuid)) {
            moved(uuid);
        }
    }


    /**
     * Top level command
     * @param addon - addon creating the command
     * @param label - string for this command
     * @param aliases - aliases
     */
    protected DelayedTeleportCommand(Addon addon, String label, String... aliases) {
        super(addon, label, aliases);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    /**
     * Command to register a command from an addon under a parent command (that could be from another addon)
     * @param addon - this command's addon
     * @param parent - parent command
     * @param aliases - aliases for this command
     */
    protected DelayedTeleportCommand(Addon addon, CompositeCommand parent, String label, String... aliases ) {
        super(addon, parent, label, aliases);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    /**
     *
     * @param parent - parent command
     * @param label - command label
     * @param aliases - command aliases
     */
    protected DelayedTeleportCommand(CompositeCommand parent, String label, String... aliases) {
        super(parent, label, aliases);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    /**
     * Initiates a delayed command execution that requires the player to stand still.
     * <p>
     * The delay is bypassed if any of these conditions are met:
     * <ul>
     *   <li>The configured delay time is less than 1 second</li>
     *   <li>The player is an operator</li>
     *   <li>The player has the {@code mod.bypasscooldowns} permission</li>
     *   <li>The player has the {@code mod.bypassdelays} permission</li>
     * </ul>
     *
     * @param user      The user who triggered the command
     * @param message   Optional context message (must be pre-translated)
     * @param confirmed The action to execute after the delay
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
            toBeMonitored.get(uuid).task().cancel();
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
            Bukkit.getScheduler().runTask(getPlugin(), toBeMonitored.get(uuid).runnable());
            toBeMonitored.remove(uuid);
        }, getPlugin().getSettings().getDelayTime() * 20L);

        // Add to the monitor
        toBeMonitored.put(uuid, new DelayedCommand(confirmed, task, user.getLocation()));
    }

    /**
     * Convenience method for {@link #delayCommand(User, String, Runnable)} with no message.
     *
     * @param user    The user who triggered the command
     * @param command The action to execute after the delay
     */
    public void delayCommand(User user, Runnable command) {
        delayCommand(user, "", command);
    }

    /**
     * Record that holds the data for a pending delayed command.
     * 
     * @param runnable The action to execute when the delay completes
     * @param task     The cancellation task that will run if the player moves
     * @param location The player's original location for movement comparison
     */
    private record DelayedCommand(Runnable runnable, BukkitTask task, Location location) {}
}
