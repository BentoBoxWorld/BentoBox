package world.bentobox.bentobox.listeners.flags.protection;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.lists.Flags;


/**
 * Listener for {@link Flags#CROP_TRAMPLE}, {@link Flags#PRESSURE_PLATE}, {@link Flags#TURTLE_EGGS}, {@link Flags#BUTTON}
 * @author tastybento
 *
 */
public class PhysicalInteractionListener extends FlagListener
{
    /**
     * Handle physical interaction with blocks
     * Crop trample, pressure plates, triggering redstone, tripwires
     * @param e - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        if (!e.getAction().equals(Action.PHYSICAL) || e.getClickedBlock() == null)
        {
            return;
        }

        if (Tag.PRESSURE_PLATES.isTagged(e.getClickedBlock().getType()))
        {
            // Pressure plates
            this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.PRESSURE_PLATE);
            return;
        }
        if (e.getClickedBlock().getType() == Material.FARMLAND) {
            this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.CROP_TRAMPLE);
        } else if (e.getClickedBlock().getType() == Material.TURTLE_EGG) {
            this.checkIsland(e, e.getPlayer(), e.getPlayer().getLocation(), Flags.TURTLE_EGGS);
        }
    }


    /**
     * Protects buttons and plates from being activated by projectiles
     * @param e  - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(EntityInteractEvent e)
    {
        if (e.getEntity() instanceof Projectile p && p.getShooter() instanceof Player player)
        {
            checkBlocks(e, player, e.getBlock());
        }
    }

    private boolean checkBlocks(Event e, Player player, Block block) {
        Map<Tag<Material>, Flag> TAG_TO_FLAG = Map.of(Tag.WOODEN_BUTTONS, Flags.BUTTON, Tag.PRESSURE_PLATES,
                Flags.PRESSURE_PLATE, Tag.FENCE_GATES, Flags.GATE, Tag.DOORS, Flags.DOOR, Tag.CANDLE_CAKES,
                Flags.CANDLES, Tag.CANDLES, Flags.CANDLES);
        Map<Material, Flag> MAT_TO_FLAG = Map.of(Material.LEVER, Flags.LEVER, Material.TRIPWIRE, Flags.REDSTONE,
                Material.TARGET, Flags.REDSTONE);
        boolean result = TAG_TO_FLAG.entrySet().stream().filter(entry -> entry.getKey().isTagged(block.getType()))
                .findFirst().map(entry -> this.checkIsland(e, player, block.getLocation(), entry.getValue()))
                .orElse(true);
        if (result && MAT_TO_FLAG.containsKey(block.getType())) {
            result = this.checkIsland(e, player, block.getLocation(), MAT_TO_FLAG.get(block.getType()));
        }

        return result;
    }

    /**
     * Protects buttons and plates, etc. from being activated by projectiles that explode
     * @param e  - event
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileExplode(EntityExplodeEvent e) {
        if (e.getEntity() instanceof Projectile p && p.getShooter() instanceof Player player) {
            for (Block b : e.blockList()) {
                this.checkBlocks(e, player, b);
                /*
                 * TODO:
                 * Add protection for candles
                 * 
                 */
            }
        }
    }


}
