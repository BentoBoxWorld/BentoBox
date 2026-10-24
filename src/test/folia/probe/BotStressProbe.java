package probe;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.Orientable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.scheduler.BukkitTask;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.PanelListenerManager;
import world.bentobox.bentobox.util.TaskScheduler;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.util.teleport.ClosestSafeSpotTeleport;

/**
 * Disposable-server only: fake-player stress for the Folia concurrency fixes.
 * <p>
 * Scenarios:
 * <ol>
 * <li>two islands paste their missing Nether island while three members each enter the portal in
 * the same tick - one paste per island, no duplicate blueprint entities;</li>
 * <li>a fake player is retired while {@link ClosestSafeSpotTeleport} is still searching - the
 * result future must finish instead of hanging;</li>
 * <li>six fake players on six region threads open/click/close the settings panel repeatedly -
 * every open panel is found and removed, no exception.</li>
 * </ol>
 * This is bot-level evidence only. It never claims real-client GUI or input coverage.
 */
public final class BotStressProbe {

    private final FoliaProbe plugin;
    private volatile String phase = "start";

    public BotStressProbe(FoliaProbe plugin) {
        this.plugin = plugin;
    }

    public static CompletableFuture<Void> run(FoliaProbe plugin) {
        return new BotStressProbe(plugin).run();
    }

    /** Runs only the retired-entity scenario, for a fast diagnostic cycle. */
    public static CompletableFuture<Void> runRetiredOnly(FoliaProbe plugin) {
        BotStressProbe probe = new BotStressProbe(plugin);
        CompletableFuture<Void> result;
        try {
            result = probe.retiredEntity(Objects.requireNonNull(Bukkit.getWorld("bskyblock_world_nether")));
        } catch (Throwable error) {
            result = CompletableFuture.failedFuture(error);
        }
        return result.orTimeout(240, TimeUnit.SECONDS).whenComplete((unused, error) -> {
            if (error != null) plugin.getLogger().log(Level.SEVERE, "BOT_STRESS_RETIRED_FAILED", error);
            else plugin.getLogger().info("BOT_STRESS_RETIRED_ONLY_PASSED");
        });
    }

    public CompletableFuture<Void> run() {
        CompletableFuture<Void> result;
        try {
            var bb = BentoBox.getInstance();
            World world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
            World nether = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world_nether"));
            require(bb.getIWM().isNetherIslands(world), "Nether islands must be enabled");
            require(bb.getIWM().getWorldSettings(world).isMakeNetherPortals(), "Make-nether-portals must be enabled");
            require(bb.getIWM().isPasteMissingIslands(world), "Paste-missing-islands must be enabled");
            require(!bb.getIWM().isUseOwnGenerator(world), "Use-own-generator must be disabled");
            phase = "concurrent-missing-paste";
            result = concurrentMissingPaste(world, nether)
                    .thenCompose(unused -> {
                        phase = "retired-entity";
                        return retiredEntity(nether);
                    }).thenCompose(unused -> {
                        phase = "panel-concurrency";
                        return panelConcurrency(world);
                    });
        } catch (Throwable error) {
            result = CompletableFuture.failedFuture(error);
        }
        return result.orTimeout(420, TimeUnit.SECONDS).whenComplete((unused, error) -> {
            if (error != null) plugin.getLogger().log(Level.SEVERE, "BOT_STRESS_FAILED phase=" + phase, error);
            else plugin.getLogger().info("BOT_STRESS_PASSED: concurrent missing-island paste, retired entity "
                    + "teleport completion, six-region panel open/click/close");
            plugin.getLogger().info("BOT_STRESS_SCOPE: fake players (Leaves Bot via FakePlayerPlugin) only; "
                    + "no real client, GUI input, protocol or long-run multi-player evidence; disposable test server");
        });
    }

    // ---------------------------------------------------------------------
    // Scenario 1: concurrent missing-island paste
    // ---------------------------------------------------------------------

    private CompletableFuture<Void> concurrentMissingPaste(World world, World nether) {
        List<CompletableFuture<PasteFixture>> preparations = new ArrayList<>();
        for (int i = 0; i < 2; i++) preparations.add(preparePasteIsland(world, 3));
        return CompletableFuture.allOf(preparations.toArray(CompletableFuture[]::new)).thenCompose(unused -> {
            List<PasteFixture> fixtures = preparations.stream().map(CompletableFuture::join).toList();
            long started = System.nanoTime();
            List<CompletableFuture<Void>> work = new ArrayList<>();
            for (PasteFixture fixture : fixtures) {
                // One region task per island: all three members enter in the same tick, the two
                // islands do it on two region threads at the same time.
                work.add(TaskScheduler.regionFuture(plugin, fixture.frame(), () -> {
                    Location[] spots = {
                            fixture.frame().clone().add(1.5, 1, 0.5),
                            fixture.frame().clone().add(2.5, 1, 0.5),
                            fixture.frame().clone().add(1.5, 2, 0.5)};
                    for (int i = 0; i < fixture.bots().size(); i++) {
                        fixture.bots().get(i).teleportAsync(spots[i]);
                    }
                    plugin.getLogger().info("BOT_STRESS_ENTRY: island=" + fixture.island().getUniqueId()
                            + " bots=" + fixture.bots().size() + " region_thread=" + Thread.currentThread().getName());
                }));
                for (Player bot : fixture.bots()) work.add(watchArrival(bot, world, nether, fixture.island()));
            }
            return CompletableFuture.allOf(work.toArray(CompletableFuture[]::new))
                    .thenCompose(done -> verifyPaste(fixtures, nether))
                    .thenRun(() -> plugin.getLogger().info("BOT_STRESS_PASTE_PASSED: islands=" + fixtures.size()
                            + " bots=6 elapsed_ms=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)))
                    .whenComplete((done, error) -> fixtures.forEach(fixture ->
                            fixture.bots().forEach(bot -> despawn(bot).exceptionally(ignored -> null))));
        });
    }

    private CompletableFuture<PasteFixture> preparePasteIsland(World world, int botCount) {
        World nether = Objects.requireNonNull(BentoBox.getInstance().getIWM().getNetherWorld(world));
        Location center = freshCenter(world);
        Location frame = center.clone();
        // Above the pasted Nether island (blocks span roughly centre_y-10..centre_y+21).
        frame.setY(110);
        Location netherFloor = new Location(nether, center.getBlockX(), 110, center.getBlockZ());
        Island island = Objects.requireNonNull(BentoBox.getInstance().getIslands()
                .createIsland(center, UUID.randomUUID()), "Island rejected at " + center);
        return Util.getChunkAtAsync(frame)
                .thenCompose(unused -> Util.getChunkAtAsync(netherFloor))
                .thenCompose(unused -> TaskScheduler.regionFuture(plugin, frame, () -> buildPortal(world, frame)))
                .thenCompose(unused -> TaskScheduler.regionFuture(plugin, netherFloor,
                        () -> buildTargetFloor(netherFloor)))
                .thenCompose(unused -> {
                    List<CompletableFuture<Player>> spawns = new ArrayList<>();
                    for (int i = 0; i < botCount; i++) {
                        spawns.add(spawn("Stress" + randomSuffix(), world, frame.clone().add(1.5, 1, -2)));
                    }
                    return CompletableFuture.allOf(spawns.toArray(CompletableFuture[]::new))
                            .thenApply(ignored -> spawns.stream().map(CompletableFuture::join).toList());
                }).thenCompose(bots -> {
                    CompletableFuture<Void> setup = CompletableFuture.completedFuture(null);
                    for (Player bot : bots) {
                        setup = setup.thenCompose(unused -> TaskScheduler.userFuture(plugin,
                                User.getInstance(bot), () -> {
                                    bot.setInvulnerable(true);
                                    bot.setCollidable(false);
                                    bot.setGravity(false);
                                    bot.addAttachment(plugin, "bskyblock.mod.bypassprotect", true);
                                    bot.addAttachment(plugin, "bskyblock.island", true);
                                }));
                    }
                    return setup.thenCompose(unused -> island.hasNetherIslandAsync())
                            .thenApply(exists -> {
                                require(!exists, "Fresh island already has Nether terrain at " + center);
                                plugin.getLogger().info("BOT_STRESS_FIXTURE: island=" + island.getUniqueId()
                                        + " center=" + center + " bots=" + bots.size());
                                return new PasteFixture(island, frame, bots);
                            });
                });
    }

    private CompletableFuture<Void> watchArrival(Player bot, World source, World nether, Island island) {
        CompletableFuture<Void> result = new CompletableFuture<>();
        AtomicInteger ticks = new AtomicInteger();
        bot.getScheduler().runAtFixedRate(plugin, task -> {
            try {
                int elapsed = ticks.addAndGet(20);
                if (!bot.getWorld().equals(source)) {
                    require(bot.getWorld().equals(nether), "Bot arrived in " + bot.getWorld().getName());
                    require(island.inIslandSpace(bot.getLocation()),
                            "Bot arrived outside its island: " + bot.getLocation());
                    require(bot.isValid() && bot.isOnline(), "Arrived bot is not live");
                    plugin.getLogger().info("BOT_STRESS_ARRIVAL: island=" + island.getUniqueId()
                            + " bot=" + bot.getName() + " at=" + bot.getLocation()
                            + " elapsed_ticks=" + elapsed);
                    result.complete(null);
                    task.cancel();
                    return;
                }
                if (elapsed % 100 == 0) {
                    plugin.getLogger().info("BOT_STRESS_WAIT: island=" + island.getUniqueId()
                            + " bot=" + bot.getName() + " at=" + bot.getLocation()
                            + " block=" + bot.getLocation().getBlock().getType()
                            + " cooldown=" + bot.getPortalCooldown() + " valid=" + bot.isValid());
                }
                if (elapsed >= 2400) throw new IllegalStateException("Portal arrival timeout: " + bot.getLocation());
            } catch (Throwable error) {
                result.completeExceptionally(error);
                task.cancel();
            }
        }, () -> result.completeExceptionally(new IllegalStateException("Bot retired before arrival")), 20, 20);
        return result.orTimeout(150, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> verifyPaste(List<PasteFixture> fixtures, World nether) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (PasteFixture fixture : fixtures) {
            Location center = fixture.frame().clone();
            center.setWorld(nether);
            chain = chain.thenCompose(unused -> fixture.island().hasNetherIslandAsync().thenAccept(exists ->
                    require(exists, "Missing Nether blueprint was not pasted for " + fixture.island().getUniqueId())))
                    .thenCompose(unused -> TaskScheduler.regionFuture(plugin, center, () -> {
                        java.util.Map<String, Long> types = new java.util.TreeMap<>();
                        for (org.bukkit.entity.Entity entity : nether.getNearbyEntities(center, 48, 128, 48)) {
                            if (!(entity instanceof Player)) {
                                types.merge(entity.getType().name(), 1L, Long::sum);
                            }
                        }
                        long count = types.values().stream().mapToLong(Long::longValue).sum();
                        // Fake player name tags are TextDisplays and belong to the bot plugin, not the blueprint
                        long blueprintCount = types.entrySet().stream()
                                .filter(e -> !"TEXT_DISPLAY".equals(e.getKey()))
                                .mapToLong(java.util.Map.Entry::getValue).sum();
                        plugin.getLogger().info("BOT_STRESS_ENTITY_COUNT: island="
                                + fixture.island().getUniqueId() + " count=" + count + " blueprint=" + blueprintCount
                                + " types=" + types);
                        // Single paste: 13 blueprint entities. A second paste would add the whole
                        // blueprint again (>= 26). Nether_wastes also spawns zombified piglins
                        // naturally, which can add a few, so only guard against duplication.
                        require(blueprintCount < 20,
                                "Unexpected Nether island entity count " + blueprintCount
                                        + " (single paste 13, double 26, natural spawns add a few)");
                    }));
        }
        return chain;
    }

    // ---------------------------------------------------------------------
    // Scenario 2: entity retired during safe-spot search
    // ---------------------------------------------------------------------

    private CompletableFuture<Void> retiredEntity(World nether) {
        Location center = freshCenter(nether);
        Location target = center.clone();
        target.setY(200);
        CompletableFuture<Void> done = new CompletableFuture<>();
        spawn("Retire" + randomSuffix(), nether, target).thenCompose(bot ->
                TaskScheduler.userFuture(plugin, User.getInstance(bot), () -> {
                    bot.setInvulnerable(true);
                    bot.setGravity(false);
                    ClosestSafeSpotTeleport.Builder builder = ClosestSafeSpotTeleport.builder(BentoBox.getInstance())
                            .entity(bot).location(target).portal();
                    ClosestSafeSpotTeleport teleport = builder.build();
                    plugin.getLogger().info("BOT_STRESS_RETIRED_BEGIN: bot=" + bot.getName()
                            + " target=" + target);
                    logSearchState(teleport, builder.getResult());
                    despawn(bot).whenComplete((unused, despawnError) -> {
                        if (despawnError != null) {
                            done.completeExceptionally(despawnError);
                            return;
                        }
                        builder.getResult().orTimeout(120, TimeUnit.SECONDS)
                                .whenComplete((value, error) -> {
                                    if (error != null) {
                                        done.completeExceptionally(error);
                                    } else if (!Boolean.FALSE.equals(value)) {
                                        done.completeExceptionally(new IllegalStateException(
                                                "Retired entity teleport completed with " + value));
                                    } else {
                                        plugin.getLogger().info("BOT_STRESS_RETIRED_PASSED: future completed false "
                                                + "after entity removal");
                                        done.complete(null);
                                    }
                                });
                    });
                })).whenComplete((unused, error) -> {
                    if (error != null) done.completeExceptionally(error);
                });
        return done.orTimeout(120, TimeUnit.SECONDS);
    }

    // ---------------------------------------------------------------------
    // Scenario 3: six-region panel open/click/close
    // ---------------------------------------------------------------------

    private CompletableFuture<Void> panelConcurrency(World world) {
        int botCount = 6;
        int rounds = 10;
        List<CompletableFuture<Player>> spawns = new ArrayList<>();
        for (int i = 0; i < botCount; i++) {
            Location location = freshCenter(world).clone().add(0.5, 20, 0.5);
            spawns.add(spawn("Panel" + randomSuffix(), world, location));
        }
        AtomicInteger peak = new AtomicInteger();
        return CompletableFuture.allOf(spawns.toArray(CompletableFuture[]::new)).thenCompose(unused -> {
            List<Player> bots = spawns.stream().map(CompletableFuture::join).toList();
            CompletableFuture<Void> permissions = CompletableFuture.completedFuture(null);
            for (Player bot : bots) {
                permissions = permissions.thenCompose(step -> TaskScheduler.userFuture(plugin,
                        User.getInstance(bot), () -> {
                            bot.setInvulnerable(true);
                            bot.setGravity(false);
                            bot.addAttachment(plugin, "bskyblock.island", true);
                            bot.addAttachment(plugin, "bskyblock.island.settings", true);
                        }));
            }
            return permissions.thenCompose(step -> {
                List<CompletableFuture<Void>> loops = new ArrayList<>();
                for (Player bot : bots) {
                    CompletableFuture<Void> loop = new CompletableFuture<>();
                    loops.add(loop);
                    panelRound(bot, rounds, peak, loop);
                }
                return CompletableFuture.allOf(loops.toArray(CompletableFuture[]::new));
            }).thenRun(() -> {
                int left = PanelListenerManager.getOpenPanels().size();
                require(left == 0, "Open panel map not empty after stress: " + left);
                plugin.getLogger().info("BOT_STRESS_PANEL_PASSED: bots=" + bots.size()
                        + " rounds=" + rounds + " peak_open=" + peak.get()
                        + " remaining=" + left);
            }).whenComplete((done, error) -> bots.forEach(bot -> despawn(bot).exceptionally(ignored -> null)));
        });
    }

    private void panelRound(Player bot, int remaining, AtomicInteger peak, CompletableFuture<Void> done) {
        if (remaining == 0) {
            done.complete(null);
            return;
        }
        TaskScheduler.entityLater(plugin, bot, () -> {
            try {
                require(bot.performCommand("island settings"),
                        "island settings command rejected for " + bot.getName());
            } catch (Throwable error) {
                done.completeExceptionally(error);
                return;
            }
            pollPanel(bot, 40, peak, () -> panelRound(bot, remaining - 1, peak, done), done);
        }, 1);
    }

    private void pollPanel(Player bot, int attempts, AtomicInteger peak, Runnable next,
            CompletableFuture<Void> done) {
        TaskScheduler.entityLater(plugin, bot, () -> {
            try {
                Panel panel = PanelListenerManager.getOpenPanels().get(bot.getUniqueId());
                if (panel == null || !panel.getInventory().equals(bot.getOpenInventory().getTopInventory())) {
                    require(attempts > 0, "Settings panel did not open for " + bot.getName());
                    pollPanel(bot, attempts - 1, peak, next, done);
                    return;
                }
                int slot = emptySlot(panel);
                InventoryClickEvent event = slot < 0
                        ? new InventoryClickEvent(bot.getOpenInventory(), InventoryType.SlotType.OUTSIDE,
                                -999, ClickType.LEFT, InventoryAction.PICKUP_ALL)
                        : new InventoryClickEvent(bot.getOpenInventory(), InventoryType.SlotType.CONTAINER,
                                slot, ClickType.LEFT, InventoryAction.PICKUP_ALL);
                Bukkit.getPluginManager().callEvent(event);
                require(event.isCancelled(), "Panel click was not cancelled for " + bot.getName());
                peak.accumulateAndGet(PanelListenerManager.getOpenPanels().size(), Math::max);
                bot.closeInventory();
                pollRemoval(bot, 10, next, done);
            } catch (Throwable error) {
                done.completeExceptionally(error);
            }
        }, 2);
    }

    private void pollRemoval(Player bot, int attempts, Runnable next, CompletableFuture<Void> done) {
        TaskScheduler.entityLater(plugin, bot, () -> {
            try {
                if (PanelListenerManager.getOpenPanels().containsKey(bot.getUniqueId())) {
                    require(attempts > 0, "Panel entry survived close for " + bot.getName());
                    pollRemoval(bot, attempts - 1, next, done);
                    return;
                }
                next.run();
            } catch (Throwable error) {
                done.completeExceptionally(error);
            }
        }, 1);
    }

    private static int emptySlot(Panel panel) {
        for (int slot = 0; slot < panel.getInventory().getSize(); slot++) {
            if (panel.getItems().get(slot) == null) return slot;
        }
        return -1;
    }

    // ---------------------------------------------------------------------
    // Fixtures and helpers
    // ---------------------------------------------------------------------

    /** Logs the search's internals so a stall can be told apart from a start that never ran. */
    private void logSearchState(ClosestSafeSpotTeleport teleport, CompletableFuture<Boolean> result) {
        AtomicInteger ticks = new AtomicInteger();
        AtomicReference<BukkitTask> timer = new AtomicReference<>();
        timer.set(TaskScheduler.globalTimer(plugin, () -> {
            int elapsed = ticks.addAndGet(20);
            try {
                Object task = field(teleport, "task");
                Object checking = field(teleport, "checking");
                Object iterator = field(teleport, "chunksToScanIterator");
                Object queue = field(teleport, "blockQueue");
                plugin.getLogger().info("BOT_STRESS_RETIRED_STATE: ticks=" + elapsed
                        + " done=" + result.isDone()
                        + " task=" + (task == null ? "null"
                                : ((BukkitTask) task).isCancelled() ? "cancelled" : "scheduled")
                        + " checking=" + (checking == null ? "null" : ((AtomicBoolean) checking).get())
                        + " chunksLeft=" + (iterator == null ? "null" : ((java.util.Iterator<?>) iterator).hasNext())
                        + " queue=" + (queue == null ? "null" : ((java.util.Queue<?>) queue).size()));
            } catch (Throwable error) {
                plugin.getLogger().log(Level.WARNING, "BOT_STRESS_RETIRED_STATE_FAILED", error);
            }
            if (result.isDone() || elapsed >= 2400) {
                BukkitTask current = timer.get();
                if (current != null) current.cancel();
            }
        }, 1L, 20L));
    }

    private static Object field(Object target, String name) {
        try {
            java.lang.reflect.Field declared = target.getClass().getDeclaredField(name);
            declared.setAccessible(true);
            return declared.get(target);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("field " + name, error);
        }
    }

    /** Obsidian floor plus a lit 2x3 portal at {@code frame.y}. */
    private void buildPortal(World world, Location frame) {
        int y = frame.getBlockY();
        for (int x = frame.getBlockX(); x <= frame.getBlockX() + 3; x++) {
            for (int z = frame.getBlockZ() - 4; z <= frame.getBlockZ(); z++) {
                world.getBlockAt(x, y - 1, z).setType(Material.OBSIDIAN, false);
                for (int offset = 0; offset <= 4; offset++) {
                    world.getBlockAt(x, y + offset, z).setType(Material.AIR, false);
                }
            }
        }
        for (int x = frame.getBlockX(); x <= frame.getBlockX() + 3; x++) {
            world.getBlockAt(x, y, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
            world.getBlockAt(x, y + 4, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
        }
        for (int offset = 0; offset <= 4; offset++) {
            world.getBlockAt(frame.getBlockX(), y + offset, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
            world.getBlockAt(frame.getBlockX() + 3, y + offset, frame.getBlockZ()).setType(Material.OBSIDIAN, false);
        }
        for (int x = frame.getBlockX() + 1; x <= frame.getBlockX() + 2; x++) {
            for (int offset = 1; offset <= 3; offset++) {
                var portalBlock = world.getBlockAt(x, y + offset, frame.getBlockZ());
                portalBlock.setType(Material.NETHER_PORTAL, false);
                if (portalBlock.getBlockData() instanceof Orientable portal) {
                    portal.setAxis(org.bukkit.Axis.X);
                    portalBlock.setBlockData(portal, false);
                }
            }
        }
    }

    /** Native portal creation needs room at the destination; the paste stays below y+22. */
    private void buildTargetFloor(Location center) {
        World world = center.getWorld();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                world.getBlockAt(center.getBlockX() + x, center.getBlockY() - 1, center.getBlockZ() + z)
                        .setType(Material.OBSIDIAN, false);
                for (int offset = 0; offset <= 5; offset++) {
                    world.getBlockAt(center.getBlockX() + x, center.getBlockY() + offset, center.getBlockZ() + z)
                            .setType(Material.AIR, false);
                }
            }
        }
    }

    /** Region-aligned centre: a 512-block region owns the whole 100-block island box. */
    private Location freshCenter(World world) {
        var islands = BentoBox.getInstance().getIslands();
        for (int attempt = 0; attempt < 20; attempt++) {
            int k = 1000 + ThreadLocalRandom.current().nextInt(20_000);
            int m = 1000 + ThreadLocalRandom.current().nextInt(20_000);
            Location center = new Location(world, (k << 9) + 256, 80, (m << 9) + 256);
            if (islands.getIslandAt(center).isEmpty()
                    && islands.getIslandAt(center.clone().add(64, 0, 64)).isEmpty()) {
                return center;
            }
        }
        throw new IllegalStateException("No free island coordinate in " + world.getName());
    }

    private static String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private CompletableFuture<Player> spawn(String name, World world, Location location) {
        return invokeFuture("spawnFakePlayerPlugin",
                new Class<?>[]{String.class, World.class, Location.class}, name, world, location);
    }

    private CompletableFuture<Void> despawn(Player player) {
        return invokeFuture("despawnFakePlayerVerified", new Class<?>[]{Player.class}, player);
    }

    @SuppressWarnings("unchecked")
    private <T> CompletableFuture<T> invokeFuture(String name, Class<?>[] types, Object... args) {
        return (CompletableFuture<T>) invoke(name, types, args);
    }

    private Object invoke(String name, Class<?>[] types, Object... args) {
        try {
            Method method = FoliaProbe.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method.invoke(plugin, args);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(name, error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record PasteFixture(Island island, Location frame, List<Player> bots) {
    }
}
