package world.bentobox.bentobox.database.sql.sqlite;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.sql.SQLDatabaseConnector;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class SQLiteDatabaseConnector extends SQLDatabaseConnector {

    private static final String DATABASE_FOLDER_NAME = "database";

    SQLiteDatabaseConnector(@NonNull BentoBox plugin) {
        super(null, ""); // Not used by SQLite
        File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            BentoBox.getInstance().logError("Could not create database folder!");
            return;
        }
        connectionUrl = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "database.db";
    }


    /* (non-Javadoc)
     * @see world.bentobox.bentobox.database.sql.SQLDatabaseConnector#createConnection(java.lang.Class)
     */
    @Override
    public Object createConnection(Class<?> type) {
        types.add(type);
        // Only make one connection at a time
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(connectionUrl);
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Could not connect to the database! " + e.getMessage());
            }
        }
        return connection;
    }
}
