package us.tastybento.bskyblock.database.objects;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.gson.annotations.Expose;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.util.Util;

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
    private int resetsLeft;
    @Expose
    private String locale = "";
    @Expose
    private int deaths;
    @Expose
    private Map<Location, Long> kickedList = new HashMap<>();

    /**
     * This is required for database storage
     */
    public Players() {}

    /**
     * @param plugin - BSkyBlock plugin object
     * @param uniqueId - unique ID
     *            Constructor - initializes the state variables
     *
     */
    public Players(BSkyBlock plugin, UUID uniqueId) {
        this.uniqueId = uniqueId.toString();
        homeLocations = new HashMap<>();
        resetsLeft = plugin.getSettings().getResetLimit();
        locale = "";
        kickedList = new HashMap<>();
        // Try to get player's name
        this.playerName = Bukkit.getOfflinePlayer(uniqueId).getName();
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
                .map(en -> en.getKey())
                .findFirst()
                .orElse(null);
    }

    /**
     * @param world 
     * @return List of home locations
     */
    public Map<Location, Integer> getHomeLocations(World world) {
        return homeLocations.entrySet().stream().filter(e -> Util.sameWorld(e.getKey().getWorld(),world))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * @return the kickedList
     */
    public Map<Location, Long> getKickedList() {
        return kickedList;
    }

    /**
     * @param kickedList the kickedList to set
     */
    public void setKickedList(Map<Location, Long> kickedList) {
        this.kickedList = kickedList;
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
     * @return the resetsLeft
     */
    public int getResetsLeft() {
        return resetsLeft;
    }

    /**
     * @param resetsLeft
     *            the resetsLeft to set
     */
    public void setResetsLeft(int resetsLeft) {
        this.resetsLeft = resetsLeft;
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
     * @param world 
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
    public int getDeaths() {
        return deaths;
    }

    /**
     * @param deaths the deaths to set
     */
    public void setDeaths(int deaths) {
        this.deaths = deaths > getPlugin().getSettings().getDeathsMax() ? getPlugin().getSettings().getDeathsMax() : deaths;
    }

    /**
     * Add death
     */
    public void addDeath() {
        if (deaths < getPlugin().getSettings().getDeathsMax()) {
            deaths++;
        }
    }

    /**
     * Can invite or still waiting for cool down to end
     *
     * @param location - the location
     *            to check
     * @return number of mins/hours left until cool down ends
     */
    public long getInviteCoolDownTime(Location location) {
        // Check the hashmap
        if (location != null && kickedList.containsKey(location)) {
            // The location is in the list
            // Check the date/time
            Date kickedDate = new Date(kickedList.get(location));
            Calendar coolDownTime = Calendar.getInstance();
            coolDownTime.setTime(kickedDate);
            coolDownTime.add(Calendar.MINUTE, getPlugin().getSettings().getInviteWait());
            // Add the invite cooldown period
            Calendar timeNow = Calendar.getInstance();
            if (coolDownTime.before(timeNow)) {
                // The time has expired
                kickedList.remove(location);
                return 0;
            } else {
                // Still not there yet
                // Time in minutes
                return (long) Math.ceil((coolDownTime.getTimeInMillis() - timeNow.getTimeInMillis()) / (1000 * 60D));
            }
        }
        return 0;
    }

    /**
     * Starts the invite cooldown timer for location. Location should be the center of an island.
     * @param location - the location
     */
    public void startInviteCoolDownTimer(Location location) {
        if (location != null) {
            kickedList.put(location, System.currentTimeMillis());
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

}
