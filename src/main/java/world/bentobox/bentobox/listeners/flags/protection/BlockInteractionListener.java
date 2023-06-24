package world.bentobox.bentobox.listeners.flags.protection;

import java.util.Optional;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Waterlogged;
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

/**
 * Handle interaction with blocks
 * @author tastybento
 */
public class BlockInteractionListener extends FlagListener
{

    /**
     * Handle interaction with blocks
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(final PlayerInteractEvent e)
    {
        // We only care about the RIGHT_CLICK_BLOCK action.
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getClickedBlock() == null)
        {
            return;
        }

        // Check clicked block
        this.checkClickedBlock(e, e.getPlayer(), e.getClickedBlock());

        // Now check for in-hand items
        if (e.getItem() != null && !e.getItem().getType().equals(Material.AIR))
        {
            // Boats
            if (Tag.ITEMS_BOATS.isTagged(e.getItem().getType()))
            {
                this.checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BOAT);
            }
            else if (e.getItem().getType().name().endsWith("_SPAWN_EGG"))
            {
                this.checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.SPAWN_EGGS);
            }
            else if (e.getItem().getType() == Material.ENDER_PEARL)
            {
                this.checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.ENDER_PEARL);
            }
            else if (e.getItem().getType() == Material.BONE_MEAL)
            {
                this.checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.PLACE_BLOCKS);
            }
            else if (e.getItem().getType() == Material.GLASS_BOTTLE)
            {
                Block targetedBlock = e.getPlayer().getTargetBlockExact(5, FluidCollisionMode.ALWAYS);

                // Check if player is clicking on water or waterlogged block with a bottle.
                if (targetedBlock != null && (Material.WATER.equals(targetedBlock.getType()) ||
                        targetedBlock.getBlockData() instanceof Waterlogged))
                {
                    this.checkIsland(e, e.getPlayer(), e.getClickedBlock().getLocation(), Flags.BREWING);
                }
            }
        }
    }


    /**
     * Check if an action can occur on a clicked block
     *
     * @param e - event called
     * @param player - player
     * @param block - block being clicked or used
     */
    private void checkClickedBlock(Event e, Player player, Block block)
    {
        Material type = block.getType();
        Location loc = block.getLocation();
        // Handle pots
        if (type.name().startsWith("POTTED"))
        {
            this.checkIsland(e, player, loc, Flags.FLOWER_POT);
            return;
        }

        if (Tag.ANVIL.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.ANVIL);
            return;
        }

        if (Tag.BUTTONS.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.BUTTON);
            return;
        }

        if (Tag.BEDS.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.BED);
            return;
        }

        if (Tag.DOORS.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.DOOR);
            return;
        }

        if (Tag.SHULKER_BOXES.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.SHULKER_BOX);
            return;
        }

        if (Tag.TRAPDOORS.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.TRAPDOOR);
            return;
        }

        if (Tag.SIGNS.isTagged(type) && block instanceof Sign sign && sign.isEditable()) {
            // If waxed, then sign cannot be edited otherwise check
            this.checkIsland(e, player, loc, Flags.SIGN_EDITING);
            return;
        }

        if (Tag.FENCE_GATES.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.GATE);
        }

        if (Tag.ITEMS_CHEST_BOATS.isTagged(type))
        {
            this.checkIsland(e, player, loc, Flags.CHEST);
        }

        switch (type)
        {
        case BEACON -> this.checkIsland(e, player, loc, Flags.BEACON);
        case BREWING_STAND -> this.checkIsland(e, player, loc, Flags.BREWING);
        case BEEHIVE, BEE_NEST -> this.checkIsland(e, player, loc, Flags.HIVE);
        case BARREL -> this.checkIsland(e, player, loc, Flags.BARREL);
        case CHEST, CHEST_MINECART -> this.checkIsland(e, player, loc, Flags.CHEST);
        case TRAPPED_CHEST -> this.checkIsland(e, player, loc, Flags.TRAPPED_CHEST);
        case FLOWER_POT -> this.checkIsland(e, player, loc, Flags.FLOWER_POT);
        case COMPOSTER -> this.checkIsland(e, player, loc, Flags.COMPOSTER);
        case DISPENSER -> this.checkIsland(e, player, loc, Flags.DISPENSER);
        case DROPPER -> this.checkIsland(e, player, loc, Flags.DROPPER);
        case HOPPER, HOPPER_MINECART -> this.checkIsland(e, player, loc, Flags.HOPPER);
        case BLAST_FURNACE, CAMPFIRE, FURNACE_MINECART, FURNACE, SMOKER ->
        this.checkIsland(e, player, loc, Flags.FURNACE);
        case ENCHANTING_TABLE -> this.checkIsland(e, player, loc, Flags.ENCHANTING);
        case ENDER_CHEST -> this.checkIsland(e, player, loc, Flags.ENDER_CHEST);
        case JUKEBOX -> this.checkIsland(e, player, loc, Flags.JUKEBOX);
        case NOTE_BLOCK -> this.checkIsland(e, player, loc, Flags.NOTE_BLOCK);
        case CRAFTING_TABLE, CARTOGRAPHY_TABLE, GRINDSTONE, STONECUTTER, LOOM ->
        this.checkIsland(e, player, loc, Flags.CRAFTING);
        case LEVER -> this.checkIsland(e, player, loc, Flags.LEVER);
        case REDSTONE_WIRE, REPEATER, COMPARATOR, DAYLIGHT_DETECTOR -> this.checkIsland(e, player, loc, Flags.REDSTONE);
        case DRAGON_EGG -> this.checkIsland(e, player, loc, Flags.DRAGON_EGG);
        case END_PORTAL_FRAME, RESPAWN_ANCHOR -> this.checkIsland(e, player, loc, Flags.PLACE_BLOCKS);
        case GLOW_ITEM_FRAME, ITEM_FRAME -> this.checkIsland(e, player, loc, Flags.ITEM_FRAME);
        case SWEET_BERRY_BUSH, CAVE_VINES -> this.checkIsland(e, player, loc, Flags.BREAK_BLOCKS);
        case CAKE -> this.checkIsland(e, player, loc, Flags.CAKE);
        case LAVA_CAULDRON ->
        {
            if (BlockInteractionListener.holds(player, Material.BUCKET))
            {
                this.checkIsland(e, player, loc, Flags.COLLECT_LAVA);
            }
        }
        case WATER_CAULDRON ->
        {
            if (BlockInteractionListener.holds(player, Material.BUCKET))
            {
                this.checkIsland(e, player, loc, Flags.COLLECT_WATER);
            }
            else if (BlockInteractionListener.holds(player, Material.GLASS_BOTTLE) ||
                    BlockInteractionListener.holds(player, Material.POTION))
            {
                this.checkIsland(e, player, loc, Flags.BREWING);
            }
        }
        case POWDER_SNOW_CAULDRON ->
        {
            if (BlockInteractionListener.holds(player, Material.BUCKET))
            {
                this.checkIsland(e, player, loc, Flags.COLLECT_POWDERED_SNOW);
            }
        }
        case CAULDRON ->
        {
            if (BlockInteractionListener.holds(player, Material.WATER_BUCKET) ||
                    BlockInteractionListener.holds(player, Material.LAVA_BUCKET) ||
                    BlockInteractionListener.holds(player, Material.POWDER_SNOW_BUCKET))
            {
                this.checkIsland(e, player, loc, Flags.BUCKET);
            }
            else if (BlockInteractionListener.holds(player, Material.POTION))
            {
                this.checkIsland(e, player, loc, Flags.BREWING);
            }
        }
        default ->
        { // nothing to do
        }
        }
    }


    /**
     * When breaking blocks is allowed, this protects specific blocks from being broken, which would bypass the
     * protection. For example, player enables break blocks, but chests are still protected Fires after the BreakBlocks
     * check.
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent e)
    {
        this.checkClickedBlock(e, e.getPlayer(), e.getBlock());
    }


    /**
     * Prevents dragon eggs from flying out of an island's protected space
     *
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDragonEggTeleport(BlockFromToEvent e)
    {
        Block block = e.getBlock();

        if (!block.getType().equals(Material.DRAGON_EGG) || !this.getIWM().inWorld(block.getLocation()))
        {
            return;
        }

        // If egg starts in a protected island...
        // Cancel if toIsland is not fromIsland or if there is no protected island there
        // This protects against eggs dropping into adjacent islands, e.g. island distance and protection range are equal
        Optional<Island> fromIsland = this.getIslands().getProtectedIslandAt(block.getLocation());
        Optional<Island> toIsland = this.getIslands().getProtectedIslandAt(e.getToBlock().getLocation());
        fromIsland.ifPresent(from -> e.setCancelled(toIsland.map(to -> to != from).orElse(true)));
    }




    /**
     * This method returns if player is holding given material in main or offhand.
     * @param player Player that must be checked.
     * @param material item that mus t be checjed.
     * @return {@code true} if player is holding item in main hand or offhand.
     */
    private static boolean holds(Player player, Material material)
    {
        return player.getInventory().getItemInMainHand().getType().equals(material) ||
                player.getInventory().getItemInOffHand().getType().equals(material);
    }
}