package world.bentobox.bentobox.managers.island;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.Vector;

import world.bentobox.bentobox.BStats;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;

/**
 * Create and paste a new island
 * @author tastybento
 *
 */
public class NewIsland {
    private final BentoBox plugin;
    private Island island;
    private final User user;
    private final Reason reason;
    private final World world;
    private String name;
    private final boolean noPaste;
    private final GameModeAddon addon;

    private NewIslandLocationStrategy locationStrategy;

    public NewIsland(Builder builder) throws IOException {
        plugin = BentoBox.getInstance();
        this.user = builder.user2;
        this.reason = builder.reason2;
        this.world = builder.world2;
        this.name = builder.name2;
        this.noPaste = builder.noPaste2;
        this.addon = builder.addon2;
        this.locationStrategy = builder.locationStrategy2;

        if (this.locationStrategy == null) {
            this.locationStrategy = new DefaultNewIslandLocationStrategy();
        }
        // Fire pre-create event
        IslandBaseEvent event = IslandEvent.builder()
                .involvedPlayer(user.getUniqueId())
                .reason(Reason.PRECREATE)
                .build();
        if (event.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(event.isCancelled())) {
            // Do nothing
            return;
        }
        newIsland(builder.oldIsland2);
    }

    /**
     * @return the island that was created
     */
    public Island getIsland() {
        return island;
    }

    /**
     * Start building a new island
     * @return New island builder object
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Build a new island for a player
     * @author tastybento
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

        public Builder oldIsland(Island oldIsland) {
            this.oldIsland2 = oldIsland;
            this.world2 = oldIsland.getWorld();
            return this;
        }


        public Builder player(User player) {
            this.user2 = player;
            return this;
        }

        /**
         * Sets the reason
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
         * Set the addon
         * @param addon a game mode addon
         */
        public Builder addon(GameModeAddon addon) {
            this.addon2 = addon;
            this.world2 = addon.getOverWorld();
            return this;
        }

        /**
         * No blocks will be pasted
         */
        public Builder noPaste() {
            this.noPaste2 = true;
            return this;
        }

        /**
         * @param name - name of Blueprint bundle
         */
        public Builder name(String name) {
            this.name2 = name;
            return this;
        }

        /**
         * @param strategy - the location strategy to use
         * @since 1.8.0
         */
        public Builder locationStrategy(NewIslandLocationStrategy strategy) {
            this.locationStrategy2 = strategy;
            return this;
        }

        /**
         * @return Island
         * @throws IOException - if there are insufficient parameters, i.e., no user
         */
        public Island build() throws IOException {
            if (user2 != null) {
                NewIsland newIsland = new NewIsland(this);
                return newIsland.getIsland();
            }
            throw new IOException("Insufficient parameters. Must have a user!");
        }
    }

    /**
     * Makes an island.
     * @param oldIsland old island that is being replaced, if any
     * @throws IOException - if an island cannot be made. Message is the tag to show the user.
     */
    public void newIsland(Island oldIsland) throws IOException {
        // Find the new island location
        Location next = checkReservedIsland();
        if (next == null) {
            next = this.makeNextIsland();
        }
        // Clean up the user
        cleanUpUser(next);
        // Fire event
        IslandBaseEvent event = IslandEvent.builder()
                .involvedPlayer(user.getUniqueId())
                .reason(reason)
                .island(island)
                .location(island.getCenter())
                .blueprintBundle(plugin.getBlueprintsManager().getBlueprintBundles(addon).get(name))
                .oldIsland(oldIsland)
                .build();
        if (event.getNewEvent().map(IslandBaseEvent::isCancelled).orElse(event.isCancelled())) {
            // Do nothing
            return;
        }
        event = event.getNewEvent().orElse(event);
        // Get the new BlueprintBundle if it was changed
        switch (reason) {
        case CREATE -> name = ((IslandCreateEvent) event).getBlueprintBundle().getUniqueId();
        case RESET -> name = ((IslandResetEvent) event).getBlueprintBundle().getUniqueId();
        default -> {
            // Do nothing of other cases
        }
        }

        // Run task to run after creating the island in one tick if island is not being pasted
        if (noPaste) {
            Bukkit.getScheduler().runTask(plugin, () -> postCreationTask(oldIsland));
        } else {
            // Create islands, then run task
            plugin.getBlueprintsManager().paste(addon, island, name, () -> postCreationTask(oldIsland));
        }
        // Set default settings
        island.setFlagsDefaults();
        // Register metrics
        plugin.getMetrics().ifPresent(BStats::increaseIslandsCreatedCount);
        // Save island
        plugin.getIslands().save(island);
    }

    /**
     * Tasks to run after the new island has been created
     * @param oldIsland - old island that will be deleted
     */
    private void postCreationTask(Island oldIsland) {
        // Set initial spawn point if one exists
        if (island.getSpawnPoint(Environment.NORMAL) != null) {
            plugin.getIslands().setHomeLocation(user, island.getSpawnPoint(Environment.NORMAL));
        }
        // Stop the player from falling or moving if they are
        if (user.isOnline()) {
            if (reason.equals(Reason.RESET) || (reason.equals(Reason.CREATE) && plugin.getIWM().isTeleportPlayerToIslandUponIslandCreation(world))) {
                user.getPlayer().setVelocity(new Vector(0, 0, 0));
                user.getPlayer().setFallDistance(0F);
                // Teleport player after this island is built
                plugin.getIslands().homeTeleportAsync(world, user.getPlayer(), true).thenRun(() -> tidyUp(oldIsland));
                return;
            } else {
                // let's send him a message so that he knows he can teleport to his island!
                user.sendMessage("commands.island.create.you-can-teleport-to-your-island");
            }
        } else {
            // Remove the player again to completely clear the data
            User.removePlayer(user.getPlayer());
        }
        tidyUp(oldIsland);
    }

    /**
     * Cleans up a user before moving them to a new island.
     * Removes any old home locations. Sets the next home location. Resets deaths.
     * Checks range permissions and saves the player to the database.
     * @param loc - the new island location
     */
    private void cleanUpUser(Location loc) {
        // Set home location
        plugin.getIslands().setHomeLocation(user, new Location(loc.getWorld(), loc.getX() + 0.5D, loc.getY(), loc.getZ() + 0.5D));
        // Reset deaths
        if (plugin.getIWM().isDeathsResetOnNewIsland(world)) {
            plugin.getPlayers().setDeaths(world, user.getUniqueId(), 0);
        }
        // Check if owner has a different range permission than the island size
        island.setProtectionRange(user.getPermissionValue(plugin.getIWM().getAddon(island.getWorld())
                .map(GameModeAddon::getPermissionPrefix).orElse("") + "island.range", island.getProtectionRange()));
        // Save the player so that if the server crashes weird things won't happen
        plugin.getPlayers().save(user.getUniqueId());
    }

    /**
     * Get the next island location and add it to the island grid
     * @return location of new island
     * @throws IOException - if there are no unoccupied spots or the island could not be added to the grid
     */
    private Location makeNextIsland() throws IOException {
        // If the reservation fails, then we need to make a new island anyway
        Location next = this.locationStrategy.getNextLocation(world);
        if (next == null) {
            plugin.logError("Failed to make island - no unoccupied spot found.");
            plugin.logError("If the world was imported, try multiple times until all unowned islands are known.");
            throw new IOException("commands.island.create.cannot-create-island");
        }
        // Add to the grid
        island = plugin.getIslands().createIsland(next, user.getUniqueId());
        if (island == null) {
            plugin.logError("Failed to make island! Island could not be added to the grid.");
            throw new IOException("commands.island.create.unable-create-island");
        }
        return next;
    }

    /**
     * Get the reserved island location
     * @return reserved island location, or null if none found
     */
    private Location checkReservedIsland() {
        if (plugin.getIslands().hasIsland(world, user)) {
            // Island exists, it just needs pasting
            island = plugin.getIslands().getIsland(world, user);
            if (island != null && island.isReserved()) {
                Location l = island.getCenter();
                // Clear the reservation
                island.setReserved(false);
                return l;
            } else {
                // This should never happen unless we allow another way to paste over islands without reserving
                plugin.logError("New island for user " + user.getName() + " was not reserved!");
            }
        }
        return null;
    }

    private void tidyUp(Island oldIsland) {
        // Delete old island
        if (oldIsland != null && !plugin.getSettings().isKeepPreviousIslandOnReset()) {
            // Delete the old island
            plugin.getIslands().deleteIsland(oldIsland, true, user.getUniqueId());
        }

        // Fire exit event
        IslandEvent.builder()
        .involvedPlayer(user.getUniqueId())
        .reason(reason == Reason.RESET ? Reason.RESETTED : Reason.CREATED)
        .island(island)
        .location(island.getCenter())
        .oldIsland(oldIsland)
        .build();

    }
}