package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.database.objects.DataObject;

/**
 * Represents a bundle of three {@link Blueprint}s.
 * This is what the player will choose when creating his island.
 * @since 1.5.0
 * @author Poslovitch, tastybento
 */
public class BlueprintBundle implements DataObject {

    /**
     * The unique id of this bundle
     */
    @Expose
    private String uniqueId;
    /**
     * Icon of the bundle
     */
    @Expose
    private Material icon = Material.PAPER;
    /**
     * Name on the icon
     */
    @Expose
    private String displayName = "";
    /**
     * Description to show players
     */
    @Expose
    private List<String> description = new ArrayList<>();
    /**
     * If true, then the player needs to have a permission to view or use this bundle
     * The permission is GameModeAddon.island.create.uniqueId of blueprint bundle.
     * e.g. bskyblock.island.create.vip
     */
    @Expose
    private boolean requirePermission;

    /**
     * Reference to the blueprint
     */
    @Expose
    private Map<World.Environment, String> blueprints = new EnumMap<>(World.Environment.class);

    /**
     * Preferred slot in GUI.
     */
    @Expose
    private int slot = 0;



    /**
     * @return the uniqueId
     */
    @Override
    public String getUniqueId() {
        return uniqueId.toLowerCase(Locale.ENGLISH);
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
    public Map<World.Environment, String> getBlueprints() {
        return blueprints;
    }
    /**
     * @param blueprints the blueprints to set
     */
    public void setBlueprints(Map<World.Environment, String> blueprints) {
        this.blueprints = blueprints;
    }

    /**
     * Adds a blueprint to the blueprint bundle. It will replace any blueprint that already exists of the same Environment type.
     * @param env - the Environment
     * @param bp - blueprint
     */
    public void setBlueprint(World.Environment env, Blueprint bp) {
        this.blueprints.put(env, bp.getName());
    }

    /**
     * Removes a blueprint from this environment slot
     * @param env - the world environment
     */
    public void clearBlueprint(World.Environment env) {
        this.blueprints.remove(env);
    }

    /**
     * Get the blueprint for the environment type
     * @param env - Environment type
     * @return Blueprint or null if one does not exist
     */
    public String getBlueprint(World.Environment env) {
        return this.blueprints.get(env);
    }

    /**
     * Adds a line to the description
     *
     * @param string description
     */
    public void setDescription(String string) {
        if (description == null)
            description = new ArrayList<>();
        this.description.add(string);

    }
    /**
     * @return the requirePermission
     */
    public boolean isRequirePermission() {
        return requirePermission;
    }
    /**
     * @param requirePermission the requirePermission to set
     */
    public void setRequirePermission(boolean requirePermission) {
        this.requirePermission = requirePermission;
    }
    /**
     * @return the slot
     */
    public int getSlot() {
        return slot;
    }
    /**
     * @param slot the slot to set
     */
    public void setSlot(int slot) {
        this.slot = slot;
    }

}
