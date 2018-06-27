package us.tastybento.bskyblock.island.builders;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.material.Button;
import org.bukkit.material.Colorable;
import org.bukkit.material.Directional;
import org.bukkit.material.Lever;
import org.bukkit.material.MaterialData;
import org.bukkit.material.Openable;
import org.bukkit.material.Redstone;
import org.bukkit.material.Stairs;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.localization.TextVariables;
import us.tastybento.bskyblock.api.user.User;

public class Clipboard {

    private enum TorchDir {
        UNUSED,
        EAST,
        WEST,
        SOUTH,
        NORTH,
        UP
    }

    private static final String ATTACHED = "attached";

    private static final String BLOCK = "blocks";

    private static final String FACING = "facing";

    private static final String POWERED = "powered";

    private static final String LOAD_ERROR = "Could not load schems file - does not exist : ";

    private YamlConfiguration blockConfig = new YamlConfiguration();
    private Location pos1;
    private Location pos2;
    private Location origin;
    private BSkyBlock plugin;
    private boolean copied;

    private File schemFolder;

    public Clipboard(BSkyBlock plugin) {
        super();
        this.plugin = plugin;
        schemFolder = new File(plugin.getDataFolder(), "schems");
        if (!schemFolder.exists()) {
            schemFolder.mkdirs();
        }
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
                    Block block = pos1.getWorld().getBlockAt(x, y, z);
                    if (copyBlock(block, origin == null ? user.getLocation() : origin, copyAir)) {
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
     * Pastes the clipboard to location
     * @param location - location to paste
     */
    public void paste(Location location) {
        blockConfig.getConfigurationSection(BLOCK).getKeys(false).forEach(b -> pasteBlock(location, blockConfig.getConfigurationSection(BLOCK + "." + b)));
    }

    private void pasteBlock(Location location, ConfigurationSection s) {
        String[] pos = s.getName().split(",");
        int x = location.getBlockX() + Integer.valueOf(pos[0]);
        int y = location.getBlockY() + Integer.valueOf(pos[1]);
        int z = location.getBlockZ() + Integer.valueOf(pos[2]);
        Material m = Material.getMaterial(s.getString("type"));
        Block block = location.getWorld().getBlockAt(x, y, z);
        if (s.getBoolean(ATTACHED)) {
            plugin.log("Setting 1 tick later for " + m.toString());
            plugin.getServer().getScheduler().runTask(plugin, () -> setBlock(block, s, m));
        } else {
            setBlock(block, s, m);
        }
    }

    @SuppressWarnings("deprecation")
    private void setBlock(Block block, ConfigurationSection s, Material m) {
        // Block state

        if (s.getBoolean(ATTACHED) && m.toString().contains("TORCH")) {
            TorchDir d = TorchDir.valueOf(s.getString(FACING));

            Block rel = block.getRelative(BlockFace.DOWN);
            Material rm = rel.getType();
            Byte data = rel.getData();

            if (rel.isEmpty() || rel.isLiquid()) {
                rel.setType(Material.STONE);
                block.setType(m);
                block.setData((byte)d.ordinal());
                // Set the block back to what it was
                rel.setType(rm);
                rel.setData(data);
            } else {
                block.setType(m);
                block.setData((byte)d.ordinal());
            }
            return;
        }

        block.setType(m, false);

        BlockState bs = block.getState();

        byte data = (byte)s.getInt("data");
        block.setData(data);


        // Material Data
        MaterialData md = bs.getData();
        if (md instanceof Openable) {
            Openable open = (Openable)md;
            open.setOpen(s.getBoolean("open"));
        }

        if (md instanceof Directional) {
            Directional facing = (Directional)md;
            if (md instanceof Stairs) {
                facing.setFacingDirection(BlockFace.valueOf(s.getString(FACING)).getOppositeFace());
            } else {
                facing.setFacingDirection(BlockFace.valueOf(s.getString(FACING)));
            }
        }

        if (md instanceof Lever) {
            Lever r = (Lever)md;
            r.setPowered(s.getBoolean(POWERED));
        }
        if (md instanceof Button) {
            Button r = (Button)md;
            r.setPowered(s.getBoolean(POWERED));
        }

        // Block data
        if (bs instanceof Sign) {
            Sign sign = (Sign)bs;
            List<String> lines = s.getStringList("lines");
            for (int i =0 ; i < lines.size(); i++) {
                sign.setLine(i, lines.get(i));
            }
            sign.update();
        }
        if (bs instanceof Banner) {
            Banner banner = (Banner)bs;
            DyeColor baseColor = DyeColor.valueOf(s.getString("baseColor"));
            banner.setBaseColor(baseColor);
            int i = 0;
            ConfigurationSection pat = s.getConfigurationSection("pattern");
            if (pat != null) {
                for (String pattern : pat.getKeys(false)) {
                    banner.setPattern(i, new Pattern(DyeColor.valueOf(pat.getString(pattern))
                            , PatternType.valueOf(pattern)));
                    i++;
                }
            }
        }
        if (bs instanceof CreatureSpawner) {
            CreatureSpawner spawner = ((CreatureSpawner) bs);
            spawner.setSpawnedType(EntityType.valueOf(s.getString("spawnedType", "PIG")));
            spawner.setMaxNearbyEntities(s.getInt("maxNearbyEntities", 16));
            spawner.setMaxSpawnDelay(s.getInt("maxSpawnDelay", 2*60*20));
            spawner.setMinSpawnDelay(s.getInt("minSpawnDelay", 5*20));

            spawner.setDelay(s.getInt("delay", -1));
            spawner.setRequiredPlayerRange(s.getInt("requiredPlayerRange", 16));
            spawner.setSpawnRange(s.getInt("spawnRange", 4));

        }


        bs.update(true, false);

        if (bs instanceof InventoryHolder) {
            Inventory ih = ((InventoryHolder)bs).getInventory();
            ConfigurationSection inv = s.getConfigurationSection("inventory");
            inv.getKeys(false).forEach(i -> ih.setItem(Integer.valueOf(i), (ItemStack)inv.get(i)));
        }


    }

    @SuppressWarnings("deprecation")
    private boolean copyBlock(Block block, Location copyOrigin, boolean copyAir) {
        if (!copyAir && block.getType().equals(Material.AIR)) {
            return false;
        }
        // Create position
        int x = block.getLocation().getBlockX() - copyOrigin.getBlockX();
        int y = block.getLocation().getBlockY() - copyOrigin.getBlockY();
        int z = block.getLocation().getBlockZ() - copyOrigin.getBlockZ();
        String pos = x + "," + y + "," + z;

        // Position defines the section
        ConfigurationSection s = blockConfig.createSection(BLOCK + "." + pos);
        // Set the block type
        s.set("type", block.getType().toString());
        if (block.getData() != 0) {
            s.set("data", block.getData());
        }

        // Block state
        BlockState bs = block.getState();

        // Material Data
        MaterialData md = bs.getData();
        if (md instanceof Openable) {
            Openable open = (Openable)md;
            s.set("open", open.isOpen());
        }
        if (md instanceof Directional) {
            Directional facing = (Directional)md;
            s.set(FACING, facing.getFacing().name());
        }
        if (md instanceof Attachable) {
            Attachable facing = (Attachable)md;
            s.set(FACING, facing.getFacing().name());
            s.set("attached-face", facing.getAttachedFace().name());
            s.set(ATTACHED, true);
        }
        if (md instanceof Colorable) {
            Colorable c = (Colorable)md;
            s.set("color", c.getColor().name());
        }
        if (block.getType().equals(Material.CARPET)) {
            DyeColor c = DyeColor.getByWoolData(block.getData());
            s.set("color", c.name());
        }
        if (md instanceof Redstone) {
            Redstone r = (Redstone)md;
            blockConfig.set(POWERED, r.isPowered());
        }

        // Block data
        if (bs instanceof Sign) {
            Sign sign = (Sign)bs;
            s.set("lines", Arrays.asList(sign.getLines()));
        }
        if (bs instanceof Banner) {
            Banner banner = (Banner)bs;
            s.set("baseColor", banner.getBaseColor().toString());
            banner.getPatterns().forEach(p -> s.set("pattern." + p.getPattern().toString(), p.getColor().toString()));
        }
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
     * @param string - filename
     * @return - true if successful, false if error
     */
    public boolean save(User user, String string) {
        File file = new File(schemFolder, string);
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
