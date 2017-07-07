package us.tastybento.bskyblock.util.nms;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import us.tastybento.org.jnbt.Tag;

public interface NMSAbstraction {

    /**
     * Update the low-level chunk information for the given block to the new block ID and data.  This
     * change will not be propagated to clients until the chunk is refreshed to them.
     * @param block
     * @param blockId
     * @param data
     * @param applyPhysics
     */
    public void setBlockSuperFast(Block block, int blockId, byte data, boolean applyPhysics);

    public ItemStack setBook(Tag item);

    /**
     * Sets a block to be an item stack
     * @param block
     * @param itemStack
     */
    public void setFlowerPotBlock(Block block, ItemStack itemStack);

    boolean isPotion(ItemStack item);

    /**
     * Returns a potion ItemStack
     * @param itemMaterial 
     * @param itemTag
     * @param chestItem
     * @return
     */
    public ItemStack setPotion(Material itemMaterial, Tag itemTag, ItemStack chestItem);
    
    /**
     * Gets a monster egg itemstack
     * @param type
     * @param amount
     * @return itemstack
     */
    public ItemStack getSpawnEgg(EntityType type, int amount);
    
    /**
     * Send an action bar message to player
     * @param player
     * @param message
     */
    public void sendActionBar(Player player, String message);


    /**
     * Send a title to a player
     * @param player
     * @param message
     */
    public void sendTitle(Player player, String message);


    /**
     * Send a subtitle to a player
     * @param player
     * @param message
     */
    public void sendSubtitle(Player player, String message);
}
