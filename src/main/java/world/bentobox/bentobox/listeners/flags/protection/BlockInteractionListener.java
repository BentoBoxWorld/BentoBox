package world.bentobox.bentobox.listeners.flags.protection;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;

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
    private final static Map<String, String> stringFlags;
    static {
        stringFlags = Collections.emptyMap();
    }

    /**
     * Handle interaction with blocks
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e) {
        // We only care about the RIGHT_CLICK_BLOCK action.
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getClickedBlock() == null) {
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
                    case ENDER_PEARL ->
                        checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.ENDER_PEARL);
                    case BONE_MEAL ->
                        checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
                    default -> {}
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
            checkIsland(e, player, loc, Flags.FLOWER_POT);
            return;
        }
        if (Tag.ANVIL.isTagged(type)) {
            checkIsland(e, player, loc, Flags.ANVIL);
            return;
        }
        if (Tag.BUTTONS.isTagged(type)) {
            checkIsland(e, player, loc, Flags.BUTTON);
            return;
        }
        if (Tag.BEDS.isTagged(type)) {
            checkIsland(e, player, loc, Flags.BED);
            return;
        }
        if (Tag.DOORS.isTagged(type)) {
            checkIsland(e, player, loc, Flags.DOOR);
            return;
        }
        if (Tag.SHULKER_BOXES.isTagged(type)) {
            checkIsland(e, player, loc, Flags.SHULKER_BOX);
            return;
        }
        if (Tag.TRAPDOORS.isTagged(type)) {
            checkIsland(e, player, loc, Flags.TRAPDOOR);
            return;
        }

        switch (type)
        {
            case BEACON -> checkIsland(e, player, loc, Flags.BEACON);
            case BREWING_STAND, CAULDRON -> checkIsland(e, player, loc, Flags.BREWING);
            case BEEHIVE, BEE_NEST -> checkIsland(e, player, loc, Flags.HIVE);
            case BARREL -> checkIsland(e, player, loc, Flags.BARREL);
            case CHEST, CHEST_MINECART -> checkIsland(e, player, loc, Flags.CHEST);
            case TRAPPED_CHEST -> checkIsland(e, player, loc, Flags.TRAPPED_CHEST);
            case FLOWER_POT -> checkIsland(e, player, loc, Flags.FLOWER_POT);
            case COMPOSTER -> checkIsland(e, player, loc, Flags.COMPOSTER);
            case DISPENSER -> checkIsland(e, player, loc, Flags.DISPENSER);
            case DROPPER -> checkIsland(e, player, loc, Flags.DROPPER);
            case HOPPER, HOPPER_MINECART -> checkIsland(e, player, loc, Flags.HOPPER);
            case BLAST_FURNACE, CAMPFIRE, FURNACE_MINECART, FURNACE, SMOKER ->
                checkIsland(e, player, loc, Flags.FURNACE);
            case ENCHANTING_TABLE -> checkIsland(e, player, loc, Flags.ENCHANTING);
            case ENDER_CHEST -> checkIsland(e, player, loc, Flags.ENDER_CHEST);
            case JUKEBOX -> checkIsland(e, player, loc, Flags.JUKEBOX);
            case NOTE_BLOCK -> checkIsland(e, player, loc, Flags.NOTE_BLOCK);
            case CRAFTING_TABLE, CARTOGRAPHY_TABLE, GRINDSTONE, STONECUTTER, LOOM ->
                checkIsland(e, player, loc, Flags.CRAFTING);
            case LEVER -> checkIsland(e, player, loc, Flags.LEVER);
            case REDSTONE_WIRE, REPEATER, COMPARATOR, DAYLIGHT_DETECTOR -> checkIsland(e, player, loc, Flags.REDSTONE);
            case DRAGON_EGG -> checkIsland(e, player, loc, Flags.DRAGON_EGG);
            case END_PORTAL_FRAME, RESPAWN_ANCHOR -> checkIsland(e, player, loc, Flags.PLACE_BLOCKS);
            case GLOW_ITEM_FRAME, ITEM_FRAME -> checkIsland(e, player, loc, Flags.ITEM_FRAME);
            case SWEET_BERRY_BUSH -> checkIsland(e, player, loc, Flags.BREAK_BLOCKS);
            case CAKE -> checkIsland(e, player, loc, Flags.CAKE);
            case OAK_FENCE_GATE, SPRUCE_FENCE_GATE, BIRCH_FENCE_GATE, JUNGLE_FENCE_GATE,
                DARK_OAK_FENCE_GATE, ACACIA_FENCE_GATE, CRIMSON_FENCE_GATE, WARPED_FENCE_GATE ->
                checkIsland(e, player, loc, Flags.GATE);
            default -> {
                if (stringFlags.containsKey(type.name())) {
                    Optional<Flag> f = BentoBox.getInstance().getFlagsManager().getFlag(stringFlags.get(type.name()));
                    f.ifPresent(flag -> checkIsland(e, player, loc, flag));
                }
            }
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
