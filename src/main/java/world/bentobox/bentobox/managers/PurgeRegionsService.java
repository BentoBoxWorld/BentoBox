package world.bentobox.bentobox.managers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.IslandGrid;
import world.bentobox.bentobox.managers.island.IslandGrid.IslandData;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.level.Level;

/**
 * Core implementation of the "purge region files" operation shared by the
 * {@code /bbox admin purge regions} command and the periodic
 * {@link HousekeepingManager} auto-purge task.
 *
 * <p>All public methods perform blocking disk I/O and must be called from an
 * async thread. The service does not interact with players or issue
 * confirmations — the caller is responsible for any user-facing UX.
 *
 * <p>Extracted from {@code AdminPurgeRegionsCommand} so the command and the
 * scheduler can share a single code path for scanning, filtering, and
 * deleting region files across the overworld + optional nether/end
 * dimensions.
 *
 * @since 3.14.0
 */
public class PurgeRegionsService {

    private static final String REGION = "region";
    private static final String ENTITIES = "entities";
    private static final String POI = "poi";
    private static final String DIM_1 = "DIM-1";
    private static final String DIM1 = "DIM1";
    private static final String PLAYERS = "players";
    private static final String PLAYERDATA = "playerdata";
    private static final String EXISTS_PREFIX = " (exists=";
    private static final String PURGE_FOUND = "Purge found ";

    private final BentoBox plugin;

    /**
     * Island IDs whose region files were deleted by a deleted-sweep
     * ({@code days == 0}) but whose DB rows are deferred until plugin
     * shutdown. Paper's internal chunk cache may still serve stale block
     * data even after the {@code .mca} file is gone from disk; only a
     * clean shutdown guarantees the cache is cleared.
     */
    private final Set<String> pendingDeletions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public PurgeRegionsService(BentoBox plugin) {
        this.plugin = plugin;
    }

    /**
     * Result of a purge scan — a map of deletable region coordinates to the
     * set of island IDs in each region, plus filtering statistics.
     *
     * @param world the world scanned
     * @param days  the age cutoff (days) used
     * @param deleteableRegions regions considered deletable keyed by region
     *                          coordinate {@code (regionX, regionZ)}
     * @param isNether whether the nether dimension was included
     * @param isEnd    whether the end dimension was included
     * @param stats    filter statistics for logging/reporting
     */
    public record PurgeScanResult(
            World world,
            int days,
            Map<Pair<Integer, Integer>, Set<String>> deleteableRegions,
            boolean isNether,
            boolean isEnd,
            FilterStats stats) {
        public boolean isEmpty() {
            return deleteableRegions.isEmpty();
        }

        public int uniqueIslandCount() {
            Set<String> ids = new HashSet<>();
            deleteableRegions.values().forEach(ids::addAll);
            return ids.size();
        }
    }

    /** Tracks island-level and region-level block counts during filtering. */
    public record FilterStats(int islandsOverLevel, int islandsPurgeProtected,
            int regionsBlockedByLevel, int regionsBlockedByProtection) {}

    /** Groups the three folder types (region, entities, poi) for one world dimension. */
    private record DimFolders(File region, File entities, File poi) {}

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Scans the given world for islands flagged as {@code deletable} and
     * returns the set of region files that can be reaped immediately,
     * ignoring region-file age.
     *
     * <p>Unlike {@link #scan(World, int)} this does not look at region
     * timestamps at all: the {@code deletable} flag is the sole source of
     * truth. A region is only returned if <em>every</em> island that
     * overlaps it is deletable — a lone active neighbour blocks the whole
     * region.
     *
     * <p>The returned {@link PurgeScanResult} uses {@code days = 0} as a
     * sentinel meaning "no age filter" so that {@link #delete(PurgeScanResult)}
     * and {@code deleteRegionFiles} skip their freshness re-check.
     *
     * <p>Runs synchronously on the calling thread and performs disk I/O.
     * Callers must invoke this from an async task.
     *
     * @param world the gamemode overworld to scan
     * @return scan result, never {@code null}
     * @since 3.14.0
     */
    public PurgeScanResult scanDeleted(World world) {
        boolean isNether = plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world);
        boolean isEnd = plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world);

        IslandGrid islandGrid = plugin.getIslands().getIslandCache().getIslandGrid(world);
        if (islandGrid == null) {
            return new PurgeScanResult(world, 0, new HashMap<>(), isNether, isEnd,
                    new FilterStats(0, 0, 0, 0));
        }

        // Collect candidate region coords from every deletable island's
        // protection bounds. A single island may straddle multiple regions.
        Set<Pair<Integer, Integer>> candidateRegions = new HashSet<>();
        for (Island island : plugin.getIslands().getIslandCache().getIslands(world)) {
            if (!island.isDeletable()) continue;
            int minRX = island.getMinProtectedX() >> 9;
            int maxRX = (island.getMaxProtectedX() - 1) >> 9;
            int minRZ = island.getMinProtectedZ() >> 9;
            int maxRZ = (island.getMaxProtectedZ() - 1) >> 9;
            for (int rx = minRX; rx <= maxRX; rx++) {
                for (int rz = minRZ; rz <= maxRZ; rz++) {
                    candidateRegions.add(new Pair<>(rx, rz));
                }
            }
        }
        plugin.log("Purge deleted-sweep: " + candidateRegions.size()
                + " candidate region(s) from deletable islands in world " + world.getName());

        Map<Pair<Integer, Integer>, Set<String>> deleteableRegions =
                mapIslandsToRegions(new ArrayList<>(candidateRegions), islandGrid);
        FilterStats stats = filterForDeletedSweep(deleteableRegions);
        logFilterStats(stats);
        return new PurgeScanResult(world, 0, deleteableRegions, isNether, isEnd, stats);
    }

    /**
     * Scans the given world (and its nether/end if the gamemode owns them)
     * for region files older than {@code days} and returns the set of
     * regions whose overlapping islands may all be safely deleted.
     *
     * <p>Runs synchronously on the calling thread and performs disk I/O.
     * Callers must invoke this from an async task.
     *
     * @param world the gamemode overworld to scan
     * @param days  minimum age in days for region files to be candidates
     * @return scan result, never {@code null}
     */
    public PurgeScanResult scan(World world, int days) {
        boolean isNether = plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world);
        boolean isEnd = plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world);

        IslandGrid islandGrid = plugin.getIslands().getIslandCache().getIslandGrid(world);
        if (islandGrid == null) {
            return new PurgeScanResult(world, days, new HashMap<>(), isNether, isEnd,
                    new FilterStats(0, 0, 0, 0));
        }

        List<Pair<Integer, Integer>> oldRegions = findOldRegions(world, days, isNether, isEnd);
        Map<Pair<Integer, Integer>, Set<String>> deleteableRegions = mapIslandsToRegions(oldRegions, islandGrid);
        FilterStats stats = filterNonDeletableRegions(deleteableRegions, days);
        logFilterStats(stats);
        return new PurgeScanResult(world, days, deleteableRegions, isNether, isEnd, stats);
    }

    /**
     * Deletes the region files identified by a prior {@link #scan(World, int)}
     * along with any island database entries, island cache entries, and
     * orphaned player data files that correspond to them.
     *
     * <p>Runs synchronously on the calling thread and performs disk I/O.
     * Callers must invoke this from an async task. Callers are also
     * responsible for flushing in-memory chunk state by calling
     * {@code World.save()} on the main thread <b>before</b> dispatching
     * this method — {@code World.save()} is not safe to invoke from an
     * async thread.
     *
     * @param scan the prior scan result
     * @return {@code true} if all file deletions succeeded; {@code false} if
     *         any file was unexpectedly fresh or could not be deleted
     */
    public boolean delete(PurgeScanResult scan) {
        if (scan.deleteableRegions().isEmpty()) {
            return false;
        }
        plugin.log("Now deleting region files for world " + scan.world().getName());
        boolean ok = deleteRegionFiles(scan);
        if (!ok) {
            plugin.logError("Not all region files could be deleted");
        }

        // Collect unique island IDs across all reaped regions. An island
        // that spans multiple regions will only be considered once here.
        Set<String> affectedIds = new HashSet<>();
        for (Set<String> islandIDs : scan.deleteableRegions().values()) {
            affectedIds.addAll(islandIDs);
        }

        int islandsRemoved = 0;
        int islandsDeferred = 0;
        for (String islandID : affectedIds) {
            Optional<Island> opt = plugin.getIslands().getIslandById(islandID);
            if (opt.isEmpty()) {
                continue;
            }
            Island island = opt.get();

            if (scan.days() == 0) {
                // Deleted sweep: region files are gone from disk but Paper
                // may still serve stale chunk data from its internal memory
                // cache. Defer DB row removal to plugin shutdown when the
                // cache is guaranteed clear.
                pendingDeletions.add(islandID);
                islandsDeferred++;
                plugin.log("Island ID " + islandID
                        + " region files deleted \u2014 DB row deferred to shutdown");
            } else {
                // Age sweep: regions are old enough that Paper won't have
                // them cached. Gate on residual-region completeness check
                // to avoid orphaning blocks when the strict filter blocked
                // some of the island's regions.
                List<Pair<Integer, Integer>> residual = findResidualRegions(island, scan.world());
                if (!residual.isEmpty()) {
                    islandsDeferred++;
                    plugin.log("Island ID " + islandID + " has " + residual.size()
                            + " residual region(s) still on disk: " + residual
                            + " \u2014 DB row retained for a future purge");
                    continue;
                }
                deletePlayerFromWorldFolder(scan.world(), islandID, scan.deleteableRegions(), scan.days());
                plugin.getIslands().getIslandCache().deleteIslandFromCache(islandID);
                if (plugin.getIslands().deleteIslandId(islandID)) {
                    plugin.log("Island ID " + islandID + " deleted from cache and database");
                    islandsRemoved++;
                }
            }
        }
        plugin.log("Purge complete for world " + scan.world().getName()
                + ": " + scan.deleteableRegions().size() + " region(s), "
                + islandsRemoved + " island(s) removed, "
                + islandsDeferred + " island(s) deferred"
                + (scan.days() == 0 ? " (to shutdown)" : " (partial cleanup)"));
        return ok;
    }

    /**
     * Processes all island IDs whose region files were deleted by a prior
     * deleted-sweep but whose DB rows were deferred because Paper's internal
     * memory cache may still serve stale chunk data. Call this on plugin
     * shutdown when the cache is guaranteed to be cleared.
     *
     * <p>If the server crashes before a clean shutdown, the pending set is
     * lost — the islands stay {@code deletable=true} in the database and the
     * next purge cycle will pick them up again (safe failure mode).
     */
    public void flushPendingDeletions() {
        if (pendingDeletions.isEmpty()) {
            return;
        }
        plugin.log("Flushing " + pendingDeletions.size() + " deferred island deletion(s)...");
        int count = 0;
        for (String islandID : pendingDeletions) {
            plugin.getIslands().getIslandCache().deleteIslandFromCache(islandID);
            if (plugin.getIslands().deleteIslandId(islandID)) {
                count++;
            }
        }
        pendingDeletions.clear();
        plugin.log("Flushed " + count + " island(s) from cache and database");
    }

    /**
     * Returns an unmodifiable view of the island IDs currently pending
     * DB deletion (deferred to shutdown). Primarily for testing.
     */
    public Set<String> getPendingDeletions() {
        return Collections.unmodifiableSet(pendingDeletions);
    }

    /**
     * Returns the region coordinates for every {@code r.X.Z.mca} file still
     * present on disk that overlaps the island's protection box, across the
     * overworld and (if the gamemode owns them) the nether and end
     * dimensions. An empty list means every region file the island touches
     * is gone from disk and the island DB row can safely be reaped.
     *
     * <p>The protection box is converted to region coordinates with
     * {@code blockX >> 9} (each .mca covers a 512×512 block square). The
     * maximum bound is inclusive at the block level so we shift
     * {@code max - 1} to avoid picking up a neighbour region when the
     * protection ends exactly on a region boundary.
     */
    private List<Pair<Integer, Integer>> findResidualRegions(Island island, World overworld) {
        int rxMin = island.getMinProtectedX() >> 9;
        int rxMax = (island.getMaxProtectedX() - 1) >> 9;
        int rzMin = island.getMinProtectedZ() >> 9;
        int rzMax = (island.getMaxProtectedZ() - 1) >> 9;

        File base = overworld.getWorldFolder();
        File overworldRegionDir = new File(base, REGION);

        World netherWorld = plugin.getIWM().getNetherWorld(overworld);
        File netherRegionDir = plugin.getIWM().isNetherIslands(overworld)
                ? new File(netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(base), REGION)
                : null;

        World endWorld = plugin.getIWM().getEndWorld(overworld);
        File endRegionDir = plugin.getIWM().isEndIslands(overworld)
                ? new File(endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(base), REGION)
                : null;

        List<Pair<Integer, Integer>> residual = new ArrayList<>();
        for (int rx = rxMin; rx <= rxMax; rx++) {
            for (int rz = rzMin; rz <= rzMax; rz++) {
                String name = "r." + rx + "." + rz + ".mca";
                if (regionFileExists(overworldRegionDir, name)
                        || regionFileExists(netherRegionDir, name)
                        || regionFileExists(endRegionDir, name)) {
                    residual.add(new Pair<>(rx, rz));
                }
            }
        }
        return residual;
    }

    private static boolean regionFileExists(File dir, String name) {
        return dir != null && new File(dir, name).exists();
    }

    // ---------------------------------------------------------------
    // Chunk eviction
    // ---------------------------------------------------------------

    /**
     * Unloads every loaded chunk that falls inside any region in
     * {@code scan.deleteableRegions()} with {@code save = false}, so the
     * in-memory chunk copy is thrown away rather than flushed back over the
     * region files we are about to delete.
     *
     * <p>Each {@code r.X.Z.mca} covers a 32×32 chunk square. For every target
     * region this iterates {@code (rX*32 .. rX*32+31, rZ*32 .. rZ*32+31)} and
     * unloads any chunk currently loaded. Chunks that cannot be unloaded
     * (e.g. a player is inside, or the chunk is force-loaded) are silently
     * skipped — reaping a chunk out from under a present player would be
     * worse than waiting for the next sweep.
     *
     * <p>The deleted-sweep callers (manual command + housekeeping) must
     * invoke this on the main thread <b>before</b> dispatching the async
     * {@link #delete(PurgeScanResult)}; otherwise Paper's autosave or shutdown
     * will rewrite the region file with the stale in-memory chunks immediately
     * after we delete it on disk.
     *
     * <p>Nether and end dimensions are evicted only when the gamemode owns
     * them, mirroring the dimension gating in {@link #deleteRegionFiles}.
     *
     * @param scan a prior scan result whose regions should be evicted
     */
    public void evictChunks(PurgeScanResult scan) {
        if (scan.deleteableRegions().isEmpty()) {
            return;
        }
        World overworld = scan.world();
        World netherWorld = scan.isNether() ? plugin.getIWM().getNetherWorld(overworld) : null;
        World endWorld = scan.isEnd() ? plugin.getIWM().getEndWorld(overworld) : null;

        int evicted = 0;
        for (Pair<Integer, Integer> coords : scan.deleteableRegions().keySet()) {
            int baseCx = coords.x() << 5;   // rX * 32
            int baseCz = coords.z() << 5;
            evicted += evictRegion(overworld, baseCx, baseCz);
            if (netherWorld != null) {
                evicted += evictRegion(netherWorld, baseCx, baseCz);
            }
            if (endWorld != null) {
                evicted += evictRegion(endWorld, baseCx, baseCz);
            }
        }
        plugin.log("Purge deleted: evicted " + evicted + " loaded chunk(s) from "
                + scan.deleteableRegions().size() + " target region(s)");
    }

    private int evictRegion(World world, int baseCx, int baseCz) {
        int count = 0;
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                int cx = baseCx + dx;
                int cz = baseCz + dz;
                if (world.isChunkLoaded(cx, cz) && world.unloadChunk(cx, cz, false)) {
                    count++;
                }
            }
        }
        return count;
    }

    // ---------------------------------------------------------------
    // Debug / testing: artificially age region files
    // ---------------------------------------------------------------

    /**
     * Debug/testing utility. Rewrites the per-chunk timestamp table of every
     * {@code .mca} region file in the given world's overworld (and nether/end
     * if the gamemode owns those dimensions) so that every chunk entry looks
     * like it was last written {@code days} days ago.
     *
     * <p>The purge scanner reads per-chunk timestamps from the second 4KB
     * block of each region file's header (not file mtime), so {@code touch}
     * cannot fake ageing. This rewrites that 4KB timestamp table in place,
     * setting all 1024 slots to {@code now - days*86400} seconds. File mtime
     * is not modified.
     *
     * <p>Runs synchronously and performs disk I/O. Callers must invoke
     * this from an async task, and should call {@code World.save()} on the
     * main thread first to flush in-memory chunk state.
     *
     * @param world the gamemode overworld whose regions should be aged
     * @param days  how many days in the past to pretend the regions were
     *              last written
     * @return number of {@code .mca} files successfully rewritten across
     *         all dimensions
     */
    public int ageRegions(World world, int days) {
        boolean isNether = plugin.getIWM().isNetherGenerate(world) && plugin.getIWM().isNetherIslands(world);
        boolean isEnd = plugin.getIWM().isEndGenerate(world) && plugin.getIWM().isEndIslands(world);

        File worldDir = world.getWorldFolder();
        File overworldRegion = new File(worldDir, REGION);

        World netherWorld = plugin.getIWM().getNetherWorld(world);
        File netherRegion = new File(
                netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(worldDir), REGION);

        World endWorld = plugin.getIWM().getEndWorld(world);
        File endRegion = new File(
                endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(worldDir), REGION);

        long targetSeconds = (System.currentTimeMillis() / 1000L) - (days * 86400L);
        int total = 0;
        total += ageRegionsInFolder(overworldRegion, "overworld", targetSeconds);
        if (isNether) {
            total += ageRegionsInFolder(netherRegion, "nether", targetSeconds);
        }
        if (isEnd) {
            total += ageRegionsInFolder(endRegion, "end", targetSeconds);
        }
        return total;
    }

    private int ageRegionsInFolder(File folder, String dimension, long targetSeconds) {
        if (!folder.isDirectory()) {
            plugin.log("Age-regions: " + dimension + " folder does not exist, skipping: "
                    + folder.getAbsolutePath());
            return 0;
        }
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null || files.length == 0) {
            plugin.log("Age-regions: no .mca files in " + dimension + " folder " + folder.getAbsolutePath());
            return 0;
        }
        int count = 0;
        for (File file : files) {
            if (writeTimestampTable(file, targetSeconds)) {
                count++;
            }
        }
        plugin.log("Age-regions: rewrote " + count + "/" + files.length + " " + dimension + " region file(s)");
        return count;
    }

    /**
     * Overwrites the 4KB timestamp table (bytes 4096..8191) of a Minecraft
     * {@code .mca} file with a single repeating big-endian int timestamp.
     *
     * @param regionFile    the file to rewrite
     * @param targetSeconds the Unix timestamp (seconds) to write into every slot
     * @return {@code true} if the table was rewritten
     */
    private boolean writeTimestampTable(File regionFile, long targetSeconds) {
        if (!regionFile.exists() || regionFile.length() < 8192) {
            plugin.log("Age-regions: skipping " + regionFile.getName()
                    + " (missing or smaller than 8192 bytes)");
            return false;
        }
        byte[] table = new byte[4096];
        int ts = (int) targetSeconds;
        for (int i = 0; i < 1024; i++) {
            int offset = i * 4;
            table[offset]     = (byte) (ts >> 24);
            table[offset + 1] = (byte) (ts >> 16);
            table[offset + 2] = (byte) (ts >> 8);
            table[offset + 3] = (byte)  ts;
        }
        try (RandomAccessFile raf = new RandomAccessFile(regionFile, "rw")) {
            raf.seek(4096);
            raf.write(table);
            return true;
        } catch (IOException e) {
            plugin.logError("Age-regions: failed to rewrite timestamp table of "
                    + regionFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Filtering
    // ---------------------------------------------------------------

    /**
     * Removes regions whose island-set contains at least one island that
     * cannot be deleted, returning blocking statistics.
     */
    private FilterStats filterNonDeletableRegions(
            Map<Pair<Integer, Integer>, Set<String>> deleteableRegions, int days) {
        int islandsOverLevel = 0;
        int islandsPurgeProtected = 0;
        int regionsBlockedByLevel = 0;
        int regionsBlockedByProtection = 0;

        var iter = deleteableRegions.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            int[] regionCounts = evaluateRegionIslands(entry.getValue(), days);
            if (regionCounts[0] > 0) { // shouldRemove
                iter.remove();
                islandsOverLevel += regionCounts[1];
                islandsPurgeProtected += regionCounts[2];
                if (regionCounts[1] > 0) regionsBlockedByLevel++;
                if (regionCounts[2] > 0) regionsBlockedByProtection++;
            }
        }
        return new FilterStats(islandsOverLevel, islandsPurgeProtected,
                regionsBlockedByLevel, regionsBlockedByProtection);
    }

    /**
     * Strict filter for the deleted sweep: any non-deletable island in a
     * region blocks the whole region. Unlike {@link #filterNonDeletableRegions}
     * this has no age/login/level logic — only the {@code deletable} flag
     * matters.
     */
    private FilterStats filterForDeletedSweep(
            Map<Pair<Integer, Integer>, Set<String>> deleteableRegions) {
        int regionsBlockedByProtection = 0;
        var iter = deleteableRegions.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            boolean block = false;
            for (String id : entry.getValue()) {
                Optional<Island> opt = plugin.getIslands().getIslandById(id);
                if (opt.isEmpty()) {
                    // Missing rows don't block — they're already gone.
                    continue;
                }
                if (!opt.get().isDeletable()) {
                    block = true;
                    break;
                }
            }
            if (block) {
                iter.remove();
                regionsBlockedByProtection++;
            }
        }
        return new FilterStats(0, 0, 0, regionsBlockedByProtection);
    }

    private int[] evaluateRegionIslands(Set<String> islandIds, int days) {
        int shouldRemove = 0;
        int levelBlocked = 0;
        int purgeBlocked = 0;
        for (String id : islandIds) {
            Optional<Island> opt = plugin.getIslands().getIslandById(id);
            if (opt.isEmpty()) {
                shouldRemove = 1;
                continue;
            }
            Island isl = opt.get();
            if (cannotDeleteIsland(isl, days)) {
                shouldRemove = 1;
                if (isl.isPurgeProtected()) purgeBlocked++;
                if (isLevelTooHigh(isl)) levelBlocked++;
            }
        }
        return new int[] { shouldRemove, levelBlocked, purgeBlocked };
    }

    private void logFilterStats(FilterStats stats) {
        if (stats.islandsOverLevel() > 0) {
            plugin.log("Purge: " + stats.islandsOverLevel() + " island(s) exceed the level threshold of "
                    + plugin.getSettings().getIslandPurgeLevel()
                    + " - preventing " + stats.regionsBlockedByLevel() + " region(s) from being purged");
        }
        if (stats.islandsPurgeProtected() > 0) {
            plugin.log("Purge: " + stats.islandsPurgeProtected() + " island(s) are purge-protected"
                    + " - preventing " + stats.regionsBlockedByProtection() + " region(s) from being purged");
        }
    }

    /**
     * Check if an island cannot be deleted. Purge protected, spawn, or
     * unowned (non-deletable) islands cannot be deleted. Islands whose members
     * recently logged in, or that exceed the level threshold, cannot be
     * deleted.
     *
     * @param island island
     * @param days   the age cutoff
     * @return {@code true} if the island cannot be deleted
     */
    public boolean cannotDeleteIsland(Island island, int days) {
        if (island.isDeletable()) {
            return false;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        boolean recentLogin = island.getMemberSet().stream().anyMatch(uuid -> {
            Long lastLogin = plugin.getPlayers().getLastLoginTimestamp(uuid);
            if (lastLogin == null) {
                lastLogin = Bukkit.getOfflinePlayer(uuid).getLastSeen();
            }
            return lastLogin >= cutoffMillis;
        });
        if (recentLogin) {
            return true;
        }
        if (isLevelTooHigh(island)) {
            return true;
        }
        return island.isPurgeProtected() || island.isSpawn() || !island.isOwned();
    }

    private boolean isLevelTooHigh(Island island) {
        return plugin.getAddonsManager().getAddonByName("Level")
                .map(l -> ((Level) l).getIslandLevel(island.getWorld(), island.getOwner())
                        >= plugin.getSettings().getIslandPurgeLevel())
                .orElse(false);
    }

    // ---------------------------------------------------------------
    // Scan
    // ---------------------------------------------------------------

    /**
     * Finds all region files in the overworld (and optionally nether/end)
     * that have not been modified in the last {@code days} days.
     */
    private List<Pair<Integer, Integer>> findOldRegions(World world, int days, boolean isNether, boolean isEnd) {
        File worldDir = world.getWorldFolder();
        File overworldRegion = new File(worldDir, REGION);

        World netherWorld = plugin.getIWM().getNetherWorld(world);
        File netherBase = netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(worldDir);
        File netherRegion = new File(netherBase, REGION);

        World endWorld = plugin.getIWM().getEndWorld(world);
        File endBase = endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(worldDir);
        File endRegion = new File(endBase, REGION);

        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);

        logRegionFolderPaths(overworldRegion, netherRegion, endRegion, world, isNether, isEnd);

        Set<String> candidateNames = collectCandidateNames(overworldRegion, netherRegion, endRegion, isNether, isEnd);
        plugin.log("Purge total candidate region coordinates: " + candidateNames.size());
        plugin.log("Purge checking candidate region(s) against island data, please wait...");

        List<Pair<Integer, Integer>> regions = new ArrayList<>();
        for (String name : candidateNames) {
            Pair<Integer, Integer> coords = parseRegionCoords(name);
            if (coords == null) continue;
            if (!isAnyDimensionFresh(name, overworldRegion, netherRegion, endRegion, cutoffMillis, isNether, isEnd)) {
                regions.add(coords);
            }
        }
        return regions;
    }

    private void logRegionFolderPaths(File overworldRegion, File netherRegion, File endRegion,
            World world, boolean isNether, boolean isEnd) {
        plugin.log("Purge region folders - Overworld: " + overworldRegion.getAbsolutePath()
                + EXISTS_PREFIX + overworldRegion.isDirectory() + ")");
        if (isNether) {
            plugin.log("Purge region folders - Nether: " + netherRegion.getAbsolutePath()
                    + EXISTS_PREFIX + netherRegion.isDirectory() + ")");
        } else {
            plugin.log("Purge region folders - Nether: disabled (isNetherGenerate="
                    + plugin.getIWM().isNetherGenerate(world) + ", isNetherIslands="
                    + plugin.getIWM().isNetherIslands(world) + ")");
        }
        if (isEnd) {
            plugin.log("Purge region folders - End: " + endRegion.getAbsolutePath()
                    + EXISTS_PREFIX + endRegion.isDirectory() + ")");
        } else {
            plugin.log("Purge region folders - End: disabled (isEndGenerate="
                    + plugin.getIWM().isEndGenerate(world) + ", isEndIslands="
                    + plugin.getIWM().isEndIslands(world) + ")");
        }
    }

    private Set<String> collectCandidateNames(File overworldRegion, File netherRegion, File endRegion,
            boolean isNether, boolean isEnd) {
        Set<String> names = new HashSet<>();
        addFileNames(names, overworldRegion.listFiles((dir, name) -> name.endsWith(".mca")), "overworld");
        if (isNether) {
            addFileNames(names, netherRegion.listFiles((dir, name) -> name.endsWith(".mca")), "nether");
        }
        if (isEnd) {
            addFileNames(names, endRegion.listFiles((dir, name) -> name.endsWith(".mca")), "end");
        }
        return names;
    }

    private void addFileNames(Set<String> names, File[] files, String dimension) {
        if (files != null) {
            for (File f : files) names.add(f.getName());
        }
        plugin.log(PURGE_FOUND + (files != null ? files.length : 0) + " " + dimension + " region files");
    }

    private Pair<Integer, Integer> parseRegionCoords(String name) {
        String coordsPart = name.substring(2, name.length() - 4);
        String[] parts = coordsPart.split("\\.");
        if (parts.length != 2) return null;
        try {
            return new Pair<>(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Maps each old region to the set of island IDs whose island-squares
     * overlap it. Each region covers a 512x512 block square.
     */
    private Map<Pair<Integer, Integer>, Set<String>> mapIslandsToRegions(
            List<Pair<Integer, Integer>> oldRegions, IslandGrid islandGrid) {
        final int blocksPerRegion = 512;
        Map<Pair<Integer, Integer>, Set<String>> regionToIslands = new HashMap<>();

        for (Pair<Integer, Integer> region : oldRegions) {
            int regionMinX = region.x() * blocksPerRegion;
            int regionMinZ = region.z() * blocksPerRegion;
            int regionMaxX = regionMinX + blocksPerRegion - 1;
            int regionMaxZ = regionMinZ + blocksPerRegion - 1;

            Set<String> ids = new HashSet<>();
            for (IslandData data : islandGrid.getIslandsInBounds(regionMinX, regionMinZ, regionMaxX, regionMaxZ)) {
                ids.add(data.id());
            }
            regionToIslands.put(region, ids);
        }
        return regionToIslands;
    }

    // ---------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------

    private boolean deleteRegionFiles(PurgeScanResult scan) {
        int days = scan.days();
        if (days < 0) {
            plugin.logError("Days is somehow negative!");
            return false;
        }
        // days == 0 is the "deleted sweep" sentinel — no age filter and no
        // freshness recheck. days > 0 is the age-based sweep.
        boolean ageGated = days > 0;
        long cutoffMillis = ageGated ? System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days) : 0L;

        World world = scan.world();
        File base = world.getWorldFolder();
        File overworldRegion   = new File(base, REGION);
        File overworldEntities = new File(base, ENTITIES);
        File overworldPoi      = new File(base, POI);

        World netherWorld = plugin.getIWM().getNetherWorld(world);
        File netherBase     = netherWorld != null ? resolveDataFolder(netherWorld) : resolveNetherFallback(base);
        File netherRegion   = new File(netherBase, REGION);
        File netherEntities = new File(netherBase, ENTITIES);
        File netherPoi      = new File(netherBase, POI);

        World endWorld = plugin.getIWM().getEndWorld(world);
        File endBase     = endWorld != null ? resolveDataFolder(endWorld) : resolveEndFallback(base);
        File endRegion   = new File(endBase, REGION);
        File endEntities = new File(endBase, ENTITIES);
        File endPoi      = new File(endBase, POI);

        // Verify none of the files have been updated since the cutoff.
        // Skipped for the deleted sweep (ageGated == false) — the deletable
        // flag on the island row is the sole authority there.
        if (ageGated) {
            for (Pair<Integer, Integer> coords : scan.deleteableRegions().keySet()) {
                String name = "r." + coords.x() + "." + coords.z() + ".mca";
                if (isAnyDimensionFresh(name, overworldRegion, netherRegion, endRegion, cutoffMillis,
                        scan.isNether(), scan.isEnd())) {
                    return false;
                }
            }
        }

        DimFolders ow     = new DimFolders(overworldRegion, overworldEntities, overworldPoi);
        DimFolders nether = new DimFolders(netherRegion,    netherEntities,    netherPoi);
        DimFolders end    = new DimFolders(endRegion,       endEntities,       endPoi);
        plugin.log("Purge delete: overworld region folder = " + overworldRegion.getAbsolutePath()
                + " (exists=" + overworldRegion.isDirectory() + ")");
        if (scan.isNether()) {
            plugin.log("Purge delete: nether region folder    = " + netherRegion.getAbsolutePath()
                    + " (exists=" + netherRegion.isDirectory() + ")");
        }
        if (scan.isEnd()) {
            plugin.log("Purge delete: end region folder       = " + endRegion.getAbsolutePath()
                    + " (exists=" + endRegion.isDirectory() + ")");
        }
        boolean allOk = true;
        for (Pair<Integer, Integer> coords : scan.deleteableRegions().keySet()) {
            String name = "r." + coords.x() + "." + coords.z() + ".mca";
            if (!deleteOneRegion(name, ow, nether, end, scan.isNether(), scan.isEnd())) {
                plugin.logError("Could not delete all the region/entity/poi files for some reason");
                allOk = false;
            }
        }
        return allOk;
    }

    private boolean deleteOneRegion(String name, DimFolders overworld, DimFolders nether, DimFolders end,
            boolean isNether, boolean isEnd) {
        boolean ok = deleteIfExists(new File(overworld.region(),   name))
                  && deleteIfExists(new File(overworld.entities(), name))
                  && deleteIfExists(new File(overworld.poi(),      name));
        if (isNether) {
            ok &= deleteIfExists(new File(nether.region(),   name));
            ok &= deleteIfExists(new File(nether.entities(), name));
            ok &= deleteIfExists(new File(nether.poi(),      name));
        }
        if (isEnd) {
            ok &= deleteIfExists(new File(end.region(),   name));
            ok &= deleteIfExists(new File(end.entities(), name));
            ok &= deleteIfExists(new File(end.poi(),      name));
        }
        return ok;
    }

    private boolean deleteIfExists(File file) {
        if (!file.getParentFile().exists()) {
            plugin.log("Purge delete: parent folder missing, skipping " + file.getAbsolutePath());
            return true;
        }
        boolean existedBefore = file.exists();
        long sizeBefore = existedBefore ? file.length() : -1L;
        try {
            boolean removed = Files.deleteIfExists(file.toPath());
            boolean existsAfter = file.exists();
            if (existedBefore) {
                plugin.log("Purge delete: " + file.getAbsolutePath()
                        + " size=" + sizeBefore + "B"
                        + " removed=" + removed
                        + " existsAfter=" + existsAfter);
                if (existsAfter) {
                    plugin.logError("Purge delete: file still present after delete! " + file.getAbsolutePath());
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            plugin.logError("Failed to delete file: " + file.getAbsolutePath() + " — " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Player data cleanup
    // ---------------------------------------------------------------

    private void deletePlayerFromWorldFolder(World world, String islandID,
            Map<Pair<Integer, Integer>, Set<String>> deleteableRegions, int days) {
        File playerData = resolvePlayerDataFolder(world);
        plugin.getIslands().getIslandById(islandID)
                .ifPresent(island -> island.getMemberSet()
                        .forEach(uuid -> maybeDeletePlayerData(world, uuid, playerData, deleteableRegions, days)));
    }

    private void maybeDeletePlayerData(World world, UUID uuid, File playerData,
            Map<Pair<Integer, Integer>, Set<String>> deleteableRegions, int days) {
        // Deleted sweep (days == 0) skips player-data cleanup entirely —
        // the player might still be active, and the age-based sweep will
        // reap orphaned .dat files later.
        if (days <= 0) {
            return;
        }
        List<Island> memberOf = new ArrayList<>(plugin.getIslands().getIslands(world, uuid));
        deleteableRegions.values().forEach(ids -> memberOf.removeIf(i -> ids.contains(i.getUniqueId())));
        if (!memberOf.isEmpty()) {
            return;
        }
        if (Bukkit.getOfflinePlayer(uuid).isOp()) {
            return;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        Long lastLogin = plugin.getPlayers().getLastLoginTimestamp(uuid);
        long actualLast = lastLogin != null ? lastLogin : Bukkit.getOfflinePlayer(uuid).getLastSeen();
        if (actualLast >= cutoffMillis) {
            return;
        }
        deletePlayerFiles(uuid, playerData);
    }

    private void deletePlayerFiles(UUID uuid, File playerData) {
        if (!playerData.exists()) {
            return;
        }
        deletePlayerFile(new File(playerData, uuid + ".dat"), "player data file");
        deletePlayerFile(new File(playerData, uuid + ".dat_old"), "player data backup file");
    }

    private void deletePlayerFile(File file, String description) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ex) {
            plugin.logError("Failed to delete " + description + ": " + file.getAbsolutePath());
        }
    }

    // ---------------------------------------------------------------
    // Dimension path resolution (pre-26.1 vs 26.1.1+)
    // ---------------------------------------------------------------

    private File resolveDataFolder(World world) {
        File worldFolder = world.getWorldFolder();
        return switch (world.getEnvironment()) {
            case NETHER -> {
                File dim = new File(worldFolder, DIM_1);
                yield dim.isDirectory() ? dim : worldFolder;
            }
            case THE_END -> {
                File dim = new File(worldFolder, DIM1);
                yield dim.isDirectory() ? dim : worldFolder;
            }
            default -> worldFolder;
        };
    }

    private File resolvePlayerDataFolder(World world) {
        File worldFolder = world.getWorldFolder();
        File oldPath = new File(worldFolder, PLAYERDATA);
        if (oldPath.isDirectory()) {
            return oldPath;
        }
        File root = worldFolder.getParentFile();
        if (root != null) root = root.getParentFile();
        if (root != null) root = root.getParentFile();
        if (root != null) {
            File newPath = new File(root, PLAYERS + File.separator + "data");
            if (newPath.isDirectory()) {
                return newPath;
            }
        }
        return oldPath;
    }

    private File resolveNetherFallback(File overworldFolder) {
        File dim = new File(overworldFolder, DIM_1);
        if (dim.isDirectory()) {
            return dim;
        }
        File parent = overworldFolder.getParentFile();
        if (parent != null) {
            File sibling = new File(parent, overworldFolder.getName() + "_nether");
            if (sibling.isDirectory()) {
                return sibling;
            }
        }
        return dim;
    }

    private File resolveEndFallback(File overworldFolder) {
        File dim = new File(overworldFolder, DIM1);
        if (dim.isDirectory()) {
            return dim;
        }
        File parent = overworldFolder.getParentFile();
        if (parent != null) {
            File sibling = new File(parent, overworldFolder.getName() + "_the_end");
            if (sibling.isDirectory()) {
                return sibling;
            }
        }
        return dim;
    }

    // ---------------------------------------------------------------
    // Freshness checks + region timestamp reader
    // ---------------------------------------------------------------

    private boolean isFileFresh(File file, long cutoffMillis) {
        return file.exists() && getRegionTimestamp(file) >= cutoffMillis;
    }

    private boolean isAnyDimensionFresh(String name, File overworldRegion, File netherRegion,
            File endRegion, long cutoffMillis, boolean isNether, boolean isEnd) {
        if (isFileFresh(new File(overworldRegion, name), cutoffMillis)) return true;
        if (isNether && isFileFresh(new File(netherRegion, name), cutoffMillis)) return true;
        return isEnd && isFileFresh(new File(endRegion, name), cutoffMillis);
    }

    /**
     * Reads the most recent per-chunk timestamp in a Minecraft .mca file
     * header, in milliseconds since epoch.
     */
    private long getRegionTimestamp(File regionFile) {
        if (!regionFile.exists() || regionFile.length() < 8192) {
            return 0L;
        }
        try (FileInputStream fis = new FileInputStream(regionFile)) {
            byte[] buffer = new byte[4096];
            if (fis.skip(4096) != 4096) {
                return 0L;
            }
            if (fis.read(buffer) != 4096) {
                return 0L;
            }
            ByteBuffer bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.BIG_ENDIAN);
            long maxTimestampSeconds = 0;
            for (int i = 0; i < 1024; i++) {
                long timestamp = Integer.toUnsignedLong(bb.getInt());
                if (timestamp > maxTimestampSeconds) {
                    maxTimestampSeconds = timestamp;
                }
            }
            return maxTimestampSeconds * 1000L;
        } catch (IOException e) {
            plugin.logError("Failed to read region file timestamps: " + regionFile.getAbsolutePath()
                    + " " + e.getMessage());
            return 0L;
        }
    }
}
