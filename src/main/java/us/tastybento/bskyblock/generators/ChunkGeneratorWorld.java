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

import us.tastybento.bskyblock.config.Settings;

/**
 * @author tastybento
 *         Creates the world
 */
public class ChunkGeneratorWorld extends ChunkGenerator {
    Random rand = new Random();
    PerlinOctaveGenerator gen;
    //BSkyBlock plugin = BSkyBlock.getPlugin();

    @SuppressWarnings("deprecation")
    public byte[][] generateBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        // Bukkit.getLogger().info("DEBUG: world environment = " + world.getEnvironment().toString());
        if (world.getEnvironment().equals(World.Environment.NETHER)) {
            return generateNetherBlockSections(world, random, chunkX, chunkZ, biomeGrid);
        }
        byte[][] result = new byte[world.getMaxHeight() / 16][];
        if (Settings.seaHeight == 0) {
            return result;
        } else {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < Settings.seaHeight; y++) {
                        setBlock(result, x, y, z, (byte) Material.STATIONARY_WATER.getId()); 
                    }
                }
            }
            return result;
        }
    }

    void setBlock(byte[][] result, int x, int y, int z, byte blkid) {
        // is this chunk part already initialized?
        if (result[y >> 4] == null) {
            // Initialize the chunk part
            result[y >> 4] = new byte[4096];
        }
        // set the block (look above, how this is done)
        result[y >> 4][((y & 0xF) << 8) | (z << 4) | x] = blkid;
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
    @SuppressWarnings("deprecation")
    private byte[][] generateNetherBlockSections(World world, Random random, int chunkX, int chunkZ, BiomeGrid biomeGrid) {
        // Bukkit.getLogger().info("DEBUG: world environment(nether) = " +
        // world.getEnvironment().toString());
        rand.setSeed(world.getSeed());
        gen = new PerlinOctaveGenerator((long) (random.nextLong() * random.nextGaussian()), 8);
        byte[][] result = new byte[world.getMaxHeight() / 16][];
        // This is a nether generator
        if (!world.getEnvironment().equals(Environment.NETHER)) {
            return result;
        }
        if (Settings.netherRoof) {
            // Make the roof - common across the world
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    // Do the ceiling
                    // Bukkit.getLogger().info("debug: " + x + ", " +
                    // (world.getMaxHeight()-1) + ", " + z);
                    int maxHeight = world.getMaxHeight();
                    setBlock(result, x, (maxHeight - 1), z, (byte) Material.BEDROCK.getId());
                    // Next three layers are a mix of bedrock and netherrack
                    for (int y = 2; y < 5; y++) {
                        double r = gen.noise(x, (maxHeight - y), z, 0.5, 0.5);
                        if (r > 0D) {
                            setBlock(result, x, (maxHeight - y), z, (byte) Material.BEDROCK.getId());
                        } else {
                            setBlock(result, x, (maxHeight - y), z, (byte) Material.NETHERRACK.getId());
                        }
                    }
                    // Next three layers are a mix of netherrack and air
                    for (int y = 5; y < 8; y++) {
                        double r = gen.noise(x, maxHeight - y, z, 0.5, 0.5);
                        if (r > 0D) {
                            setBlock(result, x, (maxHeight - y), z, (byte) Material.NETHERRACK.getId());
                        } else {
                            setBlock(result, x, (maxHeight - y), z, (byte) Material.AIR.getId());
                        }
                    }
                    // Layer 8 may be glowstone
                    double r = gen.noise(x, maxHeight - 8, z, random.nextFloat(), random.nextFloat());
                    if (r > 0.5D) {
                        // Have blobs of glowstone
                        switch (random.nextInt(4)) {
                        case 1:
                            // Single block
                            setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
                            if (x < 14 && z < 14) {
                                setBlock(result, x + 1, (maxHeight - 8), z + 1, (byte) Material.GLOWSTONE.getId());
                                setBlock(result, x + 2, (maxHeight - 8), z + 2, (byte) Material.GLOWSTONE.getId());
                                setBlock(result, x + 1, (maxHeight - 8), z + 2, (byte) Material.GLOWSTONE.getId());
                                setBlock(result, x + 1, (maxHeight - 8), z + 2, (byte) Material.GLOWSTONE.getId());
                            }
                            break;
                        case 2:
                            // Stalatite
                            for (int i = 0; i < random.nextInt(10); i++) {
                                setBlock(result, x, (maxHeight - 8 - i), z, (byte) Material.GLOWSTONE.getId());
                            }
                        case 3:
                            setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
                            if (x > 3 && z > 3) {
                                for (int xx = 0; xx < 3; xx++) {
                                    for (int zz = 0; zz < 3; zz++) {
                                        setBlock(result, x - xx, (maxHeight - 8 - random.nextInt(2)), z - xx, (byte) Material.GLOWSTONE.getId());
                                    }
                                }
                            }
                            break;
                        default:
                            setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
                        }
                        setBlock(result, x, (maxHeight - 8), z, (byte) Material.GLOWSTONE.getId());
                    } else {
                        setBlock(result, x, (maxHeight - 8), z, (byte) Material.AIR.getId());
                    }
                }
            }
        }
        return result;

    }
}