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
 * @since 1.6.0
 * @author tastybento, Poslovitch
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
                .schema("CREATE TABLE IF NOT EXISTS \"" + type.getCanonicalName() + "\" (json jsonb NOT NULL)")
                .loadObject("SELECT * FROM \"" + type.getCanonicalName() + "\" WHERE json->>'uniqueId' = ?")
                .deleteObject("DELETE FROM \"" + type.getCanonicalName() + "\" WHERE json->>'uniqueId' = ?")
                .saveObject("INSERT INTO \"" + type.getCanonicalName() + "\" (json) VALUES (cast(? as json))")
                .loadObjects("SELECT json FROM \"" + type.getCanonicalName() + "\"")
                .objectExists("SELECT EXISTS(SELECT * FROM \"" + type.getCanonicalName() + "\" WHERE json->>'uniqueId' = ?)")
                );
        // Create index
        /*
        try (Statement s = this.getConnection().createStatement()) {
            s.executeQuery("CREATE INDEX idx_json ON \"" + type.getCanonicalName() + "\" USING GIN ((json->‘uniqueId’))");
        } catch (SQLException e) {
            plugin.logError("Could not make index in Postgresql database table for " + type.getCanonicalName());
            e.printStackTrace();
        }*/
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
        String uniqueId = ((DataObject)instance).getUniqueId();
        this.deleteID(uniqueId);
        processQueue.add(() -> {
            try (PreparedStatement preparedStatement = getConnection().prepareStatement(getSqlConfig().getSaveObjectSQL())) {
                preparedStatement.setString(1, toStore);
                plugin.log(preparedStatement.toString());
                preparedStatement.execute();
            } catch (SQLException e) {
                plugin.logError("Could not save object " + instance.getClass().getName() + " " + e.getMessage());
            }
        });
    }
}
