package world.bentobox.bentobox.blueprints;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.material.Colorable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * This class pastes the clipboard it is given
 * @author tastybento
 *
 */
public class BlueprintPaster {

    enum PasteState {
        BLOCKS,
        ATTACHMENTS,
        ENTITIES,
        DONE,
        CANCEL
    }

    private static final String MINECRAFT = "minecraft:";

    private static final Map<String, String> BLOCK_CONVERSION = ImmutableMap.of("sign", "oak_sign", "wall_sign", "oak_wall_sign");

    private BentoBox plugin;
    // The minimum block position (x,y,z)
    private Location pos1;
    // The maximum block position (x,y,z)
    private Location pos2;
    private PasteState pasteState;
    private BukkitTask pastingTask;
    private BlueprintClipboard clipboard;

    /**
     * Paste a clipboard to a location and run task
     * @param plugin - BentoBox
     * @param clipboard - clipboard to paste
     * @param location - location to paste to
     * @param task - task to run after pasting, null if none
     */
    public BlueprintPaster(@NonNull BentoBox plugin, @NonNull BlueprintClipboard clipboard, @NonNull Location location, @Nullable Runnable task) {
        this.plugin = plugin;
        this.clipboard = clipboard;
        // Calculate location for pasting
        paste(location.getWorld(), null, location, clipboard.getBlueprint(), task);
    }

    /**
     * Pastes a blueprint to an island
     * @param plugin - BentoBox
     * @param bp - blueprint to paste
     * @param world - world to paste to
     * @param island - island related to this paste
     * @param task - task to run after pasting
     */
    public BlueprintPaster(@NonNull BentoBox plugin, Blueprint bp, World world, Island island, Runnable task) {
        this.plugin = plugin;
        // Offset due to bedrock
        Vector off = bp.getBedrock() != null ? bp.getBedrock() : new Vector(0,0,0);
        // Calculate location for pasting
        Location loc = island.getCenter().toVector().subtract(off).toLocation(world);
        // Paste
        paste(world, island, loc, bp, task);
    }

    /**
     * The main pasting method
     * @param world - world to paste to
     * @param island - the island related to this pasting - may be null
     * @param loc - the location to paste to
     * @param blueprint - the blueprint to paste
     * @param task - task to run after pasting
     */
    private void paste(@NonNull World world, @Nullable Island island, @NonNull Location loc, @NonNull Blueprint blueprint, @Nullable Runnable task) {
        // Iterators for the various maps to paste
        Map<Vector, BlueprintBlock> blocks = blueprint.getBlocks() == null ? Collections.emptyMap() : blueprint.getBlocks();
        Map<Vector, BlueprintBlock> attached = blueprint.getAttached() == null ? Collections.emptyMap() : blueprint.getAttached();
        Map<Vector, List<BlueprintEntity>> entities = blueprint.getEntities() == null ? Collections.emptyMap() : blueprint.getEntities();
        Iterator<Entry<Vector, BlueprintBlock>> it = blocks.entrySet().iterator();
        Iterator<Entry<Vector, BlueprintBlock>> it2 = attached.entrySet().iterator();
        Iterator<Entry<Vector, List<BlueprintEntity>>> it3 = entities.entrySet().iterator();

        // Initial state & speed
        pasteState = PasteState.BLOCKS;
        final int pasteSpeed = plugin.getSettings().getPasteSpeed();

        pastingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int count = 0;
            while (pasteState.equals(PasteState.BLOCKS) && count < pasteSpeed && it.hasNext()) {
                pasteBlock(world, island, loc, it.next());
                count++;
            }
            while (pasteState.equals(PasteState.ATTACHMENTS) && count < pasteSpeed && it2.hasNext()) {
                pasteBlock(world, island, loc, it2.next());
                count++;
            }
            while (pasteState.equals(PasteState.ENTITIES) && count < pasteSpeed && it3.hasNext()) {
                pasteEntity(world, loc, it3.next());
                count++;
            }
            // STATE SHIFT
            if (pasteState.equals(PasteState.BLOCKS) && !it.hasNext()) {
                // Blocks done
                // Next paste attachments
                pasteState = PasteState.ATTACHMENTS;
            }
            if (pasteState.equals(PasteState.ATTACHMENTS) && !it2.hasNext()) {
                // Attachments done. Next paste entities
                pasteState = PasteState.ENTITIES;
            }
            if (pasteState.equals(PasteState.ENTITIES) && !it3.hasNext()) {
                pasteState = PasteState.DONE;
            }
            if (pasteState.equals(PasteState.DONE)) {
                // All done. Cancel task
                // Set pos1 and 2 if this was a clipboard paste
                if (island == null && clipboard != null &&(clipboard.getPos1() == null || clipboard.getPos2() == null)) {
                    clipboard.setPos1(pos1);
                    clipboard.setPos2(pos2);
                }
                if (task != null) {
                    // Run follow-on task if it exists
                    Bukkit.getScheduler().runTask(plugin, task);
                }
                pasteState = PasteState.CANCEL;
            } else if (pasteState.equals(PasteState.CANCEL)) {
                // This state makes sure the follow-on task only ever runs once
                pastingTask.cancel();
            }
        }, 0L, 1L);

    }

    private void pasteBlock(World world, Island island, Location location, Entry<Vector, BlueprintBlock> entry) {
        Location pasteTo = location.clone().add(entry.getKey());
        BlueprintBlock bpBlock = entry.getValue();
        Block block = pasteTo.getBlock();
        // Set the block data - default is AIR
        BlockData bd;
        try {
            bd = Bukkit.createBlockData(bpBlock.getBlockData());
        } catch (Exception e) {
            bd = convertBlockData(world, bpBlock);
        }
        block.setBlockData(bd, false);
        setBlockState(island, block, bpBlock);
        // pos1 and pos2 update
        updatePos(world, entry.getKey());
    }

    /**
     * Tries to convert the BlockData to a newer version, and logs a warning if it fails to do so.
     * @return the converted BlockData or a default AIR BlockData.
     * @since 1.6.0
     */
    private BlockData convertBlockData(World world, BlueprintBlock block) {
        BlockData blockData = Bukkit.createBlockData(Material.AIR);
        try {
            for (Entry<String, String> en : BLOCK_CONVERSION.entrySet()) {
                if (block.getBlockData().startsWith(MINECRAFT + en.getKey())) {
                    blockData = Bukkit.createBlockData(block.getBlockData().replace(MINECRAFT + en.getKey(), MINECRAFT + en.getValue()));
                    break;
                }
            }
        } catch (IllegalArgumentException e) {
            // This may happen if the block type is no longer supported by the server
            plugin.logWarning("Blueprint references materials not supported on this server version.");
            plugin.logWarning("Load blueprint manually, check and save to fix for this server version.");
            plugin.logWarning("World: " + world.getName() + "; Failed block data: " + block.getBlockData());
        }
        return blockData;
    }

    private void pasteEntity(World world, Location location, Entry<Vector, List<BlueprintEntity>> entry) {
        int x = location.getBlockX() + entry.getKey().getBlockX();
        int y = location.getBlockY() + entry.getKey().getBlockY();
        int z = location.getBlockZ() + entry.getKey().getBlockZ();
        setEntity(new Location(world, x, y, z), entry.getValue());
    }

    /**
     * Handles signs, chests and mob spawner blocks
     * @param island - island
     * @param block - block
     * @param bpBlock - config
     */
    private void setBlockState(Island island, Block block, BlueprintBlock bpBlock) {
        // Get the block state
        BlockState bs = block.getState();
        // Signs
        if (bs instanceof org.bukkit.block.Sign) {
            writeSign(island, block, bpBlock.getSignLines());
        }
        // Chests, in general
        if (bs instanceof InventoryHolder) {
            bs.update(true, false);
            Inventory ih = ((InventoryHolder)bs).getInventory();
            // Double chests are pasted as two blocks so inventory is filled twice. This code stops over filling for the first block.
            bpBlock.getInventory().forEach(ih::setItem);
        }
        // Mob spawners
        if (bs instanceof CreatureSpawner) {
            CreatureSpawner spawner = ((CreatureSpawner) bs);
            BlueprintCreatureSpawner s = bpBlock.getCreatureSpawner();
            spawner.setSpawnedType(s.getSpawnedType());
            spawner.setMaxNearbyEntities(s.getMaxNearbyEntities());
            spawner.setMaxSpawnDelay(s.getMaxSpawnDelay());
            spawner.setMinSpawnDelay(s.getMinSpawnDelay());
            spawner.setDelay(s.getDelay());
            spawner.setRequiredPlayerRange(s.getRequiredPlayerRange());
            spawner.setSpawnRange(s.getSpawnRange());
            bs.update(true, false);
        }
    }

    /**
     * Sets any entity that is in this location
     * @param location - location
     * @param list - list of entities to paste
     */
    private void setEntity(Location location, List<BlueprintEntity> list) {
        list.stream().filter(k -> k.getType() != null).forEach(k -> {
            // Center, and just a bit high
            Location center = location.add(new Vector(0.5, 0.5, 0.5));
            LivingEntity e = (LivingEntity)location.getWorld().spawnEntity(center, k.getType());
            if (k.getCustomName() != null) {
                e.setCustomName(k.getCustomName());
            }
            if (e instanceof Colorable && k.getColor() != null) {
                ((Colorable) e).setColor(k.getColor());
            }
            if (e instanceof Tameable && k.getTamed() != null) {
                ((Tameable)e).setTamed(k.getTamed());
            }
            if (e instanceof ChestedHorse && k.getChest() != null) {
                ((ChestedHorse)e).setCarryingChest(k.getChest());
            }
            if (e instanceof Ageable && k.getAdult() != null) {
                if (k.getAdult()) {
                    ((Ageable)e).setAdult();
                } else {
                    ((Ageable)e).setBaby();
                }
            }
            if (e instanceof AbstractHorse) {
                AbstractHorse horse = (AbstractHorse)e;
                if (k.getDomestication() != null) horse.setDomestication(k.getDomestication());
                if (k.getInventory() != null) {
                    k.getInventory().forEach(horse.getInventory()::setItem);
                }
            }
            if (e instanceof Horse && k.getStyle() != null) {
                ((Horse)e).setStyle(k.getStyle());
            }
        });

    }

    /**
     * Tracks the minimum and maximum block positions
     * @param world - world
     * @param v - vector
     */
    private void updatePos(World world, Vector v) {
        if (pos1 == null) {
            pos1 = v.toLocation(world);
        }
        if (pos2 == null) {
            pos2 = v.toLocation(world);
        }
        if (v.getBlockX() < pos1.getBlockX()) {
            pos1.setX(v.getBlockX());
        }
        if (v.getBlockX() > pos2.getBlockX()) {
            pos2.setX(v.getBlockX());
        }
        if (v.getBlockY() < pos1.getBlockY()) {
            pos1.setY(v.getBlockY());
        }
        if (v.getBlockY() > pos2.getBlockY()) {
            pos2.setY(v.getBlockY());
        }
        if (v.getBlockZ() < pos1.getBlockZ()) {
            pos1.setZ(v.getBlockZ());
        }
        if (v.getBlockZ() > pos2.getBlockZ()) {
            pos2.setZ(v.getBlockZ());
        }
    }

    private void writeSign(final Island island, final Block block, final List<String> lines) {
        BlockFace bf;
        if (block.getType().name().contains("WALL_SIGN")) {
            WallSign wallSign = (WallSign)block.getBlockData();
            bf = wallSign.getFacing();
        } else {
            Sign sign = (Sign)block.getBlockData();
            bf = sign.getRotation();
        }
        // Handle spawn sign
        if (island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.SPAWN_HERE)) {
            block.setType(Material.AIR);
            // Orient to face same direction as sign
            Location spawnPoint = new Location(block.getWorld(), block.getX() + 0.5D, block.getY(),
                    block.getZ() + 0.5D, Util.blockFaceToFloat(bf.getOppositeFace()), 30F);
            island.setSpawnPoint(block.getWorld().getEnvironment(), spawnPoint);
            return;
        }
        // Get the name of the player
        String name = "";
        if (island != null) {
            name = plugin.getPlayers().getName(island.getOwner());
        }
        // Handle locale text for starting sign
        org.bukkit.block.Sign s = (org.bukkit.block.Sign)block.getState();
        // Sign text must be stored under the addon's name.sign.line0,1,2,3 in the yaml file
        if (island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.START_TEXT)) {
            // Get the addon that is operating in this world
            String addonName = plugin.getIWM().getAddon(island.getWorld()).map(addon -> addon.getDescription().getName().toLowerCase()).orElse("");
            for (int i = 0; i < 4; i++) {
                s.setLine(i, ChatColor.translateAlternateColorCodes('&', plugin.getLocalesManager().getOrDefault(User.getInstance(island.getOwner()),
                        addonName + ".sign.line" + i,"").replace(TextVariables.NAME, name)));
            }
        } else {
            // Just paste
            for (int i = 0; i < 4; i++) {
                s.setLine(i, lines.get(i));
            }
        }
        // Update the sign
        s.update();
    }

}
