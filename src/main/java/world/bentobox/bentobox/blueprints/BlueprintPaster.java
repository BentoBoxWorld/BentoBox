package world.bentobox.bentobox.blueprints;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.util.Util;

/**
 * This class pastes the clipboard it is given
 * @author tastybento
 *
 */
public class BlueprintPaster {

    enum PasteState {
        CHUNK_LOAD,
        CHUNK_LOADING,
        BLOCKS,
        ATTACHMENTS,
        ENTITIES,
        DONE,
        CANCEL
    }

    /**
     * Longest chunk loading time experienced when pasting an island.
     * It is used to fine-tune the estimated pasting time.
     * @since 1.11.1
     */
    private static long chunkLoadTime = 0;

    private static final String MINECRAFT = "minecraft:";

    private static final Map<String, String> BLOCK_CONVERSION = ImmutableMap.of("sign", "oak_sign", "wall_sign", "oak_wall_sign");

    private BentoBox plugin;
    // The minimum block position (x,y,z)
    private Location pos1;
    // The maximum block position (x,y,z)
    private Location pos2;
    private PasteState pasteState;
    private BukkitTask pastingTask;
    private BlueprintClipboard clipboard;

    /**
     * The Blueprint to paste.
     */
    @NonNull
    private Blueprint blueprint;

    /**
     * The Location to paste to.
     */
    @NonNull
    private Location location;

    /**
     * Island related to this paste, may be null.
     */
    @Nullable
    private Island island;

    /**
     * Paste a clipboard to a location and run task
     * @param plugin - BentoBox
     * @param clipboard - clipboard to paste
     * @param location - location to paste to
     */
    public BlueprintPaster(@NonNull BentoBox plugin, @NonNull BlueprintClipboard clipboard, @NonNull Location location) {
        this.plugin = plugin;
        this.clipboard = clipboard;
        // Calculate location for pasting
        this.blueprint = clipboard.getBlueprint();
        this.location = location;
        this.island = null;

        // Paste
        paste();
    }

    /**
     * Pastes a blueprint to an island
     * @param plugin - BentoBox
     * @param bp - blueprint to paste
     * @param world - world to paste to
     * @param island - island related to this paste
     */
    public BlueprintPaster(@NonNull BentoBox plugin, @NonNull Blueprint bp, World world, @NonNull Island island) {
        this.plugin = plugin;
        this.blueprint = bp;
        this.island = island;
        // Offset due to bedrock
        Vector off = bp.getBedrock() != null ? bp.getBedrock() : new Vector(0,0,0);
        // Calculate location for pasting
        this.location = island.getCenter().toVector().subtract(off).toLocation(world);
    }

    /**
     * The main pasting method
     */
    public CompletableFuture<Boolean> paste() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        // Iterators for the various maps to paste
        Map<Vector, BlueprintBlock> blocks = blueprint.getBlocks() == null ? Collections.emptyMap() : blueprint.getBlocks();
        Map<Vector, BlueprintBlock> attached = blueprint.getAttached() == null ? Collections.emptyMap() : blueprint.getAttached();
        Map<Vector, List<BlueprintEntity>> entities = blueprint.getEntities() == null ? Collections.emptyMap() : blueprint.getEntities();
        Iterator<Entry<Vector, BlueprintBlock>> it = blocks.entrySet().iterator();
        Iterator<Entry<Vector, BlueprintBlock>> it2 = attached.entrySet().iterator();
        Iterator<Entry<Vector, List<BlueprintEntity>>> it3 = entities.entrySet().iterator();

        // Initial state & speed
        pasteState = PasteState.CHUNK_LOAD;
        final int pasteSpeed = plugin.getSettings().getPasteSpeed();

        // If this is an island OVERWORLD paste, get the island owner.
        final Optional<User> owner = Optional.ofNullable(island)
                .filter(i -> location.getWorld().getEnvironment().equals(World.Environment.NORMAL))
                .map(i -> User.getInstance(i.getOwner()));
        // Tell the owner we're pasting blocks and how much time it might take
        owner.ifPresent(user -> {
            // Estimated time:
            double total = (double) blocks.size() + attached.size() + entities.size();
            BigDecimal time = BigDecimal.valueOf(total / (pasteSpeed * 20.0D) + (chunkLoadTime / 1000.0D)).setScale(1, RoundingMode.UP);
            user.sendMessage("commands.island.create.pasting.estimated-time", TextVariables.NUMBER, String.valueOf(time.doubleValue()));
            // We're pasting blocks!
            user.sendMessage("commands.island.create.pasting.blocks", TextVariables.NUMBER, String.valueOf(blocks.size() + attached.size()));
        });

        pastingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long timer = System.currentTimeMillis();
            int count = 0;
            if (pasteState.equals(PasteState.CHUNK_LOAD)) {
                pasteState = PasteState.CHUNK_LOADING;
                // Load chunk
                Util.getChunkAtAsync(location).thenRun(() -> {
                    pasteState = PasteState.BLOCKS;
                    long duration = System.currentTimeMillis() - timer;
                    if (duration > chunkLoadTime) {
                        chunkLoadTime = duration;
                    }
                });
            }
            while (pasteState.equals(PasteState.BLOCKS) && count < pasteSpeed && it.hasNext()) {
                pasteBlock(location, it.next());
                count++;
            }
            while (pasteState.equals(PasteState.ATTACHMENTS) && count < pasteSpeed && it2.hasNext()) {
                pasteBlock(location, it2.next());
                count++;
            }
            while (pasteState.equals(PasteState.ENTITIES) && count < pasteSpeed && it3.hasNext()) {
                pasteEntity(location, it3.next());
                count++;
            }
            // STATE SHIFT
            if (pasteState.equals(PasteState.BLOCKS) && !it.hasNext()) {
                // Blocks done
                // Next paste attachments
                pasteState = PasteState.ATTACHMENTS;
            }
            else if (pasteState.equals(PasteState.ATTACHMENTS) && !it2.hasNext()) {
                // Attachments done. Next paste entities
                pasteState = PasteState.ENTITIES;
                if (entities.size() != 0) {
                    owner.ifPresent(user -> user.sendMessage("commands.island.create.pasting.entities", TextVariables.NUMBER, String.valueOf(entities.size())));
                }
            }
            else if (pasteState.equals(PasteState.ENTITIES) && !it3.hasNext()) {
                pasteState = PasteState.DONE;
                owner.ifPresent(user -> user.sendMessage("commands.island.create.pasting.done"));
            }
            else if (pasteState.equals(PasteState.DONE)) {
                // All done. Cancel task
                // Set pos1 and 2 if this was a clipboard paste
                if (island == null && clipboard != null) {
                    clipboard.setPos1(pos1);
                    clipboard.setPos2(pos2);
                }
                result.complete(true);
                pasteState = PasteState.CANCEL;
            } else if (pasteState.equals(PasteState.CANCEL)) {
                // This state makes sure the follow-on task only ever runs once
                pastingTask.cancel();
                result.complete(true);
            }
        }, 0L, 1L);

        return result;
    }

    private void pasteBlock(Location location, Entry<Vector, BlueprintBlock> entry) {
        World world = location.getWorld();
        Location pasteTo = location.clone().add(entry.getKey());
        BlueprintBlock bpBlock = entry.getValue();
        Util.getChunkAtAsync(pasteTo).thenRun(() -> {
            Block block = pasteTo.getBlock();
            // Set the block data - default is AIR
            BlockData bd;
            try {
                bd = Bukkit.createBlockData(bpBlock.getBlockData());
            } catch (Exception e) {
                bd = convertBlockData(world, bpBlock);
            }
            block.setBlockData(bd, false);
            setBlockState(block, bpBlock);
            // pos1 and pos2 update
            updatePos(block.getLocation());
        });
    }

    /**
     * Tries to convert the BlockData to a newer version, and logs a warning if it fails to do so.
     * @return the converted BlockData or a default AIR BlockData.
     * @since 1.6.0
     */
    private BlockData convertBlockData(World world, BlueprintBlock block) {
        BlockData blockData = Bukkit.createBlockData(Material.AIR);
        try {
            for (Entry<String, String> en : BLOCK_CONVERSION.entrySet()) {
                if (block.getBlockData().startsWith(MINECRAFT + en.getKey())) {
                    blockData = Bukkit.createBlockData(block.getBlockData().replace(MINECRAFT + en.getKey(), MINECRAFT + en.getValue()));
                    break;
                }
            }
        } catch (IllegalArgumentException e) {
            // This may happen if the block type is no longer supported by the server
            plugin.logWarning("Blueprint references materials not supported on this server version.");
            plugin.logWarning("Load blueprint manually, check and save to fix for this server version.");
            plugin.logWarning("World: " + world.getName() + "; Failed block data: " + block.getBlockData());
        }
        return blockData;
    }

    private void pasteEntity(Location location, Entry<Vector, List<BlueprintEntity>> entry) {
        int x = location.getBlockX() + entry.getKey().getBlockX();
        int y = location.getBlockY() + entry.getKey().getBlockY();
        int z = location.getBlockZ() + entry.getKey().getBlockZ();
        setEntity(new Location(location.getWorld(), x, y, z), entry.getValue());
    }

    /**
     * Handles signs, chests and mob spawner blocks
     * @param block - block
     * @param bpBlock - config
     */
    private void setBlockState(Block block, BlueprintBlock bpBlock) {
        // Get the block state
        BlockState bs = block.getState();
        // Signs
        if (bs instanceof org.bukkit.block.Sign) {
            writeSign(block, bpBlock.getSignLines());
        }
        // Chests, in general
        if (bs instanceof InventoryHolder) {
            bs.update(true, false);
            Inventory ih = ((InventoryHolder)bs).getInventory();
            // Double chests are pasted as two blocks so inventory is filled twice. This code stops over filling for the first block.
            bpBlock.getInventory().forEach(ih::setItem);
        }
        // Mob spawners
        if (bs instanceof CreatureSpawner) {
            CreatureSpawner spawner = ((CreatureSpawner) bs);
            BlueprintCreatureSpawner s = bpBlock.getCreatureSpawner();
            spawner.setSpawnedType(s.getSpawnedType());
            spawner.setMaxNearbyEntities(s.getMaxNearbyEntities());
            spawner.setMaxSpawnDelay(s.getMaxSpawnDelay());
            spawner.setMinSpawnDelay(s.getMinSpawnDelay());
            spawner.setDelay(s.getDelay());
            spawner.setRequiredPlayerRange(s.getRequiredPlayerRange());
            spawner.setSpawnRange(s.getSpawnRange());
            bs.update(true, false);
        }
        // Banners
        if (bs instanceof Banner && bpBlock.getBannerPatterns() != null) {
            Banner banner = (Banner) bs;
            bpBlock.getBannerPatterns().removeIf(Objects::isNull);
            banner.setPatterns(bpBlock.getBannerPatterns());
            banner.update(true, false);
        }
    }

    /**
     * Sets any entity that is in this location
     * @param location - location
     * @param list - list of entities to paste
     */
    private void setEntity(Location location, List<BlueprintEntity> list) {
        list.stream().filter(k -> k.getType() != null).forEach(k -> {
            // Center, and just a bit high
            Location center = location.add(new Vector(0.5, 0.5, 0.5));
            Util.getChunkAtAsync(center).thenRun(() -> {
                LivingEntity e = (LivingEntity)location.getWorld().spawnEntity(center, k.getType());
                if (k.getCustomName() != null) {
                    String customName = k.getCustomName();

                    if (island != null) {
                        // Parse any placeholders in the entity's name, if the owner's connected (he should)
                        Player owner = User.getInstance(island.getOwner()).getPlayer();
                        if (owner != null) {
                            // Parse for the player's name first (in case placeholders might need it)
                            customName = customName.replace(TextVariables.NAME, owner.getName());
                            // Now parse the placeholders
                            customName = plugin.getPlaceholdersManager().replacePlaceholders(owner, customName);
                        }
                    }

                    // Actually set the custom name
                    e.setCustomName(customName);
                }
                k.configureEntity(e);
            });
        });
    }

    /**
     * Tracks the minimum and maximum block positions
     * @param l - location of block pasted
     */
    private void updatePos(Location l) {
        if (pos1 == null) {
            pos1 = l.clone();
        }
        if (pos2 == null) {
            pos2 = l.clone();
        }
        if (l.getBlockX() < pos1.getBlockX()) {
            pos1.setX(l.getBlockX());
        }
        if (l.getBlockX() > pos2.getBlockX()) {
            pos2.setX(l.getBlockX());
        }
        if (l.getBlockY() < pos1.getBlockY()) {
            pos1.setY(l.getBlockY());
        }
        if (l.getBlockY() > pos2.getBlockY()) {
            pos2.setY(l.getBlockY());
        }
        if (l.getBlockZ() < pos1.getBlockZ()) {
            pos1.setZ(l.getBlockZ());
        }
        if (l.getBlockZ() > pos2.getBlockZ()) {
            pos2.setZ(l.getBlockZ());
        }
    }

    private void writeSign(final Block block, final List<String> lines) {
        BlockFace bf;
        if (block.getType().name().contains("WALL_SIGN")) {
            WallSign wallSign = (WallSign)block.getBlockData();
            bf = wallSign.getFacing();
        } else {
            Sign sign = (Sign)block.getBlockData();
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
        org.bukkit.block.Sign s = (org.bukkit.block.Sign)block.getState();
        // Sign text must be stored under the addon's name.sign.line0,1,2,3 in the yaml file
        if (island != null && !lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.START_TEXT)) {
            // Get the addon that is operating in this world
            String addonName = plugin.getIWM().getAddon(island.getWorld()).map(addon -> addon.getDescription().getName().toLowerCase(Locale.ENGLISH)).orElse("");
            for (int i = 0; i < 4; i++) {
                s.setLine(i, ChatColor.translateAlternateColorCodes('&', plugin.getLocalesManager().getOrDefault(User.getInstance(island.getOwner()),
                        addonName + ".sign.line" + i,"").replace(TextVariables.NAME, name)));
            }
        } else {
            // Just paste
            for (int i = 0; i < 4; i++) {
                s.setLine(i, lines.get(i));
            }
        }
        // Update the sign
        s.update();
    }
}
