package world.bentobox.bentobox.database.sql.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.sql.SQLConfiguration;
import world.bentobox.bentobox.database.sql.SQLDatabaseHandler;

/**
 *
 * @param <T>
 *
 * @since 1.11.0
 * @author tastybento
 */
public class PostgreSQLDatabaseHandler<T> extends SQLDatabaseHandler<T>
{
    /**
     * Constructor
     *
     * @param plugin BentoBox plugin
     * @param type The type of the objects that should be created and filled with values from the database or inserted
     * into the database
     * @param databaseConnector Contains the settings to create a connection to the database
     */
    PostgreSQLDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector)
    {
        super(plugin,
                type,
                databaseConnector,
                new SQLConfiguration(plugin, type).
                // Set uniqueid as the primary key (index). Postgresql convention is to use lower case field names
                // Postgresql also uses double quotes (") instead of (`) around tables names with dots.
                schema("CREATE TABLE IF NOT EXISTS \"[tableName]\" (uniqueid VARCHAR PRIMARY KEY, json jsonb NOT NULL)").
                loadObject("SELECT * FROM \"[tableName]\" WHERE uniqueid = ? LIMIT 1").
                deleteObject("DELETE FROM \"[tableName]\" WHERE uniqueid = ?").
                // uniqueId has to be added into the row explicitly so we need to override the saveObject method
                // The json value is a string but has to be cast to json when done in Java
                saveObject("INSERT INTO \"[tableName]\" (uniqueid, json) VALUES (?, cast(? as json)) "
                        // This is the Postgresql version of UPSERT.
                        + "ON CONFLICT (uniqueid) DO UPDATE SET json = cast(? as json)").
                loadObjects("SELECT json FROM \"[tableName]\"").
                // Postgres exists function returns true or false natively
                objectExists("SELECT EXISTS(SELECT * FROM \"[tableName]\" WHERE uniqueid = ?)").
                renameTable("ALTER TABLE IF EXISTS \"[oldTableName]\" RENAME TO \"[tableName]\"").
                setUseQuotes(false)
                );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Boolean> saveObject(T instance)
    {
        return this.saveInstance(instance, false, "PostgreSQL", PostgreSQLDatabaseHandler::bindSave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Boolean> saveObjectNow(T instance)
    {
        return this.saveInstance(instance, true, "PostgreSQL", PostgreSQLDatabaseHandler::bindSave);
    }

    /**
     * PostgreSQL's upsert takes the id first for the INSERT, then the json, then the json again for
     * the ON CONFLICT update.
     */
    private static void bindSave(PreparedStatement statement, String json, String uniqueId) throws SQLException
    {
        statement.setString(1, uniqueId);
        statement.setString(2, json);
        statement.setString(3, json);
    }
}
