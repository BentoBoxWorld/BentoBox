package world.bentobox.bentobox.api.user;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Utilities class that helps to avoid spamming the User with potential repeated messages
 * @author Poslovitch
 */
public class Notifier {

    /**
     * Time in seconds before {@link #notificationCache} removes the entry related to the player.
     */
    private static final int NOTIFICATION_DELAY = 4;

    private final LoadingCache<User, Notification> notificationCache = CacheBuilder.newBuilder()
            .expireAfterAccess(NOTIFICATION_DELAY, TimeUnit.SECONDS)
            .maximumSize(500)
            .build(
                    new CacheLoader<User, Notification>() {
                        @Override
                        public Notification load(User user) {
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

            if (now >= lastNotification.getTime() + (NOTIFICATION_DELAY * 1000) || !message.equals(lastNotification.getMessage())) {
                notificationCache.put(user, new Notification(message, now));
                user.sendRawMessage(message);
                return true;
            }
            return false;
        } catch (ExecutionException e) {
            return false;
        }
    }

    private class Notification {
        private final String message;
        private final long time;

        private Notification(String message, long time) {
            this.message = message;
            this.time = time;
        }

        public String getMessage() {
            return message;
        }

        public long getTime() {
            return time;
        }
    }
}
