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
    private final World world;
    // The minimum block position (x,y,z)
    private Location pos1;
    // The maximum block position (x,y,z)
    private Location pos2;
    private PasteState pasteState;
    private BukkitTask pastingTask;
    private BlueprintClipboard clipboard;
    private CompletableFuture<Void> currentTask = CompletableFuture.completedFuture(null);

    /**
     * The Blueprint to paste.
     */
    @NonNull
    private final Blueprint blueprint;

    /**
     * The Location to paste to.
     */
    @NonNull
    private final Location location;

    /**
     * Island related to this paste, may be null.
     */
    @Nullable
    private final Island island;

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
        this.blueprint = Objects.requireNonNull(clipboard.getBlueprint(), "Clipboard cannot have a null Blueprint");
        this.location = location;
        this.world = location.getWorld();
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
        this.world = world;
        // Offset due to bedrock
        Vector off = bp.getBedrock() != null ? bp.getBedrock() : new Vector(0,0,0);
        // Calculate location for pasting
        this.location = island.getProtectionCenter().toVector().subtract(off).toLocation(world);
    }

    private record Bits(Map<Vector, BlueprintBlock> blocks,
            Map<Vector, BlueprintBlock> attached,
            Map<Vector, List<BlueprintEntity>> entities,
            Iterator<Entry<Vector, BlueprintBlock>> it,
            Iterator<Entry<Vector, BlueprintBlock>> it2,
            Iterator<Entry<Vector, List<BlueprintEntity>>> it3,
            int pasteSpeed) {}
    /**
     * The main pasting method
     */
    public CompletableFuture<Boolean> paste() {
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
        pastingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> pasterTask(result, owner, bits), 0L, 1L);

        return result;
    }

    private void pasterTask(CompletableFuture<Boolean> result, Optional<User> owner, Bits bits) {
        if (!currentTask.isDone()) return;

        final int pasteSpeed = plugin.getSettings().getPasteSpeed();

        long timer = System.currentTimeMillis();
        int count = 0;
        if (pasteState.equals(PasteState.CHUNK_LOAD)) {
            pasteState = PasteState.CHUNK_LOADING;
            // Load chunk
            currentTask = Util.getChunkAtAsync(location).thenRun(() -> {
                pasteState = PasteState.BLOCKS;
                long duration = System.currentTimeMillis() - timer;
                if (duration > chunkLoadTime) {
                    chunkLoadTime = duration;
                }
            });
        }
        else if (pasteState.equals(PasteState.BLOCKS) || pasteState.equals(PasteState.ATTACHMENTS)) {
            Iterator<Entry<Vector, BlueprintBlock>> it = pasteState.equals(PasteState.BLOCKS) ? bits.it : bits.it2;
            if (it.hasNext()) {
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
                    currentTask = paster.pasteBlocks(island, world, blockMap);
                }
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
        else if (pasteState.equals(PasteState.ENTITIES)) {
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
                    Location center = new Location(world, x, y, z).add(new Vector(0.5, 0.5, 0.5));
                    List<BlueprintEntity> entities = entry.getValue();
                    entityMap.put(center, entities);
                    count++;
                }
                if (!entityMap.isEmpty()) {
                    currentTask = paster.pasteEntities(island, world, entityMap);
                }
            } else {
                pasteState = PasteState.DONE;

                String world = switch (location.getWorld().getEnvironment()) {
                    case NETHER -> owner.map(user -> user.getTranslation("general.worlds.nether")).orElse("");
                    case THE_END -> owner.map(user -> user.getTranslation("general.worlds.the-end")).orElse("");
                    default -> owner.map(user -> user.getTranslation("general.worlds.overworld")).orElse("");
                };

                owner.ifPresent(user -> user.sendMessage("commands.island.create.pasting.dimension-done", "[world]", world));
            }
        }
        else if (pasteState.equals(PasteState.DONE)) {
            // All done. Cancel task
            // Set pos1 and 2 if this was a clipboard paste
            if (island == null && clipboard != null) {
                clipboard.setPos1(pos1);
                clipboard.setPos2(pos2);
            }
            pasteState = PasteState.CANCEL;
            result.complete(true);
        } else if (pasteState.equals(PasteState.CANCEL)) {
            // This state makes sure the follow-on task only ever runs once
            pastingTask.cancel();
            result.complete(true);
        }
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
