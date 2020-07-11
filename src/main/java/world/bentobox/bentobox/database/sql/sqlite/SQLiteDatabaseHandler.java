package world.bentobox.bentobox.database.sql.sqlite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.sql.SQLConfiguration;
import world.bentobox.bentobox.database.sql.SQLDatabaseHandler;

/**
 * @since 1.6.0
 * @author Poslovitch, tastybento
 */
public class SQLiteDatabaseHandler<T> extends SQLDatabaseHandler<T> {

    /**
     * Constructor
     *
     * @param plugin            BentoBox plugin
     * @param type              The type of the objects that should be created and filled with
     *                          values from the database or inserted into the database
     * @param databaseConnector Contains the settings to create a connection to the database
     */
    protected SQLiteDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector, new SQLConfiguration(plugin, type)
                .schema("CREATE TABLE IF NOT EXISTS `[tableName]` (json JSON, uniqueId VARCHAR(255) NOT NULL PRIMARY KEY)")
                .saveObject("INSERT INTO `[tableName]` (json, uniqueId) VALUES (?, ?) ON CONFLICT(uniqueId) DO UPDATE SET json = ?")
                .objectExists("SELECT EXISTS (SELECT 1 FROM `[tableName]` WHERE `uniqueId` = ?)")
                .renameTable("ALTER TABLE `[oldTableName]` RENAME TO `[tableName]`")
                .setUseQuotes(false)
                );
    }


    /**
     * Creates the table in the database if it doesn't exist already
     */
    @Override
    protected void createSchema() {
        if (getSqlConfig().renameRequired()) {
            // SQLite does not have a rename if exists command so we have to manually check if the old table exists
            String sql = "SELECT EXISTS (SELECT 1 FROM sqlite_master WHERE type='table' AND name='" + getSqlConfig().getOldTableName() + "' COLLATE NOCASE)";
            try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
                rename(pstmt);
            } catch (SQLException e) {
                plugin.logError("Could not check if " + getSqlConfig().getOldTableName() + " exists for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
            }
        }
        // Prepare and execute the database statements
        try (PreparedStatement pstmt = getConnection().prepareStatement(getSqlConfig().getSchemaSQL())) {
            pstmt.execute();
        } catch (SQLException e) {
            plugin.logError("Problem trying to create schema for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
        }
    }

    private void rename(PreparedStatement pstmt) {
        try (ResultSet resultSet = pstmt.executeQuery()) {
            if (resultSet.next() && resultSet.getBoolean(1)) {
                // Transition from the old table name
                try (PreparedStatement pstmt2 = getConnection().prepareStatement(getSqlConfig().getRenameTableSQL())) {
                    pstmt2.execute();
                } catch (SQLException e) {
                    plugin.logError("Could not rename " + getSqlConfig().getOldTableName() + " for data object " + dataObject.getCanonicalName() + " " + e.getMessage());
                }
            }
        } catch (Exception ex) {
            plugin.logError("Could not check if " + getSqlConfig().getOldTableName() + " exists for data object " + dataObject.getCanonicalName() + " " + ex.getMessage());
        }
    }

    @Override
    public CompletableFuture<Boolean> saveObject(T instance) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        // Null check
        if (instance == null) {
            plugin.logError("SQLite database request to store a null. ");
            completableFuture.complete(false);
            return completableFuture;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            completableFuture.complete(false);
            return completableFuture;
        }
        Gson gson = getGson();
        String toStore = gson.toJson(instance);
        processQueue.add(() -> {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getSaveObjectSQL())) {
                preparedStatement.setString(1, toStore);
                preparedStatement.setString(2, ((DataObject)instance).getUniqueId());
                preparedStatement.setString(3, toStore);
                preparedStatement.execute();
                completableFuture.complete(true);
            } catch (SQLException e) {
                plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
                completableFuture.complete(false);
            }
        });
        return completableFuture;
    }


}
