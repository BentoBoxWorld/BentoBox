package world.bentobox.bentobox.hooks;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenFurniture;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.block.BlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.furniture.FurnitureMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.noteblock.NoteBlockMechanic;
import io.th0rgal.oraxen.mechanics.provided.gameplay.stringblock.StringBlockMechanic;
import io.th0rgal.oraxen.utils.drops.Drop;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;


/**
 * Hook to provide Oraxen API without the  the Addon needing to call
 * it directly. 
 */
public class OraxenHook extends Hook {


    /**
     * Register the hook
     * @param plugin BentoBox
     */
    public OraxenHook(BentoBox plugin) {
        super("Oraxen", Material.ORANGE_BANNER);
    }

    @Override
    public boolean hook() {
        // See if Oraxen is around
        if (Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            return false;
        }
        return true;
    }

    /**
     * Breaks an OraxenBlock at the given location
     *
     * @param location The location of the OraxenBlock
     */
    public void clearBlockInfo(Location location) {
        OraxenBlocks.remove(location, null); // Null player
    }

    /**
     * Returns Optional empty if the provided {@link ItemStack} is not a custom item created with ItemsAdder.
     *
     * @param myItemStack the Bukkit ItemStack
     * @return optional namespacedId or empty
     */
    public static Optional<String> getNamespacedId(ItemStack myItemStack) {
        return Optional.ofNullable(OraxenItems.getIdByItem(myItemStack));
    }

    /**
     * Get all OraxenItem ID's that have either a NoteBlockMechanic or a StringBlockMechanic
     *
     * @return A set of all OraxenItem ID's that have either a NoteBlockMechanic or a StringBlockMechanic
     */
    public static Set<String> getBlockIDs() {   
        return OraxenBlocks.getBlockIDs();
    }

    /**
     * Get all OraxenItem ID's that have a NoteBlockMechanic
     *
     * @return A set of all OraxenItem ID's that have a NoteBlockMechanic
     */
    public static Set<String> getNoteBlockIDs() {
        return OraxenBlocks.getNoteBlockIDs();
    }

    /**
     * Get all OraxenItem ID's that have a StringBlockMechanic
     *
     * @return A set of all OraxenItem ID's that have a StringBlockMechanic
     */
    public static Set<String> getStringBlockIDs() {
        return OraxenBlocks.getStringBlockIDs();
    }

    /**
     * Check if a block is an instance of an OraxenBlock
     *
     * @param block The block to check
     * @return true if the block is an instance of an OraxenBlock, otherwise false
     */
    public static boolean isOraxenBlock(Block block) {
        return OraxenBlocks.isOraxenBlock(block);
    }

    /**
     * Get the OraxenBlock at a location
     *
     * @param location The location to check
     * @return The Mechanic of the OraxenBlock at the location, or null if there is no OraxenBlock at the location.
     * Keep in mind that this method returns the base Mechanic, not the type. Therefore, you will need to cast this to the type you need
     */
    public static Mechanic getOraxenBlock(Location location) {
        return OraxenBlocks.getOraxenBlock(location);
    }

    public static Mechanic getOraxenBlock(BlockData blockData) {
        return OraxenBlocks.getOraxenBlock(blockData);
    }

    public static NoteBlockMechanic getNoteBlockMechanic(BlockData data) {
        return OraxenBlocks.getNoteBlockMechanic(data);
    }

    public static NoteBlockMechanic getNoteBlockMechanic(Block block) {
        return OraxenBlocks.getNoteBlockMechanic(block);
    }

    @org.jetbrains.annotations.Nullable
    public static NoteBlockMechanic getNoteBlockMechanic(String itemID) {
        return OraxenBlocks.getNoteBlockMechanic(itemID);
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getStringMechanic(BlockData blockData) {
        return OraxenBlocks.getStringMechanic(blockData);
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getStringMechanic(Block block) {
        return OraxenBlocks.getStringMechanic(block);
    }

    @org.jetbrains.annotations.Nullable
    public static StringBlockMechanic getStringMechanic(String itemID) {
        return OraxenBlocks.getStringMechanic(itemID);
    }

    @org.jetbrains.annotations.Nullable
    public static BlockMechanic getBlockMechanic(Block block) {
        return OraxenBlocks.getBlockMechanic(block);
    }

    public static void loadItems() {
        OraxenItems.loadItems();
    }

    public static String getIdByItem(final ItemBuilder item) {
        return OraxenItems.getIdByItem(item);
    }

    public static String getIdByItem(final ItemStack item) {
        return OraxenItems.getIdByItem(item);
     }

    public static boolean exists(final String itemId) {
        return OraxenItems.exists(itemId);
    }

    public static boolean exists(final ItemStack itemStack) {
        return OraxenItems.exists(itemStack);
    }

    public static Optional<ItemBuilder> getOptionalItemById(final String id) {
        return OraxenItems.getOptionalItemById(id);
        }

    public static ItemBuilder getItemById(final String id) {
        return OraxenItems.getItemById(id);
    }

    public static ItemBuilder getBuilderByItem(ItemStack item) {
        return OraxenItems.getBuilderByItem(item);
    }

    public static List<ItemBuilder> getUnexcludedItems() {
        return OraxenItems.getUnexcludedItems();
    }

    public static List<ItemBuilder> getUnexcludedItems(final File file) {
        return OraxenItems.getUnexcludedItems(file);
        }

    public static List<ItemStack> getItemStacksByName(final List<List<String>> lists) {
        return OraxenItems.getItemStacksByName(lists);
    }

    public static boolean hasMechanic(String itemID, String mechanicID) {
        return OraxenItems.hasMechanic(itemID, mechanicID);
        }

    public static Map<File, Map<String, ItemBuilder>> getMap() {
        return OraxenItems.getMap();
    }

    public static Map<String, ItemBuilder> getEntriesAsMap() {
        return OraxenItems.getEntriesAsMap();
    }

    public static Set<Entry<String, ItemBuilder>> getEntries() {
        return OraxenItems.getEntries();
    }

    public static Collection<ItemBuilder> getItems() {
        return OraxenItems.getItems();
    }

    public static Set<String> getNames() {
        return OraxenItems.getNames();
    }

    public static String[] nameArray() {
        return OraxenItems.nameArray();
    }

    public static Stream<String> nameStream() {
        return OraxenItems.nameStream();
    }

    public static Stream<ItemBuilder> itemStream() {
        return OraxenItems.itemStream();
    }

    public static Stream<Entry<String, ItemBuilder>> entryStream() {
        return OraxenItems.entryStream();
    }

    public static String[] getItemNames() {
        return OraxenItems.getItemNames();
    }
    
    /**
     * Get all OraxenItem IDs that have a FurnitureMechanic
     *
     * @return a Set of all OraxenItem IDs that have a FurnitureMechanic
     */
    public static Set<String> getFurnitureIDs() {
        return OraxenFurniture.getFurnitureIDs();
        }

    /**
     * Check if a block is an instance of a Furniture
     *
     * @param block The block to check
     * @return true if the block is an instance of a Furniture, otherwise false
     */
    public static boolean isFurniture(Block block) {
        return OraxenFurniture.isFurniture(block);
        }

    /**
     * Check if an itemID has a FurnitureMechanic
     *
     * @param itemID The itemID to check
     * @return true if the itemID has a FurnitureMechanic, otherwise false
     */
    public static boolean isFurniture(String itemID) {
        return OraxenFurniture.isFurniture(itemID);
        }

    public static boolean isFurniture(Entity entity) {
        return OraxenFurniture.isFurniture(entity);
        }

    public static boolean isBaseEntity(Entity entity) {
        return OraxenFurniture.isBaseEntity(entity);
    }

    /*
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player     The player who removed the Furniture, can be null
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(Entity baseEntity, @Nullable Player player) {
        return OraxenFurniture.remove(baseEntity, player);
    }

    /**
     * Removes Furniture at a given Entity, optionally by a player and with an altered Drop
     *
     * @param baseEntity The entity at which the Furniture should be removed
     * @param player     The player who removed the Furniture, can be null
     * @param drop       The drop of the furniture, if null the default drop will be used
     * @return true if the Furniture was removed, false otherwise
     */
    public static boolean remove(Entity baseEntity, @Nullable Player player, @Nullable Drop drop) {  
        return OraxenFurniture.remove(baseEntity, player, drop);
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param block The block to get the FurnitureMechanic from
     * @return Instance of this block's FurnitureMechanic, or null if the block is not tied to a Furniture
     */
    @Nullable
    public static FurnitureMechanic getFurnitureMechanic(Block block) {
        return OraxenFurniture.getFurnitureMechanic(block);
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param entity The entity to get the FurnitureMechanic from
     * @return Returns this entity's FurnitureMechanic, or null if the entity is not tied to a Furniture
     */
    public static FurnitureMechanic getFurnitureMechanic(Entity entity) {
        return OraxenFurniture.getFurnitureMechanic(entity);
    }

    /**
     * Get the FurnitureMechanic from a given block.
     * This will only return non-null for furniture with a barrier-hitbox
     *
     * @param itemID The itemID tied to this FurnitureMechanic
     * @return Returns the FurnitureMechanic tied to this itemID, or null if the itemID is not tied to a Furniture
     */
    public static FurnitureMechanic getFurnitureMechanic(String itemID) {
        return OraxenFurniture.getFurnitureMechanic(itemID);
    }

    /**
     * Ensures that the given entity is a Furniture, and updates it if it is
     *
     * @param entity The furniture baseEntity to update
     */
    public static void updateFurniture(@NotNull Entity entity) {
         OraxenFurniture.updateFurniture(entity);

    }

}
