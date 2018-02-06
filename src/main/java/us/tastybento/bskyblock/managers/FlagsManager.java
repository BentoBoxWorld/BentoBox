package us.tastybento.bskyblock.managers;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.flags.Flag.FlagType;
import us.tastybento.bskyblock.api.flags.FlagBuilder;
import us.tastybento.bskyblock.api.panels.PanelItem;
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
import us.tastybento.bskyblock.lists.Flags;

public class FlagsManager {

    private BSkyBlock plugin;
    private HashMap<Flags, Flag> flags = new HashMap<>();


    public FlagsManager(BSkyBlock plugin) {
        this.plugin = plugin;

        // Register flags
        registerFlags();
    }

    public void registerFlag(Flag flag) {
        //Bukkit.getLogger().info("DEBUG: registering flag " + flag.getID());
        flags.put(flag.getID(), flag);
        // If there is a listener, register it into Bukkit.
        flag.getListener().ifPresent(l -> Bukkit.getServer().getPluginManager().registerEvents(l, plugin));
    }

    public HashMap<Flags, Flag> getFlags() {
        return flags;
    }

    public Flag getFlagByID(Flags id) {
        //Bukkit.getLogger().info("DEBUG: requesting " + id + " flags size = " + flags.size());
        return flags.get(id);
    }

    public Flag getFlagByIcon(PanelItem item) {
        for (Flag flag : flags.values()) {
            if (flag.getIcon().equals(item)) return flag;
        }
        return null;
    }
    
    private void registerFlags() {

        // Break and place blocks
        registerFlag(new FlagBuilder().id(Flags.BREAK_BLOCKS).icon(Material.STONE).listener(new BreakBlocksListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.PLACE_BLOCKS).icon(Material.DIRT).listener(new PlaceBlocksListener(plugin)).build());

        // Block interactions - all use BlockInteractionListener()
        registerFlag(new FlagBuilder().id(Flags.ANVIL).icon(Material.ANVIL).listener(new BlockInteractionListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.BEACON).icon(Material.BEACON).build());
        registerFlag(new FlagBuilder().id(Flags.BED).icon(Material.BED).build());
        registerFlag(new FlagBuilder().id(Flags.BREWING).icon(Material.BREWING_STAND_ITEM).build());
        registerFlag(new FlagBuilder().id(Flags.CHEST).icon(Material.CHEST).build());
        registerFlag(new FlagBuilder().id(Flags.DOOR).allowedByDefault(true).icon(Material.WOODEN_DOOR).build());
        registerFlag(new FlagBuilder().id(Flags.CRAFTING).allowedByDefault(true).icon(Material.WORKBENCH).build());
        registerFlag(new FlagBuilder().id(Flags.ENCHANTING).allowedByDefault(true).icon(Material.ENCHANTMENT_TABLE).build());
        registerFlag(new FlagBuilder().id(Flags.FURNACE).icon(Material.FURNACE).build());
        registerFlag(new FlagBuilder().id(Flags.GATE).allowedByDefault(true).icon(Material.FENCE_GATE).build());
        registerFlag(new FlagBuilder().id(Flags.MUSIC).icon(Material.JUKEBOX).build());
        registerFlag(new FlagBuilder().id(Flags.LEVER_BUTTON).icon(Material.LEVER).build());
        registerFlag(new FlagBuilder().id(Flags.REDSTONE).icon(Material.REDSTONE).build());
        registerFlag(new FlagBuilder().id(Flags.SPAWN_EGGS).icon(Material.MONSTER_EGG).build());

        // Entity interactions
        registerFlag(new FlagBuilder().id(Flags.ARMOR_STAND).icon(Material.ARMOR_STAND).listener(new EntityInteractListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.RIDING).icon(Material.GOLD_BARDING).build());
        registerFlag(new FlagBuilder().id(Flags.TRADING).allowedByDefault(true).icon(Material.EMERALD).build());

        // Breeding
        registerFlag(new FlagBuilder().id(Flags.BREEDING).icon(Material.CARROT).listener(new BreedingListener(plugin)).build());

        // Buckets. All bucket use is covered by one listener
        registerFlag(new FlagBuilder().id(Flags.BUCKET).icon(Material.BUCKET).listener(new BucketListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.COLLECT_LAVA).icon(Material.LAVA_BUCKET).build());
        registerFlag(new FlagBuilder().id(Flags.COLLECT_WATER).icon(Material.WATER_BUCKET).build());
        registerFlag(new FlagBuilder().id(Flags.MILKING).icon(Material.MILK_BUCKET).build());    

        // Chorus Fruit and Enderpearls
        registerFlag(new FlagBuilder().id(Flags.CHORUS_FRUIT).icon(Material.CHORUS_FRUIT).listener(new TeleportationListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.ENDER_PEARL).icon(Material.ENDER_PEARL).build());

        // Physical interactions
        registerFlag(new FlagBuilder().id(Flags.CROP_TRAMPLE).icon(Material.WHEAT).listener(new PhysicalInteractionListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.PRESSURE_PLATE).icon(Material.GOLD_PLATE).build());

        // Egg throwing
        registerFlag(new FlagBuilder().id(Flags.EGGS).icon(Material.EGG).listener(new EggListener(plugin)).build());

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
        registerFlag(new FlagBuilder().id(Flags.FIRE).icon(Material.FLINT_AND_STEEL).listener(new FireListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.FIRE_EXTINGUISH).icon(Material.POTION).build());
        registerFlag(new FlagBuilder().id(Flags.FIRE_SPREAD).icon(Material.FIREWORK_CHARGE).build());

        // Inventories
        registerFlag(new FlagBuilder().id(Flags.MOUNT_INVENTORY).icon(Material.IRON_BARDING).listener(new InventoryListener(plugin)).build());

        // Hurting things
        registerFlag(new FlagBuilder().id(Flags.HURT_MOBS).icon(Material.STONE_SWORD).listener(new HurtingListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.HURT_MONSTERS).icon(Material.WOOD_SWORD).build());

        // Leashes
        registerFlag(new FlagBuilder().id(Flags.LEASH).icon(Material.LEASH).listener(new LeashListener(plugin)).build());

        // Portal use protection
        registerFlag(new FlagBuilder().id(Flags.PORTAL).icon(Material.OBSIDIAN).listener(new PortalListener(plugin)).build());

        // Shearing
        registerFlag(new FlagBuilder().id(Flags.SHEARING).icon(Material.SHEARS).listener(new ShearingListener(plugin)).build());

        // Item pickup or drop
        registerFlag(new FlagBuilder().id(Flags.ITEM_DROP).icon(Material.DIRT).allowedByDefault(true).listener(new ItemDropPickUpListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.ITEM_PICKUP).icon(Material.DIRT).build());

        /*
         * Settings flags (not protection flags)
         */
        // PVP
        registerFlag(new FlagBuilder().id(Flags.PVP_OVERWORLD).icon(Material.ARROW).type(FlagType.SETTING).listener(new PVPListener(plugin)).build());
        registerFlag(new FlagBuilder().id(Flags.PVP_NETHER).icon(Material.IRON_AXE).type(FlagType.SETTING).build());
        registerFlag(new FlagBuilder().id(Flags.PVP_END).icon(Material.END_CRYSTAL).type(FlagType.SETTING).build());
        // Others
        registerFlag(new FlagBuilder().id(Flags.ENTER_EXIT_MESSAGES).icon(Material.DIRT).allowedByDefault(true).type(FlagType.SETTING).build());
        registerFlag(new FlagBuilder().id(Flags.MOB_SPAWN).icon(Material.APPLE).allowedByDefault(true).type(FlagType.SETTING).build());
        registerFlag(new FlagBuilder().id(Flags.MONSTER_SPAWN).icon(Material.MOB_SPAWNER).allowedByDefault(true).type(FlagType.SETTING).build());

    }

    /**
     * Get flag by string
     * @param key - string name same as the enum
     * @return Flag or null if not known
     */
    public Flag getFlagByID(String key) {
        for (Flags flag: Flags.values()) {
            if (flag.name().equalsIgnoreCase(key)) return this.getFlagByID(flag);
        }
        return null;
    }

    public boolean isDefaultAllowed(Flags flag) {
        if (flags.containsKey(flag)) return flags.get(flag).isDefaultSetting();
        return false;
    }

}
