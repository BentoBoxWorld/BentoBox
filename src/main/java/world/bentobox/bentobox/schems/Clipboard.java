package world.bentobox.bentobox.schems;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Colorable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class Clipboard {

    enum PasteState {
        BLOCKS,
        ATTACHMENTS,
        ENTITIES,
        DONE
    }

    // Speed of pasting
    private int pasteSpeed;
    private PasteState pasteState;

    // Commonly used texts along this class.
    private static final String ATTACHED_YAML_PREFIX = "attached.";
    private static final String ENTITIES_YAML_PREFIX = "entities.";
    private static final String BLOCKS_YAML_PREFIX = "blocks.";
    private static final String BEDROCK = "bedrock";
    private static final String INVENTORY = "inventory";
    private static final String ENTITY = "entity";
    private static final String COLOR = "color";
    private static final String LOAD_ERROR = "Could not load schems file - does not exist : ";
    private static final String LINES = "lines";

    private YamlConfiguration blockConfig = new YamlConfiguration();
    private Location pos1;
    private Location pos2;
    private Location origin;
    private BentoBox plugin;
    private boolean copied;

    private File schemFolder;

    private BukkitTask pastingTask;

    public Clipboard(BentoBox plugin, File schemFolder) {
        super();
        this.plugin = plugin;
        if (!schemFolder.exists()) {
            schemFolder.mkdirs();
        }
        this.schemFolder = schemFolder;
        pasteSpeed = plugin.getSettings().getPasteSpeed();
        pasteState = PasteState.BLOCKS;
    }

    /**
     * @return the pos1
     */
    public Location getPos1() {
        return pos1;
    }
    /**
     * @param pos1 the pos1 to set
     */
    public void setPos1(Location pos1) {
        origin = null;
        this.pos1 = pos1;
    }
    /**
     * @return the pos2
     */
    public Location getPos2() {
        return pos2;
    }
    /**
     * @param pos2 the pos2 to set
     */
    public void setPos2(Location pos2) {
        origin = null;
        this.pos2 = pos2;
    }

    /**
     * @return the origin
     */
    public Location getOrigin() {
        return origin;
    }
    /**
     * @param origin the origin to set
     */
    public void setOrigin(Location origin) {
        this.origin = origin;
    }
    /**
     * Copy the blocks between pos1 and pos2 to the clipboard
     * @param user - user
     * @return true if successful, false if pos1 or pos2 are undefined
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
        int minX = Math.max(pos1.getBlockX(),pos2.getBlockX());
        int maxX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.max(pos1.getBlockY(),pos2.getBlockY());
        int maxY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.max(pos1.getBlockZ(),pos2.getBlockZ());
        int maxZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        for (int x = Math.min(pos1.getBlockX(), pos2.getBlockX()); x <= Math.max(pos1.getBlockX(),pos2.getBlockX()); x++) {
            for (int y = Math.min(pos1.getBlockY(), pos2.getBlockY()); y <= Math.max(pos1.getBlockY(),pos2.getBlockY()); y++) {
                for (int z = Math.min(pos1.getBlockZ(), pos2.getBlockZ()); z <= Math.max(pos1.getBlockZ(),pos2.getBlockZ()); z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (copyBlock(block, origin == null ? user.getLocation() : origin, copyAir, world.getLivingEntities().stream()
                            .filter(Objects::nonNull)
                            .filter(e -> !(e instanceof Player) && e.getLocation().getBlock().equals(block))
                            .collect(Collectors.toList()))) {
                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                        count ++;
                    }
                }
            }
        }
        blockConfig.set("size.xsize", maxX - minX + 1);
        blockConfig.set("size.ysize", maxY - minY + 1);
        blockConfig.set("size.zsize", maxZ - minZ + 1);
        user.sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, String.valueOf(count));
        copied = true;
        return true;
    }

    /**
     * Pastes the clipboard to island location
     * @param world - world in which to paste
     * @param island - location to paste
     * @param task - task to run after pasting
     */
    public void pasteIsland(World world, Island island, Runnable task) {
        // Offset due to bedrock
        Vector off = new Vector(0,0,0);
        if (blockConfig.contains(BEDROCK)) {
            String[] offset = blockConfig.getString(BEDROCK).split(",");
            off = new Vector(Integer.valueOf(offset[0]), Integer.valueOf(offset[1]), Integer.valueOf(offset[2]));
        }
        // Calculate location for pasting
        Location loc = island.getCenter().toVector().subtract(off).toLocation(world);
        // Paste
        paste(world, island, loc, task);
    }

    private void paste(World world, Island island, Location loc, Runnable task) {
        if (!blockConfig.contains(BLOCKS_YAML_PREFIX)) {
            plugin.logError("Clipboard has no block data in it to paste!");
            return;
        }
        // Iterators for the various schem sections
        Iterator<String> it = blockConfig.getConfigurationSection(BLOCKS_YAML_PREFIX).getKeys(false).iterator();
        Iterator<String> it2 = blockConfig.contains(ATTACHED_YAML_PREFIX) ? blockConfig.getConfigurationSection(ATTACHED_YAML_PREFIX).getKeys(false).iterator() : null;
        Iterator<String> it3 = blockConfig.contains(ENTITIES_YAML_PREFIX) ? blockConfig.getConfigurationSection(ENTITIES_YAML_PREFIX).getKeys(false).iterator() : null;

        // Initial state & speed
        pasteState = PasteState.BLOCKS;
        pasteSpeed = plugin.getSettings().getPasteSpeed();

        pastingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int count = 0;
            while (pasteState.equals(PasteState.BLOCKS) && count < pasteSpeed && it.hasNext()) {
                pasteBlock(world, island, loc, blockConfig.getConfigurationSection(BLOCKS_YAML_PREFIX + it.next()));
                count++;
            }
            while (it2 != null && pasteState.equals(PasteState.ATTACHMENTS) && count < pasteSpeed && it2.hasNext()) {
                pasteBlock(world, island, loc, blockConfig.getConfigurationSection(ATTACHED_YAML_PREFIX + it2.next()));
                count++;
            }
            while (it3 != null && pasteState.equals(PasteState.ENTITIES) && count < pasteSpeed && it3.hasNext()) {
                pasteEntity(world, loc, blockConfig.getConfigurationSection(ENTITIES_YAML_PREFIX + it3.next()));
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
                pastingTask.cancel();
                if (task != null) {
                    // Run follow on task if it exists
                    Bukkit.getScheduler().runTaskLater(plugin, task, 2L);
                }
            }
        }, 0L, 1L);


    }

    /**
     * Paste clipboard at this location
     * @param location - location
     */
    public void pasteClipboard(Location location) {
        if (blockConfig.contains(BLOCKS_YAML_PREFIX)) {
            paste(location.getWorld(), null, location, null);
        } else {
            plugin.logError("Clipboard has no block data in it to paste!");
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
    private YamlConfiguration getBlockConfig() {
        return blockConfig;
    }

    private void unzip(final String zipFilePath) throws IOException {
        Path path = Paths.get(zipFilePath);
        if (!(path.toFile().exists())) {
            throw new IOException("No file exists!");
        }
        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipInputStream.getNextEntry();
            while (entry != null) {
                Path filePath = Paths.get(path.getParent().toString(), entry.getName());
                if (!entry.isDirectory()) {
                    unzipFiles(zipInputStream, filePath);
                } else {
                    Files.createDirectories(filePath);
                }

                zipInputStream.closeEntry();
                entry = zipInputStream.getNextEntry();
            }
        }
    }

    private static void unzipFiles(final ZipInputStream zipInputStream, final Path unzipFilePath) throws IOException {

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(unzipFilePath.toAbsolutePath().toString()))) {
            byte[] bytesIn = new byte[1024];
            int read;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }

    }

    private void zip(File targetFile) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(targetFile.getAbsolutePath() + ".schem"))) {
            zipOutputStream.putNextEntry(new ZipEntry(targetFile.getName()));
            try (FileInputStream inputStream = new FileInputStream(targetFile)) {
                final byte[] buffer = new byte[1024];
                int length;
                while((length = inputStream.read(buffer)) >= 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
                try {
                    Files.delete(targetFile.toPath());
                } catch (Exception e) {
                    plugin.logError(e.getMessage());
                }
            }
        }
    }

    public boolean isFull() {
        return copied;
    }


    /**
     * Load a file to clipboard
     * @param fileName - filename in schems folder
     * @throws IOException - if there's a load error with unziping or name
     * @throws InvalidConfigurationException - the YAML of the schem is at fault
     */
    public void load(String fileName) throws IOException, InvalidConfigurationException {
        File zipFile = new File(schemFolder, fileName + ".schem");
        if (!zipFile.exists()) {
            plugin.logError(LOAD_ERROR + zipFile.getName());
            throw new IOException(LOAD_ERROR + zipFile.getName());
        }
        unzip(zipFile.getAbsolutePath());
        File file = new File(schemFolder, fileName);
        if (!file.exists()) {
            plugin.logError(LOAD_ERROR + file.getName());
            throw new IOException(LOAD_ERROR + file.getName());
        }
        blockConfig = new YamlConfiguration();
        blockConfig.load(file);
        copied = true;
        Files.delete(file.toPath());
    }

    /*
      Load a file to clipboard
     */
    /**
     * @param user - use trying to load
     * @param fileName - filename
     * @return - ture if load is successful, false if not
     */
    public boolean load(User user, String fileName) {
        try {
            load(fileName);
        } catch (IOException e1) {
            user.sendMessage("commands.admin.schem.could-not-load");
            plugin.logError("Could not load schems file: " + fileName + " " + e1.getMessage());
            return false;
        } catch (InvalidConfigurationException e1) {
            user.sendMessage("commands.admin.schem.could-not-load");
            plugin.logError("Could not load schems file - YAML error : " + fileName + " " + e1.getMessage());
            return false;
        }
        user.sendMessage("general.success");
        return true;
    }

    /**
     * Save the clipboard to a file
     * @param user - user who is copying
     * @param newFile - filename
     * @return - true if successful, false if error
     */
    public boolean save(User user, String newFile) {
        File file = new File(schemFolder, newFile);
        try {
            getBlockConfig().save(file);
        } catch (IOException e) {
            user.sendMessage("commands.admin.schem.could-not-save", "[message]", "Could not save temp schems file.");
            plugin.logError("Could not save temporary schems file: " + file.getName());
            return false;
        }
        try {
            zip(file);
        } catch (IOException e) {
            user.sendMessage("commands.admin.schem.could-not-save", "[message]", "Could not zip temp schems file.");
            plugin.logError("Could not zip temporary schems file: " + file.getName());
            return false;
        }
        user.sendMessage("general.success");
        return true;
    }


}
