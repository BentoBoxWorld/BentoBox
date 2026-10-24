package world.bentobox.bentobox.database.json;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.util.TaskScheduler;

class JSONDatabaseHandlerTest {
    @TempDir Path directory;
    private BentoBox plugin;
    private TestHandler handler;
    private MockedStatic<TaskScheduler> tasks;

    public static class Record implements DataObject {
        @Expose private String uniqueId = "test";
        @Expose private String value = "old";
        @Override public String getUniqueId() { return uniqueId; }
        @Override public void setUniqueId(String id) { uniqueId = id; }
    }

    private static class TestHandler extends JSONDatabaseHandler<Record> {
        TestHandler(BentoBox plugin) { super(plugin, Record.class, mock(DatabaseConnector.class)); }
        void runQueued() { processQueue.remove().run(); }
    }

    @BeforeEach
    void setup() {
        plugin = mock(BentoBox.class);
        when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        tasks = mockStatic(TaskScheduler.class);
        handler = new TestHandler(plugin);
    }

    @AfterEach
    void cleanup() { tasks.close(); }

    @Test
    void completionSeesClosedCompleteFileAndBackupPreservesOldData() throws Exception {
        var record = new Record();
        assertTrue(handler.saveObjectNow(record).join());
        record.value = "新的等级";
        var saved = handler.saveObject(record);
        assertFalse(saved.isDone());
        var visible = saved.thenApply(success -> {
            assertTrue(success);
            return handler.loadObject("test").value;
        });
        handler.runQueued();
        assertEquals(record.value, visible.join());
        assertTrue(Files.readString(directory.resolve("database_backup/Record/test.json")).contains("old"));
        try (var files = Files.list(directory.resolve("database/Record"))) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void failedReplacementReportsFailureAndRemovesTemporaryFile() throws Exception {
        Path target = directory.resolve("database/Record/test.json");
        Files.createDirectories(target);
        Files.writeString(target.resolve("keep"), "original");
        assertFalse(handler.saveObjectNow(new Record()).join());
        assertEquals("original", Files.readString(target.resolve("keep")));
        try (var files = Files.list(target.getParent())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void skippedQueuedWriteCompletesFalseInsteadOfHanging() throws Exception {
        var saved = handler.saveObject(new Record());
        when(plugin.isEnabled()).thenReturn(false);
        handler.runQueued();
        assertTrue(saved.isDone());
        assertFalse(saved.join());
    }

    @Test
    void shutdownFlushStillPersistsQueuedWrites() throws Exception {
        var saved = handler.saveObject(new Record());
        when(plugin.isEnabled()).thenReturn(false);
        handler.flush();
        assertTrue(saved.join());
        assertEquals("old", handler.loadObject("test").value);
    }

    /**
     * An addon that is disabled must not leave its database writer running. The
     * pending write is flushed and the writer is stopped, so the writer task no
     * longer keeps the addon's class loader alive.
     */
    @Test
    void stopHandlersFlushesAndStopsTheWriterOfThatClassLoader() throws Exception {
        Record record = new Record();
        record.value = "flushed";
        handler.saveObject(record);
        // Queued writes are not on disk yet.
        assertFalse(Files.exists(directory.resolve("database/Record/test.json")));
        AbstractDatabaseHandler.stopHandlers(getClass().getClassLoader());
        assertTrue(Files.exists(directory.resolve("database/Record/test.json")));
        assertEquals("flushed", handler.loadObject("test").value);
    }
}
