package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.block.Biome;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.sign.Side;
import org.bukkit.inventory.ItemStack;

import com.google.gson.annotations.Expose;

/**
 * Represents a block to be pasted
 * @author tastybento
 * @since 1.5.0
 */
public class BlueprintBlock {

    @Expose
    private String blockData;
    /**
     * Front of sign
     */
    @Expose
    private List<String> signLines;
    /**
     * Back of sign
     */
    @Expose
    private List<String> signLines2;
    @Expose
    private Map<Integer, ItemStack> inventory;
    @Expose
    private BlueprintCreatureSpawner creatureSpawner;
    /**
     * @since 3.4.2
     */
    @Expose
    private BlueprintTrialSpawner trialSpawner;

    @Expose
    private String itemsAdderBlock;

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
    /**
     * Front of sign
     */
    @Expose
    private boolean glowingText;
    /**
     * Back of sign
     */
    @Expose
    private boolean glowingText2;

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
     * @deprecated signs now have two sides
     * @since 1.24.0
     */
    @Deprecated
    public List<String> getSignLines() {
        return signLines;
    }

    /**
     * @param signLines the signLines to set
     * @deprecated signs now have two sides
     * @since 1.24.0
     */
    @Deprecated
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
     * @deprecated signs now have two sides
     * @since 1.24.0
     */
    @Deprecated
    public boolean isGlowingText() {
        return glowingText;
    }

    /**
     * @param glowingText the glowingText to set
     * @deprecated signs now have two sides
     * @since 1.24.0
     */
    @Deprecated
    public void setGlowingText(boolean glowingText) {
        this.glowingText = glowingText;
    }

    /**
     * @param side side of sign
     * @param glowingText the glowingText to set
     * @since 1.24.0
     */
    public void setGlowingText(Side side, boolean glowingText) {
        if (side == Side.FRONT) {
            this.glowingText = glowingText;
        } else {
            this.glowingText2 = glowingText;
        }

    }

    /**
     * @param side side of sign
     * @return the glowingText
     * @since 1.24.0
     */
    public boolean isGlowingText(Side side) {
        if (side == Side.FRONT) return glowingText;
        return glowingText2;
    }

    /**
     * @param side side of sign
     * @return the signLines
     * @since 1.24.0
     */
    public List<String> getSignLines(Side side) {
        if (side == Side.FRONT) return signLines;
        return signLines2;
    }

    /**
     * @param side side of sign
     * @param signLines the signLines to set
     * @since 1.24.0
     */
    public void setSignLines(Side side, List<String> signLines) {
        if (side == Side.FRONT) {
            this.signLines = signLines;
        } else {
            this.signLines2 = signLines;
        }
    }

    /**
     * @return the trialSpawner
     */
    public BlueprintTrialSpawner getTrialSpawner() {
        return trialSpawner;
    }

    /**
     * @param trialSpawner the trialSpawner to set
     */
    public void setTrialSpawner(BlueprintTrialSpawner trialSpawner) {
        this.trialSpawner = trialSpawner;
    }

    /**
     * @return the itemsAdderBlock
     */
    public String getItemsAdderBlock() {
        return itemsAdderBlock;
    }

    /**
     * @param itemsAdderBlock the itemsAdderBlock to set
     */
    public void setItemsAdderBlock(String itemsAdderBlock) {
        this.itemsAdderBlock = itemsAdderBlock;
    }
}
