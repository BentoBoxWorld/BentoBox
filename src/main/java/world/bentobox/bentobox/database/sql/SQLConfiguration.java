package world.bentobox.bentobox.database.sql;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Table;

/**
 * Contains the SQL strings for the database.
 * The default strings are for MySQL, so only the deltas need to be supplied.
 * @author tastybento
 *
 */
public class SQLConfiguration {
    private String loadObjectSQL;
    private String saveObjectSQL;
    private String deleteObjectSQL;
    private String objectExistsSQL;
    private String schemaSQL;
    private String loadObjectsSQL;
    private String renameTableSQL;
    private final String tableName;
    private boolean renameRequired;
    private final String oldTableName;

    public <T> SQLConfiguration(BentoBox plugin, Class<T> type) {
        // Set the table name
        oldTableName = plugin.getSettings().getDatabasePrefix() + type.getCanonicalName();
        this.tableName = plugin.getSettings().getDatabasePrefix() +
                (type.getAnnotation(Table.class) == null ?
                        type.getCanonicalName()
                        : type.getAnnotation(Table.class).name());
        // Only rename if there is a specific Table annotation
        renameRequired = !tableName.equals(oldTableName);
        schema("CREATE TABLE IF NOT EXISTS `[tableName]` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) )");
        loadObjects("SELECT `json` FROM `[tableName]`");
        loadObject("SELECT `json` FROM `[tableName]` WHERE uniqueId = ? LIMIT 1");
        saveObject("INSERT INTO `[tableName]` (json) VALUES (?) ON DUPLICATE KEY UPDATE json = ?");
        deleteObject("DELETE FROM `[tableName]` WHERE uniqueId = ?");
        objectExists("SELECT IF ( EXISTS( SELECT * FROM `[tableName]` WHERE `uniqueId` = ?), 1, 0)");
        renameTable("SELECT Count(*) INTO @exists " +
                "FROM information_schema.tables " +
                "WHERE table_schema = '" + plugin.getSettings().getDatabaseName() + "' " +
                "AND table_type = 'BASE TABLE' " +
                "AND table_name = '[oldTableName]'; " +
                "SET @query = If(@exists=1,'RENAME TABLE `[oldTableName]` TO `[tableName]`','SELECT \\'nothing to rename\\' status'); " +
                "PREPARE stmt FROM @query;" +
                "EXECUTE stmt;");
    }

    private final String TABLE_NAME = "\\[tableName\\]";
    /**
     * By default, use quotes around the unique ID in the SQL statement
     */
    private boolean useQuotes = true;

    public SQLConfiguration loadObject(String string) {
        this.loadObjectSQL = string.replaceFirst(TABLE_NAME, tableName);
        return this;
    }

    public SQLConfiguration saveObject(String string) {
        this.saveObjectSQL = string.replaceFirst(TABLE_NAME, tableName);
        return this;
    }

    public SQLConfiguration deleteObject(String string) {
        this.deleteObjectSQL = string.replaceFirst(TABLE_NAME, tableName);
        return this;
    }

    public SQLConfiguration objectExists(String string) {
        this.objectExistsSQL = string.replaceFirst(TABLE_NAME, tableName);
        return this;
    }

    public SQLConfiguration schema(String string) {
        this.schemaSQL = string.replaceFirst(TABLE_NAME, tableName);
        return this;
    }

    public SQLConfiguration loadObjects(String string) {
        this.loadObjectsSQL = string.replaceFirst(TABLE_NAME, tableName);
        return this;
    }

    public SQLConfiguration renameTable(String string) {
        this.renameTableSQL = string.replaceAll(TABLE_NAME, tableName).replaceAll("\\[oldTableName\\]", oldTableName);
        return this;
    }

    public SQLConfiguration setUseQuotes(boolean b) {
        this.useQuotes = b;
        return this;
    }


    /**
     * @return the loadObjectSQL
     */
    public String getLoadObjectSQL() {
        return loadObjectSQL;
    }
    /**
     * @return the saveObjectSQL
     */
    public String getSaveObjectSQL() {
        return saveObjectSQL;
    }
    /**
     * @return the deleteObjectSQL
     */
    public String getDeleteObjectSQL() {
        return deleteObjectSQL;
    }
    /**
     * @return the objectExistsSQL
     */
    public String getObjectExistsSQL() {
        return objectExistsSQL;
    }
    /**
     * @return the schemaSQL
     */
    public String getSchemaSQL() {
        return schemaSQL;
    }
    /**
     * @return the loadItSQL
     */
    public String getLoadObjectsSQL() {
        return loadObjectsSQL;
    }

    /**
     * @return the renameTableSQL
     */
    public String getRenameTableSQL() {
        return renameTableSQL;
    }

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @return the oldName
     */
    public String getOldTableName() {
        return oldTableName;
    }

    public boolean renameRequired() {
        return renameRequired;
    }

    /**
     * @return the useQuotes
     */
    public boolean isUseQuotes() {
        return useQuotes;
    }


}
