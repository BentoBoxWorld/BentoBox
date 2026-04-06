package world.bentobox.bentobox.listeners.flags.worldsettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import net.kyori.adventure.text.Component;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.ExpiringSet;
import world.bentobox.bentobox.util.Util;

/**
 * Enables changing of obsidian back into lava
 * For {@link world.bentobox.bentobox.lists.Flags#OBSIDIAN_SCOOPING}
 * @author tastybento
 */
public class ObsidianScoopingListener extends FlagListener {

    private static final String LAVA_TIP_REFERENCE = "protection.flags.OBSIDIAN_SCOOPING.lavaTip";

    /**
     * The preferred order for hologram placement: above, sides, then below.
     */
    private static final BlockFace[] HOLOGRAM_FACES = {
        BlockFace.UP,
        BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
        BlockFace.DOWN
    };

    /**
     * Cooldown to prevent lava duplication by rapid obsidian scooping.
     * Initialized lazily on first use so that the configured duration from settings
     * can be read after BentoBox has fully loaded its configuration.
     * Changes to the cooldown duration in config require a server restart to take effect.
     */
    @SuppressWarnings("java:S3077") // volatile is correct here for double-checked locking lazy init
    private volatile ExpiringSet<UUID> cooldowns;

    /**
     * Returns the cooldown set, initializing it lazily on first use with the
     * configured duration from {@link world.bentobox.bentobox.Settings#getObsidianScoopingCooldown()}.
     *
     * @return the cooldown set
     */
    private ExpiringSet<UUID> getCooldowns() {
        if (cooldowns == null) {
            synchronized (this) {
                if (cooldowns == null) {
                    cooldowns = new ExpiringSet<>(BentoBox.getInstance().getSettings().getObsidianScoopingCooldown(), TimeUnit.MINUTES);
                }
            }
        }
        return cooldowns;
    }

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
     * Shows a hologram tip when obsidian forms from lava and water mixing,
     * if the obsidian could potentially be scooped back into lava.
     *
     * @param e the block form event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onObsidianForm(final BlockFormEvent e) {
        handleObsidianForm(e);
    }

    /**
     * Handles obsidian formation and shows a lava tip hologram if applicable.
     *
     * @param e the block form event
     * @return true if a hologram was spawned, false otherwise
     */
    boolean handleObsidianForm(final BlockFormEvent e) {
        if (!Material.OBSIDIAN.equals(e.getNewState().getType())) {
            return false;
        }
        Block b = e.getBlock();
        if (!getIWM().inWorld(b.getLocation()) || !Flags.OBSIDIAN_SCOOPING.isSetForWorld(b.getWorld())) {
            return false;
        }
        int duration = BentoBox.getInstance().getSettings().getObsidianScoopingLavaTipDuration();
        if (duration <= 0) {
            return false;
        }
        int radius = BentoBox.getInstance().getSettings().getObsidianScoopingRadius();
        // Check if this obsidian is solitary (could be scooped)
        if (radius > 0 && getBlocksAround(b, radius).stream().anyMatch(block -> block.getType().equals(Material.OBSIDIAN))) {
            return false;
        }
        // Find a suitable location for the hologram
        Location holoLoc = findHologramLocation(b);
        if (holoLoc == null) {
            return false;
        }
        // Get the lava tip text from the locale
        String tipText = BentoBox.getInstance().getLocalesManager().getOrDefault(LAVA_TIP_REFERENCE, "");
        if (tipText.isEmpty()) {
            return false;
        }
        Component tipComponent = Util.parseMiniMessage(tipText);
        // Spawn a TextDisplay hologram
        TextDisplay hologram = b.getWorld().spawn(holoLoc, TextDisplay.class, td -> {
            td.text(tipComponent);
            td.setBillboard(Display.Billboard.CENTER);
            td.setSeeThrough(true);
            td.setGravity(false);
        });
        // Schedule removal after the configured duration
        Bukkit.getScheduler().runTaskLater(BentoBox.getInstance(), () -> {
            if (hologram.isValid()) {
                hologram.remove();
            }
        }, duration * 20L);
        return true;
    }

    /**
     * Finds a suitable location for a hologram near the given block.
     * Prefers above the block, then sides, then below.
     * A location is suitable if the block there is air or a liquid.
     *
     * @param b the obsidian block
     * @return a suitable location, or null if none found
     */
    Location findHologramLocation(Block b) {
        for (BlockFace face : HOLOGRAM_FACES) {
            Block relative = b.getRelative(face);
            Material type = relative.getType();
            if (type.isAir() || relative.isLiquid()) {
                return relative.getLocation().add(0.5, 0.5, 0.5);
            }
        }
        return null;
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

        if (Material.BUCKET.equals(e.getPlayer().getInventory().getItemInOffHand().getType()) &&
                Material.BUCKET.equals(e.getPlayer().getInventory().getItemInMainHand().getType())
                && EquipmentSlot.OFF_HAND.equals(e.getHand()))
        {
            // If player is holding bucket in both hands, then allow to interact only with main hand.
            // Prevents lava duplication glitch.
            return false;
        }

        return lookForLava(e);
    }

    /**
     * @param e PlayerInteractEvent
     * @return false if obsidian not scooped, true if scooped
     */
    private boolean lookForLava(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack bucket = e.getItem();

        // Get block player is looking at
        RayTraceResult rtBlocks = e.getPlayer().rayTraceBlocks(5, FluidCollisionMode.ALWAYS);
        if (rtBlocks == null) {
            return false;
        }
        Block b = rtBlocks.getHitBlock();
        if (!b.getType().equals(Material.OBSIDIAN)) {
            // This should not be needed but might catch some attempts
            return false;
        }
        User user = User.getInstance(player);
        if (getIslands().userIsOnIsland(user.getWorld(), user)) {
            // Check cooldown to prevent lava duplication exploit
            if (getCooldowns().contains(player.getUniqueId())) {
                user.sendMessage("protection.flags.OBSIDIAN_SCOOPING.cooldown");
                return false;
            }
            int radius = BentoBox.getInstance().getSettings().getObsidianScoopingRadius();
            // Look around to see if this is a lone obsidian block
            if (radius > 0 && getBlocksAround(b, radius).stream().anyMatch(block -> block.getType().equals(Material.OBSIDIAN))) {
                user.sendMessage("protection.flags.OBSIDIAN_SCOOPING.obsidian-nearby", "[radius]", String.valueOf(radius));
                return false;
            }
            // Add player to cooldown set to prevent rapid scooping
            getCooldowns().add(player.getUniqueId());
            user.sendMessage("protection.flags.OBSIDIAN_SCOOPING.scooping");
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL_LAVA, 1F, 1F);
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(BentoBox.getInstance(), () -> givePlayerLava(player, b, bucket));
            return true;
        }
        return false;

    }

    private void givePlayerLava(Player player, Block b, ItemStack bucket) {
        // Remove one empty bucket and add a lava bucket to the player's inventory
        bucket.setAmount(bucket.getAmount() - 1);
        Map<Integer, ItemStack> map = player.getInventory().addItem(new ItemStack(Material.LAVA_BUCKET));
        if (!map.isEmpty()) {
            map.values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));
        }
        // Set block to air only after giving bucket
        b.setType(Material.AIR);
    }

    private List<Block> getBlocksAround(Block b, int radius) {
        List<Block> blocksAround = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    blocksAround.add(b.getWorld().getBlockAt(b.getX() + x, b.getY() + y, b.getZ() + z));
                }
            }
        }

        // Remove the block at x = 0, y = 0 and z = 0 (which is the base block)
        blocksAround.remove(b);

        return blocksAround;
    }
}
