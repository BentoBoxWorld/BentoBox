package world.bentobox.bentobox.api.blueprints;

import org.bukkit.Material;

import java.util.List;

/**
 * Represents a bundle of three {@link Blueprint}s.
 * This is what the player will choose when creating his island.
 * @since 1.5.0
 * @author Poslovitch
 */
public class BlueprintBundle {

    private Material icon;
    private String displayName;
    private List<String> description;
    private Blueprint overworldBlueprint;
    private Blueprint netherBlueprint;
    private Blueprint endBlueprint;
}
