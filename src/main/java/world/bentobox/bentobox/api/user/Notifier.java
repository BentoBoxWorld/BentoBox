package world.bentobox.bentobox.api.user;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Utilities class that helps to avoid spamming the User with potential repeated messages
 * @author Poslovitch, tastybento
 */
public class Notifier {

    /**
     * Time in seconds before {@link #notificationCache} removes the entry related to the player.
     */
    private static final int NOTIFICATION_DELAY = 4;

    private record Notification(String message, long time) {}

    private final LoadingCache<User, Notification> notificationCache = CacheBuilder.newBuilder()
            .expireAfterAccess(NOTIFICATION_DELAY, TimeUnit.SECONDS)
            .maximumSize(500)
            .build(
                    new CacheLoader<>() {
                        @Override
                        public Notification load(@NonNull User user) {
                            return new Notification(null, 0);
                        }
                    }
                    );

    /**
     * Sends message to a user only if the message hasn't been sent recently
     * @param user - user
     * @param message - message to send (already translated)
     * @return true if message sent successfully, false it it has been throttled
     */
    public synchronized boolean notify(User user, String message) {
        try {
            Notification lastNotification = notificationCache.get(user);
            long now = System.currentTimeMillis();

            if (now >= lastNotification.time() + (NOTIFICATION_DELAY * 1000) || !message.equals(lastNotification.message())) {
                notificationCache.put(user, new Notification(message, now));
                user.sendRawMessage(message);
                return true;
            }
            return false;
        } catch (ExecutionException e) {
            return false;
        }
    }

}
