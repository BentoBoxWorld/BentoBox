package world.bentobox.bentobox.lists;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import com.google.common.base.Enums;

import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Mode;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener;
import world.bentobox.bentobox.listeners.flags.clicklisteners.GeoLimitClickListener;
import world.bentobox.bentobox.listeners.flags.clicklisteners.MobLimitClickListener;
import world.bentobox.bentobox.listeners.flags.protection.BlockInteractionListener;
import world.bentobox.bentobox.listeners.flags.protection.BreakBlocksListener;
import world.bentobox.bentobox.listeners.flags.protection.BreedingListener;
import world.bentobox.bentobox.listeners.flags.protection.BucketListener;
import world.bentobox.bentobox.listeners.flags.protection.CandleListener;
import world.bentobox.bentobox.listeners.flags.protection.DyeListener;
import world.bentobox.bentobox.listeners.flags.protection.EggListener;
import world.bentobox.bentobox.listeners.flags.protection.ElytraListener;
import world.bentobox.bentobox.listeners.flags.protection.EntityInteractListener;
import world.bentobox.bentobox.listeners.flags.protection.ExperiencePickupListener;
import world.bentobox.bentobox.listeners.flags.protection.ExplosionListener;
import world.bentobox.bentobox.listeners.flags.protection.FireListener;
import world.bentobox.bentobox.listeners.flags.protection.HurtingListener;
import world.bentobox.bentobox.listeners.flags.protection.InventoryListener;
import world.bentobox.bentobox.listeners.flags.protection.ItemDropPickUpListener;
import world.bentobox.bentobox.listeners.flags.protection.LeashListener;
import world.bentobox.bentobox.listeners.flags.protection.LecternListener;
import world.bentobox.bentobox.listeners.flags.protection.LockAndBanListener;
import world.bentobox.bentobox.listeners.flags.protection.PaperExperiencePickupListener;
import world.bentobox.bentobox.listeners.flags.protection.PhysicalInteractionListener;
import world.bentobox.bentobox.listeners.flags.protection.PlaceBlocksListener;
import world.bentobox.bentobox.listeners.flags.protection.PortalListener;
import world.bentobox.bentobox.listeners.flags.protection.SculkSensorListener;
import world.bentobox.bentobox.listeners.flags.protection.SculkShriekerListener;
import world.bentobox.bentobox.listeners.flags.protection.ShearingListener;
import world.bentobox.bentobox.listeners.flags.protection.TeleportationListener;
import world.bentobox.bentobox.listeners.flags.protection.ThrowingListener;
import world.bentobox.bentobox.listeners.flags.settings.DecayListener;
import world.bentobox.bentobox.listeners.flags.settings.MobSpawnListener;
import world.bentobox.bentobox.listeners.flags.settings.MobTeleportListener;
import world.bentobox.bentobox.listeners.flags.settings.PVPListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.ChestDamageListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.CleanSuperFlatListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.CoarseDirtTillingListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.EnderChestListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.EndermanListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.EnterExitListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.GeoLimitMobsListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.InvincibleVisitorsListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.IslandRespawnListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.ItemFrameListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.LimitMobsListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.LiquidsFlowingOutListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.NaturalSpawningOutsideRangeListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.ObsidianScoopingListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.OfflineGrowthListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.OfflineRedstoneListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.PistonPushListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.PodzolListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.RemoveMobsListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.SpawnerSpawnEggsListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.TreesGrowingOutsideRangeListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.VisitorKeepInventoryListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.VisitorsStartingRaidListener;
import world.bentobox.bentobox.listeners.flags.worldsettings.WitherListener;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

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
    public static final Flag BREAK_BLOCKS = new Flag.Builder("BREAK_BLOCKS", Material.STONE_PICKAXE).listener(new BreakBlocksListener()).mode(Flag.Mode.BASIC).build();
    /**
     * Prevents players from breaking spawners on one's island.
     * @see BreakBlocksListener
     * @since 1.13.0
     */
    public static final Flag BREAK_SPAWNERS = new Flag.Builder("BREAK_SPAWNERS", Material.SPAWNER).mode(Flag.Mode.EXPERT).build();
    /**
     * Prevents players from breaking hoppers on one's island.
     * @see BreakBlocksListener
     * @since 1.14.0
     */
    public static final Flag BREAK_HOPPERS = new Flag.Builder("BREAK_HOPPERS", Material.HOPPER).mode(Flag.Mode.EXPERT).build();
    /**
     * Prevents players from placing blocks on one's island.
     * @see PlaceBlocksListener
     */
    public static final Flag PLACE_BLOCKS = new Flag.Builder("PLACE_BLOCKS", Material.OAK_PLANKS).listener(new PlaceBlocksListener()).mode(Flag.Mode.BASIC).build();

    /**
     * Prevents players from generating Frosted Ice on one's island using "Frost Walker" enchanted boots.
     * @see PlaceBlocksListener
     */
    public static final Flag FROST_WALKER = new Flag.Builder("FROST_WALKER", Material.ICE).build();

    // Block interactions - all use BlockInteractionListener()
    public static final Flag ANVIL = new Flag.Builder("ANVIL", Material.ANVIL).listener(new BlockInteractionListener()).mode(Flag.Mode.BASIC).build();
    public static final Flag BEACON = new Flag.Builder("BEACON", Material.BEACON).build();
    public static final Flag BED = new Flag.Builder("BED", Material.RED_BED).build();
    public static final Flag BREWING = new Flag.Builder("BREWING", Material.BREWING_STAND).mode(Flag.Mode.ADVANCED).build();
    // START CONTAINER split
    public static final Flag CHEST = new Flag.Builder("CHEST", Material.CHEST).mode(Flag.Mode.ADVANCED).build();
    public static final Flag BARREL = new Flag.Builder("BARREL", Material.BARREL).mode(Flag.Mode.ADVANCED).build();
    public static final Flag COMPOSTER = new Flag.Builder("COMPOSTER", Material.COMPOSTER).mode(Flag.Mode.ADVANCED).build();
    public static final Flag FLOWER_POT = new Flag.Builder("FLOWER_POT", Material.FLOWER_POT).mode(Flag.Mode.ADVANCED).build();
    public static final Flag SHULKER_BOX = new Flag.Builder("SHULKER_BOX", Material.SHULKER_BOX).mode(Flag.Mode.ADVANCED).build();
    public static final Flag TRAPPED_CHEST = new Flag.Builder("TRAPPED_CHEST", Material.TRAPPED_CHEST).mode(Flag.Mode.ADVANCED).build();
    public static final Flag CRAFTER = new Flag.Builder("CRAFTER", Material.CRAFTER).mode(Flag.Mode.ADVANCED).build();
    // END CONTAINER split
    public static final Flag DISPENSER = new Flag.Builder("DISPENSER", Material.DISPENSER).mode(Flag.Mode.ADVANCED).build();
    public static final Flag DROPPER = new Flag.Builder("DROPPER", Material.DROPPER).mode(Flag.Mode.ADVANCED).build();
    public static final Flag HOPPER = new Flag.Builder("HOPPER", Material.HOPPER).mode(Flag.Mode.ADVANCED).build();
    public static final Flag DOOR = new Flag.Builder("DOOR", Material.OAK_DOOR).defaultSetting(true).mode(Flag.Mode.BASIC).build();
    public static final Flag TRAPDOOR = new Flag.Builder("TRAPDOOR", Material.OAK_TRAPDOOR).defaultSetting(true).mode(Flag.Mode.BASIC).build();
    public static final Flag CRAFTING = new Flag.Builder("CRAFTING", Material.CRAFTING_TABLE).defaultSetting(true).build();
    public static final Flag ENCHANTING = new Flag.Builder("ENCHANTING", Material.ENCHANTING_TABLE).defaultSetting(true).mode(Flag.Mode.BASIC).build();
    public static final Flag FURNACE = new Flag.Builder("FURNACE", Material.FURNACE).mode(Flag.Mode.BASIC).build();
    public static final Flag GATE = new Flag.Builder("GATE", Material.OAK_FENCE_GATE).mode(Flag.Mode.BASIC).defaultSetting(true).build();
    public static final Flag NOTE_BLOCK = new Flag.Builder("NOTE_BLOCK", Material.NOTE_BLOCK).build();
    public static final Flag JUKEBOX = new Flag.Builder("JUKEBOX", Material.JUKEBOX).build();
    public static final Flag LEVER = new Flag.Builder("LEVER", Material.LEVER).mode(Flag.Mode.ADVANCED).build();
    public static final Flag BUTTON = new Flag.Builder("BUTTON", Material.OAK_BUTTON).mode(Flag.Mode.ADVANCED).build();
    public static final Flag REDSTONE = new Flag.Builder("REDSTONE", Material.REDSTONE).mode(Flag.Mode.ADVANCED).build();
    public static final Flag SPAWN_EGGS = new Flag.Builder("SPAWN_EGGS", Material.COW_SPAWN_EGG).build();
    public static final Flag ITEM_FRAME = new Flag.Builder("ITEM_FRAME", Material.ITEM_FRAME).mode(Flag.Mode.ADVANCED).build();
    public static final Flag CAKE = new Flag.Builder("CAKE", Material.CAKE).build();
    public static final Flag HIVE = new Flag.Builder("HIVE", Material.HONEY_BOTTLE).type(Type.PROTECTION).build();
    public static final Flag CARTOGRAPHY = new Flag.Builder("CARTOGRAPHY", Material.CARTOGRAPHY_TABLE).build();
    public static final Flag GRINDSTONE = new Flag.Builder("GRINDSTONE", Material.GRINDSTONE).build();
    public static final Flag SMITHING = new Flag.Builder("SMITHING", Material.SMITHING_TABLE).build();
    public static final Flag STONECUTTING = new Flag.Builder("STONECUTTING", Material.STONECUTTER).build();
    public static final Flag LOOM = new Flag.Builder("LOOM", Material.LOOM).build();

    public static final Flag CONTAINER = new Flag.Builder("CONTAINER", Material.CHEST).mode(Flag.Mode.BASIC)
            .subflags(BREWING, BARREL, CHEST, COMPOSTER, FLOWER_POT, SHULKER_BOX, TRAPPED_CHEST, FURNACE, JUKEBOX, DISPENSER,
                    DROPPER, HOPPER, ITEM_FRAME, HIVE)
            .build();

    /**
     * Prevents players from interacting with the Dragon Egg.
     * @since 1.3.1
     * @see BlockInteractionListener
     * @see BreakBlocksListener
     */
    public static final Flag DRAGON_EGG = new Flag.Builder("DRAGON_EGG", Material.DRAGON_EGG).build();
    /**
     * Prevents players from placing a book on a lectern or taking the book from it.
     * @since 1.10.0
     * @see LecternListener
     */
    public static final Flag LECTERN = new Flag.Builder("LECTERN", Material.LECTERN).mode(Mode.ADVANCED).listener(new LecternListener()).build();

    /**
     * Prevents players from placing a book in a bookshelf or taking the book from it.
     * @since 1.24.0
     * @see BlockInteractionListener
     */
    public static final Flag BOOKSHELF = new Flag.Builder("BOOKSHELF", Material.CHISELED_BOOKSHELF).mode(Mode.ADVANCED).build();

    // Entity interactions
    public static final Flag ARMOR_STAND = new Flag.Builder("ARMOR_STAND", Material.ARMOR_STAND).listener(new EntityInteractListener()).mode(Mode.ADVANCED).build();
    public static final Flag RIDING = new Flag.Builder("RIDING", Material.GOLDEN_HORSE_ARMOR).mode(Mode.ADVANCED).build();
    /**
     * Prevents players from issuing any kind of interactions with Minecarts (entering, placing and opening if chest).
     * @since 1.3.0
     * @see EntityInteractListener
     * @see PlaceBlocksListener
     */
    public static final Flag MINECART = new Flag.Builder("MINECART", Material.MINECART).mode(Flag.Mode.ADVANCED).build();
    /**
     * Prevents players from issuing any kind of interactions with Boats (entering, placing).
     * @since 1.3.0
     * @see EntityInteractListener
     * @see PlaceBlocksListener
     */
    public static final Flag BOAT = new Flag.Builder("BOAT", Material.OAK_BOAT).mode(Flag.Mode.BASIC).build();
    public static final Flag TRADING = new Flag.Builder("TRADING", Material.EMERALD).defaultSetting(true).mode(Flag.Mode.BASIC).build();
    public static final Flag NAME_TAG = new Flag.Builder("NAME_TAG", Material.NAME_TAG).mode(Flag.Mode.ADVANCED).build();
    /**
     * @since 1.21
     */
    public static final Flag ALLAY = new Flag.Builder("ALLAY", Material.AMETHYST_SHARD).mode(Flag.Mode.ADVANCED).build();
    // Breeding
    public static final Flag BREEDING = new Flag.Builder("BREEDING", Material.CARROT).listener(new BreedingListener()).mode(Flag.Mode.ADVANCED).build();

    // Buckets. All bucket use is covered by one listener
    public static final Flag BUCKET = new Flag.Builder("BUCKET", Material.BUCKET).listener(new BucketListener()).mode(Flag.Mode.BASIC).build();
    public static final Flag COLLECT_LAVA = new Flag.Builder("COLLECT_LAVA", Material.LAVA_BUCKET).mode(Mode.ADVANCED).build();
    public static final Flag COLLECT_WATER = new Flag.Builder("COLLECT_WATER", Material.WATER_BUCKET).mode(Flag.Mode.ADVANCED).build();
    /**
     * @since 1.21
     */
    public static final Flag COLLECT_POWDERED_SNOW = new Flag.Builder("COLLECT_POWDERED_SNOW", Material.POWDER_SNOW_BUCKET).mode(Flag.Mode.ADVANCED).build();
    public static final Flag MILKING = new Flag.Builder("MILKING", Material.MILK_BUCKET).mode(Flag.Mode.ADVANCED).build();
    public static final Flag FISH_SCOOPING = new Flag.Builder("FISH_SCOOPING", Material.TROPICAL_FISH_BUCKET).build();
    /**
     * @since 1.21
     */
    public static final Flag AXOLOTL_SCOOPING = new Flag.Builder("AXOLOTL_SCOOPING", Material.AXOLOTL_BUCKET).build();

    // Chorus Fruit and Enderpearls
    public static final Flag CHORUS_FRUIT = new Flag.Builder("CHORUS_FRUIT", Material.CHORUS_FRUIT).listener(new TeleportationListener()).build();
    public static final Flag ENDER_PEARL = new Flag.Builder("ENDER_PEARL", Material.ENDER_PEARL).build();

    // Physical interactions
    public static final Flag CROP_TRAMPLE = new Flag.Builder("CROP_TRAMPLE", Material.WHEAT).listener(new PhysicalInteractionListener()).build();
    public static final Flag PRESSURE_PLATE = new Flag.Builder("PRESSURE_PLATE", Material.STONE_PRESSURE_PLATE).mode(Flag.Mode.ADVANCED).build();
    public static final Flag TURTLE_EGGS = new Flag.Builder("TURTLE_EGGS", Material.TURTLE_EGG).build();

    // Throwing things
    /**
     * Prevents players from throwing eggs.
     * @see EggListener
     */
    public static final Flag EGGS = new Flag.Builder("EGGS", Material.EGG).mode(Mode.ADVANCED).listener(new EggListener()).build();
    /**
     * Prevents players from throwing potions / experience bottles.
     * @since 1.1
     * @see ThrowingListener
     */
    public static final Flag POTION_THROWING = new Flag.Builder("POTION_THROWING", Material.SPLASH_POTION).listener(new ThrowingListener()).build();
    /**
     * Prevents players from throwing experience bottles.
     * @since 1.3.1
     * @see ThrowingListener
     */
    public static final Flag EXPERIENCE_BOTTLE_THROWING = new Flag.Builder("EXPERIENCE_BOTTLE_THROWING", Material.EXPERIENCE_BOTTLE).build();

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
    /**
     * Prevents players from starting fires using flint and steel or fire charges.
     * @since 1.3.0
     *
     * @see FireListener
     */
    public static final Flag FLINT_AND_STEEL = new Flag.Builder("FLINT_AND_STEEL", Material.FLINT_AND_STEEL).listener(new FireListener()).mode(Flag.Mode.ADVANCED).build();

    /**
     * Prevents players from priming TNT.
     * @since 1.5.0
     *
     * @see ExplosionListener
     */
    public static final Flag TNT_PRIMING = new Flag.Builder("TNT_PRIMING", Material.TNT).listener(new ExplosionListener()).build();

    /**
     * Prevents players from extinguishing fires.
     * @see FireListener
     */
    public static final Flag FIRE_EXTINGUISH = new Flag.Builder("FIRE_EXTINGUISH", Material.POTION).mode(Mode.ADVANCED).build();

    // Inventories
    public static final Flag MOUNT_INVENTORY = new Flag.Builder("MOUNT_INVENTORY", Material.IRON_HORSE_ARMOR).listener(new InventoryListener()).mode(Flag.Mode.ADVANCED).build();

    // Hurting things
    public static final Flag HURT_ANIMALS = new Flag.Builder("HURT_ANIMALS", Material.STONE_SWORD).listener(new HurtingListener()).mode(Flag.Mode.ADVANCED).build();
    public static final Flag HURT_MONSTERS = new Flag.Builder("HURT_MONSTERS", Material.WOODEN_SWORD).mode(Flag.Mode.BASIC).build();
    public static final Flag HURT_VILLAGERS = new Flag.Builder("HURT_VILLAGERS", Material.GOLDEN_SWORD).mode(Flag.Mode.ADVANCED).build();
    public static final Flag HURT_TAMED_ANIMALS = new Flag.Builder("HURT_TAMED_ANIMALS", Material.DIAMOND_SWORD).mode(Flag.Mode.ADVANCED).build();

    // Leashes
    public static final Flag LEASH = new Flag.Builder("LEASH", Material.LEAD).listener(new LeashListener()).build();

    // Portal use protection
    /**
     * Prevents players from going through the Nether Portal.
     * @see PortalListener
     */
    public static final Flag NETHER_PORTAL = new Flag.Builder("NETHER_PORTAL", Material.NETHERRACK).mode(Mode.ADVANCED).listener(new PortalListener()).build();
    /**
     * Prevents players from going through the End Portal.
     * @see PortalListener
     */
    public static final Flag END_PORTAL = new Flag.Builder("END_PORTAL", Material.END_PORTAL_FRAME).mode(Mode.ADVANCED).build();

    // Shearing
    public static final Flag SHEARING = new Flag.Builder("SHEARING", Material.SHEARS).listener(new ShearingListener()).mode(Flag.Mode.ADVANCED).build();

    // Item pickup or drop
    public static final Flag ITEM_DROP = new Flag.Builder("ITEM_DROP", Material.DIAMOND).defaultRank(RanksManager.VISITOR_RANK).listener(new ItemDropPickUpListener()).mode(Flag.Mode.BASIC).build();
    public static final Flag ITEM_PICKUP = new Flag.Builder("ITEM_PICKUP", Material.SUGAR_CANE).mode(Flag.Mode.BASIC).defaultRank(RanksManager.VISITOR_RANK).build();

    // Experience
    public static final Flag EXPERIENCE_PICKUP = new Flag.Builder("EXPERIENCE_PICKUP", Material.EXPERIENCE_BOTTLE)
            .listener(Util.isPaper() ? new PaperExperiencePickupListener() : new ExperiencePickupListener()).mode(Flag.Mode.ADVANCED).defaultRank(RanksManager.VISITOR_RANK).build();

    // Command ranks
    public static final Flag COMMAND_RANKS = new Flag.Builder("COMMAND_RANKS", Material.PLAYER_HEAD).type(Type.SETTING)
            .clickHandler(new CommandRankClickListener()).usePanel(true).build();

    /**
     * Protects against visitors dying stuff, like sheep or signs
     *
     * @since 1.5.0
     * @see DyeListener
     */
    public static final Flag DYE = new Flag.Builder("DYE", Material.LIGHT_BLUE_DYE).listener(new DyeListener()).mode(Flag.Mode.ADVANCED).build();

    /**
     * Protects against visitors using elytra. By default, it is allowed.
     *
     * @since 1.6.0
     */
    public static final Flag ELYTRA = new Flag.Builder("ELYTRA",  Material.ELYTRA).defaultRank(RanksManager.VISITOR_RANK).listener(new ElytraListener()).mode(Flag.Mode.ADVANCED).build();

    // Island lock
    public static final Flag LOCK = new Flag.Builder("LOCK", Material.TRIPWIRE_HOOK).defaultSetting(true)
            .defaultRank(RanksManager.VISITOR_RANK).listener(new LockAndBanListener())
            .clickHandler(new CycleClick("LOCK", RanksManager.VISITOR_RANK, RanksManager.MEMBER_RANK))
            .mode(Flag.Mode.TOP_ROW).build();

    /**
     * This flag allows choosing which island members can change island settings values.
     *
     * @since 1.20.0
     */
    public static final Flag CHANGE_SETTINGS = new Flag.Builder("CHANGE_SETTINGS", Material.CRAFTING_TABLE).defaultSetting(true)
            .defaultRank(RanksManager.OWNER_RANK)
            .clickHandler(new CycleClick("CHANGE_SETTINGS", RanksManager.MEMBER_RANK, RanksManager.OWNER_RANK))
            .mode(Flag.Mode.TOP_ROW).build();

    private static final String SCULKSENSOR = "SCULK_SENSOR";

    /**
     * This flag allows choosing which island member group can activate sculk sensors.
     * TODO: Enums#getIfPresent is used to support 1.18
     * @since 1.21.0
     */
    public static final Flag SCULK_SENSOR = new Flag.Builder(SCULKSENSOR, Enums.getIfPresent(Material.class, SCULKSENSOR).or(Material.BARRIER)).
            listener(new SculkSensorListener()).
            type(Type.PROTECTION).
            defaultSetting(true).
            defaultRank(RanksManager.MEMBER_RANK).
            clickHandler(new CycleClick(SCULKSENSOR, RanksManager.VISITOR_RANK, RanksManager.MEMBER_RANK)).
            build();

    private static final String SCULKSHRIEKER = "SCULK_SHRIEKER";

    /**
     * This flag allows choosing which island member group can activate sculk shrieker.
     * TODO: Enums#getIfPresent is used to support 1.18
     * @since 1.21.0
     */
    public static final Flag SCULK_SHRIEKER = new Flag.Builder(SCULKSHRIEKER, Enums.getIfPresent(Material.class, SCULKSHRIEKER).or(Material.BARRIER)).
            listener(new SculkShriekerListener()).
            type(Type.PROTECTION).
            defaultSetting(true).
            defaultRank(RanksManager.MEMBER_RANK).
            clickHandler(new CycleClick(SCULKSHRIEKER, RanksManager.VISITOR_RANK, RanksManager.MEMBER_RANK)).
            build();

    /*
     * Settings flags (not protection flags)
     */
    // PVP
    public static final Flag PVP_OVERWORLD = new Flag.Builder("PVP_OVERWORLD", Material.ARROW).type(Type.SETTING)
            .defaultRank(DISABLED).listener(new PVPListener()).cooldown(60).mode(Flag.Mode.BASIC).build();
    public static final Flag PVP_NETHER = new Flag.Builder("PVP_NETHER", Material.IRON_AXE).type(Type.SETTING)
            .defaultRank(DISABLED).cooldown(60).mode(Flag.Mode.BASIC).build();
    public static final Flag PVP_END = new Flag.Builder("PVP_END", Material.END_CRYSTAL).type(Type.SETTING)
            .defaultRank(DISABLED).cooldown(60).mode(Flag.Mode.BASIC).build();

    // Fire
    /**
     * Prevents fire from burning blocks.
     * @since 1.3.0
     * @see FireListener
     */
    public static final Flag FIRE_BURNING = new Flag.Builder("FIRE_BURNING", Material.CHARCOAL).defaultSetting(true).type(Type.SETTING)
            .mode(Flag.Mode.ADVANCED).build();
    /**
     * Prevents fire from being ignited by non-players.
     * @since 1.3.0
     * @see FireListener
     */
    public static final Flag FIRE_IGNITE = new Flag.Builder("FIRE_IGNITE", Material.FLINT_AND_STEEL).defaultSetting(true)
            .mode(Flag.Mode.ADVANCED).type(Type.SETTING).build();
    /**
     * Prevents fire from spreading to other blocks.
     * @see FireListener
     */
    public static final Flag FIRE_SPREAD = new Flag.Builder("FIRE_SPREAD", Material.FIREWORK_STAR).defaultSetting(true).type(Type.SETTING)
            .mode(Flag.Mode.ADVANCED).build();

    // Mob spawning
    /**
     * @deprecated see {@link #ANIMAL_NATURAL_SPAWN} and {@link #ANIMAL_SPAWNERS_SPAWN}.
     * @since 1.14.0
     */
    @Deprecated(since="1.14.0", forRemoval=true)
    public static final Flag ANIMAL_SPAWN = new Flag.Builder("ANIMAL_SPAWN", Material.APPLE).defaultSetting(true).type(Type.SETTING).build();
    /**
     * @deprecated see {@link #MONSTER_NATURAL_SPAWN} and {@link #MONSTER_SPAWNERS_SPAWN}.
     * @since 1.14.0
     */
    @Deprecated(since="1.14.0", forRemoval=true)
    public static final Flag MONSTER_SPAWN = new Flag.Builder("MONSTER_SPAWN", Material.SPAWNER).defaultSetting(true).type(Type.SETTING).build();

    /**
     * Toggles animal natural spawning.
     * @since 1.14.0
     * @see MobSpawnListener
     */
    public static final Flag ANIMAL_NATURAL_SPAWN = new Flag.Builder("ANIMAL_NATURAL_SPAWN", Material.APPLE).defaultSetting(true).type(Type.SETTING)
            .listener(new MobSpawnListener()).build();
    /**
     * Toggles animal spawning with spawners.
     * @since 1.14.0
     * @see MobSpawnListener
     */
    public static final Flag ANIMAL_SPAWNERS_SPAWN = new Flag.Builder("ANIMAL_SPAWNERS_SPAWN", Material.SPAWNER).defaultSetting(true).type(Type.SETTING).build();

    /**
     * Toggles monster natural spawning.
     * @since 1.14.0
     * @see MobSpawnListener
     */
    public static final Flag MONSTER_NATURAL_SPAWN = new Flag.Builder("MONSTER_NATURAL_SPAWN", Material.ZOMBIE_HEAD).defaultSetting(true).type(Type.SETTING).build();
    /**
     * Toggles monster spawning with spawners.
     * @since 1.14.0
     * @see MobSpawnListener
     */
    public static final Flag MONSTER_SPAWNERS_SPAWN = new Flag.Builder("MONSTER_SPAWNERS_SPAWN", Material.SPAWNER).defaultSetting(true).type(Type.SETTING).build();

    // Others
    /**
     * If {@code false}, prevents leaves from disappearing.
     * @since 1.3.1
     * @see DecayListener
     */
    public static final Flag LEAF_DECAY = new Flag.Builder("LEAF_DECAY", Material.OAK_LEAVES).type(Type.SETTING).listener(new DecayListener()).defaultSetting(true).build();

    /**
     * If {@code false}, prevents TNT from breaking blocks and damaging nearby entities.
     * @since 1.5.0
     * @see ExplosionListener
     */
    public static final Flag TNT_DAMAGE = new Flag.Builder("TNT_DAMAGE", Material.TNT).type(Type.SETTING)
            .mode(Flag.Mode.ADVANCED).build();

    /**
     * If {@code false}, prevents Block Explode from breaking blocks and damaging nearby entities.
     * @since 1.19.1
     * @see ExplosionListener
     */
    public static final Flag BLOCK_EXPLODE_DAMAGE = new Flag.Builder("BLOCK_EXPLODE_DAMAGE", Material.TNT_MINECART).type(Type.SETTING)
            .mode(Flag.Mode.ADVANCED).build();

    /**
     * If {@code false}, prevents TNT from breaking blocks and damaging nearby entities outside of island boundaries.
     * @since 1.15.3
     * @see ExplosionListener
     */
    public static final Flag WORLD_TNT_DAMAGE = new Flag.Builder("WORLD_TNT_DAMAGE", Material.TNT)
            .type(Type.WORLD_SETTING)
            .build();

    /**
     * If {@code false}, prevents Block Explode from breaking blocks and damaging nearby entities outside of island boundaries.
     * @since 1.19.1
     * @see ExplosionListener
     */
    public static final Flag WORLD_BLOCK_EXPLODE_DAMAGE = new Flag.Builder("WORLD_BLOCK_EXPLODE_DAMAGE", Material.TNT_MINECART)
            .type(Type.WORLD_SETTING)
            .build();

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

    /**
     * If {@code false}, prevents Endermans from teleporting
     * @since 1.22.1
     */
    public static final Flag ENDERMAN_TELEPORT = new Flag.Builder("ENDERMAN_TELEPORT", Material.ENDER_PEARL).type(Type.SETTING)
            .defaultSetting(true).listener(new MobTeleportListener()).mode(Flag.Mode.ADVANCED).build();
    /**
     * If {@code false}, prevents Shulkers from teleporting
     * Uses same listener as ENDERMAN_TELEPORT
     * @since 1.22.1
     */
    public static final Flag SHULKER_TELEPORT = new Flag.Builder("SHULKER_TELEPORT", Material.SHULKER_SHELL).type(Type.SETTING)
            .defaultSetting(true).mode(Flag.Mode.ADVANCED).build();


    public static final Flag ENTER_EXIT_MESSAGES = new Flag.Builder("ENTER_EXIT_MESSAGES", Material.DIRT).defaultSetting(true).type(Type.WORLD_SETTING)
            .listener(new EnterExitListener())
            .build();

    public static final Flag PISTON_PUSH = new Flag.Builder("PISTON_PUSH", Material.PISTON).defaultSetting(true).type(Type.WORLD_SETTING)
            .listener(new PistonPushListener())
            .build();

    private static final InvincibleVisitorsListener ilv = new InvincibleVisitorsListener();
    public static final Flag INVINCIBLE_VISITORS = new Flag.Builder("INVINCIBLE_VISITORS", Material.DIAMOND_CHESTPLATE).type(Type.WORLD_SETTING)
            .listener(ilv).clickHandler(ilv).usePanel(true).build();

    public static final Flag GEO_LIMIT_MOBS = new Flag.Builder("GEO_LIMIT_MOBS", Material.CHAINMAIL_CHESTPLATE).type(Type.WORLD_SETTING)
            .listener(new GeoLimitMobsListener()).clickHandler(new GeoLimitClickListener()).usePanel(true).build();

    /**
     * @since 1.12.0
     */
    public static final Flag LIMIT_MOBS = new Flag.Builder("LIMIT_MOBS", Material.CHAINMAIL_BOOTS).type(Type.WORLD_SETTING)
            .listener(new LimitMobsListener()).clickHandler(new MobLimitClickListener()).usePanel(true).build();

    public static final Flag REMOVE_MOBS = new Flag.Builder("REMOVE_MOBS", Material.GLOWSTONE_DUST).type(Type.WORLD_SETTING)
            .listener(new RemoveMobsListener()).defaultSetting(true).build();

    public static final Flag ITEM_FRAME_DAMAGE = new Flag.Builder("ITEM_FRAME_DAMAGE", Material.ITEM_FRAME).type(Type.WORLD_SETTING)
            .listener(new ItemFrameListener()).build();

    public static final Flag ISLAND_RESPAWN = new Flag.Builder("ISLAND_RESPAWN", Material.TORCH).type(Type.WORLD_SETTING)
            .listener(new IslandRespawnListener()).defaultSetting(true).build();

    /**
     * If disabled, prevents redstone from operating on islands whose members are offline.
     * @see OfflineRedstoneListener
     */
    public static final Flag OFFLINE_REDSTONE = new Flag.Builder("OFFLINE_REDSTONE", Material.COMPARATOR).type(Type.WORLD_SETTING)
            .listener(new OfflineRedstoneListener()).defaultSetting(true).build();

    /**
     * If disabled, prevents crops/plants from growing on islands whose members are offline.
     * @since 1.4.0
     * @see OfflineGrowthListener
     */
    public static final Flag OFFLINE_GROWTH = new Flag.Builder("OFFLINE_GROWTH", Material.WHEAT_SEEDS).type(Type.WORLD_SETTING)
            .listener(new OfflineGrowthListener()).defaultSetting(true).build();

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

    public static final Flag COARSE_DIRT_TILLING = new Flag.Builder("COARSE_DIRT_TILLING", Material.COARSE_DIRT).type(Type.WORLD_SETTING).defaultSetting(true).listener(new CoarseDirtTillingListener()).build();

    public static final Flag PREVENT_TELEPORT_WHEN_FALLING = new Flag.Builder("PREVENT_TELEPORT_WHEN_FALLING", Material.FEATHER).type(Type.WORLD_SETTING).build();

    public static final Flag OBSIDIAN_SCOOPING = new Flag.Builder("OBSIDIAN_SCOOPING", Material.OBSIDIAN).type(Type.WORLD_SETTING)
            .listener(new ObsidianScoopingListener()).defaultSetting(true).build();

    /**
     * Toggles whether liquids can flow outside an island's protection range or not.
     * It is disabled by default in order to avoid cobblestone/stone/obsidian being generated outside an island's protection range and remaining unbreakable by players.
     * Liquids will still flow vertically, however they won't spread horizontally if they're placed outside an island's protection range.
     *
     * @since 1.3.0
     * @see LiquidsFlowingOutListener
     */
    public static final Flag LIQUIDS_FLOWING_OUT = new Flag.Builder("LIQUIDS_FLOWING_OUT", Material.WATER_BUCKET).type(Type.WORLD_SETTING)
            .listener(new LiquidsFlowingOutListener()).build();

    /**
     * Enables toggling for removal of the end exit island. May not be required on some servers, e.g. PaperSpigot.
     * @since 1.3.0
     * @see world.bentobox.bentobox.listeners.BlockEndDragon
     */
    public static final Flag REMOVE_END_EXIT_ISLAND = new Flag.Builder("REMOVE_END_EXIT_ISLAND", Material.DRAGON_HEAD).type(Type.WORLD_SETTING).defaultSetting(true).build();

    /**
     * Toggles whether trees can grow outside an island's protection range or not.
     * Not only will it prevent saplings placed outside an island's protection range from growing, but it will also block generation of leaves/logs outside of it, thus "cutting" the tree.
     * It is disabled by default in order to avoid leaves/logs being generated outside an island's protection range and remaining unbreakable by players.
     *
     * @since 1.3.0
     * @see TreesGrowingOutsideRangeListener
     */
    public static final Flag TREES_GROWING_OUTSIDE_RANGE = new Flag.Builder("TREES_GROWING_OUTSIDE_RANGE", Material.OAK_SAPLING).type(Type.WORLD_SETTING).listener(new TreesGrowingOutsideRangeListener()).build();

    /**
     * Toggles whether monsters and animals can spawn naturally outside an island's protection range or not.
     * It is allowed by default.
     *
     * @since 1.3.0
     * @see NaturalSpawningOutsideRangeListener
     */
    public static final Flag NATURAL_SPAWNING_OUTSIDE_RANGE = new Flag.Builder("NATURAL_SPAWNING_OUTSIDE_RANGE", Material.ZOMBIE_SPAWN_EGG).type(Type.WORLD_SETTING).listener(new NaturalSpawningOutsideRangeListener()).defaultSetting(true).build();

    /**
     * Toggles wither explosion damage
     * @since 1.6.0
     * @see WitherListener
     */
    public static final Flag WITHER_DAMAGE = new Flag.Builder("WITHER_DAMAGE", Material.WITHER_SKELETON_SKULL).listener(new WitherListener()).type(Type.WORLD_SETTING).build();

    /**
     * Toggles whether players can change a spawner's entity using spawn eggs.
     * @since 1.7.0
     * @see SpawnerSpawnEggsListener
     */
    public static final Flag SPAWNER_SPAWN_EGGS = new Flag.Builder("SPAWNER_SPAWN_EGGS", Material.SPAWNER).listener(new SpawnerSpawnEggsListener()).type(Type.WORLD_SETTING).defaultSetting(true).build();

    /**
     * Keeps pets on the player's island.
     * @since 1.16.0
     * @see PetTeleportListener
     */
    public static final Flag PETS_STAY_AT_HOME = new Flag.Builder("PETS_STAY_AT_HOME", Material.TROPICAL_FISH).listener(new PetTeleportListener()).type(Type.WORLD_SETTING).defaultSetting(true).build();

    /**
     * Toggles whether island visitors keep their items if they die on another player's island.
     * @since 1.17.0
     * @see VisitorKeepInventoryListener
     */
    public static final Flag VISITOR_KEEP_INVENTORY = new Flag.Builder("VISITOR_KEEP_INVENTORY", Material.TOTEM_OF_UNDYING).listener(new VisitorKeepInventoryListener()).type(Type.WORLD_SETTING).defaultSetting(false).build();

    /**
     * Toggles whether island visitors can trigger to start a raid on another player's island.
     * @since 1.21.0
     * @see VisitorsStartingRaidListener
     */
    public static final Flag VISITOR_TRIGGER_RAID = new Flag.Builder("VISITOR_TRIGGER_RAID", Material.RAVAGER_SPAWN_EGG).listener(new VisitorsStartingRaidListener()).type(Type.WORLD_SETTING).defaultSetting(true).build();

    /**
     * Toggles whether entities can teleport between dimensions using portals.
     * @since 1.21.0
     * @see world.bentobox.bentobox.listeners.teleports.EntityTeleportListener
     */
    public static final Flag ENTITY_PORTAL_TELEPORT = new Flag.Builder("ENTITY_PORTAL_TELEPORT", Material.ENDER_EYE).type(Type.WORLD_SETTING).defaultSetting(false).build();

    /**
     * Harvest Setting
     * Controls who gets to harvest any crop related contents. e.g. Wheat, Sugar Cane, melon blocks, not stems, pumpkin blocks, etc.
     * Listener is {@link BreakBlocksListener}
     * @since 1.23.0
     */
    public static final Flag HARVEST = new Flag.Builder("HARVEST", Material.PUMPKIN).mode(Flag.Mode.BASIC).type(Type.PROTECTION).build();

    /**
     * Crop Planting
     * Controls who gets to plant crops on tilled soil.
     * Listener is {@link world.bentobox.bentobox.listeners.flags.protection.PlaceBlocksListener}
     * @since 1.23.0
     */
    public static final Flag CROP_PLANTING = new Flag.Builder("CROP_PLANTING", Material.PUMPKIN_SEEDS).mode(Flag.Mode.BASIC).type(Type.PROTECTION).build();

    /**
     * Sign edit protection
     * Listener is {@link BlockInteractionListener}
     * @since 1.24.0
     */
    public static final Flag SIGN_EDITING = new Flag.Builder("SIGN_EDITING", Material.DARK_OAK_SIGN).mode(Flag.Mode.BASIC).type(Type.PROTECTION).build();

    /**
     * Bell ringing protection
     * Listeners are {@link BlockInteractionListener} and {@link PhysicalInteractionListener}
     * @since 2.4.2
     */
    public static final Flag BELL_RINGING = new Flag.Builder("BELL_RINGING", Material.BELL).mode(Flag.Mode.EXPERT)
            .type(Type.PROTECTION).build();

    /**
     * Candle protection
     * Listener is {@link CandleListener}
     * @since 2.4.2
     */
    public static final Flag CANDLES = new Flag.Builder("CANDLES", Material.CANDLE).mode(Flag.Mode.EXPERT)
            .listener(new CandleListener())
            .type(Type.PROTECTION).build();

    /**
     * Prevents podzol production when large trees grow
     * @since 3.4.2
     */
    public static final Flag PODZOL = new Flag.Builder("PODZOL", Material.PODZOL).type(Type.WORLD_SETTING)
            .defaultSetting(false).listener(new PodzolListener()).build();

    /**
     * Provides a list of all the Flag instances contained in this class using reflection.
     * Deprecated Flags are ignored.
     * @return List of all the flags in this class
     */
    public static List<Flag> values() {
        return Arrays.stream(Flags.class.getFields())
                .filter(field -> field.getAnnotation(Deprecated.class) == null) // Ensures it is not deprecated
                .map(field -> {
                    try {
                        return (Flag)field.get(null);
                    } catch (IllegalArgumentException | IllegalAccessException e) {
                        Bukkit.getLogger().severe("Could not get Flag values " + e.getMessage());
                    }
                    return null;
                }).toList();
    }
}
