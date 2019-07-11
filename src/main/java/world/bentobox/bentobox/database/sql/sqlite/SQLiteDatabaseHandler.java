package world.bentobox.bentobox.database.sql.sqlite;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.gson.Gson;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.sql.AbstractSQLDatabaseHandler;
import world.bentobox.bentobox.database.sql.SQLConfiguration;

/**
 * @since 1.6.0
 * @author Poslovitch, tastybento
 */
public class SQLiteDatabaseHandler<T> extends AbstractSQLDatabaseHandler<T> {

    /**
     * Constructor
     *
     * @param plugin
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

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getSaveObjectSQL())) {
            preparedStatement.setString(1, toStore);
            preparedStatement.setString(2, ((DataObject)instance).getUniqueId());
            preparedStatement.setString(3, toStore);
            preparedStatement.execute();
        } catch (SQLException e) {
            plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
        }
    }

    @Override
    public void deleteID(String uniqueId) {
        try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getDeleteObjectSQL())) {
            // UniqueId must *not* be placed in quotes
            preparedStatement.setString(1, uniqueId);
            int result = preparedStatement.executeUpdate();
            if (result != 1) {
                throw new SQLException("Delete did not affect any rows!");
            }
        } catch (Exception e) {
            plugin.logError("Could not delete object " + dataObject.getCanonicalName() + " " + uniqueId + " " + e.getMessage());
        }
    }
}
