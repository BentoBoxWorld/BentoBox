/**
 * 
 */
package us.tastybento.bskyblock.database.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * @author ben
 *
 */
public class MySQLDatabaseHandlerTest {

    private static MySQLDatabaseHandler<MySQLDatabaseHandlerTestDataObject> handler;
    private static MySQLDatabaseHandlerTestDataObject instance;
    private static String UNIQUE_ID = "xyz";

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        Server server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");
        Bukkit.setServer(server);

        BSkyBlock plugin = mock(BSkyBlock.class);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        MySQLDatabaseConnecter dbConn = mock(MySQLDatabaseConnecter.class);
        Connection connection = mock(Connection.class);
        when(dbConn.createConnection()).thenReturn(connection);
        PreparedStatement ps = mock(PreparedStatement.class);
        when(connection.prepareStatement(Mockito.anyString())).thenReturn(ps);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        ResultSet rs = mock(ResultSet.class);
        when(ps.executeQuery()).thenReturn(rs);
        when(statement.executeQuery(Mockito.anyString())).thenReturn(rs);
        instance = new MySQLDatabaseHandlerTestDataObject();
        instance.setUniqueId(UNIQUE_ID);
        handler = new MySQLDatabaseHandler<>(plugin, MySQLDatabaseHandlerTestDataObject.class, dbConn);

    }


    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#getColumns(boolean)}.
     */
    @Test
    public void testGetColumns() {
        // This should be a list of 20 ?'s which related to the 20
        assertEquals("?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?", handler.getColumns(true));
        assertEquals("`uniqueId`, `center`, `range`, `minX`, `minZ`, `minProtectedX`, `minProtectedZ`, " + 
                "`protectionRange`, `world`, `name`, `createdDate`, `updatedDate`, `owner`, `members`, `locked`, " + 
                "`spawn`, `purgeProtected`, `flags`, `levelHandicap`, `spawnPoint`",
                handler.getColumns(false));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#createSelectQuery()}.
     */
    @Test
    public void testCreateSelectQuery() {
        assertEquals("SELECT `uniqueId`, `center`, `range`, `minX`, `minZ`, `minProtectedX`, " +
                "`minProtectedZ`, `protectionRange`, `world`, `name`, `createdDate`, `updatedDate`, " +
                "`owner`, `members`, `locked`, `spawn`, `purgeProtected`, `flags`, `levelHandicap`, " +
                "`spawnPoint` FROM `us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandlerTestDataObject`",
                handler.createSelectQuery());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#createInsertQuery()}.
     */
    @Test
    public void testCreateInsertQuery() {
        assertEquals("REPLACE INTO `us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandlerTestDataObject`(`uniqueId`, " +
                "`center`, `range`, `minX`, `minZ`, `minProtectedX`, `minProtectedZ`, `protectionRange`, " + 
                "`world`, `name`, `createdDate`, `updatedDate`, `owner`, `members`, `locked`, `spawn`, " + 
                "`purgeProtected`, `flags`, `levelHandicap`, `spawnPoint`) " + 
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                handler.createInsertQuery());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#createDeleteQuery()}.
     */
    @Test
    public void testCreateDeleteQuery() {        
        assertEquals("DELETE FROM [table_name] WHERE uniqueId = ?", handler.createDeleteQuery());
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#loadObjects()}.
     */
    @Test
    public void testLoadObjects() {
        try {
            java.util.List<MySQLDatabaseHandlerTestDataObject> result = handler.loadObjects();
            System.out.println("Size of result " + result.size());
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | ClassNotFoundException | SQLException | IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#loadObject(java.lang.String)}.
     */
    @Test
    public void testLoadObject() {
        try {
            MySQLDatabaseHandlerTestDataObject obj = (MySQLDatabaseHandlerTestDataObject) handler.loadObject(UNIQUE_ID);
            assertNull(obj);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException | ClassNotFoundException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#saveObject(java.lang.Object)}.
     */
    @Test
    public void testSaveObject() {
        try {
            handler.saveObject(instance);
        } catch (SecurityException | IllegalArgumentException | InstantiationException | IllegalAccessException
                | InvocationTargetException | NoSuchMethodException | SQLException | IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#deleteObject(java.lang.Object)}.
     */
    @Test
    public void testDeleteObject() {
        try {
            handler.deleteObject(instance);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException | IntrospectionException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#objectExists(java.lang.String)}.
     */
    @Test
    public void testObjectExits() {
        // This right now is not tested properly
        assertFalse(handler.objectExists(UNIQUE_ID));
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#saveSettings(java.lang.Object)}.
     */
    @Test
    public void testSaveSettings() {
        try {
            handler.saveSettings(instance);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link us.tastybento.bskyblock.database.mysql.MySQLDatabaseHandler#loadSettings(java.lang.String, java.lang.Object)}.
     */
    @Test
    public void testLoadSettings() {
        try {
            handler.loadSettings(UNIQUE_ID, instance);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | ClassNotFoundException | IntrospectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
