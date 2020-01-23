package world.bentobox.bentobox.database.sql.postgresql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.gson.Gson;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.database.sql.SQLConfiguration;
import world.bentobox.bentobox.database.sql.SQLDatabaseHandler;

/**
 *
 * @param <T>
 *
 * @since 1.11.0
 * @author tastybento
 */
public class PostgreSQLDatabaseHandler<T> extends SQLDatabaseHandler<T> {

    /**
     * Constructor
     *
     * @param plugin            BentoBox plugin
     * @param type              The type of the objects that should be created and filled with
     *                          values from the database or inserted into the database
     * @param databaseConnector Contains the settings to create a connection to the database
     */
    PostgreSQLDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector, new SQLConfiguration(type.getCanonicalName())
                // Set uniqueid as the primary key (index). Postgresql convention is to use lower case field names
                // Postgresql also uses double quotes (") instead of (`) around tables names with dots.
                .schema("CREATE TABLE IF NOT EXISTS \"" + type.getCanonicalName() + "\" (uniqueid VARCHAR PRIMARY KEY, json jsonb NOT NULL)")
                .loadObject("SELECT * FROM \"" + type.getCanonicalName() + "\" WHERE uniqueid = ? LIMIT 1")
                .deleteObject("DELETE FROM \"" + type.getCanonicalName() + "\" WHERE uniqueid = ?")
                // uniqueId has to be added into the row explicitly so we need to override the saveObject method
                // The json value is a string but has to be cast to json when done in Java
                .saveObject("INSERT INTO \"" + type.getCanonicalName() + "\" (uniqueid, json) VALUES (?, cast(? as json)) "
                        // This is the Postgresql version of UPSERT.
                        + "ON CONFLICT (uniqueid) "
                        + "DO UPDATE SET json = cast(? as json)")
                .loadObjects("SELECT json FROM \"" + type.getCanonicalName() + "\"")
                // Postgres exists function returns true or false natively
                .objectExists("SELECT EXISTS(SELECT * FROM \"" + type.getCanonicalName() + "\" WHERE uniqueid = ?)")
                );
    }

    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.sql.SQLDatabaseHandler#saveObject(java.lang.Object)
     */
    @Override
    public void saveObject(T instance) {
        // Null check
        if (instance == null) {
            plugin.logError("Postgres database request to store a null. ");
            return;
        }
        if (!(instance instanceof DataObject)) {
            plugin.logError("This class is not a DataObject: " + instance.getClass().getName());
            return;
        }
        Gson gson = getGson();
        String toStore = gson.toJson(instance);
        String uniqueId = ((DataObject)instance).getUniqueId();
        processQueue.add(() -> {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getSaveObjectSQL())) {
                preparedStatement.setString(1, uniqueId); // INSERT
                preparedStatement.setString(2, toStore); // INSERT
                preparedStatement.setString(3, toStore); // ON CONFLICT
                preparedStatement.execute();
            } catch (SQLException e) {
                plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
            }
        });
    }
}
