package world.bentobox.bentobox.blueprints;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.util.ItemParser;

/**
 * Stores all details of a blueprint
 * @author tastybento
 *
 */
public class Blueprint {

    private static final String DEFAULT_ICON = "PAPER";

    /**
     * Unique name for this blueprint. The filename will be this plus the blueprint suffix
     */
    @Expose
    private @NonNull String name = "";
    @Expose
    private String displayName;
    /**
     * Icon of the blueprint. Supports plain material names (e.g. "DIAMOND"),
     * vanilla namespaced materials (e.g. "minecraft:diamond"), and custom
     * item model keys (e.g. "myserver:island_tropical").
     */
    @Expose
    private String icon = DEFAULT_ICON;
    @Expose
    private List<String> description;
    @Expose
    private Map<Vector, BlueprintBlock> attached;
    @Expose
    private Map<Vector, List<BlueprintEntity>> entities;
    @Expose
    private Map<Vector, BlueprintBlock> blocks;
    @Expose
    private int xSize;
    @Expose
    private int ySize;
    @Expose
    private int zSize;
    @Expose
    private Vector bedrock;
    @Expose
    private boolean sink;
    /**
     * @return the name
     */
    @NonNull
    public String getName() {
        // Ensure non-null return value even if deserialization sets name to null
        return name == null ? "" : name;
    }
    /**
     * @param name the name to set
     */
    public Blueprint setName(@NonNull String name) {
        // Force lowercase
        this.name = name;
        return this;
    }
    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }
    /**
     * @param displayName the displayName to set
     */
    public Blueprint setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }
    /**
     * Returns the base Material for this blueprint's icon.
     * Resolves plain names ("DIAMOND") and vanilla namespaced keys ("minecraft:diamond")
     * via {@link Material#matchMaterial}. For custom item-model keys that are not
     * valid vanilla materials (e.g. "myserver:island_tropical"), returns {@link Material#PAPER}
     * as the base item — use {@link #getIconItemStack()} to get the full item with model data.
     * @return the icon material, never null
     */
    public @NonNull Material getIcon() {
        return ItemParser.parseIconMaterial(icon);
    }

    /**
     * Returns an {@link ItemStack} representing this blueprint's icon.
     * <ul>
     *   <li>Plain material name (e.g. {@code "DIAMOND"}) → {@code new ItemStack(Material.DIAMOND)}</li>
     *   <li>Vanilla namespaced material (e.g. {@code "minecraft:diamond"}) → same as above</li>
     *   <li>Custom item-model key (e.g. {@code "myserver:island_tropical"}) → PAPER base item
     *       with the model key set via {@link ItemMeta#setItemModel}</li>
     * </ul>
     * @return ItemStack for this blueprint's icon, never null
     * @since 3.0.0
     */
    public @NonNull ItemStack getIconItemStack() {
        return ItemParser.parseIconItemStack(icon);
    }

    /**
     * Sets the icon from a Material (backward-compatible setter).
     * @param icon the icon material to set; if null, defaults to {@link Material#PAPER}
     * @return blueprint
     */
    public Blueprint setIcon(Material icon) {
        this.icon = icon != null ? icon.name() : DEFAULT_ICON;
        return this;
    }

    /**
     * Sets the icon from a string. Accepts plain material names (e.g. {@code "DIAMOND"}),
     * vanilla namespaced materials (e.g. {@code "minecraft:diamond"}), and custom item-model
     * keys (e.g. {@code "myserver:island_tropical"}).
     * @param icon the icon string; if null, defaults to {@code "PAPER"}
     * @return blueprint
     * @since 3.0.0
     */
    public Blueprint setIcon(String icon) {
        this.icon = icon != null ? icon : DEFAULT_ICON;
        return this;
    }
    /**
     * @return the description
     */
    public List<String> getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public Blueprint setDescription(List<String> description) {
        this.description = description;
        return this;
    }
    /**
     * @param description the description to set
     */
    public Blueprint setDescription(String description) {
        if (this.description == null) this.description = new ArrayList<>();
        this.description.add(description);
        return this;
    }
    /**
     * @return the attached
     */
    public Map<Vector, BlueprintBlock> getAttached() {
        return attached;
    }
    /**
     * @param attached the attached to set
     */
    public void setAttached(Map<Vector, BlueprintBlock> attached) {
        this.attached = attached;
    }
    /**
     * @return the entities
     */
    public Map<Vector, List<BlueprintEntity>> getEntities() {
        return entities;
    }
    /**
     * @param entities the entities to set
     */
    public void setEntities(Map<Vector, List<BlueprintEntity>> entities) {
        if (this.entities == null) {
            this.entities = entities;
        } else {
            this.entities.putAll(entities);
        }
    }
    /**
     * @return the blocks
     */
    public Map<Vector, BlueprintBlock> getBlocks() {
        return blocks;
    }
    /**
     * @param blocks the blocks to set
     */
    public void setBlocks(Map<Vector, BlueprintBlock> blocks) {
        this.blocks = blocks;
    }
    /**
     * @return the xSize
     */
    public int getxSize() {
        return xSize;
    }
    /**
     * @param xSize the xSize to set
     */
    public void setxSize(int xSize) {
        this.xSize = xSize;
    }
    /**
     * @return the ySize
     */
    public int getySize() {
        return ySize;
    }
    /**
     * @param ySize the ySize to set
     */
    public void setySize(int ySize) {
        this.ySize = ySize;
    }
    /**
     * @return the zSize
     */
    public int getzSize() {
        return zSize;
    }
    /**
     * @param zSize the zSize to set
     */
    public void setzSize(int zSize) {
        this.zSize = zSize;
    }
    /**
     * @return the bedrock
     */
    public Vector getBedrock() {
        return bedrock;
    }
    /**
     * @param bedrock the bedrock to set
     */
    public void setBedrock(Vector bedrock) {
        this.bedrock = bedrock;
    }

    /**
     * Check if the blueprint should sink or not
     * @return the sink
     */
    public boolean isSink() {
        return sink;
    }

    /**
     * Set if the blueprint should sink or not
     * @param sink the sink to set
     */
    public void setSink(boolean sink) {
        this.sink = sink;
    }

}
