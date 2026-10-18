package world.bentobox.bentobox.nms;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.managers.AddonsManager;
import world.bentobox.bentobox.util.TaskScheduler;

class WorldRegeneratorImplTest extends CommonTestSetup {
    @Test
    void generatesCustomStagesOnOwnerWithoutRemovedApi() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        when(world.getMaxHeight()).thenReturn(4);
        Block block = mock(Block.class);
        when(chunk.getBlock(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.getBlockData()).thenReturn(mock(BlockData.class));
        BlockData replacement = mock(BlockData.class);
        var data = mock(ChunkGenerator.ChunkData.class);
        when(data.getBlockData(anyInt(), anyInt(), anyInt())).thenReturn(replacement);
        Server api = mock(Server.class);
        mockedBukkit.when(Bukkit::getServer).thenReturn(api);
        when(api.createChunkData(world)).thenReturn(data);
        var generator = new ChunkGenerator() {
            @Override
            public void generateNoise(WorldInfo info, Random random, int x, int z, ChunkData output) {
                assertSame(data, output);
            }
        };
        var addons = mock(AddonsManager.class);
        when(plugin.getAddonsManager()).thenReturn(addons);
        when(addons.getDefaultWorldGenerator(any(), anyString())).thenReturn(generator);
        try (var scheduler = mockStatic(TaskScheduler.class)) {
            scheduler.when(() -> TaskScheduler.regionFuture(eq(plugin), any(), any())).thenAnswer(call -> {
                call.getArgument(2, Runnable.class).run();
                return CompletableFuture.completedFuture(null);
            });
            new WorldRegeneratorImpl().regenerateChunk(chunk).join();
            verify(block, times(16 * 4 * 16)).setBlockData(replacement, false);
            verify(world, never()).regenerateChunk(anyInt(), anyInt());
            scheduler.verify(() -> TaskScheduler.regionFuture(eq(plugin), any(), any()));
        }
    }

    @Test
    void refusesLegacyGenerationBeforeChangingBlocks() {
        Chunk chunk = mock(Chunk.class);
        when(chunk.getWorld()).thenReturn(world);
        var addons = mock(AddonsManager.class);
        when(plugin.getAddonsManager()).thenReturn(addons);
        when(addons.getDefaultWorldGenerator(any(), anyString())).thenReturn(new ChunkGenerator() {
            @Override public ChunkData generateChunkData(org.bukkit.World ignored, Random random, int x, int z,
                    BiomeGrid biomeGrid) { return null; }
        });
        try (var scheduler = mockStatic(TaskScheduler.class)) {
            scheduler.when(() -> TaskScheduler.regionFuture(eq(plugin), any(), any())).thenAnswer(call -> {
                try {
                    call.getArgument(2, Runnable.class).run();
                    return CompletableFuture.completedFuture(null);
                } catch (Throwable error) {
                    return CompletableFuture.failedFuture(error);
                }
            });
            assertInstanceOf(UnsupportedOperationException.class,
                    assertThrows(CompletionException.class,
                            () -> new WorldRegeneratorImpl().regenerateChunk(chunk).join()).getCause());
            verify(chunk, never()).getBlock(anyInt(), anyInt(), anyInt());
        }
    }
}
