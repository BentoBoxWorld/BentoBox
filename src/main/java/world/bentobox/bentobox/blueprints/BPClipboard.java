package world.bentobox.bentobox.blueprints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
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
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Colorable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.blueprints.BP_Block;
import world.bentobox.bentobox.api.blueprints.BP_CreatureSpawner;
import world.bentobox.bentobox.api.blueprints.BP_Entity;
import world.bentobox.bentobox.api.blueprints.Blueprint;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BPClipboard {

    private @Nullable Blueprint bp;
    private @Nullable Location pos1;
    private @Nullable Location pos2;
    private @Nullable Location origin;
    private BukkitTask copyTask;
    private int count;
    private boolean copying;
    private int index;
    private int lastPercentage;
    private Map<Vector, List<BP_Entity>> bpEntities = new HashMap<>();
    private Map<Vector, BP_Block> bpAttachable = new HashMap<>();
    private Map<Vector, BP_Block> bpBlocks = new HashMap<>();

    public BPClipboard(@NonNull Blueprint bp) {
        this.bp = bp;
    }

    public BPClipboard() {
        super();
    }

    /**
     * Copy the blocks between pos1 and pos2 into the clipboard for a user.
     * This will erase any previously registered data from the clipboard.
     * Copying is done async.
     * @param user - user
     * @return true if successful, false if pos1 or pos2 are undefined.
     */
    public boolean copy(User user, boolean copyAir) {
        if (copying) {
            user.sendMessage("commands.admin.schem.mid-copy");
            return false;
        }
        origin = origin == null ? user.getLocation() : origin;
        if (pos1 == null || pos2 == null) {
            user.sendMessage("commands.admin.schem.need-pos1-pos2");
            return false;
        }

        user.sendMessage("commands.admin.schem.copying");

        // World
        World world = pos1.getWorld();
        // Clear the clipboard
        bp = new Blueprint();

        count = 0;
        index = 0;
        lastPercentage = 0;
        BoundingBox toCopy = BoundingBox.of(pos1, pos2);
        bp.setxSize((int)toCopy.getWidthX());
        bp.setySize((int)toCopy.getHeight());
        bp.setzSize((int)toCopy.getWidthZ());

        BentoBox plugin = BentoBox.getInstance();

        final int speed = plugin.getSettings().getPasteSpeed();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            final List<Vector> vectorsToCopy = getVectors(toCopy);
            copying = false;
            copyTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (copying) {
                    return;
                }
                copying = true;
                vectorsToCopy.stream().skip(index).limit(speed).forEach(v -> {
                    List<LivingEntity> ents = world.getLivingEntities().stream()
                            .filter(Objects::nonNull)
                            .filter(e -> !(e instanceof Player))
                            .filter(e -> new Vector(e.getLocation().getBlockX(),
                                    e.getLocation().getBlockY(),
                                    e.getLocation().getBlockZ()).equals(v))
                            .collect(Collectors.toList());
                    if (copyBlock(v.toLocation(world), origin, copyAir, ents)) {
                        count++;
                    }
                });
                index += speed;
                int percent = (int)(index * 100 / (double)vectorsToCopy.size());
                if (percent != lastPercentage && percent % 10 == 0) {
                    user.sendMessage("commands.admin.schem.copied-percent", TextVariables.NUMBER, String.valueOf(percent));
                    lastPercentage = percent;
                }
                if (index > vectorsToCopy.size()) {
                    copyTask.cancel();
                    bp.setAttached(bpAttachable);
                    bp.setBlocks(bpBlocks);
                    bp.setEntities(bpEntities);
                    user.sendMessage("general.success");
                    user.sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, String.valueOf(count));
                }
                copying = false;
            }, 0L, 1L);
        });
        return true;
    }

    /**
     * Get all the x,y,z coords that must be copied
     * @param b - bounding box
     * @return - list of vectors
     */
    private List<Vector> getVectors(BoundingBox b) {
        List<Vector> r = new ArrayList<>();
        for (int x = (int)b.getMinX(); x <= b.getMaxX(); x++) {
            for (int y = (int)b.getMinY(); y <= b.getMaxY(); y++) {
                for (int z = (int)b.getMinZ(); z <= b.getMaxZ(); z++) {
                    r.add(new Vector(x,y,z));
                }
            }
        }
        return r;
    }

    private boolean copyBlock(Location l, Location copyOrigin, boolean copyAir, Collection<LivingEntity> entities) {
        Block block = l.getBlock();
        if (!copyAir && block.getType().equals(Material.AIR) && entities.isEmpty()) {
            return false;
        }
        // Create position
        int x = l.getBlockX() - copyOrigin.getBlockX();
        int y = l.getBlockY() - copyOrigin.getBlockY();
        int z = l.getBlockZ() - copyOrigin.getBlockZ();
        Vector pos = new Vector(x, y, z);

        // Set entities
        List<BP_Entity> bpEnts = new ArrayList<>();
        for (LivingEntity entity: entities) {
            BP_Entity bpe = new BP_Entity();
            bpe.setType(entity.getType());
            bpe.setCustomName(entity.getCustomName());
            if (entity instanceof Colorable) {
                Colorable c = (Colorable)entity;
                if (c.getColor() != null) {
                    bpe.setColor(c.getColor());
                }
            }
            if (entity instanceof Tameable) {
                bpe.setTamed(((Tameable)entity).isTamed());
            }
            if (entity instanceof ChestedHorse) {
                bpe.setChest(((ChestedHorse)entity).isCarryingChest());
            }
            if (entity instanceof Ageable) {
                // Only set if child. Most animals are adults
                if (!((Ageable)entity).isAdult()) bpe.setAdult(false);
            }
            if (entity instanceof AbstractHorse) {
                AbstractHorse horse = (AbstractHorse)entity;
                bpe.setDomestication(horse.getDomestication());
                bpe.setInventory(new HashMap<>());
                for (int index = 0; index < horse.getInventory().getSize(); index++) {
                    ItemStack i = horse.getInventory().getItem(index);
                    if (i != null) {
                        bpe.getInventory().put(index, i);
                    }
                }
            }

            if (entity instanceof Horse) {
                Horse horse = (Horse)entity;
                bpe.setStyle(horse.getStyle());
            }
            bpEnts.add(bpe);
        }
        // Store
        if (!bpEnts.isEmpty()) {
            bpEntities.put(pos, bpEnts);
        }

        // Return if this is just air block
        if (!copyAir && block.getType().equals(Material.AIR) && !entities.isEmpty()) {
            return true;
        }

        // Block state
        BlockState blockState = block.getState();
        BP_Block b = new BP_Block(block.getBlockData().getAsString());
        // Signs
        if (blockState instanceof Sign) {
            Sign sign = (Sign)blockState;
            b.setSignLines(Arrays.asList(sign.getLines()));
        }
        // Set block data
        if (blockState.getData() instanceof Attachable) {
            // Placeholder for attachment
            bpBlocks.put(pos, new BP_Block("minecraft:air"));
            bpAttachable.put(pos, b);
            return true;
        }

        if (block.getType().equals(Material.BEDROCK)) {
            bp.setBedrock(pos);
        }

        // Chests
        if (blockState instanceof InventoryHolder) {
            b.setInventory(new HashMap<>());
            InventoryHolder ih = (InventoryHolder)blockState;
            for (int index = 0; index < ih.getInventory().getSize(); index++) {
                ItemStack i = ih.getInventory().getItem(index);
                if (i != null) {
                    b.getInventory().put(index, i);
                }
            }
        }

        if (blockState instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner)blockState;
            BP_CreatureSpawner cs = new BP_CreatureSpawner();
            cs.setSpawnedType(spawner.getSpawnedType());
            cs.setDelay(spawner.getDelay());
            cs.setMaxNearbyEntities(spawner.getMaxNearbyEntities());
            cs.setMaxSpawnDelay(spawner.getMaxSpawnDelay());
            cs.setMinSpawnDelay(spawner.getMinSpawnDelay());
            cs.setRequiredPlayerRange(spawner.getRequiredPlayerRange());
            cs.setSpawnRange(spawner.getSpawnRange());
            b.setCreatureSpawner(cs);
        }
        this.bpBlocks.put(pos, b);
        return true;
    }

    /**
     * @return the origin
     */
    @Nullable
    public Location getOrigin() {
        return origin;
    }
    /**
     * @return the pos1
     */
    @Nullable
    public Location getPos1() {
        return pos1;
    }
    /**
     * @return the pos2
     */
    @Nullable
    public Location getPos2() {
        return pos2;
    }

    public boolean isFull() {
        return bp != null;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(@Nullable Location origin) {
        this.origin = origin;
    }

    /**
     * @param pos1 the pos1 to set
     */
    public void setPos1(@Nullable Location pos1) {
        origin = null;
        if (pos1.getBlockY() < 0) {
            pos1.setY(0);
        }
        if (pos1.getBlockY() > 255) {
            pos1.setY(255);
        }
        this.pos1 = pos1;
    }

    /**
     * @param pos2 the pos2 to set
     */
    public void setPos2(@Nullable Location pos2) {
        origin = null;
        if (pos2.getBlockY() < 0) {
            pos2.setY(0);
        }
        if (pos2.getBlockY() > 255) {
            pos2.setY(255);
        }
        this.pos2 = pos2;
    }

    /**
     * @return the bp
     */
    public Blueprint getBp() {
        return bp;
    }

    /**
     * @param bp the bp to set
     */
    public void setBp(Blueprint bp) {
        this.bp = bp;
    }

}
