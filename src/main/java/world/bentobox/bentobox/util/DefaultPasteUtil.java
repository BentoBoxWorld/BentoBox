package world.bentobox.bentobox.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.nms.PasteHandler;

import java.util.*;

/**
 * A utility class for {@link PasteHandler}
 *
 * @author tastybento
 */
public class DefaultPasteUtil {
    private static final String MINECRAFT = "minecraft:";
    private static final Map<String, String> BLOCK_CONVERSION = Map.of("sign", "oak_sign", "wall_sign", "oak_wall_sign");
    private static final BentoBox plugin;

    static {
        plugin = BentoBox.getInstance();
    }

    /**
     * Set the block to the location
     *
     * @param island   - island
     * @param location - location
     * @param bpBlock  - blueprint block
     */
    public static void setBlock(Island island, Location location, BlueprintBlock bpBlock) {
        Util.getChunkAtAsync(location).thenRun(() -> {
            Block block = location.getBlock();
            // Set the block data - default is AIR
            BlockData bd;
            try {
                bd = Bukkit.createBlockData(bpBlock.getBlockData());
            } catch (Exception e) {
                bd = convertBlockData(bpBlock);
            }
            block.setBlockData(bd, false);
            setBlockState(island, block, bpBlock);
            // Set biome
            if (bpBlock.getBiome() != null) {
                block.setBiome(bpBlock.getBiome());
            }
        });
    }

    /**
     * Convert the blueprint to block data
     *
     * @param block - the blueprint block
     * @return the block data
     */
    public static BlockData convertBlockData(BlueprintBlock block) {
        BlockData blockData = Bukkit.createBlockData(Material.AIR);
        try {
            for (Map.Entry<String, String> en : BLOCK_CONVERSION.entrySet()) {
                if (block.getBlockData().startsWith(MINECRAFT + en.getKey())) {
                    blockData = Bukkit.createBlockData(block.getBlockData().replace(MINECRAFT + en.getKey(), MINECRAFT + en.getValue()));
                    break;
                }
            }
        } catch (IllegalArgumentException e) {
            // This may happen if the block type is no longer supported by the server
            plugin.logWarning("Blueprint references materials not supported on this server version.");
            plugin.logWarning("Load blueprint manually, check and save to fix for this server version.");
            plugin.logWarning("Failed block data: " + block.getBlockData());
        }
        return blockData;
    }

    /**
     * Handles signs, chests and mob spawner blocks
     *
     * @param island  - island
     * @param block   - block
     * @param bpBlock - config
     */
    public static void setBlockState(Island island, Block block, BlueprintBlock bpBlock) {
        // Get the block state
        BlockState bs = block.getState();
        // Signs
        if (bs instanceof Sign) {
            writeSign(island, block, bpBlock.getSignLines(), bpBlock.isGlowingText());
        }
        // Chests, in general
        if (bs instanceof InventoryHolder holder) {
            Inventory ih = holder.getInventory();
            // Double chests are pasted as two blocks so inventory is filled twice.
            // This code stops over-filling for the first block.
            bpBlock.getInventory().forEach(ih::setItem);
        }
        // Mob spawners
        if (bs instanceof CreatureSpawner spawner) {
            setSpawner(spawner, bpBlock.getCreatureSpawner());
        }
        // Banners
        if (bs instanceof Banner banner && bpBlock.getBannerPatterns() != null) {
            bpBlock.getBannerPatterns().removeIf(Objects::isNull);
            banner.setPatterns(bpBlock.getBannerPatterns());
            banner.update(true, false);
        }
    }

    /**
     * Set the spawner setting from the blueprint
     *
     * @param spawner - spawner
     * @param s       - blueprint spawner
     */
    public static void setSpawner(CreatureSpawner spawner, BlueprintCreatureSpawner s) {
        spawner.setSpawnedType(s.getSpawnedType());
        spawner.setMaxNearbyEntities(s.getMaxNearbyEntities());
        spawner.setMaxSpawnDelay(s.getMaxSpawnDelay());
        spawner.setMinSpawnDelay(s.getMinSpawnDelay());
        spawner.setDelay(s.getDelay());
        spawner.setRequiredPlayerRange(s.getRequiredPlayerRange());
        spawner.setSpawnRange(s.getSpawnRange());
        spawner.update(true, false);
    }

    /**
     * Spawn the blueprint entities to the location
     *
     * @param island   - island
     * @param location - location
     * @param list     - blueprint entities
     */
    public static void setEntity(Island island, Location location, List<BlueprintEntity> list) {
        World world = location.getWorld();
        assert world != null;
        Util.getChunkAtAsync(location).thenRun(() -> list.stream().filter(k -> k.getType() != null).forEach(k -> {
            LivingEntity e = (LivingEntity) location.getWorld().spawnEntity(location, k.getType());
            if (k.getCustomName() != null) {
                String customName = k.getCustomName();

                if (island != null) {
                    // Parse any placeholders in the entity's name, if the owner's connected (he should)
                    Optional<Player> owner = Optional.ofNullable(island.getOwner())
                            .map(User::getInstance)
                            .map(User::getPlayer);
                    if (owner.isPresent()) {
                        // Parse for the player's name first (in case placeholders might need it)
                        customName = customName.replace(TextVariables.NAME, owner.get().getName());
                        // Now parse the placeholders
                        customName = plugin.getPlaceholdersManager().replacePlaceholders(owner.get(), customName);
                    }
                }

                // Actually set the custom name
                e.setCustomName(customName);
            }
            k.configureEntity(e);
        }));
    }

    /**
     * Write the lines to the sign at the block
     *
     * @param island - island
     * @param block  - block
     * @param lines  - lines
     * @param glow   - is sign glowing?
     */
    public static void writeSign(Island island, final Block block, final List<String> lines, boolean glow) {
        BlockFace bf;
        if (block.getType().name().contains("WALL_SIGN")) {
            WallSign wallSign = (WallSign) block.getBlockData();
            bf = wallSign.getFacing();
        } else {
            org.bukkit.block.data.type.Sign sign = (org.bukkit.block.data.type.Sign) block.getBlockData();
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
        org.bukkit.block.Sign s = (org.bukkit.block.Sign) block.getState();
        // Sign text must be stored under the addon's name.sign.line0,1,2,3 in the yaml file
        if (island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.START_TEXT)) {
            // Get the addon that is operating in this world
            String addonName = plugin.getIWM().getAddon(island.getWorld()).map(addon -> addon.getDescription().getName().toLowerCase(Locale.ENGLISH)).orElse("");
            Optional<User> user = Optional.ofNullable(island.getOwner()).map(User::getInstance);
            if (user.isPresent()) {
                for (int i = 0; i < 4; i++) {
                    s.setLine(i, Util.translateColorCodes(plugin.getLocalesManager().getOrDefault(user.get(),
                            addonName + ".sign.line" + i, "").replace(TextVariables.NAME, name)));
                }
            }
        } else {
            // Just paste
            for (int i = 0; i < 4; i++) {
                s.setLine(i, lines.get(i));
            }
        }
        s.setGlowingText(glow);
        // Update the sign
        s.update();
    }
}
