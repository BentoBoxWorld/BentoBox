package verification;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.managers.island.NewIsland;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Bot-level probe for IslandWarp on BentoBox/BSkyBlock.
 * <p>
 * Safety rules baked in: the bot gets a unique name, is staged far away from any
 * existing island, and the island it works on is only ever looked up by the bot's
 * own UUID plus an owner check. Cleanup deletes that island only.
 */
public final class BentoProbe extends JavaPlugin {
    static final double FAR = 20000.5;

    Player bot, guest;
    Island island;
    World sky;
    JavaPlugin target;
    UUID botId, guestId;
    String warpName;

    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    <T> T global(Callable<T> action) throws Exception {
        var future = new CompletableFuture<T>();
        Bukkit.getGlobalRegionScheduler().execute(this, () -> {
            try { future.complete(action.call()); } catch (Throwable e) { future.completeExceptionally(e); }
        });
        return future.get(40, TimeUnit.SECONDS);
    }

    <T> T onPlayer(Player p, Callable<T> action) throws Exception {
        var future = new CompletableFuture<T>();
        p.getScheduler().run(this, t -> {
            try { future.complete(action.call()); } catch (Throwable e) { future.completeExceptionally(e); }
        }, () -> future.completeExceptionally(new IllegalStateException("retired")));
        return future.get(40, TimeUnit.SECONDS);
    }

    <T> T tryPlayer(Player p, Callable<T> action) {
        try { return onPlayer(p, action); } catch (Exception e) { return null; }
    }

    void waitFor(BooleanSupplier test, String name) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(90);
        while (System.nanoTime() < end) {
            if (test.getAsBoolean()) return;
            Thread.sleep(250);
        }
        throw new AssertionError("timeout: " + name);
    }

    boolean await(BooleanSupplier test, int seconds) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
        while (System.nanoTime() < end) {
            if (test.getAsBoolean()) return true;
            Thread.sleep(250);
        }
        return false;
    }

    void pass(String name) { getLogger().info("PROBE_PASS: " + name); }
    void info(String name) { getLogger().info("PROBE_INFO: " + name); }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent e) {
                if (e.getPlayer().getName().startsWith("IWProbe")) {
                    getLogger().info("PROBE_CMD: " + e.getPlayer().getName() + " -> " + e.getMessage());
                }
            }
        }, this);
        Bukkit.getGlobalRegionScheduler().runDelayed(this, t -> new Thread(this::run, "IW-BentoProbe").start(), 100);
    }

    void run() {
        boolean passed = false;
        try {
            runTests();
            passed = true;
        } catch (Throwable e) {
            getLogger().log(java.util.logging.Level.SEVERE, "PROBE_FAILED", e);
        } finally {
            try { cleanup(); } catch (Exception e) { getLogger().log(java.util.logging.Level.SEVERE, "probe cleanup failed", e); }
            getLogger().info(passed ? "PROBE_ALL_PASS" : "PROBE_ALL_FAIL");
            getLogger().info("PROBE_FINISHED");
        }
    }

    Player spawnBot(String name) throws Exception {
        global(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fp spawn " + name));
        waitFor(() -> Bukkit.getPlayer(name) != null, "bot " + name + " spawned");
        Player p = Bukkit.getPlayer(name);
        check(p != null && p.isOnline(), "bot " + name + " offline");
        check(p.getClass().getName().contains("CraftPlayer"), "not a CraftPlayer: " + p.getClass().getName());
        return p;
    }

    IslandsManager manager() { return BentoBox.getInstance().getIslandsManager(); }

    /** Islands this bot owns. Never returns somebody else's island. */
    Island owned(UUID id) {
        List<Island> found = manager().getIslands(sky, id);
        for (Island i : found) {
            if (id.equals(i.getOwner()) && !i.isDeleted()) return i;
        }
        return null;
    }

    void move(Player p, double x, double y, double z) throws Exception {
        check(p.teleportAsync(new Location(sky, x, y, z)).get(40, TimeUnit.SECONDS), "teleport failed for " + p.getName());
        Thread.sleep(600);
    }

    /** The probe stages bots over void; without this the fake player falls, dies and is removed. */
    void harden(Player p) throws Exception {
        onPlayer(p, () -> {
            p.setInvulnerable(true);
            p.setGravity(false);
            p.setAllowFlight(true);
            p.setFlying(true);
            p.setHealth(20);
            p.setFoodLevel(20);
            return null;
        });
    }

    <T> T region(Location where, Callable<T> action) throws Exception {
        var future = new CompletableFuture<T>();
        Bukkit.getRegionScheduler().execute(this, where, () -> {
            try { future.complete(action.call()); } catch (Throwable e) { future.completeExceptionally(e); }
        });
        return future.get(40, TimeUnit.SECONDS);
    }

    /**
     * Nearest standable spot within {@code radius} of {@code centre} that is at least
     * {@code min} blocks away. Scanned in one region task because block reads must not
     * happen off the region thread. The BSkyBlock starter island is only ~11x11.
     */
    Location solidSpot(Location centre, double min, int radius) throws Exception {
        List<Location> found = region(centre, () -> {
            List<Location> out = new ArrayList<>();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Location c = centre.clone().add(dx, 0, dz);
                    boolean floor = c.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType().isSolid();
                    boolean clear = !c.getBlock().getType().isSolid()
                            && !c.getBlock().getRelative(org.bukkit.block.BlockFace.UP).getType().isSolid();
                    if (floor && clear && c.distanceSquared(centre) >= min * min) out.add(c);
                }
            }
            out.sort(Comparator.comparingDouble(c -> c.distanceSquared(centre)));
            return out;
        });
        check(!found.isEmpty(), "no standable spot within " + radius + " blocks of " + centre.toVector());
        return found.get(0);
    }

    void command(Player p, String cmd) throws Exception {
        onPlayer(p, () -> { p.performCommand(cmd); return null; });
        Thread.sleep(1200);
    }

    boolean near(Player p, Location where, double distance) {
        Boolean b = tryPlayer(p, () -> p.getLocation().distanceSquared(where) < distance * distance);
        return Boolean.TRUE.equals(b);
    }

    void runTests() throws Exception {
        check(Bukkit.getPluginManager().isPluginEnabled("BentoBox"), "BentoBox not enabled");
        check(Bukkit.getPluginManager().isPluginEnabled("FakePlayerPlus"), "FakePlayerPlus not enabled");
        var plugin = Bukkit.getPluginManager().getPlugin("IslandWarp");
        check(plugin != null && plugin.isEnabled(), "IslandWarp not enabled");
        target = (JavaPlugin) plugin;
        pass("IslandWarp enabled next to BentoBox " + Bukkit.getPluginManager().getPlugin("BentoBox").getPluginMeta().getVersion());

        sky = global(() -> Bukkit.getWorld("bskyblock_world"));
        check(sky != null, "bskyblock_world missing");
        int islandsBefore = manager().getIslands().size();
        info("islands before=" + islandsBefore);

        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        bot = spawnBot("IWProbe" + suffix);
        botId = bot.getUniqueId();
        harden(bot);
        info("bot=" + bot.getName() + " uuid=" + botId);

        move(bot, FAR, 120, FAR);
        waitFor(() -> bot.getWorld().getName().startsWith("bskyblock_world"), "bot in island world");
        check(manager().getIslandAt(bot.getLocation()).isEmpty(), "staging spot is already inside an island");
        pass("bot staged outside every island at " + bot.getLocation().toVector());

        info("PROBE_STEP /island create");
        info("world border size=" + sky.getWorldBorder().getSize() + " bot=" + bot.getLocation().toVector());
        Boolean dispatched = tryPlayer(bot, () -> Bukkit.dispatchCommand(bot, "island create"));
        info("/island create dispatched=" + dispatched);
        if (!await(() -> owned(botId) != null, 20)) {
            info("command path produced no island, falling back to the NewIsland API");
        }
        if (owned(botId) == null) {
            GameModeAddon addon = BentoBox.getInstance().getAddonsManager().getGameModeAddons().stream()
                    .filter(a -> a.getDescription().getName().equalsIgnoreCase("BSkyBlock"))
                    .findFirst().orElse(null);
            check(addon != null, "BSkyBlock addon not found");
            CompletableFuture<Island> creation = NewIsland.builder()
                    .player(User.getInstance(bot))
                    .reason(Reason.CREATE)
                    .addon(addon)
                    .buildAsync();
            try {
                creation.get(120, TimeUnit.SECONDS);
            } catch (Exception e) {
                getLogger().log(java.util.logging.Level.SEVERE, "PROBE_INFO: NewIsland failed", e);
            }
            waitFor(() -> owned(botId) != null, "island owned by bot (API)");
        }
        island = owned(botId);
        check(island != null, "no island owned by bot");
        info("island=" + island.getUniqueId() + " center=" + island.getCenter() + " deletable=" + island.isDeletable());
        check(!island.isDeletable(), "new island is already marked deletable");
        pass("fresh island " + island.getUniqueId() + " owned by the bot");

        waitFor(() -> Boolean.TRUE.equals(tryPlayer(bot, () -> island.onIsland(bot.getLocation()))), "bot on its island");
        pass("bot standing on its own island at " + bot.getLocation().toVector());

        Location landmark = bot.getLocation().clone();
        info("perms islandwarp.create=" + bot.hasPermission("islandwarp.create")
                + " islandwarp.use=" + bot.hasPermission("islandwarp.use")
                + " bskyblock.island.create=" + bot.hasPermission("bskyblock.island.create")
                + " op=" + bot.isOp());
        info("ground=" + onPlayer(bot, () -> bot.getLocation().getBlock().getType().name()
                + " below=" + bot.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN).getType().name()
                + " below2=" + bot.getLocation().getBlock().getRelative(org.bukkit.block.BlockFace.DOWN, 2).getType().name()
                + " at " + bot.getLocation().toVector()));

        // 1. Paid create with no Vault economy provider must stop at the payment page.
        info("PROBE_STEP paid create without an economy provider");
        command(bot, "iw create IWProbeGate other");
        boolean paidPage = Boolean.TRUE.equals(tryPlayer(bot,
                () -> bot.getOpenInventory().getType().name().equals("CHEST")));
        info("paid create opened payment page=" + paidPage);
        onPlayer(bot, () -> { bot.closeInventory(); return null; });
        Thread.sleep(800);
        check(paidPage, "creation-cost 100 did not open the payment page");
        pass("paid create stops at the payment page (no Vault provider on this server)");

        // 2. Free landmark (economy disabled in memory only) then teleport back.
        info("PROBE_STEP free landmark with economy disabled in memory");
        global(() -> { target.getConfig().set("economy.enabled", false); return null; });
        info("economy.enabled now=" + global(() -> target.getConfig().getBoolean("economy.enabled", true)));
        warpName = "IWProbeWarp" + suffix;
        command(bot, "iw create " + warpName + " other");
        info("open inventory after free create=" + onPlayer(bot, () -> bot.getOpenInventory().getType().name()));
        onPlayer(bot, () -> { bot.closeInventory(); return null; });
        Location away = solidSpot(landmark, 3.5, 9);
        info("moving to " + away.toVector());
        move(bot, away.getX(), away.getY(), away.getZ());
        check(!near(bot, landmark, 3), "bot never left the landmark, teleport would prove nothing");
        command(bot, "iw tp " + warpName);
        waitFor(() -> near(bot, landmark, 2), "bot teleported to its landmark");
        pass("landmark created and /iw tp landed the bot on it");

        // 3. A non-member may visit a public landmark.
        info("PROBE_STEP non-member visit");
        guest = spawnBot("IWProbeG" + suffix);
        guestId = guest.getUniqueId();
        harden(guest);
        move(guest, FAR, 120, FAR);
        Thread.sleep(800);
        check(!near(guest, landmark, 6), "guest already at the landmark, teleport would prove nothing");
        command(guest, "iw tp " + warpName);
        waitFor(() -> near(guest, landmark, 2), "guest teleported to the public landmark");
        pass("non-member visitor reached the public landmark");

        // 4. Banning the visitor must block the teleport.
        info("PROBE_STEP banned visitor");
        global(() -> { island.setRank(guestId, RanksManager.BANNED_RANK); return null; });
        Thread.sleep(1000);
        check(island.isBanned(guestId), "guest not banned on the island");
        move(guest, FAR, 120, FAR);
        Location bannedStart = onPlayer(guest, () -> guest.getLocation().clone());
        command(guest, "iw tp " + warpName);
        Thread.sleep(6000);
        check(near(guest, bannedStart, 3), "banned visitor was teleported");
        pass("BentoBox ban blocks the teleport");
    }

    void cleanup() throws Exception {
        if (island != null && botId != null) {
            Island live = owned(botId);
            if (live != null) {
                global(() -> { manager().deleteIsland(live, true, null); return null; });
                getLogger().info("PROBE_CLEANUP deleted own island " + live.getUniqueId());
            } else {
                getLogger().info("PROBE_CLEANUP island already gone or not owned by the bot, nothing deleted");
            }
        }
        // Only islands owned by a probe bot may be removed here.
        global(() -> {
            for (Island other : manager().getIslands()) {
                UUID owner = other.getOwner();
                if (owner == null) continue;
                String name = Bukkit.getOfflinePlayer(owner).getName();
                if (name != null && name.startsWith("IWProbe")) {
                    manager().deleteIsland(other, true, null);
                    getLogger().info("PROBE_CLEANUP removed stale probe island " + other.getUniqueId() + " owner=" + name);
                }
            }
            return null;
        });
        for (Player p : new Player[]{bot, guest}) {
            if (p != null && p.isOnline()) {
                try { onPlayer(p, () -> { p.kick(net.kyori.adventure.text.Component.text("probe finished")); return null; }); }
                catch (Exception e) { getLogger().warning("bot kick failed: " + e); }
            }
        }
        if (sky != null) {
            getLogger().info("PROBE_CLEANUP islands after=" + manager().getIslands().size());
            getLogger().info("PROBE_CLEANUP landmark name=" + warpName);
        }
    }
}
