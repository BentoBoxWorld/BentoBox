package world.bentobox.bentobox.hooks;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.eclipse.jdt.annotation.Nullable;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.FlagListener;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Hook to provide ItemsAdder API without the  the Addon needing to call
 * it directly. 
 * Also provides Island Deletion support.
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
     * Remove the CustomBlock at location. This is not an efficient call so use chunk-related calls for large clearances
     * @param location location
     */
    public void clearBlockInfo(Location location) {
        CustomBlock.remove(location);
    }

    /**
     * Returns Optional empty if the provided {@link ItemStack} is not a custom item created with ItemsAdder.
     *
     * @param itemStack the Bukkit ItemStack
     * @return optional namespacedId or empty
     */
    public static Optional<String> getNamespacedId(ItemStack myItemStack) {
        CustomStack stack = CustomStack.byItemStack(myItemStack);
        return Optional.of(stack == null ? null : stack.getNamespacedID());
    }

    /**
     * Returns a list of all the registered blocks identifiers in the format {@code namespace:id}
     * @return a list of Namespaces and IDs in the format {@code namespace:id}
     */
    public static Set<String> getAllBlocks() {
        return CustomBlock.getNamespacedIdsInRegistry();
    }

    /**
     * Gets the Namespace and ID in the format {@code namespace:id} of the placed CustomBlock in a specific location.
     *
     * @param location the location to check
     * @return the Namespace and ID in the format {@code namespace:id} or null if it's not a CustomBlock.
     */
    public static String getInCustomRegion(Location loc) {
        return CustomBlock.Advanced.getInCustomRegion(loc);
    }

    /**
     * Gets the Bukkit ItemStack associated with this CustomStack instance.
     * @param namespacedId name spaced ID
     * @return optional ItemStack
     */
    public static Optional<ItemStack> getItemStack(String namespacedId) {
        CustomBlock cb = getInstance(namespacedId);
        ItemStack item = cb.getItemStack();
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(cb.getDisplayName()));
            item.setItemMeta(meta);
        }
        return Optional.of(item);
    }

    /**
     * Places a CustomBlock provided through the namespace and ID at the provided location. If the item does
     * not exist it will fail silently.
     *
     * @param namespacedId Namespace and ID in the format {@code namespace:id}
     * @param location     The location to place the CustomBlock
     */
    public static void place(String namespacedId, Location loc) {
        CustomBlock.place(namespacedId, loc);
    }

    /**
     * Delete ItemsAdder blocks in the chunk. Used when deleting island
     * @param chunk chunk
     */
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

    /**
     * Returns true if the registry contains a block with the specified namespaced id in the format {@code namespace:id}
     * @param namespacedId Namespace and ID in the format {@code namespace:id}
     * @return true if it contains the namespaced id, otherwise false
     */
    public static boolean isInRegistry(String namespacedId) {
        return CustomBlock.isInRegistry(namespacedId);
    }

    /**
     * Gets a CustomBlock instance through the provided namespace and ID.
     * <br>This may return null if the provided namespace and ID are invalid.
     *
     * @param namespacedID Namespace and ID in the format {@code namespace:id}
     * @return Possibly-null CustomBlock instance.
     */
    @Nullable
    public static CustomBlock getInstance(String namespacedID) {
        return CustomBlock.getInstance(namespacedID);
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
