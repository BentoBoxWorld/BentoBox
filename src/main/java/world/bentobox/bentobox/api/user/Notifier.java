package world.bentobox.bentobox.api.user;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Utilities class that helps to avoid spamming the User with potential repeated messages
 * @author Poslovitch, tastybento
 */
public class Notifier {

    /**
     * Time in seconds before a sent message may be sent to the same user again.
     */
    private static final int NOTIFICATION_DELAY = 4;

    private record Notification(User user, String message) {}

    /**
     * Recently sent notifications, keyed by user and message so that each distinct
     * message is throttled independently. A user alternating between two different
     * messages (e.g. hitting two different limits back-to-back) is throttled on both.
     */
    private final Cache<Notification, Boolean> notificationCache = CacheBuilder.newBuilder()
            .expireAfterWrite(NOTIFICATION_DELAY, TimeUnit.SECONDS)
            .maximumSize(2000)
            .build();

    /**
     * Sends a message to a user unless the same message was sent to them within the last
     * {@code NOTIFICATION_DELAY} seconds. Different messages are throttled independently.
     * @param user - user
     * @param message - message to send (already translated)
     * @return true if message sent successfully, false if it is being throttled
     */
    public synchronized boolean notify(User user, String message) {
        Notification notification = new Notification(user, message);
        if (notificationCache.getIfPresent(notification) != null) {
            return false;
        }
        notificationCache.put(notification, Boolean.TRUE);
        user.sendRawMessage(message);
        return true;
    }

}
