package world.bentobox.bentobox.listeners.flags;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.AbstractFlagListener;
import world.bentobox.bentobox.lists.Flags;

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
        checkClickedBlock(e, e.getClickedBlock().getLocation(), e.getClickedBlock().getType());

        // Now check for in-hand items
        if (e.getItem() != null) {
            switch (e.getItem().getType()) {
            case ENDER_PEARL:
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.ENDER_PEARL);
                break;
            case BAT_SPAWN_EGG:
            case BLAZE_SPAWN_EGG:
            case CAVE_SPIDER_SPAWN_EGG:
            case CHICKEN_SPAWN_EGG:
            case COD_SPAWN_EGG:
            case COW_SPAWN_EGG:
            case CREEPER_SPAWN_EGG:
            case DOLPHIN_SPAWN_EGG:
            case DONKEY_SPAWN_EGG:
            case DROWNED_SPAWN_EGG:
            case ELDER_GUARDIAN_SPAWN_EGG:
            case ENDERMAN_SPAWN_EGG:
            case ENDERMITE_SPAWN_EGG:
            case EVOKER_SPAWN_EGG:
            case GHAST_SPAWN_EGG:
            case GUARDIAN_SPAWN_EGG:
            case HORSE_SPAWN_EGG:
            case HUSK_SPAWN_EGG:
            case LLAMA_SPAWN_EGG:
            case MAGMA_CUBE_SPAWN_EGG:
            case MOOSHROOM_SPAWN_EGG:
            case MULE_SPAWN_EGG:
            case OCELOT_SPAWN_EGG:
            case PARROT_SPAWN_EGG:
            case PHANTOM_SPAWN_EGG:
            case PIG_SPAWN_EGG:
            case POLAR_BEAR_SPAWN_EGG:
            case PUFFERFISH_SPAWN_EGG:
            case RABBIT_SPAWN_EGG:
            case SALMON_SPAWN_EGG:
            case SHEEP_SPAWN_EGG:
            case SHULKER_SPAWN_EGG:
            case SILVERFISH_SPAWN_EGG:
            case SKELETON_HORSE_SPAWN_EGG:
            case SKELETON_SPAWN_EGG:
            case SLIME_SPAWN_EGG:
            case SPIDER_SPAWN_EGG:
            case SQUID_SPAWN_EGG:
            case STRAY_SPAWN_EGG:
            case TROPICAL_FISH_SPAWN_EGG:
            case TURTLE_SPAWN_EGG:
            case VEX_SPAWN_EGG:
            case VILLAGER_SPAWN_EGG:
            case VINDICATOR_SPAWN_EGG:
            case WITCH_SPAWN_EGG:
            case WITHER_SKELETON_SPAWN_EGG:
            case WOLF_SPAWN_EGG:
            case ZOMBIE_HORSE_SPAWN_EGG:
            case ZOMBIE_PIGMAN_SPAWN_EGG:
            case ZOMBIE_SPAWN_EGG:
            case ZOMBIE_VILLAGER_SPAWN_EGG:
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS);
                break;
            default:
                break;

            }
        }
    }
    
    /**
     * Check if an action can occur on a clicked block
     * @param e - event called
     * @param loc - location of clicked block
     * @param type - material type of clicked block
     */
    private void checkClickedBlock(Event e, Location loc, Material type) {
        switch (type) {
        case ANVIL:
            checkIsland(e, loc, Flags.ANVIL);
            break;
        case BEACON:
            checkIsland(e, loc, Flags.BEACON);
            break;
        case BLACK_BED:
        case BLUE_BED:
        case BROWN_BED:
        case CYAN_BED:
        case GRAY_BED:
        case GREEN_BED:
        case LIGHT_BLUE_BED:
        case LIGHT_GRAY_BED:
        case LIME_BED:
        case MAGENTA_BED:
        case ORANGE_BED:
        case PINK_BED:
        case PURPLE_BED:
        case RED_BED:
        case WHITE_BED:
        case YELLOW_BED:
            checkIsland(e, loc, Flags.BED);
            break;
        case BREWING_STAND:
        case CAULDRON:
            checkIsland(e, loc, Flags.BREWING);
            break;
        case CHEST:
        case CHEST_MINECART:
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
        case LIGHT_GRAY_SHULKER_BOX:
        case WHITE_SHULKER_BOX:
        case YELLOW_SHULKER_BOX:
        case DISPENSER:
        case DROPPER:
        case HOPPER:
        case HOPPER_MINECART:
            checkIsland(e, loc, Flags.CHEST);
            break;
        case ACACIA_DOOR:
        case BIRCH_DOOR:
        case DARK_OAK_DOOR:
        case IRON_DOOR:
        case JUNGLE_DOOR:
        case SPRUCE_DOOR:
        case OAK_DOOR:
            checkIsland(e, loc, Flags.DOOR);
            break;
        case ACACIA_TRAPDOOR:
        case BIRCH_TRAPDOOR:
        case DARK_OAK_TRAPDOOR:
        case OAK_TRAPDOOR:
        case JUNGLE_TRAPDOOR:
        case SPRUCE_TRAPDOOR:
        case IRON_TRAPDOOR:
            checkIsland(e, loc, Flags.TRAPDOOR);
            break;
        case ACACIA_FENCE_GATE:
        case BIRCH_FENCE_GATE:
        case DARK_OAK_FENCE_GATE:
        case OAK_FENCE_GATE:
        case JUNGLE_FENCE_GATE:
        case SPRUCE_FENCE_GATE:
            checkIsland(e, loc, Flags.GATE);
            break;
        case FURNACE:
            checkIsland(e, loc, Flags.FURNACE);
            break;
        case ENCHANTING_TABLE:
            checkIsland(e, loc, Flags.ENCHANTING);
            break;
        case ENDER_CHEST:
            checkIsland(e, loc, Flags.ENDER_CHEST);
            break;
        case JUKEBOX:
            checkIsland(e, loc, Flags.JUKEBOX);
            break;
        case NOTE_BLOCK:
            checkIsland(e, loc, Flags.NOTE_BLOCK);
            break;
        case CRAFTING_TABLE:
            checkIsland(e, loc, Flags.CRAFTING);
            break;
        case STONE_BUTTON:
        case ACACIA_BUTTON:
        case BIRCH_BUTTON:
        case DARK_OAK_BUTTON:
        case JUNGLE_BUTTON:
        case OAK_BUTTON:
        case SPRUCE_BUTTON:
            checkIsland(e, loc, Flags.BUTTON);
            break;
        case LEVER:
            checkIsland(e, loc, Flags.LEVER);
            break;
        case REPEATER:
        case COMPARATOR:
        case DAYLIGHT_DETECTOR:
            checkIsland(e, loc, Flags.REDSTONE);
            break;
        case DRAGON_EGG:
            checkIsland(e, loc, Flags.BREAK_BLOCKS);
            break;
        case END_PORTAL_FRAME:
            checkIsland(e, loc, Flags.PLACE_BLOCKS);
            break;
        default:
            break;

        }
        
    }

    /**
     * Prevents blocks that are protected from being broken, which would bypass the protection
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        checkClickedBlock(e, e.getBlock().getLocation(), e.getBlock().getType());
    }
}
