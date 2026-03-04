package world.bentobox.bentobox.database.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;

class SQLDatabaseHandlerTest extends CommonTestSetup {

    // ── Inner helpers ────────────────────────────────────────────────────────

    /** Concrete handler that exposes protected members for assertions. */
    private static class TestHandler extends SQLDatabaseHandler<TestDataObject> {
        TestHandler(world.bentobox.bentobox.BentoBox p, DatabaseConnector c, SQLConfiguration cfg) {
            super(p, TestDataObject.class, c, cfg);
        }
        Queue<Runnable> queue() { return processQueue; }
        boolean isShutdown()    { return shutdown; }
    }

    /** Handler whose type parameter is NOT a DataObject — used to exercise that branch. */
    private static class StringHandler extends SQLDatabaseHandler<String> {
        StringHandler(world.bentobox.bentobox.BentoBox p, DatabaseConnector c, SQLConfiguration cfg) {
            super(p, String.class, c, cfg);
        }
    }

    /** Minimal DataObject for round-trip JSON tests. */
    static class TestDataObject implements DataObject {
        @Expose
        private String uniqueId = "test-id";
        @Override public String getUniqueId()             { return uniqueId; }
        @Override public void setUniqueId(String id) { this.uniqueId = id; }
    }

    // ── Mocks ────────────────────────────────────────────────────────────────

    @Mock DatabaseConnector connector;
    @Mock SQLConfiguration  sqlConfig;
    @Mock DataSource        dataSource;
    @Mock Connection        connection;
    @Mock PreparedStatement ps;
    @Mock Statement         stmt;
    @Mock ResultSet         rs;
    @Mock BukkitTask        task;

    TestHandler handler;

    // ── Setup / teardown ─────────────────────────────────────────────────────

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Scheduler must return a non-null task so asyncSaveTask is assigned
        when(sch.runTaskTimerAsynchronously(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(task);

        // DataSource & SQL mocks
        when(connector.createConnection(any())).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(connection.createStatement()).thenReturn(stmt);

        // SQLConfiguration defaults (no rename, standard SQL strings)
        when(sqlConfig.renameRequired()).thenReturn(false);
        when(sqlConfig.getSchemaSQL()).thenReturn("CREATE TABLE IF NOT EXISTS `t` (json JSON)");
        when(sqlConfig.getLoadObjectsSQL()).thenReturn("SELECT json FROM `t`");
        when(sqlConfig.getLoadObjectSQL()).thenReturn("SELECT json FROM `t` WHERE uniqueId = ? LIMIT 1");
        when(sqlConfig.getSaveObjectSQL()).thenReturn("INSERT INTO `t` (json) VALUES (?) ON DUPLICATE KEY UPDATE json = ?");
        when(sqlConfig.getDeleteObjectSQL()).thenReturn("DELETE FROM `t` WHERE uniqueId = ?");
        when(sqlConfig.getObjectExistsSQL()).thenReturn("SELECT IF(EXISTS(SELECT * FROM `t` WHERE uniqueId = ?),1,0)");
        when(sqlConfig.isUseQuotes()).thenReturn(false);

        when(plugin.isEnabled()).thenReturn(true);
        handler = new TestHandler(plugin, connector, sqlConfig);
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ── Constructor / setDataSource ──────────────────────────────────────────

    @Test
    void testConstructor_invokesCreateSchema() throws SQLException {
        // createSchema calls prepareStatement at least once (for schema creation)
        verify(connection, atLeast(1)).prepareStatement(anyString());
        verify(ps, atLeast(1)).execute();
    }

    @Test
    void testConstructor_withRenameRequired_executesRenameAndSchema() throws SQLException {
        when(sqlConfig.renameRequired()).thenReturn(true);
        when(sqlConfig.getRenameTableSQL()).thenReturn("RENAME TABLE [oldTableName] TO [tableName]");
        when(sqlConfig.getOldTableName()).thenReturn("old_table");
        when(sqlConfig.getTableName()).thenReturn("new_table");

        // Constructing a new handler triggers createSchema with renameRequired=true
        new TestHandler(plugin, connector, sqlConfig);

        // rename + schema → at least 2 prepareStatement calls on top of initial handler
        verify(connection, atLeast(2)).prepareStatement(anyString());
    }

    @Test
    void testConstructor_nullDataSource_disablesPlugin() {
        when(connector.createConnection(any())).thenReturn(null);
        new TestHandler(plugin, connector, sqlConfig);
        verify(pim).disablePlugin(plugin);
    }

    @Test
    void testSetDataSource_null_returnsFalseAndDisablesPlugin() {
        assertFalse(handler.setDataSource(null));
        verify(pim).disablePlugin(plugin);
    }

    @Test
    void testSetDataSource_nonNull_returnsTrue() {
        assertTrue(handler.setDataSource(mock(DataSource.class)));
    }

    // ── getSqlConfig / setSqlConfig ──────────────────────────────────────────

    @Test
    void testGetSqlConfig_returnsConfigPassedToConstructor() {
        assertEquals(sqlConfig, handler.getSqlConfig());
    }

    @Test
    void testSetSqlConfig_updatesConfig() {
        SQLConfiguration newConfig = mock(SQLConfiguration.class);
        handler.setSqlConfig(newConfig);
        assertEquals(newConfig, handler.getSqlConfig());
    }

    // ── createSchema ─────────────────────────────────────────────────────────

    @Test
    void testCreateSchema_schemaError_logsError() throws SQLException {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("schema error"));
        handler.createSchema();
        verify(plugin).logError(contains("schema error"));
    }

    @Test
    void testCreateSchema_renameError_logsError() throws SQLException {
        when(sqlConfig.renameRequired()).thenReturn(true);
        when(sqlConfig.getRenameTableSQL()).thenReturn("RENAME [oldTableName] TO [tableName]");
        when(sqlConfig.getOldTableName()).thenReturn("old");
        when(sqlConfig.getTableName()).thenReturn("new");
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("rename error"));

        handler.createSchema();

        verify(plugin).logError(contains("Could not rename"));
    }

    // ── loadObjects ──────────────────────────────────────────────────────────

    @Test
    void testLoadObjects_emptyResultSet_returnsEmptyList() throws SQLException {
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<TestDataObject> result = handler.loadObjects();

        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadObjects_validJson_returnsList() throws SQLException {
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("json")).thenReturn("{\"uniqueId\":\"loaded-id\"}");

        List<TestDataObject> result = handler.loadObjects();

        assertEquals(1, result.size());
        assertEquals("loaded-id", result.get(0).getUniqueId());
    }

    @Test
    void testLoadObjects_multipleRows_returnsAll() throws SQLException {
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getString("json"))
                .thenReturn("{\"uniqueId\":\"id-1\"}", "{\"uniqueId\":\"id-2\"}");

        List<TestDataObject> result = handler.loadObjects();

        assertEquals(2, result.size());
    }

    @Test
    void testLoadObjects_nullJson_skipsRow() throws SQLException {
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("json")).thenReturn(null);

        List<TestDataObject> result = handler.loadObjects();

        assertTrue(result.isEmpty());
    }

    @Test
    void testLoadObjects_invalidJson_logsErrorAndSkips() throws SQLException {
        when(stmt.executeQuery(anyString())).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("json")).thenReturn("not_valid_json");

        List<TestDataObject> result = handler.loadObjects();

        assertTrue(result.isEmpty());
        verify(plugin, atLeast(1)).logError(anyString());
    }

    @Test
    void testLoadObjects_sqlException_returnsEmptyList() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("connection failed"));

        List<TestDataObject> result = handler.loadObjects();

        assertTrue(result.isEmpty());
        verify(plugin).logError(contains("Could not load objects"));
    }

    // ── loadObject ───────────────────────────────────────────────────────────

    @Test
    void testLoadObject_found_returnsDeserializedObject() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getString("json")).thenReturn("{\"uniqueId\":\"abc\"}");

        TestDataObject result = handler.loadObject("abc");

        assertNotNull(result);
        assertEquals("abc", result.getUniqueId());
    }

    @Test
    void testLoadObject_notFound_returnsNull() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertNull(handler.loadObject("missing"));
    }

    @Test
    void testLoadObject_sqlException_returnsNull() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("load error"));

        assertNull(handler.loadObject("some-id"));
        verify(plugin).logError(contains("Could not load object"));
    }

    @Test
    void testLoadObject_useQuotes_true_wrapsUniqueId() throws SQLException {
        when(sqlConfig.isUseQuotes()).thenReturn(true);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        handler.loadObject("my-id");

        verify(ps).setString(1, "\"my-id\"");
    }

    @Test
    void testLoadObject_useQuotes_false_usesRawId() throws SQLException {
        when(sqlConfig.isUseQuotes()).thenReturn(false);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        handler.loadObject("my-id");

        verify(ps).setString(1, "my-id");
    }

    // ── saveObject ───────────────────────────────────────────────────────────

    @Test
    void testSaveObject_null_completesFalseAndLogsError() throws Exception {
        CompletableFuture<Boolean> future = handler.saveObject(null);

        assertFalse(future.get());
        verify(plugin).logError(contains("null"));
    }

    @Test
    void testSaveObject_notDataObject_completesFalseAndLogsError() throws Exception {
        StringHandler sh = new StringHandler(plugin, connector, sqlConfig);

        CompletableFuture<Boolean> future = sh.saveObject("not-a-data-object");

        assertFalse(future.get());
        verify(plugin).logError(contains("This class is not a DataObject"));
    }

    @Test
    void testSaveObject_pluginDisabled_storesSynchronously() throws Exception {
        // Disable plugin so saveObject takes the sync path
        when(plugin.isEnabled()).thenReturn(false);
        clearInvocations(ps);

        CompletableFuture<Boolean> future = handler.saveObject(new TestDataObject());

        assertTrue(future.get());
        verify(ps).setString(eq(1), anyString());
        verify(ps).setString(eq(2), anyString());
        verify(ps).execute();
    }

    @Test
    void testSaveObject_pluginEnabled_addsToQueue() {
        when(plugin.isEnabled()).thenReturn(true);

        handler.saveObject(new TestDataObject());

        assertFalse(handler.queue().isEmpty());
    }

    @Test
    void testSaveObject_sqlException_completesFalse() throws Exception {
        when(plugin.isEnabled()).thenReturn(false);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("write error"));

        CompletableFuture<Boolean> future = handler.saveObject(new TestDataObject());

        assertFalse(future.get());
        verify(plugin).logError(contains("Could not save object"));
    }

    // ── deleteID ─────────────────────────────────────────────────────────────

    @Test
    void testDeleteID_addsRunnableToQueue() {
        handler.deleteID("some-id");

        assertFalse(handler.queue().isEmpty());
    }

    @Test
    void testDeleteID_runnable_executesDeleteStatement() throws Exception {
        handler.deleteID("del-id");
        clearInvocations(ps);

        // Drain the queue to run the stored delete runnable
        handler.queue().poll().run();

        verify(ps).setString(1, "del-id");
        verify(ps).execute();
    }

    @Test
    void testDeleteID_useQuotes_true_wrapsIdInRunnable() throws Exception {
        when(sqlConfig.isUseQuotes()).thenReturn(true);

        handler.deleteID("del-id");
        handler.queue().poll().run();

        verify(ps).setString(1, "\"del-id\"");
    }

    @Test
    void testDeleteID_sqlException_logsError() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("delete error"));

        handler.deleteID("bad-id");
        handler.queue().poll().run();

        verify(plugin).logError(contains("Could not delete object"));
    }

    // ── deleteObject ─────────────────────────────────────────────────────────

    @Test
    void testDeleteObject_null_logsError() {
        handler.deleteObject(null);

        verify(plugin).logError(contains("null"));
        assertTrue(handler.queue().isEmpty());
    }

    @Test
    void testDeleteObject_notDataObject_logsError() {
        StringHandler sh = new StringHandler(plugin, connector, sqlConfig);

        sh.deleteObject("not-a-data-object");

        verify(plugin).logError(contains("This class is not a DataObject"));
    }

    @Test
    void testDeleteObject_validDataObject_addsToQueue() {
        handler.deleteObject(new TestDataObject());

        assertFalse(handler.queue().isEmpty());
    }

    // ── objectExists ─────────────────────────────────────────────────────────

    @Test
    void testObjectExists_dbReturnsTrue() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(true);

        assertTrue(handler.objectExists("existing-id"));
    }

    @Test
    void testObjectExists_dbReturnsFalse() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getBoolean(1)).thenReturn(false);

        assertFalse(handler.objectExists("missing-id"));
    }

    @Test
    void testObjectExists_noResultRow_returnsFalse() throws SQLException {
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        assertFalse(handler.objectExists("missing-id"));
    }

    @Test
    void testObjectExists_sqlException_returnsFalseAndLogsError() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("db down"));

        assertFalse(handler.objectExists("id"));
        verify(plugin).logError(contains("Could not check if key exists"));
    }

    @Test
    void testObjectExists_useQuotes_true_wrapsId() throws SQLException {
        when(sqlConfig.isUseQuotes()).thenReturn(true);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        handler.objectExists("my-id");

        verify(ps).setString(1, "\"my-id\"");
    }

    @Test
    void testObjectExists_useQuotes_false_usesRawId() throws SQLException {
        when(sqlConfig.isUseQuotes()).thenReturn(false);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        handler.objectExists("my-id");

        verify(ps).setString(1, "my-id");
    }

    // ── close ────────────────────────────────────────────────────────────────

    @Test
    void testClose_setsShutdown() {
        assertFalse(handler.isShutdown());
        handler.close();
        assertTrue(handler.isShutdown());
    }

    // ── store (via saveObject sync path): async-guard branch ─────────────────

    @Test
    void testStore_asyncTrueAndPluginDisabled_doesNotComplete() throws Exception {
        // saveObject must see isEnabled()=true to enqueue (async path)
        when(plugin.isEnabled()).thenReturn(true);
        CompletableFuture<Boolean> future = handler.saveObject(new TestDataObject());
        assertFalse(handler.queue().isEmpty());

        // Now disable the plugin; when the queued runnable runs, the guard returns early
        // and the future is never completed.
        when(plugin.isEnabled()).thenReturn(false);
        handler.queue().poll().run();

        assertFalse(future.isDone());
    }
}
