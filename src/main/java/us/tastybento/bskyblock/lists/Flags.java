package us.tastybento.bskyblock.lists;

import org.bukkit.Material;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.flags.FlagBuilder;
import us.tastybento.bskyblock.listeners.flags.*;

public class Flags {

    public static final Flag BREAK_BLOCKS = new FlagBuilder().id("BREAK_BLOCKS").icon(Material.STONE).listener(new BreakBlocksListener()).build();
    public static final Flag PLACE_BLOCKS = new FlagBuilder().id("PLACE_BLOCKS").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag ANVIL = new FlagBuilder().id("ANVIL").icon(Material.DIRT).listener(new AnvilListener()).build();
    public static final Flag ARMOR_STAND = new FlagBuilder().id("ARMOR_STAND").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag BEACON = new FlagBuilder().id("BEACON").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag BED = new FlagBuilder().id("BED").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag BREEDING = new FlagBuilder().id("BREEDING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag BREWING = new FlagBuilder().id("BREWING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag BUCKET = new FlagBuilder().id("BUCKET").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag COLLECT_LAVA = new FlagBuilder().id("COLLECT_LAVA").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag COLLECT_WATER = new FlagBuilder().id("COLLECT_WATER").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag CHEST = new FlagBuilder().id("CHEST").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag CHORUS_FRUIT = new FlagBuilder().id("CHORUS_FRUIT").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag CRAFTING = new FlagBuilder().id("CRAFTING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag CROP_TRAMPLE = new FlagBuilder().id("CROP_TRAMPLE").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag DOOR = new FlagBuilder().id("DOOR").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag EGGS = new FlagBuilder().id("EGGS").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag ENCHANTING = new FlagBuilder().id("ENCHANTING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag ENDER_PEARL = new FlagBuilder().id("ENDER_PEARL").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag ENTER_EXIT_MESSAGES = new FlagBuilder().id("ENTER_EXIT_MESSAGES").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag FIRE = new FlagBuilder().id("FIRE").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag FIRE_EXTINGUISH = new FlagBuilder().id("FIRE_EXTINGUISH").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag FIRE_SPREAD = new FlagBuilder().id("FIRE_SPREAD").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag FURNACE = new FlagBuilder().id("FURNACE").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag GATE = new FlagBuilder().id("GATE").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag MOUNT_INVENTORY = new FlagBuilder().id("MOUNT_INVENTORY").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag RIDING = new FlagBuilder().id("RIDING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag HURT_MOBS = new FlagBuilder().id("HURT_MOBS").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag LEASH = new FlagBuilder().id("LEASH").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag LEVER_BUTTON = new FlagBuilder().id("LEVER_BUTTON").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag MOB_SPAWN = new FlagBuilder().id("MOB_SPAWN").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag MUSIC = new FlagBuilder().id("MUSIC").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag PORTAL = new FlagBuilder().id("PORTAL").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag PRESSURE_PLATE = new FlagBuilder().id("PRESSURE_PLATE").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag PVP = new FlagBuilder().id("PVP").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag MILKING = new FlagBuilder().id("MILKING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag NETHER_PVP = new FlagBuilder().id("NETHER_PVP").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag END_PVP = new FlagBuilder().id("END_PVP").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag REDSTONE = new FlagBuilder().id("REDSTONE").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag SPAWN_EGGS = new FlagBuilder().id("SPAWN_EGGS").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag SHEARING = new FlagBuilder().id("SHEARING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag TRADING = new FlagBuilder().id("TRADING").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag VISITOR_ITEM_DROP = new FlagBuilder().id("VISITOR_ITEM_DROP").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    public static final Flag VISITOR_ITEM_PICKUP = new FlagBuilder().id("VISITOR_ITEM_PICKUP").icon(Material.DIRT).listener(new PlaceBlocksListener()).build();
    

}
