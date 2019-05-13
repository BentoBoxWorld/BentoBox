package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.database.objects.DataObject;

/**
 * Represents a bundle of three {@link Blueprint}s.
 * This is what the player will choose when creating his island.
 * @since 1.5.0
 * @author Poslovitch
 */
public class BlueprintBundle implements DataObject {

    @Expose
    private String uniqueId;
    @Expose
    private Material icon = Material.PAPER;
    @Expose
    private String displayName = "";
    @Expose
    private List<String> description = new ArrayList<>();
    /**
     * Reference to the blueprint
     */
    @Expose
    private EnumMap<World.Environment, String> blueprints = new EnumMap<>(World.Environment.class);
    /**
     * @return the uniqueId
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }
    /**
     * @param uniqueId the uniqueId to set
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
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
     * @return the blueprints
     */
    public EnumMap<World.Environment, String> getBlueprints() {
        return blueprints;
    }
    /**
     * @param blueprints the blueprints to set
     */
    public void setBlueprints(EnumMap<World.Environment, String> blueprints) {
        this.blueprints = blueprints;
    }

    /**
     * Adds a blueprint to the blueprint bundle. It will replace any blueprint that already exists of the same {@link World.Environment} type.
     * @param env - the {@link World#Environment}
     * @param bp - blueprint
     */
    public void setBlueprint(World.Environment env, Blueprint bp) {
        this.blueprints.put(env, bp.getName());
    }

    /**
     * Get the blueprint for the environment type
     * @param env - {@link World#Environment} type
     * @return Blueprint or null if one does not exist
     */
    public String getBlueprint(World.Environment env) {
        return this.blueprints.get(env);
    }

}
