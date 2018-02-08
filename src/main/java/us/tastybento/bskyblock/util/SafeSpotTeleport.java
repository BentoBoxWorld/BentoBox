package us.tastybento.bskyblock.util;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChunkSnapshot;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * A class that calculates finds a safe spot asynchronously and then teleports the player there. 
 * @author tastybento
 *
 */
public class SafeSpotTeleport {

    /**
     * Teleport to a safe place and if it fails, show a failure message
     * @param plugin
     * @param player
     * @param l
     * @param failureMessage
     */
    public SafeSpotTeleport(final BSkyBlock plugin, final Entity player, final Location l, final String failureMessage) {
        new SafeSpotTeleport(plugin, player, l, 1, failureMessage, false);
    }

    /**
     * Teleport to a safe place and set home
     * @param plugin
     * @param player
     * @param l
     * @param number
     */
    public SafeSpotTeleport(final BSkyBlock plugin, final Entity player, final Location l, final int number) {
        new SafeSpotTeleport(plugin, player, l, number, "", true);
    }

    /**
     * Teleport to a safe spot on an island
     * @param plugin
     * @param player
     * @param l
     */
    public SafeSpotTeleport(final BSkyBlock plugin, final Entity player, final Location l) {
        new SafeSpotTeleport(plugin, player, l, 1, "", false);
    }
    /**
     * Teleport to a safe spot on an island
     * 
     * TODO: REFACTOR THIS!

     * @param plugin
     * @param entity
     * @param islandLoc
     */
    public SafeSpotTeleport(final BSkyBlock plugin, final Entity entity, final Location islandLoc, final int homeNumber, final String failureMessage, final boolean setHome) {
        //this.plugin = plugin;
        //plugin.getLogger().info("DEBUG: running safe spot");
        // Get island
        Island island = plugin.getIslands().getIslandAt(islandLoc).orElse(null);
        if (island != null) {
            final World world = islandLoc.getWorld();
            // Get the chunks
            List<ChunkSnapshot> chunkSnapshot = new ArrayList<ChunkSnapshot>();
            // Add the center chunk
            chunkSnapshot.add(island.getCenter().toVector().toLocation(world).getChunk().getChunkSnapshot());
            // Add immediately adjacent chunks
            for (int x = islandLoc.getChunk().getX()-1; x <= islandLoc.getChunk().getX()+1; x++) {
                for (int z = islandLoc.getChunk().getZ()-1; z <= islandLoc.getChunk().getZ()+1; z++) {
                    if (x != islandLoc.getChunk().getX() || z != islandLoc.getChunk().getZ()) {
                        chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());
                    }
                }
            }
            // Add the rest of the island protected area
            for (int x = island.getMinProtectedX() /16; x <= (island.getMinProtectedX() + island.getProtectionRange() - 1)/16; x++) {
                for (int z = island.getMinProtectedZ() /16; z <= (island.getMinProtectedZ() + island.getProtectionRange() - 1)/16; z++) {
                    // This includes the center spots again, so is not as efficient...
                    chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());
                }
            }
            //plugin.getLogger().info("DEBUG: size of chunk ss = " + chunkSnapshot.size());
            final List<ChunkSnapshot> finalChunk = chunkSnapshot;
            int maxHeight = world.getMaxHeight() - 2;
            if (world.getEnvironment().equals(Environment.NETHER)) {
                // We need to ignore the roof
                maxHeight -= 20;
            }
            final int worldHeight = maxHeight;
            //plugin.getLogger().info("DEBUG:world height = " + worldHeight);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                // Find a safe spot, defined as a solid block, with 2 air spaces above it
                //long time = System.nanoTime();
                int x = 0;
                int y = 0;
                int z = 0;
                ChunkSnapshot safeChunk = null;
                ChunkSnapshot portalChunk = null;
                boolean safeSpotFound = false;
                Vector safeSpotInChunk = null;
                Vector portalPart = null;
                double distance = 0D;
                double safeDistance = 0D;
                for (ChunkSnapshot chunk: finalChunk) {
                    for (x = 0; x< 16; x++) {
                        for (z = 0; z < 16; z++) {
                            // Work down from the entry point up
                            for (y = Math.min(chunk.getHighestBlockYAt(x, z), worldHeight); y >= 0; y--) {
                                //System.out.println("Trying " + (16 * chunk.getX() + x) + " " + y + " " + (16 * chunk.getZ() + z));
                                // Check for portal - only if this is not a safe home search
                                if (!setHome && chunk.getBlockType(x, y, z).equals(Material.PORTAL)) {
                                    if (portalPart == null || (distance > islandLoc.toVector().distanceSquared(new Vector(x,y,z)))) {
                                        // First one found or a closer one, save the chunk the position and the distance
                                        portalChunk = chunk;
                                        portalPart = new Vector(x,y,z);
                                        distance = portalPart.distanceSquared(islandLoc.toVector());
                                    }
                                }
                                // Check for safe spot, but only if it is closer than one we have found already
                                if (!safeSpotFound || (safeDistance > islandLoc.toVector().distanceSquared(new Vector(x,y,z)))) {
                                    // No safe spot yet, or closer distance
                                    if (checkBlock(chunk,x,y,z, worldHeight)) {
                                        safeChunk = chunk;
                                        safeSpotFound = true;
                                        safeSpotInChunk = new Vector(x,y,z);
                                        safeDistance = islandLoc.toVector().distanceSquared(safeSpotInChunk);
                                    }
                                }
                            }
                        } //end z
                    } // end x
                    //if (safeSpotFound) {
                    //System.out.print("DEBUG: safe spot found " + safeSpotInChunk.toString());
                    //break search;
                    //}
                }
                // End search
                // Check if the portal is safe (it should be)
                if (portalPart != null) {
                    //System.out.print("DEBUG: Portal found");
                    // There is a portal available, but is it safe?
                    // Get the lowest portal spot
                    x = portalPart.getBlockX();
                    y = portalPart.getBlockY();
                    z = portalPart.getBlockZ();
                    while (portalChunk.getBlockType(x,y,z).equals(Material.PORTAL)) {
                        y--;
                    }
                    //System.out.print("DEBUG: Portal teleport loc = " + (16 * portalChunk.getX() + x) + "," + (y) + "," + (16 * portalChunk.getZ() + z));
                    // Now check if this is a safe location
                    if (checkBlock(portalChunk,x,y,z, worldHeight)) {
                        // Yes, so use this instead of the highest location
                        //System.out.print("DEBUG: Portal is safe");
                        safeSpotFound = true;
                        safeSpotInChunk = new Vector(x,y,z);
                        safeChunk = portalChunk;
                        // TODO: Add safe portal spot to island
                    }
                }
                //System.out.print("Seconds = " + ((System.nanoTime() - time) * 0.000000001));
                if (safeChunk != null && safeSpotFound) {
                    //final Vector spot = new Vector((16 *currentChunk.getX()) + x + 0.5D, y +1, (16 * currentChunk.getZ()) + z + 0.5D)
                    final Vector spot = new Vector((16 *safeChunk.getX()) + 0.5D, 1, (16 * safeChunk.getZ()) + 0.5D).add(safeSpotInChunk);
                    // Return to main thread and teleport the player
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        Location destination = spot.toLocation(islandLoc.getWorld());
                        if (setHome && entity instanceof Player) {
                            plugin.getPlayers().setHomeLocation(entity.getUniqueId(), destination, homeNumber);
                        }
                        Vector velocity = entity.getVelocity();
                        entity.teleport(destination);
                        entity.setVelocity(velocity);
                        // Exit spectator mode if in it
                        if (entity instanceof Player) {
                            Player player = (Player)entity;
                            if (player.getGameMode().equals(GameMode.SPECTATOR)) {
                                player.setGameMode(GameMode.SURVIVAL);
                            }
                        }
                    });
                } else {
                    // We did not find a spot
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        //plugin.getLogger().info("DEBUG: safe spot not found");
                        if (entity instanceof Player) {
                            if (!failureMessage.isEmpty()) {
                                entity.sendMessage(failureMessage);
                            } else {
                                entity.sendMessage("Warp not safe");
                            }
                        }
                    });
                }
            });
        }
    }

    /**
     * Returns true if the location is a safe one.
     * @param chunk
     * @param x
     * @param y
     * @param z
     * @param worldHeight
     * @return
     */
    @SuppressWarnings("deprecation")
    private boolean checkBlock(ChunkSnapshot chunk, int x, int y, int z, int worldHeight) {
        int type = chunk.getBlockTypeId(x, y, z);
        if (type != 0) { // AIR
            int space1 = chunk.getBlockTypeId(x, Math.min(y + 1, worldHeight), z);
            int space2 = chunk.getBlockTypeId(x, Math.min(y + 2, worldHeight), z);
            if ((space1 == 0 && space2 == 0) || (space1 == Material.PORTAL.getId() || space2 == Material.PORTAL.getId())) {
                // Now there is a chance that this is a safe spot
                // Check for safe ground
                Material mat = Material.getMaterial(type);
                if (!mat.toString().contains("FENCE")
                        && !mat.toString().contains("DOOR")
                        && !mat.toString().contains("GATE")
                        && !mat.toString().contains("PLATE")) {
                    switch (mat) {
                    // Unsafe
                    case ANVIL:
                    case BARRIER:
                    case BOAT:
                    case CACTUS:
                    case DOUBLE_PLANT:
                    case ENDER_PORTAL:
                    case FIRE:
                    case FLOWER_POT:
                    case LADDER:
                    case LAVA:
                    case LEVER:
                    case LONG_GRASS:
                    case PISTON_EXTENSION:
                    case PISTON_MOVING_PIECE:
                    case PORTAL:
                    case SIGN_POST:
                    case SKULL:
                    case STANDING_BANNER:
                    case STATIONARY_LAVA:
                    case STATIONARY_WATER:
                    case STONE_BUTTON:
                    case TORCH:
                    case TRIPWIRE:
                    case WATER:
                    case WEB:
                    case WOOD_BUTTON:
                        //System.out.println("Block is dangerous " + mat.toString());
                        break;
                    default:
                        // Safe
                        // System.out.println("Block is safe " + mat.toString());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}