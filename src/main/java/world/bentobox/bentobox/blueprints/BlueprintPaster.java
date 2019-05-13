package world.bentobox.bentobox.blueprints;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
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
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

    private BentoBox plugin;
    // The minimum block position (x,y,z)
    private Location pos1;
    // The maximum block position (x,y,z)
    private Location pos2;
    // Speed of pasting
    private int pasteSpeed;
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
        Location loc = location.toVector().subtract(clipboard.getOrigin().toVector()).toLocation(location.getWorld());
        paste(location.getWorld(), null, loc, clipboard.getBp(), task);
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
        Map<Vector, BlueprintBlock> blocks = blueprint.getBlocks() == null ? new HashMap<>() : blueprint.getBlocks();
        Map<Vector, BlueprintBlock> attached = blueprint.getAttached() == null ? new HashMap<>() : blueprint.getAttached();
        Map<Vector, List<BlueprintEntity>> entities = blueprint.getEntities() == null ? new HashMap<>() : blueprint.getEntities();
        Iterator<Entry<Vector, BlueprintBlock>> it = blocks.entrySet().iterator();
        Iterator<Entry<Vector, BlueprintBlock>> it2 = attached.entrySet().iterator();
        Iterator<Entry<Vector, List<BlueprintEntity>>> it3 = entities.entrySet().iterator();

        // Initial state & speed
        pasteState = PasteState.BLOCKS;
        pasteSpeed = plugin.getSettings().getPasteSpeed();

        pastingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int count = 0;
            while (pasteState.equals(PasteState.BLOCKS) && count < pasteSpeed && it.hasNext()) {
                pasteBlock(world, island, loc, it.next());
                count++;
            }
            while (it2 != null && pasteState.equals(PasteState.ATTACHMENTS) && count < pasteSpeed && it2.hasNext()) {
                pasteBlock(world, island, loc, it2.next());
                count++;
            }
            while (it3 != null && pasteState.equals(PasteState.ENTITIES) && count < pasteSpeed && it3.hasNext()) {
                pasteEntity(world, loc, it3.next());
                count++;
            }
            // STATE SHIFT
            if (pasteState.equals(PasteState.BLOCKS) && !it.hasNext()) {
                // Blocks done.
                if (it2 == null && it3 == null) {
                    // No attachments or entities
                    pasteState = PasteState.DONE;
                } else {
                    // Next paste attachments, otherwise skip to entities
                    pasteState = it2 != null ? PasteState.ATTACHMENTS : PasteState.ENTITIES;
                }
            }
            if (pasteState.equals(PasteState.ATTACHMENTS) && !it2.hasNext()) {
                // Attachments done. Next paste entities, otherwise done
                pasteState = it3 != null ? PasteState.ENTITIES : PasteState.DONE;
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
        // Set the block data
        block.setBlockData(Bukkit.createBlockData(bpBlock.getBlockData()));
        setBlockState(island, block, bpBlock);
        // pos1 and pos2 update
        updatePos(world, entry.getKey());
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
        if (bs instanceof Sign) {
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
        list.forEach(k -> {
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

    private void writeSign(Island island, Block block, List<String> lines) {
        Sign sign = (Sign) block.getState();
        org.bukkit.material.Sign s = (org.bukkit.material.Sign) sign.getData();
        // Handle spawn sign
        if (island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.SPAWN_HERE)) {
            block.setType(Material.AIR);
            // Orient to face same direction as sign
            Location spawnPoint = new Location(block.getWorld(), block.getX() + 0.5D, block.getY(),
                    block.getZ() + 0.5D, Util.blockFaceToFloat(s.getFacing().getOppositeFace()), 30F);
            island.setSpawnPoint(block.getWorld().getEnvironment(), spawnPoint);
            return;
        }
        // Handle locale text for starting sign
        // Sign text must be stored under the addon's name.sign.line0,1,2,3 in the yaml file
        if (island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.START_TEXT)) {
            // Get the addon that is operating in this world
            plugin.getIWM().getAddon(island.getWorld()).ifPresent(addon -> {
                lines.clear();
                for (int i = 0; i < 4; i++) {
                    lines.add(ChatColor.translateAlternateColorCodes('&', plugin.getLocalesManager().getOrDefault(User.getInstance(island.getOwner()),
                            addon.getDescription().getName().toLowerCase() + ".sign.line" + i,"")));
                }
            });
        }
        // Get the name of the player
        String name = TextVariables.NAME;
        if (island != null) {
            name = plugin.getPlayers().getName(island.getOwner());
        }
        // Sub in player's name
        for (int i = 0 ; i < lines.size(); i++) {
            sign.setLine(i, lines.get(i).replace(TextVariables.NAME, name));
        }
        // Update the sign
        sign.update();
    }

}
