package probe;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.TaskScheduler;
import world.bentobox.bentobox.util.Util;

/** Opt-in, disposable-server probe. Never reuses players, islands or challenge definitions. */
public final class TeamChallengeProbe {
    private final JavaPlugin plugin;
    private final BentoBox box = BentoBox.getInstance();
    private final String run = UUID.randomUUID().toString().replace("-", "");
    private final List<Object> challenges = new ArrayList<>();
    private final List<Player> bots = new ArrayList<>();
    private Object addon;
    private Object manager;
    private ClassLoader loader;
    private World world;
    private Island island;
    private Player first;
    private Player second;
    private String gameMode;

    private TeamChallengeProbe(JavaPlugin plugin) { this.plugin = plugin; }

    public static void run(JavaPlugin plugin) {
        var probe = new TeamChallengeProbe(plugin);
        try {
            probe.setup().thenCompose(unused -> probe.inventoryCase(false))
                    .thenCompose(unused -> probe.inventoryCase(true))
                    .thenCompose(unused -> probe.statisticCase())
                    .thenCompose(unused -> probe.cleanup())
                    .orTimeout(180, TimeUnit.SECONDS).whenComplete((unused, error) -> {
                        if (error == null) plugin.getLogger().info("TEAM_CHALLENGES_PROBE_PASSED: cross-region concurrent dedup, pooled split, per-member gate/rounding, statistic delta/reduction, all-member item/XP rewards");
                        else plugin.getLogger().log(Level.SEVERE,
                                "TEAM_CHALLENGES_PROBE_FAILED: fixtures retained; run=" + probe.run
                                + " (a timeout does not cancel in-flight completion)", error);
                    });
        } catch (Throwable error) {
            plugin.getLogger().log(Level.SEVERE, "TEAM_CHALLENGES_PROBE_FAILED: setup run=" + probe.run, error);
        }
    }

    private CompletableFuture<Void> setup() {
        require(TaskScheduler.isFolia(), "Requires a real Folia server");
        world = Objects.requireNonNull(Bukkit.getWorld("bskyblock_world"));
        addon = box.getAddonsManager().getAddonByName("Challenges").orElseThrow();
        loader = addon.getClass().getClassLoader();
        manager = call(addon, "getChallengesManager", new Class<?>[0]);
        gameMode = box.getIWM().getAddon(world).orElseThrow().getDescription().getName();
        int range = box.getIWM().getIslandDistance(world);
        require(range >= 16, "Probe island is too small for the owner platform");
        int coordinate = 2000000 + java.util.concurrent.ThreadLocalRandom.current().nextInt(10000) * 2048;
        Location center = new Location(world, coordinate, 100, -coordinate);
        Location a = center.clone().add(.5, 1, .5);
        Location b = center.clone().add(8192.5, 1, .5);
        return global(() -> {
            for (Island existing : box.getIslands().getIslands(world)) {
                Location c = Objects.requireNonNull(existing.getCenter(), "Existing island has no center");
                long separation = (long) range + existing.getRange() + 32;
                require(Math.abs(c.getX() - center.getX()) > separation
                        || Math.abs(c.getZ() - center.getZ()) > separation, "Fixture would overlap an existing island");
                require(Math.abs(c.getX() - b.getX()) > existing.getRange() + 2048L
                        || Math.abs(c.getZ() - b.getZ()) > existing.getRange() + 2048L,
                        "Remote platform is too close to an existing island");
            }
            island = Objects.requireNonNull(box.getIslands().createIsland(center, null), "New probe island rejected");
            island.setName("team-challenges-probe-" + run);
            require(island.inIslandSpace(a) && !island.inIslandSpace(b), "Invalid owner/remote platform layout");
            record("island", island.getUniqueId());
            record("center", center.toString());
            record("platforms", List.of(a.toString(), b.toString()));
            return null;
        }).thenCompose(unused -> platform(a, true)).thenCompose(unused -> platform(b, false))
                .thenCompose(unused -> spawn("TCPa" + run.substring(0, 10), a))
                .thenCompose(player -> { first = player; return spawn("TCPb" + run.substring(0, 10), b); })
                .thenCompose(player -> {
                    second = player;
                    return global(() -> {
                        require(box.getIslands().getIslands(first.getUniqueId()).isEmpty()
                                && box.getIslands().getIslands(second.getUniqueId()).isEmpty(), "Fresh bot already belongs to an island");
                        // Fixture-only owner assignment avoids setowner's live permission reads/notifications.
                        box.getIslands().getIslandCache().setOwner(island, first.getUniqueId());
                        box.getIslands().setJoinTeam(island, second.getUniqueId());
                        Flag flag = (Flag) type("ChallengesAddon").getField("CHALLENGES_ISLAND_PROTECTION").get(null);
                        island.setFlag(flag, RanksManager.MEMBER_RANK);
                        record("members", List.of(first.getUniqueId().toString(), second.getUniqueId().toString()));
                        require(island.getMemberSet().size() == 2, "Unexpected team members");
                        return null;
                    });
                }).thenCompose(unused -> awaitSeparateRegions(30, 0)).thenRun(() -> {
                    plugin.getLogger().info("TEAM_CHALLENGES_FIXTURE_READY: run=" + run + " island=" + island.getUniqueId());
                });
    }

    private CompletableFuture<Void> platform(Location location, boolean onTeamIsland) {
        return Util.getChunkAtAsync(world, location.getBlockX() >> 4, location.getBlockZ() >> 4, false)
                .thenCompose(existing -> {
                    require(existing == null, "Platform chunk already exists; refusing to modify it");
                    return Util.getChunkAtAsync(location);
                }).thenCompose(unused -> TaskScheduler.regionFuture(plugin, location, () -> {
                    require(onTeamIsland ? box.getIslands().getIslandAt(location).orElse(null) == island
                            : box.getIslands().getIslandAt(location).isEmpty(), "Platform island changed");
                    location.clone().add(0, -1, 0).getBlock().setType(Material.STONE, false);
                    require(location.getBlock().isPassable() && location.clone().add(0, 1, 0).getBlock().isPassable(), "Platform not clear");
                }));
    }

    private CompletableFuture<Void> awaitSeparateRegions(int remaining, int stable) {
        CompletableFuture<Void> delay = new CompletableFuture<>();
        TaskScheduler.globalLater(plugin, () -> delay.complete(null), 20);
        return delay.thenCompose(unused -> on(first, () -> {
            require(box.getIslands().getIslandAt(first.getLocation()).orElseThrow() == island, "Owner outside probe island");
            require(box.getIslands().getIsland(world, first.getUniqueId()) == island, "Owner team changed");
            return !Bukkit.isOwnedByCurrentRegion(second);
        }).thenCombine(on(second, () -> {
            require(box.getIslands().getIslandAt(second.getLocation()).isEmpty(), "Remote member entered an island");
            require(box.getIslands().getIsland(world, second.getUniqueId()) == island, "Remote member team changed");
            return !Bukkit.isOwnedByCurrentRegion(first);
        }), (a, b) -> a && b)).thenCompose(separate -> {
            int consecutive = separate ? stable + 1 : 0;
            if (consecutive >= 5) {
                plugin.getLogger().info("TEAM_CHALLENGES_REGIONS_STABLE: five dual-owner checks; remote member 8192 blocks away; both submissions use island owner");
                return CompletableFuture.completedFuture(null);
            }
            require(remaining > 1, "Bots did not remain in separate regions for five checks within 30 seconds");
            return awaitSeparateRegions(remaining - 1, consecutive);
        });
    }

    private CompletableFuture<Player> spawn(String name, Location location) {
        CompletableFuture<Player> result = new CompletableFuture<>();
        global(() -> {
            require(Bukkit.getPlayerExact(name) == null && !Bukkit.getOfflinePlayer(name).hasPlayedBefore(), "Bot identity already exists");
            record("bot-" + name, location.toString());
            require(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "fp spawn " + name), "fp spawn rejected; FakePlayerPlus is required");
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
                if (result.isDone()) { task.cancel(); return; }
                Player player = Bukkit.getPlayerExact(name);
                if (player == null) return;
                task.cancel();
                bots.add(player);
                // FakePlayerPlus completes initial positioning after publishing the player.
                TaskScheduler.globalLater(plugin, () -> on(player, () -> player.teleportAsync(location))
                        .thenCompose(future -> future).whenComplete((ok, error) -> {
                            if (error != null) result.completeExceptionally(error);
                            else if (!Boolean.TRUE.equals(ok)) result.completeExceptionally(new IllegalStateException("Bot teleport rejected"));
                            else result.complete(player);
                        }), 60);
            }, 20, 20);
            return null;
        }).exceptionally(error -> { result.completeExceptionally(error); return null; });
        return result.orTimeout(30, TimeUnit.SECONDS);
    }

    private CompletableFuture<Void> inventoryCase(boolean perMember) {
        Object challenge = challenge(perMember ? "per_member" : "pooled", "INVENTORY_TYPE");
        Object requirements = construct("database.object.requirements.InventoryRequirements");
        call(requirements, "setRequiredItems", new Class<?>[] { List.class }, List.of(new ItemStack(Material.COBBLESTONE, perMember ? 9 : 10)));
        call(requirements, "setTakeItems", new Class<?>[] { boolean.class }, true);
        call(challenge, "setPerMember", new Class<?>[] { boolean.class }, perMember);
        call(challenge, "setRequirements", new Class<?>[] { type("database.object.requirements.Requirements") }, requirements);
        return register(challenge).thenCompose(unused -> {
            if (!perMember) return CompletableFuture.completedFuture(null);
            return prepare(4, 20, 0).thenCompose(ignored -> attempt(first, challenge)).thenCompose(success -> {
                require(!success, "Per-member gate accepted a member below ceil(9/2)");
                return verify(challenge, 0, 4, 20, 0, -1);
            });
        }).thenCompose(unused -> prepare(20, 20, 0))
                .thenCompose(unused -> simultaneous(challenge))
                .thenCompose(unused -> verify(challenge, 1, 15, 15, 20, -1))
                .thenRun(() -> plugin.getLogger().info("TEAM_CHALLENGES_CASE_PASSED: " + (perMember ? "per-member ceil(9/2), insufficient member rejected" : "pooled water-level split") + ", exactly one concurrent commit and one reward per member"));
    }

    private CompletableFuture<Void> statisticCase() {
        Object challenge = challenge("statistic", "STATISTIC_TYPE");
        Object requirements = construct("database.object.requirements.StatisticRequirements");
        Object statistic;
        try {
            statistic = type("database.object.requirements.StatisticRequirements$StatisticRec")
                    .getConstructor(Statistic.class, org.bukkit.entity.EntityType.class, Material.class, Integer.class, Boolean.class)
                    .newInstance(Statistic.JUMP, null, null, 10, true);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        call(requirements, "setStatisticList", new Class<?>[] { java.util.Collection.class }, List.of(statistic));
        call(challenge, "setRequirements", new Class<?>[] { type("database.object.requirements.Requirements") }, requirements);
        return register(challenge).thenCompose(unused -> prepare(0, 0, 0))
                .thenCompose(unused -> attempt(first, challenge)).thenCompose(success -> {
                    require(!success, "Zero-delta statistic completion accepted");
                    return verify(challenge, 0, 0, 0, 0, 0);
                }).thenCompose(unused -> prepare(0, 0, 6))
                .thenCompose(unused -> simultaneous(challenge))
                .thenCompose(unused -> verify(challenge, 1, 0, 0, 20, 2))
                .thenRun(() -> plugin.getLogger().info("TEAM_CHALLENGES_CASE_PASSED: statistic baselines, aggregate 6+6, reduce exactly 10, concurrent dedup and all-member rewards"));
    }

    private Object challenge(String label, String kind) {
        Object challenge = construct("database.object.Challenge");
        call(challenge, "setUniqueId", new Class<?>[] { String.class }, gameMode + "_team_probe_" + run + "_" + label);
        call(challenge, "setFriendlyName", new Class<?>[] { String.class }, "Team probe " + label);
        call(challenge, "setLevel", new Class<?>[] { String.class }, "");
        call(challenge, "setDeployed", new Class<?>[] { boolean.class }, true);
        call(challenge, "setTeamChallenge", new Class<?>[] { boolean.class }, true);
        call(challenge, "setAggregateTeam", new Class<?>[] { boolean.class }, true);
        call(challenge, "setTeamPresence", new Class<?>[] { double.class }, 1.0);
        call(challenge, "setRepeatable", new Class<?>[] { boolean.class }, false);
        call(challenge, "setRewardChance", new Class<?>[] { int.class }, 100);
        call(challenge, "setRewardExperience", new Class<?>[] { int.class }, 20);
        call(challenge, "setRewardItems", new Class<?>[] { List.class }, List.of(new ItemStack(Material.DIAMOND, 2)));
        Class<?> kinds = type("database.object.Challenge$ChallengeType");
        Object value = Arrays.stream(kinds.getEnumConstants()).filter(v -> v.toString().equals(kind)).findFirst().orElseThrow();
        call(challenge, "setChallengeType", new Class<?>[] { kinds }, value);
        return challenge;
    }

    private CompletableFuture<Void> register(Object challenge) {
        return global(() -> {
            String id = (String) call(challenge, "getUniqueId", new Class<?>[0]);
            require(call(manager, "getChallenge", new Class<?>[] { String.class }, id) == null, "Challenge ID collision");
            Object requirements = call(challenge, "getRequirements", new Class<?>[0]);
            call(requirements, "setRequiredPermissions", new Class<?>[] { java.util.Set.class }, java.util.Set.of("teamchallengesprobe." + run));
            challenges.add(challenge);
            record("challenges", challenges.stream().map(c -> call(c, "getUniqueId", new Class<?>[0])).toList());
            require(Boolean.TRUE.equals(call(manager, "loadChallenge", new Class<?>[] { type("database.object.Challenge"), World.class, boolean.class, User.class, boolean.class }, challenge, world, false, null, true)), "Temporary challenge registration failed");
            require(Boolean.TRUE.equals(call(challenge, "isDeployed", new Class<?>[0])), "Temporary challenge invalid");
            return null;
        });
    }

    private CompletableFuture<Void> prepare(int a, int b, int statistic) {
        return CompletableFuture.allOf(prepare(first, a, statistic), prepare(second, b, statistic));
    }

    private CompletableFuture<Void> prepare(Player player, int items, int statistic) {
        return on(player, () -> {
            player.setGameMode(GameMode.SURVIVAL);
            player.addAttachment(plugin, "teamchallengesprobe." + run, true);
            player.setInvulnerable(true);
            player.getInventory().clear();
            if (items > 0) player.getInventory().setItem(0, new ItemStack(Material.COBBLESTONE, items));
            player.setExp(0); player.setLevel(0); player.setTotalExperience(0);
            player.setStatistic(Statistic.JUMP, statistic);
            return null;
        });
    }

    private CompletableFuture<Boolean> attempt(Player player, Object challenge) {
        return on(player, () -> {
            require(!Bukkit.isOwnedByCurrentRegion(second), "Regions merged before submission");
            try {
                Object future = type("tasks.TryToComplete").getMethod("completeAsync", type("ChallengesAddon"), User.class,
                        type("database.object.Challenge"), World.class, String.class, String.class, int.class)
                        .invoke(null, addon, User.getInstance(player), challenge, world, "island", "bskyblock.", 1);
                @SuppressWarnings("unchecked") var result = (CompletableFuture<Boolean>) future;
                return result;
            } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        }).thenCompose(future -> future);
    }

    private CompletableFuture<Void> simultaneous(Object challenge) {
        // Only submitters need island permission; the remote teammate still contributes and receives rewards.
        CompletableFuture<Boolean> a = attempt(first, challenge);
        CompletableFuture<Boolean> b = attempt(first, challenge);
        return CompletableFuture.allOf(a, b).thenRun(() -> require(a.join() ^ b.join(),
                "Concurrent nonrepeatable submissions must return exactly one true: " + a.join() + ", " + b.join()));
    }

    private CompletableFuture<Void> verify(Object challenge, long completions, int a, int b, int xp, int statisticTotal) {
        return check(first, challenge, completions, a, xp).thenCombine(check(second, challenge, completions, b, xp),
                (sa, sb) -> {
                    if (statisticTotal >= 0) require(sa + sb == statisticTotal, "Wrong aggregate statistic deduction: " + sa + "+" + sb);
                    return null;
                });
    }

    private CompletableFuture<Integer> check(Player player, Object challenge, long completions, int items, int xp) {
        return on(player, () -> {
            require(!Bukkit.isOwnedByCurrentRegion(player == first ? second : first), "Regions merged during probe");
            long count = ((Number) call(manager, "getChallengeTimes", new Class<?>[] { User.class, World.class, type("database.object.Challenge") }, User.getInstance(player), world, challenge)).longValue();
            require(count == completions, player.getName() + ": shared completion count=" + count);
            require(count(player, Material.COBBLESTONE) == items, player.getName() + ": incorrect item charge");
            require(count(player, Material.DIAMOND) == (completions == 0 ? 0 : 2), player.getName() + ": missing/duplicate item reward");
            require(player.getTotalExperience() == xp, player.getName() + ": missing/duplicate XP reward=" + player.getTotalExperience());
            return player.getStatistic(Statistic.JUMP);
        });
    }

    private int count(Player player, Material material) {
        return Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull)
                .filter(stack -> stack.getType() == material).mapToInt(ItemStack::getAmount).sum();
    }

    private CompletableFuture<Void> cleanup() {
        return global(() -> {
            for (Object challenge : challenges) call(manager, "deleteChallenge", new Class<?>[] { type("database.object.Challenge") }, challenge);
            record("status", "assertions passed; temporary challenges removed; probe island/blocks/member progress retained for inspection");
            return null;
        }).thenCompose(unused -> CompletableFuture.allOf(bots.stream().map(player -> on(player, () -> {
            player.kickPlayer("team challenges probe complete"); return null;
        })).toArray(CompletableFuture[]::new)));
    }

    private <T> CompletableFuture<T> on(Player player, Supplier<T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        TaskScheduler.userFuture(plugin, User.getInstance(player), () -> {
            require(Bukkit.isOwnedByCurrentRegion(player), "Player action off owner thread");
            result.complete(action.get());
        }).whenComplete((unused, error) -> { if (error != null) result.completeExceptionally(error); });
        return result;
    }

    private <T> CompletableFuture<T> global(CheckedSupplier<T> action) {
        CompletableFuture<T> result = new CompletableFuture<>();
        TaskScheduler.global(plugin, () -> {
            try { result.complete(action.get()); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result;
    }

    private void record(String key, Object value) {
        plugin.getConfig().set("team-challenges-probes." + run + "." + key, value);
        plugin.saveConfig();
        plugin.getLogger().info("TEAM_CHALLENGES_FIXTURE: " + run + " " + key + "=" + value);
    }

    private Class<?> type(String suffix) {
        try { return Class.forName("world.bentobox.challenges." + suffix, true, loader); }
        catch (ClassNotFoundException error) { throw new IllegalStateException("Required Challenges API missing", error); }
    }

    private Object construct(String suffix) {
        try { return type(suffix).getConstructor().newInstance(); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }

    private Object call(Object target, String method, Class<?>[] signature, Object... arguments) {
        try { return target.getClass().getMethod(method, signature).invoke(target, arguments); }
        catch (InvocationTargetException error) { throw new IllegalStateException(method, error.getCause()); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException(method, error); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @FunctionalInterface
    private interface CheckedSupplier<T> { T get() throws Exception; }
}
