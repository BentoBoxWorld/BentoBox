package world.bentobox.bentobox.database.objects;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

/**
 * Tracks the following info on the player
 *
 * @author tastybento
 */
public class Players implements DataObject {
    @Expose
    private Map<Location, Integer> homeLocations = new HashMap<>();
    @Expose
    private String uniqueId;
    @Expose
    private String playerName;
    @Expose
    private Map<String, Integer> resets = new HashMap<>();
    @Expose
    private String locale = "";
    @Expose
    private Map<String, Integer> deaths = new HashMap<>();

    /**
     * This is required for database storage
     */
    public Players() {}

    /**
     * @param plugin - plugin object
     * @param uniqueId - unique ID
     *            Constructor - initializes the state variables
     *
     */
    public Players(BentoBox plugin, UUID uniqueId) {
        this.uniqueId = uniqueId.toString();
        homeLocations = new HashMap<>();
        locale = "";
        // Try to get player's name
        this.playerName = Bukkit.getOfflinePlayer(uniqueId).getName();
        if (this.playerName == null) {
            this.playerName = uniqueId.toString();
        }
    }

    /**
     * Gets the default home location.
     * @param world - world to check
     * @return Location - home location in world
     */
    public Location getHomeLocation(World world) {
        return getHomeLocation(world, 1); // Default
    }

    /**
     * Gets the home location by number for world
     * @param world - includes world and any related nether or end worlds
     * @param number - a number
     * @return Location of this home or null if not available
     */
    public Location getHomeLocation(World world, int number) {
        return homeLocations.entrySet().stream()
                .filter(en -> Util.sameWorld(en.getKey().getWorld(), world) && en.getValue() == number)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * @param world - world
     * @return List of home locations
     */
    public Map<Location, Integer> getHomeLocations(World world) {
        return homeLocations.entrySet().stream().filter(e -> Util.sameWorld(e.getKey().getWorld(),world))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @return the homeLocations
     */
    public Map<Location, Integer> getHomeLocations() {
        return homeLocations;
    }

    /**
     * @param homeLocations the homeLocations to set
     */
    public void setHomeLocations(Map<Location, Integer> homeLocations) {
        this.homeLocations = homeLocations;
    }

    /**
     * @param playerName the playerName to set
     */
    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(UUID.fromString(uniqueId));
    }

    public UUID getPlayerUUID() {
        return UUID.fromString(uniqueId);
    }

    public String getPlayerName() {
        return playerName;
    }

    /**
     * Get number of resets done in this world
     * @param world - world
     * @return the resetsLeft
     */
    public int getResets(World world) {
        resets.putIfAbsent(world.getName(), 0);
        return resets.get(world.getName());
    }

    /**
     * @return the resets
     */
    public Map<String, Integer> getResets() {
        return resets;
    }

    /**
     * @param resets the resets to set
     */
    public void setResets(Map<String, Integer> resets) {
        this.resets = resets;
    }

    /**
     * @param resets
     *            the resets to set
     */
    public void setResets(World world, int resets) {
        this.resets.put(world.getName(), resets);
    }

    /**
     * Stores the home location of the player in a String format
     *
     * @param l
     *            a Bukkit location
     */
    public void setHomeLocation(final Location l) {
        setHomeLocation(l, 1);
    }

    /**
     * Stores the numbered home location of the player. Numbering starts at 1.
     * @param location - the location
     * @param number - a number
     */
    public void setHomeLocation(Location location, int number) {
        // Remove any home locations in the same world with the same number
        homeLocations.entrySet().removeIf(e -> Util.sameWorld(location.getWorld(), e.getKey().getWorld()) && e.getValue().equals(number));
        homeLocations.put(location, number);
    }

    /**
     * Set the uuid for this player object
     * @param uuid - UUID
     */
    public void setPlayerUUID(UUID uuid) {
        uniqueId = uuid.toString();
    }

    /**
     * Clears all home Locations in world
     * @param world - world
     */
    public void clearHomeLocations(World world) {
        homeLocations.keySet().removeIf(l -> Util.sameWorld(l.getWorld(), world));
    }

    /**
     * @return the locale
     */
    public String getLocale() {
        return locale;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * @return the deaths
     */
    public Map<String, Integer> getDeaths() {
        return deaths;
    }

    /**
     * @param world - world
     * @param deaths the deaths to set
     */
    public void setDeaths(World world, int deaths) {
        this.deaths.put(world.getName(), deaths > getPlugin().getIWM().getDeathsMax(world) ? getPlugin().getIWM().getDeathsMax(world) : deaths);
    }

    /**
     * Add death
     */
    public void addDeath(World world) {
        deaths.putIfAbsent(world.getName(), 0);
        if (deaths.get(world.getName()) < getPlugin().getIWM().getDeathsMax(world)) {
            deaths.put(world.getName(), deaths.get(world.getName()) + 1);
        }
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * Increments the reset counter for player in world
     * @param world - world
     */
    public void addReset(World world) {
        resets.merge(world.getName(), 1, Integer::sum);
    }

    /**
     * Get the number of deaths in this world
     * @param world - world
     * @return number of deaths
     */
    public int getDeaths(World world) {
        return deaths.getOrDefault(world.getName(), 0);
    }

    /**
     * @param deaths the deaths to set
     */
    public void setDeaths(Map<String, Integer> deaths) {
        this.deaths = deaths;
    }

}
