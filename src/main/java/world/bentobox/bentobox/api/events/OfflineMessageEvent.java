package world.bentobox.bentobox.api.events;

import java.util.UUID;

import org.bukkit.event.HandlerList;
import org.eclipse.jdt.annotation.NonNull;

/**
 * Fired when a message is going to an offline player
 *
 * @author tastybento
 * @since 1.5.0
 */
public class OfflineMessageEvent extends BentoBoxEvent {
    private final UUID offlinePlayer;
    private final String message;
    private static final HandlerList handlers = new HandlerList();

    @Override
    public @NonNull HandlerList getHandlers() {
        return getHandlerList();
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @param offlinePlayer - offline player
     * @param message message to send offline player
     */
    public OfflineMessageEvent(UUID offlinePlayer, String message) {
        this.offlinePlayer = offlinePlayer;
        this.message = message;
    }

    /**
     * @return the offlinePlayer
     */
    public UUID getOfflinePlayer() {
        return offlinePlayer;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

}
