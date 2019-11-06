package world.bentobox.bentobox.listeners.flags.protection;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private final Map<Material, String> inHandItems;

    private final Map<Material, String> clickedBlocks;

    public BlockInteractionListener() {
        inHandItems = new EnumMap<>(Material.class);
        inHandItems.put(Material.ENDER_PEARL, "ENDER_PEARL");
        inHandItems.put(Material.BONE_MEAL, "PLACE_BLOCKS");
        clickedBlocks = new EnumMap<>(Material.class);
        clickedBlocks.put(Material.ANVIL, "ANVIL");
        clickedBlocks.put(Material.CHIPPED_ANVIL, "ANVIL");
        clickedBlocks.put(Material.DAMAGED_ANVIL, "ANVIL");
        clickedBlocks.put(Material.BEACON, "BEACON");
        clickedBlocks.put(Material.BLACK_BED, "BED");
        clickedBlocks.put(Material.BLUE_BED, "BED");
        clickedBlocks.put(Material.BROWN_BED, "BED");
        clickedBlocks.put(Material.CYAN_BED, "BED");
        clickedBlocks.put(Material.GRAY_BED, "BED");
        clickedBlocks.put(Material.GREEN_BED, "BED");
        clickedBlocks.put(Material.LIGHT_BLUE_BED, "BED");
        clickedBlocks.put(Material.LIGHT_GRAY_BED, "BED");
        clickedBlocks.put(Material.LIME_BED, "BED");
        clickedBlocks.put(Material.MAGENTA_BED, "BED");
        clickedBlocks.put(Material.ORANGE_BED, "BED");
        clickedBlocks.put(Material.PINK_BED, "BED");
        clickedBlocks.put(Material.PURPLE_BED, "BED");
        clickedBlocks.put(Material.RED_BED, "BED");
        clickedBlocks.put(Material.WHITE_BED, "BED");
        clickedBlocks.put(Material.YELLOW_BED, "BED");
        clickedBlocks.put(Material.BREWING_STAND, "BREWING");
        clickedBlocks.put(Material.CAULDRON, "BREWING");
        clickedBlocks.put(Material.BARREL, "CONTAINER");
        clickedBlocks.put(Material.CHEST, "CONTAINER");
        clickedBlocks.put(Material.CHEST_MINECART, "CONTAINER");
        clickedBlocks.put(Material.TRAPPED_CHEST, "CONTAINER");
        clickedBlocks.put(Material.BLACK_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.BLUE_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.BROWN_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.CYAN_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.GRAY_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.GREEN_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.LIGHT_BLUE_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.LIME_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.PINK_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.MAGENTA_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.ORANGE_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.PURPLE_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.RED_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.LIGHT_GRAY_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.WHITE_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.YELLOW_SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.SHULKER_BOX, "CONTAINER");
        clickedBlocks.put(Material.FLOWER_POT, "CONTAINER");
        clickedBlocks.put(Material.COMPOSTER, "CONTAINER");
        clickedBlocks.put(Material.DISPENSER, "DISPENSER");
        clickedBlocks.put(Material.DROPPER, "DROPPER");
        clickedBlocks.put(Material.HOPPER, "HOPPER");
        clickedBlocks.put(Material.HOPPER_MINECART, "HOPPER");
        clickedBlocks.put(Material.ACACIA_DOOR, "DOOR");
        clickedBlocks.put(Material.BIRCH_DOOR, "DOOR");
        clickedBlocks.put(Material.DARK_OAK_DOOR, "DOOR");
        clickedBlocks.put(Material.IRON_DOOR, "DOOR");
        clickedBlocks.put(Material.JUNGLE_DOOR, "DOOR");
        clickedBlocks.put(Material.SPRUCE_DOOR, "DOOR");
        clickedBlocks.put(Material.OAK_DOOR, "DOOR");
        clickedBlocks.put(Material.ACACIA_TRAPDOOR, "TRAPDOOR");
        clickedBlocks.put(Material.BIRCH_TRAPDOOR, "TRAPDOOR");
        clickedBlocks.put(Material.DARK_OAK_TRAPDOOR, "TRAPDOOR");
        clickedBlocks.put(Material.OAK_TRAPDOOR, "TRAPDOOR");
        clickedBlocks.put(Material.JUNGLE_TRAPDOOR, "TRAPDOOR");
        clickedBlocks.put(Material.SPRUCE_TRAPDOOR, "TRAPDOOR");
        clickedBlocks.put(Material.IRON_TRAPDOOR, "TRAPDOOR");
        clickedBlocks.put(Material.ACACIA_FENCE_GATE, "GATE");
        clickedBlocks.put(Material.BIRCH_FENCE_GATE, "GATE");
        clickedBlocks.put(Material.DARK_OAK_FENCE_GATE, "GATE");
        clickedBlocks.put(Material.OAK_FENCE_GATE, "GATE");
        clickedBlocks.put(Material.JUNGLE_FENCE_GATE, "GATE");
        clickedBlocks.put(Material.SPRUCE_FENCE_GATE, "GATE");
        clickedBlocks.put(Material.BLAST_FURNACE, "FURNACE");
        clickedBlocks.put(Material.CAMPFIRE, "FURNACE");
        clickedBlocks.put(Material.FURNACE_MINECART, "FURNACE");
        clickedBlocks.put(Material.FURNACE, "FURNACE");
        clickedBlocks.put(Material.SMOKER, "FURNACE");
        clickedBlocks.put(Material.ENCHANTING_TABLE, "ENCHANTING");
        clickedBlocks.put(Material.ENDER_CHEST, "ENDER_CHEST");
        clickedBlocks.put(Material.JUKEBOX, "JUKEBOX");
        clickedBlocks.put(Material.NOTE_BLOCK, "NOTE_BLOCK");
        clickedBlocks.put(Material.CRAFTING_TABLE, "CRAFTING");
        clickedBlocks.put(Material.CARTOGRAPHY_TABLE, "CRAFTING");
        clickedBlocks.put(Material.GRINDSTONE, "CRAFTING");
        clickedBlocks.put(Material.STONECUTTER, "CRAFTING");
        clickedBlocks.put(Material.LOOM, "CRAFTING");
        clickedBlocks.put(Material.STONE_BUTTON, "BUTTON");
        clickedBlocks.put(Material.ACACIA_BUTTON, "BUTTON");
        clickedBlocks.put(Material.BIRCH_BUTTON, "BUTTON");
        clickedBlocks.put(Material.DARK_OAK_BUTTON, "BUTTON");
        clickedBlocks.put(Material.JUNGLE_BUTTON, "BUTTON");
        clickedBlocks.put(Material.OAK_BUTTON, "BUTTON");
        clickedBlocks.put(Material.SPRUCE_BUTTON, "BUTTON");
        clickedBlocks.put(Material.LEVER, "LEVER");
        clickedBlocks.put(Material.REPEATER, "REDSTONE");
        clickedBlocks.put(Material.COMPARATOR, "REDSTONE");
        clickedBlocks.put(Material.DAYLIGHT_DETECTOR, "REDSTONE");
        clickedBlocks.put(Material.DRAGON_EGG, "DRAGON_EGG");
        clickedBlocks.put(Material.END_PORTAL_FRAME, "PLACE_BLOCKS");
        clickedBlocks.put(Material.ITEM_FRAME, "ITEM_FRAME");
        clickedBlocks.put(Material.LECTERN, "BREAK_BLOCKS");
        clickedBlocks.put(Material.SWEET_BERRY_BUSH, "BREAK_BLOCKS");
        clickedBlocks.put(Material.CAKE, "CAKE");
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
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BOAT);
            }
            // Spawn eggs
            else if (e.getItem().getType().name().endsWith("_SPAWN_EGG")) {
                checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS);
            }
            // Other items
            else if (inHandItems.containsKey(e.getItem().getType())) {
                getInHandItemFlag(e.getItem().getType()).ifPresent(f -> checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), f));
            }
        }
    }

    private Optional<Flag> getInHandItemFlag(Material type) {
        return BentoBox.getInstance().getFlagsManager().getFlag(inHandItems.get(type));
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
            getClickedBlockFlag(type).ifPresent(f -> checkIsland(e, player, loc, f));
        }
        if (stringFlags.containsKey(type.name())) {
            Optional<Flag> f = BentoBox.getInstance().getFlagsManager().getFlag(stringFlags.get(type.name()));
            f.ifPresent(flag -> checkIsland(e, player, loc, flag));
        }
    }

    private Optional<Flag> getClickedBlockFlag(Material type) {
        return BentoBox.getInstance().getFlagsManager().getFlag(clickedBlocks.get(type));
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
     * @return the inHandItems with flag values
     */
    public Map<Material, Flag> getInHandItems() {
        return inHandItems.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> BentoBox.getInstance().getFlagsManager().getFlag(e.getValue()).orElse(null)
                        ));
    }

    /**
     * @return the clickedBlocks with flag values
     */
    public Map<Material, Flag> getClickedBlocks() {
        return clickedBlocks.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> BentoBox.getInstance().getFlagsManager().getFlag(e.getValue()).orElse(null)
                        ));
    }
}
