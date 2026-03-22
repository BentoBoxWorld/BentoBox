package world.bentobox.bentobox.database.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.database.DatabaseConnectionSettingsImpl;

class SQLDatabaseConnectorTest extends CommonTestSetup {

    /** Concrete subclass for testing the abstract SQLDatabaseConnector. */
    private static class TestSQLConnector extends SQLDatabaseConnector {
        TestSQLConnector(DatabaseConnectionSettingsImpl dbSettings, String connectionUrl) {
            super(dbSettings, connectionUrl);
        }

        @Override
        public HikariConfig createConfig() {
            return new HikariConfig();
        }
    }

    @Mock
    private DatabaseConnectionSettingsImpl dbSettings;

    private TestSQLConnector connector;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Reset static state before each test
        SQLDatabaseConnector.dataSource = null;
        SQLDatabaseConnector.types.clear();
        connector = new TestSQLConnector(dbSettings, "jdbc:test://localhost/db");
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        // Reset static state after each test
        SQLDatabaseConnector.dataSource = null;
        SQLDatabaseConnector.types.clear();
        super.tearDown();
    }

    // ── getConnectionUrl ─────────────────────────────────────────────────────

    @Test
    void testGetConnectionUrl() {
        assertEquals("jdbc:test://localhost/db", connector.getConnectionUrl());
    }

    // ── getUniqueId ──────────────────────────────────────────────────────────

    @Test
    void testGetUniqueId_returnsEmptyString() {
        assertEquals("", connector.getUniqueId("any_table"));
    }

    // ── uniqueIdExists ───────────────────────────────────────────────────────

    @Test
    void testUniqueIdExists_returnsFalse() {
        assertFalse(connector.uniqueIdExists("any_table", "any_key"));
    }

    // ── createConnection ─────────────────────────────────────────────────────

    @Test
    void testCreateConnection_reusesExistingDataSource() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;

        Object result = connector.createConnection(String.class);

        assertEquals(mockDs, result);
    }

    @Test
    void testCreateConnection_addsType() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;

        connector.createConnection(String.class);

        assertTrue(SQLDatabaseConnector.types.contains(String.class));
    }

    @Test
    void testCreateConnection_multipleDifferentTypes() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;

        connector.createConnection(String.class);
        connector.createConnection(Integer.class);

        assertTrue(SQLDatabaseConnector.types.contains(String.class));
        assertTrue(SQLDatabaseConnector.types.contains(Integer.class));
    }

    @Test
    void testCreateConnection_whenDataSourceNull_createsNew() {
        try (MockedConstruction<HikariDataSource> mocked = Mockito.mockConstruction(HikariDataSource.class,
                (ds, context) -> {
                    Connection conn = mock(Connection.class);
                    when(conn.isValid(5000)).thenReturn(true);
                    when(ds.getConnection()).thenReturn(conn);
                })) {

            Object result = connector.createConnection(String.class);

            assertNotNull(result);
            assertTrue(result instanceof HikariDataSource);
            assertEquals(1, mocked.constructed().size());
        }
    }

    @Test
    void testCreateConnection_whenDataSourceNull_sqlExceptionSetsNull() {
        try (MockedConstruction<HikariDataSource> mocked = Mockito.mockConstruction(HikariDataSource.class,
                (ds, context) -> {
                    when(ds.getConnection()).thenThrow(new SQLException("Connection failed"));
                })) {

            Object result = connector.createConnection(String.class);

            assertNull(result);
            assertTrue(SQLDatabaseConnector.types.contains(String.class));
        }
    }

    @Test
    void testCreateConnection_doesNotRecreateExistingDataSource() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;

        connector.createConnection(String.class);
        connector.createConnection(Integer.class);

        // dataSource should remain the same mock, not be recreated
        assertEquals(mockDs, SQLDatabaseConnector.dataSource);
    }

    // ── closeConnection ──────────────────────────────────────────────────────

    @Test
    void testCloseConnection_removesType() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;
        SQLDatabaseConnector.types.add(String.class);
        SQLDatabaseConnector.types.add(Integer.class);

        connector.closeConnection(String.class);

        assertFalse(SQLDatabaseConnector.types.contains(String.class));
        assertTrue(SQLDatabaseConnector.types.contains(Integer.class));
    }

    @Test
    void testCloseConnection_closesAndClearsDataSourceWhenLastType() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;
        SQLDatabaseConnector.types.add(String.class);

        connector.closeConnection(String.class);

        verify(mockDs).close();
        assertNull(SQLDatabaseConnector.dataSource);
        assertTrue(SQLDatabaseConnector.types.isEmpty());
    }

    @Test
    void testCloseConnection_doesNotCloseDataSourceWhenTypesRemain() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;
        SQLDatabaseConnector.types.add(String.class);
        SQLDatabaseConnector.types.add(Integer.class);

        connector.closeConnection(String.class);

        verify(mockDs, never()).close();
        assertNotNull(SQLDatabaseConnector.dataSource);
    }

    @Test
    void testCloseConnection_removingNonexistentTypeIsHarmless() {
        HikariDataSource mockDs = mock(HikariDataSource.class);
        SQLDatabaseConnector.dataSource = mockDs;
        SQLDatabaseConnector.types.add(String.class);

        // Removing a type that was never added should not close the data source
        connector.closeConnection(Integer.class);

        verify(mockDs, never()).close();
        assertTrue(SQLDatabaseConnector.types.contains(String.class));
    }

    // ── createConfig (abstract) ──────────────────────────────────────────────

    @Test
    void testCreateConfig_returnsNonNull() {
        assertNotNull(connector.createConfig());
    }
}
