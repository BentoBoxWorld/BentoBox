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
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * A class that calculates finds a safe spot asynchronously and then teleports the player there.
 * @author tastybento
 *
 */
public class SafeSpotTeleport {

    private enum State {
        CENTER, SURROUNDING, LAST_CHECK, FAILURE, CENTER_WAIT, SURROUNDING_WAIT
    }
    private static final long SPEED = 10;
    private State step = State.CENTER;
    private BukkitTask task;


    private BSkyBlock plugin;
    private final Entity entity;
    private final Location location;
    private final int homeNumber;
    private final boolean setHome;
    private int lastX;
    private int lastZ;
    private int chunksToCheck = 10;
    private int worldHeight = 255;
    private World world;
    private double safeDistance;
    private Vector safeSpotInChunk;
    private boolean safeSpotFound;
    private Vector portalPart;
    private ChunkSnapshot portalChunk;
    private ChunkSnapshot safeChunk;

    /**
     * Teleports and entity to a safe spot on island
     * @param plugin2
     * @param entity2
     * @param island
     * @param failureMessage
     * @param setHome2
     * @param homeNumber2
     */
    public SafeSpotTeleport(BSkyBlock plugin2, Entity entity2, Location location, String failureMessage, boolean setHome2,
            int homeNumber2) {
        this.plugin = plugin2;
        this.entity = entity2;
        this.setHome = setHome2;
        this.homeNumber = homeNumber2;
        this.location = location;

        // Put player into spectator mode
        if (entity instanceof Player && ((Player)entity).getGameMode().equals(GameMode.SURVIVAL)) {
            ((Player)entity).setGameMode(GameMode.SPECTATOR);
        }
        // Get world info
        world = location.getWorld();
        worldHeight = world.getEnvironment().equals(Environment.NETHER) ? world.getMaxHeight() - 20 : world.getMaxHeight() - 2;

        // Get island mins and max
        Island island = plugin.getIslands().getIslandAt(location).orElse(null);
        if (island == null) {
            if (entity instanceof Player) {
                User.getInstance((Player)entity).sendMessage(failureMessage);
            }
            return;
        }
        // Set the minimums and maximums
        lastX = island.getMinProtectedX() / 16;
        lastZ = island.getMinProtectedZ() / 16;
        int biggestX = (island.getMinProtectedX() + island.getProtectionRange() - 1) / 16;
        int biggestZ = (island.getMinProtectedZ() + island.getProtectionRange() - 1) / 16;

        // Start a recurring task until done or cancelled
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            List<ChunkSnapshot> chunkSnapshot = new ArrayList<>();
            switch (step) {
            case CENTER:
                // Add the center chunk
                chunkSnapshot.add(location.toVector().toLocation(world).getChunk().getChunkSnapshot());
                // Add immediately adjacent chunks
                for (int x = location.getChunk().getX()-1; x <= location.getChunk().getX()+1; x++) {
                    for (int z = location.getChunk().getZ()-1; z <= location.getChunk().getZ()+1; z++) {
                        if (x != location.getChunk().getX() || z != location.getChunk().getZ()) {
                            chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());
                        }
                    }
                }
                // Move to next step
                step = State.CENTER_WAIT;
                checkChunks(chunkSnapshot);
                break;
            case CENTER_WAIT:
                // Do nothing while the center scan is done
                break;
            case SURROUNDING:
                for (int x = lastX; x <= biggestX; x++) {
                    for (int z = lastZ; z <= biggestZ; z++) {
                        chunkSnapshot.add(world.getChunkAt(x, z).getChunkSnapshot());                     
                        if (chunkSnapshot.size() == chunksToCheck) {
                            lastX = x;
                            lastZ = z;
                            step = State.SURROUNDING_WAIT;
                            checkChunks(chunkSnapshot);
                            return;
                        }
                    }                   
                }
                // Last few chunks, may be none
                step = State.LAST_CHECK;
                checkChunks(chunkSnapshot);
                break;
            case SURROUNDING_WAIT:
                // Do nothing while the surrounding scan is done
                break;
            case LAST_CHECK:
                // Do nothing while the last few chunks are scanned
                break;
            case FAILURE:
                // We are done searching - failure
                task.cancel();
                if (entity instanceof Player) {
                    if (!failureMessage.isEmpty()) {
                        entity.sendMessage(failureMessage);
                    }
                }
            }
        }, 0L, SPEED);
    }

    private boolean checkChunks(List<ChunkSnapshot> chunkSnapshot) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            // Find a safe spot, defined as a solid block, with 2 air spaces above it
            //long time = System.nanoTime();
            int x = 0;
            int y = 0;
            int z = 0;
            double distance = 0D;

            for (ChunkSnapshot chunk: chunkSnapshot) {
                // Run through the chunk
                for (x = 0; x< 16; x++) {
                    for (z = 0; z < 16; z++) {
                        // Work down from the entry point up
                        for (y = Math.min(chunk.getHighestBlockYAt(x, z), worldHeight); y >= 0; y--) {
                            //System.out.println("Trying " + (16 * chunk.getX() + x) + " " + y + " " + (16 * chunk.getZ() + z));
                            // Check for portal - only if this is not a safe home search
                            if (!setHome && chunk.getBlockType(x, y, z).equals(Material.PORTAL)) {
                                if (portalPart == null || (distance > location.toVector().distanceSquared(new Vector(x,y,z)))) {
                                    // First one found or a closer one, save the chunk the position and the distance
                                    portalChunk = chunk;
                                    portalPart = new Vector(x,y,z);
                                    distance = portalPart.distanceSquared(location.toVector());
                                }
                            }
                            // Check for safe spot, but only if it is closer than one we have found already
                            if (!safeSpotFound || (safeDistance > location.toVector().distanceSquared(new Vector(x,y,z)))) {
                                // No safe spot yet, or closer distance
                                if (checkBlock(chunk,x,y,z, worldHeight)) {
                                    safeChunk = chunk;
                                    safeSpotFound = true;
                                    safeSpotInChunk = new Vector(x,y,z);
                                    safeDistance = location.toVector().distanceSquared(safeSpotInChunk);
                                }
                            }
                        }
                    } //end z
                } // end x
                // If this is not a home search do a check for portal
                if (!this.setHome) {
                    checkPortal();
                }

                // If successful, teleport otherwise move to the next step in the state machine
                if (safeSpotFound) {
                    task.cancel();
                    teleportEntity();
                } else if (step.equals(State.SURROUNDING_WAIT) || step.equals(State.CENTER_WAIT)) {
                    step = State.SURROUNDING;
                } else if (step.equals(State.LAST_CHECK)) {
                    step = State.FAILURE;
                } 
            }
        });
        return false;
    }


    /**
     * Teleports entity to the safe spot
     */
    private void teleportEntity() {
        final Vector spot = new Vector((16 *safeChunk.getX()) + 0.5D, 1, (16 * safeChunk.getZ()) + 0.5D).add(safeSpotInChunk);
        // Return to main thread and teleport the player
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location destination = spot.toLocation(world);
            if (setHome && entity instanceof Player) {
                plugin.getPlayers().setHomeLocation(entity.getUniqueId(), destination, homeNumber);
            }
            Vector velocity = entity.getVelocity();
            entity.teleport(destination);
            // Exit spectator mode if in it
            if (entity instanceof Player) {
                Player player = (Player)entity;
                if (player.getGameMode().equals(GameMode.SPECTATOR)) {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            } else {
                entity.setVelocity(velocity);
            }
        });

    }

    /**
     * Checks if a portal is safe
     */
    private void checkPortal() {
        if (portalPart == null) {
            return;
        }
        // There is a portal available, but is it safe?
        // Get the lowest portal spot
        int x = portalPart.getBlockX();
        int y = portalPart.getBlockY();
        int z = portalPart.getBlockZ();
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