package us.tastybento.bskyblock.managers;

import java.util.HashMap;

import org.bukkit.Material;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.flags.Flag;
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
import us.tastybento.bskyblock.lists.Flaggs;

public class FlagsManager {

    private BSkyBlock p;

    public FlagsManager(BSkyBlock plugin) {
        this.p = plugin;

        // Register flags
        registerFlags();
    }

    private HashMap<Flaggs, Flag> flags = new HashMap<>();

    public void registerFlag(Flag flag) {
        //TODO all the security checks
        //plugin.getLogger().info("DEBUG: registering flag " + flag.getID());
        flags.put(flag.getID(), flag);
        // If there is a listener, register it into Bukkit.
        flag.getListener().ifPresent(l -> p.getServer().getPluginManager().registerEvents(l, p));
    }

    public HashMap<Flaggs, Flag> getFlags() {
        return flags;
    }

    public Flag getFlagByID(Flaggs id) {
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
        new FlagBuilder().id(Flaggs.BREAK_BLOCKS).icon(Material.STONE).listener(new BreakBlocksListener(p)).build(p);
        new FlagBuilder().id(Flaggs.PLACE_BLOCKS).icon(Material.DIRT).listener(new PlaceBlocksListener(p)).build(p);

        // Block interactions - all use BlockInteractionListener()
        new FlagBuilder().id(Flaggs.ANVIL).icon(Material.ANVIL).listener(new BlockInteractionListener(p)).build(p);
        new FlagBuilder().id(Flaggs.BEACON).icon(Material.BEACON).build(p);
        new FlagBuilder().id(Flaggs.BED).icon(Material.BED).build(p);
        new FlagBuilder().id(Flaggs.BREWING).icon(Material.BREWING_STAND_ITEM).build(p);
        new FlagBuilder().id(Flaggs.CHEST).icon(Material.CHEST).build(p);
        new FlagBuilder().id(Flaggs.DOOR).allowedByDefault(true).icon(Material.WOODEN_DOOR).build(p);
        new FlagBuilder().id(Flaggs.CRAFTING).allowedByDefault(true).icon(Material.WORKBENCH).build(p);
        new FlagBuilder().id(Flaggs.ENCHANTING).allowedByDefault(true).icon(Material.ENCHANTMENT_TABLE).build(p);
        new FlagBuilder().id(Flaggs.FURNACE).icon(Material.FURNACE).build(p);
        new FlagBuilder().id(Flaggs.GATE).allowedByDefault(true).icon(Material.FENCE_GATE).build(p);
        new FlagBuilder().id(Flaggs.MUSIC).icon(Material.JUKEBOX).build(p);
        new FlagBuilder().id(Flaggs.LEVER_BUTTON).icon(Material.LEVER).build(p);
        new FlagBuilder().id(Flaggs.REDSTONE).icon(Material.REDSTONE).build(p);
        new FlagBuilder().id(Flaggs.SPAWN_EGGS).icon(Material.MONSTER_EGG).build(p);

        // Entity interactions
        new FlagBuilder().id(Flaggs.ARMOR_STAND).icon(Material.ARMOR_STAND).listener(new EntityInteractListener(p)).build(p);
        new FlagBuilder().id(Flaggs.RIDING).icon(Material.GOLD_BARDING).build(p);
        new FlagBuilder().id(Flaggs.TRADING).allowedByDefault(true).icon(Material.EMERALD).build(p);

        // Breeding
        new FlagBuilder().id(Flaggs.BREEDING).icon(Material.CARROT).listener(new BreedingListener(p)).build(p);

        // Buckets. All bucket use is covered by one listener
        new FlagBuilder().id(Flaggs.BUCKET).icon(Material.BUCKET).listener(new BucketListener(p)).build(p);
        new FlagBuilder().id(Flaggs.COLLECT_LAVA).icon(Material.LAVA_BUCKET).build(p);
        new FlagBuilder().id(Flaggs.COLLECT_WATER).icon(Material.WATER_BUCKET).build(p);
        new FlagBuilder().id(Flaggs.MILKING).icon(Material.MILK_BUCKET).build(p);    

        // Chorus Fruit and Enderpearls
        new FlagBuilder().id(Flaggs.CHORUS_FRUIT).icon(Material.CHORUS_FRUIT).listener(new TeleportationListener(p)).build(p);
        new FlagBuilder().id(Flaggs.ENDER_PEARL).icon(Material.ENDER_PEARL).build(p);

        // Physical interactions
        new FlagBuilder().id(Flaggs.CROP_TRAMPLE).icon(Material.WHEAT).listener(new PhysicalInteractionListener(p)).build(p);
        new FlagBuilder().id(Flaggs.PRESSURE_PLATE).icon(Material.GOLD_PLATE).build(p);

        // Egg throwing
        new FlagBuilder().id(Flaggs.EGGS).icon(Material.EGG).listener(new EggListener(p)).build(p);

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
        new FlagBuilder().id(Flaggs.FIRE).icon(Material.FLINT_AND_STEEL).listener(new FireListener(p)).build(p);
        new FlagBuilder().id(Flaggs.FIRE_EXTINGUISH).icon(Material.POTION).build(p);
        new FlagBuilder().id(Flaggs.FIRE_SPREAD).icon(Material.FIREWORK_CHARGE).build(p);

        // Inventories
        new FlagBuilder().id(Flaggs.MOUNT_INVENTORY).icon(Material.IRON_BARDING).listener(new InventoryListener(p)).build(p);

        // Hurting things
        new FlagBuilder().id(Flaggs.HURT_MOBS).icon(Material.STONE_SWORD).listener(new HurtingListener(p)).build(p);
        new FlagBuilder().id(Flaggs.HURT_MONSTERS).icon(Material.WOOD_SWORD).build(p);

        // Leashes
        new FlagBuilder().id(Flaggs.LEASH).icon(Material.LEASH).listener(new LeashListener(p)).build(p);

        // Portal use protection
        new FlagBuilder().id(Flaggs.PORTAL).icon(Material.OBSIDIAN).listener(new PortalListener(p)).build(p);

        // Shearing
        new FlagBuilder().id(Flaggs.SHEARING).icon(Material.SHEARS).listener(new ShearingListener(p)).build(p);

        // Item pickup or drop
        new FlagBuilder().id(Flaggs.ITEM_DROP).icon(Material.DIRT).allowedByDefault(true).listener(new ItemDropPickUpListener(p)).build(p);
        new FlagBuilder().id(Flaggs.ITEM_PICKUP).icon(Material.DIRT).build(p);

        /*
         * Non-protection flags
         */
        // PVP
        new FlagBuilder().id(Flaggs.PVP_OVERWORLD).icon(Material.ARROW).listener(new PVPListener(p)).build(p);
        new FlagBuilder().id(Flaggs.PVP_NETHER).icon(Material.IRON_AXE).build(p);
        new FlagBuilder().id(Flaggs.PVP_END).icon(Material.END_CRYSTAL).build(p);
        new FlagBuilder().id(Flaggs.ENTER_EXIT_MESSAGES).icon(Material.DIRT).allowedByDefault(true).build(p);
        new FlagBuilder().id(Flaggs.MOB_SPAWN).icon(Material.APPLE).allowedByDefault(true).build(p);
        new FlagBuilder().id(Flaggs.MONSTER_SPAWN).icon(Material.MOB_SPAWNER).allowedByDefault(true).build(p);

    }

    /**
     * Get flag by string
     * @param key - string name same as the enum
     * @return Flag or null if not known
     */
    public Flag getFlagByID(String key) {
        for (Flaggs flag: Flaggs.values()) {
            if (flag.name().equalsIgnoreCase(key)) return this.getFlagByID(flag);
        }
        return null;
    }

}
