package us.tastybento.bskyblock.database.managers;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.api.commands.User;
import us.tastybento.bskyblock.database.BSBDatabase;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.database.objects.Players;

public class PlayersManager{

    private static final boolean DEBUG = true;
    private BSkyBlock plugin;
    private BSBDatabase database;
    private AbstractDatabaseHandler<Players> handler;

    private Map<UUID, Players> playerCache;
    private Set<UUID> inTeleport;

    /**
     * Provides a memory cache of online player information
     * This is the one-stop-shop of player info
     * If the player is not cached, then a request is made to Players to obtain it
     *
     * @param plugin - BSkyBlock plugin object
     */
    @SuppressWarnings("unchecked")
    public PlayersManager(BSkyBlock plugin){
        this.plugin = plugin;
        database = BSBDatabase.getDatabase();
        // Set up the database handler to store and retrieve Players classes
        handler = (AbstractDatabaseHandler<Players>) database.getHandler(Players.class);
        playerCache = new HashMap<>();
        inTeleport = new HashSet<>();
    }


    /**
     * Load all players - not normally used as to load all players into memory will be wasteful
     */
    public void load(){
        playerCache.clear();
        inTeleport.clear();
        try {
            for (Players player : handler.loadObjects()) {
                playerCache.put(player.getPlayerUUID(), player);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load players from the database!" + e.getMessage());
        }
    }

    /**
     * Save all players
     * @param async - if true, save async
     */
    public void save(boolean async){
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: saving " + async);
        }
        Collection<Players> set = Collections.unmodifiableCollection(playerCache.values());
        if(async){
            Runnable save = () -> {
                for(Players player : set){
                    if (DEBUG) {
                        plugin.getLogger().info("DEBUG: saving player " + player.getPlayerName() + " "+ player.getUniqueId());
                    }
                    try {
                        handler.saveObject(player);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Could not save player " + player.getPlayerName() + " "+ player.getUniqueId() + " " + e.getMessage());
                    }
                }
            };
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, save);
        } else {
            for(Players player : set){
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: saving player " + player.getPlayerName() + " "+ player.getUniqueId());
                }
                try {
                    handler.saveObject(player);
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not save player " + player.getPlayerName() + " "+ player.getUniqueId() + " " + e.getMessage());
                }
            }
        }
    }

    public void shutdown(){
        save(false);
        playerCache.clear();
    }

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
     * Adds a player to the cache
     * @param playerUUID - the player's UUID
     * @return the players object
     */
    public Players addPlayer(final UUID playerUUID) {
        if (playerUUID == null) {
            return null;
        }
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: adding player " + playerUUID);
        }
        if (!playerCache.containsKey(playerUUID)) {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: player not in cache");
            }
            Players player = null;
            // If the player is in the database, load it, otherwise create a new player
            if (handler.objectExists(playerUUID.toString())) {
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: player in database");
                }
                try {
                    player = handler.loadObject(playerUUID.toString());
                } catch (Exception e) {
                    plugin.getLogger().severe("Could not load player " + playerUUID + " " + e.getMessage());
                }
            } else {
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: new player");
                }
                player = new Players(plugin, playerUUID);
            }
            playerCache.put(playerUUID, player);
            return player;
        } else {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: known player");
            }
            return playerCache.get(playerUUID);
        }
    }

    /**
     * Stores the player's info and removes the player from the cache
     *
     * @param player - the player - UUID of player
     *
     */
    public void removeOnlinePlayer(final UUID player) {
        save(player);
        playerCache.remove(player);
    }

    /**
     * Saves all players on the server and clears the cache
     */
    public void removeAllPlayers() {
        for (UUID pl : playerCache.keySet()) {
            save(pl);
        }
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
    public boolean isKnown(final UUID uniqueID) {
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
     * Returns the player object for the named player
     *
     * @param playerUUID - the player's UUID
     *            - String name of player
     * @return - player object
     */
    public Players get(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID);
    }

    /**
     * Checks if player has an island.
     *
     * @param playerUUID - the player's UUID
     *            - string name of player
     * @return true if player has island
     */
    public boolean hasIsland(final UUID playerUUID) {
        addPlayer(playerUUID);
        return plugin.getIslands().hasIsland(playerUUID);
    }

    /**
     * Checks if player is in a Team from cache if available
     *
     * @param playerUUID - the player's UUID
     * @return true if player in a team
     */
    public boolean inTeam(UUID playerUUID) {
        addPlayer(playerUUID);
        return plugin.getIslands().getMembers(playerUUID).size() > 1;
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
        //this.save(true);
    }

    /**
     * Set the default home location for player
     * @param playerUUID - the player's UUID
     * @param location - the location
     */
    public void setHomeLocation(UUID playerUUID, Location location) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).setHomeLocation(location,1);
        //this.save(true);
    }

    /**
     * Clears any home locations for player
     * @param playerUUID - the player's UUID
     */
    public void clearHomeLocations(UUID playerUUID) {
        addPlayer(playerUUID);
        playerCache.get(playerUUID).clearHomeLocations();
    }

    /**
     * Returns the home location, or null if none
     *
     * @param playerUUID - the player's UUID
     * @param number - a number
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID, int number) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(number);
    }

    /**
     * Gets the default home location for player
     * @param playerUUID - the player's UUID
     * @return Home location or null if none
     */
    public Location getHomeLocation(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocation(1);
    }

    /**
     * Provides all home locations for player
     * @param playerUUID - the player's UUID
     * @return List of home locations
     */
    public Map<Integer, Location> getHomeLocations(UUID playerUUID) {
        addPlayer(playerUUID);
        return playerCache.get(playerUUID).getHomeLocations();
    }

    /**
     * Attempts to return a UUID for a given player's name.
     * @param string
     * @return UUID of player or null if unknown
     */
    @SuppressWarnings("deprecation")
    public UUID getUUID(String string) {
        // See if this is a UUID
        try {
            return UUID.fromString(string);
        } catch (Exception e) {}
        // Look in the name cache
        return Bukkit.getOfflinePlayer(string).getUniqueId();
    }

    /**
     * Sets the player's name and updates the name>UUID database
     * @param user - the User
     */
    public void setPlayerName(User user) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: Setting player name to " + user.getName() + " for " + user.getUniqueId());
        }
        addPlayer(user.getUniqueId());
        playerCache.get(user.getUniqueId()).setPlayerName(user.getName());
    }

    /**
     * Obtains the name of the player from their UUID
     * Player must have logged into the game before
     *
     * @param playerUUID - the player's UUID
     * @return String - playerName
     */
    public String getName(UUID playerUUID) {
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: Geting player name");
        }
        if (playerUUID == null) {
            return "";
        }
        addPlayer(playerUUID);
        if (DEBUG) {
            plugin.getLogger().info("DEBUG: name is " + playerCache.get(playerUUID).getPlayerName());
        }
        return playerCache.get(playerUUID).getPlayerName();
    }

    /**
     * Reverse lookup - returns the owner of an island from the location
     *
     * @param loc - location
     * @return UUID of owner of island
     */
    public UUID getPlayerFromIslandLocation(Location loc) {
        if (loc == null) {
            return null;
        }
        // Look in the grid
        Optional<Island> island = plugin.getIslands().getIslandAt(loc);
        return island.map(Island::getOwner).orElse(null);
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
     * Ban target from a player's island. Ban may be blocked by event being cancelled.
     * @param playerUUID - the player's UUID
     * @param targetUUID - the target's UUID
     * @return true if banned, false if not
     */
    public boolean ban(UUID playerUUID, UUID targetUUID) {
        addPlayer(playerUUID);
        addPlayer(targetUUID);
        Island island = plugin.getIslands().getIsland(playerUUID);
        if (island != null) {
            // Player has island
            return island.addToBanList(targetUUID);
        }
        return false;
    }

    /**
     * Unban target from player's island
     * @param playerUUID - the player's UUID
     * @param targetUUID - the target's UUID
     * @return true if target sucessfully unbanned
     */
    public boolean unban(UUID playerUUID, UUID targetUUID) {
        addPlayer(playerUUID);
        addPlayer(targetUUID);
        Island island = plugin.getIslands().getIsland(playerUUID);
        if (island != null) {
            // Player has island
            return island.removeFromBanList(targetUUID);
        }
        return false;
    }

    /**
     * @param playerUUID - the player's UUID
     * @param targetUUID - the target's UUID
     * @return true if target is banned from player's island
     */
    public boolean isBanned(UUID playerUUID, UUID targetUUID) {
        if (playerUUID == null || targetUUID == null) {
            // If the island is unowned, then playerUUID could be null
            return false;
        }
        addPlayer(playerUUID);
        addPlayer(targetUUID);
        // Check if the target player has a permission bypass (admin.noban)
        Player target = plugin.getServer().getPlayer(targetUUID);
        if (target != null && target.hasPermission(Constants.PERMPREFIX + "admin.noban")) {
            return false;
        }
        Island island = plugin.getIslands().getIsland(playerUUID);
        if (island != null) {
            // Player has island
            return island.isBanned(targetUUID);
        }
        return false;
    }

    /**
     * Clears resets for online players or players in the cache
     * @param resetLimit
     */
    public void clearResets(int resetLimit) {
        for (Players player : playerCache.values()) {
            player.setResetsLeft(resetLimit);
        }
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
     * Resets everything to do with a player that needs to be reset
     * @param player - the player
     */
    public void resetPlayer(Player player) {
        // TODO Auto-generated method stub

    }

    /**
     * Saves the player to the database
     * @param playerUUID - the player's UUID
     */
    public void save(UUID playerUUID) {
        if (playerCache.containsKey(playerUUID)) {
            final Players player = playerCache.get(playerUUID);
            try {
                if (DEBUG) {
                    plugin.getLogger().info("DEBUG: saving player by uuid " + player.getPlayerName() + " " + playerUUID + " saved");
                }
                handler.saveObject(player);

            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException | SecurityException
                    | InstantiationException | NoSuchMethodException
                    | IntrospectionException | SQLException e) {
                plugin.getLogger().severe("Could not save player to database: " + playerUUID + " " + e.getMessage());
            }
        } else {
            if (DEBUG) {
                plugin.getLogger().info("DEBUG: " + playerUUID + " is not in the cache to save");
            }
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
