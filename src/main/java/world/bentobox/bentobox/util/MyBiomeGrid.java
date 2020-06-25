package world.bentobox.bentobox.util;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.generator.ChunkGenerator.BiomeGrid;
import org.bukkit.util.Vector;

/**
 * A biome grid for generators
 * @author tastybento
 *
 */
public class MyBiomeGrid implements BiomeGrid {
    Map<Vector, Biome> map = new HashMap<>();
    private Biome defaultBiome;
    public MyBiomeGrid(Environment environment) {
        switch(environment) {
        case NETHER:
            try {
                // 1.16 onwards
                defaultBiome = Biome.valueOf("NETHER_WASTES");
            } catch (Exception e) {
                // Before 1.16
                defaultBiome = Biome.valueOf("NETHER");
            }
            break;
        case THE_END:
            defaultBiome = Biome.THE_END;
            break;
        default:
            defaultBiome = Biome.PLAINS;
            break;
        }

    }
    @Override
    public Biome getBiome(int x, int z) {
        return map.getOrDefault(new Vector(x,0,z), defaultBiome);
    }
    @Override
    public void setBiome(int x, int z, Biome bio) {
        map.put(new Vector(x,0,z), bio);
    }
    @Override
    public Biome getBiome(int x, int y, int z) {
        return map.getOrDefault(new Vector(x,y,z), defaultBiome);
    }
    @Override
    public void setBiome(int x, int y, int z, Biome bio) {
        map.put(new Vector(x, y, z), bio);

    }
}