package us.tastybento.bskyblock.database;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class Test {
    private int      id;
    private String     name;
    private HashMap<Integer, Location> homeLocations;
    private List<ItemStack>    inventory;

    public Test() {}

    public Test(int id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the homeLocations
     */
    public HashMap<Integer, Location> getHomeLocations() {
        return homeLocations;
    }

    /**
     * @param homeLocations the homeLocations to set
     */
    public void setHomeLocations(HashMap<Integer, Location> homeLocations) {
        this.homeLocations = homeLocations;
    }

    /**
     * @return the inventory
     */
    public List<ItemStack> getInventory() {
        return inventory;
    }

    /**
     * @param inventory the inventory to set
     */
    public void setInventory(List<ItemStack> inventory) {
        this.inventory = inventory;
    }

    public String toString() {
        final String TAB = "    \n";

        StringBuilder retValue = new StringBuilder();

        retValue.append("Test (\n ")
        .append(super.toString()).append(TAB)
        .append("     id = ").append(this.id).append(TAB)
        .append("     name = ").append(this.name).append(TAB)
        .append(" )");

        return retValue.toString();
    }   
}
