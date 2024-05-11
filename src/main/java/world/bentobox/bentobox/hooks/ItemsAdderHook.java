package world.bentobox.bentobox.hooks;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.eclipse.jdt.annotation.Nullable;

import dev.lone.itemsadder.api.CustomBlock;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Hook to enable itemsadder blocks to be deleted when islands are deleted.
 * It also includes a flag to track explosion access
 */
/*
 * add some methods under CustomBlock#Advanced class.

        public static void deleteAllCustomBlocksInChunk(Chunk chunk)

        @Nullable
        public List<Location> getAllBlocksLocationsList(Chunk chunk)

        @Nullable
        public Map<String, Location> getAllBlocksLocations(Chunk chunk)

        public void runActionOnBlocks(Chunk chunk, BiConsumer<String, Location> action)
 */
public class ItemsAdderHook extends Hook {

    /**
     * This flag allows to switch which island member group can use explosive items from Items Adder.
     */
    public static final Flag ITEMS_ADDER_EXPLOSIONS =
            new Flag.Builder("ITEMS_ADDER_EXPLOSIONS", Material.TNT).
            type(Flag.Type.PROTECTION).
            defaultRank(RanksManager.MEMBER_RANK).
            clickHandler(new CycleClick("ITEMS_ADDER_EXPLOSIONS",
                            RanksManager.VISITOR_RANK, RanksManager.OWNER_RANK))
                    .
            build();

    private BentoBox plugin;

    private BlockInteractListener listener;

    /**
     * Register the hook
     * @param plugin BentoBox
     */
    public ItemsAdderHook(BentoBox plugin) {
        super("ItemsAdder", Material.NETHER_STAR);
        this.plugin = plugin;
    }

    @Override
    public boolean hook() {
        // See if ItemsAdder is around
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return false;
        }
        // Register listener
        listener = new BlockInteractListener();
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        plugin.getFlagsManager().registerFlag(ITEMS_ADDER_EXPLOSIONS);
        return true;
    }

    /**
     * @return the listener
     */
    protected BlockInteractListener getListener() {
        return listener;
    }

    /**
     * Remove the CustomBlock at location
     * @param location
     */
    public void clearBlockInfo(Location location) {
        // TODO: find a more efficient way of doing this.
        // CustomBlock.remove(location);
    }

    public static void deleteAllCustomBlocksInChunk(Chunk chunk) {
        CustomBlock.Advanced.deleteAllCustomBlocksInChunk(chunk);
    }

    @Nullable
    public List<Location> getAllBlocksLocationsList(Chunk chunk) {
        return CustomBlock.Advanced.getAllBlocksLocationsList(chunk);
    }

    @Nullable
    public Map<Location, String> getAllBlocksLocations(Chunk chunk) {
        return CustomBlock.Advanced.getAllBlocksLocations(chunk);
    }

    public void runActionOnBlocks(Chunk chunk, BiConsumer<String, Location> action) {
        CustomBlock.Advanced.runActionOnBlocks(chunk, action);
    }

    class BlockInteractListener extends FlagListener {

        /**
         * Handles explosions of ItemAdder items
         * @param event explosion event
         */
        @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
        public void onExplosion(EntityExplodeEvent event)
        {
            if (!EntityType.PLAYER.equals(event.getEntityType())) {
                // Ignore non-player explosions.
                return;
            }

            Player player = (Player) event.getEntity();

            if (!player.hasPermission("XXXXXX")) {
                // Ignore players that does not have magic XXXXXX permission.
                return;
            }

            // Use BentoBox flag processing system to validate usage.
            // Technically not necessary as internally it should be cancelled by BentoBox.

            if (!this.checkIsland(event, player, event.getLocation(), ITEMS_ADDER_EXPLOSIONS)) {
                // Remove any blocks from the explosion list if required
                event.blockList().removeIf(block -> this.protect(player, block.getLocation()));
                event.setCancelled(this.protect(player, event.getLocation()));
            }
        }


        /**
         * This method returns if the protection in given location is enabled or not.
         * @param player Player who triggers explosion.
         * @param location Location where explosion happens.
         * @return {@code true} if location is protected, {@code false} otherwise.
         */
        private boolean protect(Player player, Location location)
        {
            return plugin.getIslands().getProtectedIslandAt(location)
                    .map(island -> !island.isAllowed(User.getInstance(player), ITEMS_ADDER_EXPLOSIONS)).orElse(false);
        }
    }
}
