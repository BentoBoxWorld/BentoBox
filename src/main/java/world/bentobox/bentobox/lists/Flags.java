package world.bentobox.bentobox.lists;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.FlagBuilder;
import world.bentobox.bentobox.listeners.flags.BlockInteractionListener;
import world.bentobox.bentobox.listeners.flags.BreakBlocksListener;
import world.bentobox.bentobox.listeners.flags.BreedingListener;
import world.bentobox.bentobox.listeners.flags.BucketListener;
import world.bentobox.bentobox.listeners.flags.ChestDamageListener;
import world.bentobox.bentobox.listeners.flags.CleanSuperFlatListener;
import world.bentobox.bentobox.listeners.flags.CreeperListener;
import world.bentobox.bentobox.listeners.flags.EggListener;
import world.bentobox.bentobox.listeners.flags.EnderChestListener;
import world.bentobox.bentobox.listeners.flags.EndermanListener;
import world.bentobox.bentobox.listeners.flags.EnterExitListener;
import world.bentobox.bentobox.listeners.flags.EntityInteractListener;
import world.bentobox.bentobox.listeners.flags.FireListener;
import world.bentobox.bentobox.listeners.flags.GeoLimitMobsListener;
import world.bentobox.bentobox.listeners.flags.HurtingListener;
import world.bentobox.bentobox.listeners.flags.InventoryListener;
import world.bentobox.bentobox.listeners.flags.InvincibleVisitorsListener;
import world.bentobox.bentobox.listeners.flags.IslandRespawnListener;
import world.bentobox.bentobox.listeners.flags.ItemDropPickUpListener;
import world.bentobox.bentobox.listeners.flags.ItemFrameListener;
import world.bentobox.bentobox.listeners.flags.LeashListener;
import world.bentobox.bentobox.listeners.flags.LockAndBanListener;
import world.bentobox.bentobox.listeners.flags.MobSpawnListener;
import world.bentobox.bentobox.listeners.flags.OfflineRedstoneListener;
import world.bentobox.bentobox.listeners.flags.PVPListener;
import world.bentobox.bentobox.listeners.flags.PhysicalInteractionListener;
import world.bentobox.bentobox.listeners.flags.PistonPushListener;
import world.bentobox.bentobox.listeners.flags.PlaceBlocksListener;
import world.bentobox.bentobox.listeners.flags.PortalListener;
import world.bentobox.bentobox.listeners.flags.RemoveMobsListener;
import world.bentobox.bentobox.listeners.flags.ShearingListener;
import world.bentobox.bentobox.listeners.flags.TNTListener;
import world.bentobox.bentobox.listeners.flags.TeleportationListener;
import world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener;
import world.bentobox.bentobox.listeners.flags.clicklisteners.GeoLimitClickListener;
import world.bentobox.bentobox.managers.RanksManager;

public class Flags {

    private Flags() {}

    // Disabled setting 'rank'
    private static final int DISABLED = -1;

    public static final Flag BREAK_BLOCKS = new FlagBuilder().id("BREAK_BLOCKS").icon(Material.STONE).listener(new BreakBlocksListener()).build();
    public static final Flag PLACE_BLOCKS = new FlagBuilder().id("PLACE_BLOCKS").icon(Material.GRASS).listener(new PlaceBlocksListener()).build();

    // Block interactions - all use BlockInteractionListener()
    public static final Flag ANVIL = new FlagBuilder().id("ANVIL").icon(Material.ANVIL).listener(new BlockInteractionListener()).build();
    public static final Flag BEACON = new FlagBuilder().id("BEACON").icon(Material.BEACON).build();
    public static final Flag BED = new FlagBuilder().id("BED").icon(Material.RED_BED).build();
    public static final Flag BREWING = new FlagBuilder().id("BREWING").icon(Material.BREWING_STAND).build();
    public static final Flag CHEST = new FlagBuilder().id("CHEST").icon(Material.CHEST).build();
    public static final Flag DOOR = new FlagBuilder().id("DOOR").allowedByDefault(true).icon(Material.OAK_DOOR).build();
    public static final Flag TRAPDOOR = new FlagBuilder().id("TRAPDOOR").allowedByDefault(true).icon(Material.OAK_TRAPDOOR).build();
    public static final Flag CRAFTING = new FlagBuilder().id("CRAFTING").allowedByDefault(true).icon(Material.CRAFTING_TABLE).build();
    public static final Flag ENCHANTING = new FlagBuilder().id("ENCHANTING").allowedByDefault(true).icon(Material.ENCHANTING_TABLE).build();
    public static final Flag FURNACE = new FlagBuilder().id("FURNACE").icon(Material.FURNACE).build();
    public static final Flag GATE = new FlagBuilder().id("GATE").allowedByDefault(true).icon(Material.OAK_FENCE_GATE).build();
    public static final Flag NOTE_BLOCK = new FlagBuilder().id("NOTE_BLOCK").icon(Material.NOTE_BLOCK).build();
    public static final Flag JUKEBOX = new FlagBuilder().id("JUKEBOX").icon(Material.JUKEBOX).build();
    public static final Flag LEVER = new FlagBuilder().id("LEVER").icon(Material.LEVER).build();
    public static final Flag BUTTON = new FlagBuilder().id("BUTTON").icon(Material.OAK_BUTTON).build();
    public static final Flag REDSTONE = new FlagBuilder().id("REDSTONE").icon(Material.REDSTONE).build();
    public static final Flag SPAWN_EGGS = new FlagBuilder().id("SPAWN_EGGS").icon(Material.COW_SPAWN_EGG).build();

    // Entity interactions
    public static final Flag ARMOR_STAND = new FlagBuilder().id("ARMOR_STAND").icon(Material.ARMOR_STAND).listener(new EntityInteractListener()).build();
    public static final Flag RIDING = new FlagBuilder().id("RIDING").icon(Material.GOLDEN_HORSE_ARMOR).build();
    public static final Flag TRADING = new FlagBuilder().id("TRADING").allowedByDefault(true).icon(Material.EMERALD).build();

    // Breeding
    public static final Flag BREEDING = new FlagBuilder().id("BREEDING").icon(Material.CARROT).listener(new BreedingListener()).build();

    // Buckets. All bucket use is covered by one listener
    public static final Flag BUCKET = new FlagBuilder().id("BUCKET").icon(Material.BUCKET).listener(new BucketListener()).build();
    public static final Flag COLLECT_LAVA = new FlagBuilder().id("COLLECT_LAVA").icon(Material.LAVA_BUCKET).build();
    public static final Flag COLLECT_WATER = new FlagBuilder().id("COLLECT_WATER").icon(Material.WATER_BUCKET).build();
    public static final Flag MILKING = new FlagBuilder().id("MILKING").icon(Material.MILK_BUCKET).build();

    // Chorus Fruit and Enderpearls
    public static final Flag CHORUS_FRUIT = new FlagBuilder().id("CHORUS_FRUIT").icon(Material.CHORUS_FRUIT).listener(new TeleportationListener()).build();
    public static final Flag ENDER_PEARL = new FlagBuilder().id("ENDER_PEARL").icon(Material.ENDER_PEARL).build();

    // Physical interactions
    public static final Flag CROP_TRAMPLE = new FlagBuilder().id("CROP_TRAMPLE").icon(Material.WHEAT).listener(new PhysicalInteractionListener()).build();
    public static final Flag PRESSURE_PLATE = new FlagBuilder().id("PRESSURE_PLATE").icon(Material.STONE_PRESSURE_PLATE).build();

    // Egg throwing
    public static final Flag EGGS = new FlagBuilder().id("EGGS").icon(Material.EGG).listener(new EggListener()).build();

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
    public static final Flag FIRE = new FlagBuilder().id("FIRE").icon(Material.FLINT_AND_STEEL).listener(new FireListener()).build();
    public static final Flag FIRE_EXTINGUISH = new FlagBuilder().id("FIRE_EXTINGUISH").icon(Material.POTION).build();

    // Inventories
    public static final Flag MOUNT_INVENTORY = new FlagBuilder().id("MOUNT_INVENTORY").icon(Material.IRON_HORSE_ARMOR).listener(new InventoryListener()).build();

    // Hurting things
    public static final Flag HURT_ANIMALS = new FlagBuilder().id("HURT_ANIMALS").icon(Material.STONE_SWORD).listener(new HurtingListener()).build();
    public static final Flag HURT_MONSTERS = new FlagBuilder().id("HURT_MONSTERS").icon(Material.WOODEN_SWORD).build();
    public static final Flag HURT_VILLAGERS = new FlagBuilder().id("HURT_VILLAGERS").icon(Material.GOLDEN_SWORD).build();

    // Leashes
    public static final Flag LEASH = new FlagBuilder().id("LEASH").icon(Material.LEAD).listener(new LeashListener()).build();

    // Portal use protection
    public static final Flag PORTAL = new FlagBuilder().id("PORTAL").icon(Material.OBSIDIAN).listener(new PortalListener()).build();

    // Shearing
    public static final Flag SHEARING = new FlagBuilder().id("SHEARING").icon(Material.SHEARS).listener(new ShearingListener()).build();

    // Item pickup or drop
    public static final Flag ITEM_DROP = new FlagBuilder().id("ITEM_DROP").icon(Material.BEETROOT_SOUP).allowedByDefault(true).listener(new ItemDropPickUpListener()).build();
    public static final Flag ITEM_PICKUP = new FlagBuilder().id("ITEM_PICKUP").icon(Material.BEETROOT_SEEDS).build();

    // TNT
    public static final Flag TNT = new FlagBuilder().id("TNT").icon(Material.TNT).listener(new TNTListener()).allowedByDefault(false).type(Type.PROTECTION).build();

    // Island lock
    public static final Flag LOCK = new FlagBuilder().id("LOCK")
            .icon(Material.TRIPWIRE_HOOK).type(Type.PROTECTION).allowedByDefault(true)
            .defaultRank(RanksManager.VISITOR_RANK).listener(new LockAndBanListener())
            .build();

    /*
     * Settings flags (not protection flags)
     */
    // PVP
    public static final Flag PVP_OVERWORLD = new FlagBuilder().id("PVP_OVERWORLD").icon(Material.ARROW).type(Type.SETTING)
            .defaultRank(DISABLED).listener(new PVPListener()).build();
    public static final Flag PVP_NETHER = new FlagBuilder().id("PVP_NETHER").icon(Material.IRON_AXE).type(Type.SETTING)
            .defaultRank(DISABLED).build();
    public static final Flag PVP_END = new FlagBuilder().id("PVP_END").icon(Material.END_CRYSTAL).type(Type.SETTING)
            .defaultRank(DISABLED).build();

    // Others
    public static final Flag ANIMAL_SPAWN = new FlagBuilder().id("ANIMAL_SPAWN").icon(Material.APPLE).allowedByDefault(true).type(Type.SETTING)
            .listener(new MobSpawnListener()).build();
    public static final Flag MONSTER_SPAWN = new FlagBuilder().id("MONSTER_SPAWN").icon(Material.SPAWNER).allowedByDefault(true).type(Type.SETTING).build();
    public static final Flag FIRE_SPREAD = new FlagBuilder().id("FIRE_SPREAD").icon(Material.FIREWORK_STAR).allowedByDefault(true).type(Type.SETTING).build();

    /*
     * World Settings - they apply to every island in the game worlds.
     */

    // World Settings - apply to every island in the game worlds
    public static final Flag ENDER_CHEST = new FlagBuilder().id("ENDER_CHEST").icon(Material.ENDER_CHEST)
            .allowedByDefault(false).type(Type.WORLD_SETTING)
            .listener(new EnderChestListener())
            .build();

    public static final Flag ENDERMAN_GRIEFING = new FlagBuilder().id("ENDERMAN_GRIEFING").icon(Material.END_STONE_BRICKS)
            .allowedByDefault(true).type(Type.WORLD_SETTING)
            .listener(new EndermanListener())
            .build();

    public static final Flag ENTER_EXIT_MESSAGES = new FlagBuilder().id("ENTER_EXIT_MESSAGES").icon(Material.DIRT).allowedByDefault(true).type(Type.WORLD_SETTING)
            .listener(new EnterExitListener())
            .build();

    public static final Flag PISTON_PUSH = new FlagBuilder().id("PISTON_PUSH").icon(Material.PISTON).allowedByDefault(true).type(Type.WORLD_SETTING)
            .listener(new PistonPushListener())
            .build();

    private static InvincibleVisitorsListener ilv = new InvincibleVisitorsListener();
    public static final Flag INVINCIBLE_VISITORS = new FlagBuilder().id("INVINCIBLE_VISITORS").icon(Material.DIAMOND_CHESTPLATE).type(Type.WORLD_SETTING)
            .listener(ilv).onClick(ilv).subPanel(true).build();

    public static final Flag GEO_LIMIT_MOBS = new FlagBuilder().id("GEO_LIMIT_MOBS").icon(Material.CHAINMAIL_CHESTPLATE).type(Type.WORLD_SETTING)
            .listener(new GeoLimitMobsListener()).onClick(new GeoLimitClickListener()).subPanel(true).build();

    public static final Flag REMOVE_MOBS = new FlagBuilder().id("REMOVE_MOBS").icon(Material.GLOWSTONE_DUST).type(Type.WORLD_SETTING)
            .listener(new RemoveMobsListener()).allowedByDefault(true).build();

    public static final Flag ITEM_FRAME_DAMAGE = new FlagBuilder().id("ITEM_FRAME_DAMAGE").icon(Material.ITEM_FRAME).type(Type.WORLD_SETTING)
            .listener(new ItemFrameListener()).allowedByDefault(false).build();

    public static final Flag ISLAND_RESPAWN = new FlagBuilder().id("ISLAND_RESPAWN").icon(Material.TORCH).type(Type.WORLD_SETTING)
            .listener(new IslandRespawnListener()).allowedByDefault(true).build();

    public static final Flag OFFLINE_REDSTONE = new FlagBuilder().id("OFFLINE_REDSTONE").icon(Material.COMPARATOR).type(Type.WORLD_SETTING)
            .listener(new OfflineRedstoneListener()).allowedByDefault(true).build();

    public static final Flag CLEAN_SUPER_FLAT = new FlagBuilder().id("CLEAN_SUPER_FLAT").icon(Material.BEDROCK).type(Type.WORLD_SETTING)
            .listener(new CleanSuperFlatListener()).allowedByDefault(false).build();

    public static final Flag CHEST_DAMAGE = new FlagBuilder().id("CHEST_DAMAGE").icon(Material.TRAPPED_CHEST).type(Type.WORLD_SETTING)
            .listener(new ChestDamageListener()).allowedByDefault(false).build();
    public static final Flag CREEPER_DAMAGE = new FlagBuilder().id("CREEPER_DAMAGE").listener(new CreeperListener()).icon(Material.GREEN_SHULKER_BOX).type(Type.WORLD_SETTING)
            .allowedByDefault(true).build();
    /**
     * Prevents creeper griefing. This is where a visitor will trigger a creeper to blow up an island.
     */
    public static final Flag CREEPER_GRIEFING = new FlagBuilder().id("CREEPER_GRIEFING").icon(Material.CREEPER_HEAD).type(Type.WORLD_SETTING)
            .allowedByDefault(false).build();
    
    public static final Flag COMMAND_RANKS = new FlagBuilder().id("COMMAND_RANKS").icon(Material.PLAYER_HEAD).type(Type.WORLD_SETTING)
            .onClick(new CommandRankClickListener()).subPanel(true).build();

    /**
     * @return List of all the flags in this class
     */
    public static List<Flag> values() {
        return Arrays.stream(Flags.class.getFields()).map(field -> {
            try {
                return (Flag)field.get(null);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Bukkit.getLogger().severe("Could not get Flag values " + e.getMessage());
            }
            return null;
        }).collect(Collectors.toList());
    }
}
