/**
 * API to enable plugins to request data from addons.
 * <p>
 * Addons can expose data that they want to expose. To access it, call this class with the appropriate addon name,
 * the label for the data that is requested and if required, a map of key-value pairs that will be given to the addon.
 *
 * <b>Note</b> Since BentoBox 1.17.0, Addons can be declared as Pladdons and be loaded by the Bukkit classloader. This
 * enables Plugins to access Addon methods directly so this API is not required.
 * </p>
 *
 * @author tastybento
 */
package world.bentobox.bentobox.api.addons.request;