package us.tastybento.bskyblock.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.nms.NMSAbstraction;

//import com.wasteofplastic.askyblock.nms.NMSAbstraction;

/**
 * Deletes islands fast using chunk regeneration
 * 
 * @author tastybento
 * 
 */
public class DeleteIslandBlocks {
    protected static final int CLEAN_RATE = 2;
    private Set<Pair> chunksToClear = new HashSet<Pair>();
    //private HashMap<Location, Material> blocksToClear = new HashMap<Location,Material>();
    private NMSAbstraction nms = null;

    /**
     * Deletes the island
     * @param plugin
     * @param island
     */
    public DeleteIslandBlocks(final BSkyBlock plugin, final Island island) {
        plugin.getLogger().info("DEBUG: deleting the island");
        final World world = island.getCenter().getWorld();
        if (world == null)
            return;
        // Determine if blocks need to be cleaned up or not
        boolean cleanUpBlocks = false;
        plugin.getLogger().info("DEBUG: island protection = " + island.getProtectionRange());
        // DEBUG
        island.setProtectionRange(Settings.islandDistance);
        if (Settings.islandDistance - island.getProtectionRange() < 16) {
            cleanUpBlocks = true;
        }
        int range = island.getProtectionRange() / 2 * +1;
        final int minx = island.getMinProtectedX();
        final int minz = island.getMinProtectedZ();
        final int maxx = island.getMinProtectedX() + island.getProtectionRange();
        final int maxz = island.getMinProtectedZ() + island.getProtectionRange();
        plugin.getLogger().info("DEBUG: protection limits are: " + minx + ", " + minz + " to " + maxx + ", " + maxz );
        int islandSpacing = Settings.islandDistance - island.getProtectionRange();
        int minxX = (island.getCenter().getBlockX() - range - islandSpacing);
        int minzZ = (island.getCenter().getBlockZ() - range - islandSpacing);
        int maxxX = (island.getCenter().getBlockX() + range + islandSpacing);
        int maxzZ = (island.getCenter().getBlockZ() + range + islandSpacing);
        // plugin.getLogger().info("DEBUG: absolute max limits are: " + minxX +
        // ", " + minzZ + " to " + maxxX + ", " + maxzZ );
        // get the chunks for these locations
        final Chunk minChunk = world.getBlockAt(minx,0,minz).getChunk();
        final Chunk maxChunk = world.getBlockAt(maxx, 0, maxz).getChunk();

        // Find out what chunks are within the island protection range
        // plugin.getLogger().info("DEBUG: chunk limits are: " +
        // (minChunk.getBlock(0, 0, 0).getLocation().getBlockX()) + ", " +
        // (minChunk.getBlock(0, 0, 0).getLocation().getBlockZ())
        // + " to " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockX()) +
        // ", " + (maxChunk.getBlock(15, 0, 15).getLocation().getBlockZ()));

        for (int x = minChunk.getX(); x <= maxChunk.getX(); x++) {
            for (int z = minChunk.getZ(); z <= maxChunk.getZ(); z++) {
                boolean regen = true;

                if (world.getChunkAt(x, z).getBlock(0, 0, 0).getX() < minxX) {
                    // plugin.getLogger().info("DEBUG: min x coord is less than absolute min! "
                    // + minxX);
                    regen = false;
                }
                if (world.getChunkAt(x, z).getBlock(0, 0, 0).getZ() < minzZ) {
                    // plugin.getLogger().info("DEBUG: min z coord is less than absolute min! "
                    // + minzZ);
                    regen = false;
                }
                if (world.getChunkAt(x, z).getBlock(15, 0, 15).getX() > maxxX) {
                    // plugin.getLogger().info("DEBUG: max x coord is more than absolute max! "
                    // + maxxX);
                    regen = false;
                }
                if (world.getChunkAt(x, z).getBlock(15, 0, 15).getZ() > maxzZ) {
                    // plugin.getLogger().info("DEBUG: max z coord in chunk is more than absolute max! "
                    // + maxzZ);
                    regen = false;
                }
                if (regen) {
                    world.regenerateChunk(x, z);
                    if (Settings.netherIslands && Settings.netherGenerate) {
                        if (world.equals(IslandWorld.getIslandWorld())) {
                            IslandWorld.getNetherWorld().regenerateChunk(x, z);
                        }
                        if (world.equals(IslandWorld.getNetherWorld())) {
                            IslandWorld.getIslandWorld().regenerateChunk(x, z);
                        }
                    }
                } else {
                    // Add to clear up list if requested
                    if (cleanUpBlocks) {
                        chunksToClear.add(new Pair(x,z));
                    }
                }
            }
        }
        // Do not do this: Remove from database
        //plugin.getIslands().deleteIsland(island.getCenter());
        // Clear up any chunks
        if (!chunksToClear.isEmpty()) {
            try {
                nms = Util.getNMSHandler();
            } catch (Exception ex) {
                plugin.getLogger().warning("Cannot clean up blocks because there is no NMS acceleration available");
                return;
            }
            plugin.getLogger().info("Island delete: There are " + chunksToClear.size() + " chunks that need to be cleared up.");
            plugin.getLogger().info("Clean rate is " + CLEAN_RATE + " chunks per second. Should take ~" + Math.round(chunksToClear.size()/CLEAN_RATE) + "s");
            new BukkitRunnable() {
                @SuppressWarnings("deprecation")
                @Override
                public void run() {
                    Iterator<Pair> it = chunksToClear.iterator();
                    int count = 0;
                    while (it.hasNext() && count++ < CLEAN_RATE) {                    
                        Pair pair = it.next();
                        //plugin.getLogger().info("DEBUG: There are " + chunksToClear.size() + " chunks that need to be cleared up");
                        //plugin.getLogger().info("DEBUG: Deleting chunk " + pair.getLeft() + ", " + pair.getRight());                       
                        // Check if coords are in island space
                        for (int x = 0; x < 16; x ++) {
                            for (int z = 0; z < 16; z ++) {
                                int xCoord = pair.getLeft() * 16 + x;
                                int zCoord = pair.getRight() * 16 + z;
                                if (island.inIslandSpace(xCoord, zCoord)) {                                 
                                    //plugin.getLogger().info(xCoord + "," + zCoord + " is in island space - deleting column");
                                    // Delete all the blocks here
                                    for (int y = 0; y < IslandWorld.getIslandWorld().getMaxHeight(); y ++) {
                                        // Overworld
                                        Block b = IslandWorld.getIslandWorld().getBlockAt(xCoord, y, zCoord);                                       
                                        Material bt = b.getType();
                                        Material setTo = Material.AIR;
                                        // Split depending on below or above water line
                                        if (y < Settings.seaHeight) {
                                            setTo = Material.STATIONARY_WATER;
                                        }
                                        // Grab anything out of containers (do that it is
                                        // destroyed)                                  
                                        switch (bt) {
                                        case CHEST:
                                        case TRAPPED_CHEST:                                           
                                        case FURNACE:
                                        case DISPENSER:
                                        case HOPPER:
                                            final InventoryHolder ih = ((InventoryHolder)b.getState());
                                            ih.getInventory().clear();                                            
                                            b.setType(setTo);
                                            break;
                                        case AIR:   
                                            if (setTo.equals(Material.STATIONARY_WATER)) {
                                                nms.setBlockSuperFast(b, setTo.getId(), (byte)0, false);
                                            }
                                        case STATIONARY_WATER:
                                            if (setTo.equals(Material.AIR)) {
                                                nms.setBlockSuperFast(b, setTo.getId(), (byte)0, false);
                                            }
                                        default:
                                            nms.setBlockSuperFast(b, setTo.getId(), (byte)0, false);
                                            break;
                                        }
                                        // Nether, if it exists
                                        if (Settings.netherIslands && Settings.netherGenerate && y < IslandWorld.getNetherWorld().getMaxHeight() - 8) {
                                            b = IslandWorld.getNetherWorld().getBlockAt(xCoord, y, zCoord);                                       
                                            bt = b.getType();
                                            if (!b.equals(Material.AIR)) {
                                                setTo = Material.AIR;                                            
                                                // Grab anything out of containers (do that it is
                                                // destroyed)                                  
                                                switch (bt) {
                                                case CHEST:
                                                case TRAPPED_CHEST:                                           
                                                case FURNACE:
                                                case DISPENSER:
                                                case HOPPER:
                                                    final InventoryHolder ih = ((InventoryHolder)b.getState());
                                                    ih.getInventory().clear();                                            
                                                    b.setType(setTo);
                                                    break;
                                                 default:
                                                    nms.setBlockSuperFast(b, setTo.getId(), (byte)0, false);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }                                
                            }
                        }
                        it.remove();                        
                    } 
                    if (chunksToClear.isEmpty()){
                        plugin.getLogger().info("Finished island deletion");
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);

        }
    }
    
    /**
     * Class that pairs two ints together
     * @author tastybento
     *
     */
    public class Pair {
        private final int left;
        private final int right;

        public Pair(int left, int right) {
            this.left = left;
            this.right = right;
        }

        public int getLeft() {
            return left;
        }

        public int getRight() {
            return right;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (!(o instanceof Pair))
                return false;
            Pair pairo = (Pair) o;
            return (this.left == pairo.getLeft()) && (this.right == pairo.getRight());
        }
    }
}