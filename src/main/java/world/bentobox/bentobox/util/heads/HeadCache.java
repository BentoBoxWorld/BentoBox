package world.bentobox.bentobox.util.heads;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;


/**
 * This would allow to implement changeable player head for server owner.
 * @since 1.14.1
 * @author tastybento, BONNe1704
 */
public class HeadCache
{
    // ---------------------------------------------------------------------
    // Section: Variables
    // ---------------------------------------------------------------------

    /**
     * Username for cached head.
     */
    private final String userName;

    /**
     * Userid for cached head.
     */
    private final UUID userId;

    /**
     * Player profile for cached head.
     */
    public final PlayerProfile playerProfile;

    /**
     * Time when head was created. Setting it to 0 will result in keeping head in cache
     * for ever.
     */
    private final long timestamp;


    // ---------------------------------------------------------------------
    // Section: Constructors
    // ---------------------------------------------------------------------


    /**
     * Constructor HeadCache creates a new HeadCache instance.
     *
     * @param userName      of type String
     * @param userId        of type String
     * @param playerProfile of type PlayerProfile
     */
    public HeadCache(String userName, UUID userId, PlayerProfile playerProfile)
    {
        this(userName, userId, playerProfile, System.currentTimeMillis());
    }


    /**
     * Constructor HeadCache creates a new HeadCache instance.
     *
     * @param userName      of type String
     * @param userId        of type UUID
     * @param playerProfile of type String
     * @param timestamp     of type long
     */
    public HeadCache(String userName,
        UUID userId,
        PlayerProfile playerProfile,
        long timestamp)
    {
        this.userName = userName;
        this.playerProfile = playerProfile;
        this.userId = userId;
        this.timestamp = timestamp;
    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Returns a new Player head with a cached texture. Be AWARE, usage does not use clone
     * method. If for some reason item stack is stored directly, then use clone in return
     * :)
     *
     * @return an ItemStack of the custom head.
     */
    public ItemStack getPlayerHead()
    {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        // Set correct Skull texture
        if (meta != null && this.playerProfile != null)
        {
            meta.setOwnerProfile(this.playerProfile);
            item.setItemMeta(meta);
        }

        return item;
    }


    /**
     * Method HeadCache#getUserName returns the userName of this object.
     *
     * @return the userName (type String) of this object.
     */
    public String getUserName()
    {
        return this.userName;
    }


    /**
     * Method HeadCache#getTimestamp returns the timestamp of this object.
     *
     * @return the timestamp (type long) of this object.
     */
    public long getTimestamp()
    {
        return timestamp;
    }


    /**
     * Method HeadCache#getUserId returns the userId of this object.
     *
     * @return the userId (type UUID) of this object.
     */
    public UUID getUserId()
    {
        return this.userId;
    }
}
