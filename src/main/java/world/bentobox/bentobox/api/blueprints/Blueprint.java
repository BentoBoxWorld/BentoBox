/**
 *
 */
package world.bentobox.bentobox.api.blueprints;

import com.google.gson.annotations.Expose;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import java.util.List;
import java.util.Map;

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
    private @NonNull String name;
    @Expose
    private String displayName;
    @Expose
    private @NonNull Material icon;
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
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
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
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    /**
     * @return the icon
     */
    public Material getIcon() {
        return icon;
    }
    /**
     * @param icon the icon to set
     */
    public void setIcon(Material icon) {
        this.icon = icon;
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
    public void setDescription(List<String> description) {
        this.description = description;
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
        this.entities = entities;
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
