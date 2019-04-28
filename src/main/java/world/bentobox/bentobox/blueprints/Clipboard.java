package world.bentobox.bentobox.blueprints;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.util.BoundingBox;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class Clipboard {

    // Commonly used texts along this class.
    private static final String ATTACHED_YAML_PREFIX = "attached.";
    private static final String ENTITIES_YAML_PREFIX = "entities.";
    private static final String BLOCKS_YAML_PREFIX = "blocks.";
    private static final String BEDROCK = "bedrock";
    private static final String COLOR = "color";
    private static final String LINES = "lines";

    private @Nullable YamlConfiguration blockConfig;
    private @Nullable Location pos1;
    private @Nullable Location pos2;
    private @Nullable Location origin;

    public Clipboard(String contents) throws InvalidConfigurationException {
        set(contents);
    }

    public Clipboard(@NonNull YamlConfiguration config) {
        this.blockConfig = config;
    }

    public Clipboard() {
        super();
    }

    /**
     * Copy the blocks between pos1 and pos2 into the clipboard.
     * This will erase any previously registered data from the clipboard.
     * @param user - user
     * @return true if successful, false if pos1 or pos2 are undefined.
     */
    public boolean copy(User user, boolean copyAir) {
        if (pos1 == null || pos2 == null) {
            user.sendMessage("commands.admin.schem.need-pos1-pos2");
            return false;
        }
        // World
        World world = pos1.getWorld();
        // Clear the clipboard
        blockConfig = new YamlConfiguration();

        int count = 0;
        BoundingBox toCopy = BoundingBox.of(pos1, pos2);

        for (int x = (int)toCopy.getMinX(); x <= toCopy.getMaxX(); x++) {
            for (int y = (int)toCopy.getMinY(); y <= toCopy.getMaxY(); y++) {
                for (int z = (int)toCopy.getMinZ(); z <= toCopy.getMaxZ(); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (copyBlock(block, origin == null ? user.getLocation() : origin, copyAir, world.getLivingEntities().stream()
                            .filter(Objects::nonNull)
                            .filter(e -> !(e instanceof Player) && e.getLocation().getBlock().equals(block))
                            .collect(Collectors.toList()))) {
                        count ++;
                    }
                }
            }
        }
        blockConfig.set("size.xsize", toCopy.getWidthX());
        blockConfig.set("size.ysize", toCopy.getHeight());
        blockConfig.set("size.zsize", toCopy.getWidthZ());
        user.sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, String.valueOf(count));
        return true;
    }

    private boolean copyBlock(Block block, Location copyOrigin, boolean copyAir, Collection<LivingEntity> entities) {
        if (!copyAir && block.getType().equals(Material.AIR) && entities.isEmpty()) {
            return false;
        }
        // Create position
        int x = block.getLocation().getBlockX() - copyOrigin.getBlockX();
        int y = block.getLocation().getBlockY() - copyOrigin.getBlockY();
        int z = block.getLocation().getBlockZ() - copyOrigin.getBlockZ();
        String pos = x + "," + y + "," + z;

        // Position defines the section
        ConfigurationSection blocksSection = blockConfig.createSection(BLOCKS_YAML_PREFIX + "." + pos);

        // Set entities
        for (LivingEntity entity: entities) {
            ConfigurationSection entitySection = blockConfig.createSection(ENTITIES_YAML_PREFIX + pos + "." + entity.getUniqueId());
            entitySection.set("type", entity.getType().name());
            entitySection.set("name", entity.getCustomName());
            if (entity instanceof Colorable) {
                Colorable c = (Colorable)entity;
                if (c.getColor() != null) {
                    entitySection.set(COLOR, c.getColor().name());
                }
            }
            if (entity instanceof Tameable && ((Tameable)entity).isTamed()) {
                entitySection.set("tamed", true);
            }
            if (entity instanceof ChestedHorse && ((ChestedHorse)entity).isCarryingChest()) {
                entitySection.set("chest", true);
            }
            if (entity instanceof Ageable) {
                entitySection.set("adult", ((Ageable)entity).isAdult());
            }
            if (entity instanceof AbstractHorse) {
                AbstractHorse horse = (AbstractHorse)entity;
                entitySection.set("domestication", horse.getDomestication());
                for (int index = 0; index < horse.getInventory().getSize(); index++) {
                    ItemStack i = horse.getInventory().getItem(index);
                    if (i != null) {
                        entitySection.set("inventory." + index, i);
                    }
                }
            }

            if (entity instanceof Horse) {
                Horse horse = (Horse)entity;
                entitySection.set("style", horse.getStyle().name());
            }
        }

        // Return if this is just air block
        if (!copyAir && block.getType().equals(Material.AIR) && !entities.isEmpty()) {
            return true;
        }

        // Block state
        BlockState blockState = block.getState();

        // Set block data
        if (blockState.getData() instanceof Attachable) {
            ConfigurationSection attachedSection = blockConfig.createSection(ATTACHED_YAML_PREFIX + pos);
            attachedSection.set("bd", block.getBlockData().getAsString());
            // Placeholder for attachment
            blocksSection.set("bd", "minecraft:air");
            // Signs
            if (blockState instanceof Sign) {
                Sign sign = (Sign)blockState;
                attachedSection.set(LINES, Arrays.asList(sign.getLines()));
            }
            return true;
        } else {
            blocksSection.set("bd", block.getBlockData().getAsString());
            // Signs
            if (blockState instanceof Sign) {
                Sign sign = (Sign)blockState;
                blocksSection.set(LINES, Arrays.asList(sign.getLines()));
            }
        }

        if (block.getType().equals(Material.BEDROCK)) {
            blockConfig.set(BEDROCK, x + "," + y + "," + z);
        }

        // Chests
        if (blockState instanceof InventoryHolder) {
            InventoryHolder ih = (InventoryHolder)blockState;
            for (int index = 0; index < ih.getInventory().getSize(); index++) {
                ItemStack i = ih.getInventory().getItem(index);
                if (i != null) {
                    blocksSection.set("inventory." + index, i);
                }
            }
        }

        if (blockState instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner)blockState;
            blocksSection.set("spawnedType",spawner.getSpawnedType().name());
            blocksSection.set("delay", spawner.getDelay());
            blocksSection.set("maxNearbyEntities", spawner.getMaxNearbyEntities());
            blocksSection.set("maxSpawnDelay", spawner.getMaxSpawnDelay());
            blocksSection.set("minSpawnDelay", spawner.getMinSpawnDelay());
            blocksSection.set("requiredPlayerRange", spawner.getRequiredPlayerRange());
            blocksSection.set("spawnRange", spawner.getSpawnRange());
        }
        return true;
    }

    /**
     * @return the blockConfig
     */
    @Nullable
    public YamlConfiguration getBlockConfig() {
        return blockConfig;
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
        return blockConfig != null;
    }

    /**
     * Set the clipboard from a YAML string
     * @param contents
     * @return
     * @throws InvalidConfigurationException
     */
    public Clipboard set(String contents) throws InvalidConfigurationException {
        this.blockConfig.loadFromString(contents);
        setPos1(null);
        setPos2(null);
        return this;
    }

    /**
     * Set the clipboard contents from a YAML configuration
     * @param blockConfig the blockConfig
     */
    public Clipboard set(@NonNull YamlConfiguration blockConfig) {
        this.blockConfig = blockConfig;
        setPos1(null);
        setPos2(null);
        return this;
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
        this.pos1 = pos1;
    }

    /**
     * @param pos2 the pos2 to set
     */
    public void setPos2(@Nullable Location pos2) {
        origin = null;
        this.pos2 = pos2;
    }

    /**
     * Returns the clipboard as a String using {@link YamlConfiguration#saveToString()}.
     * @return the clipboard as a String.
     */
    @Override
    public String toString() {
        return blockConfig.saveToString();
    }
}
