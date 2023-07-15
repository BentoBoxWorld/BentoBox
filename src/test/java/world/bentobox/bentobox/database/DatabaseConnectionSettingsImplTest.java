package world.bentobox.bentobox.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl.DatabaseSettings;

/**
 * @author tastybento
 *
 */
public class DatabaseConnectionSettingsImplTest {

    private DatabaseSettings setting = new DatabaseSettings(
            "localhost",
            3306,
            "mydb",
            "myuser",
            "mypassword",
            true,
            10,
            Collections.singletonMap("key", "value")
            );

    @Test
    public void testConstructorWithDatabaseSettings() {
        DatabaseConnectionSettingsImpl.DatabaseSettings settings = new DatabaseConnectionSettingsImpl.DatabaseSettings(
                "localhost",
                3306,
                "mydb",
                "myuser",
                "mypassword",
                true,
                10,
                Collections.singletonMap("key", "value")
                );

        DatabaseConnectionSettingsImpl connectionSettings = new DatabaseConnectionSettingsImpl(settings);

        assertEquals("localhost", connectionSettings.getHost());
        assertEquals(3306, connectionSettings.getPort());
        assertEquals("mydb", connectionSettings.getDatabaseName());
        assertEquals("myuser", connectionSettings.getUsername());
        assertEquals("mypassword", connectionSettings.getPassword());
        assertTrue(connectionSettings.isUseSSL());
        assertEquals(10, connectionSettings.getMaxConnections());
        assertEquals(Collections.singletonMap("key", "value"), connectionSettings.getExtraProperties());
    }

    @Test
    public void testConstructorWithAllParameters() {
        DatabaseConnectionSettingsImpl connectionSettings = new DatabaseConnectionSettingsImpl(setting);

        assertEquals("localhost", connectionSettings.getHost());
        assertEquals(3306, connectionSettings.getPort());
        assertEquals("mydb", connectionSettings.getDatabaseName());
        assertEquals("myuser", connectionSettings.getUsername());
        assertEquals("mypassword", connectionSettings.getPassword());
        assertTrue(connectionSettings.isUseSSL());
        assertEquals(10, connectionSettings.getMaxConnections());
        assertEquals(Collections.singletonMap("key", "value"), connectionSettings.getExtraProperties());
    }

    @Test
    public void testConstructorWithoutExtraProperties() {
        DatabaseConnectionSettingsImpl connectionSettings = new DatabaseConnectionSettingsImpl(
                "localhost",
                3306,
                "mydb",
                "myuser",
                "mypassword",
                true,
                10
                );

        assertEquals("localhost", connectionSettings.getHost());
        assertEquals(3306, connectionSettings.getPort());
        assertEquals("mydb", connectionSettings.getDatabaseName());
        assertEquals("myuser", connectionSettings.getUsername());
        assertEquals("mypassword", connectionSettings.getPassword());
        assertTrue(connectionSettings.isUseSSL());
        assertEquals(10, connectionSettings.getMaxConnections());
        assertNotNull(connectionSettings.getExtraProperties());
        assertTrue(connectionSettings.getExtraProperties().isEmpty());
    }

    @Test
    public void testGettersAndSetters() {
        DatabaseConnectionSettingsImpl connectionSettings = new DatabaseConnectionSettingsImpl(setting);

        connectionSettings.setHost("localhost");
        assertEquals("localhost", connectionSettings.getHost());

        connectionSettings.setPort(3306);
        assertEquals(3306, connectionSettings.getPort());

        connectionSettings.setDatabaseName("mydb");
        assertEquals("mydb", connectionSettings.getDatabaseName());

        connectionSettings.setUsername("myuser");
        assertEquals("myuser", connectionSettings.getUsername());

        connectionSettings.setPassword("mypassword");
        assertEquals("mypassword", connectionSettings.getPassword());

        connectionSettings.setUseSSL(true);
        assertTrue(connectionSettings.isUseSSL());

        connectionSettings.setMaxConnections(10);
        assertEquals(10, connectionSettings.getMaxConnections());

        Map<String, String> extraProperties = new HashMap<>();
        extraProperties.put("key", "value");
        connectionSettings.setExtraProperties(extraProperties);
        assertEquals(extraProperties, connectionSettings.getExtraProperties());
    }
}

