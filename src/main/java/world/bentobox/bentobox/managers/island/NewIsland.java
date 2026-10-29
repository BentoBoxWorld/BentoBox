package world.bentobox.bentobox.managers.island;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import world.bentobox.bentobox.util.TaskScheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BStats;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.api.events.team.TeamEvent;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.logs.LogEntry.LogType;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * Handles the creation and pasting of a new island for a player.
 * This class uses a builder pattern for flexible construction.
 * 
 * <p>
 * The process involves:
 * <ul>
 *   <li>Determining the next available location for the island</li>
 *   <li>Cleaning up user data (e.g., deaths, permissions)</li>
 *   <li>Firing relevant events for plugins/addons</li>
 *   <li>Pasting the blueprint or skipping paste if requested</li>
 *   <li>Setting up metrics and logging</li>
 * </ul>
 * </p>
 * 
 * @author tastybento
 */
public class NewIsland {
    private static final java.util.Set<java.util.UUID> CREATING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static boolean isCreating(User user) {
        return CREATING.contains(user.getUniqueId());
    }
    private final BentoBox plugin;
    private Island island;
    private final User user;
    private final Reason reason;
    private final World world;
    private String name;
    private final boolean noPaste;
    private final GameModeAddon addon;
    private CompletableFuture<Void> completion = CompletableFuture.completedFuture(null);
    private final Supplier<CompletableFuture<Void>> beforeTeleport;
    private boolean newlyAllocated;
    private boolean creationGuardHeld;
    private boolean leaveCleanupStarted;
    private boolean leaveCleanupCompleted;
    /** Guards the member cleanup: a failed tidyUp is retried, but on-leave commands must not run twice. */
    private boolean oldIslandMembersCleaned;
    /** The exit event and the creation guard are released once, even if tidyUp is retried. */
    private boolean tidyUpDone;
    private final boolean transactional;
    private final java.util.function.Function<Island, CompletableFuture<Void>> allocated;
    private PreparationAccessGuard preparationAccess;

    /** Operation-local barrier, including addon warps that do not use IslandsManager's home methods. */
    static final class PreparationAccessGuard implements org.bukkit.event.Listener {
        private final Location center;
        private final int range;

        PreparationAccessGuard(Location center, int range) {
            this.center = center.clone();
            this.range = range;
        }

        boolean blocks(Location destination) {
            return destination != null && Util.sameWorld(center.getWorld(), destination.getWorld())
                    && destination.getBlockX() >= center.getBlockX() - range
                    && destination.getBlockX() < center.getBlockX() + range
                    && destination.getBlockZ() >= center.getBlockZ() - range
                    && destination.getBlockZ() < center.getBlockZ() + range;
        }

        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
        public void teleport(org.bukkit.event.player.PlayerTeleportEvent event) {
            if (blocks(event.getTo())) event.setCancelled(true);
        }

        @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
        public void move(org.bukkit.event.player.PlayerMoveEvent event) {
            if (blocks(event.getTo())) event.setCancelled(true);
        }
    }

    private void protectPreparation(Location center) {
        if ((!TaskScheduler.isFolia() && !transactional) || preparationAccess != null) return;
        preparationAccess = new PreparationAccessGuard(center, plugin.getIWM().getIslandDistance(center.getWorld()));
        Bukkit.getPluginManager().registerEvents(preparationAccess, plugin);
    }

    private void releasePreparation() {
        if (preparationAccess != null) {
            org.bukkit.event.HandlerList.unregisterAll(preparationAccess);
            preparationAccess = null;
        }
    }

    private NewIslandLocationStrategy locationStrategy;

    /**
     * Constructs a new island using the provided builder.
     * Fires a PRECREATE event before proceeding.
     * 
     * @param builder Builder containing all required parameters
     * @throws IOException if insufficient parameters or event is cancelled
     */
    public NewIsland(Builder builder) throws IOException {
        plugin = BentoBox.getInstance();
        this.user = builder.user2;
        this.reason = builder.reason2;
        this.world = builder.world2;
        this.name = builder.name2;
        this.noPaste = builder.noPaste2;
        this.addon = builder.addon2;
        this.locationStrategy = builder.locationStrategy2;
        this.beforeTeleport = builder.beforeTeleport2;
        this.transactional = builder.transactional2;
        this.allocated = builder.allocated2;

        // Use default location strategy if none provided
        if (this.locationStrategy == null) {
            this.locationStrategy = new DefaultNewIslandLocationStrategy();
        }
        // Fire pre-create event to allow cancellation or modification
        IslandBaseEvent event = IslandEvent.builder().involvedPlayer(user.getUniqueId()).reason(Reason.PRECREATE)
                .build();
        if (event.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(event.isCancelled())) {
            // Event was cancelled, abort creation
            return;
        }
        if (!transactional) {
            newIsland(builder.oldIsland2);
        } else {
            try {
                newIsland(builder.oldIsland2);
            } catch (Throwable error) {
                if (island == null) releasePreparation();
                completion = (island == null ? CompletableFuture.<Void>completedFuture(null) : discardUnfinishedIsland())
                        .handle((ignored, cleanupError) -> {
                            if (cleanupError != null) {
                                error.addSuppressed(cleanupError);
                                throw new java.util.concurrent.CompletionException(error);
                            }
                            throw new RolledBackException(error);
                        });
            }
        }
    }

    /**
     * @return the island that was created
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Start building a new island
     * 
     * @return New island builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for NewIsland.
     * Allows flexible construction and validation of required parameters.
     */
    public static class Builder {
        private Island oldIsland2;
        private User user2;
        private Reason reason2;
        private World world2;
        private String name2 = BlueprintsManager.DEFAULT_BUNDLE_NAME;
        private boolean noPaste2;
        private GameModeAddon addon2;
        private NewIslandLocationStrategy locationStrategy2;
        private Supplier<CompletableFuture<Void>> beforeTeleport2 = () -> CompletableFuture.completedFuture(null);
        private java.util.function.Function<Island, CompletableFuture<Void>> afterBuild2 =
                island -> CompletableFuture.completedFuture(null);
        private boolean transactional2;
        private java.util.function.Function<Island, CompletableFuture<Void>> allocated2 =
                island -> CompletableFuture.completedFuture(null);

        public Builder allocated(java.util.function.Function<Island, CompletableFuture<Void>> action) {
            allocated2 = java.util.Objects.requireNonNull(action);
            return this;
        }

        /** Uses the observable prepare/commit pipeline on ordinary servers as well as Folia. */
        public Builder transactional() { transactional2 = true; return this; }

        /**
         * Sets the old island to be replaced.
         * Also sets the world to the old island's world.
         */
        public Builder oldIsland(Island oldIsland) {
            this.oldIsland2 = oldIsland;
            this.world2 = oldIsland.getWorld();
            return this;
        }

        /**
         * Sets the player for whom the island is being created.
         */
        public Builder player(User player) {
            this.user2 = player;
            return this;
        }

        /**
         * Sets the reason for island creation.
         * Only CREATE or RESET are allowed.
         * 
         * @param reason reason, can only be {@link Reason#CREATE} or {@link Reason#RESET}.
         */
        public Builder reason(Reason reason) {
            if (!reason.equals(Reason.CREATE) && !reason.equals(Reason.RESET)) {
                throw new IllegalArgumentException("Reason must be CREATE or RESET.");
            }
            this.reason2 = reason;
            return this;
        }

        /**
         * Sets the game mode addon and its world.
         */
        public Builder addon(GameModeAddon addon) {
            this.addon2 = addon;
            this.world2 = addon.getOverWorld();
            return this;
        }

        /**
         * Indicates that no blocks should be pasted for the island.
         */
        public Builder noPaste() {
            this.noPaste2 = true;
            return this;
        }

        /**
         * Sets the name of the blueprint bundle to use for the island.
         */
        public Builder name(String name) {
            this.name2 = name;
            return this;
        }

        /**
         * Sets the location strategy for finding the next island location.
         * 
         * @param strategy - the location strategy to use
         * @since 1.8.0
         */
        public Builder locationStrategy(NewIslandLocationStrategy strategy) {
            this.locationStrategy2 = strategy;
            return this;
        }

        /**
         * Runs after a successful Folia paste, before join resets or teleport.
         * CancellationException signals a veto and must be returned before mutating old-player state.
         */
        public Builder beforeTeleport(Supplier<CompletableFuture<Void>> action) {
            this.beforeTeleport2 = java.util.Objects.requireNonNull(action);
            return this;
        }

        /** Folia settlement runs after successful creation, while the player creation guard is held. */
        public Builder afterBuild(java.util.function.Function<Island, CompletableFuture<Void>> action) {
            this.afterBuild2 = java.util.Objects.requireNonNull(action);
            return this;
        }

        /**
         * Builds the island.
         * 
         * @return Island
         * @throws IOException if insufficient parameters (e.g., no user)
         */
        public Island build() throws IOException {
            if (user2 != null) {
                if (!CREATING.add(user2.getUniqueId())) {
                    throw new IOException("commands.island.create.creating-island");
                }
                try {
                    NewIsland newIsland = new NewIsland(this);
                    if (newIsland.getIsland() == null) CREATING.remove(user2.getUniqueId());
                    else newIsland.creationGuardHeld = true;
                    return newIsland.getIsland();
                } catch (Throwable error) {
                    CREATING.remove(user2.getUniqueId());
                    if (error instanceof IOException io) throw io;
                    throw error;
                }
            }
            throw new IOException("Insufficient parameters. Must have a user!");
        }

        /** Selects terrain on its owning regions, then creates the island on the player's thread. */
        public CompletableFuture<Island> buildAsync() {
            if (!TaskScheduler.isFolia() && !transactional2) {
                try {
                    return CompletableFuture.completedFuture(build());
                } catch (IOException error) {
                    return CompletableFuture.failedFuture(error);
                }
            }
            if (user2 == null) return CompletableFuture.failedFuture(new RolledBackException(
                    new IOException("Insufficient parameters. Must have a user!")));
            if (!CREATING.add(user2.getUniqueId())) {
                // Nothing was allocated: a caller that already debited must be able to refund safely.
                return CompletableFuture.failedFuture(new RolledBackException(
                        new IllegalStateException("Island creation is already in progress")));
            }
            try {
                return selectAndBuildAsync().thenCompose(island -> island == null
                        ? CompletableFuture.<Island>completedFuture(null)
                        : afterBuild2.apply(island).thenApply(ignored -> island))
                        .whenComplete((island, error) -> CREATING.remove(user2.getUniqueId()));
            } catch (Throwable error) {
                CREATING.remove(user2.getUniqueId());
                return CompletableFuture.failedFuture(new RolledBackException(error));
            }
        }

        private CompletableFuture<Island> selectAndBuildAsync() {
            var plugin = BentoBox.getInstance();
            var reserved = plugin.getIslands().getIsland(world2, user2);
            var strategy = locationStrategy2 == null ? new DefaultNewIslandLocationStrategy() : locationStrategy2;
            CompletableFuture<Location> selected;
            try {
                selected = reserved != null && reserved.isReserved()
                        ? CompletableFuture.completedFuture(reserved.getCenter())
                        : strategy.getNextLocationAsync(world2, user2);
            } catch (Throwable error) {
                return CompletableFuture.failedFuture(new RolledBackException(error));
            }
            selected = selected.exceptionally(error -> { throw new RolledBackException(error); });
            return selected.thenCompose(location -> {
                CompletableFuture<Island> result = new CompletableFuture<>();
                locationStrategy2 = world -> location;
                TaskScheduler.userFuture(plugin, user2, () -> {
                    try {
                        NewIsland created = new NewIsland(this);
                        created.completion.whenComplete((ignored, error) -> {
                            if (error != null) result.completeExceptionally(error);
                            else result.complete(created.getIsland());
                        });
                    } catch (Throwable error) {
                        result.completeExceptionally(error);
                    }
                }).exceptionally(error -> { result.completeExceptionally(new RolledBackException(error)); return null; });
                return result;
            });
        }
    }

    /**
     * Creates a new island for the user.
     * Handles location finding, user cleanup, event firing, blueprint pasting, and logging.
     * 
     * @param oldIsland old island that is being replaced, if any
     * @throws IOException if an island cannot be made. Message is the tag to show the user.
     */
    public void newIsland(Island oldIsland) throws IOException {
        // Find the new island location, either reserved or next available
        Location next = checkReservedIsland();
        if (next == null) {
            next = this.makeNextIsland();
        }
        // Clean up user data before moving to new island
        if (!TaskScheduler.isFolia() && !transactional) cleanUpUser();
        setProtectionRange();
        // Fire event for plugins/addons to react or cancel
        IslandBaseEvent event = IslandEvent.builder().involvedPlayer(user.getUniqueId()).reason(reason).island(island)
                .location(island.getCenter())
                .blueprintBundle(plugin.getBlueprintsManager().getBlueprintBundles(addon).get(name))
                .oldIsland(oldIsland).build();
        if (event.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(event.isCancelled())) {
            // Event was cancelled, abort creation
            completion = discardUnfinishedIsland();
            island = null;
            return;
        }
        event = event.getNewEvent().orElse(event);
        // Get the new BlueprintBundle if it was changed by the event
        switch (reason) {
        case CREATE -> name = ((IslandCreateEvent) event).getBlueprintBundle().getUniqueId();
        case RESET -> name = ((IslandResetEvent) event).getBlueprintBundle().getUniqueId();
        default -> {
            // Do nothing for other cases
        }
        }
        // Initialize before a no-paste or already-completed future can fire completion events.
        island.setFlagsDefaults();
        plugin.getMetrics().ifPresent(BStats::increaseIslandsCreatedCount);
        island.log(new LogEntry.Builder(LogType.JOINED).data(user.getUniqueId().toString(), "owner").build());
        IslandsManager.updateIsland(island);
        // Set the player's primary island
        if (!TaskScheduler.isFolia() && !transactional) plugin.getIslands().setPrimaryIsland(user.getUniqueId(), island);
        // Run post-creation tasks after creating the island
        if (TaskScheduler.isFolia() || transactional) {
            CompletableFuture<Void> pasted = allocated.apply(island).thenCompose(ignored -> noPaste
                    ? CompletableFuture.completedFuture(null)
                    : plugin.getBlueprintsManager().pasteTerrainAsync(addon, island, name, true));
            // Teleport before the Level scan: the scan can take minutes on large protection
            // ranges and the player must not be left waiting in the void for it.
            boolean[] delivered = {false};
            completion = pasted.thenRun(this::releasePreparation).thenCompose(ignored -> {
                CompletableFuture<Void> prepared = new CompletableFuture<>();
                TaskScheduler.userFuture(plugin, user, () -> {
                    // Leave commands and economy hooks cannot in general be rolled back.
                    leaveCleanupStarted = true;
                    beforeTeleport.get().whenComplete((done, error) -> {
                        if (error != null) prepared.completeExceptionally(error);
                        else {
                            leaveCleanupCompleted = true;
                            prepared.complete(null);
                        }
                    });
                }).exceptionally(error -> { prepared.completeExceptionally(error); return null; });
                return prepared;
            }).thenCompose(ignored -> TaskScheduler.userFuture(plugin, user, () -> {
                cleanUpUser();
                plugin.getIslands().setPrimaryIsland(user.getUniqueId(), island);
                if (island.getSpawnPoint(Environment.NORMAL) != null) {
                    plugin.getIslands().setHomeLocation(island, island.getSpawnPoint(Environment.NORMAL), "");
                }
            })).thenCompose(ignored -> noPaste ? CompletableFuture.completedFuture(null)
                    : plugin.getBlueprintsManager().completePasteAsync(addon, island, name)).thenCompose(ignored -> {
                if (reason == Reason.RESET || plugin.getIWM().isTeleportPlayerToIslandUponIslandCreation(world)) {
                    CompletableFuture<Void> teleported = new CompletableFuture<>();
                    TaskScheduler.userFuture(plugin, user, () -> {
                        user.getPlayer().setVelocity(new Vector(0, 0, 0));
                        user.getPlayer().setFallDistance(0F);
                        // This overload returns Void; rejected/unsafe teleports complete exceptionally upstream.
                        plugin.getIslands().homeTeleportAsync(island, user, true).whenComplete((ignoredTeleport, error) -> {
                            if (error != null) teleported.completeExceptionally(error);
                            else teleported.complete(null);
                        });
                    }).whenComplete((done, error) -> {
                        if (error != null) teleported.completeExceptionally(error);
                    });
                    return teleported;
                }
                return TaskScheduler.userFuture(plugin, user, () -> {
                    user.sendMessage("commands.island.create.you-can-teleport-to-your-island");
                    applyJoinResets(user);
                });
            }).thenCompose(ignored -> {
                // The island is now delivered; a failure from here must not strand the player.
                delivered[0] = true;
                // Clean up the old island before the initial level scan. The scan of a
                // protectionRange 400 island takes minutes, and the old island file is not
                // touched by it, so the order does not affect the new island's baseline.
                return tidyUp(oldIsland);
            }).thenCompose(ignored -> initializeLevelCount(plugin, island).handle((ignoredLevel, levelError) -> {
                if (levelError != null) {
                    // The island is usable; the level add-on can still scan it later, so this
                    // must not roll the reset back.
                    plugin.logError("Could not start the initial level scan for " + island.getUniqueId()
                            + ": " + levelError.getMessage());
                }
                return null;
            }));
            completion = completion.exceptionallyCompose(error -> {
                Throwable cause = error instanceof java.util.concurrent.CompletionException && error.getCause() != null
                        ? error.getCause() : error;
                // The teleport may have landed before the bookkeeping failed. Never delete the
                // island the player is standing on.
                if (!delivered[0] && island != null && user.isOnline() && island.onIsland(user.getLocation())) {
                    delivered[0] = true;
                }
                if (leaveCleanupStarted && !leaveCleanupCompleted
                        && cause instanceof java.util.concurrent.CancellationException) {
                    return discardUnfinishedIsland().thenRun(() -> island = null);
                }
                if (delivered[0]) {
                    // The player already has the new island, so the reset must finish:
                    // keeping the old island would give the player two islands. This is not a
                    // creation failure, so the command must not tell them it was.
                    plugin.logError("Island " + island.getUniqueId()
                            + " was delivered but the reset did not finish cleanly: " + cause);
                    return tidyUp(oldIsland).handle((ignoredCleanup, cleanupError) -> {
                        if (cleanupError != null) {
                            plugin.logError("Old island cleanup failed after a delivered reset: "
                                    + cleanupError.getMessage());
                        }
                        return null;
                    });
                }
                // The player never reached the new island. Deleting the old island now would
                // leave them standing in the void, so drop the unfinished draft instead.
                return discardUnfinishedIsland().handle((ignored, cleanupError) -> {
                    if (cleanupError != null) error.addSuppressed(cleanupError);
                    // The draft is gone and the player never reached the new island. beforeCommit is
                    // a no-op now, so refunding is safe even though the receipt is COMMITTING.
                    if (cleanupError == null) throw new RolledBackException(error);
                    throw new java.util.concurrent.CompletionException(error);
                });
            });
        } else if (noPaste) {
            // If noPaste is true, skip blueprint paste and run post-creation immediately
            TaskScheduler.user(plugin, user, () -> postCreationTask(oldIsland));
        } else {
            if (user.getWorld().equals(island.getWorld())) {
                // Determine if NMS (native Minecraft server) paste is needed based on player state
                double dist = user.getLocation().distance(island.getCenter());
                boolean useNMS = (user.getPlayer() instanceof ConsoleCommandSender) || !user.getWorld().equals(island.getWorld())
                        || (dist >= Bukkit.getViewDistance() * 16D);
                // Paste the blueprint, then run post-creation tasks
                plugin.getBlueprintsManager().paste(addon, island, name, () -> postCreationTask(oldIsland), useNMS);
            } else {
                plugin.getBlueprintsManager().paste(addon, island, name, () -> postCreationTask(oldIsland), true);
            }
        }
    }

    /**
     * Tasks to run after the new island has been created.
     * Handles spawn point setup, player teleportation, and cleanup.
     * 
     * @param oldIsland - old island that will be deleted
     */
    private void postCreationTask(Island oldIsland) {
        // Set initial spawn point if one exists
        if (island.getSpawnPoint(Environment.NORMAL) != null) {
            plugin.getIslands().setHomeLocation(island, island.getSpawnPoint(Environment.NORMAL), "");
            plugin.getIslands().setPrimaryIsland(user.getUniqueId(), island);
        }
        // If player is online, handle teleportation and movement
        if (user.isOnline()) {
            if (reason.equals(Reason.RESET) || (reason.equals(Reason.CREATE)
                    && plugin.getIWM().isTeleportPlayerToIslandUponIslandCreation(world))) {
                // Stop the player from falling or moving
                user.getPlayer().setVelocity(new Vector(0, 0, 0));
                user.getPlayer().setFallDistance(0F);
                // Teleport player after island is built, then tidy up
                // Teleport failure must not skip the cleanup: the old island would stay owned.
                plugin.getIslands().homeTeleportAsync(island, user, true).whenComplete((ignored, error) -> {
                    if (error != null) {
                        plugin.logError("Could not teleport to the new island: " + error.getMessage());
                    }
                    tidyUp(oldIsland);
                });
                return;
            } else {
                // Notify player they can teleport to their island
                user.sendMessage("commands.island.create.you-can-teleport-to-your-island");
                // Apply on-join resets even when not auto-teleporting to the new island
                applyJoinResets(user);
            }
        } else {
            // If player is offline, remove player data to clear cache
            User.removePlayer(user.getPlayer());
        }
        tidyUp(oldIsland);
    }

    /**
     * Cleans up a user before moving them to a new island.
     * Resets deaths once creation has been accepted.
     */
    private void cleanUpUser() {
        // Reset deaths if configured
        if (plugin.getIWM().isDeathsResetOnNewIsland(world)) {
            plugin.getPlayers().setDeaths(world, user.getUniqueId(), 0);
        }
    }

    /** Only emitted after the replacement was safely discarded, before player state was committed. */
    public static final class RolledBackException extends java.util.concurrent.CompletionException {
        public RolledBackException(Throwable cause) { super(cause); }
    }

    /** The initial baseline must be durable before cleanup or any player can edit the pasted island. */
    static CompletableFuture<Void> initializeLevelCount(BentoBox plugin, Island island) {
        var level = plugin.getAddonsManager().getAddonByName("Level").filter(addon -> addon.isEnabled());
        if (level.isEmpty()) return CompletableFuture.completedFuture(null);
        try {
            Object addon = level.get();
            Class<?> type = Class.forName(addon.getClass().getName(), true, addon.getClass().getClassLoader());
            // Reflect only the addon facade: manager signatures reference optional addons such as Visit.
            Object result = type.getMethod("initializeIslandCountAsync", Island.class).invoke(addon, island);
            if (!(result instanceof CompletableFuture<?> future)) {
                return CompletableFuture.failedFuture(new IllegalStateException("Level initialization returned no future"));
            }
            return future.thenApply(ignored -> null);
        } catch (java.lang.reflect.InvocationTargetException error) {
            return CompletableFuture.failedFuture(error.getCause());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            return CompletableFuture.failedFuture(new IllegalStateException("Could not initialize Level before island delivery", error));
        }
    }

    private void setProtectionRange() {
        // Set protection range based on user's permission, if different from default
        island.setProtectionRange(user.getPermissionValue(
                plugin.getIWM().getAddon(island.getWorld()).map(GameModeAddon::getPermissionPrefix).orElse("")
                + "island.range",
                island.getProtectionRange()));
    }

    private CompletableFuture<Void> discardUnfinishedIsland() {
        Island unfinished = island;
        if (!newlyAllocated) {
            unfinished.setReserved(true);
            IslandsManager.updateIsland(unfinished);
            releasePreparation();
            CREATING.remove(user.getUniqueId());
            return CompletableFuture.completedFuture(null);
        }
        return TaskScheduler.regionFuture(plugin, unfinished.getCenter(), () -> {
            plugin.getIslands().deleteIsland(unfinished, true, user.getUniqueId());
            if (unfinished.isOwned()) {
                // A listener vetoed the delete. Leaving it owned would give the player two islands.
                plugin.logError("Unfinished island deletion was cancelled, orphaning " + unfinished.getUniqueId());
                plugin.getIslands().orphanIsland(unfinished);
            }
            releasePreparation();
            CREATING.remove(user.getUniqueId());
        });
    }

    /**
     * Finds the next available location for a new island and adds it to the grid.
     * 
     * @return location of new island
     * @throws IOException if no unoccupied spots or island cannot be added to grid
     */
    private Location makeNextIsland() throws IOException {
        // Use location strategy to find next available spot
        Location next = this.locationStrategy.getNextLocation(world, user);
        if (next == null) {
            if (plugin.getIWM().getAddon(world).map(GameModeAddon::isEnforceEqualRanges).orElse(true)) {
                plugin.logError("Failed to make island - no unoccupied spot found.");
                plugin.logError("If the world was imported, try multiple times until all unowned islands are known.");
            }
            throw new IOException("commands.island.create.cannot-create-island");
        }
        // Add island to grid
        protectPreparation(next);
        island = plugin.getIslands().createIsland(next, user.getUniqueId());
        if (island == null) {
            releasePreparation();
            if (plugin.getIWM().getAddon(world).map(GameModeAddon::isEnforceEqualRanges).orElse(true)) {
                plugin.logError("Failed to make island! Island could not be added to the grid.");
            }
            throw new IOException("commands.island.create.unable-create-island");
        }
        newlyAllocated = true;
        return next;
    }

    /**
     * Checks if the user has a reserved island location.
     * If so, clears the reservation and returns the location.
     * 
     * @return reserved island location, or null if none found
     */
    private Location checkReservedIsland() {
        if (plugin.getIslands().hasIsland(world, user)) {
            // Island exists, just needs pasting. Do not assign it to the field unless it really
            // is reserved: if makeNextIsland() then throws, discardUnfinishedIsland() would mark
            // the player's existing island reserved and a later /island reset could delete it.
            Island existing = plugin.getIslands().getIsland(world, user);
            if (existing != null && existing.isReserved()) {
                island = existing;
                Location l = existing.getCenter();
                protectPreparation(l);
                // Clear the reservation
                existing.setReserved(false);
                return l;
            }
        }
        return null;
    }

    /**
     * Applies on-join resets (health, hunger, XP, inventory, etc.) to the player.
     * This is used when the player is not auto-teleported to their new island,
     * ensuring that resets still occur at island creation time.
     *
     * @param user the user to apply resets to
     */
    private void applyJoinResets(User user) {
        if (plugin.getIWM().isOnJoinResetEnderChest(world)) {
            user.getPlayer().getEnderChest().clear();
        }
        if (plugin.getIWM().isOnJoinResetInventory(world)) {
            user.getPlayer().getInventory().clear();
        }
        if (plugin.getSettings().isUseEconomy() && plugin.getIWM().isOnJoinResetMoney(world)) {
            plugin.getVault().ifPresent(vault -> vault.withdraw(user, vault.getBalance(user)));
        }
        if (plugin.getIWM().isOnJoinResetHealth(world)) {
            Util.resetHealth(user.getPlayer());
        }
        if (plugin.getIWM().isOnJoinResetHunger(world)) {
            user.getPlayer().setFoodLevel(20);
        }
        if (plugin.getIWM().isOnJoinResetXP(world)) {
            // Player collected XP (displayed)
            user.getPlayer().setLevel(0);
            user.getPlayer().setExp(0);
            // Player total XP (not displayed)
            user.getPlayer().setTotalExperience(0);
        }
        user.setGameMode(plugin.getIWM().getDefaultGameMode(world));
        Util.runCommands(user, user.getName(), plugin.getIWM().getOnJoinCommands(world), "join");
    }

    /**
     * Cleans up after island creation.
     * Deletes old island and fires exit event.
     *
     * <p>Member cleanup runs here, not before the teleport, so a failed reset leaves the old
     * island untouched instead of a ghost island with no members.
     *
     * @param oldIsland the old island to delete
     */
    private CompletableFuture<Void> tidyUp(Island oldIsland) {
        if (oldIsland != null && oldIsland.getOwner() != null && !user.getUniqueId().equals(oldIsland.getOwner())) {
            plugin.logError("Old island ownership changed during reset, not deleting " + oldIsland.getUniqueId());
            // The normal release at the end of this method is never reached on this path, and
            // without it the player is told they are already creating an island until a restart.
            if (creationGuardHeld) {
                CREATING.remove(user.getUniqueId());
            }
            // Nobody owns it now: mark it so the purge can reclaim the slot
            oldIsland.setDeletable(true);
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> members = oldIsland == null || oldIslandMembersCleaned
                ? CompletableFuture.completedFuture(null) : clearOldIslandMembers(oldIsland);
        oldIslandMembersCleaned = true;
        return members.handle((ignored, error) -> {
            if (error != null) {
                plugin.logError("Could not clean up the old island members: " + error.getMessage());
            }
            return null;
        }).thenCompose(ignored -> TaskScheduler.regionFuture(plugin,
                oldIsland == null ? island.getCenter() : oldIsland.getCenter(), () -> {
            if (oldIsland != null) {
                if (oldIsland.getOwner() != null) {
                    // Remove every member even if they are offline: the island row must not keep ghosts.
                    for (UUID memberId : oldIsland.getMemberSet()) {
                        plugin.getIslands().removePlayer(oldIsland, memberId);
                    }
                    plugin.getIslands().deleteIsland(oldIsland, true, user.getUniqueId());
                    if (oldIsland.isOwned()) {
                        // A listener vetoed the delete. The reset already delivered the new island, so
                        // keeping this one owned would leave the player with two islands.
                        plugin.logError("Old island was not deleted after reset, orphaning " + oldIsland.getUniqueId());
                        plugin.getIslands().orphanIsland(oldIsland);
                    }
                } else if (!oldIsland.isDeletable()) {
                    // Someone else removed the owner (admin delete, kick) but left the island in
                    // the grid. It must still be marked for the purge, or it occupies its slot forever.
                    plugin.getIslands().orphanIsland(oldIsland);
                }
            }

            if (!tidyUpDone) {
                tidyUpDone = true;
                try {
                    // Fire exit event for plugins/addons
                    IslandEvent.builder().involvedPlayer(user.getUniqueId())
                            .reason(reason == Reason.RESET ? Reason.RESETTED : Reason.CREATED).island(island)
                            .location(island.getCenter()).oldIsland(oldIsland).build();
                } finally {
                    // A listener that throws must not lock this player out of creating islands.
                    if (creationGuardHeld) CREATING.remove(user.getUniqueId());
                }
            }
        }));
    }

    /** Removes every member from the old island and tells them they were kicked. */
    private CompletableFuture<Void> clearOldIslandMembers(Island oldIsland) {
        UUID ownerId = oldIsland.getOwner();
        var tasks = oldIsland.getMemberSet().stream().map(memberId -> {
            User member = User.getInstance(memberId);
            // Notify and clean up separately: an offline member's user task fails
            // ("Player disconnected"), but their data still has to be reset.
            CompletableFuture<Void> notified = TaskScheduler.userFuture(plugin, member, () -> {
                if (!memberId.equals(ownerId)) {
                    member.sendMessage("commands.island.reset.kicked-from-island", TextVariables.GAMEMODE,
                            addon.getDescription().getName());
                }
            }).exceptionally(error -> null);
            return notified
                    .thenCompose(ignored -> plugin.getPlayers().cleanLeavingPlayerAsync(world, member, false, oldIsland))
                    .thenRun(() -> TeamEvent.builder().island(oldIsland).reason(TeamEvent.Reason.DELETE)
                            .involvedPlayer(memberId).build())
                    .exceptionally(error -> null);
        }).toArray(CompletableFuture<?>[]::new);
        return CompletableFuture.allOf(tasks);
    }
}
