package world.bentobox.bentobox.blueprints;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Colorable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * This class pastes the clipboard it is given
 * @author tastybento
 *
 */
public class Paster {

    enum PasteState {
        BLOCKS,
        ATTACHMENTS,
        ENTITIES,
        DONE,
        CANCEL
    }

    private static final String BEDROCK = "bedrock";

    // Commonly used texts along this class.
    private static final String ATTACHED_YAML_PREFIX = "attached.";
    private static final String ENTITIES_YAML_PREFIX = "entities.";
    private static final String BLOCKS_YAML_PREFIX = "blocks.";

    private static final String INVENTORY = "inventory";
    private static final String ENTITY = "entity";
    private static final String COLOR = "color";

    private static final String LINES = "lines";
    private BentoBox plugin;
    // The minimum block position (x,y,z)
    private Location pos1;
    // The maximum block position (x,y,z)
    private Location pos2;
    // Speed of pasting
    private int pasteSpeed;
    private PasteState pasteState;
    private BukkitTask pastingTask;

    /**
     * Paste a clipboard
     * @param plugin - BentoBox
     * @param clipboard - clipboard to paste
     * @param location - location to paste to
     */
    public Paster(@NonNull BentoBox plugin, @NonNull Clipboard clipboard, @NonNull Location location) {
        this.plugin = plugin;
        paste(location.getWorld(), null, location, clipboard, null);
    }

    /**
     * Paste a clipboard
     * @param plugin - BentoBox
     * @param clipboard - clipboard to paste
     * @param location - location to paste to
     * @param task - task to run after pasting
     */
    public Paster(@NonNull BentoBox plugin, @NonNull Clipboard clipboard, @NonNull Location location, @Nullable Runnable task) {
        this.plugin = plugin;
        paste(location.getWorld(), null, location, clipboard, task);
    }

    /**
     * Pastes a clipboard
     * @param plugin - BentoBox
     * @param clipboard - clipboard to paste
     * @param world - world to paste to
     * @param island - island related to this paste
     * @param task - task to run after pasting
     */
    public Paster(@NonNull BentoBox plugin, @NonNull Clipboard clipboard, @NonNull World world, @NonNull Island island, @Nullable Runnable task) {
        this.plugin = plugin;
        // Offset due to bedrock
        Vector off = new Vector(0,0,0);
        if (clipboard.getBlockConfig().contains(BEDROCK)) {
            String[] offset = clipboard.getBlockConfig().getString(BEDROCK).split(",");
            off = new Vector(Integer.valueOf(offset[0]), Integer.valueOf(offset[1]), Integer.valueOf(offset[2]));
        }
        // Calculate location for pasting
        Location loc = island.getCenter().toVector().subtract(off).toLocation(world);
        // Paste
        paste(world, island, loc, clipboard, task);
    }

    private void paste(@NonNull World world, @Nullable Island island, @NonNull Location loc, @NonNull Clipboard clipboard, @Nullable Runnable task) {
        // Iterators for the various schem sections
        Iterator<String> it = clipboard.getBlockConfig().getConfigurationSection(BLOCKS_YAML_PREFIX).getKeys(false).iterator();
        Iterator<String> it2 = clipboard.getBlockConfig().contains(ATTACHED_YAML_PREFIX) ? clipboard.getBlockConfig().getConfigurationSection(ATTACHED_YAML_PREFIX).getKeys(false).iterator() : null;
        Iterator<String> it3 = clipboard.getBlockConfig().contains(ENTITIES_YAML_PREFIX) ? clipboard.getBlockConfig().getConfigurationSection(ENTITIES_YAML_PREFIX).getKeys(false).iterator() : null;

        // Initial state & speed
        pasteState = PasteState.BLOCKS;
        pasteSpeed = plugin.getSettings().getPasteSpeed();

        pastingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int count = 0;
            while (pasteState.equals(PasteState.BLOCKS) && count < pasteSpeed && it.hasNext()) {
                pasteBlock(world, island, loc, clipboard.getBlockConfig().getConfigurationSection(BLOCKS_YAML_PREFIX + it.next()));
                count++;
            }
            while (it2 != null && pasteState.equals(PasteState.ATTACHMENTS) && count < pasteSpeed && it2.hasNext()) {
                pasteBlock(world, island, loc, clipboard.getBlockConfig().getConfigurationSection(ATTACHED_YAML_PREFIX + it2.next()));
                count++;
            }
            while (it3 != null && pasteState.equals(PasteState.ENTITIES) && count < pasteSpeed && it3.hasNext()) {
                pasteEntity(world, loc, clipboard.getBlockConfig().getConfigurationSection(ENTITIES_YAML_PREFIX + it3.next()));
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
                if (island == null && (clipboard.getPos1() == null || clipboard.getPos2() == null)) {
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

    private void pasteBlock(World world, Island island, Location location, ConfigurationSection config) {
        String[] pos = config.getName().split(",");
        int x = location.getBlockX() + Integer.valueOf(pos[0]);
        int y = location.getBlockY() + Integer.valueOf(pos[1]);
        int z = location.getBlockZ() + Integer.valueOf(pos[2]);

        Block block = world.getBlockAt(x, y, z);
        String blockData = config.getString("bd");
        if (blockData != null) {
            setBlock(island, block, config, blockData);
        }
        // Entities (legacy)
        if (config.isConfigurationSection(ENTITY)) {
            setEntity(block.getLocation(), config.getConfigurationSection(ENTITY));
        }
        // pos1 and pos2 update
        updatePos(world, x,y,z);
    }

    private void pasteEntity(World world, Location location, ConfigurationSection config) {
        String[] pos = config.getName().split(",");
        int x = location.getBlockX() + Integer.valueOf(pos[0]);
        int y = location.getBlockY() + Integer.valueOf(pos[1]);
        int z = location.getBlockZ() + Integer.valueOf(pos[2]);
        setEntity(new Location(world, x, y, z), config);
    }

    private void setBlock(Island island, Block block, ConfigurationSection config, String blockData) {
        // Set the block data
        block.setBlockData(Bukkit.createBlockData(blockData));
        // Set the block state for chests, signs and mob spawners
        setBlockState(island, block, config);
    }

    /**
     * Handles signs, chests and mob spawner blocks
     * @param island - island
     * @param block - block
     * @param config - config
     */
    private void setBlockState(Island island, Block block, ConfigurationSection config) {
        // Get the block state
        BlockState bs = block.getState();
        // Signs
        if (bs instanceof Sign) {
            List<String> lines = config.getStringList(LINES);
            writeSign(island, block, lines);
        }
        // Chests, in general
        if (bs instanceof InventoryHolder) {
            bs.update(true, false);
            Inventory ih = ((InventoryHolder)bs).getInventory();
            if (config.isConfigurationSection(INVENTORY)) {
                ConfigurationSection inv = config.getConfigurationSection(INVENTORY);
                // Double chests are pasted as two blocks so inventory is filled twice. This code stops over filling for the first block.
                inv.getKeys(false).stream()
                .filter(i -> Integer.valueOf(i) < ih.getSize())
                .forEach(i -> ih.setItem(Integer.valueOf(i), (ItemStack)inv.get(i)));
            }
        }
        // Mob spawners
        if (bs instanceof CreatureSpawner) {
            CreatureSpawner spawner = ((CreatureSpawner) bs);
            spawner.setSpawnedType(EntityType.valueOf(config.getString("spawnedType", "PIG")));
            spawner.setMaxNearbyEntities(config.getInt("maxNearbyEntities", 16));
            spawner.setMaxSpawnDelay(config.getInt("maxSpawnDelay", 2*60*20));
            spawner.setMinSpawnDelay(config.getInt("minSpawnDelay", 5*20));

            spawner.setDelay(config.getInt("delay", -1));
            spawner.setRequiredPlayerRange(config.getInt("requiredPlayerRange", 16));
            spawner.setSpawnRange(config.getInt("spawnRange", 4));
            bs.update(true, false);
        }
    }

    /**
     * Sets any entity that is in this location
     * @param location - location
     * @param en - config section
     */
    private void setEntity(Location location, ConfigurationSection en) {
        en.getKeys(false).forEach(k -> {
            ConfigurationSection ent = en.getConfigurationSection(k);
            // Center, and just a bit high
            Location center = location.add(new Vector(0.5, 0.5, 0.5));
            LivingEntity e = (LivingEntity)location.getWorld().spawnEntity(center, EntityType.valueOf(ent.getString("type", "PIG")));
            if (e != null) {
                e.setCustomName(ent.getString("name"));
            }
            if (e instanceof Colorable && ent.contains(COLOR)) {
                ((Colorable) e).setColor(DyeColor.valueOf(ent.getString(COLOR)));
            }
            if (e instanceof Tameable) {
                ((Tameable)e).setTamed(ent.getBoolean("tamed"));
            }
            if (e instanceof ChestedHorse) {
                ((ChestedHorse)e).setCarryingChest(ent.getBoolean("chest"));
            }
            if (e instanceof Ageable) {
                if (ent.getBoolean("adult")) {
                    ((Ageable)e).setAdult();
                } else {
                    ((Ageable)e).setBaby();
                }
            }
            if (e instanceof AbstractHorse) {
                AbstractHorse horse = (AbstractHorse)e;
                horse.setDomestication(ent.getInt("domestication"));
                if (ent.isConfigurationSection(INVENTORY)) {
                    ConfigurationSection inv = ent.getConfigurationSection(INVENTORY);
                    inv.getKeys(false).forEach(i -> horse.getInventory().setItem(Integer.valueOf(i), (ItemStack)inv.get(i)));
                }
            }
            if (e instanceof Horse) {
                ((Horse)e).setStyle(Horse.Style.valueOf(ent.getString("style", "NONE")));
            }
        });

    }

    /**
     * Tracks the minimum and maximum block positions
     * @param world - world
     * @param x - x
     * @param y - y
     * @param z - z
     */
    private void updatePos(World world, int x, int y, int z) {
        if (pos1 == null) {
            pos1 = new Location(world, x, y, z);
        }
        if (pos2 == null) {
            pos2 = new Location(world, x, y, z);
        }
        if (x < pos1.getBlockX()) {
            pos1.setX(x);
        }
        if (x > pos2.getBlockX()) {
            pos2.setX(x);
        }
        if (y < pos1.getBlockY()) {
            pos1.setY(y);
        }
        if (y > pos2.getBlockY()) {
            pos2.setY(y);
        }
        if (z < pos1.getBlockZ()) {
            pos1.setZ(z);
        }
        if (z > pos2.getBlockZ()) {
            pos2.setZ(z);
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
