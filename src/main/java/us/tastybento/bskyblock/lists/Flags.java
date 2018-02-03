package us.tastybento.bskyblock.lists;

import org.bukkit.Material;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
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

/**
 * Protection flags
 * @author tastybento
 *
 */
public class Flags {
    public static Flag ANVIL;
    public static Flag ARMOR_STAND;
    public static Flag BEACON;
    public static Flag BED;
    public static Flag BREAK_BLOCKS;
    public static Flag BREEDING;
    public static Flag BREWING;
    public static Flag BUCKET;
    public static Flag CHEST;
    public static Flag CHORUS_FRUIT;
    public static Flag COLLECT_LAVA;
    public static Flag COLLECT_WATER;
    public static Flag CRAFTING;
    public static Flag CROP_TRAMPLE;
    public static Flag DOOR;
    public static Flag EGGS;
    public static Flag ENCHANTING;
    public static Flag ENDER_PEARL;
    public static Flag ENTER_EXIT_MESSAGES;
    public static Flag FIRE;
    public static Flag FIRE_EXTINGUISH;
    public static Flag FIRE_SPREAD;
    public static Flag FURNACE;
    public static Flag GATE;
    public static Flag HURT_MOBS;
    public static Flag HURT_MONSTERS;
    public static Flag ITEM_DROP;
    public static Flag ITEM_PICKUP;
    public static Flag LEASH;
    public static Flag LEVER_BUTTON;
    public static Flag MILKING;
    public static Flag MOB_SPAWN;
    public static Flag MONSTER_SPAWN;
    public static Flag MOUNT_INVENTORY;
    public static Flag MUSIC;
    public static Flag PLACE_BLOCKS;
    public static Flag PORTAL;
    public static Flag PRESSURE_PLATE;
    public static Flag PVP_END;
    public static Flag PVP_NETHER;
    public static Flag PVP_OVERWORLD;
    public static Flag REDSTONE;
    public static Flag RIDING;
    public static Flag SHEARING;
    public static Flag SPAWN_EGGS;
    public static Flag TRADING;

    private BSkyBlock p;

    public Flags(BSkyBlock plugin) {
        p = plugin;

        // Break and place blocks
        BREAK_BLOCKS = new FlagBuilder().id("BREAK_BLOCKS").icon(Material.STONE).listener(new BreakBlocksListener(p)).build(p);
        PLACE_BLOCKS = new FlagBuilder().id("PLACE_BLOCKS").icon(Material.DIRT).listener(new PlaceBlocksListener(p)).build(p);

        // Block interactions - all use BlockInteractionListener()
        ANVIL = new FlagBuilder().id("ANVIL").icon(Material.ANVIL).listener(new BlockInteractionListener(p)).build(p);
        BEACON = new FlagBuilder().id("BEACON").icon(Material.BEACON).build(p);
        BED = new FlagBuilder().id("BED").icon(Material.BED).build(p);
        BREWING = new FlagBuilder().id("BREWING").icon(Material.BREWING_STAND_ITEM).build(p);
        CHEST = new FlagBuilder().id("CHEST").icon(Material.CHEST).build(p);
        DOOR = new FlagBuilder().id("DOOR").allowedByDefault(true).icon(Material.WOODEN_DOOR).build(p);
        CRAFTING = new FlagBuilder().id("CRAFTING").allowedByDefault(true).icon(Material.WORKBENCH).build(p);
        ENCHANTING = new FlagBuilder().id("ENCHANTING").allowedByDefault(true).icon(Material.ENCHANTMENT_TABLE).build(p);
        FURNACE = new FlagBuilder().id("FURNACE").icon(Material.FURNACE).build(p);
        GATE = new FlagBuilder().id("GATE").allowedByDefault(true).icon(Material.FENCE_GATE).build(p);
        MUSIC = new FlagBuilder().id("MUSIC").icon(Material.JUKEBOX).build(p);
        LEVER_BUTTON = new FlagBuilder().id("LEVER_BUTTON").icon(Material.LEVER).build(p);
        REDSTONE = new FlagBuilder().id("REDSTONE").icon(Material.REDSTONE).build(p);
        SPAWN_EGGS = new FlagBuilder().id("SPAWN_EGGS").icon(Material.MONSTER_EGG).build(p);

        // Entity interactions
        ARMOR_STAND = new FlagBuilder().id("ARMOR_STAND").icon(Material.ARMOR_STAND).listener(new EntityInteractListener(p)).build(p);
        RIDING = new FlagBuilder().id("RIDING").icon(Material.GOLD_BARDING).build(p);
        TRADING = new FlagBuilder().id("TRADING").allowedByDefault(true).icon(Material.EMERALD).build(p);

        // Breeding
        BREEDING = new FlagBuilder().id("BREEDING").icon(Material.CARROT).listener(new BreedingListener(p)).build(p);

        // Buckets. All bucket use is covered by one listener
        BUCKET = new FlagBuilder().id("BUCKET").icon(Material.BUCKET).listener(new BucketListener(p)).build(p);
        COLLECT_LAVA = new FlagBuilder().id("COLLECT_LAVA").icon(Material.LAVA_BUCKET).build(p);
        COLLECT_WATER = new FlagBuilder().id("COLLECT_WATER").icon(Material.WATER_BUCKET).build(p);
        MILKING = new FlagBuilder().id("MILKING").icon(Material.MILK_BUCKET).build(p);    

        // Chorus Fruit and Enderpearls
        CHORUS_FRUIT = new FlagBuilder().id("CHORUS_FRUIT").icon(Material.CHORUS_FRUIT).listener(new TeleportationListener(p)).build(p);
        ENDER_PEARL = new FlagBuilder().id("ENDER_PEARL").icon(Material.ENDER_PEARL).build(p);

        // Physical interactions
        CROP_TRAMPLE = new FlagBuilder().id("CROP_TRAMPLE").icon(Material.WHEAT).listener(new PhysicalInteractionListener(p)).build(p);
        PRESSURE_PLATE = new FlagBuilder().id("PRESSURE_PLATE").icon(Material.GOLD_PLATE).build(p);

        // Egg throwing
        EGGS = new FlagBuilder().id("EGGS").icon(Material.EGG).listener(new EggListener(p)).build(p);

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
        FIRE = new FlagBuilder().id("FIRE").icon(Material.FLINT_AND_STEEL).listener(new FireListener(p)).build(p);
        FIRE_EXTINGUISH = new FlagBuilder().id("FIRE_EXTINGUISH").icon(Material.POTION).build(p);
        FIRE_SPREAD = new FlagBuilder().id("FIRE_SPREAD").icon(Material.FIREWORK_CHARGE).build(p);

        // Inventories
        MOUNT_INVENTORY = new FlagBuilder().id("MOUNT_INVENTORY").icon(Material.IRON_BARDING).listener(new InventoryListener(p)).build(p);

        // Hurting things
        HURT_MOBS = new FlagBuilder().id("HURT_MOBS").icon(Material.STONE_SWORD).listener(new HurtingListener(p)).build(p);
        HURT_MONSTERS = new FlagBuilder().id("HURT_MONSTERS").icon(Material.WOOD_SWORD).build(p);

        // Leashes
        LEASH = new FlagBuilder().id("LEASH").icon(Material.LEASH).listener(new LeashListener(p)).build(p);

        // Portal use protection
        PORTAL = new FlagBuilder().id("PORTAL").icon(Material.OBSIDIAN).listener(new PortalListener(p)).build(p);

        // PVP
        PVP_OVERWORLD = new FlagBuilder().id("PVP_OVERWORLD").icon(Material.ARROW).listener(new PVPListener(p)).build(p);
        PVP_NETHER = new FlagBuilder().id("PVP_NETHER").icon(Material.IRON_AXE).build(p);
        PVP_END = new FlagBuilder().id("PVP_END").icon(Material.END_CRYSTAL).build(p);

        // Shearing
        SHEARING = new FlagBuilder().id("SHEARING").icon(Material.SHEARS).listener(new ShearingListener(p)).build(p);

        // Item pickup or drop
        ITEM_DROP = new FlagBuilder().id("ITEM_DROP").icon(Material.DIRT).allowedByDefault(true).listener(new ItemDropPickUpListener(p)).build(p);
        ITEM_PICKUP = new FlagBuilder().id("ITEM_PICKUP").icon(Material.DIRT).build(p);

        /*
         * Non-protection flags
         */

        ENTER_EXIT_MESSAGES = new FlagBuilder().id("ENTER_EXIT_MESSAGES").icon(Material.DIRT).allowedByDefault(true).build(p);
        MOB_SPAWN = new FlagBuilder().id("MOB_SPAWN").icon(Material.APPLE).allowedByDefault(true).build(p);
        MONSTER_SPAWN = new FlagBuilder().id("MONSTER_SPAWN").icon(Material.MOB_SPAWNER).allowedByDefault(true).build(p);
    }
}
