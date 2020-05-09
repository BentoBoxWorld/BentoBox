package world.bentobox.bentobox.database.sql.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( { Bukkit.class, BentoBox.class, Util.class })
public class MySQLDatabaseHandlerTest {

    private static final String JSON = "{\n" +
            "  \"deleted\": false,\n" +
            "  \"uniqueId\": \"xyz\",\n" +
            "  \"range\": 0,\n" +
            "  \"protectionRange\": 0,\n" +
            "  \"maxEverProtectionRange\": 0,\n" +
            "  \"createdDate\": 0,\n" +
            "  \"updatedDate\": 0,\n" +
            "  \"members\": {},\n" +
            "  \"spawn\": false,\n" +
            "  \"purgeProtected\": false,\n" +
            "  \"flags\": {},\n" +
            "  \"history\": [],\n" +
            "  \"levelHandicap\": 0,\n" +
            "  \"spawnPoint\": {},\n" +
            "  \"doNotLoad\": false,\n" +
            "  \"cooldowns\": {}\n" +
            "}";
    private MySQLDatabaseHandler<Island> handler;
    private Island instance;
    private String UNIQUE_ID = "xyz";
    @Mock
    private MySQLDatabaseConnector dbConn;
    @Mock
    private BentoBox plugin;
    @Mock
    private BukkitScheduler sch;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement ps;
    @Mock
    private Settings settings;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Setup plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.isEnabled()).thenReturn(true);

        // Settings
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getDatabasePrefix()).thenReturn(""); // No prefix

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Plugin Manager
        pluginManager = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pluginManager);

        // MySQLDatabaseConnector
        when(dbConn.createConnection(any())).thenReturn(connection);

        // Queries
        when(connection.prepareStatement(Mockito.anyString())).thenReturn(ps);
        when(connection.createStatement()).thenReturn(ps);
        ResultSet rs = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(rs);
        when(ps.executeQuery(Mockito.anyString())).thenReturn(rs);

        // Instance to save
        instance = new Island();
        instance.setUniqueId(UNIQUE_ID);
        handler = new MySQLDatabaseHandler<>(plugin, Island.class, dbConn);

    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObjects()}.
     * @throws SQLException
     */
    @Test
    public void testLoadObjectsNoConnection() throws SQLException {
        when(connection.createStatement()).thenThrow(new SQLException("no connection"));
        handler.loadObjects();
        verify(plugin).logError("Could not load objects no connection");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObjects()}.
     * @throws SQLException
     */
    @Test
    public void testLoadObjects() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(any())).thenReturn(JSON);
        // Three islands
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(ps.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        List<Island> objects = handler.loadObjects();
        verify(ps).executeQuery("SELECT `json` FROM `Islands`");
        assertTrue(objects.size() == 3);
        assertEquals("xyz", objects.get(2).getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObjects()}.
     * @throws SQLException
     */
    @Test
    @Ignore
    public void testLoadObjectsPrefix() throws SQLException {
        when(settings.getDatabasePrefix()).thenReturn("a");
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(any())).thenReturn(JSON);
        // Three islands
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(ps.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        List<Island> objects = handler.loadObjects();
        verify(ps).executeQuery("SELECT `json` FROM `aIslands`");
        assertTrue(objects.size() == 3);
        assertEquals("xyz", objects.get(2).getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObjects()}.
     * @throws SQLException
     */
    @Test
    public void testLoadObjectsBadJSON() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(any())).thenReturn("sfdasfasdfsfd");
        // Three islands
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(ps.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        List<Island> objects = handler.loadObjects();
        verify(ps).executeQuery("SELECT `json` FROM `Islands`");
        assertTrue(objects.isEmpty());
        verify(plugin, Mockito.times(3)).logError("Could not load object java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObjects()}.
     * @throws SQLException
     */
    @Test
    public void testLoadObjectsError() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(any())).thenThrow(new SQLException("SQL error"));
        // Three islands
        when(resultSet.next()).thenReturn(true, true, true, false);
        when(ps.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        List<Island> objects = handler.loadObjects();
        verify(ps).executeQuery("SELECT `json` FROM `Islands`");
        assertTrue(objects.isEmpty());
        verify(plugin).logError("Could not load objects SQL error");

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObject(java.lang.String)}.
     */
    @Test
    public void testLoadObjectNoConnection() throws SQLException {
        when(connection.prepareStatement(Mockito.anyString())).thenThrow(new SQLException("no connection"));
        handler.loadObject("abc");
        verify(plugin).logError("Could not load object abc no connection");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObject(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    public void testLoadObject() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(any())).thenReturn(JSON);
        when(resultSet.next()).thenReturn(true);
        when(ps.executeQuery()).thenReturn(resultSet);
        Island object = handler.loadObject("abc");
        verify(ps).executeQuery();
        verify(ps).setString(1, "\"abc\"");
        verify(resultSet).next();
        assertNotNull(object);
        assertEquals("xyz", object.getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObject(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    public void testLoadObjectBadJSON() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(any())).thenReturn("afdsaf");
        when(resultSet.next()).thenReturn(true);
        when(ps.executeQuery()).thenReturn(resultSet);
        Island object = handler.loadObject("abc");
        assertNull(object);
        verify(plugin).logError("Could not load object abc java.lang.IllegalStateException: Expected BEGIN_OBJECT but was STRING at line 1 column 1 path $");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#loadObject(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    public void testLoadObjectError() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getString(any())).thenReturn(JSON);
        when(resultSet.next()).thenThrow(new SQLException("SQL Exception"));
        when(ps.executeQuery()).thenReturn(resultSet);
        Island object = handler.loadObject("abc");
        assertNull(object);
        verify(plugin).logError("Could not load object abc SQL Exception");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#saveObject(java.lang.Object)}.
     */
    @Test
    public void testSaveObjectNull() {
        handler.saveObject(null);
        verify(plugin).logError(eq("SQL database request to store a null. "));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#saveObject(java.lang.Object)}.
     */
    @Test
    public void testSaveObjectNotDataObject() {
        @SuppressWarnings("rawtypes")
        MySQLDatabaseHandler<List> h = new MySQLDatabaseHandler<List>(plugin, List.class, dbConn);
        h.saveObject(Collections.singletonList("test"));
        verify(plugin).logError(eq("This class is not a DataObject: java.util.Collections$SingletonList"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#saveObject(java.lang.Object)}.
     * @throws SQLException
     */
    @Test
    @Ignore("Async cannot be tested")
    public void testSaveObject() throws SQLException {
        // Disable plugin
        when(plugin.isEnabled()).thenReturn(false);
        handler.saveObject(instance);
        verify(ps).execute();
        verify(ps).setString(1, JSON);
        verify(ps).setString(2, "{\n" +
                "  \"deleted\": false,\n" +
                "  \"uniqueId\": \"xyz\",\n" +
                "  \"range\": 0,\n" +
                "  \"protectionRange\": 0,\n" +
                "  \"maxEverProtectionRange\": 0,\n" +
                "  \"createdDate\": 0,\n" +
                "  \"updatedDate\": 0,\n" +
                "  \"members\": {},\n" +
                "  \"spawn\": false,\n" +
                "  \"purgeProtected\": false,\n" +
                "  \"flags\": {},\n" +
                "  \"history\": [],\n" +
                "  \"levelHandicap\": 0,\n" +
                "  \"spawnPoint\": {},\n" +
                "  \"doNotLoad\": false,\n" +
                "  \"cooldowns\": {}\n" +
                "}");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#saveObject(java.lang.Object)}.
     * @throws SQLException
     */
    @Test
    @Ignore("Async cannot be tested")
    public void testSaveObjectFail() throws SQLException {
        // Disable plugin
        when(plugin.isEnabled()).thenReturn(false);
        when(ps.execute()).thenThrow(new SQLException("fail!"));
        handler.saveObject(instance);
        verify(plugin).logError(eq("Could not save object Islands fail!"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#deleteObject(java.lang.Object)}.
     */
    @Test
    public void testDeleteObjectNull() {
        handler.deleteObject(null);
        verify(plugin).logError(eq("SQL database request to delete a null."));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#deleteObject(java.lang.Object)}.
     */
    @Test
    public void testDeleteObjectIncorrectType() {
        @SuppressWarnings("rawtypes")
        MySQLDatabaseHandler<List> h = new MySQLDatabaseHandler<List>(plugin, List.class, dbConn);
        h.deleteObject(Collections.singletonList("test"));
        verify(plugin).logError(eq("This class is not a DataObject: java.util.Collections$SingletonList"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#deleteObject(java.lang.Object)}.
     * @throws SQLException
     */
    @Test
    @Ignore("Async cannot be tested")
    public void testDeleteObject() throws SQLException {
        // Disable plugin
        when(plugin.isEnabled()).thenReturn(false);
        handler.deleteObject(instance);
        verify(ps).execute();
        verify(ps).setString(1, "\"xyz\"");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#objectExists(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    public void testObjectExistsNot() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);
        assertFalse(handler.objectExists("hello"));
        verify(connection).prepareStatement("CREATE TABLE IF NOT EXISTS `Islands` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) ) ENGINE = INNODB");
        verify(ps).executeQuery();
        verify(ps).setString(1, "\"hello\"");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#objectExists(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    public void testObjectExistsFalse() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(eq(1))).thenReturn(false);
        assertFalse(handler.objectExists("hello"));
        verify(connection).prepareStatement("CREATE TABLE IF NOT EXISTS `Islands` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) ) ENGINE = INNODB");
        verify(ps).executeQuery();
        verify(ps).setString(1, "\"hello\"");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#objectExists(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    public void testObjectExists() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(eq(1))).thenReturn(true);
        assertTrue(handler.objectExists("hello"));
        verify(connection).prepareStatement("CREATE TABLE IF NOT EXISTS `Islands` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) ) ENGINE = INNODB");
        verify(ps).executeQuery();
        verify(ps).setString(1, "\"hello\"");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#objectExists(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    @Ignore
    public void testObjectExistsPrefix() throws SQLException {
        when(settings.getDatabasePrefix()).thenReturn("a");
        ResultSet resultSet = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBoolean(eq(1))).thenReturn(true);
        assertTrue(handler.objectExists("hello"));
        verify(connection).prepareStatement("CREATE TABLE IF NOT EXISTS `aIslands` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) ) ENGINE = INNODB");
        verify(ps).executeQuery();
        verify(ps).setString(1, "\"hello\"");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#objectExists(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    public void testObjectExistsError() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenThrow(new SQLException("error"));
        handler.objectExists("hello");
        verify(plugin).logError(eq("Could not check if key exists in database! hello error"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#deleteID(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    @Ignore("Cannot test async")
    public void testDeleteID() throws SQLException {
        // Disable plugin
        when(plugin.isEnabled()).thenReturn(false);
        handler.deleteID("abc123");
        verify(ps).execute();
        verify(ps).setString(1, "\"abc123\"");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#deleteID(java.lang.String)}.
     * @throws SQLException
     */
    @Test
    @Ignore("Cannot test async")
    public void testDeleteIDError() throws SQLException {
        // Disable plugin
        when(plugin.isEnabled()).thenReturn(false);
        when(ps.execute()).thenThrow(new SQLException("fail!"));
        handler.deleteID("abc123");
        verify(plugin).logError(eq("Could not delete object Islands abc123 fail!"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#MySQLDatabaseHandler(world.bentobox.bentobox.BentoBox, java.lang.Class, world.bentobox.bentobox.database.DatabaseConnector)}.
     */
    @Test
    public void testMySQLDatabaseHandlerBadPassword() {
        when(dbConn.createConnection(any())).thenReturn(null);
        new MySQLDatabaseHandler<>(plugin, Island.class, dbConn);
        verify(plugin).logError("Could not connect to the database. Are the credentials in the config.yml file correct?");
        verify(pluginManager).disablePlugin(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#MySQLDatabaseHandler(world.bentobox.bentobox.BentoBox, java.lang.Class, world.bentobox.bentobox.database.DatabaseConnector)}.
     * @throws SQLException
     */
    @Test
    public void testMySQLDatabaseHandlerCreateSchema() throws SQLException {
        verify(dbConn).createConnection(any());
        verify(connection).prepareStatement("CREATE TABLE IF NOT EXISTS `Islands` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) ) ENGINE = INNODB");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#MySQLDatabaseHandler(world.bentobox.bentobox.BentoBox, java.lang.Class, world.bentobox.bentobox.database.DatabaseConnector)}.
     * @throws SQLException
     */
    @Test
    @Ignore
    public void testMySQLDatabaseHandlerCreateSchemaPrefix() throws SQLException {
        when(settings.getDatabasePrefix()).thenReturn("a");
        verify(dbConn).createConnection(any());
        verify(connection).prepareStatement("CREATE TABLE IF NOT EXISTS `aIslands` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) ) ENGINE = INNODB");
    }
    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseHandler#MySQLDatabaseHandler(world.bentobox.bentobox.BentoBox, java.lang.Class, world.bentobox.bentobox.database.DatabaseConnector)}.
     * @throws SQLException
     */
    @Test
    public void testMySQLDatabaseHandlerSchemaFail() throws SQLException {
        when(ps.execute()).thenThrow(new SQLException("oh no!"));
        handler = new MySQLDatabaseHandler<>(plugin, Island.class, dbConn);
        verify(plugin).logError("Problem trying to create schema for data object world.bentobox.bentobox.database.objects.Island oh no!");

    }

}
