package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;

/**
 * Enables changing of obsidian back into lava
 * For {@link world.bentobox.bentobox.lists.Flags#OBSIDIAN_SCOOPING}
 * @author tastybento
 */
public class ObsidianScoopingListener extends FlagListener {

    /**
     * Enables changing of obsidian back into lava
     *
     * @param e event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEvent(final PlayerInteractEvent e) {
        onPlayerInteract(e);
    }
    /**
     * Enables changing of obsidian back into lava
     *
     * @param e event
     * @return false if obsidian not scooped, true if scooped
     */
    boolean onPlayerInteract(final PlayerInteractEvent e) {
        if (!getIWM().inWorld(e.getPlayer().getLocation())
                || !Flags.OBSIDIAN_SCOOPING.isSetForWorld(e.getPlayer().getWorld())
                || !e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)
                || !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                || !(e.getItem() != null && e.getItem().getType().equals(Material.BUCKET))
                || !(e.getClickedBlock() != null && e.getClickedBlock().getType().equals(Material.OBSIDIAN))
                || e.getClickedBlock().getRelative(e.getBlockFace()).getType().equals(Material.WATER)) {
            return false;
        }
        return lookForLava(e);
    }

    private boolean lookForLava(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack bucket = e.getItem();

        // Get block player is looking at
        Block b = e.getPlayer().rayTraceBlocks(5, FluidCollisionMode.ALWAYS).getHitBlock();
        if (!b.getType().equals(Material.OBSIDIAN)) {
            // This should not be needed but might catch some attempts
            return false;
        }
        User user = User.getInstance(player);
        if (getIslands().userIsOnIsland(user.getWorld(), user)) {
            // Look around to see if this is a lone obsidian block
            if (getBlocksAround(b).stream().anyMatch(block -> block.getType().equals(Material.OBSIDIAN))) {
                user.sendMessage("protection.flags.OBSIDIAN_SCOOPING.obsidian-nearby");
                return false;
            }
            user.sendMessage("protection.flags.OBSIDIAN_SCOOPING.scooping");
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1F, 1F);
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> givePlayerLava(player, b, bucket));
            return true;
        }
        return false;

    }
    private void givePlayerLava(Player player, Block b, ItemStack bucket) {
        if (bucket.getAmount() == 1) {
            // Needs some special handling when there is only 1 bucket in the stack
            bucket.setType(Material.LAVA_BUCKET);
        } else {
            // Remove one empty bucket and add a lava bucket to the player's inventory
            bucket.setAmount(bucket.getAmount() - 1);
            HashMap<Integer, ItemStack> map = player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET));
            if (!map.isEmpty()) {
                map.values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));
            }
        }
        // Set block to air only after giving bucket
        b.setType(Material.AIR);
    }

    private List<Block> getBlocksAround(Block b) {
        List<Block> blocksAround = new ArrayList<>();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    blocksAround.add(b.getWorld().getBlockAt(b.getX() + x, b.getY() + y, b.getZ() + z));
                }
            }
        }

        // Remove the block at x = 0, y = 0 and z = 0 (which is the base block)
        blocksAround.remove(b);

        return blocksAround;
    }
}
