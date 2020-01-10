package world.bentobox.bentobox.database.sql.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
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

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, DriverManager.class })
public class MySQLDatabaseConnectorTest {

    @Mock
    private DatabaseConnectionSettingsImpl dbSettings;
    @Mock
    private Connection connection;
    @Mock
    private Logger logger;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        when(dbSettings.getDatabaseName()).thenReturn("bentobox");
        when(dbSettings.getHost()).thenReturn("localhost");
        when(dbSettings.getPort()).thenReturn(1234);
        when(dbSettings.getUsername()).thenReturn("username");
        when(dbSettings.getPassword()).thenReturn("password");
        // Logger
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getLogger()).thenReturn(logger);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#MySQLDatabaseConnector(world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl)}.
     */
    @Test
    public void testMySQLDatabaseConnector() {
        new MySQLDatabaseConnector(dbSettings);
        verify(dbSettings).getDatabaseName();
        verify(dbSettings).getHost();
        verify(dbSettings).getPort();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#createConnection()}.
     */
    @Ignore("This is apparently very hard to do!")
    @Test
    public void testCreateConnection() {
        MySQLDatabaseConnector dc = new MySQLDatabaseConnector(dbSettings);
        assertEquals(connection, dc.createConnection(null));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#createConnection()}.
     * @throws SQLException
     */
    @Ignore("Does not work in Java 11")
    @Test
    public void testCreateConnectionError() throws SQLException {
        PowerMockito.doThrow(new SQLException("error")).when(DriverManager.class);
        DriverManager.getConnection(any(), any(), any());
        MySQLDatabaseConnector dc = new MySQLDatabaseConnector(dbSettings);
        dc.createConnection(null);
        verify(logger).severe("Could not connect to the database! No suitable driver found for jdbc:mysql://localhost:1234/bentobox?autoReconnect=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#getConnectionUrl()}.
     */
    @Test
    public void testGetConnectionUrl() {
        MySQLDatabaseConnector dc = new MySQLDatabaseConnector(dbSettings);
        assertEquals("jdbc:mysql://localhost:1234/bentobox"
                + "?autoReconnect=true&useSSL=false&allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8", dc.getConnectionUrl());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#getUniqueId(java.lang.String)}.
     */
    @Test
    public void testGetUniqueId() {
        assertTrue(new MySQLDatabaseConnector(dbSettings).getUniqueId("any").isEmpty());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#uniqueIdExists(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testUniqueIdExists() {
        assertFalse(new MySQLDatabaseConnector(dbSettings).uniqueIdExists("", ""));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#closeConnection()}.
     */
    @Test
    public void testCloseConnection() {
        MySQLDatabaseConnector dc = new MySQLDatabaseConnector(dbSettings);
        dc.createConnection(null);
        dc.closeConnection(null);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.sql.mysql.MySQLDatabaseConnector#closeConnection()}.
     */
    @Test
    public void testCloseConnectionError() throws SQLException {
        MySQLDatabaseConnector dc = new MySQLDatabaseConnector(dbSettings);
        dc.createConnection(null);
        dc.closeConnection(null);
    }

}
