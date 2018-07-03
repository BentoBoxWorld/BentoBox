package us.tastybento.bskyblock.island.builders;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.World.Environment;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Generates islands
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandBuilder {

    private Island island;
    private World world;
    private Environment type = Environment.NORMAL;
    private BSkyBlock plugin;
    private Runnable task;

    public IslandBuilder(BSkyBlock plugin, Island island) {
        this.plugin = plugin;
        this.island = island;
        world = island.getWorld();
    }

    /**
     * @param type the type to set
     */
    public IslandBuilder setType(Environment type) {
        this.type = type;
        return this;
    }

    /**
     * The task to run when the island is built
     * @param task
     * @return IslandBuilder
     */
    public IslandBuilder run(Runnable task) {
        this.task = task;
        return this;
    }

    public void build() {
        plugin.log("Pasting island to " + type);
        // Switch on island type
        switch (type) {
        case NETHER:
            world = Bukkit.getWorld(island.getWorld().getName() + "_nether");
            if (world == null) {
                return;
            }
            break;
        case THE_END:
            world = Bukkit.getWorld(island.getWorld().getName() + "_the_end");
            if (world == null) {
                return;
            }
            break;
        default:
            break;
        }
        plugin.getSchemsManager().paste(world, island, task);
    }

}


