/*******************************************************************************
 * This file is part of BSkyBlock.
 *
 *     BSkyBlock is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     BSkyBlock is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with BSkyBlock.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

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
