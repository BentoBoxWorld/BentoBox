package world.bentobox.bentobox.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import world.bentobox.bentobox.api.user.User;

/** Routes work by ownership; global tasks must never access blocks or entities. */
public final class TaskScheduler {
    private static final boolean REGION_THREADED = detectFolia();

    private TaskScheduler() { }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isFolia() {
        return REGION_THREADED;
    }

    public static BukkitTask async(Plugin plugin, Runnable action) {
        return isFolia() ? wrap(Bukkit.getAsyncScheduler().runNow(plugin, task -> action.run()), false)
                : Bukkit.getScheduler().runTaskAsynchronously(plugin, action);
    }

    public static BukkitTask asyncLater(Plugin plugin, Runnable action, long delay) {
        return isFolia() ? wrap(Bukkit.getAsyncScheduler().runDelayed(plugin, task -> action.run(),
                Math.max(1, delay) * 50, TimeUnit.MILLISECONDS), false)
                : Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, action, delay);
    }

    public static BukkitTask asyncTimer(Plugin plugin, Runnable action, long delay, long period) {
        return isFolia() ? wrap(Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> action.run(),
                Math.max(1, delay) * 50, period * 50, TimeUnit.MILLISECONDS), false)
                : Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, action, delay, period);
    }

    public static BukkitTask global(Plugin plugin, Runnable action) {
        return isFolia() ? wrap(Bukkit.getGlobalRegionScheduler().run(plugin, task -> action.run()), true)
                : Bukkit.getScheduler().runTask(plugin, action);
    }

    /** Completes after global work runs, including scheduling and callback failures. */
    public static CompletableFuture<Void> globalFuture(Plugin plugin, Runnable action) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        try {
            global(plugin, () -> {
                try {
                    action.run();
                    result.complete(null);
                } catch (Throwable error) {
                    result.completeExceptionally(error);
                }
            });
        } catch (Throwable error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    public static BukkitTask globalTimer(Plugin plugin, Runnable action, long delay, long period) {
        return isFolia() ? wrap(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> action.run(),
                Math.max(1, delay), period), true)
                : Bukkit.getScheduler().runTaskTimer(plugin, action, delay, period);
    }

    public static BukkitTask globalLater(Plugin plugin, Runnable action, long delay) {
        return isFolia() ? wrap(Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> action.run(),
                Math.max(1, delay)), true) : Bukkit.getScheduler().runTaskLater(plugin, action, delay);
    }

    public static BukkitTask user(Plugin plugin, User user, Runnable action) {
        return user.isPlayer() ? entity(plugin, user.getPlayer(), action) : global(plugin, action);
    }

    public static BukkitTask userLater(Plugin plugin, User user, Runnable action, long delay) {
        return user.isPlayer() ? entityLater(plugin, user.getPlayer(), action, delay)
                : globalLater(plugin, action, delay);
    }

    /** Unlike fire-and-forget entity work, a retired player must also complete waiting callers. */
    public static CompletableFuture<Void> userFuture(Plugin plugin, User user, Runnable action) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Runnable work = () -> {
            try {
                action.run();
                result.complete(null);
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        };
        try {
            if (isFolia() && user.isPlayer()) {
                if (Bukkit.isOwnedByCurrentRegion(user.getPlayer())) {
                    work.run();
                    return result;
                }
                Runnable retired = () -> result.completeExceptionally(new IllegalStateException("Player disconnected"));
                if (!user.getPlayer().getScheduler().execute(plugin, work, retired, 1)) retired.run();
            } else user(plugin, user, work);
        } catch (Throwable error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    /** A retired entity has no owner thread; its pending action is discarded. */
    public static BukkitTask entity(Plugin plugin, Entity entity, Runnable action) {
        return isFolia() ? entityLater(plugin, entity, action, 1)
                : Bukkit.getScheduler().runTask(plugin, action);
    }

    public static BukkitTask entityLater(Plugin plugin, Entity entity, Runnable action, long delay) {
        return isFolia() ? wrap(entity.getScheduler().runDelayed(plugin, task -> action.run(), null,
                Math.max(1, delay)), true)
                : Bukkit.getScheduler().runTaskLater(plugin, action, delay);
    }

    public static BukkitTask entityTimer(Plugin plugin, Entity entity, Runnable action, long delay, long period) {
        return isFolia() ? wrap(entity.getScheduler().runAtFixedRate(plugin, task -> action.run(), null,
                Math.max(1, delay), period), true)
                : Bukkit.getScheduler().runTaskTimer(plugin, action, delay, period);
    }

    public static BukkitTask region(Plugin plugin, Location location, Runnable action) {
        return isFolia() ? wrap(Bukkit.getRegionScheduler().run(plugin, location, task -> action.run()), true)
                : Bukkit.getScheduler().runTask(plugin, action);
    }

    public static BukkitTask regionLater(Plugin plugin, Location location, Runnable action, long delay) {
        return isFolia() ? wrap(Bukkit.getRegionScheduler().runDelayed(plugin, location,
                task -> action.run(), Math.max(1, delay)), true)
                : Bukkit.getScheduler().runTaskLater(plugin, action, delay);
    }

    /** Completes only after the target region has finished the operation. */
    public static CompletableFuture<Void> regionFuture(Plugin plugin, Location location, Runnable action) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Runnable work = () -> {
            try {
                action.run();
                result.complete(null);
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        };
        try {
            if ((!isFolia() && Bukkit.isPrimaryThread())
                    || (isFolia() && Bukkit.isOwnedByCurrentRegion(location))) {
                work.run();
            } else {
                region(plugin, location, work);
            }
        } catch (Throwable error) {
            result.completeExceptionally(error);
        }
        return result;
    }

    // Keep existing internal cancellation handles while Folia has no numeric Bukkit task IDs.
    private static BukkitTask wrap(ScheduledTask task, boolean sync) {
        if (task == null) return null;
        return new BukkitTask() {
            @Override public int getTaskId() { throw new UnsupportedOperationException("Folia tasks have no Bukkit ID"); }
            @Override public Plugin getOwner() { return task.getOwningPlugin(); }
            @Override public boolean isSync() { return sync; }
            @Override public boolean isCancelled() { return task.isCancelled(); }
            @Override public void cancel() { task.cancel(); }
        };
    }
}
