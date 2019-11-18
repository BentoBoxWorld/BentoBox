package world.bentobox.bentobox.listeners.flags.protection;

import java.util.Map;
import java.util.Optional;

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

import com.google.common.collect.ImmutableMap;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Handle interaction with blocks
 * @author tastybento
 */
public class BlockInteractionListener extends FlagListener {

    /**
     * These cover materials in another server version.
     * This avoids run time errors due to unknown enum values, at the expense of a string comparison
     */
    private final Map<String, String> stringFlags = ImmutableMap.<String, String>builder()
            .build();

    /**
     * Handle interaction with blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        // We only care about the RIGHT_CLICK_BLOCK action.
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        // Check clicked block
        checkClickedBlock(e, e.getPlayer(), e.getClickedBlock().getLocation(), e.getClickedBlock().getType());

        // Now check for in-hand items
        if (e.getItem() != null && !e.getItem().getType().equals(Material.AIR)) {
            // Boats
            if (e.getItem().getType().name().endsWith("_BOAT")) {
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BOAT);
            }
            // Spawn eggs
            else if (e.getItem().getType().name().endsWith("_SPAWN_EGG")) {
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS);
            }
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
                default:
                    break;
                }
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
        if (type == Material.ANVIL
                || type == Material.CHIPPED_ANVIL
                || type == Material.DAMAGED_ANVIL) {
            checkIsland(e, player, loc, Flags.ANVIL);
        } else if (type == Material.BEACON) {
            checkIsland(e, player, loc, Flags.BEACON);
        } else if (type == Material.BLACK_BED
                || type == Material.BLUE_BED
                || type == Material.BROWN_BED
                || type == Material.CYAN_BED
                || type == Material.GRAY_BED
                || type == Material.GREEN_BED
                || type == Material.LIGHT_BLUE_BED
                || type == Material.LIGHT_GRAY_BED
                || type == Material.LIME_BED
                || type == Material.MAGENTA_BED
                || type == Material.ORANGE_BED
                || type == Material.PINK_BED
                || type == Material.PURPLE_BED
                || type == Material.RED_BED
                || type == Material.WHITE_BED
                || type == Material.YELLOW_BED) {
            checkIsland(e, player, loc, Flags.BED);
        } else if (type == Material.BREWING_STAND
                || type == Material.CAULDRON) {
            checkIsland(e, player, loc, Flags.BREWING);
        } else if (type.name().equals("BARREL")
                || type == Material.CHEST
                || type == Material.CHEST_MINECART
                || type == Material.TRAPPED_CHEST
                || type == Material.BLACK_SHULKER_BOX
                || type == Material.BLUE_SHULKER_BOX
                || type == Material.BROWN_SHULKER_BOX
                || type == Material.CYAN_SHULKER_BOX
                || type == Material.GRAY_SHULKER_BOX
                || type == Material.GREEN_SHULKER_BOX
                || type == Material.LIGHT_BLUE_SHULKER_BOX
                || type == Material.LIME_SHULKER_BOX
                || type == Material.PINK_SHULKER_BOX
                || type == Material.MAGENTA_SHULKER_BOX
                || type == Material.ORANGE_SHULKER_BOX
                || type == Material.PURPLE_SHULKER_BOX
                || type == Material.RED_SHULKER_BOX
                || type == Material.LIGHT_GRAY_SHULKER_BOX
                || type == Material.WHITE_SHULKER_BOX
                || type == Material.YELLOW_SHULKER_BOX
                || type == Material.SHULKER_BOX
                || type == Material.FLOWER_POT
                || type.name().equals("COMPOSTER")) {
            checkIsland(e, player, loc, Flags.CONTAINER);
        } else if (type == Material.DISPENSER) {
            checkIsland(e, player, loc, Flags.DISPENSER);
        } else if (type == Material.DROPPER) {
            checkIsland(e, player, loc, Flags.DROPPER);
        } else if (type == Material.HOPPER
                || type == Material.HOPPER_MINECART) {
            checkIsland(e, player, loc, Flags.HOPPER);
        } else if (type == Material.ACACIA_DOOR
                || type == Material.BIRCH_DOOR
                || type == Material.DARK_OAK_DOOR
                || type == Material.IRON_DOOR
                || type == Material.JUNGLE_DOOR
                || type == Material.SPRUCE_DOOR
                || type == Material.OAK_DOOR) {
            checkIsland(e, player, loc, Flags.DOOR);
        } else if (type == Material.ACACIA_TRAPDOOR
                || type == Material.BIRCH_TRAPDOOR
                || type == Material.DARK_OAK_TRAPDOOR
                || type == Material.OAK_TRAPDOOR
                || type == Material.JUNGLE_TRAPDOOR
                || type == Material.SPRUCE_TRAPDOOR
                || type == Material.IRON_TRAPDOOR) {
            checkIsland(e, player, loc, Flags.TRAPDOOR);
        } else if (type == Material.ACACIA_FENCE_GATE
                || type == Material.BIRCH_FENCE_GATE
                || type == Material.DARK_OAK_FENCE_GATE
                || type == Material.OAK_FENCE_GATE
                || type == Material.JUNGLE_FENCE_GATE
                || type == Material.SPRUCE_FENCE_GATE) {
            checkIsland(e, player, loc, Flags.GATE);
        } else if (type.name().equals("BLAST_FURNACE")
                || type.name().equals("CAMPFIRE")
                || type == Material.FURNACE_MINECART
                || type == Material.FURNACE
                || type.name().equals("SMOKER")) {
            checkIsland(e, player, loc, Flags.FURNACE);
        } else if (type == Material.ENCHANTING_TABLE) {
            checkIsland(e, player, loc, Flags.ENCHANTING);
        } else if (type == Material.ENDER_CHEST) {
            checkIsland(e, player, loc, Flags.ENDER_CHEST);
        } else if (type == Material.JUKEBOX) {
            checkIsland(e, player, loc, Flags.JUKEBOX);
        } else if (type == Material.NOTE_BLOCK) {
            checkIsland(e, player, loc, Flags.NOTE_BLOCK);
        } else if (type == Material.CRAFTING_TABLE
                || type.name().equals("CARTOGRAPHY_TABLE")
                || type.name().equals("GRINDSTONE")
                || type.name().equals("STONECUTTER")
                || type.name().equals("LOOM")) {
            checkIsland(e, player, loc, Flags.CRAFTING);
        } else if (type == Material.STONE_BUTTON
                || type == Material.ACACIA_BUTTON
                || type == Material.BIRCH_BUTTON
                || type == Material.DARK_OAK_BUTTON
                || type == Material.JUNGLE_BUTTON
                || type == Material.OAK_BUTTON
                || type == Material.SPRUCE_BUTTON) {
            checkIsland(e, player, loc, Flags.BUTTON);
        } else if (type == Material.LEVER) {
            checkIsland(e, player, loc, Flags.LEVER);
        } else if (type == Material.REPEATER
                || type == Material.COMPARATOR
                || type == Material.DAYLIGHT_DETECTOR) {
            checkIsland(e, player, loc, Flags.REDSTONE);
        } else if (type == Material.DRAGON_EGG) {
            checkIsland(e, player, loc, Flags.DRAGON_EGG);
        } else if (type == Material.END_PORTAL_FRAME) {
            checkIsland(e, player, loc, Flags.PLACE_BLOCKS);
        } else if (type == Material.ITEM_FRAME) {
            checkIsland(e, player, loc, Flags.ITEM_FRAME);
        } else if (type.name().equals("LECTERN")
                || type.name().equals("SWEET_BERRY_BUSH")) {
            checkIsland(e, player, loc, Flags.BREAK_BLOCKS);
        } else if (type == Material.CAKE) {
            checkIsland(e, player, loc, Flags.CAKE);
        } else if (stringFlags.containsKey(type.name())) {
            Optional<Flag> f = BentoBox.getInstance().getFlagsManager().getFlag(stringFlags.get(type.name()));
            f.ifPresent(flag -> checkIsland(e, player, loc, flag));
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
