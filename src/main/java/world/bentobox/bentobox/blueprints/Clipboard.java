package world.bentobox.bentobox.blueprints;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

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

import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;

/**
 * @author tastybento
 */
public class Clipboard {

    // Commonly used texts along this class.
    private static final String ATTACHED_YAML_PREFIX = "attached.";
    private static final String ENTITIES_YAML_PREFIX = "entities.";
    private static final String BLOCKS_YAML_PREFIX = "blocks.";
    private static final String BEDROCK = "bedrock";
    private static final String COLOR = "color";
    private static final String LINES = "lines";

    private YamlConfiguration blockConfig;
    private Location pos1;
    private Location pos2;
    private Location origin;

    public Clipboard(String contents) throws InvalidConfigurationException {
        super();
        set(contents);
    }

    public Clipboard(YamlConfiguration config) {
        super();
        blockConfig = config;
    }

    public Clipboard() {
        super();
    }

    /**
     * Copy the blocks between pos1 and pos2 to the clipboard
     * @param user - user
     * @return true if successful, false if pos1 or pos2 are undefined or something is already in the clipboard
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
        ConfigurationSection s = blockConfig.createSection(BLOCKS_YAML_PREFIX + "." + pos);

        // Set entities
        for (LivingEntity e: entities) {
            ConfigurationSection en = blockConfig.createSection(ENTITIES_YAML_PREFIX + pos + "." + e.getUniqueId());
            en.set("type", e.getType().name());
            en.set("name", e.getCustomName());
            if (e instanceof Colorable) {
                Colorable c = (Colorable)e;
                if (c.getColor() != null) {
                    en.set(COLOR, c.getColor().name());
                }
            }
            if (e instanceof Tameable && ((Tameable)e).isTamed()) {
                en.set("tamed", true);
            }
            if (e instanceof ChestedHorse && ((ChestedHorse)e).isCarryingChest()) {
                en.set("chest", true);
            }
            if (e instanceof Ageable) {
                en.set("adult", ((Ageable)e).isAdult());
            }
            if (e instanceof AbstractHorse) {
                AbstractHorse horse = (AbstractHorse)e;
                en.set("domestication", horse.getDomestication());
                for (int index = 0; index < horse.getInventory().getSize(); index++) {
                    ItemStack i = horse.getInventory().getItem(index);
                    if (i != null) {
                        en.set("inventory." + index, i);
                    }
                }
            }

            if (e instanceof Horse) {
                Horse horse = (Horse)e;
                en.set("style", horse.getStyle().name());
            }
        }

        // Return if this is just air block
        if (!copyAir && block.getType().equals(Material.AIR) && !entities.isEmpty()) {
            return true;
        }

        // Block state
        BlockState bs = block.getState();

        // Set block data
        if (bs.getData() instanceof Attachable) {
            ConfigurationSection a = blockConfig.createSection(ATTACHED_YAML_PREFIX + pos);
            a.set("bd", block.getBlockData().getAsString());
            // Placeholder for attachment
            s.set("bd", "minecraft:air");
            // Signs
            if (bs instanceof Sign) {
                Sign sign = (Sign)bs;
                a.set(LINES, Arrays.asList(sign.getLines()));
            }
            return true;
        } else {
            s.set("bd", block.getBlockData().getAsString());
            // Signs
            if (bs instanceof Sign) {
                Sign sign = (Sign)bs;
                s.set(LINES, Arrays.asList(sign.getLines()));
            }
        }

        if (block.getType().equals(Material.BEDROCK)) {
            blockConfig.set(BEDROCK, x + "," + y + "," + z);
        }

        // Chests
        if (bs instanceof InventoryHolder) {
            InventoryHolder ih = (InventoryHolder)bs;
            for (int index = 0; index < ih.getInventory().getSize(); index++) {
                ItemStack i = ih.getInventory().getItem(index);
                if (i != null) {
                    s.set("inventory." + index, i);
                }
            }
        }

        if (bs instanceof CreatureSpawner) {
            CreatureSpawner spawner = (CreatureSpawner)bs;
            s.set("spawnedType",spawner.getSpawnedType().name());
            s.set("delay", spawner.getDelay());
            s.set("maxNearbyEntities", spawner.getMaxNearbyEntities());
            s.set("maxSpawnDelay", spawner.getMaxSpawnDelay());
            s.set("minSpawnDelay", spawner.getMinSpawnDelay());
            s.set("requiredPlayerRange", spawner.getRequiredPlayerRange());
            s.set("spawnRange", spawner.getSpawnRange());
        }
        return true;
    }

    /**
     * @return the blockConfig
     */
    public YamlConfiguration getBlockConfig() {
        return blockConfig;
    }

    /**
     * @return the origin
     */
    public Location getOrigin() {
        return origin;
    }
    /**
     * @return the pos1
     */
    public Location getPos1() {
        return pos1;
    }
    /**
     * @return the pos2
     */
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
     * @param set the blockConfig
     */
    public Clipboard set(YamlConfiguration blockConfig) {
        this.blockConfig = blockConfig;
        setPos1(null);
        setPos2(null);
        return this;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(Location origin) {
        this.origin = origin;
    }

    /**
     * @param pos1 the pos1 to set
     */
    public void setPos1(Location pos1) {
        origin = null;
        this.pos1 = pos1;
    }

    /**
     * @param pos2 the pos2 to set
     */
    public void setPos2(Location pos2) {
        origin = null;
        this.pos2 = pos2;
    }


    /**
     * Get the clipboard
     * @return the clipboard as a string
     */
    @Override
    public String toString() {
        return blockConfig.saveToString();
    }

}
