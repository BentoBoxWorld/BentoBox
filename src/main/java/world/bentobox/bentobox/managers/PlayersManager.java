package world.bentobox.bentobox.managers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.Database;
import world.bentobox.bentobox.database.objects.Names;
import world.bentobox.bentobox.database.objects.Players;

public class PlayersManager {

    private BentoBox plugin;
    private Database<Players> handler;
    private Database<Names> names;

    private Map<UUID, Players> playerCache;
    private Set<UUID> inTeleport;

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

    /**
     * Save all players
     * @param async - if true, save async
     */
    public void save(boolean async){
        Collection<Players> set = Collections.unmodifiableCollection(playerCache.values());
        if(async) {
            Runnable save = () -> set.forEach(handler::saveObject);
            Bukkit.getScheduler().runTaskAsynchronously(plugin, save);
        } else {
            set.forEach(handler::saveObject);
        }
    }

    public void shutdown(){
        save(false);
        playerCache.clear();
        handler.close();
    }

    /**
     * Get player by UUID. Adds player to cache if not in there already
     * @param uuid of player
     * @return player object or null if it does not exist
     */
    public Players getPlayer(UUID uuid){
        if (!playerCache.containsKey(uuid)) {
            addPlayer(uuid);
        }
        return playerCache.get(uuid);
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
            } else {
                player = new Players(plugin, playerUUID);
            }
            playerCache.put(playerUUID, player);
        }
    }

    /**
     * Checks if the player is known or not. Will check not just the cache but if the object is
     * in the database too.
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
     */
    public void setHomeLocation(User user, Location location, int number) {
        addPlayer(user.getUniqueId());
        playerCache.get(user.getUniqueId()).setHomeLocation(location,number);
    }

    /**
     * Sets the home location for the player
     * @param playerUUID - the player's UUID
     * @param location - the location
     * @param number - a number - 1 is default. Can be any number.
     */
    public void setHomeLocation(UUID playerUUID, Location location, int number) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,number);
    }

    /**
     * Set the default home location for player
     * @param playerUUID - the player's UUID
     * @param location - the location
     */
    public void setHomeLocation(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,1);
    }

    /**
     * Clears any home locations for player
     * @param world - world
     * @param playerUUID - the player's UUID
     */
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
     */
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
     */
    public Location getHomeLocation(World world, UUID playerUUID, int number) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(world, number);
    }

    /**
     * Gets the default home location for player
     * @param playerUUID - the player's UUID
     * @return Home location or null if none
     */
    public Location getHomeLocation(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(world, 1);
    }

    /**
     * Provides all home locations for player
     * @param playerUUID - the player's UUID
     * @return List of home locations
     */
    public Map<Location, Integer> getHomeLocations(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocations(world);
    }

    /**
     * Attempts to return a UUID for a given player's name.
     * @param name - name of player
     * @return UUID of player or null if unknown
     */
    public UUID getUUID(String name) {
        // See if this is a UUID
        // example: 5988eecd-1dcd-4080-a843-785b62419abb
        if (name.length() == 36 && name.contains("-")) {
            try {
                return UUID.fromString(name);
            } catch (Exception ignored) {}
        }
        // Look in the name cache, then the data base and then give up
        return playerCache.values().stream()
                .filter(p -> p.getPlayerName().equalsIgnoreCase(name)).findFirst()
                .map(p -> UUID.fromString(p.getUniqueId()))
                .orElse(names.objectExists(name) ? names.loadObject(name).getUuid() : null);
    }

    /**
     * Sets the player's name and updates the name>UUID database
     * @param user - the User
     */
    public void setPlayerName(User user) {
        addPlayer(user.getUniqueId());
        playerCache.get(user.getUniqueId()).setPlayerName(user.getName());
        Names newName = new Names(user.getName(), user.getUniqueId());
        // Add to names database
        names.saveObject(newName);
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     *
     * @param playerUUID - the player's UUID
     * @return String - playerName, empty string if UUID is null
     */
    public String getName(UUID playerUUID) {
        if (playerUUID == null) {
            return "";
        }
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getPlayerName();
    }

    /**
     * Gets how many island resets the player has done
     * @param world - world
     *
     * @param playerUUID - the player's UUID
     * @return number of resets
     */
    public int getResets(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getResets(world);
    }

    /**
     * Sets how many resets the player has performed
     *
     * @param world - world
     * @param playerUUID - the player's UUID
     * @param resets - number of resets
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
        return playerCache.get(playerUUID).getDeaths(world);
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
            handler.saveObject(playerCache.get(playerUUID));
        }
    }

    /**
     * Tries to get the user from his name
     * @param name - name
     * @return user - user
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


    public void addReset(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).addReset(world);

    }

}
