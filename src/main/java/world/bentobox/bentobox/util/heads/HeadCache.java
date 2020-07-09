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
     * Base64 Encoded texture link to given player skin.
     */
    public final String encodedTextureLink;

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
     * @param userName           of type String
     * @param userId             of type String
     * @param encodedTextureLink of type String
     */
    public HeadCache(String userName, UUID userId, String encodedTextureLink)
    {
        this(userName, userId, encodedTextureLink, System.currentTimeMillis());
    }


    /**
     * Constructor HeadCache creates a new HeadCache instance.
     *
     * @param userName           of type String
     * @param userId             of type UUID
     * @param encodedTextureLink of type String
     * @param timestamp          of type long
     */
    public HeadCache(String userName,
        UUID userId,
        String encodedTextureLink,
        long timestamp)
    {
        this.userName = userName;
        this.encodedTextureLink = encodedTextureLink;
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
        ItemMeta meta = item.getItemMeta();

        // Set correct Skull texture
        if (meta != null && this.encodedTextureLink != null && !this.encodedTextureLink.isEmpty())
        {
            GameProfile profile = new GameProfile(this.userId, this.userName);
            profile.getProperties().put("textures",
                new Property("textures", this.encodedTextureLink));

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
