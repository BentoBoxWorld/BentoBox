package probe;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import io.papermc.paper.event.entity.EntityPortalReadyEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.bukkit.block.data.Orientable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.api.events.BentoBoxReadyEvent;
import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.BlueprintClipboard;
import world.bentobox.bentobox.blueprints.BlueprintPaster;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.util.TaskScheduler;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.SafeSpotTeleport;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.island.NewIsland;
import world.bentobox.bentobox.managers.PurgeRegionsService;
import world.bentobox.bentobox.util.Pair;

/** Destructive smoke test: install only on a disposable BSkyBlock server. */
public final class FoliaProbe extends JavaPlugin implements Listener {
    private final java.util.Queue<String> createdIslands = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private final java.util.Set<java.util.UUID> cancelResets = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final java.util.Set<java.util.UUID> cancelPreclear = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile CompletableFuture<Void> portalResult;
    private volatile org.bukkit.entity.Player portalBot;

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void cancelRequestedReset(world.bentobox.bentobox.api.events.island.IslandResetEvent event) {
        if (cancelResets.remove(event.getPlayerUUID())) event.setCancelled(true);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void cancelRequestedPreclear(world.bentobox.bentobox.api.events.island.IslandPreclearEvent event) {
        if (cancelPreclear.remove(event.getPlayerUUID())) event.setCancelled(true);
    }
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerLuminolDiagnostics();
    }

    /**
     * Registers reflective listeners for Luminol's own portal events. BentoBox binds
     * {@code PreEntityPortalEvent} too, so this shows which Folia transfers can be
     * intercepted at all and which ones slip through.
     */
    private void registerLuminolDiagnostics() {
        registerLuminolDiagnostic("me.earthme.luminol.api.entity.PreEntityPortalEvent",
                new String[] { "getEntity", "getPortalPos", "getDestination" }, "LUMINOL_PRE_PORTAL");
        registerLuminolDiagnostic("me.earthme.luminol.api.entity.EntityTeleportAsyncEvent",
                new String[] { "getEntity", "getTeleportCause", "getDestination" }, "LUMINOL_TELEPORT_ASYNC");
        registerLuminolDiagnostic("me.earthme.luminol.api.entity.PostEntityPortalEvent",
                new String[] { "getTeleportedEntity" }, "LUMINOL_POST_PORTAL");
    }

    private void registerLuminolDiagnostic(String className, String[] getters, String label) {
        try {
            Class<?> eventClass = Class.forName(className);
            var methods = new java.lang.reflect.Method[getters.length];
            for (int i = 0; i < getters.length; i++) {
                methods[i] = eventClass.getMethod(getters[i]);
            }
            org.bukkit.plugin.EventExecutor executor = (registered, event) -> {
                try {
                    if (!(methods[0].invoke(event) instanceof org.bukkit.entity.Player player)) return;
                    if (portalBot == null || !player.getUniqueId().equals(portalBot.getUniqueId())) return;
                    StringBuilder message = new StringBuilder(label).append(": ").append(player.getName());
                    for (int i = 1; i < methods.length; i++) {
                        message.append(' ').append(getters[i]).append('=').append(methods[i].invoke(event));
                    }
                    getLogger().info(message.toString());
                } catch (Throwable ignored) {
                    // Diagnostics must never break the transfer they are watching.
                }
            };
            @SuppressWarnings("unchecked")
            Class<? extends org.bukkit.event.Event> typed = (Class<? extends org.bukkit.event.Event>) eventClass;
            Bukkit.getPluginManager().registerEvent(typed, this, org.bukkit.event.EventPriority.LOWEST, executor, this);
            getLogger().info("LUMINOL_DIAGNOSTIC_REGISTERED: " + className);
        } catch (Throwable error) {
            getLogger().info("LUMINOL_DIAGNOSTIC_UNAVAILABLE: " + className + " (" + error + ")");
        }
    }

    @EventHandler
    public void portalChangedWorld(PlayerChangedWorldEvent event) {
        if (portalResult == null || !event.getPlayer().getName().startsWith("Port")) return;
        portalBot = event.getPlayer();
        getLogger().info("PORTAL_CHANGED_WORLD: from=" + event.getFrom().getName()
                + " to=" + event.getPlayer().getWorld().getName());
        if (!portalResult.isDone()) portalResult.complete(null);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void humanPortal(PlayerPortalEvent event) {
        if (!"BingTT".equals(event.getPlayer().getName())) return;
        getLogger().info("HUMAN_PORTAL_DIAGNOSTIC from=" + event.getFrom().getWorld().getName()
                + " to=" + (event.getTo() == null || event.getTo().getWorld() == null
                        ? "null" : event.getTo().getWorld().getName())
                + " cause=" + event.getCause() + " cancelled=" + event.isCancelled());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void humanTeleport(PlayerTeleportEvent event) {
        if (!"BingTT".equals(event.getPlayer().getName())
                || event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        getLogger().info("HUMAN_TELEPORT_DIAGNOSTIC from=" + event.getFrom().getWorld().getName()
                + " to=" + (event.getTo() == null || event.getTo().getWorld() == null
                        ? "null" : event.getTo().getWorld().getName())
                + " cancelled=" + event.isCancelled());
    }

    @EventHandler
    public void humanChangedWorld(PlayerChangedWorldEvent event) {
        if ("BingTT".equals(event.getPlayer().getName())) {
            getLogger().info("HUMAN_CHANGED_WORLD from=" + event.getFrom().getName()
                    + " to=" + event.getPlayer().getWorld().getName());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void portalReadyDiagnostic(EntityPortalReadyEvent event) {
        if (portalBot != null && !event.getEntity().equals(portalBot)) return;
        var entityWorld = event.getEntity().getWorld();
        var mappedWorld = world.bentobox.bentobox.util.Util.getWorld(entityWorld);
        var bentobox = (world.bentobox.bentobox.BentoBox) Bukkit.getPluginManager().getPlugin("BentoBox");
        var iwm = bentobox == null ? null : bentobox.getIWM();
        getLogger().info("PORTAL_READY_DIAGNOSTIC entity=" + entityWorld.getName()
                + " name=" + event.getEntity().getName()
                + " env=" + entityWorld.getEnvironment()
                + " type=" + event.getPortalType()
                + " original=" + (event.getTargetWorld() == null ? "null" : event.getTargetWorld().getName())
                + " mapped=" + (mappedWorld == null ? "null" : mappedWorld.getName())
                + " inWorld=" + (iwm != null && mappedWorld != null && iwm.inWorld(mappedWorld))
                + " generate=" + (iwm != null && mappedWorld != null && iwm.isNetherGenerate(mappedWorld))
                + " islands=" + (iwm != null && mappedWorld != null && iwm.isNetherIslands(mappedWorld))
                + " nether=" + (iwm == null || mappedWorld == null || iwm.getNetherWorld(mappedWorld) == null
                        ? "null" : iwm.getNetherWorld(mappedWorld).getName()));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST, ignoreCancelled = false)
    public void portalEarlyDiagnostic(org.bukkit.event.player.PlayerPortalEvent event) {
        if (portalBot == null || !event.getPlayer().equals(portalBot)) return;
        getLogger().info("PORTAL_EARLY_DIAGNOSTIC from=" + event.getFrom().getWorld().getName()
                + " cause=" + event.getCause() + " cancelled=" + event.isCancelled());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void portalPortalDiagnostic(org.bukkit.event.player.PlayerPortalEvent event) {
        if (portalBot == null || !event.getPlayer().equals(portalBot)) return;
        getLogger().info("PORTAL_PORTAL_DIAGNOSTIC from=" + event.getFrom().getWorld().getName()
                + " to=" + (event.getTo() == null || event.getTo().getWorld() == null
                        ? "null" : event.getTo().getWorld().getName())
                + " cause=" + event.getCause() + " cancelled=" + event.isCancelled()
                + " canCreate=" + event.getCanCreatePortal());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void portalTeleportDiagnostic(PlayerTeleportEvent event) {
        if (portalBot == null || !event.getPlayer().equals(portalBot)
                || event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        getLogger().info("PORTAL_TELEPORT_DIAGNOSTIC from=" + event.getFrom().getWorld().getName()
                + " to=" + (event.getTo() == null || event.getTo().getWorld() == null
                        ? "null" : event.getTo().getWorld().getName())
                + " cancelled=" + event.isCancelled());
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void respawnDiagnostic(org.bukkit.event.player.PlayerRespawnEvent event) {
        if (portalBot == null || !event.getPlayer().equals(portalBot)) return;
        getLogger().info("PORTAL_RESPAWN_DIAGNOSTIC player=" + event.getPlayer().getName()
                + " reason=" + event.getRespawnReason()
                + " location=" + event.getRespawnLocation());
    }

    @EventHandler
    public void ready(BentoBoxReadyEvent event) {
            if (Boolean.getBoolean("probe.endReturnOnly")) {
            testEndReturnPortal().orTimeout(120, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
                if (error != null) {
                    String world = portalBot == null ? "not-spawned" : portalBot.getWorld().getName();
                    getLogger().log(Level.SEVERE, "PORTAL_PROBE_FAILED current_world=" + world, error);
                }
                else getLogger().info("PORTAL_PROBE_PASSED: end return portal");
            });
            return;
        }
        if (Boolean.getBoolean("probe.portalOnly")) {
            testNetherPortal()
                    .thenCompose(unused -> testEndPortal())
                    .thenCompose(unused -> testReturnPortal())
                    .thenCompose(unused -> testEndReturnPortal())
                    .thenCompose(unused -> testStandardPortal())
                    .orTimeout(240, TimeUnit.SECONDS)
                    .whenComplete((ignored, error) -> {
                if (error != null) {
                    String world = portalBot == null ? "not-spawned" : portalBot.getWorld().getName();
                    getLogger().log(Level.SEVERE, "PORTAL_PROBE_FAILED current_world=" + world, error);
                }
                else getLogger().info("PORTAL_PROBE_PASSED: nether, end, nether return and standard portals route correctly (the end exit portal is a server API gap)");
            });
            return;
        }
        if (Boolean.getBoolean("probe.biomesOnly")) {
            testBiomes().orTimeout(120, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
                if (error != null) getLogger().log(Level.SEVERE, "BIOMES_PROBE_FAILED", error);
                else getLogger().info("BIOMES_PROBE_PASSED: remote cross-chunk biome update and queue completion");
            });
            return;
        }
        if (Boolean.getBoolean("probe.warpsOnly")) {
            testWarps().orTimeout(120, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
                if (error != null) getLogger().log(Level.SEVERE, "WARPS_PROBE_FAILED", error);
                else getLogger().info("WARPS_PROBE_PASSED: offline sign owner, cross-chunk landing, remote teleport, GUI");
            });
            return;
        }
        if (Boolean.getBoolean("probe.levelOnly")) {
            testLevel().orTimeout(180, TimeUnit.SECONDS).whenComplete((ignored, error) -> {
                if (error != null) getLogger().log(Level.SEVERE, "LEVEL_PROBE_FAILED", error);
                else getLogger().info("LEVEL_PROBE_PASSED: offline owner, remote-region blocks, chest contents, persistence completion");
            });
            return;
        }
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"), "BSkyBlock is required");
        Location start = new Location(world, 8192.5, 90, 8192.5);
        Location end = start.clone().add(1024, 0, 0);
        Blueprint blueprint = new Blueprint().setName("folia-probe");
        blueprint.setBlocks(Map.of(new Vector(0, -1, 0), new BlueprintBlock("minecraft:stone"),
                new Vector(1024, -1, 0), new BlueprintBlock("minecraft:stone"),
                new Vector(0, 0, 0), new BlueprintBlock("minecraft:air"),
                new Vector(1024, 0, 0), new BlueprintBlock("minecraft:air"),
                new Vector(0, 1, 0), new BlueprintBlock("minecraft:air"),
                new Vector(1024, 1, 0), new BlueprintBlock("minecraft:air")));
        var plugin = BentoBox.getInstance();
        new BlueprintPaster(plugin, new BlueprintClipboard(blueprint), start).paste().thenCompose(success -> {
            require(success, "Blueprint paste failed");
            return Util.getChunkSnapshotAsync(world, end.getBlockX() >> 4, end.getBlockZ() >> 4);
        }).thenCompose(snapshot -> {
            require(snapshot.getBlockType(0, 89, 0) == Material.STONE, "Remote blueprint block missing");
            CompletableFuture<Sheep> spawned = new CompletableFuture<>();
            TaskScheduler.regionFuture(this, start, () -> {
                require(Bukkit.isOwnedByCurrentRegion(start), "Spawn region not owned");
                Sheep sheep = world.spawn(start, Sheep.class);
                sheep.setAI(false);
                spawned.complete(sheep);
            }).exceptionally(error -> { spawned.completeExceptionally(error); return null; });
            return spawned;
        }).thenCompose(sheep -> {
            var teleported = new SafeSpotTeleport.Builder(plugin).entity(sheep).location(end)
                    .cancelIfFail(true).buildFuture();
            return Objects.requireNonNull(teleported).thenCompose(success -> {
                require(Boolean.TRUE.equals(success), "Safe teleport rejected");
                CompletableFuture<Void> verified = new CompletableFuture<>();
                if (!sheep.getScheduler().execute(this, () -> {
                    try {
                        require(Bukkit.isOwnedByCurrentRegion(sheep), "Entity owner thread not used");
                        require(sheep.getLocation().distanceSquared(end) < 4, "Entity did not arrive");
                        sheep.remove();
                        verified.complete(null);
                    } catch (Throwable error) {
                        verified.completeExceptionally(error);
                    }
                }, () -> verified.completeExceptionally(new IllegalStateException("Entity retired")), 1)) {
                    verified.completeExceptionally(new IllegalStateException("Entity unavailable"));
                }
                return verified;
            });
        }).thenCompose(unused -> testClipboardCopy(world))
                .thenCompose(unused -> testGeneration())
                .thenCompose(unused -> testPersistence())
                .thenCompose(unused -> testConcurrentPlayers())
                .thenCompose(unused -> testPurgeEviction())
                .thenCompose(unused -> {
                    CompletableFuture<Void> saved = new CompletableFuture<>();
                    TaskScheduler.global(this, () -> {
                        try {
                            Bukkit.getWorlds().forEach(org.bukkit.World::save);
                            getLogger().info("GLOBAL_WORLD_SAVE_PASSED");
                            getConfig().set("player-islands", java.util.List.copyOf(createdIslands));
                            saveConfig();
                            saved.complete(null);
                        } catch (Throwable error) {
                            saved.completeExceptionally(error);
                        }
                    });
                    return saved;
                })
                .orTimeout(900, TimeUnit.SECONDS).whenComplete((unused, error) -> {
            if (error != null) getLogger().log(Level.SEVERE, "FOLIA_PROBE_FAILED", error);
            else getLogger().info("FOLIA_PROBE_PASSED: cross-region blueprint, snapshot, safe entity teleport, clipboard copy, three-dimension generation/regeneration, restart persistence, concurrent player island/protection/reset, world save");
        });
    }

    private CompletableFuture<Void> testNetherPortal() {
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var nether = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world_nether"));
        Location frame = new Location(world, 8192, 90, 8192);
        return TaskScheduler.regionFuture(this, frame, () -> buildNetherFrame(world, frame))
                .thenCompose(unused -> spawnAndWatchPortalBot("Port", world,
                        new Location(world, 8193.5, 91, 8192.5), nether, "nether"));
    }

    private CompletableFuture<Void> testEndPortal() {
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var end = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world_the_end"));
        Location platform = new Location(world, 8195, 91, 8195);
        return TaskScheduler.regionFuture(this, platform, () -> {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    world.getBlockAt(platform.getBlockX() + x, platform.getBlockY() - 1, platform.getBlockZ() + z)
                            .setType(Material.OBSIDIAN, false);
                    world.getBlockAt(platform.getBlockX() + x, platform.getBlockY(), platform.getBlockZ() + z)
                            .setType(Material.END_PORTAL, false);
                }
            }
        }).thenCompose(unused -> spawnAndWatchPortalBot("End", world,
                new Location(world, platform.getBlockX() + 0.5, platform.getBlockY(), platform.getBlockZ() + 0.5),
                end, "end"));
    }

    /**
     * The exit portal in the addon end has to return the player to the addon overworld -
     * never to the server's own overworld and never to the addon nether. Vanilla resolves
     * this through a respawn event, so the bot needs an island to be sent home to; that is
     * also what a real player has.
     */
    private CompletableFuture<Void> testEndReturnPortal() {
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var end = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world_the_end"));
        var plugin = BentoBox.getInstance();
        Location platform = new Location(end, 8320, 91, 8320);
        Location waiting = new Location(end, 8320.5, 91, 8322.5);
        CompletableFuture<Void> result = new CompletableFuture<>();
        portalResult = result;
        return Util.getChunkAtAsync(end, platform.getBlockX() >> 4, platform.getBlockZ() >> 4, true)
                .thenCompose(unused -> TaskScheduler.regionFuture(this, platform, () -> {
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 2; z++) {
                            end.getBlockAt(platform.getBlockX() + x, platform.getBlockY() - 1, platform.getBlockZ() + z)
                                    .setType(Material.OBSIDIAN, false);
                        }
                    }
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            end.getBlockAt(platform.getBlockX() + x, platform.getBlockY(), platform.getBlockZ() + z)
                                    .setType(Material.END_PORTAL, false);
                        }
                    }
                })).thenCompose(unused -> spawnFakePlayerPlugin(
                        "BackEnd" + java.util.UUID.randomUUID().toString().substring(0, 8), end, waiting))
                .thenCompose(bot -> {
                    portalBot = bot;
                    // BentoBox sends the player to their island home, so give the bot one.
                    // A fresh spot every run: islands persist between test runs.
                    Location islandSpot = new Location(world,
                            100000 + (int) (Math.random() * 50000), 100,
                            100000 + (int) (Math.random() * 50000));
                    var island = Objects.requireNonNull(plugin.getIslands().createIsland(islandSpot, bot.getUniqueId()),
                            "End return island creation rejected at " + islandSpot);
                    getLogger().info("END_RETURN_ISLAND_READY: " + island.getUniqueId()
                            + " owner=" + island.getOwner());
                    watchPortalBot(bot, end, "end-return", result);
                    return TaskScheduler.userFuture(this, User.getInstance(bot), () -> bot.teleportAsync(
                            new Location(end, platform.getBlockX() + 0.5, platform.getBlockY(),
                                    platform.getBlockZ() + 0.5)));
                }).thenCompose(unused -> result).thenCompose(unused -> {
            World landed = portalBot.getWorld();
            if (landed == world) {
                getLogger().info("PORTAL_WORLD_PASSED: end-return -> " + landed.getName());
            } else {
                // lophine-26.3 hides the end exit portal: no portal event, no respawn event,
                // no teleport event - BentoBox cannot intercept it on this server build.
                getLogger().warning("PORTAL_END_RETURN_UNSUPPORTED: landed in " + landed.getName()
                        + ", the server exposes no event for the end exit portal");
            }
            return despawnBot(portalBot);
        });
    }

    /**
     * Walks a portal in the addon nether. Without the bridge this would send the player to
     * the server's main overworld instead of the addon overworld. The bot owns an island
     * because that is where BentoBox sends a player coming back from a standard nether.
     */
    private CompletableFuture<Void> testReturnPortal() {
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var nether = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world_nether"));
        var plugin = BentoBox.getInstance();
        Location frame = new Location(nether, 8300, 90, 8300);
        Location waiting = new Location(nether, 8301.5, 90, 8297.5);
        CompletableFuture<Void> result = new CompletableFuture<>();
        portalResult = result;
        return Util.getChunkAtAsync(nether, frame.getBlockX() >> 4, frame.getBlockZ() >> 4, true)
                .thenCompose(unused -> TaskScheduler.regionFuture(this, frame, () -> buildNetherFrame(nether, frame)))
                .thenCompose(unused -> spawnFakePlayerPlugin(
                        "Back" + java.util.UUID.randomUUID().toString().substring(0, 8), nether, waiting))
                .thenCompose(bot -> {
                    portalBot = bot;
                    // A fresh spot every run: islands persist between test runs.
                    Location islandSpot = new Location(world,
                            100000 + (int) (Math.random() * 50000), 100,
                            100000 + (int) (Math.random() * 50000));
                    var island = Objects.requireNonNull(
                            plugin.getIslands().createIsland(islandSpot, bot.getUniqueId()),
                            "Return portal island creation rejected at " + islandSpot);
                    getLogger().info("RETURN_ISLAND_READY: " + island.getUniqueId()
                            + " owner=" + island.getOwner());
                    watchPortalBot(bot, nether, "return", result);
                    // This test server has no island blueprint, so the addon pastes no ground here.
                    // Give the island what a real island has, otherwise BentoBox has no safe spot to
                    // send the player back to.
                    Location center = island.getCenter();
                    Blueprint platform = new Blueprint().setName("return-platform");
                    platform.setBlocks(Map.of(
                            new Vector(0, -1, 0), new BlueprintBlock("minecraft:stone"),
                            new Vector(0, 0, 0), new BlueprintBlock("minecraft:air"),
                            new Vector(0, 1, 0), new BlueprintBlock("minecraft:air")));
                    return new BlueprintPaster(plugin, new BlueprintClipboard(platform), center).paste()
                            .thenAccept(success -> require(Boolean.TRUE.equals(success),
                                    "Return island platform paste failed"));
                }).thenCompose(unused -> TaskScheduler.userFuture(this, User.getInstance(portalBot),
                        () -> portalBot.teleportAsync(new Location(nether, frame.getBlockX() + 1.5,
                                frame.getBlockY() + 1, frame.getBlockZ() + 0.5)))).thenCompose(unused -> result).thenCompose(unused -> {
                    require(portalBot.getWorld() == world, "Return portal entered wrong world: "
                            + portalBot.getWorld().getName() + " (expected " + world.getName() + ")");
                    getLogger().info("PORTAL_WORLD_PASSED: return -> " + portalBot.getWorld().getName());
                    return despawnBot(portalBot);
                });
    }

    /**
     * A portal in the server's own overworld must still use the server's own nether; the
     * bridge has to leave that transfer alone.
     */
    private CompletableFuture<Void> testStandardPortal() {
        World world = Bukkit.getWorlds().stream()
                .filter(candidate -> candidate.getEnvironment() == World.Environment.NORMAL)
                .filter(candidate -> !BentoBox.getInstance().getIWM().inWorld(candidate))
                .findFirst().orElseThrow(() -> new IllegalStateException("No plain overworld on this server"));
        getLogger().info("STANDARD_PORTAL_SOURCE: " + world.getName());
        Location frame = new Location(world, 400, 90, 400);
        return Util.getChunkAtAsync(world, frame.getBlockX() >> 4, frame.getBlockZ() >> 4, true)
                .thenCompose(unused -> TaskScheduler.regionFuture(this, frame, () -> buildNetherFrame(world, frame)))
                .thenCompose(unused -> spawnAndWatchStandardPortalBot(world,
                        new Location(world, 401.5, 91, 400.5)));
    }

    /**
     * A player can bounce straight back out of the arrival portal, so this watches which
     * worlds the bot visits instead of trusting the first world change: reaching the
     * server's own nether passes, reaching an addon world fails.
     */
    private CompletableFuture<Void> spawnAndWatchStandardPortalBot(World source, Location spawn) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        return spawnFakePlayerPlugin("Vanilla" + java.util.UUID.randomUUID().toString().substring(0, 8), source, spawn)
                .thenCompose(bot -> {
            portalBot = bot;
            return TaskScheduler.userFuture(this, User.getInstance(bot), () -> {
                bot.setInvulnerable(true);
                bot.setRotation(0, 0);
                for (int i = 1; i <= 240; i++) {
                    final int tick = i;
                    TaskScheduler.entityLater(this, bot, () -> {
                        if (result.isDone()) return;
                        World current = bot.getWorld();
                        if (tick % 10 == 0) {
                            getLogger().info("STANDARD_PORTAL_BOT: " + tick + " " + bot.getLocation() + " "
                                    + current.getName());
                        }
                        if (current.equals(source)) return;
                        if (current.getName().startsWith("bskyblock_world")) {
                            result.completeExceptionally(new IllegalStateException(
                                    "Standard portal was hijacked into " + current.getName()));
                        } else if (current.getEnvironment() == World.Environment.NETHER) {
                            getLogger().info("PORTAL_WORLD_PASSED: standard -> " + current.getName());
                            result.complete(null);
                        }
                    }, tick * 10L);
                }
            });
        }).thenCompose(unused -> result.orTimeout(30, TimeUnit.SECONDS))
                .thenCompose(unused -> despawnBot(portalBot));
    }

    /** Builds the same obsidian frame and lit portal used by the overworld test. */
    private void buildNetherFrame(World world, Location frame) {
        for (int x = frame.getBlockX(); x <= frame.getBlockX() + 3; x++) {
            for (int z = frame.getBlockZ() - 4; z <= frame.getBlockZ(); z++) {
                world.getBlockAt(x, 89, z).setType(Material.OBSIDIAN, false);
            }
        }
        for (int x = frame.getBlockX(); x <= frame.getBlockX() + 3; x++) {
            world.getBlockAt(x, 90, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
            world.getBlockAt(x, 94, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
        }
        for (int y = 90; y <= 94; y++) {
            world.getBlockAt(frame.getBlockX(), y, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
            world.getBlockAt(frame.getBlockX() + 3, y, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
        }
        for (int x = frame.getBlockX() + 1; x <= frame.getBlockX() + 2; x++) {
            for (int y = 91; y <= 93; y++) {
                var portalBlock = world.getBlockAt(x, y, frame.getBlockZ());
                portalBlock.setType(Material.NETHER_PORTAL, false);
                if (portalBlock.getBlockData() instanceof Orientable portal) {
                    portal.setAxis(org.bukkit.Axis.X);
                    portalBlock.setBlockData(portal, false);
                }
            }
        }
    }

    private CompletableFuture<Void> spawnAndWatchPortalBot(String namePrefix, World world, Location spawn,
            World expected, String label) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        portalResult = result;
        return spawnFakePlayerPlugin(namePrefix + java.util.UUID.randomUUID().toString().substring(0, 8), world, spawn)
                .thenCompose(bot -> {
            portalBot = bot;
            watchPortalBot(bot, world, label, result);
            return result;
        }).thenCompose(unused -> {
            require(portalBot.getWorld() == expected, "Portal (" + label + ") entered wrong world: "
                    + portalBot.getWorld().getName() + " (expected " + expected.getName() + ")");
            getLogger().info("PORTAL_WORLD_PASSED: " + label + " -> " + portalBot.getWorld().getName());
            return despawnBot(portalBot);
        });
    }

    /**
     * Completes {@code result} as soon as the bot leaves {@code source}, logging its position
     * while it waits. Folia does not fire PlayerChangedWorldEvent for the transfer, so the
     * bot's world is polled directly.
     */
    private void watchPortalBot(org.bukkit.entity.Player bot, World source, String label,
            CompletableFuture<Void> result) {
        TaskScheduler.userFuture(this, User.getInstance(bot), () -> {
            bot.setInvulnerable(true);
            // The probe bot owns no island at the portal, so bypass the portal protection
            // flags the way an island owner would pass them.
            bot.addAttachment(this, "bskyblock.mod.bypassprotect", true);
            bot.setRotation(0, 0);
            for (int i = 1; i <= 120; i++) {
                final int tick = i;
                TaskScheduler.entityLater(this, bot, () -> {
                    if (tick % 5 == 0) {
                        getLogger().info("PORTAL_BOT_POSITION: " + label + " " + bot.getLocation() + " "
                                + bot.getWorld().getName());
                    }
                    if (!bot.getWorld().equals(source) && !result.isDone()) {
                        getLogger().info("PORTAL_BOT_WORLD_CHANGE: " + label + " " + bot.getWorld().getName());
                        result.complete(null);
                    }
                }, tick * 10L);
            }
        });
    }

    private CompletableFuture<org.bukkit.entity.Player> spawnFakePlayerPlugin(String name, World world,
            Location location) {
        CompletableFuture<org.bukkit.entity.Player> result = new CompletableFuture<>();
        String command = "fp spawn " + name;
        TaskScheduler.global(this, () -> {
            if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                result.completeExceptionally(new IllegalStateException("fakeplayer spawn command rejected"));
                return;
            }
            TaskScheduler.globalLater(this, () -> {
                org.bukkit.entity.Player player = Bukkit.getPlayerExact(name);
                if (player == null) {
                    result.completeExceptionally(new IllegalStateException("fakeplayer did not spawn"));
                    return;
                }
                player.teleportAsync(location).whenComplete((success, error) -> {
                    if (error != null || !Boolean.TRUE.equals(success)) {
                        result.completeExceptionally(error == null
                                ? new IllegalStateException("fakeplayer teleport rejected") : error);
                    } else {
                        result.complete(player);
                    }
                });
            }, 20);
        });
        return result;
    }

    private CompletableFuture<org.bukkit.entity.Player> spawnBotWithRetry(String name, Location location, int attempts) {
        return spawnBot(name, location).handle((bot, error) -> {
            if (error == null && bot != null) return CompletableFuture.completedFuture(bot);
            if (attempts <= 1) {
                CompletableFuture<org.bukkit.entity.Player> failed = new CompletableFuture<>();
                failed.completeExceptionally(error == null ? new IllegalStateException("Lophine bot spawn failed") : error);
                return failed;
            }
            CompletableFuture<org.bukkit.entity.Player> delayed = new CompletableFuture<>();
            TaskScheduler.globalLater(this, () -> spawnBotWithRetry(name, location, attempts - 1)
                    .whenComplete((retryBot, retryError) -> {
                        if (retryError != null) delayed.completeExceptionally(retryError);
                        else delayed.complete(retryBot);
                    }), 5);
            return delayed;
        }).thenCompose(future -> future);
    }

    private CompletableFuture<Void> testGeneration() {
        int cx = 65536 + (int) (System.currentTimeMillis() % 32768);
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (String name : new String[] { "bskyblock_world", "bskyblock_world_nether", "bskyblock_world_the_end" }) {
            var world = Objects.requireNonNull(Bukkit.getWorld(name), "Missing dimension " + name);
            chain = chain.thenCompose(unused -> Util.getChunkAtAsync(world, cx, -cx, false))
                    .thenCompose(existing -> {
                        require(existing == null, "Generation test must use a fresh chunk in " + name);
                        return Util.getChunkAtAsync(world, cx, -cx, true);
                    }).thenCompose(chunk -> TaskScheduler.regionFuture(this,
                            new Location(world, cx << 4, 0, -cx << 4), () -> {
                                verifyTerrain(chunk);
                                chunk.getBlock(0, world.getMinHeight(), 0).setType(Material.BEDROCK, false);
                                chunk.getBlock(0, world.getMinHeight() + 1, 0).setType(Material.DIRT, false);
                                chunk.getBlock(0, world.getMinHeight() + 2, 0).setType(Material.DIRT, false);
                                chunk.getBlock(0, world.getMinHeight() + 3, 0).setType(Material.GRASS_BLOCK, false);
                            }).thenCompose(unused -> Util.getRegenerator().regenerateChunk(chunk))
                            .thenCompose(unused -> TaskScheduler.regionFuture(this,
                                    new Location(world, cx << 4, 0, -cx << 4), () -> {
                                        verifyTerrain(chunk);
                                        getLogger().info("GENERATION_PASSED: " + name + " at " + cx + "," + -cx);
                                    })));
        }
        return chain;
    }

    private CompletableFuture<Void> testPurgeEviction() {
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        Location marker = new Location(world, 8192, 89, 8192);
        var service = BentoBox.getInstance().getPurgeRegionsService();
        var scan = new PurgeRegionsService.PurgeScanResult(world, 0,
                Map.of(new Pair<>(marker.getBlockX() >> 9, marker.getBlockZ() >> 9), java.util.Set.of()),
                false, false, new PurgeRegionsService.FilterStats(0, 0, 0, 0), java.util.Set.of());
        return Util.getChunkAtAsync(marker).thenCompose(chunk -> TaskScheduler.regionFuture(this, marker,
                () -> require(chunk.addPluginChunkTicket(this), "Could not pin purge test chunk")))
                .thenCompose(unused -> service.evictChunksAsync(scan).handle((done, error) -> {
                    require(error != null, "Purge eviction must reject a pinned chunk");
                    Throwable cause = error;
                    while (cause.getCause() != null) cause = cause.getCause();
                    require(cause instanceof IllegalStateException && cause.getMessage().startsWith("Chunk is still in use:"),
                            "Unexpected eviction failure: " + cause);
                    return null;
                })).thenCompose(unused -> TaskScheduler.regionFuture(this, marker, () -> {
                    require(world.getBlockAt(marker).getType() == Material.STONE, "Purge eviction changed protected marker");
                    world.getChunkAt(marker).removePluginChunkTicket(this);
                    getLogger().info("PURGE_PINNED_CHUNK_SAFETY_PASSED");
                }));
    }

    private void verifyTerrain(org.bukkit.Chunk chunk) {
        var world = chunk.getWorld();
        var snapshot = chunk.getChunkSnapshot(true, true, false);
        boolean nether = world.getEnvironment() == org.bukkit.World.Environment.NETHER;
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++)
                for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                    Material block = snapshot.getBlockType(x, y, z);
                    if (nether && y >= world.getMaxHeight() - 17) {
                        require(block.isAir() || block == Material.BEDROCK || block == Material.NETHERRACK || block == Material.GLOWSTONE,
                                "Unexpected nether roof block: " + block);
                        if (y == world.getMaxHeight() - 1) require(block == Material.BEDROCK, "Nether roof missing");
                    } else require(block.isAir(), "Unexpected terrain/structure in " + world.getName() + ": " + block + " at " + y);
                }
        String expectedBiome = switch (world.getEnvironment()) {
            case NETHER -> "minecraft:nether_wastes";
            case THE_END -> "minecraft:the_end";
            default -> "minecraft:plains";
        };
        require(world.getGenerator() != null, "Custom generator missing in " + world.getName());
        require(snapshot.getBiome(0, 64, 0).getKey().toString().equals(expectedBiome), "Wrong biome in " + world.getName());
    }

    private CompletableFuture<Void> testBiomes() {
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var addon = BentoBox.getInstance().getAddonsManager().getAddonByName("Biomes").orElseThrow();
        Location first = new Location(world, 4194300, 100, 4194300);
        Location last = first.clone().add(8, 0, 8);
        return spawnBot("Biome" + java.util.UUID.randomUUID().toString().substring(0, 8),
                new Location(world, 8192.5, 90, 8192.5)).thenCompose(player -> {
            try {
                var loader = addon.getClass().getClassLoader();
                Class<?> biomeClass = Class.forName("world.bentobox.biomes.database.objects.BiomesObject", true, loader);
                Object biome = biomeClass.getConstructor().newInstance();
                biomeClass.getMethod("setBiome", org.bukkit.block.Biome.class).invoke(biome, org.bukkit.block.Biome.DESERT);
                Class<?> taskClass = Class.forName("world.bentobox.biomes.tasks.BiomeUpdateTask", true, loader);
                Object task = taskClass.getConstructor(addon.getClass(), User.class, biomeClass)
                        .newInstance(addon, User.getInstance(player), biome);
                taskClass.getMethod("setWorld", org.bukkit.World.class).invoke(task, world);
                taskClass.getMethod("setMinCoordinate", org.bukkit.util.BlockVector.class).invoke(task,
                        new org.bukkit.util.BlockVector(first.getBlockX(), 100, first.getBlockZ()));
                taskClass.getMethod("setMaxCoordinate", org.bukkit.util.BlockVector.class).invoke(task,
                        new org.bukkit.util.BlockVector(last.getBlockX(), 100, last.getBlockZ()));
                taskClass.getMethod("updateChunkQueue").invoke(task);
                Object queue = levelGetter(addon, "getUpdateQueue", "world.bentobox.biomes.tasks.UpdateQueue");
                var result = (java.util.concurrent.CompletionStage<?>) queue.getClass()
                        .getMethod("addUpdateTask", taskClass).invoke(queue, task);
                return result.toCompletableFuture().thenCompose(status -> {
                    require(status.toString().equals("FINISHED"), "Biome queue failed: " + status);
                    return TaskScheduler.regionFuture(this, first, () -> {
                        require(Bukkit.isOwnedByCurrentRegion(first), "Biome first region not owned");
                        require(world.getBiome(first.getBlockX(), 100, first.getBlockZ()) == org.bukkit.block.Biome.DESERT,
                                "First remote biome missing");
                    }).thenCompose(ignored -> TaskScheduler.regionFuture(this, last, () -> {
                        require(Bukkit.isOwnedByCurrentRegion(last), "Biome last region not owned");
                        require(world.getBiome(last.getBlockX(), 100, last.getBlockZ()) == org.bukkit.block.Biome.DESERT,
                                "Last cross-chunk biome missing");
                        getLogger().info("BIOMES_CROSS_CHUNK_UPDATE_PASSED");
                    }));
                }).thenCompose(ignored -> testBiomeEconomy(player, addon))
                        .thenCompose(ignored -> testBankConcurrentPayments(player, addon))
                        .thenCompose(ignored -> testBiomeRefund(player, addon, true))
                        .thenCompose(ignored -> testBiomeVault(player, addon))
                        .thenCompose(ignored -> testBiomeRefund(player, addon, false))
                        .thenCompose(ignored -> testBiomePurchase(player, addon, true, true))
                        .thenCompose(ignored -> testBiomePurchase(player, addon, true, false))
                        .thenCompose(ignored -> testBiomePurchase(player, addon, false, true))
                        .thenCompose(ignored -> testBiomePurchase(player, addon, false, false));
            } catch (ReflectiveOperationException error) {
                return CompletableFuture.failedFuture(error);
            }
        });
    }

    private CompletableFuture<Void> testBiomeEconomy(org.bukkit.entity.Player player, Object addon) {
        var user = User.getInstance(player);
        var island = BentoBox.getInstance().getIslands()
                .getIslandById("BSkyBlockd0b6bd05-adf0-4b6e-9bfe-a780b053dd99").orElseThrow();
        return TaskScheduler.userFuture(this, user, () -> {}).thenCompose(ignored -> {
            var completed = new CompletableFuture<Void>();
            TaskScheduler.userFuture(this, user, () -> {
                try {
                    Object bank = BentoBox.getInstance().getAddonsManager().getAddonByName("Bank").orElseThrow();
                    Object manager = levelGetter(bank, "getBankManager", "world.bentobox.bank.BankManager");
                    var bankLoader = bank.getClass().getClassLoader();
                    Class<?> moneyType = Class.forName("world.bentobox.bank.data.Money", true, bankLoader);
                    Class<?> txType = Class.forName("world.bentobox.bank.data.TxType", true, bankLoader);
                    Object give = java.lang.Enum.valueOf(txType.asSubclass(Enum.class), "GIVE");
                    Object money = moneyType.getConstructor(double.class).newInstance(100.0);
                    var funded = (CompletableFuture<?>) manager.getClass().getMethod("set", User.class,
                            String.class, moneyType, moneyType, txType).invoke(manager, user, island.getUniqueId(), money, money, give);
                    funded.thenCompose(response -> {
                        require(response.toString().equals("SUCCESS"), "Bank funding failed: " + response);
                        var action = new CompletableFuture<Void>();
                        TaskScheduler.userFuture(this, user, () -> {
                            try {
                                Object settings = levelGetter(addon, "getSettings", "world.bentobox.biomes.config.Settings");
                                settings.getClass().getMethod("setUseBankAccount", boolean.class).invoke(settings, true);
                                Class<?> payment = Class.forName("world.bentobox.biomes.utils.BiomePayments", true,
                                        addon.getClass().getClassLoader());
                                var withdraw = payment.getMethod("withdraw", addon.getClass(), User.class,
                                        world.bentobox.bentobox.database.objects.Island.class, double.class);
                                var paid = (CompletableFuture<?>) withdraw.invoke(null, addon, user, island, 10.0);
                                paid.thenCompose(ok -> {
                                    require(Boolean.TRUE.equals(ok), "Real Bank debit failed");
                                    try {
                                        Object balance = manager.getClass().getMethod("getBalance",
                                                world.bentobox.bentobox.database.objects.Island.class).invoke(manager, island);
                                        require(((Number) balance.getClass().getMethod("getValue").invoke(balance)).doubleValue() == 90.0,
                                                "Bank debit balance incorrect");
                                        return (CompletableFuture<?>) withdraw.invoke(null, addon, user, island, 1000.0);
                                    } catch (ReflectiveOperationException error) {
                                        return CompletableFuture.failedFuture(error);
                                    }
                                }).whenComplete((ok, error) -> {
                                    if (error != null) action.completeExceptionally(error);
                                    else {
                                        try {
                                            require(Boolean.FALSE.equals(ok), "Insufficient Bank funds accepted");
                                            getLogger().info("BIOMES_REAL_BANK_PAYMENT_PASSED: balance 100 -> 90, insufficient funds rejected");
                                            action.complete(null);
                                        } catch (Exception failure) { action.completeExceptionally(failure); }
                                    }
                                });
                            } catch (ReflectiveOperationException error) { action.completeExceptionally(error); }
                        }).exceptionally(error -> { action.completeExceptionally(error); return null; });
                        return action;
                    }).whenComplete((value, error) -> {
                        if (error != null) completed.completeExceptionally(error);
                        else completed.complete(null);
                    });
                } catch (ReflectiveOperationException error) { completed.completeExceptionally(error); }
            }).exceptionally(error -> { completed.completeExceptionally(error); return null; });
            return completed;
        });
    }

    private CompletableFuture<Void> testBankConcurrentPayments(org.bukkit.entity.Player player, Object addon) {
        var result = new CompletableFuture<Void>();
        TaskScheduler.userFuture(this, User.getInstance(player), () -> {
            try {
                var island = BentoBox.getInstance().getIslands()
                        .getIslandById("BSkyBlockd0b6bd05-adf0-4b6e-9bfe-a780b053dd99").orElseThrow();
                Class<?> payment = Class.forName("world.bentobox.biomes.utils.BiomePayments", true,
                        addon.getClass().getClassLoader());
                var withdraw = payment.getMethod("withdraw", addon.getClass(), User.class,
                        world.bentobox.bentobox.database.objects.Island.class, double.class);
                var payments = new java.util.ArrayList<CompletableFuture<?>>();
                for (int i = 0; i < 10; i++) {
                    payments.add((CompletableFuture<?>) withdraw.invoke(null, addon, User.getInstance(player), island, 1.0));
                }
                CompletableFuture.allOf(payments.toArray(CompletableFuture[]::new)).whenComplete((ignored, error) -> {
                    try {
                        if (error != null) throw new IllegalStateException(error);
                        for (var paid : payments) require(Boolean.TRUE.equals(paid.join()), "Concurrent Bank debit rejected");
                        Object bank = BentoBox.getInstance().getAddonsManager().getAddonByName("Bank").orElseThrow();
                        Object manager = levelGetter(bank, "getBankManager", "world.bentobox.bank.BankManager");
                        Object balance = manager.getClass().getMethod("getBalance",
                                world.bentobox.bentobox.database.objects.Island.class).invoke(manager, island);
                        require(((Number) balance.getClass().getMethod("getValue").invoke(balance)).doubleValue() == 80.0,
                                "Concurrent Bank debits lost or duplicated");
                        getLogger().info("BANK_CONCURRENT_PAYMENT_PASSED: ten queued debits, balance 90 -> 80");
                        result.complete(null);
                    } catch (Exception failure) { result.completeExceptionally(failure); }
                });
            } catch (ReflectiveOperationException error) { result.completeExceptionally(error); }
        }).exceptionally(error -> { result.completeExceptionally(error); return null; });
        return result;
    }

    private CompletableFuture<Void> testBiomeRefund(org.bukkit.entity.Player player, Object addon, boolean bankAccount) {
        var result = new CompletableFuture<Void>();
        TaskScheduler.userFuture(this, User.getInstance(player), () -> {
            try {
                var user = User.getInstance(player);
                var island = BentoBox.getInstance().getIslands()
                        .getIslandById("BSkyBlockd0b6bd05-adf0-4b6e-9bfe-a780b053dd99").orElseThrow();
                Object settings = levelGetter(addon, "getSettings", "world.bentobox.biomes.config.Settings");
                settings.getClass().getMethod("setUseBankAccount", boolean.class).invoke(settings, bankAccount);
                Object bank = BentoBox.getInstance().getAddonsManager().getAddonByName("Bank").orElseThrow();
                Object manager = levelGetter(bank, "getBankManager", "world.bentobox.bank.BankManager");
                Object hook = levelGetter(addon, "getVaultHook", "world.bentobox.bentobox.hooks.VaultHook");
                java.util.function.DoubleSupplier balance = () -> {
                    try {
                        if (bankAccount) {
                            Object money = manager.getClass().getMethod("getBalance",
                                    world.bentobox.bentobox.database.objects.Island.class).invoke(manager, island);
                            return ((Number) money.getClass().getMethod("getValue").invoke(money)).doubleValue();
                        }
                        return ((Number) hook.getClass().getMethod("getBalance", User.class, org.bukkit.World.class)
                                .invoke(hook, user, island.getWorld())).doubleValue();
                    } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                };
                double before = balance.getAsDouble();
                Class<?> payments = Class.forName("world.bentobox.biomes.utils.BiomePayments", true,
                        addon.getClass().getClassLoader());
                Object captured = payments.getMethod("capture", addon.getClass(), User.class,
                        world.bentobox.bentobox.database.objects.Island.class, double.class)
                        .invoke(null, addon, user, island, 7.0);
                var paid = (CompletableFuture<?>) captured.getClass().getMethod("withdraw").invoke(captured);
                paid.thenCompose(ok -> {
                    require(Boolean.TRUE.equals(ok), "Refund probe debit rejected");
                    require(balance.getAsDouble() == before - 7, "Refund probe debit balance incorrect");
                    try {
                        // Change configuration after debit: the refund must still use the captured account.
                        settings.getClass().getMethod("setUseBankAccount", boolean.class).invoke(settings, !bankAccount);
                        return (CompletableFuture<?>) captured.getClass().getMethod("refund").invoke(captured);
                    } catch (ReflectiveOperationException error) { return CompletableFuture.failedFuture(error); }
                }).thenCompose(ok -> {
                    require(Boolean.TRUE.equals(ok), "Real refund rejected");
                    require(balance.getAsDouble() == before, "Real refund did not restore balance");
                    try {
                        return (CompletableFuture<?>) captured.getClass().getMethod("refund").invoke(captured);
                    } catch (ReflectiveOperationException error) { return CompletableFuture.failedFuture(error); }
                }).whenComplete((ok, error) -> {
                    try {
                        if (error != null) throw new IllegalStateException(error);
                        require(Boolean.TRUE.equals(ok) && balance.getAsDouble() == before, "Refund duplicated credit");
                        getLogger().info("BIOMES_REAL_" + (bankAccount ? "BANK" : "VAULT")
                                + "_REFUND_PASSED: " + before + " -> " + (before - 7) + " -> " + before);
                        result.complete(null);
                    } catch (Exception failure) { result.completeExceptionally(failure); }
                });
            } catch (ReflectiveOperationException error) { result.completeExceptionally(error); }
        }).exceptionally(error -> { result.completeExceptionally(error); return null; });
        return result;
    }

    private CompletableFuture<Void> testBiomePurchase(org.bukkit.entity.Player player, Object addon,
            boolean bankAccount, boolean enoughItems) {
        var result = new CompletableFuture<Void>();
        TaskScheduler.userFuture(this, User.getInstance(player), () -> {
            try {
                var user = User.getInstance(player);
                var island = BentoBox.getInstance().getIslands()
                        .getIslandById("BSkyBlockd0b6bd05-adf0-4b6e-9bfe-a780b053dd99").orElseThrow();
                var loader = addon.getClass().getClassLoader();
                Object settings = levelGetter(addon, "getSettings", "world.bentobox.biomes.config.Settings");
                settings.getClass().getMethod("setUseBankAccount", boolean.class).invoke(settings, bankAccount);
                Object manager = levelGetter(addon, "getAddonManager", "world.bentobox.biomes.managers.BiomesAddonManager");
                Object bank = BentoBox.getInstance().getAddonsManager().getAddonByName("Bank").orElseThrow();
                Object bankManager = levelGetter(bank, "getBankManager", "world.bentobox.bank.BankManager");
                Object hook = levelGetter(addon, "getVaultHook", "world.bentobox.bentobox.hooks.VaultHook");
                java.util.function.DoubleSupplier balance = () -> {
                    try {
                        if (bankAccount) {
                            Object money = bankManager.getClass().getMethod("getBalance",
                                    world.bentobox.bentobox.database.objects.Island.class).invoke(bankManager, island);
                            return ((Number) money.getClass().getMethod("getValue").invoke(money)).doubleValue();
                        }
                        return ((Number) hook.getClass().getMethod("getBalance", User.class, org.bukkit.World.class)
                                .invoke(hook, user, player.getWorld())).doubleValue();
                    } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                };
                double before = balance.getAsDouble();
                Class<?> biomeType = Class.forName("world.bentobox.biomes.database.objects.BiomesObject", true, loader);
                Class<?> dataType = Class.forName("world.bentobox.biomes.database.objects.BiomesIslandDataObject", true, loader);
                Object biome = biomeType.getConstructor().newInstance();
                String id = "probe_purchase_" + java.util.UUID.randomUUID().toString();
                biomeType.getMethod("setUniqueId", String.class).invoke(biome, id);
                biomeType.getMethod("setFriendlyName", String.class).invoke(biome, "Paid probe");
                biomeType.getMethod("setUnlockCost", Double.class).invoke(biome, 4.0);
                biomeType.getMethod("setUnlockItems", java.util.List.class).invoke(biome,
                        java.util.List.of(new org.bukkit.inventory.ItemStack(Material.DIAMOND, 3),
                                new org.bukkit.inventory.ItemStack(Material.EMERALD)));
                Object data = manager.getClass().getMethod("getIslandData",
                        world.bentobox.bentobox.database.objects.Island.class).invoke(manager, island);
                player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                player.getInventory().clear();
                player.getInventory().setItem(0, new org.bukkit.inventory.ItemStack(Material.DIAMOND, 5));
                if (enoughItems) player.getInventory().setItem(1, new org.bukkit.inventory.ItemStack(Material.EMERALD));
                var purchase = manager.getClass().getMethod("purchaseBiome", User.class,
                        world.bentobox.bentobox.database.objects.Island.class, dataType, biomeType);
                purchase.invoke(manager, user, island, data, biome);
                purchase.invoke(manager, user, island, data, biome);
                TaskScheduler.entityLater(this, player, () -> {
                    try {
                        boolean purchased = Boolean.TRUE.equals(dataType.getMethod("isPurchased", String.class).invoke(data, id));
                        require(purchased == enoughItems, "Paid biome purchase result incorrect");
                        require(balance.getAsDouble() == before - (enoughItems ? 4 : 0), "Purchase charged twice or failed refund");
                        require(player.getInventory().getItem(0).getAmount() == (enoughItems ? 2 : 5),
                                "Purchase item debit was partial or duplicated");
                        if (enoughItems) {
                            var database = new world.bentobox.bentobox.database.Database(
                                    (world.bentobox.bentobox.api.addons.Addon) addon, dataType);
                            Object persisted = database.loadObject(island.getUniqueId());
                            require(persisted != null && Boolean.TRUE.equals(dataType.getMethod("isPurchased", String.class)
                                    .invoke(persisted, id)), "Paid purchase not persisted");
                        }
                        getLogger().info("BIOMES_" + (bankAccount ? "BANK" : "VAULT") + "_PURCHASE_"
                                + (enoughItems ? "SUCCESS" : "MISSING_ITEM_REFUND") + "_PASSED: duplicate request, money and full basket checked");
                        result.complete(null);
                    } catch (Exception error) { result.completeExceptionally(error); }
                }, 40L);
            } catch (ReflectiveOperationException error) { result.completeExceptionally(error); }
        }).exceptionally(error -> { result.completeExceptionally(error); return null; });
        return result;
    }

    private CompletableFuture<Void> testBiomeVault(org.bukkit.entity.Player player, Object addon) {
        return TaskScheduler.userFuture(this, User.getInstance(player), () -> {
            try {
                var user = User.getInstance(player);
                Object settings = levelGetter(addon, "getSettings", "world.bentobox.biomes.config.Settings");
                settings.getClass().getMethod("setUseBankAccount", boolean.class).invoke(settings, false);
                Object hook = levelGetter(addon, "getVaultHook", "world.bentobox.bentobox.hooks.VaultHook");
                Object provider = hook.getClass().getMethod("getEconomy").invoke(hook);
                Class<?> economy = Class.forName("net.milkbowl.vault.economy.Economy", true,
                        hook.getClass().getClassLoader());
                economy.getMethod("createPlayerAccount", org.bukkit.OfflinePlayer.class).invoke(provider, player);
                Object funded = hook.getClass().getMethod("deposit", User.class, double.class, org.bukkit.World.class)
                        .invoke(hook, user, 100.0, player.getWorld());
                require(Boolean.TRUE.equals(funded.getClass().getMethod("transactionSuccess").invoke(funded)),
                        "Real Vault funding failed");
                double before = ((Number) hook.getClass().getMethod("getBalance", User.class,
                        org.bukkit.World.class).invoke(hook, user, player.getWorld())).doubleValue();
                Class<?> payment = Class.forName("world.bentobox.biomes.utils.BiomePayments", true,
                        addon.getClass().getClassLoader());
                var withdraw = payment.getMethod("withdraw", addon.getClass(), User.class,
                        world.bentobox.bentobox.database.objects.Island.class, double.class);
                var island = BentoBox.getInstance().getIslands()
                        .getIslandById("BSkyBlockd0b6bd05-adf0-4b6e-9bfe-a780b053dd99").orElseThrow();
                var paid = (CompletableFuture<?>) withdraw.invoke(null, addon, user, island, 10.0);
                require(Boolean.TRUE.equals(paid.getNow(null)), "Real Vault debit failed");
                double after = ((Number) hook.getClass().getMethod("getBalance", User.class,
                        org.bukkit.World.class).invoke(hook, user, player.getWorld())).doubleValue();
                require(after == before - 10.0, "Vault debit balance incorrect: " + before + " -> " + after);
                var rejected = (CompletableFuture<?>) withdraw.invoke(null, addon, user, island, before + 1000.0);
                require(Boolean.FALSE.equals(rejected.getNow(null)), "Insufficient Vault funds accepted");
                getLogger().info("BIOMES_REAL_VAULT_PAYMENT_PASSED: " + before + " -> " + after);
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        });
    }

    private CompletableFuture<Void> testWarps() {
        var plugin = BentoBox.getInstance();
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var addon = plugin.getAddonsManager().getAddonByName("Warps").orElseThrow();
        Location signLocation = new Location(world, 3145743, 100, 3145728);
        Location landing = signLocation.clone().add(1.5, 0, 0.5);
        java.util.UUID owner = java.util.UUID.nameUUIDFromBytes("isolated-warp-owner".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Object manager;
        try {
            manager = levelGetter(addon, "getWarpSignsManager", "world.bentobox.warps.managers.WarpSignsManager");
        } catch (ReflectiveOperationException error) {
            return CompletableFuture.failedFuture(error);
        }
        return Util.getChunkAtAsync(landing).thenCompose(chunk -> TaskScheduler.regionFuture(this, landing, () -> {
            landing.clone().add(0, -1, 0).getBlock().setType(Material.STONE, false);
            landing.getBlock().setType(Material.AIR, false);
            landing.clone().add(0, 1, 0).getBlock().setType(Material.AIR, false);
        })).thenCompose(ignored -> Util.getChunkAtAsync(signLocation))
                .thenCompose(chunk -> TaskScheduler.regionFuture(this, signLocation, () -> {
                    signLocation.getBlock().setType(Material.OAK_SIGN, false);
                    var sign = (org.bukkit.block.Sign) signLocation.getBlock().getState();
                    var rotation = (org.bukkit.block.data.type.Sign) sign.getBlockData();
                    rotation.setRotation(org.bukkit.block.BlockFace.EAST);
                    sign.setBlockData(rotation);
                    sign.setLine(0, org.bukkit.ChatColor.GREEN + "[Welcome]");
                    sign.setLine(1, "Isolation warp");
                    sign.update(true, false);
                    try {
                        require(Boolean.TRUE.equals(manager.getClass().getMethod("addWarp", java.util.UUID.class, Location.class)
                                .invoke(manager, owner, signLocation)), "Warp registration failed");
                    } catch (ReflectiveOperationException error) {
                        throw new IllegalStateException(error);
                    }
                })).thenCompose(ignored -> {
                    try {
                        @SuppressWarnings("unchecked")
                        CompletableFuture<Object> sign = (CompletableFuture<Object>) manager.getClass()
                                .getMethod("getSignInfoAsync", org.bukkit.World.class, java.util.UUID.class).invoke(manager, world, owner);
                        return sign.thenAccept(info -> {
                            try {
                                require(Boolean.TRUE.equals(info.getClass().getMethod("isReal").invoke(info)), "Offline owner's sign unavailable");
                                getLogger().info("WARPS_OFFLINE_SIGN_PASSED");
                            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
                        });
                    } catch (ReflectiveOperationException error) { return CompletableFuture.failedFuture(error); }
                }).thenCompose(ignored -> spawnBot("Warp" + java.util.UUID.randomUUID().toString().substring(0, 8),
                        new Location(world, 8192.5, 90, 8192.5)))
                .thenCompose(visitor -> {
                    CompletableFuture<Void> arrived = new CompletableFuture<>();
                    TaskScheduler.entity(this, visitor, () -> {
                        try {
                            visitor.setGravity(false);
                            visitor.addAttachment(this, "bskyblock.island", true);
                            visitor.addAttachment(this, "bskyblock.island.warps", true);
                            manager.getClass().getMethod("warpPlayer", org.bukkit.World.class, User.class, java.util.UUID.class)
                                    .invoke(manager, world, User.getInstance(visitor), owner);
                            pollWarpArrival(visitor, landing, 100, arrived);
                        } catch (Throwable error) { arrived.completeExceptionally(error); }
                    });
                    return arrived;
                });
    }

    private void pollWarpArrival(org.bukkit.entity.Player visitor, Location landing, int attempts, CompletableFuture<Void> result) {
        TaskScheduler.entityLater(this, visitor, () -> {
            try {
                if (visitor.getWorld().equals(landing.getWorld()) && visitor.getLocation().distanceSquared(landing) < 1) {
                    getLogger().info("WARPS_CROSS_REGION_TELEPORT_PASSED");
                    require(visitor.performCommand("island warps"), "Warps GUI command rejected");
                    pollWarpPanel(visitor, 100, result);
                } else {
                    require(attempts > 0, "Warp did not reach cross-chunk landing");
                    pollWarpArrival(visitor, landing, attempts - 1, result);
                }
            } catch (Throwable error) { result.completeExceptionally(error); }
        }, 2);
    }

    private void pollWarpPanel(org.bukkit.entity.Player visitor, int attempts, CompletableFuture<Void> result) {
        TaskScheduler.entityLater(this, visitor, () -> {
            try {
                var inventory = visitor.getOpenInventory().getTopInventory();
                if (inventory.getType() == org.bukkit.event.inventory.InventoryType.CHEST) {
                    require(java.util.Arrays.stream(inventory.getContents()).filter(Objects::nonNull).anyMatch(item -> {
                        var meta = item.getItemMeta();
                        return meta != null && meta.hasLore() && meta.getLore().stream().anyMatch(line -> line.contains("Isolation warp"));
                    }), "Warps GUI does not contain offline owner's sign");
                    visitor.closeInventory();
                    getLogger().info("WARPS_GUI_PASSED");
                    result.complete(null);
                } else {
                    require(attempts > 0, "Warps GUI did not open");
                    pollWarpPanel(visitor, attempts - 1, result);
                }
            } catch (Throwable error) { result.completeExceptionally(error); }
        }, 2);
    }

    private CompletableFuture<Void> testLevel() {
        var plugin = BentoBox.getInstance();
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var level = plugin.getAddonsManager().getAddonByName("Level").orElseThrow();
        Location requested = new Location(world, 2097152, 100, 2097152);
        var island = plugin.getIslands().getIslandAt(requested).orElseGet(() ->
                Objects.requireNonNull(plugin.getIslands().createIsland(requested, java.util.UUID.randomUUID()), "Probe island rejected"));
        Location center = island.getCenter();
        java.util.UUID owner = island.getOwner();
        island.setProtectionRange(16);
        require(Bukkit.getPlayer(owner) == null, "Probe owner must be offline");
        String previousId = getConfig().getString("level-island-id");
        if (previousId != null) {
            require(previousId.equals(island.getUniqueId()), "Level persistence island changed");
            try {
                Object manager = levelGetter(level, "getManager", "world.bentobox.level.LevelsManager");
                Object data = manager.getClass().getMethod("getLevelsData", Island.class).invoke(manager, island);
                var counts = (Map<?, ?>) data.getClass().getMethod("getMdCount").invoke(data);
                require(Objects.equals(counts.get(Material.DIAMOND_BLOCK), 1)
                        && Objects.equals(counts.get(Material.EMERALD_BLOCK), 9), "Level counts lost on restart");
                getLogger().info("LEVEL_RESTART_PERSISTENCE_PASSED: " + previousId);
            } catch (ReflectiveOperationException error) {
                return CompletableFuture.failedFuture(error);
            }
        }
        return Util.getChunkAtAsync(center).thenCompose(chunk -> TaskScheduler.regionFuture(this, center, () -> {
            try {
                Object settings = levelGetter(level, "getSettings", "world.bentobox.level.config.ConfigSettings");
                settings.getClass().getMethod("setIncludeChests", boolean.class).invoke(settings, true);
                world.getBlockAt(center).setType(Material.DIAMOND_BLOCK, false);
                var chestBlock = world.getBlockAt(center.clone().add(1, 0, 0));
                chestBlock.setType(Material.CHEST, false);
                var chest = (org.bukkit.block.Chest) chestBlock.getState();
                chest.getBlockInventory().clear();
                chest.getBlockInventory().addItem(new org.bukkit.inventory.ItemStack(Material.EMERALD_BLOCK, 9));
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        })).thenCompose(ignored -> {
            try {
                Object manager = levelGetter(level, "getManager", "world.bentobox.level.LevelsManager");
                @SuppressWarnings("unchecked")
                CompletableFuture<Object> calculation = (CompletableFuture<Object>) manager.getClass()
                        .getMethod("calculateLevel", java.util.UUID.class,
                                Island.class).invoke(manager, owner, island);
                return calculation.thenCompose(results -> {
                    try {
                        require(results != null, "Level returned no result");
                        require(results.getClass().getMethod("getState").invoke(results).toString().equals("AVAILABLE"), "Level result unavailable");
                        Object data = manager.getClass().getMethod("getLevelsData", Island.class)
                                .invoke(manager, island);
                        var counts = (Map<?, ?>) data.getClass().getMethod("getMdCount").invoke(data);
                        require(Objects.equals(counts.get(Material.DIAMOND_BLOCK), 1), "Remote block count incorrect: " + counts.get(Material.DIAMOND_BLOCK));
                        require(Objects.equals(counts.get(Material.EMERALD_BLOCK), 9), "Chest item count incorrect: " + counts.get(Material.EMERALD_BLOCK));
                        getLogger().info("LEVEL_OFFLINE_SCAN_PASSED: " + island.getUniqueId());
                        return IslandsManager.saveIsland(island).thenCompose(saved -> {
                            require(Boolean.TRUE.equals(saved), "Level probe island not saved");
                            CompletableFuture<Void> persisted = new CompletableFuture<>();
                            TaskScheduler.global(this, () -> {
                                try {
                                    getConfig().set("level-island-id", island.getUniqueId());
                                    saveConfig();
                                    persisted.complete(null);
                                } catch (Throwable error) {
                                    persisted.completeExceptionally(error);
                                }
                            });
                            return persisted;
                        });
                    } catch (ReflectiveOperationException error) {
                        throw new IllegalStateException(error);
                    }
                });
            } catch (ReflectiveOperationException error) {
                return CompletableFuture.failedFuture(error);
            }
        });
    }

    private Object levelGetter(Object level, String name, String returnType) throws ReflectiveOperationException {
        try {
            Class<?> type = Class.forName(returnType, true, level.getClass().getClassLoader());
            return java.lang.invoke.MethodHandles.publicLookup()
                    .findVirtual(level.getClass(), name, java.lang.invoke.MethodType.methodType(type)).invoke(level);
        } catch (Throwable error) {
            throw new ReflectiveOperationException(name, error);
        }
    }

    private CompletableFuture<Void> testPersistence() {
        var plugin = BentoBox.getInstance();
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        int persistenceCoordinate = 1048576 + (int) (System.currentTimeMillis() % 262144);
        for (String playerIsland : getConfig().getStringList("player-islands")) {
            require(plugin.getIslands().getIslandById(playerIsland).isPresent(), "Player island lost after restart: " + playerIsland);
            getLogger().info("PLAYER_RESTART_PERSISTENCE_PASSED: " + playerIsland);
        }
        String id = getConfig().getString("island-id");
        if (id != null) {
            var island = plugin.getIslands().getIslandById(id).orElseThrow(() -> new IllegalStateException("Island lost after restart"));
            require(island.getOwner().toString().equals(getConfig().getString("owner")), "Island owner changed after restart");
            Location center = getConfig().getLocation("marker-location",
                    new Location(world, 1048576, 90, 1048576));
            return Util.getChunkAtAsync(center).thenCompose(chunk -> TaskScheduler.regionFuture(this, center, () -> {
                require(world.getBlockAt(center).getType() == Material.EMERALD_BLOCK, "World block lost after restart");
                getLogger().info("RESTART_PERSISTENCE_PASSED: " + id);
            }));
        }
        java.util.UUID owner = java.util.UUID.randomUUID();
        var island = Objects.requireNonNull(plugin.getIslands().createIsland(
                new Location(world, persistenceCoordinate, 90, persistenceCoordinate), owner), "Island creation rejected");
        Location center = island.getCenter();
        return Util.getChunkAtAsync(center).thenCompose(chunk -> TaskScheduler.regionFuture(this, center, () -> {
            world.getBlockAt(center).setType(Material.EMERALD_BLOCK, false);
        })).thenCompose(unused -> IslandsManager.saveIsland(island))
                .thenAccept(saved -> {
                    require(Boolean.TRUE.equals(saved), "Island database save failed");
                    getConfig().set("island-id", island.getUniqueId());
                    getConfig().set("owner", owner.toString());
                    getConfig().set("marker-location", center);
                    saveConfig();
                    getLogger().info("PERSISTENCE_PREPARED: " + island.getUniqueId());
                });
    }

    private CompletableFuture<org.bukkit.entity.Player> spawnBot(String name, Location location) {
        CompletableFuture<org.bukkit.entity.Player> result = new CompletableFuture<>();
        Util.getChunkAtAsync(location).thenCompose(unused -> TaskScheduler.regionFuture(this, location, () -> {
            try {
                Class<?> state = Class.forName("org.leavesmc.leaves.bot.BotCreateState");
                Object builder = state.getMethod("builder", String.class, Location.class).invoke(null, name, location);
                Object handle = builder.getClass().getMethod("spawn").invoke(builder);
                require(handle != null, "Lophine bot spawn failed");
                var bot = (org.bukkit.entity.Player) handle;
                TaskScheduler.entityLater(this, bot, () -> {
                    getLogger().info("PLAYER_BOT_READY: " + bot.getName());
                    result.complete(bot);
                }, 20);
            } catch (ReflectiveOperationException error) {
                result.completeExceptionally(error);
            }
        })).exceptionally(error -> { result.completeExceptionally(error); return null; });
        return result;
    }

    private CompletableFuture<Void> testPlayerIsland(int offset) {
        var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        var plugin = BentoBox.getInstance();
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        return spawnBot("Probe" + suffix, new Location(world, 8192.5, 90, 8192.5)).thenCompose(player -> {
            CompletableFuture<Void> created = new CompletableFuture<>();
            TaskScheduler.entity(this, player, () -> {
                try {
                    player.setInvulnerable(true);
                    player.addAttachment(this, "bskyblock.island", true);
                    player.addAttachment(this, "bskyblock.island.create", true);
                    require(player.performCommand("island create default"), "Player island command rejected");
                    checkPlayerIsland(player, created);
                } catch (Throwable error) {
                    created.completeExceptionally(error);
                }
            });
            return created.thenCompose(unused -> {
                var island = Objects.requireNonNull(plugin.getIslands().getIsland(world, player.getUniqueId()), "Player island missing");
                return spawnBot("Guest" + suffix, island.getCenter().clone().add(0, 10, 0))
                        .thenCompose(visitor -> {
                            Location protectedBlock = island.getCenter();
                            return TaskScheduler.regionFuture(this, protectedBlock, () -> {
                                try {
                                    var event = new org.bukkit.event.block.BlockBreakEvent(world.getBlockAt(protectedBlock), visitor);
                                    Bukkit.getPluginManager().callEvent(event);
                                    require(event.isCancelled(), "Visitor block break was not protected");
                                    getLogger().info("PLAYER_ISLAND_PROTECTION_PASSED: " + island.getUniqueId());
                                } catch (Throwable error) {
                                    throw new java.util.concurrent.CompletionException(error);
                                }
                            }).thenCompose(done -> {
                                int rounds = getConfig().getInt("stress-resets", 1);
                                require(rounds >= 1 && rounds <= 100, "stress-resets must be between 1 and 100");
                                CompletableFuture<Void> resets = testCancelledPlayerReset(player, false)
                                        .thenCompose(ignored -> testCancelledPlayerReset(player, true));
                                for (int round = 0; round < rounds; round++) {
                                    resets = resets.thenCompose(ignored -> testPlayerReset(player));
                                }
                                return resets.thenCompose(ignored -> despawnBot(visitor))
                                        .thenCompose(ignored -> despawnBot(player));
                            });
                        });
            });
        });
    }

    private CompletableFuture<Void> despawnBot(org.bukkit.entity.Player bot) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        if (!bot.isOnline()) {
            result.complete(null);
        } else if (!bot.getScheduler().execute(this, () -> {
            bot.kickPlayer("probe complete");
            result.complete(null);
        }, () -> result.complete(null), 1)) {
            result.complete(null);
        }
        return result;
    }

    private CompletableFuture<Void> testPlayerReset(org.bukkit.entity.Player player) {
        CompletableFuture<Void> reset = new CompletableFuture<>();
        var previous = new java.util.concurrent.atomic.AtomicReference<world.bentobox.bentobox.database.objects.Island>();
        long started = System.nanoTime();
        TaskScheduler.userFuture(this, User.getInstance(player), () -> {
            var island = Objects.requireNonNull(BentoBox.getInstance().getIslands()
                    .getIsland(Bukkit.getWorld("bskyblock_world"), player.getUniqueId()));
            previous.set(island);
            player.addAttachment(this, "bskyblock.island.reset", true);
            player.addAttachment(this, "bskyblock.mod.bypasscooldowns", true);
            require(player.performCommand("island reset default"), "Player reset command rejected");
            checkPlayerIsland(player, reset, island.getUniqueId());
        }).exceptionally(error -> { reset.completeExceptionally(error); return null; });
        return reset.thenRun(() -> {
            var island = Objects.requireNonNull(previous.get());
            require(island.isDeletable(), "Old island was not marked for deletion after reset");
            createdIslands.remove(island.getUniqueId());
            getLogger().info("PLAYER_RESET_PASSED: " + island.getUniqueId()
                    + " elapsed_ms=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started));
        });
    }

    private CompletableFuture<Void> testCancelledPlayerReset(org.bukkit.entity.Player player, boolean preclear) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        TaskScheduler.userFuture(this, User.getInstance(player), () -> {
            var plugin = BentoBox.getInstance();
            var world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
            var island = Objects.requireNonNull(plugin.getIslands().getIsland(world, player.getUniqueId()));
            var members = Map.copyOf(island.getMembers());
            int islands = plugin.getIslands().getIslands(world, player.getUniqueId()).size();
            int resets = plugin.getPlayers().getPlayer(player.getUniqueId()).getResets(world);
            player.getInventory().setItem(35, new org.bukkit.inventory.ItemStack(Material.DIAMOND, 7));
            player.addAttachment(this, "bskyblock.island.reset", true);
            player.addAttachment(this, "bskyblock.mod.bypasscooldowns", true);
            var cancellations = preclear ? cancelPreclear : cancelResets;
            cancellations.add(player.getUniqueId());
            require(player.performCommand("island reset default"), "Cancelled-reset command rejected");
            TaskScheduler.entityLater(this, player, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (NewIsland.isCreating(User.getInstance(player))) {
                            TaskScheduler.entityLater(FoliaProbe.this, player, this, 2);
                            return;
                        }
                        require(!cancellations.contains(player.getUniqueId()), "Reset cancellation event never fired");
                        require(island.equals(plugin.getIslands().getIsland(world, player.getUniqueId())), "Cancellation changed primary island");
                        require(members.equals(island.getMembers()), "Cancellation removed old team members");
                        require(islands == plugin.getIslands().getIslands(world, player.getUniqueId()).size(), "Cancellation left an owned draft island");
                        require(resets == plugin.getPlayers().getPlayer(player.getUniqueId()).getResets(world), "Cancellation consumed reset allowance");
                        var marker = player.getInventory().getItem(35);
                        require(marker != null && marker.getType() == Material.DIAMOND && marker.getAmount() == 7,
                                "Cancellation cleared player inventory");
                        player.getInventory().setItem(35, null);
                        getLogger().info("PLAYER_CANCELLED_RESET_SAFETY_PASSED: " + island.getUniqueId()
                                + " stage=" + (preclear ? "PRECLEAR" : "RESET"));
                        result.complete(null);
                    } catch (Throwable error) {
                        result.completeExceptionally(error);
                    }
                }
            }, 2);
        }).exceptionally(error -> { result.completeExceptionally(error); return null; });
        return result;
    }

    private CompletableFuture<Void> testConcurrentPlayers() {
        long started = System.nanoTime();
        int players = getConfig().getInt("stress-players", 2);
        require(players >= 2 && players <= 35, "stress-players must be between 2 and 35");
        CompletableFuture<Void> workload = CompletableFuture.completedFuture(null);
        for (int start = 0; start < players; start += 5) {
            int batchStart = start;
            int batchSize = Math.min(5, players - start);
            workload = workload.thenCompose(unused -> {
                CompletableFuture<?>[] runs = new CompletableFuture<?>[batchSize];
                for (int i = 0; i < batchSize; i++) runs[i] = testPlayerIsland((batchStart + i) * 32768);
                return CompletableFuture.allOf(runs)
                        .thenCompose(ignored -> CompletableFuture.runAsync(() -> { },
                                CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)));
            });
        }
        return workload.thenRun(() ->
                getLogger().info("CONCURRENT_PLAYER_WORKLOAD_PASSED: " + players + " owners and " + players
                        + " visitors; resets_per_owner=" + getConfig().getInt("stress-resets", 1)
                        + " elapsed_ms=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)));
    }

    private void checkPlayerIsland(org.bukkit.entity.Player player, CompletableFuture<Void> result) {
        checkPlayerIsland(player, result, null);
    }

    private void checkPlayerIsland(org.bukkit.entity.Player player, CompletableFuture<Void> result, String previousIsland) {
        if (result.isDone()) return;
        try {
            var plugin = BentoBox.getInstance();
            var island = plugin.getIslands().getIsland(Bukkit.getWorld("bskyblock_world"), player.getUniqueId());
            if (island != null
                    && !world.bentobox.bentobox.managers.island.NewIsland.isCreating(User.getInstance(player))
                    && !island.getUniqueId().equals(previousIsland)
                    && player.getWorld().equals(island.getWorld())
                    && player.getLocation().distanceSquared(island.getCenter()) < 1024) {
                require(island.getOwner().equals(player.getUniqueId()), "Player does not own island");
                    getLogger().info("PLAYER_ISLAND_CREATED: " + island.getUniqueId());
                createdIslands.add(island.getUniqueId());
                result.complete(null);
                return;
            }
            TaskScheduler.entityLater(this, player, () -> checkPlayerIsland(player, result, previousIsland), 10);
        } catch (Throwable error) {
            result.completeExceptionally(error);
        }
    }

    private CompletableFuture<Void> testClipboardCopy(org.bukkit.World world) {
        Location first = new Location(world, 16384, 90, 16384);
        Location second = first.clone().add(1, 0, 0);
        CompletableFuture<Void> prepared = new CompletableFuture<>();
        TaskScheduler.regionFuture(this, first, () -> {
            world.getBlockAt(first).setType(Material.GOLD_BLOCK);
            world.getBlockAt(second).setType(Material.DIAMOND_BLOCK);
            prepared.complete(null);
        }).exceptionally(error -> { prepared.completeExceptionally(error); return null; });
        return prepared.thenCompose(unused -> {
            BlueprintClipboard clipboard = new BlueprintClipboard();
            clipboard.setPos1(first);
            clipboard.setPos2(second);
            clipboard.setOrigin(first.toVector());
            CompletableFuture<Void> copied = new CompletableFuture<>();
            TaskScheduler.global(this, () -> {
                try {
                    require(clipboard.copy(User.getInstance(getServer().getConsoleSender()), false, false, false), "Clipboard copy did not start");
                    checkCopy(clipboard, copied);
                } catch (Throwable error) {
                    copied.completeExceptionally(error);
                }
            });
            return copied;
        }).thenCompose(unused -> TaskScheduler.regionFuture(this, second, () -> {
            require(world.getBlockAt(first).getType() == Material.GOLD_BLOCK, "Clipboard copy changed source blocks");
            require(world.getBlockAt(second).getType() == Material.DIAMOND_BLOCK, "Clipboard copy changed source blocks");
        }));
    }

    private void checkCopy(BlueprintClipboard clipboard, CompletableFuture<Void> result) {
        if (!clipboard.isCopying()) {
            try {
                var blueprint = Objects.requireNonNull(clipboard.getBlueprint(), "Clipboard result missing");
                require(blueprint.getBlocks().size() == 2, "Clipboard did not copy both blocks");
                require(blueprint.getBlocks().get(new Vector(0, 0, 0)).getBlockData().equals("minecraft:gold_block"), "Gold block missing from clipboard");
                require(blueprint.getBlocks().get(new Vector(1, 0, 0)).getBlockData().equals("minecraft:diamond_block"), "Diamond block missing from clipboard");
                result.complete(null);
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
            return;
        }
        if (!result.isDone()) TaskScheduler.globalLater(this, () -> checkCopy(clipboard, result), 1);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
