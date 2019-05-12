package world.bentobox.bentobox.api.blueprints;

import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.google.gson.annotations.Expose;

public class BP_Block {

    @Expose
    String blockData;
    @Expose
    List<String> signLines;
    @Expose
    Map<Integer, ItemStack> inventory;
    @Expose
    BP_CreatureSpawner creatureSpawner;

    public BP_Block(String blockData) {
        this.blockData = blockData;
    }

    /**
     * @return the blockData
     */
    public String getBlockData() {
        return blockData;
    }

    /**
     * @param blockData the blockData to set
     */
    public void setBlockData(String blockData) {
        this.blockData = blockData;
    }

    /**
     * @return the signLines
     */
    public List<String> getSignLines() {
        return signLines;
    }

    /**
     * @param signLines the signLines to set
     */
    public void setSignLines(List<String> signLines) {
        this.signLines = signLines;
    }

    /**
     * @return the inventory
     */
    public Map<Integer, ItemStack> getInventory() {
        return inventory;
    }

    /**
     * @param inventory the inventory to set
     */
    public void setInventory(Map<Integer, ItemStack> inventory) {
        this.inventory = inventory;
    }

    /**
     * @return the creatureSpawner
     */
    public BP_CreatureSpawner getCreatureSpawner() {
        return creatureSpawner;
    }

    /**
     * @param creatureSpawner the creatureSpawner to set
     */
    public void setCreatureSpawner(BP_CreatureSpawner creatureSpawner) {
        this.creatureSpawner = creatureSpawner;
    }

}
