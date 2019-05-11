/**
 *
 */
package world.bentobox.bentobox.api.blueprints;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import com.google.gson.annotations.Expose;

/**
 * Stores all details of a blueprint
 * @author tastybento
 *
 */
public class Blueprint {

    @Expose
    private @NonNull String name = "";
    @Expose
    private String displayName = "";
    @Expose
    private @NonNull Material icon = Material.PAPER;
    @Expose
    private List<String> description = new ArrayList<>();
    @Expose
    private World.Environment environment = World.Environment.NORMAL;
    @Expose
    private String fileName = "";
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
     * @return the environment
     */
    public World.Environment getEnvironment() {
        return environment;
    }
    /**
     * @param environment the environment to set
     */
    public void setEnvironment(World.Environment environment) {
        this.environment = environment;
    }
    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
    }
    /**
     * @param fileName the fileName to set
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


}
