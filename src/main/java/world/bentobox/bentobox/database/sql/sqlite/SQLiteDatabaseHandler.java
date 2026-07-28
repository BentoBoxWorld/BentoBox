package world.bentobox.bentobox.database.sql.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.sql.SQLConfiguration;
import world.bentobox.bentobox.database.sql.SQLDatabaseHandler;

/**
 * @since 1.6.0
 * @author Poslovitch, tastybento
 */
public class SQLiteDatabaseHandler<T> extends SQLDatabaseHandler<T>
{
    /**
     * Constructor
     *
     * @param plugin BentoBox plugin
     * @param type The type of the objects that should be created and filled with values from the database or inserted
     * into the database
     * @param databaseConnector Contains the settings to create a connection to the database
     */
    protected SQLiteDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector)
    {
        super(plugin, type, databaseConnector, new SQLConfiguration(plugin, type).
                schema("CREATE TABLE IF NOT EXISTS `[tableName]` (json JSON, uniqueId VARCHAR(255) NOT NULL PRIMARY KEY)").
                saveObject("INSERT INTO `[tableName]` (json, uniqueId) VALUES (?, ?) ON CONFLICT(uniqueId) DO UPDATE SET json = ?").
                objectExists("SELECT EXISTS (SELECT 1 FROM `[tableName]` WHERE `uniqueId` = ?)").
                renameTable("ALTER TABLE `[oldTableName]` RENAME TO `[tableName]`").
                setUseQuotes(false)
                );
    }

    /**
     * Creates the table in the database if it doesn't exist already
     */
    @Override
    protected void createSchema()
    {
        if (this.getSqlConfig().renameRequired())
        {
            // SQLite does not have a rename if exists command so we have to manually check if the old table exists
            String sql = "SELECT EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='" +
                    this.getSqlConfig().getOldTableName() + "' COLLATE NOCASE)";

            try (Connection connection = this.dataSource.getConnection();
                    PreparedStatement preparedStatement = connection.prepareStatement(sql))
            {
                this.rename(preparedStatement);
            }
            catch (SQLException e)
            {
                this.plugin.logError("Could not check if " + this.getSqlConfig().getOldTableName() + " exists for data object " +
                        this.dataObject.getCanonicalName() + " " + e.getMessage());
            }
        }
        // Prepare and execute the database statements
        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(this.getSqlConfig().getSchemaSQL()))
        {
            preparedStatement.execute();
        }
        catch (SQLException e)
        {
            this.plugin.logError("Problem trying to create schema for data object " + dataObject.getCanonicalName() + " " +
                    e.getMessage());
        }
    }

    private void rename(PreparedStatement pstmt)
    {
        try (ResultSet resultSet = pstmt.executeQuery())
        {
            if (resultSet.next() && resultSet.getBoolean(1))
            {
                // Transition from the old table name
                String sql = this.getSqlConfig().getRenameTableSQL().replace("[oldTableName]",
                        this.getSqlConfig().getOldTableName().replace("[tableName]", this.getSqlConfig().getTableName()));

                executeStatement(sql);
            }
        }
        catch (Exception ex)
        {
            this.plugin.logError("Could not check if " + getSqlConfig().getOldTableName() + " exists for data object " +
                    this.dataObject.getCanonicalName() + " " + ex.getMessage());
        }
    }

    private void executeStatement(String sql) {
        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(sql))
        {
            preparedStatement.execute();
        }
        catch (SQLException e)
        {
            this.plugin.logError("Could not rename " + getSqlConfig().getOldTableName() + " for data object " +
                    this.dataObject.getCanonicalName() + " " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Boolean> saveObject(T instance)
    {
        return this.saveInstance(instance, false, "SQLite", SQLiteDatabaseHandler::bindSave);
    }

    @Override
    public CompletableFuture<Boolean> saveObjectNow(T instance)
    {
        return this.saveInstance(instance, true, "SQLite", SQLiteDatabaseHandler::bindSave);
    }

    /**
     * SQLite's upsert takes the json first, then the id, then the json again for the update.
     */
    private static void bindSave(PreparedStatement statement, String json, String uniqueId) throws SQLException
    {
        statement.setString(1, json);
        statement.setString(2, uniqueId);
        statement.setString(3, json);
    }
}