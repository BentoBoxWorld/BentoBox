package world.bentobox.bentobox.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.scheduler.BukkitRunnable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Names;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.util.Util;

public class PlayersManager {

    private final BentoBox plugin;
    private Database<Players> handler;
    private final Database<Names> names;

    private final Map<UUID, Players> playerCache;
    private final Set<UUID> inTeleport;

    private boolean isSaveTaskRunning;

    /**
     * Provides a memory cache of online player information
     * This is the one-stop-shop of player info
     * If the player is not cached, then a request is made to Players to obtain it
     *
     * @param plugin - plugin object
     */
    public PlayersManager(BentoBox plugin){
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Players classes
        handler = new Database<>(plugin, Players.class);
        // Set up the names database
        names = new Database<>(plugin, Names.class);
        playerCache = new HashMap<>();
        inTeleport = new HashSet<>();
    }

    /**
     * Used only for testing. Sets the database to a mock database.
     * @param handler - handler
     */
    public void setHandler(Database<Players> handler) {
        this.handler = handler;
    }

    /**
     * Load all players - not normally used as to load all players into memory will be wasteful
     */
    public void load(){
        playerCache.clear();
        inTeleport.clear();
        handler.loadObjects().forEach(p -> playerCache.put(p.getPlayerUUID(), p));
    }

    public boolean isSaveTaskRunning() {
        return isSaveTaskRunning;
    }

    /**
     * Save all players
     */
    public void saveAll() {
        saveAll(false);
    }

    /**
     * Save all players
     * @param schedule true if we should let the task run over multiple ticks to reduce lag spikes
     */
    public void saveAll(boolean schedule){
        if (!schedule) {
            for (Players player : playerCache.values()) {
                try {
                    handler.saveObjectAsync(player);
                } catch (Exception e) {
                    plugin.logError("Could not save player to database when running sync! " + e.getMessage());
                }
            }
            return;
        }

        isSaveTaskRunning = true;
        Queue<Players> queue = new LinkedList<>(playerCache.values());
        new BukkitRunnable() {
            @Override
            public void run() {
                for (int i = 0; i < plugin.getSettings().getMaxSavedPlayersPerTick(); i++) {
                    Players player = queue.poll();
                    if (player == null) {
                        isSaveTaskRunning = false;
                        cancel();
                        return;
                    }
                    try {
                        handler.saveObjectAsync(player);
                    } catch (Exception e) {
                        plugin.logError("Could not save player to database when running sync! " + e.getMessage());
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void shutdown(){
        saveAll();
        playerCache.clear();
        handler.close();
    }

    /**
     * Get player by UUID. Adds player to cache if not in there already
     * @param uuid of player
     * @return player object or null if it does not exist, for example the UUID is null
     */
    @Nullable
    public Players getPlayer(UUID uuid){
        if (!playerCache.containsKey(uuid)) {
            addPlayer(uuid);
        }
        return playerCache.get(uuid);
    }

    /**
     * Returns an <strong>unmodifiable collection</strong> of all the players that are <strong>currently in the cache</strong>.
     * @return unmodifiable collection containing every player in the cache.
     * @since 1.1
     */
    @NonNull
    public Collection<Players> getPlayers() {
        return Collections.unmodifiableCollection(playerCache.values());
    }

    /*
     * Cache control methods
     */

    /**
     * Adds a player to the cache. If the UUID does not exist, a new player is made
     * @param playerUUID - the player's UUID
     */
    public void addPlayer(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }
        if (!playerCache.containsKey(playerUUID)) {
            Players player;
            // If the player is in the database, load it, otherwise create a new player
            if (handler.objectExists(playerUUID.toString())) {
                player = handler.loadObject(playerUUID.toString());
                if (player == null) {
                    player = new Players(plugin, playerUUID);
                    // Corrupted database entry
                    plugin.logError("Corrupted player database entry for " + playerUUID + " - unrecoverable. Recreated.");
                    player.setUniqueId(playerUUID.toString());
                }
            } else {
                player = new Players(plugin, playerUUID);
            }
            playerCache.put(playerUUID, player);
        }
    }

    /**
     * Checks if the player is known or not.
     * Will check not just the cache but if the object but in the database too.
     *
     * @param uniqueID - unique ID
     * @return true if player is known, otherwise false
     */
    public boolean isKnown(UUID uniqueID) {
        return uniqueID != null && (playerCache.containsKey(uniqueID) || handler.objectExists(uniqueID.toString()));
    }

    /**
     * Sets the home location for the player
     * @param user - the player
     * @param location - the location
     * @param number - a number - 1 is default. Can be any number.
     * @deprecated Use {@link IslandsManager#setHomeLocation(User, Location, String)}
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public void setHomeLocation(User user, Location location, int number) {
        setHomeLocation(user.getUniqueId(), location,number);
    }

    /**
     * Sets the home location for the player
     * @param playerUUID - the player's UUID
     * @param location - the location
     * @param number - a number - 1 is default. Can be any number.
     * @deprecated Use {@link IslandsManager#setHomeLocation(UUID, Location, String)}
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public void setHomeLocation(UUID playerUUID, Location location, int number) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,number);
    }

    /**
     * Set the default home location for player
     * @param playerUUID - the player's UUID
     * @param location - the location
     * @deprecated Use {@link IslandsManager#setHomeLocation(UUID, Location)}
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public void setHomeLocation(UUID playerUUID, Location location) {
        setHomeLocation(playerUUID, location,1);
    }

    /**
     * Clears any home locations for player
     * @param world - world
     * @param playerUUID - the player's UUID
     * @deprecated Not used anymore. Home locations are stored on islands.
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public void clearHomeLocations(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).clearHomeLocations(world);
    }

    /**
     * Returns the home location, or null if none
     * @param world - world
     *
     * @param user - the player
     * @param number - a number
     * @return Home location or null if none
     * @deprecated Use {@link IslandsManager#getHomeLocation(World, User, String)}
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public Location getHomeLocation(World world, User user, int number) {
        addPlayer(user.getUniqueId());
        return playerCache.get(user.getUniqueId()).getHomeLocation(world, number);
    }

    /**
     * Returns the home location, or null if none
     * @param world - world
     *
     * @param playerUUID - the player's UUID
     * @param number - a number
     * @return Home location or null if none
     * @deprecated Use {@link IslandsManager#getHomeLocation(World, UUID, String)}
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public Location getHomeLocation(World world, UUID playerUUID, int number) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(world, number);
    }

    /**
     * Gets the default home location for player
     * @param playerUUID - the player's UUID
     * @return Home location or null if none
     * @deprecated Use {@link IslandsManager#getHomeLocation(World, UUID)}
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public Location getHomeLocation(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(world, 1);
    }

    /**
     * Provides all home locations for player
     * @param playerUUID - the player's UUID
     * @return List of home locations
     * @deprecated Use {@link IslandsManager#getHomeLocations(world.bentobox.bentobox.database.objects.Island)}
     */
    @Deprecated(since="1.18.0", forRemoval=true)
    public Map<Location, Integer> getHomeLocations(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocations(world);
    }

    /**
     * Attempts to return a UUID for a given player's name.
     * @param name - name of player
     * @return UUID of player or null if unknown
     */
    @Nullable
    public UUID getUUID(@NonNull String name) {
        // See if this is a UUID
        // example: 5988eecd-1dcd-4080-a843-785b62419abb
        if (name.length() == 36 && name.contains("-")) {
            try {
                return UUID.fromString(name);
            } catch (Exception ignored) {
                // Not used
            }
        }
        // Look in the name cache, then the data base and then give up
        return playerCache.values().stream()
                .filter(p -> p.getPlayerName().equalsIgnoreCase(name)).findFirst()
                .map(p -> UUID.fromString(p.getUniqueId()))
                .orElseGet(() -> names.objectExists(name) ? names.loadObject(name).getUuid() : null);
    }

    /**
     * Sets the player's name and updates the name to UUID database
     * @param user - the User
     */
    public void setPlayerName(@NonNull User user) {
        addPlayer(user.getUniqueId());
        playerCache.get(user.getUniqueId()).setPlayerName(user.getName());
        Names newName = new Names(user.getName(), user.getUniqueId());
        // Add to names database
        names.saveObjectAsync(newName);
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     *
     * @param playerUUID - the player's UUID
     * @return String - playerName, empty string if UUID is null
     */
    @NonNull
    public String getName(@Nullable UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getPlayerName();
    }

    /**
     * Returns how many island resets the player has done.
     * @param world world
     * @param playerUUID the player's UUID
     * @return number of resets
     */
    public int getResets(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getResets(world);
    }

    /**
     * Returns how many island resets the player can still do.
     * @param world world
     * @param playerUUID the player's UUID
     * @return number of resets the player can do (always {@code >= 0}), or {@code -1} if unlimited.
     * @since 1.5.0
     * @see #getResets(World, UUID)
     */
    public int getResetsLeft(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        if (plugin.getIWM().getResetLimit(world) == -1) {
            return -1;
        } else {
            return Math.max(plugin.getIWM().getResetLimit(world) - getResets(world, playerUUID), 0);
        }
    }

    /**
     * Sets how many resets the player has performed
     *
     * @param world world
     * @param playerUUID player's UUID
     * @param resets number of resets to set
     */
    public void setResets(World world, UUID playerUUID, int resets) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setResets(world, resets);
    }

    /**
     * Returns the locale for this player. If missing, will return nothing
     * @param playerUUID - the player's UUID
     * @return name of the locale this player uses
     */
    public String getLocale(UUID playerUUID) {
        addPlayer(playerUUID);
        if (playerUUID == null) {
            return "";
        }
        return playerCache.get(playerUUID).getLocale();
    }

    /**
     * Sets the locale this player wants to use
     * @param playerUUID - the player's UUID
     * @param localeName - locale name, e.g., en-US
     */
    public void setLocale(UUID playerUUID, String localeName) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setLocale(localeName);
    }

    /**
     * Add death to player
     * @param world - world
     * @param playerUUID - the player's UUID
     */
    public void addDeath(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).addDeath(world);
    }

    /**
     * Set death number for player
     * @param world - world
     * @param playerUUID - the player's UUID
     * @param deaths - number of deaths
     */
    public void setDeaths(World world, UUID playerUUID, int deaths) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setDeaths(world, deaths);
    }

    /**
     * Get number of times player has died since counting began
     * @param world - world
     * @param playerUUID - the player's UUID
     * @return number of deaths
     */
    public int getDeaths(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID) == null ? 0 : playerCache.get(playerUUID).getDeaths(world);
    }

    /**
     * Sets if a player is mid-teleport or not
     * @param uniqueId - unique ID
     */
    public void setInTeleport(UUID uniqueId) {
        inTeleport.add(uniqueId);
    }

    /**
     * Removes player from in-teleport
     * @param uniqueId - unique ID
     */
    public void removeInTeleport(UUID uniqueId) {
        inTeleport.remove(uniqueId);
    }

    /**
     * @param uniqueId - unique ID
     * @return true if a player is mid-teleport
     */
    public boolean isInTeleport(UUID uniqueId) {
        return inTeleport.contains(uniqueId);
    }

    /**
     * Saves the player to the database
     * @param playerUUID - the player's UUID
     */
    public void save(UUID playerUUID) {
        if (playerCache.containsKey(playerUUID)) {
            handler.saveObjectAsync(playerCache.get(playerUUID));
        }
    }

    /**
     * Tries to get the user from his name
     * @param name - name
     * @return user - user or null if unknown
     */
    public User getUser(String name) {
        return getUser(getUUID(name));
    }

    /**
     * Tries to get the user from his UUID
     * @param uuid - UUID
     * @return user - user
     */
    public User getUser(UUID uuid) {
        return User.getInstance(uuid);
    }

    /**
     * Adds a reset to this player's number of resets
     * @param world world where island is
     * @param playerUUID player's UUID
     */
    public void addReset(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).addReset(world);
    }

    /**
     * Sets the Flags display mode for the Settings Panel for this player.
     * @param playerUUID player's UUID
     * @param displayMode the {@link Flag.Mode} to set
     * @since 1.6.0
     */
    public void setFlagsDisplayMode(UUID playerUUID, Flag.Mode displayMode) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setFlagsDisplayMode(displayMode);
    }

    /**
     * Returns the Flags display mode for the Settings Panel for this player.
     * @param playerUUID player's UUID
     * @return the {@link Flag.Mode display mode} for the Flags in the Settings Panel.
     * @since 1.6.0
     */
    public Flag.Mode getFlagsDisplayMode(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getFlagsDisplayMode();
    }

    /**
     * Remove player from cache. Clears players with the same name or UUID
     * @param player player to remove
     */
    public void removePlayer(Player player) {
        // Clear any players with the same name
        playerCache.values().removeIf(p -> player.getName().equalsIgnoreCase(p.getPlayerName()));
        // Remove if the player's UUID is the same
        playerCache.values().removeIf(p -> player.getUniqueId().toString().equals(p.getUniqueId()));
    }

    /**
     * Cleans the player when leaving an island
     * @param world - island world
     * @param target - target user
     * @param kicked - true if player is being kicked
     * @since 1.15.4
     */
    public void cleanLeavingPlayer(World world, User target, boolean kicked) {
        // Execute commands when leaving
        Util.runCommands(target, plugin.getIWM().getOnLeaveCommands(world), "leave");

        // Remove any tamed animals
        world.getEntitiesByClass(Tameable.class).stream()
        .filter(Tameable::isTamed)
        .filter(t -> t.getOwner() != null && t.getOwner().getUniqueId().equals(target.getUniqueId()))
        .forEach(t -> t.setOwner(null));

        // Remove money inventory etc.
        if (plugin.getIWM().isOnLeaveResetEnderChest(world)) {
            if (target.isOnline()) {
                target.getPlayer().getEnderChest().clear();
            } else {
                Players p = getPlayer(target.getUniqueId());
                if (p != null) {
                    p.addToPendingKick(world);
                }
            }
        }
        if ((kicked && plugin.getIWM().isOnLeaveResetInventory(world) && !plugin.getIWM().isKickedKeepInventory(world))
                || (!kicked && plugin.getIWM().isOnLeaveResetInventory(world))) {
            if (target.isOnline()) {
                target.getPlayer().getInventory().clear();
            } else {
                Players p = getPlayer(target.getUniqueId());
                if (p != null) {
                    p.addToPendingKick(world);
                }
            }
        }

        if (plugin.getSettings().isUseEconomy() && plugin.getIWM().isOnLeaveResetMoney(world)) {
            plugin.getVault().ifPresent(vault -> vault.withdraw(target, vault.getBalance(target), world));
        }
        // Reset the health
        if (plugin.getIWM().isOnLeaveResetHealth(world)) {
            Util.resetHealth(target.getPlayer());
        }

        // Reset the hunger
        if (plugin.getIWM().isOnLeaveResetHunger(world)) {
            target.getPlayer().setFoodLevel(20);
        }

        // Reset the XP
        if (plugin.getIWM().isOnLeaveResetXP(world)) {
            target.getPlayer().setTotalExperience(0);
        }
        // Save player
        save(target.getUniqueId());
    }

}
