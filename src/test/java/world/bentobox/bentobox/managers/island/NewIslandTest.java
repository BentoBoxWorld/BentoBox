package world.bentobox.bentobox.managers.island;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandEventBuilder;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.TaskScheduler;

/**
 * @author tastybento
 *
 */
class NewIslandTest extends CommonTestSetup {

    private static final String NAME = "name";

    @org.junit.jupiter.api.Test
    void testPreparationAccessGuardPrecedesOwnerRegistrationAndReleasesBeforeTeleport() {
        var baseline = new java.util.concurrent.CompletableFuture<Void>();
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(bpm.completePasteAsync(addon, island, NAME)).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(iwm.getIslandDistance(world)).thenReturn(100);
        try (var creation = Mockito.mockStatic(NewIsland.class, Mockito.CALLS_REAL_METHODS);
                var handlers = Mockito.mockStatic(org.bukkit.event.HandlerList.class);
                var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            creation.when(() -> NewIsland.initializeLevelCount(plugin, island)).thenReturn(baseline);
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.CREATE).buildAsync();
            var access = org.mockito.ArgumentCaptor.forClass(org.bukkit.event.Listener.class);
            var order = Mockito.inOrder(pim, im);
            order.verify(pim).registerEvents(access.capture(), eq(plugin));
            order.verify(im).createIsland(any(), eq(uuid));
            // The guard cancels movement into the new island, so it must be released before
            // delivery. Waiting for the level scan would block the teleport itself.
            handlers.verify(() -> org.bukkit.event.HandlerList.unregisterAll(access.getValue()));
            org.junit.jupiter.api.Assertions.assertFalse(result.isDone());
            baseline.completeExceptionally(new IllegalStateException("baseline failed"));
            // The island was already handed over, so a failed scan is only logged, never rolled back.
            org.junit.jupiter.api.Assertions.assertTrue(result.isDone());
            org.junit.jupiter.api.Assertions.assertFalse(result.isCompletedExceptionally());
            verify(im, never()).deleteIsland(eq(island), anyBoolean(), any());
        }
    }

    @Test
    void testVetoedDraftDeletionOrphansTheDraftInsteadOfLeavingItOwned() {
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(iwm.getIslandDistance(world)).thenReturn(100);
        // A listener vetoed the delete: deleteIsland leaves the owner in place.
        when(island.isOwned()).thenReturn(true);
        try (var creation = Mockito.mockStatic(NewIsland.class, Mockito.CALLS_REAL_METHODS);
                var handlers = Mockito.mockStatic(org.bukkit.event.HandlerList.class);
                var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            creation.when(() -> NewIsland.initializeLevelCount(plugin, island)).thenReturn(
                    java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("baseline failed")));
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.CREATE).buildAsync();
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            verify(im).deleteIsland(island, true, uuid);
            verify(im).orphanIsland(island);
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = Reason.class, names = {"CREATE", "RESET"})
    void testRejectedTeleportDoesNotDeliverSettleOrDeleteRecoveryIslands(Reason reason) {
        when(builder.build()).thenReturn(reason == Reason.RESET ? ire : ice);
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(bpm.completePasteAsync(addon, island, NAME)).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(iwm.isTeleportPlayerToIslandUponIslandCreation(world)).thenReturn(true);
        when(user.getPlayer()).thenReturn(mockPlayer);
        // IslandsManager converts the underlying Boolean false into an exceptional Void future.
        when(im.homeTeleportAsync(island, user, true)).thenReturn(
                java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Home teleport was rejected")));
        var settled = new java.util.concurrent.atomic.AtomicBoolean();
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var creation = NewIsland.builder().player(user).addon(addon).name(NAME).reason(reason)
                    .afterBuild(created -> {
                        settled.set(true);
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    });
            if (reason == Reason.RESET) creation.oldIsland(oldIsland);
            var result = creation.buildAsync();
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            org.junit.jupiter.api.Assertions.assertFalse(settled.get());
            // The teleport never happened, so the player is still standing on the old island.
            // Keep it and drop the draft instead of deleting the island under their feet.
            verify(im).deleteIsland(eq(island), anyBoolean(), any());
            verify(im, never()).deleteIsland(eq(oldIsland), anyBoolean(), any());
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
        }
    }

    @Test
    void testBlueprintCommandCancellationAfterCommitKeepsTheOldIsland() {
        when(builder.build()).thenReturn(ire);
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        when(bpm.completePasteAsync(addon, island, NAME)).thenReturn(
                java.util.concurrent.CompletableFuture.failedFuture(
                        new java.util.concurrent.CancellationException("commands cancelled after commit")));
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.RESET)
                    .oldIsland(oldIsland).buildAsync();
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            verify(im).setPrimaryIsland(uuid, island);
            // The cancellation happened before delivery, so the player is still on the old
            // island: drop the draft and keep the island they are standing on.
            verify(im).deleteIsland(eq(island), anyBoolean(), any());
            verify(im, never()).deleteIsland(eq(oldIsland), anyBoolean(), any());
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
        }
    }

    @Test
    void testCommitVetoDiscardsDraftWithoutDeletingOldIsland() {
        var settled = new java.util.concurrent.atomic.AtomicBoolean();
        when(builder.build()).thenReturn(ire);
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(
                java.util.concurrent.CompletableFuture.completedFuture(null));
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.RESET)
                    .oldIsland(oldIsland).beforeTeleport(() -> java.util.concurrent.CompletableFuture.failedFuture(
                            new java.util.concurrent.CancellationException("preclear veto")))
                    .afterBuild(created -> {
                        settled.set(true);
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    }).buildAsync();
            org.junit.jupiter.api.Assertions.assertNull(result.join());
            org.junit.jupiter.api.Assertions.assertFalse(settled.get());
            verify(im).deleteIsland(island, true, uuid);
            verify(im, never()).deleteIsland(eq(oldIsland), anyBoolean(), any());
            verify(im, never()).setPrimaryIsland(any(), any());
            verify(pm, never()).setDeaths(any(), any(), anyInt());
            verify(bpm, never()).completePasteAsync(any(), any(), any());
        }
    }

    @Test
    void testFailedPasteWaitsForDraftCleanupBeforeReleasingCreationLock() {
        var paste = new java.util.concurrent.CompletableFuture<Void>();
        var discard = new java.util.concurrent.CompletableFuture<Void>();
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(paste);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenReturn(discard);
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.CREATE).buildAsync();
            paste.completeExceptionally(new IllegalStateException("paste failed"));
            org.junit.jupiter.api.Assertions.assertFalse(result.isDone());
            org.junit.jupiter.api.Assertions.assertTrue(NewIsland.isCreating(user));
            discard.complete(null);
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
        }
    }

    @Test
    void testFailedReservedIslandPasteRestoresReservation() {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(
                java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("paste failed")));
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.CREATE).buildAsync();
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            verify(island).setReserved(false);
            verify(island).setReserved(true);
            verify(im, never()).deleteIsland(any(), anyBoolean(), any());
        }
    }

    @Test
    void testCancelledAsyncResetDoesNotStartPasteOrPlayerCleanup() {
        when(builder.build()).thenReturn(ice, ire);
        when(ire.isCancelled()).thenReturn(true);
        java.util.function.Supplier<java.util.concurrent.CompletableFuture<Void>> prepare = mock(java.util.function.Supplier.class);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.RESET)
                    .oldIsland(oldIsland).beforeTeleport(prepare).buildAsync();
            org.junit.jupiter.api.Assertions.assertNull(result.join());
            verify(prepare, never()).get();
            verify(bpm, never()).pasteTerrainAsync(any(), any(), any(), anyBoolean());
            verify(pm, never()).setDeaths(any(), any(), anyInt());
            verify(im, never()).setPrimaryIsland(any(), any());
            verify(im).deleteIsland(island, true, uuid);
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
        }
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void testAsyncResetWaitsForPasteAndLeaveCleanupBeforeTeleport(boolean settlementSucceeds) {
        var paste = new java.util.concurrent.CompletableFuture<Void>();
        var leaving = new java.util.concurrent.CompletableFuture<Void>();
        var teleport = new java.util.concurrent.CompletableFuture<Void>();
        var settlement = new java.util.concurrent.CompletableFuture<Void>();
        var settlements = new java.util.concurrent.atomic.AtomicInteger();
        var prepareCalls = new java.util.concurrent.atomic.AtomicInteger();
        when(builder.build()).thenReturn(ire);
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(paste);
        when(bpm.completePasteAsync(addon, island, NAME)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(im.homeTeleportAsync(island, user, true)).thenReturn(teleport);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                try {
                    call.getArgument(2, Runnable.class).run();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                } catch (Throwable error) {
                    return java.util.concurrent.CompletableFuture.failedFuture(error);
                }
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                try {
                    call.getArgument(2, Runnable.class).run();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                } catch (Throwable error) {
                    return java.util.concurrent.CompletableFuture.failedFuture(error);
                }
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.RESET)
                    .oldIsland(oldIsland).beforeTeleport(() -> {
                        prepareCalls.incrementAndGet();
                        return leaving;
                    }).afterBuild(created -> {
                        assertEquals(island, created);
                        org.junit.jupiter.api.Assertions.assertTrue(NewIsland.isCreating(user));
                        settlements.incrementAndGet();
                        return settlement;
                    }).buildAsync();
            assertEquals(0, prepareCalls.get());
            verify(pm, never()).setDeaths(any(), any(), anyInt());
            verify(im, never()).setPrimaryIsland(any(), any());
            paste.complete(null);
            assertEquals(1, prepareCalls.get());
            verify(bpm, never()).completePasteAsync(any(), any(), any());
            verify(im, never()).homeTeleportAsync(any(Island.class), any(User.class), anyBoolean());
            leaving.complete(null);
            verify(bpm).completePasteAsync(addon, island, NAME);
            verify(pm).setDeaths(world, uuid, 0);
            verify(im).homeTeleportAsync(island, user, true);
            org.junit.jupiter.api.Assertions.assertFalse(result.isDone());
            teleport.complete(null);
            org.junit.jupiter.api.Assertions.assertFalse(result.isDone());
            org.junit.jupiter.api.Assertions.assertTrue(NewIsland.isCreating(user));
            assertEquals(1, settlements.get());
            if (settlementSucceeds) {
                settlement.complete(null);
                assertEquals(island, result.join());
            } else {
                settlement.completeExceptionally(new IllegalStateException("settlement failed"));
                org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
                verify(im, never()).deleteIsland(eq(island), anyBoolean(), any());
            }
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
            verify(im).deleteIsland(oldIsland, true, uuid);
        }
    }

    @Test
    void testFailedTeleportKeepsTheOldIslandAndDropsTheDraft() {
        var paste = new java.util.concurrent.CompletableFuture<Void>();
        var leaving = new java.util.concurrent.CompletableFuture<Void>();
        var teleport = new java.util.concurrent.CompletableFuture<Void>();
        when(builder.build()).thenReturn(ire);
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(paste);
        when(bpm.completePasteAsync(addon, island, NAME)).thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(im.homeTeleportAsync(island, user, true)).thenReturn(teleport);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                try {
                    call.getArgument(2, Runnable.class).run();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                } catch (Throwable error) {
                    return java.util.concurrent.CompletableFuture.failedFuture(error);
                }
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.RESET)
                    .oldIsland(oldIsland).beforeTeleport(() -> leaving).buildAsync();
            paste.complete(null);
            leaving.complete(null);
            teleport.completeExceptionally(new IllegalStateException("Home teleport was rejected"));
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            // The teleport failed, so the player is still on the old island: keep it and drop
            // the draft rather than deleting the island they are standing on.
            verify(im).deleteIsland(eq(island), anyBoolean(), any());
            verify(im, never()).deleteIsland(eq(oldIsland), anyBoolean(), any());
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
        }
    }

    @Test
    void testFailedPasteNeverStartsOldIslandCleanup() {
        var paste = new java.util.concurrent.CompletableFuture<Void>();
        var prepare = mock(java.util.function.Supplier.class);
        when(builder.build()).thenReturn(ire);
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(paste);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.RESET)
                    .oldIsland(oldIsland).beforeTeleport(prepare).buildAsync();
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            paste.completeExceptionally(new IllegalStateException("paste failed"));
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            verify(prepare, never()).get();
            verify(pm, never()).setDeaths(any(), any(), anyInt());
            verify(im, never()).setPrimaryIsland(any(), any());
            verify(im, never()).deleteIsland(eq(oldIsland), anyBoolean(), any());
            verify(im).deleteIsland(island, true, uuid);
        }
    }

    @Test
    void testAsyncBuildWaitsForPasteAndPropagatesFailure() {
        var paste = new java.util.concurrent.CompletableFuture<Void>();
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(paste);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(true);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                try {
                    call.getArgument(2, Runnable.class).run();
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                } catch (Throwable error) {
                    return java.util.concurrent.CompletableFuture.failedFuture(error);
                }
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.CREATE).buildAsync();
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            org.junit.jupiter.api.Assertions.assertFalse(result.isDone());
            org.junit.jupiter.api.Assertions.assertTrue(NewIsland.isCreating(user));
            paste.completeExceptionally(new IllegalStateException("Paste failed"));
            org.junit.jupiter.api.Assertions.assertTrue(result.isCompletedExceptionally());
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
            verify(im).deleteIsland(island, true, uuid);
        }
    }

    @Test
    void testTransactionalOrdinaryServerWaitsForPasteAndMarksSafeRollback() {
        var paste = new java.util.concurrent.CompletableFuture<Void>();
        when(bpm.pasteTerrainAsync(addon, island, NAME, true)).thenReturn(paste);
        try (var tasks = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            tasks.when(TaskScheduler::isFolia).thenReturn(false);
            tasks.when(() -> TaskScheduler.userFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            tasks.when(() -> TaskScheduler.regionFuture(any(), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return java.util.concurrent.CompletableFuture.completedFuture(null);
            });
            var result = NewIsland.builder().player(user).addon(addon).name(NAME).reason(Reason.CREATE)
                    .transactional().buildAsync();
            org.junit.jupiter.api.Assertions.assertFalse(result.isDone());
            paste.completeExceptionally(new IllegalStateException("paste failed"));
            var failure = org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.CompletionException.class, result::join);
            org.junit.jupiter.api.Assertions.assertInstanceOf(NewIsland.RolledBackException.class, failure);
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
            verify(pm, never()).setDeaths(any(), any(), anyInt());
        }
    }

    @Test
    void testAsyncSelectionRejectsDuplicateAndReleasesAfterFailure() {
        var pending = new java.util.concurrent.CompletableFuture<Location>();
        var strategy = mock(NewIslandLocationStrategy.class);
        when(strategy.getNextLocationAsync(any(), any())).thenReturn(pending);
        when(im.getIsland(any(org.bukkit.World.class), any(User.class))).thenReturn(null);
        try (var scheduler = Mockito.mockStatic(TaskScheduler.class, Mockito.CALLS_REAL_METHODS)) {
            scheduler.when(TaskScheduler::isFolia).thenReturn(true);
            var builder = NewIsland.builder().player(user).addon(addon).locationStrategy(strategy).reason(Reason.CREATE);
            var first = builder.buildAsync();
            org.junit.jupiter.api.Assertions.assertTrue(NewIsland.isCreating(user));
            var duplicate = builder.buildAsync();
            org.junit.jupiter.api.Assertions.assertTrue(duplicate.isCompletedExceptionally());
            // A refusal before allocation must be a safe rollback: the caller already debited.
            var refusal = org.junit.jupiter.api.Assertions.assertThrows(
                    java.util.concurrent.CompletionException.class, duplicate::join);
            org.junit.jupiter.api.Assertions.assertInstanceOf(NewIsland.RolledBackException.class, refusal);
            pending.completeExceptionally(new IllegalStateException("test selection failure"));
            org.junit.jupiter.api.Assertions.assertTrue(first.isCompletedExceptionally());
            org.junit.jupiter.api.Assertions.assertFalse(NewIsland.isCreating(user));
            verify(strategy, times(1)).getNextLocationAsync(any(), any());
        }
    }
    @Mock
    private GameModeAddon addon;
    @Mock
    private User user;
    @Mock
    private Island oldIsland;
    @Mock
    private PlayersManager pm;
    @Mock
    private IslandCreateEvent ice;
    @Mock
    private IslandResetEvent ire;
    @Mock
    private IslandDeletionManager idm;
    @Mock
    private Block block;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private IslandEventBuilder builder;
    @Mock
    private BlueprintBundle bpb;

    private final UUID uuid = UUID.randomUUID();
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private @NonNull Location location2;
    private MockedStatic<IslandsManager> mockedIslandsManager;

    /**
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        var addons = mock(world.bentobox.bentobox.managers.AddonsManager.class);
        when(plugin.getAddonsManager()).thenReturn(addons);
        // Islands manager
        mockedIslandsManager = Mockito.mockStatic(IslandsManager.class);
        when(im.createIsland(any(), any())).thenReturn(island);
        when(im.getLast(any())).thenReturn(location);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);
        when(island.isReserved()).thenReturn(true);
        // Player's manager
        when(plugin.getPlayers()).thenReturn(pm);
        // IWM
        Optional<GameModeAddon> optionalAddon = Optional.of(addon);
        when(iwm.getAddon(any())).thenReturn(optionalAddon);
        when(iwm.isDeathsResetOnNewIsland(any())).thenReturn(true);
        // Island deletion manager
        when(plugin.getIslandDeletionManager()).thenReturn(idm);
        when(idm.inDeletion(any())).thenReturn(false);
        // blueprints Manager
        when(bpb.getUniqueId()).thenReturn(NAME);
        when(bpm.getBlueprintBundles(any())).thenReturn(Collections.singletonMap(NAME, bpb));
        when(plugin.getBlueprintsManager()).thenReturn(bpm);

        // User
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(20);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);

        // Events
        MockedStatic<IslandEvent> mockedIslandEvent = Mockito.mockStatic(IslandEvent.class);
        mockedIslandEvent.when(IslandEvent::builder).thenReturn(builder);
        when(builder.admin(anyBoolean())).thenReturn(builder);
        when(builder.blueprintBundle(any())).thenReturn(builder);
        when(builder.deletedIslandInfo(any())).thenReturn(builder);
        when(builder.involvedPlayer(any())).thenReturn(builder);
        when(builder.island(any())).thenReturn(builder);
        when(builder.location(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.build()).thenReturn(ice);
        when(ice.getBlueprintBundle()).thenReturn(bpb);
        when(ire.getBlueprintBundle()).thenReturn(bpb);

        // Location and blocks
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(location2);
        when(location.getWorld()).thenReturn(world);
        when(world.getMaxHeight()).thenReturn(5);
        when(location.getBlock()).thenReturn(block);
        when(location.distance(any())).thenReturn(320D);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(oldIsland.getWorld()).thenReturn(world);
        when(oldIsland.getOwner()).thenReturn(uuid);
        when(oldIsland.getMemberSet()).thenReturn(com.google.common.collect.ImmutableSet.of(uuid));
        when(pm.cleanLeavingPlayerAsync(any(), any(), anyBoolean(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // Util - return the same location
        mockedUtil.when(() -> Util.getClosestIsland(any()))
                .thenAnswer((Answer<Location>) invocation -> invocation.getArgument(0, Location.class));

        // Bukkit Scheduler
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        mockedBukkit.when(Bukkit::getViewDistance).thenReturn(10);

        // Addon
        when(addon.getOverWorld()).thenReturn(world);
    }

    /**
     * @throws Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilderNoUser() {
        try {
            NewIsland.builder().build();
        } catch (Exception e) {
            assertEquals("Insufficient parameters. Must have a user!", e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilder() throws Exception {
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).oldIsland(oldIsland)
                .build();
        // Verifications
        mockedIslandsManager.verify(() -> IslandsManager.updateIsland(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
        verify(island).setProtectionRange(20);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilderReset() throws Exception {
        when(builder.build()).thenReturn(ire);
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.RESET).oldIsland(oldIsland).build();
        // Verifications
        mockedIslandsManager.verify(() -> IslandsManager.updateIsland(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice, never()).getBlueprintBundle();
        verify(ire).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilderNoOldIsland() throws Exception {
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).build();
        // Verifications
        mockedIslandsManager.verify(() -> IslandsManager.updateIsland(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilderNoOldIslandPasteNoNMS() throws Exception {
        when(location.distance(any())).thenReturn(30D);
        NewIsland.builder().addon(addon).name(NAME).player(user).reason(Reason.CREATE).build();
        // Verifications
        mockedIslandsManager.verify(() -> IslandsManager.updateIsland(island));
        verify(island).setFlagsDefaults();
        verify(bpm).paste(eq(addon), eq(island), eq(NAME), any(Runnable.class), eq(false));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilderNoOldIslandPasteWithNMS() throws Exception {
        NewIsland.builder().addon(addon).name(NAME).player(user).reason(Reason.CREATE).build();
        // Verifications
        mockedIslandsManager.verify(() -> IslandsManager.updateIsland(island));
        verify(island).setFlagsDefaults();
        verify(bpm).paste(eq(addon), eq(island), eq(NAME), any(Runnable.class), eq(true));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilderHasIsland() throws Exception {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).oldIsland(oldIsland).build();
        // Verifications
        mockedIslandsManager.verify(() -> IslandsManager.updateIsland(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
        verify(island).setProtectionRange(20);
        verify(island).setReserved(false);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    void testBuilderHasIslandFail() throws Exception {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).oldIsland(oldIsland).build();
        // Verifications
        mockedIslandsManager.verify(() -> IslandsManager.updateIsland(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
        verify(island).setProtectionRange(20);
    }

    // Removed testBuilderHasIslandFailnoReserve — test was unfinished and expected nonexistent logError call

    /**
     * Regression test for: Island creation changes the home location of previous island.
     * <p>
     * When an island has a spawn point (set during blueprint paste), the home location must
     * be set directly on that specific island object using {@code setHomeLocation(island, spawn, "")},
     * NOT via the user-UUID lookup {@code setHomeLocation(user, spawn)} which resolves to the
     * <em>current primary island</em>. If a second island is created concurrently and becomes
     * primary before the first island's post-creation task runs, the UUID-based lookup would
     * incorrectly set the home on the second island instead.
     */
    @Test
    void testBuilderWithSpawnPointSetsHomeOnIslandDirectly() throws Exception {
        // Make the scheduler execute the runnable immediately so postCreationTask runs inline
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        }).when(scheduler).runTask(any(BentoBox.class), any(Runnable.class));

        // Mock a spawn point being set on the island (e.g., by blueprint paste)
        Location spawnLoc = mock(Location.class);
        when(island.getSpawnPoint(Environment.NORMAL)).thenReturn(spawnLoc);

        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).build();

        // The home must be set directly on the island object (not via user UUID lookup).
        // Using the island-based overload guarantees the correct island is targeted even when
        // a concurrent creation has changed the player's primary island.
        verify(im).setHomeLocation(island, spawnLoc, "");
        // The user-based overload must never be called during island creation
        verify(im, never()).setHomeLocation(eq(user), any());
    }

}
