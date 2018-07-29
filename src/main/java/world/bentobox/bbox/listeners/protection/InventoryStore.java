package world.bentobox.bbox.listeners.protection;

import org.bukkit.inventory.ItemStack;

/**
 * Where the inventory data is stored
 *
 * @author tastybento
 */
public class InventoryStore {
    private ItemStack[] inventory;
    private ItemStack[] armor;

    /**
     * @param inventory - item stack array
     * @param armor - armor item stack array
     */
    public InventoryStore(ItemStack[] inventory, ItemStack[] armor) {
        this.inventory = inventory;
        this.armor = armor;
    }

    /**
     * @return the inventory
     */
    public ItemStack[] getInventory() {
        return inventory;
    }

    /**
     * @param inventory
     *            the inventory to set
     */
    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    /**
     * @return the armor
     */
    public ItemStack[] getArmor() {
        return armor;
    }

    /**
     * @param armor
     *            the armor to set
     */
    public void setArmor(ItemStack[] armor) {
        this.armor = armor;
    }
}
