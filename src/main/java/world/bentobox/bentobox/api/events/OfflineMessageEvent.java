package world.bentobox.bentobox.api.events;

import java.util.UUID;

/**
 * Fired when a message is going to an offline player
 *
 * @author tastybento
 * @since 1.5.0
 */
public class OfflineMessageEvent extends BentoBoxEvent {
    private final UUID offlinePlayer;
    private final String message;

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
