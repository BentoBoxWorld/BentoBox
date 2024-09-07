package world.bentobox.bentobox.database.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.metadata.MetaDataAble;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * Tracks the following info on the player
 *
 * @author tastybento
 */
@Table(name = "Players")
public class Players implements DataObject, MetaDataAble {
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
     * This variable stores set of worlds where user inventory must be cleared.
     * @since 1.3.0
     */
    @Expose
    private Set<String> pendingKicks = new HashSet<>();

    /**
     * Stores the display mode of the Settings Panel.
     * @since 1.6.0
     */
    @Expose
    private Flag.Mode flagsDisplayMode = Flag.Mode.BASIC;

    /**
     * A place to store meta data for this player.
     * @since 1.15.4
     */
    @Expose
    private Map<String, MetaDataValue> metaData;

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
        locale = "";
        // Try to get player's name
        this.playerName = Bukkit.getOfflinePlayer(uniqueId).getName();
        if (this.playerName == null) {
            this.playerName = uniqueId.toString();
        }
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
        return resets.computeIfAbsent(world.getName(), k -> 0);
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
     * Set the uuid for this player object
     * @param uuid - UUID
     */
    public void setPlayerUUID(UUID uuid) {
        uniqueId = uuid.toString();
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
        this.deaths.put(world.getName(), Math.min(deaths, getPlugin().getIWM().getDeathsMax(world)));
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
        resets.merge(world.getName(), 1, (oldValue, newValue) -> Integer.valueOf(oldValue + newValue));
    }

    /**
     * Get the number of deaths in this world.
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

    /**
     * Returns the pendingKicks value.
     * @return the value of pendingKicks.
     * @since 1.3.0
     */
    public Set<String> getPendingKicks()
    {
        return pendingKicks;
    }

    /**
     * Sets the pendingKicks value.
     * @param pendingKicks the pendingKicks new value.
     * @since 1.3.0
     */
    public void setPendingKicks(Set<String> pendingKicks)
    {
        this.pendingKicks = pendingKicks;
    }

    /**
     * Adds given world in pendingKicks world set.
     * @param world World that must be added to pendingKicks set.
     * @since 1.3.0
     */
    public void addToPendingKick(World world)
    {
        World w = Util.getWorld(world);
        if (w != null) {
            this.pendingKicks.add(w.getName());
        }
    }

    /**
     * @return the metaData
     * @since 1.15.5
     * @see User#getMetaData()
     */
    @Override
    public Optional<Map<String, MetaDataValue>> getMetaData() {
        if (metaData == null) {
            metaData = new HashMap<>();
        } else if (isImmutable(metaData)) {
            metaData = new HashMap<>(metaData); // Convert immutable map to mutable
        }
        return Optional.of(metaData);
    }

    private boolean isImmutable(Map<String, MetaDataValue> map) {
        try {
            String testKey = "testKey";
            MetaDataValue testValue = new MetaDataValue("test");

            // If the map already contains keys, use one of them
            if (!map.isEmpty()) {
                String existingKey = map.keySet().iterator().next();
                map.put(existingKey, map.get(existingKey)); // Attempt to replace value
            } else {
                // Use a unique key-value pair
                map.put(testKey, testValue);
                map.remove(testKey);
            }
            return false; // No exception means the map is mutable
        } catch (UnsupportedOperationException e) {
            return true; // Exception means the map is immutable
        }
    }

    /**
     * @param metaData the metaData to set
     * @since 1.15.4
     * @see User#setMetaData(Map)
     */
    @Override
    public void setMetaData(Map<String, MetaDataValue> metaData) {
        if (isImmutable(metaData)) {
            throw new IllegalArgumentException("Provided map is immutable and cannot be set.");
        }
        this.metaData = metaData;
    }


}
