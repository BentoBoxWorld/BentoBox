package world.bentobox.bentobox.api.hooks;

import java.util.List;
import java.util.Map;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * NPC Hooks
 * @author tastybento
 * @since 3.2.0
 */
public abstract class NPCHook extends Hook {

    protected NPCHook(@NonNull String pluginName, @NonNull Material icon) {
        super(pluginName, icon);
    }

    public abstract boolean spawnNpc(String yaml, Location pos) throws InvalidConfigurationException;

    public abstract Map<? extends Vector, ? extends List<BlueprintEntity>> getNpcsInArea(World world,
            List<Vector> vectorsToCopy, @Nullable Vector origin);

    /**
     * Remove all NPCs in chunk
     * @param chunk chunk
     */
    public abstract void removeNPCsInChunk(Chunk chunk);

}
