package world.bentobox.bentobox.database.sql;

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

    /**
     * @param canonicalName - canonical name of the class being stored.
     */
    public SQLConfiguration(String canonicalName) {
        schemaSQL = "CREATE TABLE IF NOT EXISTS `" + canonicalName +
                "` (json JSON, uniqueId VARCHAR(255) GENERATED ALWAYS AS (json->\"$.uniqueId\"), UNIQUE INDEX i (uniqueId) )";
        loadObjectsSQL = "SELECT `json` FROM `" + canonicalName + "`";
        loadObjectSQL = "SELECT `json` FROM `" + canonicalName + "` WHERE uniqueId = ? LIMIT 1";
        saveObjectSQL = "INSERT INTO `" + canonicalName + "` (json) VALUES (?) ON DUPLICATE KEY UPDATE json = ?";
        deleteObjectSQL = "DELETE FROM `" + canonicalName + "` WHERE uniqueId = ?";
        objectExistsSQL = "SELECT IF ( EXISTS( SELECT * FROM `" + canonicalName + "` WHERE `uniqueId` = ?), 1, 0)";
    }

    public SQLConfiguration loadObject(String string) {
        this.loadObjectSQL = string;
        return this;
    }

    public SQLConfiguration saveObject(String string) {
        this.saveObjectSQL = string;
        return this;
    }

    public SQLConfiguration deleteObject(String string) {
        this.deleteObjectSQL = string;
        return this;
    }

    public SQLConfiguration objectExists(String string) {
        this.objectExistsSQL = string;
        return this;
    }

    public SQLConfiguration schema(String string) {
        this.schemaSQL = string;
        return this;
    }

    public SQLConfiguration loadObjects(String string) {
        this.loadObjectsSQL = string;
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

}
