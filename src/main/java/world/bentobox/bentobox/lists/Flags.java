package world.bentobox.bentobox.lists;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.listeners.flags.worldsettings.ObsidianScoopingListener;
import world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener;
import world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener;
import world.bentobox.bentobox.listeners.flags.protection.BreedingListener;
import world.bentobox.bentobox.listeners.flags.protection.BucketListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.ChestDamageListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.CleanSuperFlatListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener;
import world.bentobox.bentobox.listeners.flags.protection.EggListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.EnderChestListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.EndermanListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.EnterExitListener;
import world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener;
import world.bentobox.bentobox.listeners.flags.protection.ExperiencePickupListener;
import world.bentobox.bentobox.listeners.flags.protection.FireListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.GeoLimitMobsListener;
import world.bentobox.bentobox.listeners.flags.protection.HurtingListener;
import world.bentobox.bentobox.listeners.flags.protection.InventoryListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.InvincibleVisitorsListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.IslandRespawnListener;
import world.bentobox.bentobox.listeners.flags.protection.ItemDropPickUpListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.ItemFrameListener;
import world.bentobox.bentobox.listeners.flags.protection.LeashListener;
import world.bentobox.bentobox.listeners.flags.protection.LockAndBanListener;
import world.bentobox.bentobox.listeners.flags.settings.MobSpawnListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.OfflineRedstoneListener;
import world.bentobox.bentobox.listeners.flags.settings.PVPListener;
import world.bentobox.bentobox.listeners.flags.protection.PhysicalInteractionListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.PistonPushListener;
import world.bentobox.bentobox.listeners.flags.protection.PlaceBlocksListener;
import world.bentobox.bentobox.listeners.flags.protection.PortalListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.RemoveMobsListener;
import world.bentobox.bentobox.listeners.flags.protection.ShearingListener;
import world.bentobox.bentobox.listeners.flags.protection.TNTListener;
import world.bentobox.bentobox.listeners.flags.protection.TeleportationListener;
import world.bentobox.bentobox.listeners.flags.protection.ThrowingListener;
import world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener;
import world.bentobox.bentobox.listeners.flags.clicklisteners.GeoLimitClickListener;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * Contains built-in {@link Flag Flags} that are registered by default into the {@link world.bentobox.bentobox.managers.FlagsManager FlagsManager} at startup.
 */
public final class Flags {

    private Flags() {}

    // Disabled setting 'rank'
    private static final int DISABLED = -1;

    /**
     * Prevents players from breaking blocks on one's island.
     * @see BreakBlocksListener
     */
    public static final Flag BREAK_BLOCKS = new Flag.Builder("BREAK_BLOCKS", Material.STONE).listener(new BreakBlocksListener()).build();
    /**
     * Prevents players from placing blocks on one's island.
     * @see PlaceBlocksListener
     */
    public static final Flag PLACE_BLOCKS = new Flag.Builder("PLACE_BLOCKS", Material.GRASS).listener(new PlaceBlocksListener()).build();

    /**
     * Prevents players from generated Frosted Ice on one's island using boots enchanted with "Frost Walker".
     * @see PlaceBlocksListener
     */
    public static final Flag FROST_WALKER = new Flag.Builder("FROST_WALKER", Material.ICE).build();

    // Block interactions - all use BlockInteractionListener()
    public static final Flag ANVIL = new Flag.Builder("ANVIL", Material.ANVIL).listener(new BlockInteractionListener()).build();
    public static final Flag BEACON = new Flag.Builder("BEACON", Material.BEACON).build();
    public static final Flag BED = new Flag.Builder("BED", Material.RED_BED).build();
    public static final Flag BREWING = new Flag.Builder("BREWING", Material.BREWING_STAND).build();
    public static final Flag CONTAINER = new Flag.Builder("CONTAINER", Material.CHEST).build();
    public static final Flag DISPENSER = new Flag.Builder("DISPENSER", Material.DISPENSER).build();
    public static final Flag DROPPER = new Flag.Builder("DROPPER", Material.DROPPER).build();
    public static final Flag HOPPER = new Flag.Builder("HOPPER", Material.HOPPER).build();
    public static final Flag DOOR = new Flag.Builder("DOOR", Material.OAK_DOOR).defaultSetting(true).build();
    public static final Flag TRAPDOOR = new Flag.Builder("TRAPDOOR", Material.OAK_TRAPDOOR).defaultSetting(true).build();
    public static final Flag CRAFTING = new Flag.Builder("CRAFTING", Material.CRAFTING_TABLE).defaultSetting(true).build();
    public static final Flag ENCHANTING = new Flag.Builder("ENCHANTING", Material.ENCHANTING_TABLE).defaultSetting(true).build();
    public static final Flag FURNACE = new Flag.Builder("FURNACE", Material.FURNACE).build();
    public static final Flag GATE = new Flag.Builder("GATE", Material.OAK_FENCE_GATE).defaultSetting(true).build();
    public static final Flag NOTE_BLOCK = new Flag.Builder("NOTE_BLOCK", Material.NOTE_BLOCK).build();
    public static final Flag JUKEBOX = new Flag.Builder("JUKEBOX", Material.JUKEBOX).build();
    public static final Flag LEVER = new Flag.Builder("LEVER", Material.LEVER).build();
    public static final Flag BUTTON = new Flag.Builder("BUTTON", Material.OAK_BUTTON).build();
    public static final Flag REDSTONE = new Flag.Builder("REDSTONE", Material.REDSTONE).build();
    public static final Flag SPAWN_EGGS = new Flag.Builder("SPAWN_EGGS", Material.COW_SPAWN_EGG).build();
    public static final Flag ITEM_FRAME = new Flag.Builder("ITEM_FRAME", Material.ITEM_FRAME).build();

    // Entity interactions
    public static final Flag ARMOR_STAND = new Flag.Builder("ARMOR_STAND", Material.ARMOR_STAND).listener(new EntityInteractListener()).build();
    public static final Flag RIDING = new Flag.Builder("RIDING", Material.GOLDEN_HORSE_ARMOR).build();
    public static final Flag TRADING = new Flag.Builder("TRADING", Material.EMERALD).defaultSetting(true).build();
    public static final Flag NAME_TAG = new Flag.Builder("NAME_TAG", Material.NAME_TAG).build();

    // Breeding
    public static final Flag BREEDING = new Flag.Builder("BREEDING", Material.CARROT).listener(new BreedingListener()).build();

    // Buckets. All bucket use is covered by one listener
    public static final Flag BUCKET = new Flag.Builder("BUCKET", Material.BUCKET).listener(new BucketListener()).build();
    public static final Flag COLLECT_LAVA = new Flag.Builder("COLLECT_LAVA", Material.LAVA_BUCKET).build();
    public static final Flag COLLECT_WATER = new Flag.Builder("COLLECT_WATER", Material.WATER_BUCKET).build();
    public static final Flag MILKING = new Flag.Builder("MILKING", Material.MILK_BUCKET).build();
    public static final Flag FISH_SCOOPING = new Flag.Builder("FISH_SCOOPING", Material.TROPICAL_FISH_BUCKET).build();

    // Chorus Fruit and Enderpearls
    public static final Flag CHORUS_FRUIT = new Flag.Builder("CHORUS_FRUIT", Material.CHORUS_FRUIT).listener(new TeleportationListener()).build();
    public static final Flag ENDER_PEARL = new Flag.Builder("ENDER_PEARL", Material.ENDER_PEARL).build();

    // Physical interactions
    public static final Flag CROP_TRAMPLE = new Flag.Builder("CROP_TRAMPLE", Material.WHEAT).listener(new PhysicalInteractionListener()).build();
    public static final Flag PRESSURE_PLATE = new Flag.Builder("PRESSURE_PLATE", Material.STONE_PRESSURE_PLATE).build();
    public static final Flag TURTLE_EGGS = new Flag.Builder("TURTLE_EGGS", Material.TURTLE_EGG).build();

    // Throwing things
    public static final Flag EGGS = new Flag.Builder("EGGS", Material.EGG).listener(new EggListener()).build();
    /**
     * Prevents players from throwing potions / exp bottles.
     * @since 1.1
     */
    public static final Flag POTION_THROWING = new Flag.Builder("POTION_THROWING", Material.SPLASH_POTION).listener(new ThrowingListener()).build();

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
    public static final Flag FIRE = new Flag.Builder("FIRE", Material.FLINT_AND_STEEL).listener(new FireListener()).build();
    public static final Flag FIRE_EXTINGUISH = new Flag.Builder("FIRE_EXTINGUISH", Material.POTION).build();

    // Inventories
    public static final Flag MOUNT_INVENTORY = new Flag.Builder("MOUNT_INVENTORY", Material.IRON_HORSE_ARMOR).listener(new InventoryListener()).build();

    // Hurting things
    public static final Flag HURT_ANIMALS = new Flag.Builder("HURT_ANIMALS", Material.STONE_SWORD).listener(new HurtingListener()).build();
    public static final Flag HURT_MONSTERS = new Flag.Builder("HURT_MONSTERS", Material.WOODEN_SWORD).build();
    public static final Flag HURT_VILLAGERS = new Flag.Builder("HURT_VILLAGERS", Material.GOLDEN_SWORD).build();

    // Leashes
    public static final Flag LEASH = new Flag.Builder("LEASH", Material.LEAD).listener(new LeashListener()).build();

    // Portal use protection
    /**
     * Prevents players from going through the Nether Portal.
     * @see PortalListener
     */
    public static final Flag NETHER_PORTAL = new Flag.Builder("NETHER_PORTAL", Material.NETHERRACK).listener(new PortalListener()).build();
    /**
     * Prevents players from going through the End Portal.
     * @see PortalListener
     */
    public static final Flag END_PORTAL = new Flag.Builder("END_PORTAL", Material.END_PORTAL_FRAME).build();

    // Shearing
    public static final Flag SHEARING = new Flag.Builder("SHEARING", Material.SHEARS).listener(new ShearingListener()).build();

    // Item pickup or drop
    public static final Flag ITEM_DROP = new Flag.Builder("ITEM_DROP", Material.BEETROOT_SOUP).defaultSetting(true).listener(new ItemDropPickUpListener()).build();
    public static final Flag ITEM_PICKUP = new Flag.Builder("ITEM_PICKUP", Material.BEETROOT_SEEDS).build();

    // Experience
    public static final Flag EXPERIENCE_PICKUP = new Flag.Builder("EXPERIENCE_PICKUP", Material.EXPERIENCE_BOTTLE).listener(new ExperiencePickupListener()).build();

    // TNT
    public static final Flag TNT = new Flag.Builder("TNT", Material.TNT).listener(new TNTListener()).build();

    // Island lock
    public static final Flag LOCK = new Flag.Builder("LOCK", Material.TRIPWIRE_HOOK).defaultSetting(true)
            .defaultRank(RanksManager.VISITOR_RANK).listener(new LockAndBanListener())
            .clickHandler(new CycleClick("LOCK", RanksManager.VISITOR_RANK, RanksManager.MEMBER_RANK))
            .build();

    /*
     * Settings flags (not protection flags)
     */
    // PVP
    public static final Flag PVP_OVERWORLD = new Flag.Builder("PVP_OVERWORLD", Material.ARROW).type(Type.SETTING)
            .defaultRank(DISABLED).listener(new PVPListener()).build();
    public static final Flag PVP_NETHER = new Flag.Builder("PVP_NETHER", Material.IRON_AXE).type(Type.SETTING)
            .defaultRank(DISABLED).build();
    public static final Flag PVP_END = new Flag.Builder("PVP_END", Material.END_CRYSTAL).type(Type.SETTING)
            .defaultRank(DISABLED).build();

    // Others
    public static final Flag ANIMAL_SPAWN = new Flag.Builder("ANIMAL_SPAWN", Material.APPLE).defaultSetting(true).type(Type.SETTING)
            .listener(new MobSpawnListener()).build();
    public static final Flag MONSTER_SPAWN = new Flag.Builder("MONSTER_SPAWN", Material.SPAWNER).defaultSetting(true).type(Type.SETTING).build();

    public static final Flag FIRE_SPREAD = new Flag.Builder("FIRE_SPREAD", Material.FIREWORK_STAR).defaultSetting(true).type(Type.SETTING).build();

    /*
     * World Settings - they apply to every island in the game worlds.
     */

    public static final Flag ENDER_CHEST = new Flag.Builder("ENDER_CHEST", Material.ENDER_CHEST)
            .type(Type.WORLD_SETTING)
            .listener(new EnderChestListener())
            .build();

    public static final Flag ENDERMAN_GRIEFING = new Flag.Builder("ENDERMAN_GRIEFING", Material.END_STONE_BRICKS)
            .defaultSetting(true).type(Type.WORLD_SETTING)
            .listener(new EndermanListener())
            .build();

    public static final Flag ENTER_EXIT_MESSAGES = new Flag.Builder("ENTER_EXIT_MESSAGES", Material.DIRT).defaultSetting(true).type(Type.WORLD_SETTING)
            .listener(new EnterExitListener())
            .build();

    public static final Flag PISTON_PUSH = new Flag.Builder("PISTON_PUSH", Material.PISTON).defaultSetting(true).type(Type.WORLD_SETTING)
            .listener(new PistonPushListener())
            .build();

    private static InvincibleVisitorsListener ilv = new InvincibleVisitorsListener();
    public static final Flag INVINCIBLE_VISITORS = new Flag.Builder("INVINCIBLE_VISITORS", Material.DIAMOND_CHESTPLATE).type(Type.WORLD_SETTING)
            .listener(ilv).clickHandler(ilv).usePanel(true).build();

    public static final Flag GEO_LIMIT_MOBS = new Flag.Builder("GEO_LIMIT_MOBS", Material.CHAINMAIL_CHESTPLATE).type(Type.WORLD_SETTING)
            .listener(new GeoLimitMobsListener()).clickHandler(new GeoLimitClickListener()).usePanel(true).build();

    public static final Flag REMOVE_MOBS = new Flag.Builder("REMOVE_MOBS", Material.GLOWSTONE_DUST).type(Type.WORLD_SETTING)
            .listener(new RemoveMobsListener()).defaultSetting(true).build();

    public static final Flag ITEM_FRAME_DAMAGE = new Flag.Builder("ITEM_FRAME_DAMAGE", Material.ITEM_FRAME).type(Type.WORLD_SETTING)
            .listener(new ItemFrameListener()).build();

    public static final Flag ISLAND_RESPAWN = new Flag.Builder("ISLAND_RESPAWN", Material.TORCH).type(Type.WORLD_SETTING)
            .listener(new IslandRespawnListener()).defaultSetting(true).build();

    public static final Flag OFFLINE_REDSTONE = new Flag.Builder("OFFLINE_REDSTONE", Material.COMPARATOR).type(Type.WORLD_SETTING)
            .listener(new OfflineRedstoneListener()).defaultSetting(true).build();

    public static final Flag CLEAN_SUPER_FLAT = new Flag.Builder("CLEAN_SUPER_FLAT", Material.BEDROCK).type(Type.WORLD_SETTING)
            .listener(new CleanSuperFlatListener()).build();

    public static final Flag CHEST_DAMAGE = new Flag.Builder("CHEST_DAMAGE", Material.TRAPPED_CHEST).type(Type.WORLD_SETTING)
            .listener(new ChestDamageListener()).build();
    public static final Flag CREEPER_DAMAGE = new Flag.Builder("CREEPER_DAMAGE", Material.GREEN_SHULKER_BOX).listener(new CreeperListener()).type(Type.WORLD_SETTING)
            .defaultSetting(true).build();
    /**
     * Prevents visitors from triggering a creeper to blow up an island.
     * @see CreeperListener
     */
    public static final Flag CREEPER_GRIEFING = new Flag.Builder("CREEPER_GRIEFING", Material.CREEPER_HEAD).type(Type.WORLD_SETTING).build();

    public static final Flag COMMAND_RANKS = new Flag.Builder("COMMAND_RANKS", Material.PLAYER_HEAD).type(Type.WORLD_SETTING)
            .clickHandler(new CommandRankClickListener()).usePanel(true).build();

    public static final Flag COARSE_DIRT_TILLING = new Flag.Builder("COARSE_DIRT_TILLING", Material.COARSE_DIRT).type(Type.WORLD_SETTING).defaultSetting(true).listener(new CoarseDirtTillingListener()).build();

    public static final Flag PREVENT_TELEPORT_WHEN_FALLING = new Flag.Builder("PREVENT_TELEPORT_WHEN_FALLING", Material.FEATHER).type(Type.WORLD_SETTING).build();

    public static final Flag OBSIDIAN_SCOOPING = new Flag.Builder("OBSIDIAN_SCOOPING", Material.OBSIDIAN).type(Type.WORLD_SETTING)
            .listener(new ObsidianScoopingListener()).defaultSetting(true).build();

    /**
     * Provides a list of all the Flag instances contained in this class using reflection.
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
