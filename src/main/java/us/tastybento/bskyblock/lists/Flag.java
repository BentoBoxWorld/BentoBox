package us.tastybento.bskyblock.lists;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import us.tastybento.bskyblock.api.flags.FlagType;
import us.tastybento.bskyblock.api.flags.FlagType.Type;
import us.tastybento.bskyblock.api.flags.FlagBuilder;
import us.tastybento.bskyblock.listeners.flags.BlockInteractionListener;
import us.tastybento.bskyblock.listeners.flags.BreakBlocksListener;
import us.tastybento.bskyblock.listeners.flags.BreedingListener;
import us.tastybento.bskyblock.listeners.flags.BucketListener;
import us.tastybento.bskyblock.listeners.flags.EggListener;
import us.tastybento.bskyblock.listeners.flags.EntityInteractListener;
import us.tastybento.bskyblock.listeners.flags.FireListener;
import us.tastybento.bskyblock.listeners.flags.HurtingListener;
import us.tastybento.bskyblock.listeners.flags.InventoryListener;
import us.tastybento.bskyblock.listeners.flags.ItemDropPickUpListener;
import us.tastybento.bskyblock.listeners.flags.LeashListener;
import us.tastybento.bskyblock.listeners.flags.PVPListener;
import us.tastybento.bskyblock.listeners.flags.PhysicalInteractionListener;
import us.tastybento.bskyblock.listeners.flags.PlaceBlocksListener;
import us.tastybento.bskyblock.listeners.flags.PortalListener;
import us.tastybento.bskyblock.listeners.flags.ShearingListener;
import us.tastybento.bskyblock.listeners.flags.TeleportationListener;

public class Flag {

    public static final FlagType BREAK_BLOCKS = new FlagBuilder().id("BREAK_BLOCKS").icon(Material.STONE).listener(new BreakBlocksListener()).build();
    public static final FlagType PLACE_BLOCKS = new FlagBuilder().id("PLACE_BLOCKS").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();

    // Block interactions - all use BlockInteractionListener()
    public static final FlagType ANVIL = new FlagBuilder().id("ANVIL").icon(Material.ANVIL).listener(new BlockInteractionListener()).build();
    public static final FlagType BEACON = new FlagBuilder().id("BEACON").icon(Material.BEACON).build();
    public static final FlagType BED = new FlagBuilder().id("BED").icon(Material.BED).build();
    public static final FlagType BREWING = new FlagBuilder().id("BREWING").icon(Material.BREWING_STAND_ITEM).build();
    public static final FlagType CHEST = new FlagBuilder().id("CHEST").icon(Material.CHEST).build();
    public static final FlagType DOOR = new FlagBuilder().id("DOOR").allowedByDefault(true).icon(Material.WOODEN_DOOR).build();
    public static final FlagType CRAFTING = new FlagBuilder().id("CRAFTING").allowedByDefault(true).icon(Material.WORKBENCH).build();
    public static final FlagType ENCHANTING = new FlagBuilder().id("ENCHANTING").allowedByDefault(true).icon(Material.ENCHANTMENT_TABLE).build();
    public static final FlagType FURNACE = new FlagBuilder().id("FURNACE").icon(Material.FURNACE).build();
    public static final FlagType GATE = new FlagBuilder().id("GATE").allowedByDefault(true).icon(Material.FENCE_GATE).build();
    public static final FlagType MUSIC = new FlagBuilder().id("MUSIC").icon(Material.JUKEBOX).build();
    public static final FlagType LEVER_BUTTON = new FlagBuilder().id("LEVER_BUTTON").icon(Material.LEVER).build();
    public static final FlagType REDSTONE = new FlagBuilder().id("REDSTONE").icon(Material.REDSTONE).build();
    public static final FlagType SPAWN_EGGS = new FlagBuilder().id("SPAWN_EGGS").icon(Material.MONSTER_EGG).build();

    // Entity interactions
    public static final FlagType ARMOR_STAND = new FlagBuilder().id("ARMOR_STAND").icon(Material.ARMOR_STAND).listener(new EntityInteractListener()).build();
    public static final FlagType RIDING = new FlagBuilder().id("RIDING").icon(Material.GOLD_BARDING).build();
    public static final FlagType TRADING = new FlagBuilder().id("TRADING").allowedByDefault(true).icon(Material.EMERALD).build();

    // Breeding
    public static final FlagType BREEDING = new FlagBuilder().id("BREEDING").icon(Material.CARROT).listener(new BreedingListener()).build();

    // Buckets. All bucket use is covered by one listener
    public static final FlagType BUCKET = new FlagBuilder().id("BUCKET").icon(Material.BUCKET).listener(new BucketListener()).build();
    public static final FlagType COLLECT_LAVA = new FlagBuilder().id("COLLECT_LAVA").icon(Material.LAVA_BUCKET).build();
    public static final FlagType COLLECT_WATER = new FlagBuilder().id("COLLECT_WATER").icon(Material.WATER_BUCKET).build();
    public static final FlagType MILKING = new FlagBuilder().id("MILKING").icon(Material.MILK_BUCKET).build();

    // Chorus Fruit and Enderpearls
    public static final FlagType CHORUS_FRUIT = new FlagBuilder().id("CHORUS_FRUIT").icon(Material.CHORUS_FRUIT).listener(new TeleportationListener()).build();
    public static final FlagType ENDER_PEARL = new FlagBuilder().id("ENDER_PEARL").icon(Material.ENDER_PEARL).build();

    // Physical interactions
    public static final FlagType CROP_TRAMPLE = new FlagBuilder().id("CROP_TRAMPLE").icon(Material.WHEAT).listener(new PhysicalInteractionListener()).build();
    public static final FlagType PRESSURE_PLATE = new FlagBuilder().id("PRESSURE_PLATE").icon(Material.GOLD_PLATE).build();

    // Egg throwing
    public static final FlagType EGGS = new FlagBuilder().id("EGGS").icon(Material.EGG).listener(new EggListener()).build();

    /*
     * Fire
     * I'll take you to burn.
     * Fire
     * I'll take you to learn.
     * You gonna burn, burn, burn
     * Fire
     * I'll take you to burn
     * - The Crazy World of Arthur Brown
     */
    public static final FlagType FIRE = new FlagBuilder().id("FIRE").icon(Material.FLINT_AND_STEEL).listener(new FireListener()).build();
    public static final FlagType FIRE_EXTINGUISH = new FlagBuilder().id("FIRE_EXTINGUISH").icon(Material.POTION).build();
    public static final FlagType FIRE_SPREAD = new FlagBuilder().id("FIRE_SPREAD").icon(Material.FIREWORK_CHARGE).build();

    // Inventories
    public static final FlagType MOUNT_INVENTORY = new FlagBuilder().id("MOUNT_INVENTORY").icon(Material.IRON_BARDING).listener(new InventoryListener()).build();

    // Hurting things
    public static final FlagType HURT_MOBS = new FlagBuilder().id("HURT_MOBS").icon(Material.STONE_SWORD).listener(new HurtingListener()).build();
    public static final FlagType HURT_MONSTERS = new FlagBuilder().id("HURT_MONSTERS").icon(Material.WOOD_SWORD).build();

    // Leashes
    public static final FlagType LEASH = new FlagBuilder().id("LEASH").icon(Material.LEASH).listener(new LeashListener()).build();

    // Portal use protection
    public static final FlagType PORTAL = new FlagBuilder().id("PORTAL").icon(Material.OBSIDIAN).listener(new PortalListener()).build();

    // Shearing
    public static final FlagType SHEARING = new FlagBuilder().id("SHEARING").icon(Material.SHEARS).listener(new ShearingListener()).build();

    // Item pickup or drop
    public static final FlagType ITEM_DROP = new FlagBuilder().id("ITEM_DROP").icon(Material.DIRT).allowedByDefault(true).listener(new ItemDropPickUpListener()).build();
    public static final FlagType ITEM_PICKUP = new FlagBuilder().id("ITEM_PICKUP").icon(Material.DIRT).build();

    /*
     * Settings flags (not protection flags)
     */
    // PVP
    public static final FlagType PVP_OVERWORLD = new FlagBuilder().id("PVP_OVERWORLD").icon(Material.ARROW).type(Type.SETTING).listener(new PVPListener()).build();
    public static final FlagType PVP_NETHER = new FlagBuilder().id("PVP_NETHER").icon(Material.IRON_AXE).type(Type.SETTING).build();
    public static final FlagType PVP_END = new FlagBuilder().id("PVP_END").icon(Material.END_CRYSTAL).type(Type.SETTING).build();
    // Others
    public static final FlagType ENTER_EXIT_MESSAGES = new FlagBuilder().id("ENTER_EXIT_MESSAGES").icon(Material.DIRT).allowedByDefault(true).type(Type.SETTING).build();
    public static final FlagType MOB_SPAWN = new FlagBuilder().id("MOB_SPAWN").icon(Material.APPLE).allowedByDefault(true).type(Type.SETTING).build();
    public static final FlagType MONSTER_SPAWN = new FlagBuilder().id("MONSTER_SPAWN").icon(Material.MOB_SPAWNER).allowedByDefault(true).type(Type.SETTING).build();

    /**
     * @return List of all the flags in this class
     */
    public static List<FlagType> values() {
        return Arrays.asList(Flag.class.getFields()).stream().map(field -> {
            try {
                return (FlagType)field.get(null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Bukkit.getLogger().severe("Could not get Flag values " + e.getMessage());
            }
            return null;
        }).collect(Collectors.toList());
    }
}
