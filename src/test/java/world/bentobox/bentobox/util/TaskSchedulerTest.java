package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;

class TaskSchedulerTest extends CommonTestSetup {
    @Test
    void globalFutureWaitsForActionAndPropagatesItsFailure() {
        var action = new java.util.concurrent.atomic.AtomicReference<Runnable>();
        try (var scheduler = mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(() -> TaskScheduler.global(Mockito.eq(plugin), any())).thenAnswer(call -> {
                action.set(call.getArgument(1));
                return mock(org.bukkit.scheduler.BukkitTask.class);
            });
            var future = TaskScheduler.globalFuture(plugin, () -> { throw new IllegalStateException("settlement failed"); });
            org.junit.jupiter.api.Assertions.assertFalse(future.isDone());
            action.get().run();
            assertTrue(future.isCompletedExceptionally());
        }
    }

    @Test
    void rejectedGlobalTaskCompletesFutureExceptionally() {
        try (var scheduler = mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(() -> TaskScheduler.global(Mockito.eq(plugin), any()))
                    .thenThrow(new IllegalStateException("plugin disabled"));
            assertTrue(TaskScheduler.globalFuture(plugin, () -> {}).isCompletedExceptionally());
        }
    }

    @Test
    void disconnectedPlayerCompletesFutureExceptionally() {
        when(mockPlayer.getScheduler()).thenReturn(mock(EntityScheduler.class));
        try (var scheduler = mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(TaskScheduler::isFolia).thenReturn(true);
            var future = TaskScheduler.userFuture(plugin, User.getInstance(mockPlayer), () -> {
                throw new AssertionError("Disconnected player must not execute work");
            });
            assertTrue(future.isCompletedExceptionally());
            assertThrows(CompletionException.class, future::join);
        }
    }

    @Test
    void playerWorkExceptionCompletesFutureExceptionally() {
        var entityScheduler = mock(EntityScheduler.class);
        when(mockPlayer.getScheduler()).thenReturn(entityScheduler);
        when(entityScheduler.execute(any(), any(), any(), anyLong())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return true;
        });
        try (var scheduler = mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(TaskScheduler::isFolia).thenReturn(true);
            var future = TaskScheduler.userFuture(plugin, User.getInstance(mockPlayer), () -> {
                throw new IllegalStateException("test");
            });
            assertThrows(CompletionException.class, future::join);
        }
    }

    @Test
    void retiredEntityRunsFallbackInsteadOfDroppingTheTask() {
        when(mockPlayer.getScheduler()).thenReturn(mock(EntityScheduler.class));
        var retired = new java.util.concurrent.atomic.AtomicBoolean();
        try (var scheduler = mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(TaskScheduler::isFolia).thenReturn(true);
            TaskScheduler.entityOrRetired(plugin, mockPlayer,
                    () -> { throw new AssertionError("Retired entity must not run the action"); },
                    () -> retired.set(true));
            assertTrue(retired.get());
        }
    }

    @Test
    void liveEntityRunsTheActionNotTheFallback() {
        var entityScheduler = mock(EntityScheduler.class);
        when(mockPlayer.getScheduler()).thenReturn(entityScheduler);
        when(entityScheduler.execute(any(), any(), any(), anyLong())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return true;
        });
        var ran = new java.util.concurrent.atomic.AtomicBoolean();
        try (var scheduler = mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(TaskScheduler::isFolia).thenReturn(true);
            TaskScheduler.entityOrRetired(plugin, mockPlayer, () -> ran.set(true),
                    () -> { throw new AssertionError("Live entity must not run the fallback"); });
            assertTrue(ran.get());
        }
    }

    @Test
    void failingActionRunsTheFallbackSoWaitersAreReleased() {
        var entityScheduler = mock(EntityScheduler.class);
        when(mockPlayer.getScheduler()).thenReturn(entityScheduler);
        when(entityScheduler.execute(any(), any(), any(), anyLong())).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return true;
        });
        var retired = new java.util.concurrent.atomic.AtomicBoolean();
        try (var scheduler = mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(TaskScheduler::isFolia).thenReturn(true);
            TaskScheduler.entityOrRetired(plugin, mockPlayer,
                    () -> { throw new IllegalStateException("teleport failed"); },
                    () -> retired.set(true));
            assertTrue(retired.get());
        }
    }
}
