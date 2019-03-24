package world.bentobox.bentobox.listeners.flags.protection;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

import java.util.Optional;

/**
 * Handle interaction with blocks
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
            checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.ITEM_FRAME);
            return;
        }

        // Otherwise, we just don't care about the RIGHT_CLICK_BLOCK action.
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        // Check clicked block
        checkClickedBlock(e, e.getPlayer(), e.getClickedBlock().getLocation(), e.getClickedBlock().getType());

        // Now check for in-hand items
        if (e.getItem() != null) {
            if (e.getItem().getType().name().contains("BOAT")) {
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
                return;
            }
            switch (e.getItem().getType()) {
            case ENDER_PEARL:
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.ENDER_PEARL);
                break;
            case BONE_MEAL:
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
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
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS);
                break;
            default:
                break;

            }
        }
    }

    /**
     * Check if an action can occur on a clicked block
     * @param e - event called
     * @param player - player
     * @param loc - location of clicked block
     * @param type - material type of clicked block
     */
    private void checkClickedBlock(Event e, Player player, Location loc, Material type) {
        // Handle pots
        if (type.name().startsWith("POTTED")) {
            checkIsland(e, player, loc, Flags.CONTAINER);
            return;
        }
        switch (type) {
        case ANVIL:
            checkIsland(e, player, loc, Flags.ANVIL);
            break;
        case BEACON:
            checkIsland(e, player, loc, Flags.BEACON);
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
            checkIsland(e, player, loc, Flags.BED);
            break;
        case BREWING_STAND:
        case CAULDRON:
            checkIsland(e, player, loc, Flags.BREWING);
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
            checkIsland(e, player, loc, Flags.CONTAINER);
            break;
        case DISPENSER:
            checkIsland(e, player, loc, Flags.DISPENSER);
            break;
        case DROPPER:
            checkIsland(e, player, loc, Flags.DROPPER);
            break;
        case HOPPER:
        case HOPPER_MINECART:
            checkIsland(e, player, loc, Flags.HOPPER);
            break;
        case ACACIA_DOOR:
        case BIRCH_DOOR:
        case DARK_OAK_DOOR:
        case IRON_DOOR:
        case JUNGLE_DOOR:
        case SPRUCE_DOOR:
        case OAK_DOOR:
            checkIsland(e, player, loc, Flags.DOOR);
            break;
        case ACACIA_TRAPDOOR:
        case BIRCH_TRAPDOOR:
        case DARK_OAK_TRAPDOOR:
        case OAK_TRAPDOOR:
        case JUNGLE_TRAPDOOR:
        case SPRUCE_TRAPDOOR:
        case IRON_TRAPDOOR:
            checkIsland(e, player, loc, Flags.TRAPDOOR);
            break;
        case ACACIA_FENCE_GATE:
        case BIRCH_FENCE_GATE:
        case DARK_OAK_FENCE_GATE:
        case OAK_FENCE_GATE:
        case JUNGLE_FENCE_GATE:
        case SPRUCE_FENCE_GATE:
            checkIsland(e, player, loc, Flags.GATE);
            break;
        case FURNACE_MINECART:
        case FURNACE:
            checkIsland(e, player, loc, Flags.FURNACE);
            break;
        case ENCHANTING_TABLE:
            checkIsland(e, player, loc, Flags.ENCHANTING);
            break;
        case ENDER_CHEST:
            checkIsland(e, player, loc, Flags.ENDER_CHEST);
            break;
        case JUKEBOX:
            checkIsland(e, player, loc, Flags.JUKEBOX);
            break;
        case NOTE_BLOCK:
            checkIsland(e, player, loc, Flags.NOTE_BLOCK);
            break;
        case CRAFTING_TABLE:
            checkIsland(e, player, loc, Flags.CRAFTING);
            break;
        case STONE_BUTTON:
        case ACACIA_BUTTON:
        case BIRCH_BUTTON:
        case DARK_OAK_BUTTON:
        case JUNGLE_BUTTON:
        case OAK_BUTTON:
        case SPRUCE_BUTTON:
            checkIsland(e, player, loc, Flags.BUTTON);
            break;
        case LEVER:
            checkIsland(e, player, loc, Flags.LEVER);
            break;
        case REPEATER:
        case COMPARATOR:
        case DAYLIGHT_DETECTOR:
            checkIsland(e, player, loc, Flags.REDSTONE);
            break;
        case DRAGON_EGG:
            checkIsland(e, player, loc, Flags.DRAGON_EGG);
            break;
        case END_PORTAL_FRAME:
            checkIsland(e, player, loc, Flags.PLACE_BLOCKS);
            break;
        case ITEM_FRAME:
            checkIsland(e, player, loc, Flags.ITEM_FRAME);
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
        checkClickedBlock(e, e.getPlayer(), e.getBlock().getLocation(), e.getBlock().getType());
    }

    /**
     * Prevents dragon eggs from flying out of an island's protected space
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDragonEggTeleport(BlockFromToEvent e) {
        Block block = e.getBlock();
        if (!block.getType().equals(Material.DRAGON_EGG) || !getIWM().inWorld(block.getLocation())) {
            return;
        }
        // If egg starts in a protected island...
        // Cancel if toIsland is not fromIsland or if there is no protected island there
        // This protects against eggs dropping into adjacent islands, e.g. island distance and protection range are equal
        Optional<Island> fromIsland = getIslands().getProtectedIslandAt(block.getLocation());
        Optional<Island> toIsland = getIslands().getProtectedIslandAt(e.getToBlock().getLocation());
        fromIsland.ifPresent(from -> e.setCancelled(toIsland.map(to -> to != from).orElse(true)));
    }
}
