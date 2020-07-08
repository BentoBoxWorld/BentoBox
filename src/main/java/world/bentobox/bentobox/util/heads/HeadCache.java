package world.bentobox.bentobox.util.heads;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.lang.reflect.Field;
import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import world.bentobox.bentobox.BentoBox;


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
     * Encoded texture for given player head.
     */
    public final String encodedTexture;

    /**
     * Time when head was created.
     * Setting it to 0 will result in keeping head in cache for ever.
     */
    private final long timestamp;


    // ---------------------------------------------------------------------
    // Section: Constructors
    // ---------------------------------------------------------------------


    /**
     * Constructor HeadCache creates a new HeadCache instance.
     *
     * @param userName of type String
     * @param userId of type String
     * @param encodedTexture of type String
     */
    public HeadCache(String userName, UUID userId, String encodedTexture)
    {
        this(userName, userId, encodedTexture, System.currentTimeMillis());
    }



    /**
     * Constructor HeadCache creates a new HeadCache instance.
     *
     * @param userName of type String
     * @param userId of type UUID
     * @param encodedTexture of type String
     * @param timestamp of type long
     */
    public HeadCache(String userName, UUID userId, String encodedTexture, long timestamp)
    {
        this.userName = userName;
        this.encodedTexture = encodedTexture;
        this.userId = userId;
        this.timestamp = timestamp;
    }


    // ---------------------------------------------------------------------
    // Section: Methods
    // ---------------------------------------------------------------------


    /**
     * Returns a new Player head with a cached texture.
     * Be AWARE, usage does not use clone method. If for some reason item stack is stored directly,
     * then use clone in return :)
     * @return an ItemStack of the custom head.
     */
    public ItemStack getPlayerHead()
    {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        // Set correct Skull texture
        if (meta != null && this.encodedTexture != null && !this.encodedTexture.isEmpty())
        {
            GameProfile profile = new GameProfile(this.userId, this.userName);
            profile.getProperties().put("textures", new Property("textures", this.encodedTexture));

            try
            {
                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
                item.setItemMeta(meta);
            }
            catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e)
            {
                BentoBox.getInstance().log("Error while creating Skull Icon");
            }
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
