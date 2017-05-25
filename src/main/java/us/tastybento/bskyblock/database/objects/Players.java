package us.tastybento.bskyblock.database.objects;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.config.Settings;

/**
 * Tracks the following info on the player
 * 
 * @author tastybento
 */
public class Players extends DataObject {
    private HashMap<Integer, Location> homeLocations;
    private UUID uuid;
    private String playerName;
    private int resetsLeft;
    private String locale;
    private boolean useControlPanel;
    private int deaths;
    private HashMap<Location, Date> kickedList;

    /**
     * @param uuid
     *            Constructor - initializes the state variables
     * 
     */
    public Players(final UUID uuid) {
        this.uuid = uuid;
        this.homeLocations = new HashMap<Integer,Location>();
        this.playerName = "";
        this.resetsLeft = Settings.resetLimit;
        this.locale = "";
        this.useControlPanel = Settings.useControlPanel;
        this.kickedList = new HashMap<Location, Date>();
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
     * @param number
     * @return Location of this home or null if not available
     */
    public Location getHomeLocation(int number) {
        if (homeLocations.containsKey(number)) {
            return homeLocations.get(number);
        } else {
            return null;
        }
    }

    /**
     * Provides a list of all home locations - used when searching for a safe spot to place someone
     * @return List of home locations
     */
    public HashMap<Integer,Location> getHomeLocations() {
        HashMap<Integer,Location> result = new HashMap<Integer,Location>();
        for (int number : homeLocations.keySet()) {
            result.put(number, homeLocations.get(number));
        }
        return result;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public UUID getPlayerUUID() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerN(String playerName) {
        this.playerName = playerName;
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
     * @param location
     * @param number
     */
    public void setHomeLocation(final Location location, int number) {
        if (location == null) {
            homeLocations.clear();
        } else {
            // Make the location x,y,z integer, but keep the yaw and pitch
            homeLocations.put(number, new Location(location.getWorld(),location.getBlockX(),location.getBlockY(),location.getBlockZ(),location.getYaw(), location.getPitch()));
        }
    }

    /**
     * Set the uuid for this player object
     * @param uuid
     */
    public void setPlayerUUID(final UUID uuid) {
        this.uuid = uuid;
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
     * Sets whether a player uses the control panel or not
     * @param b
     */
    public void setControlPanel(boolean b) {
        useControlPanel = b;
    }

    /**
     * @return useControlPanel
     */
    public boolean getControlPanel() {
        return useControlPanel;
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
        if (this.deaths > Settings.deathsMax) {
            this.deaths = Settings.deathsMax;
        }
    }

    /**
     * Add death
     */
    public void addDeath() {
        this.deaths++;
        if (this.deaths > Settings.deathsMax) {
            this.deaths = Settings.deathsMax;
        }
    }

    /**
     * Can invite or still waiting for cool down to end
     * 
     * @param location
     *            to check
     * @return number of mins/hours left until cool down ends
     */
    public long getInviteCoolDownTime(Location location) {
        // Check the hashmap
        if (location != null && kickedList.containsKey(location)) {
            // plugin.getLogger().info("DEBUG: Location is known");
            // The location is in the list
            // Check the date/time
            Date kickedDate = kickedList.get(location);
            // plugin.getLogger().info("DEBUG: kicked date = " + kickedDate);
            Calendar coolDownTime = Calendar.getInstance();
            coolDownTime.setTime(kickedDate);
            // coolDownTime.add(Calendar.HOUR_OF_DAY, Settings.inviteWait);
            coolDownTime.add(Calendar.MINUTE, Settings.inviteWait);
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
                long hours = (coolDownTime.getTimeInMillis() - timeNow.getTimeInMillis()) / (1000 * 60);
                return hours;
            }
        }
        return 0;
    }

    /**
     * Starts the invite cooldown timer for location. Location should be the center of an island.
     * @param location
     */
    public void startInviteCoolDownTimer(Location location) {
        if (location != null) {
            kickedList.put(location, new Date());
        }
    }

    @Override
    public String getUniqueId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        // TODO Auto-generated method stub
        
    }

}
