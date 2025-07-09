package world.bentobox.bentobox.api.commands.admin.purge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.Listener;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.IslandGrid.IslandData;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;
import world.bentobox.level.Level;

public class AdminPurgeRegionsCommand extends CompositeCommand implements Listener {

    private int count;
    private volatile boolean inPurge;
    private boolean toBeConfirmed;
    private User user;
    private Map<Pair<Integer, Integer>, Set<String>> deleteableRegions;
    private final boolean isNether;
    private final boolean isEnd;
    private int days;

    public AdminPurgeRegionsCommand(CompositeCommand parent) {
        super(parent, "regions");
        getAddon().registerListener(this);
        isNether = getPlugin().getIWM().isNetherGenerate(getWorld()) && getPlugin().getIWM().isNetherIslands(getWorld());
        isEnd = getPlugin().getIWM().isEndGenerate(getWorld()) && getPlugin().getIWM().isEndIslands(getWorld());
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
        if (args.get(0).equalsIgnoreCase("confirm") && toBeConfirmed && this.user.equals(user)) {
            return deleteEverything();
        }
        /*
         * This part does the searching for region files
         */
        // Clear tbc
        toBeConfirmed = false;
        
        days = Integer.parseInt(args.get(0));
        if (days < 1) {
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
            user.sendMessage("commands.admin.purge.none-found"); // Should never happen
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
                // Remove island from the cache
                getPlugin().getIslands().getIslandCache().deleteIslandFromCache(islandID);
                // Delete island from database using id
                getPlugin().getIslands().deleteIslandId(islandID);
                // Log
                getPlugin().log("Island ID " + islandID + " deleted from cache and database" );
            }
        }
 
        user.sendMessage("general.success");
        toBeConfirmed = false;
        deleteableRegions.clear();
        return true;
        
    }

    /**
     * Deletes all region files in deleteableRegions that are older than {@code days}.
     * First verifies that none of the overworld, nether or end region files (as
     * configured by the {@code isNether} and {@code isEnd} fields) have been
     * modified within the last {@code days} days. If any file is newer than the
     * cutoff, no files are deleted and the method returns {@code false}.
     * Otherwise all required files for each region are deleted (logging any errors),
     * removed from {@code deleteableRegions}, and the method returns {@code true}.
     *
     * @return {@code true} if deletion was performed; {@code false} if cancelled
     *         due to any file being newer than the cutoff
     */
    public boolean deleteRegionFiles() {
        if (days < 0) {
            getPlugin().logError("Days is somehow negative!");
            return false;
        }
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);

        World world = getWorld();
        File base = world.getWorldFolder();
        File overworldRegion = new File(base, "region");
        File netherRegion    = new File(base, "DIM-1" + File.separator + "region");
        File endRegion       = new File(base, "DIM1"  + File.separator + "region");

        // Phase 1: verify none of the files have been updated since the cutoff
        for (Pair<Integer, Integer> coords : deleteableRegions.keySet()) {
            int x = coords.x;
            int z = coords.z;
            String name = "r." + x + "." + z + ".mca";

            File owFile = new File(overworldRegion, name);
            if (owFile.exists() && owFile.lastModified() >= cutoffMillis) {
                return false;
            }
            if (isNether) {
                File nf = new File(netherRegion, name);
                if (nf.exists() && nf.lastModified() >= cutoffMillis) {
                    return false;
                }
            }
            if (isEnd) {
                File ef = new File(endRegion, name);
                if (ef.exists() && ef.lastModified() >= cutoffMillis) {
                    return false;
                }
            }
        }

        // Phase 2: perform deletions
        Iterator<Pair<Integer, Integer>> it = deleteableRegions.keySet().iterator();
        while (it.hasNext()) {
            Pair<Integer, Integer> coords = it.next();
            int x = coords.x;
            int z = coords.z;
            String name = "r." + x + "." + z + ".mca";

            boolean allDeleted = true;

            File owFile = new File(overworldRegion, name);
            if (owFile.exists() && !owFile.delete()) {
                getPlugin().logError("Failed to delete overworld region: " + owFile.getAbsolutePath());
                allDeleted = false;
            }
            if (isNether) {
                File nf = new File(netherRegion, name);
                if (nf.exists() && !nf.delete()) {
                    getPlugin().logError("Failed to delete nether region: " + nf.getAbsolutePath());
                    allDeleted = false;
                }
            }
            if (isEnd) {
                File ef = new File(endRegion, name);
                if (ef.exists() && !ef.delete()) {
                    getPlugin().logError("Failed to delete end region: " + ef.getAbsolutePath());
                    allDeleted = false;
                }
            }

            if (!allDeleted) {
                getPlugin().logError("Could not delete all the region files for some reason");
            }
        }

        return true;
    }

    /**
     * This method is run async! 
     * @param world world
     * @param days days old
     */
    private void findIslands(World world, int days) {
        try {
            // Get the grid that covers this world
            TreeMap<Integer, TreeMap<Integer, IslandData>> grid = getPlugin().getIslands().getIslandCache().getIslandGrid(world).getGrid();
            if (grid == null) {
                // There are no islands in this world yet!
                Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage("commands.admin.purge.none-found"));
                return;
            }

            // Find old regions
            List<Pair<Integer, Integer>> oldRegions = this.findOldRegions(days);
            // Get islands that are associated with these regions
            deleteableRegions = this.mapIslandsToRegions(oldRegions, grid);
            // Remove any region whose island‐set contains at least one island that either isn’t found or fails the deletion check:
            deleteableRegions.values().removeIf(islandIds ->
            islandIds.stream()
            .map(getPlugin().getIslands()::getIslandById)         // Stream<Optional<Island>>
            .anyMatch(optIsland -> 
            // If missing (empty) → treat as undeletable (true) - this is a bit conservative but maybe the database is messed up
            // If present, checkIsland(...) == true means “cannot delete”
            optIsland.map(this::checkIsland)
            .orElse(true)
                    )
                    );
            // At this point any islands that might be deleted are in the cache and so we can freely access them
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

            if (deleteableRegions.isEmpty()) {
                Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage("commands.admin.purge.none-found"));
            } else {
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    user.sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, String.valueOf(uniqueIslands.size()));
                    user.sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, this.getLabel());
                    this.toBeConfirmed = true;
                });
            }
        } finally {
            inPurge = false;
        }
    }

    private void displayIsland(Island island) {
        // Log the island data
        getPlugin().log("Island at " + Util.xyz(island.getCenter().toVector()) + " in world " + getWorld().getName() + " owned by " + getPlugin().getPlayers().getName(island.getOwner()) + " will be deleted");
    }

    /**
     * Check if an island can be deleted or not. Purge protected, spawn, or unowned islands cannot be deleted.
     * Islands over a certain level cannot be deleted.
     * @param island island
     * @return true means “cannot delete”
     */
    private boolean checkIsland(Island island) {
        // Level check 
        boolean levelCheck = getPlugin().getAddonsManager().getAddonByName("Level").map(l -> 
        ((Level) l).getIslandLevel(getWorld(), island.getOwner()) >= getPlugin().getSettings().getIslandPurgeLevel()).orElse(false);
        if (levelCheck) {
            // Island level is too high
            return true;
        }
        return island.isPurgeProtected() || island.isSpawn() || !island.isOwned(); 
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
    public List<Pair<Integer, Integer>> findOldRegions(int days) {
        List<Pair<Integer, Integer>> regions = new ArrayList<>();

        // Base folders
        World world = this.getWorld();
        File worldDir = world.getWorldFolder();
        File overworldRegion = new File(worldDir, "region");
        File netherRegion    = new File(worldDir, "DIM-1" + File.separator + "region");
        File endRegion       = new File(worldDir, "DIM1"  + File.separator + "region");

        // Compute cutoff timestamp
        long cutoffMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);

        // List all .mca files in the overworld region folder
        File[] files = overworldRegion.listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) return regions;

        for (File owFile : files) {
            // Skip if the overworld file is too recent
            if (owFile.lastModified() >= cutoffMillis) continue;

            // Parse region coords from filename "r.<x>.<z>.mca"
            String name = owFile.getName(); // e.g. "r.-2.3.mca"
            String coordsPart = name.substring(2, name.length() - 4);
            String[] parts = coordsPart.split("\\.");
            if (parts.length != 2) continue;  // malformed

            int rx, rz;
            try {
                rx = Integer.parseInt(parts[0]);
                rz = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                continue;
            }

            boolean include = true;

            // If nether flag is set, require nether region file also older than cutoff
            if (isNether) {
                File netherFile = new File(netherRegion, name);
                if (!netherFile.exists() || netherFile.lastModified() >= cutoffMillis) {
                    include = false;
                }
            }

            // If end flag is set, require end region file also older than cutoff
            if (isEnd) {
                File endFile = new File(endRegion, name);
                if (!endFile.exists() || endFile.lastModified() >= cutoffMillis) {
                    include = false;
                }
            }

            if (include) {
                regions.add(new Pair<>(rx, rz));
            }
        }

        return regions;
    }

    /**
     * Maps each old region to the set of island IDs whose island‐squares overlap it.
     *
     * <p>Each region covers blocks
     * [regionX*512 .. regionX*512 + 511] × [regionZ*512 .. regionZ*512 + 511].</p>
     *
     * <p>Each IslandData provides:
     * <ul>
     *   <li>{@code minX}, {@code minZ}: the southwest corner of its square</li>
     *   <li>{@code range}: half the side‐length of the island square</li>
     *   <li>{@code id}: the island’s unique identifier</li>
     * </ul>
     * The island’s maxX = minX + 2*range, maxZ = minZ + 2*range.</p>
     *
     * @param oldRegions the list of region coordinates to process
     * @param grid       a 2D TreeMap mapping centreX → (centreZ → IslandData)
     * @return           a map from region coords to the set of overlapping island IDs
     */
    public Map<Pair<Integer, Integer>, Set<String>> mapIslandsToRegions(
            List<Pair<Integer, Integer>> oldRegions,
            TreeMap<Integer, TreeMap<Integer, IslandData>> grid
            ) {
        final int BLOCKS_PER_REGION = 512;
        Map<Pair<Integer, Integer>, Set<String>> regionToIslands = new HashMap<>();

        for (Pair<Integer, Integer> region : oldRegions) {
            int rX = region.x;
            int rZ = region.z;

            int regionMinX = rX * BLOCKS_PER_REGION;
            int regionMinZ = rZ * BLOCKS_PER_REGION;
            int regionMaxX = regionMinX + BLOCKS_PER_REGION - 1;
            int regionMaxZ = regionMinZ + BLOCKS_PER_REGION - 1;

            Set<String> ids = new HashSet<>();

            // iterate all islands in the grid
            for (Map.Entry<Integer, TreeMap<Integer, IslandData>> xEntry : grid.entrySet()) {
                for (IslandData data : xEntry.getValue().values()) {
                    int islandMinX = data.minX();
                    int islandMinZ = data.minZ();
                    int islandMaxX = islandMinX + 2 * data.range();
                    int islandMaxZ = islandMinZ + 2 * data.range();

                    // overlap test
                    boolean overlaps = !(islandMaxX < regionMinX ||
                            islandMinX > regionMaxX ||
                            islandMaxZ < regionMinZ ||
                            islandMinZ > regionMaxZ);

                    if (overlaps) {
                        ids.add(data.id());
                    }
                }
            }

            if (!ids.isEmpty()) {
                regionToIslands.put(region, ids);
            }
        }

        return regionToIslands;
    } 

    /**
     * @return the inPurge
     */
    boolean isInPurge() {
        return inPurge;
    }

    /**
     * Stop the purge
     */
    void stop() {
        inPurge = false;
    }

    /**
     * @param user the user to set
     */
    void setUser(User user) {
        this.user = user;
    }

    /**
     * Returns the amount of purged islands.
     * @return the amount of islands that have been purged.
     * @since 1.13.0
     */
    int getPurgedIslandsCount() {
        return this.count;
    }

}
