package us.tastybento.bskyblock.generators;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.util.noise.PerlinOctaveGenerator;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * @author tastybento
 *         Creates the world
 */
public class ChunkGeneratorWorld extends ChunkGenerator {

    BSkyBlock plugin;
    Random rand = new Random();
    PerlinOctaveGenerator gen;

    /**
     * @param plugin - BSkyBlock plugin object
     */
    public ChunkGeneratorWorld(BSkyBlock plugin) {
        super();
        this.plugin = plugin;
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int chunkX, int chunkZ, ChunkGenerator.BiomeGrid biomeGrid) {
        if (world.getEnvironment().equals(World.Environment.NETHER)) {
            return generateNetherChunks(world, random, chunkX, chunkZ, biomeGrid);
        }
        ChunkData result = createChunkData(world);
        if (plugin.getSettings().getSeaHeight() != 0) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < plugin.getSettings().getSeaHeight(); y++) {
                        result.setBlock(x, y, z, Material.STATIONARY_WATER);
                    }
                }
            }

        }
        return result;
    }

    // This needs to be set to return true to override minecraft's default
    // behavior
    @Override
    public boolean canSpawn(World world, int x, int z) {
        return true;
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(final World world) {
        return Arrays.asList(new BlockPopulator[0]);
    }

    /*
     * Nether Section
     */
    private ChunkData generateNetherChunks(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        ChunkData result = createChunkData(world);
        rand.setSeed(world.getSeed());
        gen = new PerlinOctaveGenerator((long) (random.nextLong() * random.nextGaussian()), 8);
        // This is a nether generator
        if (!world.getEnvironment().equals(Environment.NETHER)) {
            return result;
        }
        if (plugin.getSettings().isNetherRoof()) {
            // Make the roof - common across the world
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    // Do the ceiling
                    int maxHeight = world.getMaxHeight();
                    result.setBlock(x, (maxHeight - 1), z, Material.BEDROCK);
                    // Next three layers are a mix of bedrock and netherrack
                    for (int y = 2; y < 5; y++) {
                        double r = gen.noise(x, (maxHeight - y), z, 0.5, 0.5);
                        if (r > 0D) {
                            result.setBlock(x, (maxHeight - y), z, Material.BEDROCK);
                        }
                    }
                    // Next three layers are a mix of netherrack and air
                    for (int y = 5; y < 8; y++) {
                        double r = gen.noise(x, (double)maxHeight - y, z, 0.5, 0.5);
                        if (r > 0D) {
                            result.setBlock(x, (maxHeight - y), z, Material.NETHERRACK);
                        } else {
                            result.setBlock(x, (maxHeight - y), z, Material.AIR);
                        }
                    }
                    // Layer 8 may be glowstone
                    double r = gen.noise(x, (double)maxHeight - 8, z, random.nextFloat(), random.nextFloat());
                    if (r > 0.5D) {
                        // Have blobs of glowstone
                        switch (random.nextInt(4)) {
                        case 1:
                            // Single block
                            result.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                            if (x < 14 && z < 14) {
                                result.setBlock(x + 1, (maxHeight - 8), z + 1, Material.GLOWSTONE);
                                result.setBlock(x + 2, (maxHeight - 8), z + 2, Material.GLOWSTONE);
                                result.setBlock(x + 1, (maxHeight - 8), z + 2, Material.GLOWSTONE);
                                result.setBlock(x + 1, (maxHeight - 8), z + 2, Material.GLOWSTONE);
                            }
                            break;
                        case 2:
                            // Stalatite
                            for (int i = 0; i < random.nextInt(10); i++) {
                                result.setBlock(x, (maxHeight - 8 - i), z, Material.GLOWSTONE);
                            }
                            break;
                        case 3:
                            result.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                            if (x > 3 && z > 3) {
                                for (int xx = 0; xx < 3; xx++) {
                                    for (int zz = 0; zz < 3; zz++) {
                                        result.setBlock(x - xx, (maxHeight - 8 - random.nextInt(2)), z - xx, Material.GLOWSTONE);
                                    }
                                }
                            }
                            break;
                        default:
                            result.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                        }
                        result.setBlock(x, (maxHeight - 8), z, Material.GLOWSTONE);
                    } else {
                        result.setBlock(x, (maxHeight - 8), z, Material.AIR);
                    }
                }
            }
        }
        return result;

    }
}