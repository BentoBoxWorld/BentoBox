package world.bentobox.bentobox.blueprints;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.math.Position;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.util.DefaultPasteUtil;
import world.bentobox.bentobox.util.Util;

/**
 * Lets a player look at a blueprint before choosing it.
 * <p>
 * The player is moved to an empty world kept for previews, put in spectator mode and kept
 * inside a box around the blueprint. The blueprint's blocks are sent to that player only: they
 * are never placed in the world, so nothing has to be cleaned up and several players can preview
 * different blueprints in the same spot without seeing each other's. When the preview ends the
 * player is put back where they were, in the game mode they had.
 *
 * @since 3.25.0
 */
public class BlueprintPreview implements Listener {

    /** Name of the empty world previews are shown in. */
    public static final String WORLD_NAME = "bentobox_preview";

    /** Where a blueprint's origin goes in the preview world. */
    private static final int ORIGIN_Y = 100;
    /** Least room left around a blueprint for the player to fly in, in blocks. */
    private static final int MIN_MARGIN = 12;
    /** Most room left around a blueprint, so its chunks stay within the player's view distance. */
    private static final int MAX_MARGIN = 32;

    private static final String REF = "commands.island.create.preview.";

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    /** One player's preview. */
    private static final class Session {
        /** Where to put the player back, and in which game mode. */
        private final Location back;
        private final GameMode gameMode;
        /** The blueprint's blocks, grouped by the chunk they are in. */
        private final Map<Long, Map<Position, BlockData>> chunks;
        /** The space the player may fly in. */
        private final BoundingBox bounds;
        private BukkitTask timeout;
        /** True while this class is moving the player, so its own teleports are let through. */
        private boolean moving;

        Session(Location back, GameMode gameMode, Map<Long, Map<Position, BlockData>> chunks, BoundingBox bounds) {
            this.back = back;
            this.gameMode = gameMode;
            this.chunks = chunks;
            this.bounds = bounds;
        }
    }

    private final BentoBox plugin;
    private final NamespacedKey returnKey;

    public BlueprintPreview(BentoBox plugin) {
        this.plugin = plugin;
        this.returnKey = returnKey(plugin);
    }

    private static NamespacedKey returnKey(BentoBox plugin) {
        return new NamespacedKey(plugin, "preview-return");
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
     * @param uuid a player
     * @return true if the player is looking at a preview
     */
    public static boolean isPreviewing(@NonNull UUID uuid) {
        return SESSIONS.containsKey(uuid);
    }

    /**
     * Takes the player to a preview of the bundle's overworld blueprint.
     *
     * @param plugin plugin
     * @param user the player
     * @param addon the game mode the bundle belongs to
     * @param bundle the bundle to preview
     * @param chooseCommand the command that picks this bundle, starting with a slash
     * @param listCommand the command that shows the bundles again, starting with a slash
     * @return true if the preview was started
     */
    public static boolean show(@NonNull BentoBox plugin, @NonNull User user, @NonNull GameModeAddon addon,
            @NonNull BlueprintBundle bundle, @NonNull String chooseCommand, @NonNull String listCommand) {
        if (!user.isPlayer() || !isAllowed(plugin, bundle)) {
            return false;
        }
        Player player = user.getPlayer();
        end(plugin, player, true);
        Blueprint bp = plugin.getBlueprintsManager().getBlueprints(addon)
                .get(bundle.getBlueprint(World.Environment.NORMAL));
        Map<Vector, BlockData> blocks = bp == null ? Map.of() : parse(bp);
        World world = blocks.isEmpty() ? null : getWorld();
        if (world == null) {
            user.sendMessage(REF + "nothing");
            return false;
        }
        BoundingBox model = bounds(blocks.keySet());
        // Lift the blueprint if it reaches below the bottom of the world
        int lift = Math.max(ORIGIN_Y, world.getMinHeight() + 8 - (int) model.getMinY());
        model.shift(0, lift, 0);
        Map<Long, Map<Position, BlockData>> chunks = new HashMap<>();
        blocks.forEach((v, data) -> chunks
                .computeIfAbsent(Chunk.getChunkKey(v.getBlockX() >> 4, v.getBlockZ() >> 4), k -> new HashMap<>())
                .put(Position.block(v.getBlockX(), v.getBlockY() + lift, v.getBlockZ()), data));

        double longest = Math.max(model.getWidthX(), Math.max(model.getHeight(), model.getWidthZ()));
        double margin = Math.clamp(longest, MIN_MARGIN, MAX_MARGIN);
        BoundingBox bounds = model.clone().expand(margin);

        Session session = new Session(player.getLocation(), player.getGameMode(), chunks, bounds);
        remember(plugin, player, session);
        SESSIONS.put(player.getUniqueId(), session);

        // Start outside the blueprint, a little above its middle, looking at it
        Vector centre = model.getCenter();
        Location start = new Location(world, centre.getX(), centre.getY() + model.getHeight() / 4 + 2,
                model.getMinZ() - margin * 0.75);
        start.setDirection(centre.clone().subtract(start.toVector()));

        session.moving = true;
        player.teleportAsync(start).thenAccept(done -> {
            session.moving = false;
            if (!Boolean.TRUE.equals(done) || SESSIONS.get(player.getUniqueId()) != session) {
                end(plugin, player, false);
                return;
            }
            begin(plugin, user, bundle, session, centre, chooseCommand, listCommand);
        });
        return true;
    }

    private static void begin(BentoBox plugin, User user, BlueprintBundle bundle, Session session, Vector centre,
            String chooseCommand, String listCommand) {
        Player player = user.getPlayer();
        player.setGameMode(GameMode.SPECTATOR);
        // Always midday and clear, whatever the preview world is doing
        player.setPlayerTime(6000, false);
        player.setPlayerWeather(WeatherType.CLEAR);
        // Show where the edge is. Spectators can cross a world border, so the move listener enforces it.
        WorldBorder border = Bukkit.createWorldBorder();
        border.setCenter(centre.getX(), centre.getZ());
        border.setSize(Math.max(session.bounds.getWidthX(), session.bounds.getWidthZ()));
        border.setWarningDistance(0);
        player.setWorldBorder(border);
        // Previewing players share the same space, so keep them out of each other's view
        SESSIONS.keySet().stream().map(Bukkit::getPlayer).filter(p -> p != null && !p.equals(player)).forEach(p -> {
            p.hidePlayer(plugin, player);
            player.hidePlayer(plugin, p);
        });
        // Chunks are sent their blocks as the player receives them. Send everything again a little
        // later, in case a chunk arrived before this session was ready for it.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (SESSIONS.get(player.getUniqueId()) == session) {
                session.chunks.values().forEach(player::sendMultiBlockChange);
            }
        }, 20L);

        int seconds = Math.max(10, plugin.getSettings().getBlueprintPreviewDuration());
        session.timeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (SESSIONS.get(player.getUniqueId()) == session) {
                end(plugin, player, true);
                user.sendMessage(REF + "ended");
            }
        }, seconds * 20L);

        user.sendMessage(REF + "showing", TextVariables.NAME, bundle.getDisplayName(), TextVariables.NUMBER,
                String.valueOf(seconds));
        player.sendMessage(Component.text()
                .append(button(user, "choose", chooseCommand + " " + bundle.getUniqueId())).append(Component.space())
                .append(button(user, "back", listCommand)).build());
    }

    private static Component button(User user, String key, String command) {
        return Util.parseMiniMessageOrLegacy(user.getTranslation(REF + key)).clickEvent(ClickEvent.runCommand(command));
    }

    /**
     * Ends a player's preview, if they are in one.
     *
     * @param plugin plugin
     * @param player the player
     * @param goBack true to put the player back where they were; false if they are already going
     * somewhere else
     */
    public static void end(@NonNull BentoBox plugin, @NonNull Player player, boolean goBack) {
        Session session = SESSIONS.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.timeout != null) {
            session.timeout.cancel();
        }
        player.resetPlayerTime();
        player.resetPlayerWeather();
        player.setWorldBorder(null);
        SESSIONS.keySet().stream().map(Bukkit::getPlayer).filter(p -> p != null).forEach(p -> {
            p.showPlayer(plugin, player);
            player.showPlayer(plugin, p);
        });
        if (goBack) {
            session.moving = true;
            // Not async: whatever ended the preview, such as a command, runs straight after this
            player.teleport(session.back.getWorld() == null ? Bukkit.getWorlds().get(0).getSpawnLocation()
                    : session.back);
        }
        player.setGameMode(session.gameMode);
        player.getPersistentDataContainer().remove(returnKey(plugin));
    }

    /**
     * Ends every preview. Called when the plugin is disabled.
     * @param plugin plugin
     */
    public static void endAll(@NonNull BentoBox plugin) {
        List.copyOf(SESSIONS.keySet()).stream().map(Bukkit::getPlayer).filter(p -> p != null)
                .forEach(p -> end(plugin, p, true));
        SESSIONS.clear();
    }

    // ---------------------------------------------------------------------
    // The preview world
    // ---------------------------------------------------------------------

    /** Generates nothing: every chunk is empty. */
    private static final class EmptyWorld extends ChunkGenerator {
    }

    /**
     * @return the preview world, made the first time it is needed, or null if it cannot be made
     */
    private static @Nullable World getWorld() {
        World world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            world = WorldCreator.name(WORLD_NAME).generator(new EmptyWorld()).generateStructures(false)
                    .createWorld();
            if (world != null) {
                // Nothing is ever built here, so there is nothing to save or to spawn
                world.setAutoSave(false);
                world.setSpawnFlags(false, false);
            }
        }
        return world;
    }

    // ---------------------------------------------------------------------
    // Working out what to show
    // ---------------------------------------------------------------------

    /**
     * Reads every block of a blueprint, including the block it is filled with. Air and blocks
     * this server cannot build are left out.
     */
    static Map<Vector, BlockData> parse(Blueprint bp) {
        Map<Vector, BlockData> result = new HashMap<>();
        Iterator<Entry<Vector, BlueprintBlock>> fill = BlueprintFill.fillIterator(bp);
        if (fill.hasNext()) {
            BlockData data = toData(bp.getFill());
            if (data != null) {
                fill.forEachRemaining(e -> result.put(blockVector(e.getKey()), data));
            }
        }
        addAll(result, bp.getBlocks());
        addAll(result, bp.getAttached());
        return result;
    }

    private static void addAll(Map<Vector, BlockData> result, @Nullable Map<Vector, BlueprintBlock> blocks) {
        if (blocks != null) {
            blocks.forEach((v, block) -> {
                BlockData data = toData(block);
                if (data != null) {
                    result.put(blockVector(v), data);
                }
            });
        }
    }

    private static Vector blockVector(Vector v) {
        return new Vector(v.getBlockX(), v.getBlockY(), v.getBlockZ());
    }

    private static @Nullable BlockData toData(BlueprintBlock block) {
        try {
            BlockData data = DefaultPasteUtil.createBlockData(block);
            return data == null || data.getMaterial().isAir() ? null : data;
        } catch (RuntimeException e) {
            // A block from another server version or a missing plugin: leave it out
            return null;
        }
    }

    /**
     * @param positions block positions; must not be empty
     * @return the box that holds every one of the blocks
     */
    static BoundingBox bounds(Iterable<Vector> positions) {
        BoundingBox box = null;
        for (Vector v : positions) {
            BoundingBox block = new BoundingBox(v.getBlockX(), v.getBlockY(), v.getBlockZ(), v.getBlockX() + 1D,
                    v.getBlockY() + 1D, v.getBlockZ() + 1D);
            box = box == null ? block : box.union(block);
        }
        return box;
    }

    /**
     * @param to where a player is trying to go
     * @param bounds the space they may be in
     * @return the nearest place inside the bounds, or null if {@code to} is already inside
     */
    static @Nullable Location clamp(Location to, BoundingBox bounds) {
        if (bounds.contains(to.getX(), to.getY(), to.getZ())) {
            return null;
        }
        Location inside = to.clone();
        inside.setX(Math.clamp(to.getX(), bounds.getMinX() + 0.5, bounds.getMaxX() - 0.5));
        inside.setY(Math.clamp(to.getY(), bounds.getMinY() + 0.5, bounds.getMaxY() - 0.5));
        inside.setZ(Math.clamp(to.getZ(), bounds.getMinZ() + 0.5, bounds.getMaxZ() - 0.5));
        return inside;
    }

    // ---------------------------------------------------------------------
    // Keeping the player in the preview, and getting them out again
    // ---------------------------------------------------------------------

    /** Notes on the player where they came from, so they can be put back even after a crash. */
    private static void remember(BentoBox plugin, Player player, Session session) {
        Location l = session.back;
        player.getPersistentDataContainer().set(returnKey(plugin), PersistentDataType.STRING,
                String.join(";", l.getWorld().getName(), String.valueOf(l.getX()), String.valueOf(l.getY()),
                        String.valueOf(l.getZ()), String.valueOf(l.getYaw()), String.valueOf(l.getPitch()),
                        session.gameMode.name()));
    }

    /** Sends a chunk's share of the blueprint when the player is sent that chunk. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkSent(PlayerChunkLoadEvent e) {
        Session session = SESSIONS.get(e.getPlayer().getUniqueId());
        if (session != null && e.getWorld().getName().equals(WORLD_NAME)) {
            Map<Position, BlockData> blocks = session.chunks.get(e.getChunk().getChunkKey());
            if (blocks != null) {
                e.getPlayer().sendMultiBlockChange(blocks);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Session session = SESSIONS.get(e.getPlayer().getUniqueId());
        if (session != null && !session.moving && e.getTo().getWorld().getName().equals(WORLD_NAME)) {
            Location inside = clamp(e.getTo(), session.bounds);
            if (inside != null) {
                e.setTo(inside);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Session session = SESSIONS.get(e.getPlayer().getUniqueId());
        if (session == null || session.moving) {
            return;
        }
        if (e.getCause() == TeleportCause.SPECTATE) {
            // No jumping to other players through the spectator menu
            e.setCancelled(true);
        } else if (!e.getTo().getWorld().getName().equals(WORLD_NAME)) {
            // Something else is taking the player away: let it, and tidy up
            end(plugin, e.getPlayer(), false);
        }
    }

    /** Any command ends the preview first, so the command runs from where the player really is. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        end(plugin, e.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent e) {
        end(plugin, e.getPlayer(), true);
    }

    /** Puts back a player who was left in the preview world, for example by a server crash. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        String saved = player.getPersistentDataContainer().get(returnKey, PersistentDataType.STRING);
        if (saved == null && !player.getWorld().getName().equals(WORLD_NAME)) {
            return;
        }
        player.getPersistentDataContainer().remove(returnKey);
        Location back = Bukkit.getWorlds().get(0).getSpawnLocation();
        GameMode gameMode = Bukkit.getDefaultGameMode();
        if (saved != null) {
            try {
                String[] s = saved.split(";");
                World world = Bukkit.getWorld(s[0]);
                if (world != null) {
                    back = new Location(world, Double.parseDouble(s[1]), Double.parseDouble(s[2]),
                            Double.parseDouble(s[3]), Float.parseFloat(s[4]), Float.parseFloat(s[5]));
                }
                gameMode = GameMode.valueOf(s[6]);
            } catch (RuntimeException ex) {
                plugin.logWarning("Could not read where " + player.getName() + " was before a blueprint preview.");
            }
        }
        if (player.getWorld().getName().equals(WORLD_NAME)) {
            player.teleport(back);
            player.setGameMode(gameMode);
        }
    }
}
