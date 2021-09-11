/**
 * These are GSON adapters used to serialize and deserialize various data types.
 * <p>
 * The {@link world.bentobox.bentobox.database.json.adapters.BukkitObjectTypeAdapter}
 * is a catch-all adapter that uses the built-in Bukkit serialization capabilities. Before
 * we knew about this, there were other ones built, like for Location, that have to remain
 * for backwards compatibility reasons.
 * </p>
 * @author tastybento
 *
 */
package world.bentobox.bentobox.database.json.adapters;