package world.bentobox.bentobox.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

/**
 * A biome grid for generators
 * @author tastybento
 *
 */
@SuppressWarnings("deprecation")
public class MyBiomeGrid implements BiomeGrid {
    Map<Vector, Biome> map = new HashMap<>();
    private Biome defaultBiome;
    public MyBiomeGrid(Environment environment) {
        switch (environment) {
        case NETHER -> defaultBiome = Biome.NETHER_WASTES;
        case THE_END -> defaultBiome = Biome.THE_END;
        default -> defaultBiome = Biome.PLAINS;
        }

    }
    @Override
    public @NonNull Biome getBiome(int x, int z) {
        return map.getOrDefault(new Vector(x,0,z), defaultBiome);
    }
    @Override
    public void setBiome(int x, int z, @NonNull Biome bio) {
        map.put(new Vector(x,0,z), bio);
    }
    @Override
    public @NonNull Biome getBiome(int x, int y, int z) {
        return map.getOrDefault(new Vector(x,y,z), defaultBiome);
    }
    @Override
    public void setBiome(int x, int y, int z, @NonNull Biome bio) {
        map.put(new Vector(x, y, z), bio);

    }
}