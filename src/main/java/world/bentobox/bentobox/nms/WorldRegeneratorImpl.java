package world.bentobox.bentobox.nms;

import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.TaskScheduler;

/** Rebuilds custom island terrain without the removed World.regenerateChunk API. */
public class WorldRegeneratorImpl implements WorldRegenerator {

    @Override
    public CompletableFuture<Void> regenerateChunk(Chunk chunk) {
        var plugin = BentoBox.getInstance();
        World world = chunk.getWorld();
        int cx = chunk.getX();
        int cz = chunk.getZ();
        return TaskScheduler.regionFuture(plugin, new Location(world, cx << 4, 0, cz << 4), () -> {
            ChunkGenerator generator = plugin.getAddonsManager().getDefaultWorldGenerator(world.getName(), "");
            if (generator == null) throw new IllegalStateException("No custom generator for " + world.getName());
            try {
                if (generator.getClass().getMethod("generateChunkData", World.class, Random.class,
                        int.class, int.class, ChunkGenerator.BiomeGrid.class).getDeclaringClass() != ChunkGenerator.class) {
                    throw new UnsupportedOperationException("Legacy chunk generators cannot be safely regenerated");
                }
            } catch (NoSuchMethodException error) {
                throw new IllegalStateException(error);
            }
            var data = Bukkit.getServer().createChunkData(world);
            long stageSeed = cx * 341873128712L + cz * 132897987541L;
            // shouldGenerate* describes the server pipeline, not whether the generator owns the stage.
            if (overrides(generator, "generateNoise"))
                generator.generateNoise(world, new Random(stageSeed), cx, cz, data);
            if (overrides(generator, "generateSurface"))
                generator.generateSurface(world, new Random(stageSeed), cx, cz, data);
            if (overrides(generator, "generateBedrock"))
                generator.generateBedrock(world, new Random(stageSeed), cx, cz, data);
            if (overrides(generator, "generateCaves"))
                generator.generateCaves(world, new Random(world.getSeed()), cx, cz, data);

            var biomeProvider = generator.getDefaultBiomeProvider(world);
            var biomeData = new Biome[4][(world.getMaxHeight() - world.getMinHeight() + 3) / 4][4];
            if (biomeProvider != null) {
                for (int x = 0; x < 16; x += 4)
                    for (int y = world.getMinHeight(); y < world.getMaxHeight(); y += 4)
                        for (int z = 0; z < 16; z += 4)
                            biomeData[x / 4][(y - world.getMinHeight()) / 4][z / 4] =
                                    biomeProvider.getBiome(world, (cx << 4) + x, y, (cz << 4) + z);
            }
            for (int x = 0; x < 16; x++) {
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    for (int z = 0; z < 16; z++) {
                        var block = chunk.getBlock(x, y, z);
                        var replacement = data.getBlockData(x, y, z);
                        if (!block.getBlockData().equals(replacement)) block.setBlockData(replacement, false);
                        if (biomeProvider != null && x % 4 == 0 && (y - world.getMinHeight()) % 4 == 0 && z % 4 == 0)
                            block.setBiome(biomeData[x / 4][(y - world.getMinHeight()) / 4][z / 4]);
                    }
                }
            }
            var populators = new java.util.ArrayList<BlockPopulator>(generator.getDefaultPopulators(world));
            populators.addAll(world.getPopulators());
            var random = new Random(stageSeed);
            for (BlockPopulator populator : populators) populator.populate(world, random, chunk);
        });
    }

    private static boolean overrides(ChunkGenerator generator, String method) {
        try {
            return generator.getClass().getMethod(method, WorldInfo.class, Random.class,
                    int.class, int.class, ChunkGenerator.ChunkData.class).getDeclaringClass() != ChunkGenerator.class;
        } catch (NoSuchMethodException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Random flagRandom(World world, int x, int z, String stage) {
        // Minecraft's coordinate seed, kept local to avoid an NMS runtime dependency.
        long seed = (long) (x * 3129871) ^ z * 116129781L ^ stage.hashCode();
        seed = (seed * seed * 42317861L + seed * 11L) >> 16;
        return new Random(seed ^ world.getSeed());
    }
}
