/**
 *
 */
package us.tastybento.bskyblock.listeners.flags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import us.tastybento.bskyblock.lists.Flags;

/**
 * @author tastybento
 *
 */
public class BlockInteractionListener extends AbstractFlagListener {

    /**
     * Handle interaction with blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        switch (e.getClickedBlock().getType()) {
        case ANVIL:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.ANVIL);
            break;
        case BEACON:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.BEACON);
            break;
        case BED_BLOCK:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.BED);
            break;
        case BREWING_STAND:
        case CAULDRON:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.BREWING);
            break;
        case CHEST:
        case STORAGE_MINECART:
        case TRAPPED_CHEST:
        case BLACK_SHULKER_BOX:
        case BLUE_SHULKER_BOX:
        case BROWN_SHULKER_BOX:
        case CYAN_SHULKER_BOX:
        case GRAY_SHULKER_BOX:
        case GREEN_SHULKER_BOX:
        case LIGHT_BLUE_SHULKER_BOX:
        case LIME_SHULKER_BOX:
        case PINK_SHULKER_BOX:
        case MAGENTA_SHULKER_BOX:
        case ORANGE_SHULKER_BOX:
        case PURPLE_SHULKER_BOX:
        case RED_SHULKER_BOX:
        case SILVER_SHULKER_BOX:
        case WHITE_SHULKER_BOX:
        case YELLOW_SHULKER_BOX:
        case DISPENSER:
        case DROPPER:
        case HOPPER:
        case HOPPER_MINECART:

            checkIsland(e, e.getClickedBlock().getLocation(), Flags.CHEST);
            break;

        case ACACIA_DOOR:
        case BIRCH_DOOR:
        case DARK_OAK_DOOR:
        case IRON_DOOR:
        case IRON_DOOR_BLOCK:
        case JUNGLE_DOOR:
        case SPRUCE_DOOR:
        case WOODEN_DOOR:
        case WOOD_DOOR:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.DOOR);
            break;
        case TRAP_DOOR:
        case IRON_TRAPDOOR:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.TRAPDOOR);
            break;
        case ACACIA_FENCE_GATE:
        case BIRCH_FENCE_GATE:
        case DARK_OAK_FENCE_GATE:
        case FENCE_GATE:
        case JUNGLE_FENCE_GATE:
        case SPRUCE_FENCE_GATE:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.GATE);
            break;

        case BURNING_FURNACE:
        case FURNACE:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.FURNACE);
            break;
        case ENCHANTMENT_TABLE:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.ENCHANTING);
            break;
        case ENDER_CHEST:
            break;
        case JUKEBOX:
        case NOTE_BLOCK:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.MUSIC);
            break;
        case WORKBENCH:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.CRAFTING);
            break;
        case STONE_BUTTON:
        case WOOD_BUTTON:
        case LEVER:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.LEVER_BUTTON);
            break;
        case DIODE:
        case DIODE_BLOCK_OFF:
        case DIODE_BLOCK_ON:
        case REDSTONE_COMPARATOR_ON:
        case REDSTONE_COMPARATOR_OFF:
        case DAYLIGHT_DETECTOR:
        case DAYLIGHT_DETECTOR_INVERTED:
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.REDSTONE);
            break;
        default:
            break;
        }
        // Now check for in-hand items
        if (e.getItem() != null) {
            switch (e.getItem().getType()) {
            case ENDER_PEARL:
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.ENDER_PEARL);
                break;
            case MONSTER_EGG:
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS);
                break;
            default:
                break;

            }
        }
    }
}
