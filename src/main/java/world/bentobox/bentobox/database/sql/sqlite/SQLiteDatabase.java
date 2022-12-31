package world.bentobox.bentobox.database.sql.sqlite;

import java.io.File;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;


/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class SQLiteDatabase implements DatabaseSetup
{
    /**
     * Database file name.
     */
    private static final String DATABASE_FOLDER_NAME = "database";

    /**
     * SQLite Database Connector.
     */
    private SQLiteDatabaseConnector connector;


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass)
    {
        if (this.connector == null)
        {
            BentoBox plugin = BentoBox.getInstance();
            File dataFolder = new File(plugin.getDataFolder(), DATABASE_FOLDER_NAME);

            if (!dataFolder.exists() && !dataFolder.mkdirs())
            {
                plugin.logError("Could not create database folder!");
                // Trigger plugin shutdown.
                plugin.onDisable();
                return null;
            }

            this.connector = new SQLiteDatabaseConnector("jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "database.db");
        }

        return new SQLiteDatabaseHandler<>(BentoBox.getInstance(), dataObjectClass, this.connector);
    }
}
