package us.tastybento.bskyblock.managers;

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

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.objects.Names;
import us.tastybento.bskyblock.database.objects.Players;

public class PlayersManager {

    private BSkyBlock plugin;
    private BSBDatabase<Players> handler;
    private BSBDatabase<Names> names;

    private Map<UUID, Players> playerCache;
    private Set<UUID> inTeleport;

    /**
     * Provides a memory cache of online player information
     * This is the one-stop-shop of player info
     * If the player is not cached, then a request is made to Players to obtain it
     *
     * @param plugin - BSkyBlock plugin object
     */
    public PlayersManager(BSkyBlock plugin){
        this.plugin = plugin;
        // Set up the database handler to store and retrieve Players classes
        handler = new BSBDatabase<>(plugin, Players.class);
        // Set up the names database
        names = new BSBDatabase<>(plugin, Names.class);
        playerCache = new HashMap<>();
        inTeleport = new HashSet<>();
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
            Players player = null;
            // If the player is in the database, load it, otherwise create a new player
            if (handler.objectExists(playerUUID.toString())) {
                player = handler.loadObject(playerUUID.toString());
            } else {
                player = new Players(plugin, playerUUID);
            }
            playerCache.put(playerUUID, player);
            return;
        }
    }

    /**
     * Stores the player's info and removes the player from the cache
     *
     * @param playerUUID - the player - UUID of player
     *
     */
    public void removeOnlinePlayer(UUID playerUUID) {
        save(playerUUID);
        playerCache.remove(playerUUID);
    }

    /**
     * Saves all players on the server and clears the cache
     */
    public void removeAllPlayers() {
        playerCache.keySet().forEach(this::save);
        playerCache.clear();
    }

    /*
     * Player info query methods
     */

    /**
     * Checks if the player is known or not
     *
     * @param uniqueID - unique ID
     * @return true if player is know, otherwise false
     */
    public boolean isKnown(UUID uniqueID) {
        if (uniqueID == null) {
            return false;
        }
        // Try cache
        if (playerCache.containsKey(uniqueID)) {
            return true;
        } else {
            // Get from the database - do not add to cache yet
            return handler.objectExists(uniqueID.toString());
        }
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
     * @param world 
     * @param playerUUID - the player's UUID
     */
    public void clearHomeLocations(World world, UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).clearHomeLocations(world);
    }

    /**
     * Returns the home location, or null if none
     * @param world 
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
     * @param world 
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
     * @param string
     * @return UUID of player or null if unknown
     */
    @SuppressWarnings("deprecation")
    public UUID getUUID(String string) {
        // See if this is a UUID
        // example: 5988eecd-1dcd-4080-a843-785b62419abb
        if (string.length() == 36 && string.contains("-")) {
            try {
                return UUID.fromString(string);
            } catch (Exception e) {}
        }
        // Look in the name cache, then the data base and finally Bukkit blocking request
        return playerCache.values().stream()
                .filter(p -> p.getPlayerName().equalsIgnoreCase(string)).findFirst()
                .map(p -> UUID.fromString(p.getUniqueId()))
                .orElse(names.objectExists(string) 
                        ? names.loadObject(string).getUuid() 
                                : Bukkit.getOfflinePlayer(string).getUniqueId());
    }

    /**
     * Sets the player's name and updates the name>UUID database
     * @param user - the User
     */
    public void setPlayerName(User user) {
        addPlayer(user.getUniqueId());
        playerCache.get(user.getUniqueId()).setPlayerName(user.getName());
        // Add to names database
        names.saveObject(new Names(user.getName(), user.getUniqueId()));
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     *
     * @param playerUUID - the player's UUID
     * @return String - playerName
     */
    public String getName(UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getPlayerName();
    }

    /**
     * Gets how many island resets the player has left
     *
     * @param playerUUID - the player's UUID
     * @return number of resets
     */
    public int getResetsLeft(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getResetsLeft();
    }

    /**
     * Sets how many resets the player has left
     *
     * @param playerUUID - the player's UUID
     * @param resets
     */
    public void setResetsLeft(UUID playerUUID, int resets) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setResetsLeft(resets);
    }

    /**
     * Returns how long the player must wait before they can be invited to an
     * island with the location
     *
     * @param playerUUID - the player's UUID
     * @param location - the location
     * @return time to wait in minutes/hours
     */
    public long getInviteCoolDownTime(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getInviteCoolDownTime(location);
    }

    /**
     * Starts the timer for the player for this location before which they can
     * be invited
     * Called when they are kicked from an island or leave.
     *
     * @param playerUUID - the player's UUID
     * @param location - the location
     */
    public void startInviteCoolDownTimer(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).startInviteCoolDownTimer(location);
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
     * @param localeName
     */
    public void setLocale(UUID playerUUID, String localeName) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setLocale(localeName);
    }

    /**
     * Add death to player
     * @param playerUUID - the player's UUID
     */
    public void addDeath(UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).addDeath();
    }

    /**
     * Set death number for player
     * @param playerUUID - the player's UUID
     * @param deaths
     */
    public void setDeaths(UUID playerUUID, int deaths) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setDeaths(deaths);
    }

    /**
     * Get number of times player has died in BSkyBlock worlds since counting began
     * @param playerUUID - the player's UUID
     * @return number of deaths
     */
    public int getDeaths(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getDeaths();
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
     * Tries to get the user from this name
     * @param string
     * @return user
     */
    public User getUser(String string) {
        return User.getInstance(getUUID(string));
    }

}
