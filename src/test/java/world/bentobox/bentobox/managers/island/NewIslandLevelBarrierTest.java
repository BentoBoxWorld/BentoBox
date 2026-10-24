package world.bentobox.bentobox.managers.island;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.AddonsManager;

class NewIslandLevelBarrierTest {
    public static abstract class LevelAddon extends Addon {
        public abstract CompletableFuture<Void> initializeIslandCountAsync(Island island);

        public Object getSettings() { throw new AssertionError("Core must not inspect Level settings"); }
        public Object getManager() { throw new NoClassDefFoundError("world/bentobox/visit/VisitAddon"); }
    }

    @Test void disabledLevelIsSkippedAndConfigurationIsDelegatedToPublicEntryWithoutVisit() {
        var plugin = mock(BentoBox.class);
        var addons = mock(AddonsManager.class);
        var level = mock(LevelAddon.class, CALLS_REAL_METHODS);
        when(plugin.getAddonsManager()).thenReturn(addons);
        when(addons.getAddonByName("Level")).thenReturn(Optional.of(level));
        assertTrue(NewIsland.initializeLevelCount(plugin, mock(Island.class)).isDone());
        verify(level, never()).getSettings();
        when(level.isEnabled()).thenReturn(true);
        var island = mock(Island.class);
        when(level.initializeIslandCountAsync(island)).thenReturn(CompletableFuture.completedFuture(null));
        NewIsland.initializeLevelCount(plugin, island).join();
        verify(level).initializeIslandCountAsync(island);
        verify(level, never()).getSettings();
        verify(level, never()).getManager();
    }

    @Test void deliveryWaitsForDurableBaselineAndPropagatesFailure() {
        var plugin = mock(BentoBox.class);
        var addons = mock(AddonsManager.class);
        var level = mock(LevelAddon.class);
        var island = mock(Island.class);
        when(plugin.getAddonsManager()).thenReturn(addons);
        when(addons.getAddonByName("Level")).thenReturn(Optional.of(level));
        when(level.isEnabled()).thenReturn(true);
        var persisted = new CompletableFuture<Void>();
        when(level.initializeIslandCountAsync(island)).thenReturn(persisted);
        var barrier = NewIsland.initializeLevelCount(plugin, island);
        assertFalse(barrier.isDone());
        persisted.completeExceptionally(new IllegalStateException("baseline save failed"));
        assertTrue(barrier.isCompletedExceptionally());
        verify(level).initializeIslandCountAsync(island);
    }

    @Test void missingInitializationApiFailsClosed() {
        var plugin = mock(BentoBox.class);
        var addons = mock(AddonsManager.class);
        var level = mock(Addon.class);
        when(plugin.getAddonsManager()).thenReturn(addons);
        when(addons.getAddonByName("Level")).thenReturn(Optional.of(level));
        when(level.isEnabled()).thenReturn(true);
        assertTrue(NewIsland.initializeLevelCount(plugin, mock(Island.class)).isCompletedExceptionally());
    }

    @Test void publicEntryLinkageFailureIsUnwrappedAndBlocksDelivery() {
        var plugin = mock(BentoBox.class);
        var addons = mock(AddonsManager.class);
        var level = mock(LevelAddon.class);
        var island = mock(Island.class);
        when(plugin.getAddonsManager()).thenReturn(addons);
        when(addons.getAddonByName("Level")).thenReturn(Optional.of(level));
        when(level.isEnabled()).thenReturn(true);
        var error = new NoClassDefFoundError("unexpected dependency inside Level entry");
        when(level.initializeIslandCountAsync(island)).thenThrow(error);
        var failure = assertThrows(java.util.concurrent.CompletionException.class,
                () -> NewIsland.initializeLevelCount(plugin, island).join());
        assertSame(error, failure.getCause());
    }
}
