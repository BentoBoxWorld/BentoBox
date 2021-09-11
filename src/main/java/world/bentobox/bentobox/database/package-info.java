/**
 * Provides an abstract database for storing Java POJOs
 * and also YAML config files.
 *
 * <p>
 * The database supports concrete implementations for JSON flat file, MongoDB, MySQL, PostgreSQL, SQLite
 * and the ability to transition between them.
 * </p>
 * <p>
 * Storage of POJOs is done via GSON, i.e, the object is serialized and then stored. Each data object must
 * implement the DataObject interface, which requires a uniqueId field. This is what is used for indexing
 * and finding.
 * </p>
 * <p>
 * Performance with JSON is generally very good, and the other databases are really there for concurrent usage
 * by other applications.
 * </p>
 * @author tastybento
 *
 */
package world.bentobox.bentobox.database;