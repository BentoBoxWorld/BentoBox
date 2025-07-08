package world.bentobox.bentobox.api.commands.admin.purge;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.island.IslandGrid;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

import world.bentobox.level.Level;

public class AdminPurgeRegionsCommand extends CompositeCommand implements Listener {

    private int count;
    private volatile boolean inPurge;
    private boolean toBeConfirmed;
     private User user;
    private Set<String> islands = new HashSet<>();
    private volatile int islandCount;
    private List<File> deletableFiles;
    private final Map<String, AtomicInteger> regionChunkCounts = new ConcurrentHashMap<>();

    public AdminPurgeRegionsCommand(CompositeCommand parent) {
        super(parent, "regions");
        getAddon().registerListener(this);
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
        if (args.get(0).equalsIgnoreCase("confirm") && toBeConfirmed && this.user.equals(user)) {
            if (deletableFiles.isEmpty()) {
                user.sendMessage("commands.admin.purge.none-found"); // Should never happen
                return false;
            }
            // Remove any regions that have recently been loaded for some reason
            int num = deletableFiles.size();
            filterDeletableFiles(deletableFiles); // Technically this could be used to delete just the unchanged ones
            // For now though, if there is any change, then stop and have them rescan
            if (num != deletableFiles.size()) {
                user.sendMessage("commands.admin.purge.regions-changed");
                return false;
            }
            // Delete them
            deleteRegionFiles();
            if (!deletableFiles.isEmpty()) {
                user.sendMessage("commands.admin.purge.not-all-deleted");
                return false;
            }
            user.sendMessage("general.success");
            toBeConfirmed = false;
            deletableFiles.clear();
            return true;
        }
        // Clear tbc
        toBeConfirmed = false;
        this.user = user;
        int days = Integer.parseInt(args.get(0));
        /*
        if (days < 1) {
            user.sendMessage("commands.admin.purge.days-one-or-more");
            return false;
        }*/
        user.sendMessage("commands.admin.purge.scanning");

        // Find the potential islands
        Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), ()-> removeIslands(getWorld(), days));
        return true;
    }
    
    /**
     * Deletes files from the deletableFiles list.
     * Files that are successfully deleted are removed from the list.
     * Any deletion errors are logged to the console via getPlugin().logError(...).
     */
    public void deleteRegionFiles() {
        Iterator<File> iterator = deletableFiles.iterator();

        while (iterator.hasNext()) {
            File file = iterator.next();
            try {
                if (file.exists() && file.delete()) {
                    iterator.remove(); // File deleted successfully
                } else {
                    getPlugin().logError("Failed to delete region file: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                getPlugin().logError("Error deleting file: " + file.getAbsolutePath() + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * Removes any region files from the given list that have loaded chunks.
     * @param deletableFiles list of region files potentially queued for deletion
     */
    public void filterDeletableFiles(List<File> deletableFiles) {
        deletableFiles.removeIf(file -> {
            String fileName = file.getName(); // e.g., r.2.-3.mca
            if (!fileName.startsWith("r.") || !fileName.endsWith(".mca")) {
                return false; // not a region file
            }

            String[] parts = fileName.substring(2, fileName.length() - 4).split("\\.");
            if (parts.length != 2) {
                return false; // invalid format
            }

            try {
                int regionX = Integer.parseInt(parts[0]);
                int regionZ = Integer.parseInt(parts[1]);
                String regionKey = regionX + "," + regionZ;
                return regionChunkCounts.containsKey(regionKey); // has loaded chunks
            } catch (NumberFormatException e) {
                return false; // skip malformed file names
            }
        });
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        String key = getRegionKey(event.getChunk());
        regionChunkCounts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        String key = getRegionKey(event.getChunk());
        regionChunkCounts.computeIfPresent(key, (k, count) -> {
            if (count.decrementAndGet() <= 0) return null;
            return count;
        });
    }

    private String getRegionKey(Chunk chunk) {
        return (chunk.getX() >> 5) + "," + (chunk.getZ() >> 5);
    }

    private void removeIslands(World world, int days) {
        try {
            IslandGrid grid = getPlugin().getIslands().getIslandCache().getIslandGrid(world);
            if (grid == null) {
                // There are no islands in this world yet!
                Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage("commands.admin.purge.none-found"));
                getPlugin().logDebug("No grid for " + world.getName());
                return;
            }
            List<Pair<Integer, Integer>> blockCoords = grid.getIslandCoordinates();

            // Set number of islands to zero
            islandCount = 0;
            
            // Group block coords by region file
            Map<File, Set<Pair<Integer, Integer>>> fileToCoords = mapCoordsToRegionFiles(blockCoords, days);

            deletableFiles = new ArrayList<>();

            for (Map.Entry<File, Set<Pair<Integer, Integer>>> entry : fileToCoords.entrySet()) {
                File regionFile = entry.getKey();
                Set<Pair<Integer, Integer>> coords = entry.getValue();

                boolean allDeletable = true;
                for (Pair<Integer, Integer> coord : coords) {
                    if (!canDelete(coord.getKey(), coord.getValue())) {
                        allDeletable = false;
                        break;
                    }
                }

                if (allDeletable) {
                    deletableFiles.add(regionFile);
                }
            }

            if (deletableFiles.isEmpty()) {
                Bukkit.getScheduler().runTask(getPlugin(), () -> user.sendMessage("commands.admin.purge.none-found"));
            } else {
                Bukkit.getScheduler().runTask(getPlugin(), () -> {
                    user.sendMessage("commands.admin.purge.purgable-islands", TextVariables.NUMBER, String.valueOf(islandCount));
                    user.sendMessage("commands.admin.purge.confirm", TextVariables.LABEL, this.getLabel());
                    this.toBeConfirmed = true;
                    });
            }
        } finally {
            inPurge = false;
        }
    }
    
    /**
     * Checks if this island can be deleted or not
     * @param x coordinate
     * @param z coordinate
     * @return true if it can be deleted
     */
    private boolean canDelete(Integer x, Integer z) {
        // Get the island and see if it is protected. If there is no island at this location, then it is deletable
        return getPlugin().getIslands().getIslandAt(new Location(getWorld(), x, 0, z)).map(is -> {
            
            if (checkIsland(is)) {
                islandCount++;
                getPlugin().log("Island at " + Util.xyz(is.getCenter().toVector()) 
                + " owned by " + getPlugin().getPlayers().getName(is.getOwner()) + " will be deleted if confirmed.");
                return true;
            } else {
                return false;
            }
            }).orElse(true);
    }
    
    /**
     * Only true if not purge protected, not spawn, and owned
     * @param island island
     * @return true if it can be deleted 
     */
    private boolean checkIsland(Island island) {
        // Level check 
        boolean levelCheck = getPlugin().getAddonsManager().getAddonByName("Level").map(l -> 
            ((Level) l).getIslandLevel(getWorld(), island.getOwner()) >= getPlugin().getSettings().getIslandPurgeLevel()).orElse(false);
        if (levelCheck) {
            // Island level is too high
            return false;
        }
       return !island.isPurgeProtected() && !island.isSpawn() && island.isOwned(); 
    }

    private Map<File, Set<Pair<Integer, Integer>>> mapCoordsToRegionFiles(List<Pair<Integer, Integer>> blockCoords, int days) {
        Map<File, Set<Pair<Integer, Integer>>> result = new HashMap<>();
        World world = this.getWorld();
        if (world == null) return result;

        // Region dirs for each dimension
        File overworldRegion = new File(world.getWorldFolder(), "region"); // Overworld
        File netherRegion = new File(world.getWorldFolder(), "DIM-1/region"); // Nether
        File endRegion = new File(world.getWorldFolder(), "DIM1/region"); // End

        long cutoff = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000);

        for (Pair<Integer, Integer> block : blockCoords) {
            int regionX = block.getKey() >> 9;
            int regionZ = block.getValue() >> 9;
            String filename = "r." + regionX + "." + regionZ + ".mca";

            File[] files = {
                new File(overworldRegion, filename),
                new File(netherRegion, filename),
                new File(endRegion, filename)
            };

            boolean allOldEnough = true;
            for (File file : files) {
                if (file.getParentFile().isDirectory() && file.exists()) {
                    if (file.lastModified() > cutoff) {
                        allOldEnough = false;
                        break;
                    }
                }
            }

            // Only add if all present files are old enough
            if (allOldEnough) {
                // Add all present files to result
                for (File file : files) {
                    if (file.getParentFile().isDirectory() && file.exists()) {
                        result.computeIfAbsent(file, k -> new HashSet<>()).add(block);
                    }
                }
            }
        }
        return result;
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
     * @param islands the islands to set
     */
    void setIslands(Set<String> islands) {
        this.islands = islands;
    }

    /**
     * Returns the amount of purged islands.
     * @return the amount of islands that have been purged.
     * @since 1.13.0
     */
    int getPurgedIslandsCount() {
        return this.count;
    }

    /**
     * Returns the amount of islands that can be purged.
     * @return the amount of islands that can be purged.
     * @since 1.13.0
     */
    int getPurgeableIslandsCount() {
        return this.islands.size();
    }
}
