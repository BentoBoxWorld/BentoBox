package world.bentobox.bentobox.database.sql.sqlite;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseSetup;

/**
 * @since 1.6.0
 * @author Poslovitch
 */
public class SQLiteDatabase implements DatabaseSetup {

    private SQLiteDatabaseConnector connector = new SQLiteDatabaseConnector(BentoBox.getInstance());

    @Override
    public <T> AbstractDatabaseHandler<T> getHandler(Class<T> dataObjectClass) {
        return new SQLiteDatabaseHandler<>(BentoBox.getInstance(), dataObjectClass, connector);
    }
}
