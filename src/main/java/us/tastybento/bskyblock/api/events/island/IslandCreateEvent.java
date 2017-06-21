package us.tastybento.bskyblock.api.events.island;

import org.bukkit.entity.Player;

import us.tastybento.bskyblock.api.events.IslandEvent;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a player starts a new island
 * 
 * @author tastybento
 * @since 1.0
 */
public class IslandCreateEvent extends IslandEvent {
    private final Player player;
    //private final Schematic schematic;

    /**
     * @param island
     * @param player
     * @param schematic
     */
    public IslandCreateEvent(Island island, Player player/*, Schematic schematic*/) {
        super(island);
        this.player = player;
        //this.schematic = schematic;
    }

    /**
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    //    /** TODO: schematics
    //     * @return the schematicName
    //     */
    //    public Schematic getSchematicName() {
    //        return schematic;
    //    }
}
