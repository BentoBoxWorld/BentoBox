package world.bentobox.bentobox.suggestions;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;

/**
 * Wires the "did you mean?" suggestion engine into the server:
 * <ul>
 * <li>replaces the vanilla "Unknown command" message with a clickable
 * suggestion when a player types a command no plugin owns (e.g. {@code /teams}
 * instead of {@code /oneblock team});</li>
 * <li>lets the player accept the last suggestion by typing {@code yes} in
 * chat;</li>
 * <li>drops a pending suggestion once the player runs any other command or
 * leaves.</li>
 * </ul>
 *
 * @author tastybento
 * @since 3.20.0
 */
public class DidYouMeanListener implements Listener {

    private final BentoBox plugin;

    public DidYouMeanListener(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Fires only when no plugin owns the typed command, so BentoBox never
     * shadows another plugin's command here.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onUnknownCommand(UnknownCommandEvent e) {
        if (!plugin.getSettings().isDidYouMeanUnknownCommands() || !(e.getSender() instanceof Player player)) {
            return;
        }
        User user = User.getInstance(player);
        if (plugin.getSuggestionsManager().suggestCommand(user, e.getCommandLine())) {
            // Our suggestion replaces the vanilla "Unknown command" message
            e.message(null);
        }
    }

    /**
     * A player answering "yes" (or "y") in chat accepts their pending
     * suggestion. Anything else is left alone.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        String plain = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        if ((plain.equalsIgnoreCase("yes") || plain.equalsIgnoreCase("y"))
                && acceptPending(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    /**
     * Consumes the pending suggestion, if any, and schedules the suggested
     * command on the main thread (chat events are async).
     *
     * @param uuid the player's UUID
     * @return true if a pending suggestion was accepted
     */
    boolean acceptPending(UUID uuid) {
        return plugin.getSuggestionsManager().acceptPending(uuid).map(command -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.performCommand(command.substring(1));
                }
            });
            return true;
        }).orElse(false);
    }

    /**
     * The player has moved on to some other command; whatever was suggested is
     * stale now. This also covers the player clicking the suggestion itself.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
        plugin.getSuggestionsManager().clearPending(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        plugin.getSuggestionsManager().clearPending(e.getPlayer().getUniqueId());
    }
}
