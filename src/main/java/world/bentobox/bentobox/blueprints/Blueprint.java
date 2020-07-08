package world.bentobox.bentobox.blueprints;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * Stores all details of a blueprint
 * @author tastybento
 *
 */
public class Blueprint {

    /**
     * Unique name for this blueprint. The filename will be this plus the blueprint suffix
     */
    @Expose
    private @NonNull String name = "";
    @Expose
    private String displayName;
    @Expose
    private @NonNull Material icon = Material.PAPER;
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
    /**
     * @return the name
     */
    @NonNull
    public String getName() {
        if (name == null) name = "unnamed";
        // Force lower case
        return name.toLowerCase(Locale.ENGLISH);
    }
    /**
     * @param name the name to set
     */
    public Blueprint setName(@NonNull String name) {
        // Force lowercase
        this.name = name.toLowerCase(Locale.ENGLISH);
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
     * @return the icon
     */
    public Material getIcon() {
        return icon;
    }
    /**
     * @param icon the icon to set
     * @return blueprint
     */
    public Blueprint setIcon(Material icon) {
        this.icon = icon;
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

}
