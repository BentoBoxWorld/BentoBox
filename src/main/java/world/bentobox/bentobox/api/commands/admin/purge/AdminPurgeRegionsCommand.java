package world.bentobox.bentobox.api.commands.admin.purge;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.IslandGrid;
import world.bentobox.bentobox.managers.island.IslandGrid.IslandData;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;

public class AdminPurgeRegionsCommand extends CompositeCommand implements Listener {

    private static final String NONE_FOUND = "commands.admin.purge.none-found";
    private static final String REGION = "region";
    private static final String ENTITIES = "entities";
    private static final String POI = "poi";
    private static final String DIM_1 = "DIM-1";
    private static final String IN_WORLD = " in world ";
    private static final String WILL_BE_DELETED = " will be deleted";
    private static final String EXISTS_PREFIX = " (exists=";
    private static final String PURGE_FOUND = "Purge found ";
    
    private volatile boolean inPurge;
    private boolean toBeConfirmed;
    private User user;
    private Map<Pair<Integer, Integer>, Set<String>> deleteableRegions;
    private boolean isNether;
    private boolean isEnd;
    private int days;

    public AdminPurgeRegionsCommand(CompositeCommand parent) {
        super(parent, "regions");
        getAddon().registerListener(this);
        // isNether/isEnd are NOT computed here: IWM may not have loaded the addon world
        // config yet at command-registration time. They are evaluated lazily in findIslands().
    }

    @Override
    public void setup() {
        setPermission("admin.purge.regions");
        setOnlyPlayer(false);
        setParametersHelp("commands.admin.purge.regions.parameters");
        setDescription("commands.admin.purge.regions.description");
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        if (inPurge) {
            user.sendMessage("commands.admin.purge.purge-in-progress", TextVariables.LABEL, this.getTopLabel());
            return false;
        }
        if (args.isEmpty()) {
            // Show help
            showHelp(this, user);
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        this.user = user;
        if (args.getFirst().equalsIgnoreCase("confirm") && toBeConfirmed && this.user.equals(user)) {
            return deleteEverything();
        }
        /*
         * This part does the searching for region files
         */
        // Clear tbc
        toBeConfirmed = false;

        try {
            days = Integer.parseInt(args.getFirst());
            if (days <= 0) {
                user.sendMessage("commands.admin.purge.days-one-or-more");
                return false;
            }

        } catch (NumberFormatException e) {
            user.sendMessage("commands.admin.purge.days-one-or-more");
            return false;
        }
        
        user.sendMessage("commands.admin.purge.scanning");
        // Save all worlds to update any region files
        Bukkit.getWorlds().forEach(World::save);

        // Find the potential islands
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), ()-> findIslands(getWorld(), days));
        return true;
    }


    private boolean deleteEverything() {
        if (deleteableRegions.isEmpty()) {
            user.sendMessage(NONE_FOUND); // Should never happen
            return false;
        }
        // Save the worlds
        Bukkit.getWorlds().forEach(World::save);
        // Recheck to see if any regions are newer than days and if so, stop everything
        getPlugin().log("Now deleting region files");
        if (!deleteRegionFiles()) {
            // Fail!
            getPlugin().logError("Not all region files could be deleted");
        }
        // Delete islands and regions
        for (Set<String> islandIDs : deleteableRegions.values()) {
            for (String islandID : islandIDs) {
                deletePlayerFromWorldFolder(islandID);
                // Remove island from the cache
                getPlugin().getIslands().getIslandCache().deleteIslandFromCache(islandID);
                // Delete island from database using id
                if (getPlugin().getIslands().deleteIslandId(islandID)) {
                    // Log
                    getPlugin().log("Island ID " + islandID + " deleted from cache and database" );
                }
            }
        }

        user.sendMessage("general.success");
        toBeConfirmed = false;
        deleteableRegions.clear();
        return true;

    }

    private void deletePlayerFromWorldFolder(String islandID) {
        File playerData = new File(getWorld().getWorldFolder(), "playerdata");
        getPlugin().getIslands().getIslandById(islandID)
                .ifPresent(island -> island.getMemberSet()
                        .forEach(uuid -> maybeDeletePlayerData(uuid, playerData)));
    }

    private void maybeDeletePlayerData(UUID uuid, File playerData) {
        List<Island> memberOf = new ArrayList<>(getIslands().getIslands(getWorld(), uuid));
        deleteableRegions.values().forEach(ids -> memberOf.removeIf(i -> ids.contains(i.getUniqueId())));
        if (!memberOf.isEmpty()) {
            return;
        }
        if (Bukkit.getOfflinePlayer(uuid).isOp()) {
            return;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        if (resolveLastLogin(uuid) >= cutoffMillis) {
            return;
        }
        deletePlayerFiles(uuid, playerData);
    }

    private long resolveLastLogin(UUID uuid) {
        Long lastLogin = getPlugin().getPlayers().getLastLoginTimestamp(uuid);
        return lastLogin != null ? lastLogin : Bukkit.getOfflinePlayer(uuid).getLastSeen();
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
            getPlugin().logError("Failed to delete " + description + ": " + file.getAbsolutePath());
        }
    }

    /**
     * Resolves the base data folder for a world, accounting for the dimension
     * subfolder that Minecraft uses for non-overworld environments.
     * <p>
     * Overworld data lives directly in the world folder, but Nether data lives
     * in {@code DIM-1/} and End data lives in {@code DIM1/} subfolders - even
     * when the world has its own separate folder.
     *
     * @param world the world to resolve
     * @return the base folder containing region/, entities/, poi/ subfolders
     */
    private File resolveDataFolder(World world) {
        File worldFolder = world.getWorldFolder();
        return switch (world.getEnvironment()) {
            case NETHER -> {
                File dim = new File(worldFolder, DIM_1);
                yield dim.isDirectory() ? dim : worldFolder;
            }
            case THE_END -> {
                File dim = new File(worldFolder, "DIM1");
                yield dim.isDirectory() ? dim : worldFolder;
            }
            default -> worldFolder;
        };
    }

    /**
     * Deletes a file if it exists, logging an error if deletion fails.
     * Does not log if the parent folder does not exist (normal for entities/poi).
     * @param file the file to delete
     * @return true if deleted or does not exist, false if exists but could not be deleted
     */
    private boolean deleteIfExists(File file) {
        if (!file.getParentFile().exists()) {
            // Parent folder missing is normal for entities/poi, do not log
            return true;
        }
        try {
            Files.deleteIfExists(file.toPath());
            return true;
        } catch (IOException e) {
            getPlugin().logError("Failed to delete file: " + file.getAbsolutePath());
            return false;
        }
    }

    /**
     * Deletes all region files in deleteableRegions that are older than {@code days}.
     * Also deletes corresponding entities and poi files in each dimension.
     * @return {@code true} if deletion was performed; {@code false} if cancelled
     *         due to any file being newer than the cutoff
     */
    private boolean deleteRegionFiles() {
        if (days <= 0) {
            getPlugin().logError("Days is somehow zero or negative!");
            return false;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);

        World world = getWorld();
        File base = world.getWorldFolder();
        File overworldRegion   = new File(base, REGION);
        File overworldEntities = new File(base, ENTITIES);
        File overworldPoi      = new File(base, POI);

        World netherWorld = getPlugin().getIWM().getNetherWorld(world);
        File netherBase     = netherWorld != null ? resolveDataFolder(netherWorld) : new File(base, DIM_1);
        File netherRegion   = new File(netherBase, REGION);
        File netherEntities = new File(netherBase, ENTITIES);
        File netherPoi      = new File(netherBase, POI);

        World endWorld = getPlugin().getIWM().getEndWorld(world);
        File endBase     = endWorld != null ? resolveDataFolder(endWorld) : new File(base, "DIM1");
        File endRegion   = new File(endBase, REGION);
        File endEntities = new File(endBase, ENTITIES);
        File endPoi      = new File(endBase, POI);

        // Phase 1: verify none of the files have been updated since the cutoff
        for (Pair<Integer, Integer> coords : deleteableRegions.keySet()) {
            String name = "r." + coords.x() + "." + coords.z() + ".mca";
            if (isAnyDimensionFresh(name, overworldRegion, netherRegion, endRegion, cutoffMillis)) {
                return false;
            }
        }

        // Phase 2: perform deletions
        DimFolders ow     = new DimFolders(overworldRegion, overworldEntities, overworldPoi);
        DimFolders nether = new DimFolders(netherRegion,    netherEntities,    netherPoi);
        DimFolders end    = new DimFolders(endRegion,       endEntities,       endPoi);
        for (Pair<Integer, Integer> coords : deleteableRegions.keySet()) {
            String name = "r." + coords.x() + "." + coords.z() + ".mca";
            if (!deleteOneRegion(name, ow, nether, end)) {
                getPlugin().logError("Could not delete all the region/entity/poi files for some reason");
            }
        }

        return true;
    }

    private boolean isFileFresh(File file, long cutoffMillis) {
        return file.exists() && getRegionTimestamp(file) >= cutoffMillis;
    }

    private boolean isAnyDimensionFresh(String name, File overworldRegion, File netherRegion,
            File endRegion, long cutoffMillis) {
        if (isFileFresh(new File(overworldRegion, name), cutoffMillis)) return true;
        if (isNether && isFileFresh(new File(netherRegion, name), cutoffMillis)) return true;
        return isEnd && isFileFresh(new File(endRegion, name), cutoffMillis);
    }

    /** Groups the three folder types (region, entities, poi) for one world dimension. */
    private record DimFolders(File region, File entities, File poi) {}

    private boolean deleteOneRegion(String name, DimFolders overworld, DimFolders nether, DimFolders end) {
        boolean owRegionOk   = deleteIfExists(new File(overworld.region(),   name));
        boolean owEntitiesOk = deleteIfExists(new File(overworld.entities(), name));
        boolean owPoiOk      = deleteIfExists(new File(overworld.poi(),      name));
        boolean ok = owRegionOk && owEntitiesOk && owPoiOk;
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

    /**
     * This method is run async!
     * @param world world
     * @param days days old
     */
    private void findIslands(World world, int days) {
        // Evaluate here, not in the constructor - IWM config is loaded by the time a command runs
        isNether = getPlugin().getIWM().isNetherGenerate(world) && getPlugin().getIWM().isNetherIslands(world);
        isEnd = getPlugin().getIWM().isEndGenerate(world) && getPlugin().getIWM().isEndIslands(world);
        try {
            // Get the grid that covers this world
            IslandGrid islandGrid = getPlugin().getIslands().getIslandCache().getIslandGrid(world);
            if (islandGrid == null) {
                Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage(NONE_FOUND));
                return;
            }
            // Find old regions
            List<Pair<Integer, Integer>> oldRegions = this.findOldRegions(days);
            // Get islands that are associated with these regions
            deleteableRegions = this.mapIslandsToRegions(oldRegions, islandGrid);
            // Filter regions: remove any whose island-set contains at least one island that cannot be deleted.
            // Track why islands are blocked so we can show a summary report.
            int islandsOverLevel = 0;
            int islandsPurgeProtected = 0;
            int regionsBlockedByLevel = 0;
            int regionsBlockedByProtection = 0;

            var iter = deleteableRegions.entrySet().iterator();
            while (iter.hasNext()) {
                var entry = iter.next();
                boolean remove = false;
                boolean regionHasLevelBlock = false;
                boolean regionHasPurgeBlock = false;
                for (String id : entry.getValue()) {
                    Optional<Island> opt = getPlugin().getIslands().getIslandById(id);
                    if (opt.isEmpty()) {
                        remove = true;
                        continue;
                    }
                    Island isl = opt.get();
                    if (canDeleteIsland(isl)) {
                        remove = true;
                        if (isl.isPurgeProtected()) {
                            islandsPurgeProtected++;
                            regionHasPurgeBlock = true;
                        }
                        if (isLevelTooHigh(isl)) {
                            islandsOverLevel++;
                            regionHasLevelBlock = true;
                        }
                    }
                }
                if (remove) {
                    iter.remove();
                    if (regionHasLevelBlock) regionsBlockedByLevel++;
                    if (regionHasPurgeBlock) regionsBlockedByProtection++;
                }
            }

            // Summary report
            if (islandsOverLevel > 0) {
                getPlugin().log("Purge: " + islandsOverLevel + " island(s) exceed the level threshold of "
                        + getPlugin().getSettings().getIslandPurgeLevel()
                        + " - preventing " + regionsBlockedByLevel + " region(s) from being purged");
            }
            if (islandsPurgeProtected > 0) {
                getPlugin().log("Purge: " + islandsPurgeProtected + " island(s) are purge-protected"
                        + " - preventing " + regionsBlockedByProtection + " region(s) from being purged");
            }
            // At this point any islands that might be deleted are in the cache, and so we can freely access them
            // 1) Pull out all island IDs,  
            // 2) resolve to Optional<Island>,  
            // 3) flatten to Island,  
            // 4) collect into a Set to dedupe,  
            // 5) display each:
            Set<Island> uniqueIslands = deleteableRegions.values().stream()      // Collection<Set<String>>
                    .flatMap(Set::stream)                                           // Stream<String>
                    .map(getPlugin().getIslands()::getIslandById)                   // Stream<Optional<Island>>
                    .flatMap(Optional::stream)                                      // Stream<Island>
                    .collect(Collectors.toSet());                                   // Set<Island> (deduped)

            // Display to the user
            uniqueIslands.forEach(this::displayIsland);

            // Display empty regions
            deleteableRegions.entrySet().stream()
                .filter(e -> e.getValue().isEmpty())
                .forEach(e -> displayEmptyRegion(e.getKey()));

            if (deleteableRegions.isEmpty()) {
                Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage(NONE_FOUND));
            } else {
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    user.sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, String.valueOf(uniqueIslands.size()));
                    user.sendMessage("commands.admin.purge.regions.confirm", TextVariables.LABEL, this.getLabel());
                    user.sendMessage("general.beta"); // TODO Remove beta in the future
                    this.toBeConfirmed = true;
                });
            }
        } finally {
            inPurge = false;
        }
    }

    private void displayIsland(Island island) {
        // Log the island data
        if (island.isDeletable()) {
            getPlugin().log("Deletable island at " + Util.xyz(island.getCenter().toVector()) + IN_WORLD + getWorld().getName() + WILL_BE_DELETED);
            return;
        }
        if (island.getOwner() == null) {
            getPlugin().log("Unowned island at " + Util.xyz(island.getCenter().toVector()) + IN_WORLD + getWorld().getName() + WILL_BE_DELETED);
            return;
        }
        getPlugin().log("Island at " + Util.xyz(island.getCenter().toVector()) + IN_WORLD + getWorld().getName() 
                + " owned by " + getPlugin().getPlayers().getName(island.getOwner())
                + " who last logged in " + formatLocalTimestamp(getPlugin().getPlayers().getLastLoginTimestamp(island.getOwner()))
                + WILL_BE_DELETED);
    }

    private void displayEmptyRegion(Pair<Integer, Integer> region) {
        getPlugin().log("Empty region at r." + region.x() + "." + region.z() + IN_WORLD + getWorld().getName() + " will be deleted (no islands)");
    }

    /**
     * Formats a millisecond timestamp into a human-readable string
     * using the system's local time zone.
     *
     * @param millis the timestamp in milliseconds
     * @return formatted string in the form "yyyy-MM-dd HH:mm"
     */
    private String formatLocalTimestamp(Long millis) {
        if (millis == null) {
            return "(unknown or never recorded)";
        }
        Instant instant = Instant.ofEpochMilli(millis);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault()); // Uses the machine's local time zone

        return formatter.format(instant);
    }

    /**
     * Check if an island cannot be deleted. Purge protected, spawn, or unowned islands cannot be deleted.
     * Islands whose members recently logged in, or that exceed the level threshold, cannot be deleted.
     * @param island island
     * @return true means "cannot delete"
     */
    private boolean canDeleteIsland(Island island) {
        // If the island is marked deletable it can always be purged
        if (island.isDeletable()) {
            return false;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        // Block if ANY member (owner or team) has logged in within the cutoff window
        boolean recentLogin = island.getMemberSet().stream().anyMatch(uuid -> {
            Long lastLogin = getPlugin().getPlayers().getLastLoginTimestamp(uuid);
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

    /**
     * Returns true if the island's level meets or exceeds the configured purge threshold.
     * Returns false when the Level addon is not present.
     * @param island island to check
     * @return true if the island level is too high to purge
     */
    private boolean isLevelTooHigh(Island island) {
        return getPlugin().getAddonsManager().getAddonByName("Level")
                .map(l -> ((Level) l).getIslandLevel(getWorld(), island.getOwner())
                        >= getPlugin().getSettings().getIslandPurgeLevel())
                .orElse(false);
    }

    /**
     * Finds all region files in the overworld (and optionally the Nether and End)
     * that have not been modified in the last {@code days} days, and returns their
     * region coordinates.
     *
     * <p>If {@code nether} is {@code true}, the matching region file in the
     * Nether (DIM-1) must also be older than the cutoff to include the coordinate.
     * If {@code end} is {@code true}, the matching region file in the End (DIM1)
     * must likewise be older than the cutoff. When both {@code nether} and
     * {@code end} are {@code true}, all three dimension files must satisfy the
     * age requirement.</p>
     *
     * @param days   the minimum age in days of region files to include
     * @return a list of {@code Pair<regionX, regionZ>} for each region meeting
     *         the age criteria
     */
    private List<Pair<Integer, Integer>> findOldRegions(int days) {
        World world = this.getWorld();
        File worldDir = world.getWorldFolder();
        File overworldRegion = new File(worldDir, REGION);

        World netherWorld = getPlugin().getIWM().getNetherWorld(world);
        File netherBase = netherWorld != null ? resolveDataFolder(netherWorld) : new File(worldDir, DIM_1);
        File netherRegion = new File(netherBase, REGION);

        World endWorld = getPlugin().getIWM().getEndWorld(world);
        File endBase = endWorld != null ? resolveDataFolder(endWorld) : new File(worldDir, "DIM1");
        File endRegion = new File(endBase, REGION);

        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);

        logRegionFolderPaths(overworldRegion, netherRegion, endRegion, world);

        // Collect all candidate region names from overworld, nether, and end.
        // This ensures orphaned nether/end files are caught even if the overworld
        // file was already deleted by a previous (buggy) purge run.
        Set<String> candidateNames = collectCandidateNames(overworldRegion, netherRegion, endRegion);
        getPlugin().log("Purge total candidate region coordinates: " + candidateNames.size());
        getPlugin().log("Purge checking candidate region(s) against island data, please wait...");

        List<Pair<Integer, Integer>> regions = new ArrayList<>();
        for (String name : candidateNames) {
            Pair<Integer, Integer> coords = parseRegionCoords(name);
            if (coords == null) continue;
            if (!isAnyDimensionFresh(name, overworldRegion, netherRegion, endRegion, cutoffMillis)) {
                regions.add(coords);
            }
        }
        return regions;
    }

    private void logRegionFolderPaths(File overworldRegion, File netherRegion, File endRegion, World world) {
        getPlugin().log("Purge region folders - Overworld: " + overworldRegion.getAbsolutePath()
                + EXISTS_PREFIX + overworldRegion.isDirectory() + ")");
        if (isNether) {
            getPlugin().log("Purge region folders - Nether: " + netherRegion.getAbsolutePath()
                    + EXISTS_PREFIX + netherRegion.isDirectory() + ")");
        } else {
            getPlugin().log("Purge region folders - Nether: disabled (isNetherGenerate="
                    + getPlugin().getIWM().isNetherGenerate(world) + ", isNetherIslands="
                    + getPlugin().getIWM().isNetherIslands(world) + ")");
        }
        if (isEnd) {
            getPlugin().log("Purge region folders - End: " + endRegion.getAbsolutePath()
                    + EXISTS_PREFIX + endRegion.isDirectory() + ")");
        } else {
            getPlugin().log("Purge region folders - End: disabled (isEndGenerate="
                    + getPlugin().getIWM().isEndGenerate(world) + ", isEndIslands="
                    + getPlugin().getIWM().isEndIslands(world) + ")");
        }
    }

    private Set<String> collectCandidateNames(File overworldRegion, File netherRegion, File endRegion) {
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
        getPlugin().log(PURGE_FOUND + (files != null ? files.length : 0) + " " + dimension + " region files");
    }

    private Pair<Integer, Integer> parseRegionCoords(String name) {
        // Parse region coords from filename "r.<x>.<z>.mca"
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
     * Maps each old region to the set of island IDs whose island-squares overlap it.
     *
     * <p>Each region covers blocks
     * [regionX*512 .. regionX*512 + 511] x [regionZ*512 .. regionZ*512 + 511].</p>
     *
     * @param oldRegions  the list of region coordinates to process
     * @param islandGrid  the spatial grid to query
     * @return            a map from region coords to the set of overlapping island IDs
     */
    private Map<Pair<Integer, Integer>, Set<String>> mapIslandsToRegions(
            List<Pair<Integer, Integer>> oldRegions,
            IslandGrid islandGrid
            ) {
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

            // Always add the region, even if ids is empty
            regionToIslands.put(region, ids);
        }

        return regionToIslands;
    } 

    /**
     * Reads a Minecraft region file (.mca) and returns the most recent
     * per-chunk timestamp found in its header, in milliseconds since epoch.
     *
     * @param regionFile the .mca file
     * @return the most recent timestamp (in millis) among all chunk entries,
     *         or 0 if the file is invalid or empty
     */
    private long getRegionTimestamp(File regionFile) {
        if (!regionFile.exists() || regionFile.length() < 8192) {
            return 0L;
        }

        try (FileInputStream fis = new FileInputStream(regionFile)) {
            byte[] buffer = new byte[4096]; // Second 4KB block is the timestamp table

            // Skip first 4KB (location table)
            if (fis.skip(4096) != 4096) {
                return 0L;
            }

            // Read the timestamp table
            if (fis.read(buffer) != 4096) {
                return 0L;
            }

            ByteBuffer bb = ByteBuffer.wrap(buffer);
            bb.order(ByteOrder.BIG_ENDIAN); // Timestamps are stored as big-endian ints

            long maxTimestampSeconds = 0;

            for (int i = 0; i < 1024; i++) {
                long timestamp = Integer.toUnsignedLong(bb.getInt());
                if (timestamp > maxTimestampSeconds) {
                    maxTimestampSeconds = timestamp;
                }
            }

            // Convert seconds to milliseconds
            return maxTimestampSeconds * 1000L;

        } catch (IOException e) {
            getPlugin().logError("Failed to read region file timestamps: " + regionFile.getAbsolutePath() + " " + e.getMessage());
            return 0L;
        }
    }
}
