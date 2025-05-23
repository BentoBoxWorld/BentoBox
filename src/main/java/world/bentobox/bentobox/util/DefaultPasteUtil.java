package world.bentobox.bentobox.util;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.TrialSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.spawner.TrialSpawnerConfiguration;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.FancyNpcsHook;
import world.bentobox.bentobox.hooks.ItemsAdderHook;
import world.bentobox.bentobox.hooks.MythicMobsHook;
import world.bentobox.bentobox.hooks.ZNPCsPlusHook;
import world.bentobox.bentobox.nms.PasteHandler;

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

    private DefaultPasteUtil() {} // private constructor to hide the implicit public one.

    /**
     * Set the block to the location
     *
     * @param island   - island
     * @param location - location
     * @param bpBlock  - blueprint block
     */
    public static CompletableFuture<Void> setBlock(Island island, Location location, BlueprintBlock bpBlock) {
        return Util.getChunkAtAsync(location).thenRun(() -> {
            Block block = location.getBlock();
            // Set the block data - default is AIR
            BlockData bd = createBlockData(bpBlock);
            block.setBlockData(bd, false);
            setBlockState(island, block, bpBlock);
            // Set biome
            if (bpBlock.getBiome() != null) {
                block.setBiome(bpBlock.getBiome());
            }
        });
    }

    /**
     * Create a block data from the blueprint
     *
     * @param block - blueprint block
     * @return the block data
     */
    public static BlockData createBlockData(BlueprintBlock block) {
        try {
            return Bukkit.createBlockData(block.getBlockData());
        } catch (Exception e) {
            return convertBlockData(block);
        }
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
     * Handles signs, chests, frames, and mob spawner blocks
     *
     * @param island  - island
     * @param block   - block
     * @param bpBlock - config
     */
    public static void setBlockState(Island island, Block block, BlueprintBlock bpBlock) {
        // Get the block state
        BlockState bs = block.getState();
        // ItemFrames
        if (bs instanceof ItemFrame frame ) {
            if (!bpBlock.getInventory().isEmpty()) {
                frame.setItem(bpBlock.getInventory().get(0));
                bs.update();
            }
        } else
            // Signs
            if (bs instanceof Sign) {
                for (Side side : Side.values()) {
                    writeSign(island, block, bpBlock, side);
                }
            }
        // Chests, in general
            else if (bs instanceof InventoryHolder holder) {
                Inventory ih = holder.getInventory();
                // Double chests are pasted as two blocks so inventory is filled twice.
                // This code stops over-filling for the first block.
                bpBlock.getInventory().forEach((slot, item) -> ih.setItem(slot, item));
            }
        // Mob spawners
            else if (bs instanceof CreatureSpawner spawner) {
                setSpawner(spawner, bpBlock.getCreatureSpawner());
            }
            else if (bs instanceof TrialSpawner ts) {
                TrialSpawnerConfiguration config = ts.getNormalConfiguration();
                ts.setOminous(bpBlock.getTrialSpawner().configTrialSpawner(config));
                if (!bs.update(true, false)) {
                    BentoBox.getInstance().logError("Trial Spawner update failed!");
                }
            }
        // Banners
            else if (bs instanceof Banner banner && bpBlock.getBannerPatterns() != null) {
                bpBlock.getBannerPatterns().removeIf(Objects::isNull);
                banner.setPatterns(bpBlock.getBannerPatterns());
                banner.update(true, false);
            } else // Check ItemsAdder
                if (bpBlock.getItemsAdderBlock() != null && !bpBlock.getItemsAdderBlock().isEmpty()) {
                    BentoBox.getInstance().getHooks().getHook("ItemsAdder")
                    .ifPresent(h -> ItemsAdderHook.place(bpBlock.getItemsAdderBlock(), block.getLocation()));
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
     * @return future boolean - true if Bukkit entity spawned, false another plugin entity spawned
     */
    public static CompletableFuture<Void> setEntity(Island island, Location location, List<BlueprintEntity> list) {
        World world = location.getWorld();
        assert world != null;
        return Util.getChunkAtAsync(location)
                .thenRun(() -> list.stream().forEach(k -> spawnBlueprintEntity(k, location, island)));
    }

    /**
     * Spawn an entity
     * @param k the blueprint entity definition
     * @param location location to paste the entity
     * @param island island
     * @return true if Bukkit entity spawned, false another plugin entity spawned
     */
    static boolean spawnBlueprintEntity(BlueprintEntity k, Location location, Island island) {
        // Display Entity (holograms, etc.)
        k.setDisplay(location);
        // FancyNpc entity
        if (k.getNpc() != null
                && plugin.getHooks().getHook("FancyNpcs").filter(mmh -> mmh instanceof FancyNpcsHook).map(mmh -> {
                    try {
                        return ((FancyNpcsHook) mmh).spawnNpc(k.getNpc(), location);
                    } catch (InvalidConfigurationException e) {
                        plugin.logError("FancyNpc loading failed in blueprint.");
                        return false;
                    }
                }).orElse(false)) {
            // Npc has spawned.
            return false;
        }
        // ZNPCsPlus
        if (k.getNpc() != null
                && plugin.getHooks().getHook("ZNPCsPlus").filter(mmh -> mmh instanceof ZNPCsPlusHook).map(znpch -> {
                    try {
                        return ((ZNPCsPlusHook) znpch).spawnNpc(k.getNpc(), location);
                    } catch (InvalidConfigurationException e) {
                        plugin.logError("ZNPCsPlus loading failed in blueprint.");
                        return false;
                    }
                }).orElse(false)) {
            // Npc has spawned.
            return false;
        }

        // Mythic Mobs entity
        if (k.getMythicMobsRecord() != null && plugin.getHooks().getHook("MythicMobs")
                .filter(mmh -> mmh instanceof MythicMobsHook)
                .map(mmh -> ((MythicMobsHook) mmh).spawnMythicMob(k.getMythicMobsRecord(), location))
                .orElse(false)) {
            // MythicMob has spawned.
            return false;
        }
        if (k.getType() == null) {
            // Nothing
            return false;
        }
        Entity e = location.getWorld().spawnEntity(location, k.getType());
        if (k.getCustomName() != null) {
            String customName = k.getCustomName();

            if (island != null) {
                // Parse any placeholders in the entity's name, if the owner's connected (he should)
                Optional<Player> owner = Optional.ofNullable(island.getOwner()).map(User::getInstance)
                        .map(User::getPlayer);
                if (owner.isPresent()) {
                    // Parse for the player's name first (in case placeholders might need it)
                    customName = customName.replace(TextVariables.NAME, owner.get().getName());
                    // Now parse the placeholders
                    customName = plugin.getPlaceholdersManager().replacePlaceholders(owner.get(), customName);
                }
            }

            // Actually set the custom name
            e.customName(Component.text(customName));
        }
        k.configureEntity(e);

        return true;
    }

    /**
     * Write the lines to the sign at the block
     *
     * @param island - island
     * @param block  - block
     * @param bpSign - BlueprintBlock that is the sign
     * @param side   - the side being written
     */
    @SuppressWarnings("deprecation")
    public static void writeSign(Island island, final Block block, BlueprintBlock bpSign, Side side) {
        List<String> lines = bpSign.getSignLines(side);
        boolean glow = bpSign.isGlowingText(side);
        BlockData bd = block.getBlockData();
        BlockFace bf = (bd instanceof WallSign ws) ? ws.getFacing()
                : ((org.bukkit.block.data.type.Sign) bd).getRotation();
        // Handle spawn sign
        if (side == Side.FRONT && island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.SPAWN_HERE)) {
            if (bd instanceof Waterlogged wl && wl.isWaterlogged()) {
                block.setType(Material.WATER);
            } else {
                block.setType(Material.AIR);
            }
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
        Sign s = (org.bukkit.block.Sign) block.getState();
        SignSide signSide = s.getSide(side);
        // Sign text must be stored under the addon's name.sign.line0,1,2,3 in the yaml file
        if (island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.START_TEXT)) {
            // Get the addon that is operating in this world
            String addonName = plugin.getIWM().getAddon(island.getWorld()).map(addon -> addon.getDescription().getName().toLowerCase(Locale.ENGLISH)).orElse("");
            Optional<User> user = Optional.ofNullable(island.getOwner()).map(User::getInstance);
            if (user.isPresent()) {
                for (int i = 0; i < 4; i++) {
                    signSide.setLine(i, Util.translateColorCodes(plugin.getLocalesManager().getOrDefault(user.get(),
                            addonName + ".sign.line" + i, "").replace(TextVariables.NAME, name)));
                }
            }
        } else {
            // Just paste
            for (int i = 0; i < 4; i++) {
                signSide.setLine(i, lines.get(i));
            }
        }
        signSide.setGlowingText(glow);
        // Update the sign
        s.update();
    }
}
