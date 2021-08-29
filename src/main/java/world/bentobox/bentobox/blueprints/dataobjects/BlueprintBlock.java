package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.block.Biome;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;

import com.google.gson.annotations.Expose;

/**
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintBlock {

    @Expose
    private String blockData;
    @Expose
    private List<String> signLines;
    @Expose
    private Map<Integer, ItemStack> inventory;
    @Expose
    private BlueprintCreatureSpawner creatureSpawner;
    /**
     * Since 1.15.2
     */
    @Expose
    private Biome biome;
    /**
     * @since 1.8.0
     */
    @Expose
    private List<Pattern> bannerPatterns;
    @Expose
    private boolean glowingText;

    public BlueprintBlock(String blockData) {
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
        return inventory == null ? new HashMap<>() : inventory;
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
    public BlueprintCreatureSpawner getCreatureSpawner() {
        return creatureSpawner;
    }

    /**
     * @param creatureSpawner the creatureSpawner to set
     */
    public void setCreatureSpawner(BlueprintCreatureSpawner creatureSpawner) {
        this.creatureSpawner = creatureSpawner;
    }

    /**
     * @return list of the banner patterns
     * @since 1.8.0
     */
    public List<Pattern> getBannerPatterns() {
        return bannerPatterns;
    }

    /**
     * @param bannerPatterns the banner Patterns to set
     * @since 1.8.0
     */
    public void setBannerPatterns(List<Pattern> bannerPatterns) {
        this.bannerPatterns = bannerPatterns;
    }

    /**
     * @return the biome
     */
    public Biome getBiome() {
        return biome;
    }

    /**
     * @param biome the biome to set
     */
    public void setBiome(Biome biome) {
        this.biome = biome;
    }

    /**
     * @return the glowingText
     */
    public boolean isGlowingText() {
        return glowingText;
    }

    /**
     * @param glowingText the glowingText to set
     */
    public void setGlowingText(boolean glowingText) {
        this.glowingText = glowingText;
    }


}
