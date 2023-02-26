package world.bentobox.bentobox.nms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Colorable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.eclipse.jdt.annotation.Nullable;

import io.papermc.lib.PaperLib;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.database.objects.IslandDeletion;
import world.bentobox.bentobox.util.MyBiomeGrid;

/**
 * Regenerates by using a seed world. The seed world is created using the same generator as the game
 * world so that features created by methods like generateNoise or generateCaves can be regenerated.
 * @author tastybento
 *
 */
public abstract class CopyWorldRegenerator implements WorldRegenerator {
    private final BentoBox plugin;

    protected CopyWorldRegenerator() {
        this.plugin = BentoBox.getInstance();
    }

    /**
     * Update the low-level chunk information for the given block to the new block ID and data.  This
     * change will not be propagated to clients until the chunk is refreshed to them.
     *
     * @param chunk        - chunk to be changed
     * @param x            - x coordinate within chunk 0 - 15
     * @param y            - y coordinate within chunk 0 - world height, e.g. 255
     * @param z            - z coordinate within chunk 0 - 15
     * @param blockData    - block data to set the block
     * @param applyPhysics - apply physics or not
     */
    protected abstract void setBlockInNativeChunk(Chunk chunk, int x, int y, int z, BlockData blockData, boolean applyPhysics);

    @Override
    public CompletableFuture<Void> regenerate(GameModeAddon gm, IslandDeletion di, World world) {
        return gm.isUsesNewChunkGeneration() ? regenerateCopy(gm, di, world) : regenerateSimple(gm, di, world);
    }
    
    public CompletableFuture<Void> regenerateCopy(GameModeAddon gm, IslandDeletion di, World world) {
        CompletableFuture<Void> bigFuture = new CompletableFuture<>();
        new BukkitRunnable() {
            private int chunkX = di.getMinXChunk();
            private int chunkZ = di.getMinZChunk();
            CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);

            @Override
            public void run() {
                if (!currentTask.isDone()) return;
                if (isEnded(chunkX)) {
                    cancel();
                    bigFuture.complete(null);
                    return;
                }
                List<CompletableFuture<Void>> newTasks = new ArrayList<>();
                for (int i = 0; i < plugin.getSettings().getDeleteSpeed(); i++) {
                    if (isEnded(chunkX)) {
                        break;
                    }
                    final int x = chunkX;
                    final int z = chunkZ;
                    newTasks.add(regenerateChunk(di, world, x, z));
                    chunkZ++;
                    if (chunkZ > di.getMaxZChunk()) {
                        chunkZ = di.getMinZChunk();
                        chunkX++;
                    }
                }
                currentTask = CompletableFuture.allOf(newTasks.toArray(new CompletableFuture[0]));
            }

            private boolean isEnded(int chunkX) {
                return chunkX > di.getMaxXChunk();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return bigFuture;
    }

    @Override
    public CompletableFuture<Void> regenerateChunk(Chunk chunk) {
        return regenerateChunk(null, chunk.getWorld(), chunk.getX(), chunk.getZ()); 
    }

    private CompletableFuture<Void> regenerateChunk(@Nullable IslandDeletion di, World world, int chunkX, int chunkZ) {
        CompletableFuture<Chunk> seedWorldFuture = getSeedWorldChunk(world, chunkX, chunkZ);

        // Set up a future to get the chunk requests using Paper's Lib. If Paper is used, this should be done async
        CompletableFuture<Chunk> chunkFuture = PaperLib.getChunkAtAsync(world, chunkX, chunkZ);

        // If there is no island, do not clean chunk
        CompletableFuture<Void> cleanFuture = di != null ? cleanChunk(chunkFuture, di) : CompletableFuture.completedFuture(null);

        CompletableFuture<Void> copyFuture = CompletableFuture.allOf(cleanFuture, chunkFuture, seedWorldFuture);

        copyFuture.thenRun(() -> {

            try {
                Chunk chunkTo = chunkFuture.get();
                Chunk chunkFrom = seedWorldFuture.get();
                copyChunkDataToChunk(chunkTo, chunkFrom, di != null ? di.getBox() : null);

            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();                
            }
        });
        return CompletableFuture.allOf(cleanFuture, copyFuture);
    }

    private CompletableFuture<Chunk> getSeedWorldChunk(World world, int chunkX, int chunkZ) {
        World seed = Bukkit.getWorld(world.getName() + "/bentobox");
        if (seed == null) return CompletableFuture.completedFuture(null);
        return PaperLib.getChunkAtAsync(seed, chunkX, chunkZ);
    }

    /**
     * Cleans up the chunk of inventories and entities
     * @param chunkFuture the future chunk to be cleaned
     * @param di island deletion data
     * @return future completion of this task
     */
    private CompletableFuture<Void> cleanChunk(CompletableFuture<Chunk> chunkFuture, IslandDeletion di) {
        // when it is complete, then run through all the tile entities in the chunk and clear them, e.g., chests are emptied
        CompletableFuture<Void> invFuture = chunkFuture.thenAccept(chunk ->
        Arrays.stream(chunk.getTileEntities()).filter(InventoryHolder.class::isInstance)
        .filter(te -> di.inBounds(te.getLocation().getBlockX(), te.getLocation().getBlockZ()))
        .forEach(te -> ((InventoryHolder) te).getInventory().clear())
                );

        // Similarly, when the chunk is loaded, remove all the entities in the chunk apart from players
        CompletableFuture<Void> entitiesFuture = chunkFuture.thenAccept(chunk -> 
        // Remove all entities in chunk, including any dropped items as a result of clearing the blocks above
        Arrays.stream(chunk.getEntities())
        .filter(e -> !(e instanceof Player) && di.inBounds(e.getLocation().getBlockX(), e.getLocation().getBlockZ()))
        .forEach(Entity::remove));
        return CompletableFuture.allOf(invFuture, entitiesFuture);
    }

    /**
     * Copies a chunk to another chunk
     * @param toChunk - chunk to be copied into
     * @param fromChunk - chunk to be copied from
     * @param limitBox - limit box that the chunk needs to be in
     */
    private void copyChunkDataToChunk(Chunk toChunk, Chunk fromChunk, BoundingBox limitBox) {
        double baseX = toChunk.getX() << 4;
        double baseZ = toChunk.getZ() << 4;
        int minHeight = toChunk.getWorld().getMinHeight();
        int maxHeight = toChunk.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (limitBox != null && !limitBox.contains(baseX + x, 0, baseZ + z)) {
                    continue;
                }
                for (int y = minHeight; y < maxHeight; y++) {
                    setBlockInNativeChunk(toChunk, x, y, z, fromChunk.getBlock(x, y, z).getBlockData(), false);
                    // 3D biomes, 4 blocks separated
                    if (x % 4 == 0 && y % 4 == 0 && z % 4 == 0) {
                        toChunk.getBlock(x, y, z).setBiome(fromChunk.getBlock(x, y, z).getBiome());
                    }
                }
            }
        }
        // Entities
        Arrays.stream(fromChunk.getEntities()).forEach(e -> processEntity(e, e.getLocation().toVector().toLocation(toChunk.getWorld())));

        // Tile Entities
        Arrays.stream(fromChunk.getTileEntities()).forEach(bs -> processTileEntity(bs.getBlock(), bs.getLocation().toVector().toLocation(toChunk.getWorld()).getBlock()));
    }

    private void processEntity(Entity entity, Location location) {
            Entity bpe = location.getWorld().spawnEntity(location, entity.getType());
            bpe.setCustomName(entity.getCustomName());
            if (entity instanceof Villager villager && bpe instanceof Villager villager2) {
                setVillager(villager, villager2);
            }
            if (entity instanceof Colorable c && bpe instanceof Colorable cc) {
                if (c.getColor() != null) {
                    cc.setColor(c.getColor());
                }
            }
            if (entity instanceof Tameable t && bpe instanceof Tameable tt) {
                tt.setTamed(t.isTamed());
            }
            if (entity instanceof ChestedHorse ch && bpe instanceof ChestedHorse ch2) {
                ch2.setCarryingChest(ch.isCarryingChest());
            }
            // Only set if child. Most animals are adults
            if (entity instanceof Ageable a && bpe instanceof Ageable aa) {
                if (a.isAdult()) aa.setAdult();
            }
            if (entity instanceof AbstractHorse horse && bpe instanceof AbstractHorse horse2) {
                horse2.setDomestication(horse.getDomestication());
                horse2.getInventory().setContents(horse.getInventory().getContents());
            }

            if (entity instanceof Horse horse && bpe instanceof Horse horse2) {
                horse2.setStyle(horse.getStyle());
            }
        }

    /**
     * Set the villager stats
     * @param v - villager
     * @param villager2 villager
     */
    private void setVillager(Villager v, Villager villager2) {
        villager2.setVillagerExperience(v.getVillagerExperience());
        villager2.setVillagerLevel(v.getVillagerLevel());
        villager2.setProfession(v.getProfession());
        villager2.setVillagerType(v.getVillagerType());
    }
    
    private void processTileEntity(Block fromBlock, Block toBlock) {
        // Block state
        BlockState blockState = fromBlock.getState();
        BlockState b = toBlock.getState();

        // Signs
        if (blockState instanceof Sign fromSign && b instanceof Sign toSign) {
            int i = 0;
            for (String line : fromSign.getLines()) {
                toSign.setLine(i++, line);  
            }
            toSign.setGlowingText(fromSign.isGlowingText());
        }
        // Chests
        else if (blockState instanceof InventoryHolder ih && b instanceof InventoryHolder toChest) {
            toChest.getInventory().setContents(ih.getInventory().getContents());
        }
        // Spawner type
        else if (blockState instanceof CreatureSpawner spawner && b instanceof CreatureSpawner toSpawner) {
            toSpawner.setSpawnedType(spawner.getSpawnedType());
        }

        // Banners
        else if (blockState instanceof Banner banner && b instanceof Banner toBanner) {
            toBanner.setBaseColor(banner.getBaseColor());
            toBanner.setPatterns(banner.getPatterns());
        }
    }
    

    public CompletableFuture<Void> regenerateSimple(GameModeAddon gm, IslandDeletion di, World world) {
        CompletableFuture<Void> bigFuture = new CompletableFuture<>();
        new BukkitRunnable() {
            private int chunkX = di.getMinXChunk();
            private int chunkZ = di.getMinZChunk();
            CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);

            @Override
            public void run() {
                if (!currentTask.isDone()) return;
                if (isEnded(chunkX)) {
                    cancel();
                    bigFuture.complete(null);
                    return;
                }
                List<CompletableFuture<Void>> newTasks = new ArrayList<>();
                for (int i = 0; i < plugin.getSettings().getDeleteSpeed(); i++) {
                    if (isEnded(chunkX)) {
                        break;
                    }
                    final int x = chunkX;
                    final int z = chunkZ;
                    newTasks.add(regenerateChunk(gm, di, world, x, z));
                    chunkZ++;
                    if (chunkZ > di.getMaxZChunk()) {
                        chunkZ = di.getMinZChunk();
                        chunkX++;
                    }
                }
                currentTask = CompletableFuture.allOf(newTasks.toArray(new CompletableFuture[0]));
            }

            private boolean isEnded(int chunkX) {
                return chunkX > di.getMaxXChunk();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return bigFuture;
    }

    @SuppressWarnings("deprecation")
    private CompletableFuture<Void> regenerateChunk(GameModeAddon gm, IslandDeletion di, World world, int chunkX, int chunkZ) {
        CompletableFuture<Chunk> chunkFuture = PaperLib.getChunkAtAsync(world, chunkX, chunkZ);
        CompletableFuture<Void> invFuture = chunkFuture.thenAccept(chunk ->
        Arrays.stream(chunk.getTileEntities()).filter(InventoryHolder.class::isInstance)
        .filter(te -> di.inBounds(te.getLocation().getBlockX(), te.getLocation().getBlockZ()))
        .forEach(te -> ((InventoryHolder) te).getInventory().clear())
                );
        CompletableFuture<Void> entitiesFuture = chunkFuture.thenAccept(chunk -> {
            for (Entity e : chunk.getEntities()) {
                if (!(e instanceof Player)) {
                    e.remove();
                }
            }
        });
        CompletableFuture<Chunk> copyFuture = chunkFuture.thenApply(chunk -> {
            // Reset blocks
            MyBiomeGrid grid = new MyBiomeGrid(chunk.getWorld().getEnvironment());
            ChunkGenerator cg = gm.getDefaultWorldGenerator(chunk.getWorld().getName(), "delete");
            // Will be null if use-own-generator is set to true
            if (cg != null) {
                ChunkGenerator.ChunkData cd = cg.generateChunkData(chunk.getWorld(), new Random(), chunk.getX(), chunk.getZ(), grid);
                copyChunkDataToChunk(chunk, cd, grid, di.getBox());
            }
            return chunk;
        });
        CompletableFuture<Void> postCopyFuture = copyFuture.thenAccept(chunk ->
        // Remove all entities in chunk, including any dropped items as a result of clearing the blocks above
        Arrays.stream(chunk.getEntities()).filter(e -> !(e instanceof Player) && di.inBounds(e.getLocation().getBlockX(), e.getLocation().getBlockZ())).forEach(Entity::remove));
        return CompletableFuture.allOf(invFuture, entitiesFuture, postCopyFuture);
    }

    @SuppressWarnings("deprecation")
    private void copyChunkDataToChunk(Chunk chunk, ChunkGenerator.ChunkData chunkData, ChunkGenerator.BiomeGrid biomeGrid, BoundingBox limitBox) {
        double baseX = chunk.getX() << 4;
        double baseZ = chunk.getZ() << 4;
        int minHeight = chunk.getWorld().getMinHeight();
        int maxHeight = chunk.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                if (!limitBox.contains(baseX + x, 0, baseZ + z)) {
                    continue;
                }
                for (int y = minHeight; y < maxHeight; y++) {
                    setBlockInNativeChunk(chunk, x, y, z, chunkData.getBlockData(x, y, z), false);
                    // 3D biomes, 4 blocks separated
                    if (x % 4 == 0 && y % 4 == 0 && z % 4 == 0) {
                        chunk.getBlock(x, y, z).setBiome(biomeGrid.getBiome(x, y, z));
                    }
                }
            }
        }
    }
}
