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

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
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

import us.tastybento.bskyblock.BSkyBlock;
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

    private YamlConfiguration blockConfig = new YamlConfiguration();
    private Location pos1;
    private Location pos2;
    private BSkyBlock plugin;
    private boolean copied;

    public Clipboard(BSkyBlock plugin) {
        super();
        this.plugin = plugin;
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
        this.pos2 = pos2;
    }

    /**
     * Copy the blocks between pos1 and pos2 to the clipboard
     * @param user - user
     * @return true if successful, false if pos1 or pos2 are undefined
     */
    public boolean copy(User user) {
        if (pos1 == null || pos2 == null) {
            user.sendMessage("commands.admin.schem.need-pos1-pos2");
            return false;
        }
        int count = 0;
        for (int x = Math.min(pos1.getBlockX(), pos2.getBlockX()); x <= Math.max(pos1.getBlockX(),pos2.getBlockX()); x++) {
            for (int y = Math.min(pos1.getBlockY(), pos2.getBlockY()); y <= Math.max(pos1.getBlockY(),pos2.getBlockY()); y++) {
                for (int z = Math.min(pos1.getBlockZ(), pos2.getBlockZ()); z <= Math.max(pos1.getBlockZ(),pos2.getBlockZ()); z++) {
                    copyBlock(pos1.getWorld().getBlockAt(x, y, z), user.getLocation());
                    count++;
                }
            }
        }
        user.sendMessage("commands.admin.schem.copied-blocks", "[number]", String.valueOf(count));
        copied = true;
        return true;
    }

    /**
     * Pastes the clipboard to location
     * @param location
     */
    public void paste(Location location) {
        blockConfig.getKeys(false).forEach(b -> pasteBlock(location, blockConfig.getConfigurationSection(b)));
    }

    private void pasteBlock(Location location, ConfigurationSection s) {
        String[] pos = s.getName().split(",");
        int x = location.getBlockX() + Integer.valueOf(pos[0]);
        int y = location.getBlockY() + Integer.valueOf(pos[1]);
        int z = location.getBlockZ() + Integer.valueOf(pos[2]);
        Material m = Material.getMaterial(s.getString("type"));
        Block block = location.getWorld().getBlockAt(x, y, z);
        if (s.getBoolean("attached")) {
            plugin.getLogger().info("Setting 1 tick later for " + m.toString());
            plugin.getServer().getScheduler().runTask(plugin, () -> setBlock(block, s, m));
        } else {
            setBlock(block, s, m);
        }
    }

    @SuppressWarnings("deprecation")
    private void setBlock(Block block, ConfigurationSection s, Material m) {
     // Block state
        
        if (s.getBoolean("attached") && m.toString().contains("TORCH")) {
            TorchDir d = TorchDir.valueOf(s.getString("facing"));

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

        block.setType(m);
        
        BlockState bs = block.getState();

        byte data = (byte)s.getInt("data");
        block.setData(data);


        // Material Data
        MaterialData md = bs.getData();
        if (md instanceof Openable) {
            Bukkit.getLogger().info("Openable");
            Openable open = (Openable)md;
            open.setOpen(s.getBoolean("open")); 
        }

        if (md instanceof Directional) {
            Bukkit.getLogger().info("Directional");
            Directional facing = (Directional)md;
            facing.setFacingDirection(BlockFace.valueOf(s.getString("facing")));
        }

        if (md instanceof Lever) {
            Bukkit.getLogger().info("Lever");
            Lever r = (Lever)md;
            r.setPowered(s.getBoolean("powered"));
        }
        if (md instanceof Button) {
            Bukkit.getLogger().info("Button");
            Button r = (Button)md;
            r.setPowered(s.getBoolean("powered"));
        }
        // Block data
        if (bs instanceof Sign) {
            Bukkit.getLogger().info("Sign");
            Sign sign = (Sign)bs;
            List<String> lines = s.getStringList("lines");
            for (int i =0 ; i < lines.size(); i++) {
                sign.setLine(i, lines.get(i));
            }
            sign.update();
        }
        if (bs instanceof Banner) {
            Bukkit.getLogger().info("Banner");
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
        
        bs.update(true, false);
        if (bs instanceof InventoryHolder) {
            Bukkit.getLogger().info("Inventory holder " + s.getCurrentPath());
            
            InventoryHolder ih = (InventoryHolder)bs;
            @SuppressWarnings("unchecked")
            List<ItemStack> items = (List<ItemStack>) s.get("inventory");
            for (int i = 0; i < ih.getInventory().getSize(); i++) {
                ih.getInventory().setItem(i, items.get(i));
            }
        }

    }
    
    @SuppressWarnings("deprecation")
    private void copyBlock(Block block, Location origin) {
        if (block.getType().equals(Material.AIR)) {
            return;
        }
        // Create position
        int x = block.getLocation().getBlockX() - origin.getBlockX();
        int y = block.getLocation().getBlockY() - origin.getBlockY();
        int z = block.getLocation().getBlockZ() - origin.getBlockZ();
        String pos = x + "," + y + "," + z;

        // Position defines the section
        ConfigurationSection s = blockConfig.createSection(pos);
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
            Bukkit.getLogger().info("Openable");
            Openable open = (Openable)md;
            s.set("open", open.isOpen()); 
        }
        if (md instanceof Directional) {
            Bukkit.getLogger().info("Directional");
            Directional facing = (Directional)md;
            s.set("facing", facing.getFacing().name()); 
        }
        if (md instanceof Attachable) {
            Bukkit.getLogger().info("Attachable");
            Attachable facing = (Attachable)md;
            s.set("facing", facing.getFacing().name());
            s.set("attached-face", facing.getAttachedFace().name());
            s.set("attached", true);
        }
        if (md instanceof Colorable) {
            Bukkit.getLogger().info("Colorable");
            Colorable c = (Colorable)md;
            s.set("color", c.getColor().name());
        }
        if (block.getType().equals(Material.CARPET)) {
            Bukkit.getLogger().info("Carpet");
            DyeColor c = DyeColor.getByWoolData(block.getData());
            s.set("color", c.name());  
        }
        if (md instanceof Redstone) {
            Bukkit.getLogger().info("Redstone");
            Redstone r = (Redstone)md;
            blockConfig.set("powered", r.isPowered());
        }

        // Block data
        if (bs instanceof Sign) {
            Bukkit.getLogger().info("Sign");
            Sign sign = (Sign)bs;
            s.set("lines", Arrays.asList(sign.getLines()));
        }
        if (bs instanceof Banner) {
            Bukkit.getLogger().info("Banner");
            Banner banner = (Banner)bs;
            s.set("baseColor", banner.getBaseColor().toString());
            banner.getPatterns().forEach(p -> {
                s.set("pattern." + p.getPattern().toString(), p.getColor().toString());
            });
        }
        if (bs instanceof InventoryHolder) {
            Bukkit.getLogger().info("Inventory holder");
            InventoryHolder ih = (InventoryHolder)bs;
            s.set("inventory", ih.getInventory().getContents());
        }

    }
    /**
     * @return the blockConfig
     */
    private YamlConfiguration getBlockConfig() {
        return blockConfig;
    }

    /**
     * Load a file to clipboard
     * @param file
     * @throws IOException 
     * @throws InvalidConfigurationException 
     */
    public void load(File file) throws IOException, InvalidConfigurationException {
            unzip(file.getAbsolutePath());
            blockConfig.load(file);
            copied = true;
            Files.delete(file.toPath());
    }
    
    private void unzip(final String zipFilePath) throws IOException {
        Path path = Paths.get(zipFilePath);
        if (!(Files.exists(path))) {
            return;
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
            int read = 0;
            while ((read = zipInputStream.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }

    }
    
    /**
     * Save the clipboard to a file
     * @param file
     */
    public void save(File file) {
        try {
            getBlockConfig().save(file);
            zip(file);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
    private void zip(File targetFile) {

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(targetFile.getAbsolutePath() + ".schem"))) {
            zipOutputStream.putNextEntry(new ZipEntry(targetFile.getName()));
            try (FileInputStream inputStream = new FileInputStream(targetFile)) {

                final byte[] buffer = new byte[1024];
                int length;
                while((length = inputStream.read(buffer)) >= 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
                inputStream.close();
                zipOutputStream.close();
                targetFile.delete();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public boolean isFull() {
        return copied;
    }
}
