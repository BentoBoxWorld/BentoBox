package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import com.google.gson.annotations.Expose;

import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.util.ItemParser;

/**
 * Represents a bundle of three {@link Blueprint}s.
 * This is what the player will choose when creating his island.
 * @since 1.5.0
 * @author Poslovitch, tastybento
 */
public class BlueprintBundle implements DataObject {

    private static final String DEFAULT_ICON = "PAPER";

    /**
     * The unique id of this bundle
     */
    @Expose
    private String uniqueId;
    /**
     * Icon of the bundle. Supports plain material names (e.g. "DIAMOND"),
     * vanilla namespaced materials (e.g. "minecraft:diamond"), and custom
     * item model keys (e.g. "myserver:island_tropical").
     */
    @Expose
    private String icon = DEFAULT_ICON;
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
     * Number of times this bundle can be used by a single player. 0 = unlimited
     */
    @Expose
    private int times = 0;

    /**
     * Cost of the bundle. 0 = free
     */
    @Expose
    private double cost = 0;

    /**
     * Commands to run when this bundle is pasted (i.e., when an island is created with it).
     * Supports [player] and [owner] placeholders.
     * Commands prefixed with [SUDO] are run as the player; all others are run as console.
     * @since 2.6.0
     */
    @Expose
    private List<String> commands = new ArrayList<>();

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
     * Returns the base Material for this bundle's icon.
     * Resolves plain names ("DIAMOND") and vanilla namespaced keys ("minecraft:diamond")
     * via {@link Material#matchMaterial}. For custom item-model keys that are not
     * valid vanilla materials (e.g. "myserver:island_tropical"), returns {@link Material#PAPER}
     * as the base item — use {@link #getIconItemStack()} to get the full item with model data.
     * @return the icon material, never null
     */
    public Material getIcon() {
        return ItemParser.parseIconMaterial(icon);
    }

    /**
     * Returns an {@link ItemStack} representing this bundle's icon.
     * <ul>
     *   <li>Plain material name (e.g. {@code "DIAMOND"}) → {@code new ItemStack(Material.DIAMOND)}</li>
     *   <li>Vanilla namespaced material (e.g. {@code "minecraft:diamond"}) → same as above</li>
     *   <li>Custom item-model key (e.g. {@code "myserver:island_tropical"}) → PAPER base item
     *       with the model key set via {@link ItemMeta#setItemModel}</li>
     * </ul>
     * @return ItemStack for this bundle's icon, never null
     * @since 3.0.0
     */
    public ItemStack getIconItemStack() {
        return ItemParser.parseIconItemStack(icon);
    }

    /**
     * Sets the icon from a Material (backward-compatible setter).
     * @param icon the icon material to set; if null, defaults to {@link Material#PAPER}
     */
    public void setIcon(Material icon) {
        this.icon = icon != null ? icon.name() : DEFAULT_ICON;
    }

    /**
     * Sets the icon from a string. Accepts plain material names (e.g. {@code "DIAMOND"}),
     * vanilla namespaced materials (e.g. {@code "minecraft:diamond"}), and custom item-model
     * keys (e.g. {@code "myserver:island_tropical"}).
     * @param icon the icon string; if null, defaults to {@code "PAPER"}
     * @since 3.0.0
     */
    public void setIcon(String icon) {
        this.icon = icon != null ? icon : DEFAULT_ICON;
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

    /**
     * @return the times
     */
    public int getTimes() {
        return times;
    }

    /**
     * @param times the times to set
     */
    public void setTimes(int times) {
        this.times = times;
    }

    /**
     * @return the cost
     */
    public double getCost() {
        return cost;
    }

    /**
     * @param cost the cost to set
     */
    public void setCost(double cost) {
        this.cost = cost;
    }

    /**
     * @return the list of commands to run when this bundle is pasted
     * @since 2.6.0
     */
    public List<String> getCommands() {
        if (commands == null) {
            commands = new ArrayList<>();
        }
        return commands;
    }

    /**
     * @param commands the commands to set
     * @since 2.6.0
     */
    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
}
