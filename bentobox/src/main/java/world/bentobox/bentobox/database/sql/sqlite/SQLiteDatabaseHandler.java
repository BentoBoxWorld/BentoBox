package world.bentobox.bentobox.database.sql.sqlite;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        super(plugin, type, databaseConnector, new SQLConfiguration(type.getCanonicalName())
                .schema("CREATE TABLE IF NOT EXISTS `" + type.getCanonicalName() + "` (json JSON, uniqueId VARCHAR(255) NOT NULL PRIMARY KEY)")
                .saveObject("INSERT INTO `" + type.getCanonicalName()
                + "` (json, uniqueId) VALUES (?, ?) ON CONFLICT(uniqueId) DO UPDATE SET json = ?")
                .objectExists("SELECT EXISTS (SELECT 1 FROM `" + type.getCanonicalName() + "` WHERE `uniqueId` = ?)"));
    }

    @Override
    public void saveObject(T instance) {
        // Null check
        if (instance == null) {
            plugin.logError("MySQL database request to store a null. ");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        Gson gson = getGson();
        String toStore = gson.toJson(instance);
        processQueue.add(() -> {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getSaveObjectSQL())) {
                preparedStatement.setString(1, toStore);
                preparedStatement.setString(2, ((DataObject)instance).getUniqueId());
                preparedStatement.setString(3, toStore);
                preparedStatement.execute();
            } catch (SQLException e) {
                plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
            }
        });
    }

    @Override
    public void deleteID(String uniqueId) {
        processQueue.add(() -> {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getDeleteObjectSQL())) {
                // UniqueId must *not* be placed in quotes
                preparedStatement.setString(1, uniqueId);
                preparedStatement.executeUpdate();
            } catch (Exception e) {
                plugin.logError("Could not delete object " + dataObject.getCanonicalName() + " " + uniqueId + " " + e.getMessage());
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
