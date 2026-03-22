package world.bentobox.bentobox.hooks;

import org.bukkit.World;

/**
 * Hook for a type of Multi-World management plugin that must be made
 * aware of the correct configuration of a BentoBox World.
 *
 * @author bergerkiller (Irmo van den Berge)
 */
public interface WorldManagementHook {

    /**
     * Register the world with the World Management hook
     *
     *
     * @param world - world to register
     * @param islandWorld - if true, then this is an island world
     */
    void registerWorld(World world, boolean islandWorld);

    /**
     * Unregisters a world.
     * @param world - world to unregister
     */
    default void unregisterWorld(World world) {
        // Do nothing
    }
}
