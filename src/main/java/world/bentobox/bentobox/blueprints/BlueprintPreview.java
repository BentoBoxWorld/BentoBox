package world.bentobox.bentobox.blueprints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Shows a player a miniature of a blueprint, built from block display entities that only that
 * player can see. Nothing is placed in the world: the entities are not saved, and they are
 * removed after a while, when the player leaves the area, or when another preview is shown.
 * <p>
 * Only blocks with an exposed face are shown, so anything buried in solid blocks stays hidden.
 * A blueprint with more exposed blocks than the configured limit is drawn at a coarser
 * resolution: several blocks become one larger block.
 *
 * @since 3.25.0
 */
public class BlueprintPreview implements Listener {

    /** How far a teleport may move the player before the preview is removed, in blocks. */
    private static final double KEEP_DISTANCE = 24;
    /** The coarsest resolution tried: this many blocks along each side become one. */
    private static final int MAX_FACTOR = 16;

    private static final String REF = "commands.island.create.preview.";

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    /** One player's preview. */
    private record Session(List<Entity> entities, Location centre, BukkitTask timeout) {
        void close() {
            timeout.cancel();
            entities.forEach(Entity::remove);
        }
    }

    /**
     * The blocks to draw, in cells.
     * @param cells what to draw, keyed by cell coordinate
     * @param factor how many blueprint blocks along each side make one cell
     */
    record Plan<T>(Map<Vector, T> cells, int factor) {
    }

    /**
     * @param plugin plugin
     * @param bundle the bundle
     * @return true if previews are switched on and this bundle may be previewed
     */
    public static boolean isAllowed(@NonNull BentoBox plugin, @NonNull BlueprintBundle bundle) {
        return plugin.getSettings().isBlueprintPreview() && !bundle.isPreviewHidden();
    }

    /**
     * Shows the player a miniature of the bundle's overworld blueprint, in front of them.
     *
     * @param plugin plugin
     * @param user the player
     * @param addon the game mode the bundle belongs to
     * @param bundle the bundle to preview
     * @return true if a preview is now showing
     */
    public static boolean show(@NonNull BentoBox plugin, @NonNull User user, @NonNull GameModeAddon addon,
            @NonNull BlueprintBundle bundle) {
        if (!user.isPlayer() || !isAllowed(plugin, bundle)) {
            return false;
        }
        Blueprint bp = plugin.getBlueprintsManager().getBlueprints(addon)
                .get(bundle.getBlueprint(World.Environment.NORMAL));
        Map<Vector, BlockData> blocks = bp == null ? Map.of() : parse(bp);
        if (blocks.isEmpty()) {
            user.sendMessage(REF + "nothing");
            return false;
        }
        Plan<BlockData> plan = plan(blocks, plugin.getSettings().getBlueprintPreviewMaxBlocks(),
                data -> data.getMaterial().isOccluding());
        clear(user.getUniqueId());
        Player player = user.getPlayer();
        List<Entity> entities = new ArrayList<>(plan.cells().size());
        Location centre = spawn(plugin, player, plan, entities);
        int seconds = Math.max(5, plugin.getSettings().getBlueprintPreviewDuration());
        BukkitTask timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> clear(player.getUniqueId()),
                seconds * 20L);
        SESSIONS.put(player.getUniqueId(), new Session(entities, centre, timeout));
        user.sendMessage(REF + "showing", TextVariables.NAME, bundle.getDisplayName(), TextVariables.NUMBER,
                String.valueOf(seconds));
        return true;
    }

    /**
     * Removes a player's preview, if they have one.
     * @param uuid the player
     */
    public static void clear(@NonNull UUID uuid) {
        Session session = SESSIONS.remove(uuid);
        if (session != null) {
            session.close();
        }
    }

    /** Removes every preview. Called when the plugin is disabled. */
    public static void clearAll() {
        List.copyOf(SESSIONS.keySet()).forEach(BlueprintPreview::clear);
    }

    // ---------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------

    /**
     * Spawns the display entities and adds them to {@code entities}.
     * @return the centre of the miniature
     */
    private static Location spawn(BentoBox plugin, Player player, Plan<BlockData> plan, List<Entity> entities) {
        int[] min = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE };
        int[] max = { Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
        for (Vector v : plan.cells().keySet()) {
            int[] c = { v.getBlockX(), v.getBlockY(), v.getBlockZ() };
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], c[i]);
                max[i] = Math.max(max[i], c[i]);
            }
        }
        int[] cells = { max[0] - min[0] + 1, max[1] - min[1] + 1, max[2] - min[2] + 1 };
        double size = Math.max(1, plugin.getSettings().getBlueprintPreviewSize());
        int longest = Math.max(cells[0], Math.max(cells[1], cells[2])) * plan.factor();
        // Never draw larger than life
        float cell = (float) (Math.min(1D, size / longest) * plan.factor());

        // Put it in front of the player, far enough away that all of it is in view
        Vector facing = player.getLocation().getDirection().setY(0);
        if (facing.lengthSquared() < 1.0E-6) {
            facing = new Vector(0, 0, 1);
        }
        double width = Math.max(cells[0], cells[2]) * cell;
        Location centre = player.getLocation().add(facing.normalize().multiply(width / 2 + 2.5));
        centre.setYaw(0);
        centre.setPitch(0);
        Location origin = centre.clone().add(-cells[0] * cell / 2D, 0.5, -cells[2] * cell / 2D);

        Transformation scale = new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(cell, cell, cell),
                new AxisAngle4f());
        plan.cells().forEach((v, data) -> {
            Location at = origin.clone().add((v.getBlockX() - min[0]) * cell, (v.getBlockY() - min[1]) * cell,
                    (v.getBlockZ() - min[2]) * cell);
            BlockDisplay display = at.getWorld().spawn(at, BlockDisplay.class, e -> {
                e.setPersistent(false);
                e.setVisibleByDefault(false);
                e.setBlock(data);
                e.setTransformation(scale);
                // Fully lit, so it can be seen at night or underground
                e.setBrightness(new Display.Brightness(15, 15));
            });
            player.showEntity(plugin, display);
            entities.add(display);
        });
        return centre;
    }

    // ---------------------------------------------------------------------
    // Working out what to draw
    // ---------------------------------------------------------------------

    /**
     * Reads the blocks of a blueprint. Air and blocks this server cannot build are left out.
     */
    static Map<Vector, BlockData> parse(Blueprint bp) {
        Map<Vector, BlockData> result = new HashMap<>();
        addAll(result, bp.getBlocks());
        addAll(result, bp.getAttached());
        return result;
    }

    private static void addAll(Map<Vector, BlockData> result, @Nullable Map<Vector, BlueprintBlock> blocks) {
        if (blocks == null) {
            return;
        }
        blocks.forEach((v, block) -> {
            try {
                BlockData data = Bukkit.createBlockData(block.getBlockData());
                if (!data.getMaterial().isAir()) {
                    result.put(new Vector(v.getBlockX(), v.getBlockY(), v.getBlockZ()), data);
                }
            } catch (IllegalArgumentException e) {
                // A block from another server version or a missing plugin: leave it out
            }
        });
    }

    /**
     * Picks the finest resolution at which no more than {@code maxBlocks} blocks have to be drawn.
     *
     * @param blocks the blueprint's blocks
     * @param maxBlocks the most blocks to draw
     * @param occludes whether a block fully hides what is behind it
     * @return what to draw
     */
    static <T> Plan<T> plan(Map<Vector, T> blocks, int maxBlocks, Predicate<T> occludes) {
        int factor = 1;
        Map<Vector, T> shown = surface(blocks, occludes);
        while (shown.size() > maxBlocks && factor < MAX_FACTOR) {
            factor++;
            shown = surface(coarsen(blocks, factor), occludes);
        }
        return new Plan<>(shown, factor);
    }

    /**
     * Merges every {@code factor} x {@code factor} x {@code factor} group of blocks into one
     * cell. The highest block in a group stands for the group, as that is the one seen from above.
     */
    static <T> Map<Vector, T> coarsen(Map<Vector, T> blocks, int factor) {
        Map<Vector, T> cells = new HashMap<>();
        Map<Vector, Integer> heights = new HashMap<>();
        blocks.forEach((v, block) -> {
            Vector cell = new Vector(Math.floorDiv(v.getBlockX(), factor), Math.floorDiv(v.getBlockY(), factor),
                    Math.floorDiv(v.getBlockZ(), factor));
            Integer height = heights.get(cell);
            if (height == null || v.getBlockY() > height) {
                heights.put(cell, v.getBlockY());
                cells.put(cell, block);
            }
        });
        return cells;
    }

    /**
     * Keeps the blocks that can be seen: those with at least one side that is not covered by a
     * block that hides what is behind it.
     */
    static <T> Map<Vector, T> surface(Map<Vector, T> blocks, Predicate<T> occludes) {
        int[][] sides = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
        Map<Vector, T> result = new HashMap<>();
        blocks.forEach((v, block) -> {
            for (int[] side : sides) {
                T neighbour = blocks
                        .get(new Vector(v.getBlockX() + side[0], v.getBlockY() + side[1], v.getBlockZ() + side[2]));
                if (neighbour == null || !occludes.test(neighbour)) {
                    result.put(v, block);
                    return;
                }
            }
        });
        return result;
    }

    // ---------------------------------------------------------------------
    // Cleaning up
    // ---------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        clear(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        clear(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Session session = SESSIONS.get(e.getPlayer().getUniqueId());
        if (session != null && (!session.centre().getWorld().equals(e.getTo().getWorld())
                || session.centre().distanceSquared(e.getTo()) > KEEP_DISTANCE * KEEP_DISTANCE)) {
            clear(e.getPlayer().getUniqueId());
        }
    }
}
