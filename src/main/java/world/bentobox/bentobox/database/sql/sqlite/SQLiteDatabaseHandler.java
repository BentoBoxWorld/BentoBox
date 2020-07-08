package world.bentobox.bentobox.database.sql.sqlite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.NonNull;

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

    private static final String COULD_NOT_LOAD_OBJECT = "Could not load object ";

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
                .renameTable("ALTER TABLE `[oldTableName]` RENAME TO `[tableName]`"));
    }

    @Override
    /*
      Creates the table in the database if it doesn't exist already
     */
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

    @Override
    public void deleteID(String uniqueId) {
        processQueue.add(() -> {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getDeleteObjectSQL())) {
                // UniqueId must *not* be placed in quotes
                preparedStatement.setString(1, uniqueId);
                preparedStatement.executeUpdate();
            } catch (Exception e) {
                plugin.logError("Could not delete object " + plugin.getSettings().getDatabasePrefix() + dataObject.getCanonicalName() + " " + uniqueId + " " + e.getMessage());
            }
        });
    }

    @Override
    public boolean objectExists(String uniqueId) {
        // Query to see if this key exists
        try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getObjectExistsSQL())) {
            // UniqueId needs to be placed in quotes
            preparedStatement.setString(1, uniqueId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBoolean(1);
                }
            }
        } catch (SQLException e) {
            plugin.logError("Could not check if key exists in database! " + uniqueId + " " + e.getMessage());
        }
        return false;
    }

    @Override
    public T loadObject(@NonNull String uniqueId) {
        try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getLoadObjectSQL())) {
            // UniqueId needs to be placed in quotes
            preparedStatement.setString(1, uniqueId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    // If there is a result, we only want/need the first one
                    Gson gson = getGson();
                    return gson.fromJson(resultSet.getString("json"), dataObject);
                }
            } catch (Exception e) {
                plugin.logError(COULD_NOT_LOAD_OBJECT + uniqueId + " " + e.getMessage());
            }
        } catch (SQLException e) {
            plugin.logError(COULD_NOT_LOAD_OBJECT + uniqueId + " " + e.getMessage());
        }
        return null;
    }

}
