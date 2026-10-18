package world.bentobox.bentobox.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Players;

class DatabaseTest {
    @Test
    @SuppressWarnings("unchecked")
    void synchronousSaveExceptionCompletesFutureExceptionally() throws Exception {
        var plugin = mock(BentoBox.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        var setup = mock(DatabaseSetup.class);
        var handler = (AbstractDatabaseHandler<Players>) mock(AbstractDatabaseHandler.class);
        when(setup.getHandler(Players.class)).thenReturn(handler);
        var player = mock(Players.class);
        var failure = new IllegalAccessException("serialization failed");
        when(handler.saveObject(player)).thenThrow(failure);
        try (var database = mockStatic(DatabaseSetup.class)) {
            database.when(DatabaseSetup::getDatabase).thenReturn(setup);
            var result = new Database<>(plugin, Players.class).saveObjectAsync(player);
            assertTrue(result.isCompletedExceptionally());
            assertSame(failure, assertThrows(CompletionException.class, result::join).getCause());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveReturnsHandlersFutureWithoutInventingSuccess() throws Exception {
        var plugin = mock(BentoBox.class);
        var setup = mock(DatabaseSetup.class);
        var handler = (AbstractDatabaseHandler<Players>) mock(AbstractDatabaseHandler.class);
        when(setup.getHandler(Players.class)).thenReturn(handler);
        var player = mock(Players.class);
        var pending = new CompletableFuture<Boolean>();
        when(handler.saveObject(player)).thenReturn(pending);
        try (var database = mockStatic(DatabaseSetup.class)) {
            database.when(DatabaseSetup::getDatabase).thenReturn(setup);
            var result = new Database<>(plugin, Players.class).saveObjectAsync(player);
            assertSame(pending, result);
            assertFalse(result.isDone());
            pending.complete(false);
            assertFalse(result.join());
        }
    }
}
