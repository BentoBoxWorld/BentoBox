package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 */
public class BlockInteractionListener extends FlagListener {

    /**
     * Handle interaction with blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        // For some items, we need to do a specific check for RIGHT_CLICK_BLOCK
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && e.getClickedBlock().getType().equals(Material.ITEM_FRAME)) {
            checkIsland(e, e.getClickedBlock().getLocation(), Flags.ITEM_FRAME, e.getPlayer());
            return;
        }

        // Otherwise, we just don't care about the RIGHT_CLICK_BLOCK action.
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        // Check clicked block
        checkClickedBlock(e, e.getClickedBlock().getLocation(), e.getClickedBlock().getType(), e.getPlayer());

        // Now check for in-hand items
        if (e.getItem() != null) {
            if (e.getItem().getType().name().contains("BOAT")) {
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS, e.getPlayer());
                return;
            }
            switch (e.getItem().getType()) {
            case ENDER_PEARL:
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.ENDER_PEARL, e.getPlayer());
                break;
            case BONE_MEAL:
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS, e.getPlayer());
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
                checkIsland(e, e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS, e.getPlayer());
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
    private void checkClickedBlock(Event e, Location loc, Material type, Player player) {
        // Handle pots
        if (type.name().startsWith("POTTED")) {
            checkIsland(e, loc, Flags.CONTAINER, player);
            return;
        }
        switch (type) {
        case ANVIL:
            checkIsland(e, loc, Flags.ANVIL, player);
            break;
        case BEACON:
            checkIsland(e, loc, Flags.BEACON, player);
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
            checkIsland(e, loc, Flags.BED, player);
            break;
        case BREWING_STAND:
        case CAULDRON:
            checkIsland(e, loc, Flags.BREWING, player);
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
        case SHULKER_BOX:
        case FLOWER_POT:
            checkIsland(e, loc, Flags.CONTAINER, player);
            break;
        case DISPENSER:
            checkIsland(e, loc, Flags.DISPENSER, player);
            break;
        case DROPPER:
            checkIsland(e, loc, Flags.DROPPER, player);
            break;
        case HOPPER:
        case HOPPER_MINECART:
            checkIsland(e, loc, Flags.HOPPER, player);
            break;
        case ACACIA_DOOR:
        case BIRCH_DOOR:
        case DARK_OAK_DOOR:
        case IRON_DOOR:
        case JUNGLE_DOOR:
        case SPRUCE_DOOR:
        case OAK_DOOR:
            checkIsland(e, loc, Flags.DOOR, player);
            break;
        case ACACIA_TRAPDOOR:
        case BIRCH_TRAPDOOR:
        case DARK_OAK_TRAPDOOR:
        case OAK_TRAPDOOR:
        case JUNGLE_TRAPDOOR:
        case SPRUCE_TRAPDOOR:
        case IRON_TRAPDOOR:
            checkIsland(e, loc, Flags.TRAPDOOR, player);
            break;
        case ACACIA_FENCE_GATE:
        case BIRCH_FENCE_GATE:
        case DARK_OAK_FENCE_GATE:
        case OAK_FENCE_GATE:
        case JUNGLE_FENCE_GATE:
        case SPRUCE_FENCE_GATE:
            checkIsland(e, loc, Flags.GATE, player);
            break;
        case FURNACE:
            checkIsland(e, loc, Flags.FURNACE, player);
            break;
        case ENCHANTING_TABLE:
            checkIsland(e, loc, Flags.ENCHANTING, player);
            break;
        case ENDER_CHEST:
            checkIsland(e, loc, Flags.ENDER_CHEST, player);
            break;
        case JUKEBOX:
            checkIsland(e, loc, Flags.JUKEBOX, player);
            break;
        case NOTE_BLOCK:
            checkIsland(e, loc, Flags.NOTE_BLOCK, player);
            break;
        case CRAFTING_TABLE:
            checkIsland(e, loc, Flags.CRAFTING, player);
            break;
        case STONE_BUTTON:
        case ACACIA_BUTTON:
        case BIRCH_BUTTON:
        case DARK_OAK_BUTTON:
        case JUNGLE_BUTTON:
        case OAK_BUTTON:
        case SPRUCE_BUTTON:
            checkIsland(e, loc, Flags.BUTTON, player);
            break;
        case LEVER:
            checkIsland(e, loc, Flags.LEVER, player);
            break;
        case REPEATER:
        case COMPARATOR:
        case DAYLIGHT_DETECTOR:
            checkIsland(e, loc, Flags.REDSTONE, player);
            break;
        case DRAGON_EGG:
            checkIsland(e, loc, Flags.BREAK_BLOCKS, player);
            break;
        case END_PORTAL_FRAME:
            checkIsland(e, loc, Flags.PLACE_BLOCKS, player);
            break;
        case ITEM_FRAME:
            checkIsland(e, loc, Flags.ITEM_FRAME, player);
            break;
        default:
            break;

        }
    }

    /**
     * When breaking blocks is allowed, this protects
     * specific blocks from being broken, which would bypass the protection.
     * For example, player enables break blocks, but chests are still protected
     * Fires after the BreakBlocks check.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e) {
        checkClickedBlock(e, e.getBlock().getLocation(), e.getBlock().getType(), e.getPlayer());
    }
}
