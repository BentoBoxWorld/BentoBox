package world.bentobox.bentobox.blueprints;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.util.Util;

/**
 * This class pastes the clipboard it is given
 * @author tastybento
 *
 */
public class BlueprintPaster {

    /**
     * This tracks the stages of pasting from loading the chunk, pasting blocks, attachments, entities and then finishing.
     */
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

    private final BentoBox plugin;
    private final PasteHandler paster = Util.getPasteHandler();
    private final PasteHandler fallback = new world.bentobox.bentobox.nms.fallback.PasteHandlerImpl();
    private final World world;
    // The minimum block position (x,y,z)
    private Location pos1;
    // The maximum block position (x,y,z)
    private Location pos2;
    private PasteState pasteState;
    private BukkitTask pastingTask;
    private BlueprintClipboard clipboard;
    private CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);
    private boolean sink;

    /**
     * The Blueprint to paste.
     */
    @NonNull
    private final Blueprint blueprint;

    /**
     * The Location to paste to.
     */
    @NonNull
    private Location location;

    /**
     * Island related to this paste, may be null.
     */
    @Nullable
    private final Island island;

    /**
     * Paste a clipboard to a location. Run {@link #paste()} to paste
     * @param plugin - BentoBox
     * @param clipboard - clipboard to paste
     * @param location - location to which to paste
     */
    public BlueprintPaster(@NonNull BentoBox plugin, @NonNull BlueprintClipboard clipboard, @NonNull Location location) {
        this.plugin = plugin;
        this.clipboard = clipboard;
        // Calculate location for pasting
        this.blueprint = Objects.requireNonNull(clipboard.getBlueprint(), "Clipboard cannot have a null Blueprint");
        this.location = location;
        this.world = location.getWorld();
        this.island = null;
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
        this.world = world;
        // Offset due to bedrock
        Vector off = bp.getBedrock() != null ? bp.getBedrock() : new Vector(0,0,0);
        // Calculate location for pasting
        this.location = island.getProtectionCenter().toVector().subtract(off).toLocation(world);
        // Ensure the y coordinate is within the world limits
        int y = Math.min(world.getMaxHeight() - 1, Math.max(world.getMinHeight(), location.getBlockY()));
        location.setY(y);
    }

    /**
     * A record of all the "bits" of the blueprint that need to be pasted
     * Consists of blocks, attached blocks, entities, iterators for the blocks and a speed
     */
    private record Bits(
            /**
             * Basic blocks to the pasted (not attached blocks)
             */
            Map<Vector, BlueprintBlock> blocks,
            /**
             * Attached blocks
             */
            Map<Vector, BlueprintBlock> attached,
            /**
             * Entities to be pasted
             */
            Map<Vector, List<BlueprintEntity>> entities,
            /**
             * Basic block pasting iterator
             */
            Iterator<Entry<Vector, BlueprintBlock>> it,
            /**
             * Attached block pasting iterator
             */
            Iterator<Entry<Vector, BlueprintBlock>> it2,
            /**
             * Entity pasting iterator
             */
            Iterator<Entry<Vector, List<BlueprintEntity>>> it3,
            /**
             * Paste speed
             */
            int pasteSpeed) {}

    /**
     * The main pasting method
     */
    public CompletableFuture<Boolean> paste() {
        return this.paste(true);
    }

    /**
     * Paste the clipboard
     * @param useNMS if true, NMS pasting will be used, otherwise Bukkit API
     * @return Future boolean where true is success
     */
    public CompletableFuture<Boolean> paste(boolean useNMS) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        // Iterators for the various maps to paste
        final Map<Vector, BlueprintBlock> blocks = blueprint.getBlocks() == null ? Collections.emptyMap() : blueprint.getBlocks();
        final Map<Vector, BlueprintBlock> attached = blueprint.getAttached() == null ? Collections.emptyMap() : blueprint.getAttached();
        final Map<Vector, List<BlueprintEntity>> entities = blueprint.getEntities() == null ? Collections.emptyMap() : blueprint.getEntities();

        // Initial state & speed
        pasteState = PasteState.CHUNK_LOAD;

        // If this is an island OVERWORLD paste, get the island owner.
        final Optional<User> owner = Optional.ofNullable(island).map(i -> User.getInstance(i.getOwner()));

        // Tell the owner we're pasting blocks and how much time it might take
        owner.ifPresent(user -> tellOwner(user, blocks.size(), attached.size(), entities.size(), plugin.getSettings().getPasteSpeed()));
        Bits bits = new Bits(blocks, attached, entities,
                blocks.entrySet().iterator(), attached.entrySet().iterator(), entities.entrySet().iterator(),
                plugin.getSettings().getPasteSpeed());
        pastingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> pasterTask(result, owner, bits, useNMS), 0L, 1L);

        return result;
    }

    private void pasterTask(CompletableFuture<Boolean> result, Optional<User> owner, Bits bits, boolean useNMS) {
        if (!currentTask.isDone()) return;

        final int pasteSpeed = plugin.getSettings().getPasteSpeed();

        int count = 0;
        if (pasteState.equals(PasteState.CHUNK_LOAD)) {
            loadChunk();
        }
        else if (pasteState.equals(PasteState.BLOCKS) || pasteState.equals(PasteState.ATTACHMENTS)) {
            pasteBlocks(bits, count, owner, pasteSpeed, useNMS);
        }
        else if (pasteState.equals(PasteState.ENTITIES)) {
            pasteEntities(bits, count, owner, pasteSpeed, useNMS);
        }
        else if (pasteState.equals(PasteState.DONE)) {
            // All done. Cancel task
            cancelTask(result);
        } else if (pasteState.equals(PasteState.CANCEL)) {
            // This state makes sure the follow-on task only ever runs once
            pastingTask.cancel();
            result.complete(true);
        }
    }

    private void cancelTask(CompletableFuture<Boolean> result) {
        // Set pos1 and 2 if this was a clipboard paste
        if (island == null && clipboard != null) {
            clipboard.setPos1(pos1);
            clipboard.setPos2(pos2);
        }
        pasteState = PasteState.CANCEL;
        result.complete(true);      
    }

    private void pasteEntities(Bits bits, int count, Optional<User> owner, int pasteSpeed, boolean useNMS) {
        if (bits.it3().hasNext()) {
            Map<Location, List<BlueprintEntity>> entityMap = new HashMap<>();
            // Paste entities
            while (count < pasteSpeed) {
                if (!bits.it3().hasNext()) {
                    break;
                }
                Entry<Vector, List<BlueprintEntity>> entry = bits.it3().next();
                int x = location.getBlockX() + entry.getKey().getBlockX();
                int y = location.getBlockY() + entry.getKey().getBlockY();
                int z = location.getBlockZ() + entry.getKey().getBlockZ();
                Location center = new Location(world, x, y, z).add(new Vector(0.5, 0D, 0.5));
                List<BlueprintEntity> entities = entry.getValue();
                entityMap.put(center, entities);
                count++;
            }
            if (!entityMap.isEmpty()) {
                currentTask = useNMS ? paster.pasteEntities(island, world, entityMap)
                        : fallback.pasteEntities(island, world, entityMap);
            }
        } else {
            pasteState = PasteState.DONE;

            String dimensionType = switch (location.getWorld().getEnvironment()) {
            case NETHER -> owner.map(user -> user.getTranslation("general.worlds.nether")).orElse("");
            case THE_END -> owner.map(user -> user.getTranslation("general.worlds.the-end")).orElse("");
            default -> owner.map(user -> user.getTranslation("general.worlds.overworld")).orElse("");
            };

            owner.ifPresent(user -> user.sendMessage("commands.island.create.pasting.dimension-done", "[world]", dimensionType));
        }

    }

    private void pasteBlocks(Bits bits, int count, Optional<User> owner, int pasteSpeed, boolean useNMS) {
        Iterator<Entry<Vector, BlueprintBlock>> it = pasteState.equals(PasteState.BLOCKS) ? bits.it : bits.it2;
        if (it.hasNext()) {
            pasteBlocksNow(it, count, pasteSpeed, useNMS);
        } else {
            if (pasteState.equals(PasteState.BLOCKS)) {
                // Blocks done
                // Next paste attachments
                pasteState = PasteState.ATTACHMENTS;
            } else {
                // Attachments done. Next paste entities
                pasteState = PasteState.ENTITIES;
                if (bits.entities.size() != 0) {
                    owner.ifPresent(user -> user.sendMessage("commands.island.create.pasting.entities", TextVariables.NUMBER, String.valueOf(bits.entities.size())));
                }
            }
        }

    }

    private void pasteBlocksNow(Iterator<Entry<Vector, BlueprintBlock>> it, int count, int pasteSpeed, boolean useNMS) {
        Map<Location, BlueprintBlock> blockMap = new HashMap<>();
        // Paste blocks
        while (count < pasteSpeed) {
            if (!it.hasNext()) {
                break;
            }
            Entry<Vector, BlueprintBlock> entry = it.next();
            Location pasteTo = location.clone().add(entry.getKey());
            // pos1 and pos2 update
            updatePos(pasteTo);

            BlueprintBlock block = entry.getValue();
            blockMap.put(pasteTo, block);
            count++;
        }
        if (!blockMap.isEmpty()) {
            currentTask = useNMS ? paster.pasteBlocks(island, world, blockMap)
                    : fallback.pasteBlocks(island, world, blockMap);
        }

    }

    private void loadChunk() {
        long timer = System.currentTimeMillis();
        pasteState = PasteState.CHUNK_LOADING;
        // Load chunk
        currentTask = Util.getChunkAtAsync(location).thenRun(() -> {
            pasteState = PasteState.BLOCKS;
            long duration = System.currentTimeMillis() - timer;
            if (duration > chunkLoadTime) {
                chunkLoadTime = duration;
            }
            // Adjust location if this is a sinking blueprint to put it on the ocean floor
            // Mayday! Mayday! We are sinking! ... What are you sinking about? https://youtu.be/gmOTpIVxji8?si=DC-u4qWRTN5fdWd8
            if (this.blueprint.isSink() && !sink) {
                sink = true; // Flag, just do this once
                location = new Location(location.getWorld(), location.getX(),
                        location.getWorld().getHighestBlockYAt(location, HeightMap.OCEAN_FLOOR), location.getZ());
            }
        });

    }

    private void tellOwner(User user, int blocksSize, int attachedSize, int entitiesSize, int pasteSpeed) {
        // Estimated time:
        double total = (double) blocksSize + attachedSize + entitiesSize;
        BigDecimal time = BigDecimal.valueOf(total / (pasteSpeed * 20.0D) + (chunkLoadTime / 1000.0D)).setScale(1, RoundingMode.UP);
        user.sendMessage("commands.island.create.pasting.estimated-time", TextVariables.NUMBER, String.valueOf(time.doubleValue()));
        // We're pasting blocks!
        user.sendMessage("commands.island.create.pasting.blocks", TextVariables.NUMBER, String.valueOf(blocksSize + attachedSize));
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
}
