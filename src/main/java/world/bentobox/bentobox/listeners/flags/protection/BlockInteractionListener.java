package world.bentobox.bentobox.listeners.flags.protection;

import java.util.EnumMap;
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

    private final Map<Material, Flag> inHandItems;

    private final Map<Material, Flag> clickedBlocks;

    public BlockInteractionListener() {
        inHandItems = new EnumMap<>(Material.class);
        inHandItems.put(Material.ENDER_PEARL, Flags.ENDER_PEARL);
        inHandItems.put(Material.BONE_MEAL, Flags.PLACE_BLOCKS);
        clickedBlocks = new EnumMap<>(Material.class);
        clickedBlocks.put(Material.ANVIL, Flags.ANVIL);
        clickedBlocks.put(Material.CHIPPED_ANVIL, Flags.ANVIL);
        clickedBlocks.put(Material.DAMAGED_ANVIL, Flags.ANVIL);
        clickedBlocks.put(Material.BEACON, Flags.BEACON);
        clickedBlocks.put(Material.BLACK_BED, Flags.BED);
        clickedBlocks.put(Material.BLUE_BED, Flags.BED);
        clickedBlocks.put(Material.BROWN_BED, Flags.BED);
        clickedBlocks.put(Material.CYAN_BED, Flags.BED);
        clickedBlocks.put(Material.GRAY_BED, Flags.BED);
        clickedBlocks.put(Material.GREEN_BED, Flags.BED);
        clickedBlocks.put(Material.LIGHT_BLUE_BED, Flags.BED);
        clickedBlocks.put(Material.LIGHT_GRAY_BED, Flags.BED);
        clickedBlocks.put(Material.LIME_BED, Flags.BED);
        clickedBlocks.put(Material.MAGENTA_BED, Flags.BED);
        clickedBlocks.put(Material.ORANGE_BED, Flags.BED);
        clickedBlocks.put(Material.PINK_BED, Flags.BED);
        clickedBlocks.put(Material.PURPLE_BED, Flags.BED);
        clickedBlocks.put(Material.RED_BED, Flags.BED);
        clickedBlocks.put(Material.WHITE_BED, Flags.BED);
        clickedBlocks.put(Material.YELLOW_BED, Flags.BED);
        clickedBlocks.put(Material.BREWING_STAND, Flags.BREWING);
        clickedBlocks.put(Material.CAULDRON, Flags.BREWING);
        clickedBlocks.put(Material.BARREL, Flags.CONTAINER);
        clickedBlocks.put(Material.CHEST, Flags.CONTAINER);
        clickedBlocks.put(Material.CHEST_MINECART, Flags.CONTAINER);
        clickedBlocks.put(Material.TRAPPED_CHEST, Flags.CONTAINER);
        clickedBlocks.put(Material.BLACK_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.BLUE_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.BROWN_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.CYAN_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.GRAY_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.GREEN_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.LIGHT_BLUE_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.LIME_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.PINK_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.MAGENTA_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.ORANGE_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.PURPLE_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.RED_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.LIGHT_GRAY_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.WHITE_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.YELLOW_SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.SHULKER_BOX, Flags.CONTAINER);
        clickedBlocks.put(Material.FLOWER_POT, Flags.CONTAINER);
        clickedBlocks.put(Material.COMPOSTER, Flags.CONTAINER);
        clickedBlocks.put(Material.DISPENSER, Flags.DISPENSER);
        clickedBlocks.put(Material.DROPPER, Flags.DROPPER);
        clickedBlocks.put(Material.HOPPER, Flags.HOPPER);
        clickedBlocks.put(Material.HOPPER_MINECART, Flags.HOPPER);
        clickedBlocks.put(Material.ACACIA_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.BIRCH_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.DARK_OAK_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.IRON_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.JUNGLE_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.SPRUCE_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.OAK_DOOR, Flags.DOOR);
        clickedBlocks.put(Material.ACACIA_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.BIRCH_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.DARK_OAK_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.OAK_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.JUNGLE_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.SPRUCE_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.IRON_TRAPDOOR, Flags.TRAPDOOR);
        clickedBlocks.put(Material.ACACIA_FENCE_GATE, Flags.GATE);
        clickedBlocks.put(Material.BIRCH_FENCE_GATE, Flags.GATE);
        clickedBlocks.put(Material.DARK_OAK_FENCE_GATE, Flags.GATE);
        clickedBlocks.put(Material.OAK_FENCE_GATE, Flags.GATE);
        clickedBlocks.put(Material.JUNGLE_FENCE_GATE, Flags.GATE);
        clickedBlocks.put(Material.SPRUCE_FENCE_GATE, Flags.GATE);
        clickedBlocks.put(Material.BLAST_FURNACE, Flags.FURNACE);
        clickedBlocks.put(Material.CAMPFIRE, Flags.FURNACE);
        clickedBlocks.put(Material.FURNACE_MINECART, Flags.FURNACE);
        clickedBlocks.put(Material.FURNACE, Flags.FURNACE);
        clickedBlocks.put(Material.SMOKER, Flags.FURNACE);
        clickedBlocks.put(Material.ENCHANTING_TABLE, Flags.ENCHANTING);
        clickedBlocks.put(Material.ENDER_CHEST, Flags.ENDER_CHEST);
        clickedBlocks.put(Material.JUKEBOX, Flags.JUKEBOX);
        clickedBlocks.put(Material.NOTE_BLOCK, Flags.NOTE_BLOCK);
        clickedBlocks.put(Material.CRAFTING_TABLE, Flags.CRAFTING);
        clickedBlocks.put(Material.CARTOGRAPHY_TABLE, Flags.CRAFTING);
        clickedBlocks.put(Material.GRINDSTONE, Flags.CRAFTING);
        clickedBlocks.put(Material.STONECUTTER, Flags.CRAFTING);
        clickedBlocks.put(Material.LOOM, Flags.CRAFTING);
        clickedBlocks.put(Material.STONE_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.ACACIA_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.BIRCH_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.DARK_OAK_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.JUNGLE_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.OAK_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.SPRUCE_BUTTON, Flags.BUTTON);
        clickedBlocks.put(Material.LEVER, Flags.LEVER);
        clickedBlocks.put(Material.REPEATER, Flags.REDSTONE);
        clickedBlocks.put(Material.COMPARATOR, Flags.REDSTONE);
        clickedBlocks.put(Material.DAYLIGHT_DETECTOR, Flags.REDSTONE);
        clickedBlocks.put(Material.DRAGON_EGG, Flags.DRAGON_EGG);
        clickedBlocks.put(Material.END_PORTAL_FRAME, Flags.PLACE_BLOCKS);
        clickedBlocks.put(Material.ITEM_FRAME, Flags.ITEM_FRAME);
        clickedBlocks.put(Material.LECTERN, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.SWEET_BERRY_BUSH, Flags.BREAK_BLOCKS);
        clickedBlocks.put(Material.CAKE, Flags.CAKE);
    }

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
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
            }
            // Spawn eggs
            else if (e.getItem().getType().name().endsWith("_SPAWN_EGG")) {
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS);
            }
            // Other items
            else if (inHandItems.containsKey(e.getItem().getType())) {
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), inHandItems.get(e.getItem().getType()));
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
        if (clickedBlocks.containsKey(type)) {
            checkIsland(e, player, loc, clickedBlocks.get(type));
        }
        if (stringFlags.containsKey(type.name())) {
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

    /**
     * @return the inHandItems
     */
    public Map<Material, Flag> getInHandItems() {
        return inHandItems;
    }

    /**
     * @return the clickedBlocks
     */
    public Map<Material, Flag> getClickedBlocks() {
        return clickedBlocks;
    }
}
