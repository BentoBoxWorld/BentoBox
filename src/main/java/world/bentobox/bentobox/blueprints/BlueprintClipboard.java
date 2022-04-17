package world.bentobox.bentobox.blueprints;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
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
import org.bukkit.entity.Villager;
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
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * The clipboard provides the holding spot for an active blueprint that is being
 * manipulated by a user. It supports copying from the world and setting of coordinates
 * such as the bounding box around the cuboid copy area.
 * Pasting is done by the {@link BlueprintPaster} class.
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintClipboard {

    private @Nullable Blueprint blueprint;
    private @Nullable Location pos1;
    private @Nullable Location pos2;
    private @Nullable Vector origin;
    private BukkitTask copyTask;
    private int count;
    private boolean copying;
    private int index;
    private int lastPercentage;
    private final Map<Vector, List<BlueprintEntity>> bpEntities = new LinkedHashMap<>();
    private final Map<Vector, BlueprintBlock> bpAttachable = new LinkedHashMap<>();
    private final Map<Vector, BlueprintBlock> bpBlocks = new LinkedHashMap<>();
    private final BentoBox plugin = BentoBox.getInstance();

    /**
     * Create a clipboard for blueprint
     * @param blueprint - the blueprint to load into the clipboard
     */
    public BlueprintClipboard(@NonNull Blueprint blueprint) {
        this.blueprint = blueprint;
    }

    public BlueprintClipboard() { }

    /**
     * Copy the blocks between pos1 and pos2 into the clipboard for a user.
     * This will erase any previously registered data from the clipboard.
     * Copying is done async.
     * @param user - user
     * @return true if successful, false if pos1 or pos2 are undefined.
     */
    public boolean copy(User user, boolean copyAir) {
        if (copying) {
            user.sendMessage("commands.admin.blueprint.mid-copy");
            return false;
        }
        if (pos1 == null || pos2 == null) {
            user.sendMessage("commands.admin.blueprint.need-pos1-pos2");
            return false;
        }
        if (origin == null) {
            setOrigin(user.getLocation().toVector());
        }
        user.sendMessage("commands.admin.blueprint.copying");

        // World
        World world = pos1.getWorld();
        if (world == null) {
            return false;
        }
        // Clear the clipboard
        blueprint = new Blueprint();
        bpEntities.clear();
        bpAttachable.clear();
        bpBlocks.clear();

        count = 0;
        index = 0;
        lastPercentage = 0;
        BoundingBox toCopy = BoundingBox.of(pos1, pos2);
        blueprint.setxSize((int)toCopy.getWidthX());
        blueprint.setySize((int)toCopy.getHeight());
        blueprint.setzSize((int)toCopy.getWidthZ());

        int speed = plugin.getSettings().getPasteSpeed();
        List<Vector> vectorsToCopy = getVectors(toCopy);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> copyAsync(world, user, vectorsToCopy, speed, copyAir));
        return true;
    }

    private void copyAsync(World world, User user, List<Vector> vectorsToCopy, int speed, boolean copyAir) {
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
                        .filter(e -> new Vector(Math.rint(e.getLocation().getX()),
                                Math.rint(e.getLocation().getY()),
                                Math.rint(e.getLocation().getZ())).equals(v))
                        .collect(Collectors.toList());
                if (copyBlock(v.toLocation(world), copyAir, ents)) {
                    count++;
                }
            });
            index += speed;
            int percent = (int)(index * 100 / (double)vectorsToCopy.size());
            if (percent != lastPercentage && percent % 10 == 0) {
                user.sendMessage("commands.admin.blueprint.copied-percent", TextVariables.NUMBER, String.valueOf(percent));
                lastPercentage = percent;
            }
            if (index > vectorsToCopy.size()) {
                copyTask.cancel();
                assert blueprint != null;
                blueprint.setAttached(bpAttachable);
                blueprint.setBlocks(bpBlocks);
                blueprint.setEntities(bpEntities);
                user.sendMessage("general.success");
                user.sendMessage("commands.admin.blueprint.copied-blocks", TextVariables.NUMBER, String.valueOf(count));
            }
            copying = false;
        }, 0L, 1L);
    }

    /**
     * Get all the x,y,z coords that must be copied
     * @param b - bounding box
     * @return - list of vectors
     */
    private List<Vector> getVectors(BoundingBox b) {
        List<Vector> r = new ArrayList<>();
        for (int y = (int)b.getMinY(); y <= b.getMaxY(); y++) {
            for (int x = (int)b.getMinX(); x <= b.getMaxX(); x++) {
                for (int z = (int)b.getMinZ(); z <= b.getMaxZ(); z++) {
                    r.add(new Vector(x,y,z));
                }
            }
        }
        return r;
    }

    private boolean copyBlock(Location l, boolean copyAir, Collection<LivingEntity> entities) {
        Block block = l.getBlock();
        if (!copyAir && block.getType().equals(Material.AIR) && entities.isEmpty()) {
            return false;
        }
        // Create position
        Vector origin2 = origin == null ? new Vector(0,0,0) : origin;
        int x = l.getBlockX() - origin2.getBlockX();
        int y = l.getBlockY() - origin2.getBlockY();
        int z = l.getBlockZ() - origin2.getBlockZ();
        Vector pos = new Vector(x, y, z);

        // Set entities
        List<BlueprintEntity> bpEnts = setEntities(entities);
        // Store
        if (!bpEnts.isEmpty()) {
            bpEntities.put(pos, bpEnts);
        }

        // Return if this is just air block
        if (!copyAir && block.getType().equals(Material.AIR) && !entities.isEmpty()) {
            return true;
        }


        BlueprintBlock b = bluePrintBlock(pos, block);
        if (b != null) {
            this.bpBlocks.put(pos, b);
        }
        return true;
    }

    private BlueprintBlock bluePrintBlock(Vector pos, Block block) {
        // Block state
        BlockState blockState = block.getState();
        BlueprintBlock b = new BlueprintBlock(block.getBlockData().getAsString());
        // Biome
        b.setBiome(block.getBiome());
        // Signs
        if (blockState instanceof Sign sign) {
            b.setSignLines(Arrays.asList(sign.getLines()));
            b.setGlowingText(sign.isGlowingText());
        }
        // Set block data
        if (blockState.getData() instanceof Attachable) {
            // Placeholder for attachment
            bpBlocks.put(pos, new BlueprintBlock("minecraft:air"));
            bpAttachable.put(pos, b);
            return null;
        }

        if (block.getType().equals(Material.BEDROCK)) {
            // Find highest bedrock
            if(blueprint.getBedrock() == null) {
                blueprint.setBedrock(pos);
            } else {
                if (pos.getBlockY() > blueprint.getBedrock().getBlockY()) {
                    blueprint.setBedrock(pos);
                }
            }
        }

        // Chests
        if (blockState instanceof InventoryHolder ih) {
            b.setInventory(new HashMap<>());
            for (int i = 0; i < ih.getInventory().getSize(); i++) {
                ItemStack item = ih.getInventory().getItem(i);
                if (item != null) {
                    b.getInventory().put(i, item);
                }
            }
        }

        if (blockState instanceof CreatureSpawner spawner) {
            b.setCreatureSpawner(getSpawner(spawner));
        }

        // Banners
        if (blockState instanceof Banner banner) {
            b.setBannerPatterns(banner.getPatterns());
        }

        return b;
    }

    private BlueprintCreatureSpawner getSpawner(CreatureSpawner spawner) {
        BlueprintCreatureSpawner cs = new BlueprintCreatureSpawner();
        cs.setSpawnedType(spawner.getSpawnedType());
        cs.setDelay(spawner.getDelay());
        cs.setMaxNearbyEntities(spawner.getMaxNearbyEntities());
        cs.setMaxSpawnDelay(spawner.getMaxSpawnDelay());
        cs.setMinSpawnDelay(spawner.getMinSpawnDelay());
        cs.setRequiredPlayerRange(spawner.getRequiredPlayerRange());
        cs.setSpawnRange(spawner.getSpawnRange());
        return cs;
    }

    private List<BlueprintEntity> setEntities(Collection<LivingEntity> entities) {
        List<BlueprintEntity> bpEnts = new ArrayList<>();
        for (LivingEntity entity: entities) {
            BlueprintEntity bpe = new BlueprintEntity();
            bpe.setType(entity.getType());
            bpe.setCustomName(entity.getCustomName());
            if (entity instanceof Villager villager) {
                setVillager(villager, bpe);
            }
            if (entity instanceof Colorable c) {
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
            // Only set if child. Most animals are adults
            if (entity instanceof Ageable && !((Ageable)entity).isAdult()) {
                bpe.setAdult(false);
            }
            if (entity instanceof AbstractHorse horse) {
                bpe.setDomestication(horse.getDomestication());
                bpe.setInventory(new HashMap<>());
                for (int i = 0; i < horse.getInventory().getSize(); i++) {
                    ItemStack item = horse.getInventory().getItem(i);
                    if (item != null) {
                        bpe.getInventory().put(i, item);
                    }
                }
            }

            if (entity instanceof Horse horse) {
                bpe.setStyle(horse.getStyle());
            }
            bpEnts.add(bpe);
        }
        return bpEnts;
    }

    /**
     * Set the villager stats
     * @param v - villager
     * @param bpe - Blueprint Entity
     */
    private void setVillager(Villager v, BlueprintEntity bpe) {
        bpe.setExperience(v.getVillagerExperience());
        bpe.setLevel(v.getVillagerLevel());
        bpe.setProfession(v.getProfession());
        bpe.setVillagerType(v.getVillagerType());
    }

    /**
     * @return the origin
     */
    @Nullable
    public Vector getOrigin() {
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
        return blueprint != null;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(@Nullable Vector origin) {
        this.origin = origin;
    }

    /**
     * @param pos1 the pos1 to set
     */
    public void setPos1(@Nullable Location pos1) {
        origin = null;
        if (pos1 != null) {
            final int minHeight = pos1.getWorld() == null ? 0 : pos1.getWorld().getMinHeight();
            final int maxHeight = pos1.getWorld() == null ? 255 : pos1.getWorld().getMaxHeight();
            
            if (pos1.getBlockY() < minHeight)
            {
                pos1.setY(minHeight);
            }

            if (pos1.getBlockY() > maxHeight)
            {
                pos1.setY(maxHeight);
            }
        }
        this.pos1 = pos1;
    }

    /**
     * @param pos2 the pos2 to set
     */
    public void setPos2(@Nullable Location pos2) {
        origin = null;
        if (pos2 != null) {
            final int minHeight = pos2.getWorld() == null ? 0 : pos2.getWorld().getMinHeight();
            final int maxHeight = pos2.getWorld() == null ? 255 : pos2.getWorld().getMaxHeight();

            if (pos2.getBlockY() < minHeight)
            {
                pos2.setY(minHeight);
            }

            if (pos2.getBlockY() > maxHeight)
            {
                pos2.setY(maxHeight);
            }
        }
        this.pos2 = pos2;
    }

    /**
     * @return the blueprint
     */
    public @Nullable Blueprint getBlueprint() {
        return blueprint;
    }

    /**
     * @param blueprint the blueprint to set
     */
    public BlueprintClipboard setBlueprint(Blueprint blueprint) {
        this.blueprint = blueprint;
        return this;
    }
}
