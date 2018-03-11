package us.tastybento.bskyblock.database.objects;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.google.gson.annotations.Expose;

import us.tastybento.bskyblock.BSkyBlock;

/**
 * Tracks the following info on the player
 *
 * @author tastybento
 */
public class Players implements DataObject {
    @Expose
    private Map<Integer, Location> homeLocations = new HashMap<>();
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
    public Players(BSkyBlock plugin, final UUID uniqueId) {
        this.uniqueId = uniqueId.toString();
        homeLocations = new HashMap<>();
        playerName = "";
        resetsLeft = plugin.getSettings().getResetLimit();
        locale = "";
        kickedList = new HashMap<>();
        playerName = Bukkit.getServer().getOfflinePlayer(uniqueId).getName();
        if (playerName == null) {
            playerName = uniqueId.toString();
        }
    }

    /**
     * Gets the default home location.
     * @return Location
     */
    public Location getHomeLocation() {
        return getHomeLocation(1); // Default
    }

    /**
     * Gets the home location by number.
     * @param number - a number
     * @return Location of this home or null if not available
     */
    public Location getHomeLocation(int number) {
        return homeLocations.get(number);
    }

    /**
     * @return List of home locations
     */
    public Map<Integer,Location> getHomeLocations() {
        return homeLocations;
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
    public void setHomeLocations(Map<Integer, Location> homeLocations) {
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
    public void setHomeLocation(final Location location, int number) {
        if (location == null) {
            homeLocations.clear();
        } else {
            homeLocations.put(number, location);
        }
    }

    /**
     * Set the uuid for this player object
     * @param uuid - UUID
     */
    public void setPlayerUUID(UUID uuid) {
        uniqueId = uuid.toString();
    }

    /**
     * Clears all home Locations
     */
    public void clearHomeLocations() {
        homeLocations.clear();
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
        this.deaths = deaths;
        if (this.deaths > getPlugin().getSettings().getDeathsMax()) {
            this.deaths = getPlugin().getSettings().getDeathsMax();
        }
    }

    /**
     * Add death
     */
    public void addDeath() {
        deaths++;
        if (deaths > getPlugin().getSettings().getDeathsMax()) {
            deaths = getPlugin().getSettings().getDeathsMax();
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
            // plugin.getLogger().info("DEBUG: Location is known");
            // The location is in the list
            // Check the date/time
            Date kickedDate = new Date(kickedList.get(location));
            // plugin.getLogger().info("DEBUG: kicked date = " + kickedDate);
            Calendar coolDownTime = Calendar.getInstance();
            coolDownTime.setTime(kickedDate);
            // coolDownTime.add(Calendar.HOUR_OF_DAY, Settings.inviteWait);
            coolDownTime.add(Calendar.MINUTE, getPlugin().getSettings().getInviteWait());
            // Add the invite cooldown period
            Calendar timeNow = Calendar.getInstance();
            // plugin.getLogger().info("DEBUG: date now = " + timeNow);
            if (coolDownTime.before(timeNow)) {
                // The time has expired
                kickedList.remove(location);
                return 0;
            } else {
                // Still not there yet
                // long hours = (coolDownTime.getTimeInMillis() -
                // timeNow.getTimeInMillis())/(1000 * 60 * 60);
                // Temp minutes
                return (coolDownTime.getTimeInMillis() - timeNow.getTimeInMillis()) / (1000 * 60);
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
