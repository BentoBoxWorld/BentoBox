package world.bentobox.bentobox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Banner;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import net.kyori.adventure.text.Component;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * Tests for {@link DefaultPasteUtil}.
 */
class DefaultPasteUtilTest extends CommonTestSetup {

    @Mock private BlueprintBlock bpBlock;
    @Mock private Block block;
    @Mock private BlockData blockData;
    @Mock private BlueprintCreatureSpawner blueprintSpawner;
    @Mock private CreatureSpawner creatureSpawner;
    @Mock private BlueprintEntity blueprintEntity;
    @Mock private Sign signState;
    @Mock private SignSide signSide;
    @Mock private org.bukkit.block.data.type.Sign signBlockData;
    @Mock private Biome biome;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        // DefaultPasteUtil has a private static final BentoBox plugin field captured at class-load
        // time. Mockito's clearInlineMocks() removes stubs from that captured mock between tests, so
        // we must re-point the field to the freshly-stubbed plugin mock before each test.
        injectPlugin();


        // Util.getChunkAtAsync returns an already-completed future so thenRun fires synchronously
        mockedUtil.when(() -> Util.getChunkAtAsync(any(org.bukkit.Location.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
        // Util.translateColorCodes passes through
        mockedUtil.when(() -> Util.translateColorCodes(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class));

        // Bukkit.createBlockData stubs
        mockedBukkit.when(() -> org.bukkit.Bukkit.createBlockData(anyString())).thenReturn(blockData);
        mockedBukkit.when(() -> org.bukkit.Bukkit.createBlockData(any(Material.class))).thenReturn(blockData);

        // location.getBlock
        when(location.getBlock()).thenReturn(block);
        // default block state: generic BlockState (not a special subtype)
        when(block.getState()).thenReturn(mock(BlockState.class));
        when(block.getWorld()).thenReturn(world);
        when(block.getBlockData()).thenReturn(signBlockData);

        // BlueprintBlock defaults
        when(bpBlock.getBlockData()).thenReturn("minecraft:stone");
        when(bpBlock.getBiome()).thenReturn(null);
        when(bpBlock.getInventory()).thenReturn(Map.of());
        when(bpBlock.getCreatureSpawner()).thenReturn(null);
        when(bpBlock.getItemsAdderBlock()).thenReturn(null);
        when(bpBlock.getBannerPatterns()).thenReturn(null);
        when(bpBlock.getSignLines(any(Side.class))).thenReturn(List.of("", "", "", ""));
        when(bpBlock.isGlowingText(any(Side.class))).thenReturn(false);

        // Sign blockdata (non-wall): getRotation used to determine BlockFace
        when(signBlockData.getRotation()).thenReturn(BlockFace.NORTH);
        // Sign state
        when(signState.getSide(any(Side.class))).thenReturn(signSide);

        // BlueprintEntity defaults
        when(blueprintEntity.getNpc()).thenReturn(null);
        when(blueprintEntity.getMythicMobsRecord()).thenReturn(null);
        when(blueprintEntity.getType()).thenReturn(null);
        when(blueprintEntity.getCustomName()).thenReturn(null);

        // World environment
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);

        // LocalesManager.getOrDefault → return defaultText (3rd arg)
        when(lm.getOrDefault(any(User.class), anyString(), anyString()))
                .thenAnswer(inv -> inv.getArgument(2, String.class));

        // Players manager: getName returns a non-null string to prevent NPE in sign text replace
        when(plugin.getPlayers().getName(any())).thenReturn("TestPlayer");
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /** Force DefaultPasteUtil.plugin to the current test's plugin mock via Unsafe. */
    private void injectPlugin() throws Exception {
        java.lang.reflect.Field f = DefaultPasteUtil.class.getDeclaredField("plugin");
        f.setAccessible(true);
        // Use sun.misc.Unsafe to bypass the final-field write restriction in Java 17+
        java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        Object staticBase = unsafe.staticFieldBase(f);
        long staticOffset = unsafe.staticFieldOffset(f);
        unsafe.putObject(staticBase, staticOffset, plugin);
    }

    // -------------------------------------------------------------------------
    // createBlockData
    // -------------------------------------------------------------------------

    @Test
    void testCreateBlockDataSuccess() {
        BlockData result = DefaultPasteUtil.createBlockData(bpBlock);
        assertEquals(blockData, result);
    }

    @Test
    void testCreateBlockDataFallsBackToConvertOnException() {
        // First call (the direct parse) throws; convertBlockData's AIR call returns blockData
        mockedBukkit.when(() -> org.bukkit.Bukkit.createBlockData("minecraft:stone"))
                .thenThrow(new IllegalArgumentException("unsupported"));
        BlockData result = DefaultPasteUtil.createBlockData(bpBlock);
        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // convertBlockData
    // -------------------------------------------------------------------------

    @Test
    void testConvertBlockDataUnknownBlockReturnsAir() {
        when(bpBlock.getBlockData()).thenReturn("minecraft:unknown_custom_block");
        BlockData result = DefaultPasteUtil.convertBlockData(bpBlock);
        // No conversion match → returns AIR blockdata (our stub for createBlockData(Material.AIR))
        assertEquals(blockData, result);
    }

    @Test
    void testConvertBlockDataSignToOakSign() {
        when(bpBlock.getBlockData()).thenReturn("minecraft:sign[facing=north]");
        BlockData converted = mock(BlockData.class);
        mockedBukkit.when(() -> org.bukkit.Bukkit.createBlockData("minecraft:oak_sign[facing=north]"))
                .thenReturn(converted);
        BlockData result = DefaultPasteUtil.convertBlockData(bpBlock);
        assertEquals(converted, result);
    }

    @Test
    void testConvertBlockDataWallSignToOakWallSign() {
        when(bpBlock.getBlockData()).thenReturn("minecraft:wall_sign[facing=east]");
        BlockData converted = mock(BlockData.class);
        mockedBukkit.when(() -> org.bukkit.Bukkit.createBlockData("minecraft:oak_wall_sign[facing=east]"))
                .thenReturn(converted);
        BlockData result = DefaultPasteUtil.convertBlockData(bpBlock);
        assertEquals(converted, result);
    }

    @Test
    void testConvertBlockDataIllegalArgumentLogsWarnings() {
        when(bpBlock.getBlockData()).thenReturn("minecraft:sign[facing=north]");
        mockedBukkit.when(() -> org.bukkit.Bukkit.createBlockData("minecraft:oak_sign[facing=north]"))
                .thenThrow(new IllegalArgumentException("unsupported server version"));
        DefaultPasteUtil.convertBlockData(bpBlock);
        verify(plugin, atLeastOnce()).logWarning(anyString());
    }

    // -------------------------------------------------------------------------
    // setBlock
    // -------------------------------------------------------------------------

    @Test
    void testSetBlockReturnsDoneAndSetsBlockData() throws Exception {
        CompletableFuture<Void> future = DefaultPasteUtil.setBlock(island, location, bpBlock);
        assertTrue(future.isDone());
        verify(block).setBlockData(blockData, false);
    }

    @Test
    void testSetBlockSetsBiomeWhenPresent() throws Exception {
        when(bpBlock.getBiome()).thenReturn(biome);
        DefaultPasteUtil.setBlock(island, location, bpBlock);
        verify(block).setBiome(biome);
    }

    @Test
    void testSetBlockSkipsBiomeWhenNull() throws Exception {
        when(bpBlock.getBiome()).thenReturn(null);
        DefaultPasteUtil.setBlock(island, location, bpBlock);
        verify(block, never()).setBiome(any());
    }

    // -------------------------------------------------------------------------
    // setSpawner
    // -------------------------------------------------------------------------

    @Test
    void testSetSpawnerAppliesAllFields() {
        when(blueprintSpawner.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        when(blueprintSpawner.getMaxNearbyEntities()).thenReturn(5);
        when(blueprintSpawner.getMaxSpawnDelay()).thenReturn(200);
        when(blueprintSpawner.getMinSpawnDelay()).thenReturn(100);
        when(blueprintSpawner.getDelay()).thenReturn(10);
        when(blueprintSpawner.getRequiredPlayerRange()).thenReturn(16);
        when(blueprintSpawner.getSpawnRange()).thenReturn(4);

        DefaultPasteUtil.setSpawner(creatureSpawner, blueprintSpawner);

        verify(creatureSpawner).setSpawnedType(EntityType.ZOMBIE);
        verify(creatureSpawner).setMaxNearbyEntities(5);
        verify(creatureSpawner).setMaxSpawnDelay(200);
        verify(creatureSpawner).setMinSpawnDelay(100);
        verify(creatureSpawner).setDelay(10);
        verify(creatureSpawner).setRequiredPlayerRange(16);
        verify(creatureSpawner).setSpawnRange(4);
        verify(creatureSpawner).update(true, false);
    }

    // -------------------------------------------------------------------------
    // setBlockState
    // -------------------------------------------------------------------------

    @Test
    void testSetBlockStateInventoryHolder() {
        // InventoryHolder does not extend BlockState, so we need a combined mock
        InventoryHolder holder = mock(InventoryHolder.class, withSettings().extraInterfaces(BlockState.class));
        Inventory inv = mock(Inventory.class);
        when(holder.getInventory()).thenReturn(inv);
        when(block.getState()).thenReturn((BlockState) holder);
        ItemStack item = mock(ItemStack.class);
        when(bpBlock.getInventory()).thenReturn(Map.of(0, item));

        DefaultPasteUtil.setBlockState(island, block, bpBlock);

        verify(inv).setItem(0, item);
    }

    @Test
    void testSetBlockStateCreatureSpawner() {
        // CreatureSpawner extends BlockState
        when(block.getState()).thenReturn(creatureSpawner);
        when(bpBlock.getCreatureSpawner()).thenReturn(blueprintSpawner);
        when(blueprintSpawner.getSpawnedType()).thenReturn(EntityType.ZOMBIE);
        when(blueprintSpawner.getMaxNearbyEntities()).thenReturn(1);
        when(blueprintSpawner.getMaxSpawnDelay()).thenReturn(10);
        when(blueprintSpawner.getMinSpawnDelay()).thenReturn(1);
        when(blueprintSpawner.getDelay()).thenReturn(1);
        when(blueprintSpawner.getRequiredPlayerRange()).thenReturn(8);
        when(blueprintSpawner.getSpawnRange()).thenReturn(2);

        DefaultPasteUtil.setBlockState(island, block, bpBlock);

        verify(creatureSpawner).setSpawnedType(EntityType.ZOMBIE);
        verify(creatureSpawner).update(true, false);
    }

    @Test
    void testSetBlockStateBannerSetsPatterns() {
        // Banner extends BlockState
        Banner banner = mock(Banner.class);
        when(block.getState()).thenReturn(banner);
        List<Pattern> patterns = new ArrayList<>(List.of(mock(Pattern.class)));
        when(bpBlock.getBannerPatterns()).thenReturn(patterns);

        DefaultPasteUtil.setBlockState(island, block, bpBlock);

        verify(banner).setPatterns(patterns);
        verify(banner).update(true, false);
    }

    @Test
    void testSetBlockStateBannerNullPatternsSkipped() {
        Banner banner = mock(Banner.class);
        when(block.getState()).thenReturn(banner);
        when(bpBlock.getBannerPatterns()).thenReturn(null);

        DefaultPasteUtil.setBlockState(island, block, bpBlock);

        verify(banner, never()).setPatterns(any());
    }

    @Test
    void testSetBlockStateSignCallsWriteSignForEachSide() {
        // Sign extends BlockState
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(any(Side.class))).thenReturn(List.of("a", "b", "c", "d"));

        DefaultPasteUtil.setBlockState(island, block, bpBlock);

        // writeSign is called once per Side (FRONT and BACK = 2 sides)
        verify(signState, times(Side.values().length)).getSide(any(Side.class));
    }

    @Test
    void testSetBlockStateItemsAdderTriesHook() {
        when(bpBlock.getItemsAdderBlock()).thenReturn("ia:custom_block");

        DefaultPasteUtil.setBlockState(island, block, bpBlock);

        // Hook lookup is attempted; absent by default so nothing further happens
        verify(hooksManager).getHook("ItemsAdder");
    }

    @Test
    void testSetBlockStateGenericDoesNothing() {
        // Default state is a plain BlockState mock — no special handling
        DefaultPasteUtil.setBlockState(island, block, bpBlock);
        // No inventory, spawner, sign, or banner interactions
        verify(block).getState();
    }

    // -------------------------------------------------------------------------
    // setEntity
    // -------------------------------------------------------------------------

    @Test
    void testSetEntityReturnsDoneAndSpawnsEntities() {
        when(blueprintEntity.getType()).thenReturn(EntityType.ZOMBIE);
        Entity e = mock(Entity.class);
        when(world.spawnEntity(any(), any(EntityType.class))).thenReturn(e);

        CompletableFuture<Void> future = DefaultPasteUtil.setEntity(island, location, List.of(blueprintEntity));

        assertTrue(future.isDone());
        verify(world).spawnEntity(location, EntityType.ZOMBIE);
    }

    @Test
    void testSetEntityEmptyListSpawnsNothing() {
        CompletableFuture<Void> future = DefaultPasteUtil.setEntity(island, location, List.of());

        assertTrue(future.isDone());
        verify(world, never()).spawnEntity(any(), any(EntityType.class));
    }

    // -------------------------------------------------------------------------
    // spawnBlueprintEntity
    // -------------------------------------------------------------------------

    @Test
    void testSpawnBlueprintEntityNullTypeReturnsFalse() {
        when(blueprintEntity.getType()).thenReturn(null);
        boolean result = DefaultPasteUtil.spawnBlueprintEntity(blueprintEntity, location, island);
        assertFalse(result);
        verify(world, never()).spawnEntity(any(), any(EntityType.class));
    }

    @Test
    void testSpawnBlueprintEntitySpawnsBukkitEntity() {
        when(blueprintEntity.getType()).thenReturn(EntityType.ZOMBIE);
        Entity e = mock(Entity.class);
        when(world.spawnEntity(any(), any(EntityType.class))).thenReturn(e);

        boolean result = DefaultPasteUtil.spawnBlueprintEntity(blueprintEntity, location, island);

        assertTrue(result);
        verify(world).spawnEntity(location, EntityType.ZOMBIE);
        verify(blueprintEntity).configureEntity(e);
    }

    @Test
    void testSpawnBlueprintEntitySetsCustomNameWithOwnerPlaceholder() {
        when(blueprintEntity.getType()).thenReturn(EntityType.ZOMBIE);
        when(blueprintEntity.getCustomName()).thenReturn("Boss of " + TextVariables.NAME);
        Entity e = mock(Entity.class);
        when(world.spawnEntity(any(), any(EntityType.class))).thenReturn(e);
        // island.getOwner() → uuid, User.getInstance(uuid) → user backed by mockPlayer (name "tastybento")
        // PlaceholdersManager.replacePlaceholders is stubbed in CommonTestSetup to pass text through

        DefaultPasteUtil.spawnBlueprintEntity(blueprintEntity, location, island);

        verify(e).customName(Component.text("Boss of tastybento"));
    }

    @Test
    void testSpawnBlueprintEntityCustomNameWithNullIsland() {
        when(blueprintEntity.getType()).thenReturn(EntityType.ZOMBIE);
        when(blueprintEntity.getCustomName()).thenReturn("Lonely Boss");
        Entity e = mock(Entity.class);
        when(world.spawnEntity(any(), any(EntityType.class))).thenReturn(e);

        DefaultPasteUtil.spawnBlueprintEntity(blueprintEntity, location, null);

        // No owner available; name is used as-is
        verify(e).customName(Component.text("Lonely Boss"));
    }

    @Test
    void testSpawnBlueprintEntityNoHooksForMythicMobsFallsThrough() {
        // getMythicMobsRecord is non-null but MythicMobs hook is absent → Bukkit spawn
        BlueprintEntity.MythicMobRecord record =
                new BlueprintEntity.MythicMobRecord("Zombie", null, 1.0, 1.0f, null);
        when(blueprintEntity.getMythicMobsRecord()).thenReturn(record);
        when(blueprintEntity.getType()).thenReturn(EntityType.ZOMBIE);
        Entity e = mock(Entity.class);
        when(world.spawnEntity(any(), any(EntityType.class))).thenReturn(e);

        boolean result = DefaultPasteUtil.spawnBlueprintEntity(blueprintEntity, location, island);

        assertTrue(result);
        verify(world).spawnEntity(location, EntityType.ZOMBIE);
    }

    // -------------------------------------------------------------------------
    // writeSign
    // -------------------------------------------------------------------------

    @Test
    void testWriteSignNormalPastesLines() {
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(Side.FRONT)).thenReturn(List.of("line0", "line1", "line2", "line3"));
        when(bpBlock.isGlowingText(Side.FRONT)).thenReturn(true);

        DefaultPasteUtil.writeSign(island, block, bpBlock, Side.FRONT);

        verify(signSide).setLine(0, "line0");
        verify(signSide).setLine(1, "line1");
        verify(signSide).setLine(2, "line2");
        verify(signSide).setLine(3, "line3");
        verify(signSide).setGlowingText(true);
        verify(signState).update();
    }

    @Test
    void testWriteSignSpawnHereSetsAirAndSpawnPoint() {
        when(block.getState()).thenReturn(signState);
        when(block.getX()).thenReturn(10);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(20);
        when(bpBlock.getSignLines(Side.FRONT)).thenReturn(List.of("[spawn_here]", "", "", ""));

        DefaultPasteUtil.writeSign(island, block, bpBlock, Side.FRONT);

        verify(block).setType(Material.AIR);
        verify(island).setSpawnPoint(eq(Environment.NORMAL), any(org.bukkit.Location.class));
        // Normal paste logic must NOT run
        verify(signSide, never()).setLine(any(Integer.class), anyString());
    }

    @Test
    void testWriteSignSpawnHereWaterloggedSetsWater() {
        // Make blockdata a WallSign that is also Waterlogged
        WallSign wallSign = mock(WallSign.class, withSettings().extraInterfaces(Waterlogged.class));
        when(((Waterlogged) wallSign).isWaterlogged()).thenReturn(true);
        when(wallSign.getFacing()).thenReturn(BlockFace.NORTH);
        when(block.getBlockData()).thenReturn(wallSign);
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(Side.FRONT)).thenReturn(List.of("[spawn_here]", "", "", ""));

        DefaultPasteUtil.writeSign(island, block, bpBlock, Side.FRONT);

        verify(block).setType(Material.WATER);
        verify(island).setSpawnPoint(eq(Environment.NORMAL), any(org.bukkit.Location.class));
    }

    @Test
    void testWriteSignSpawnHereOnBackSideIgnored() {
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(Side.BACK)).thenReturn(List.of("[spawn_here]", "", "", ""));

        // Side.BACK — spawn_here check requires Side.FRONT, so this goes to normal paste
        DefaultPasteUtil.writeSign(island, block, bpBlock, Side.BACK);

        verify(block, never()).setType(any());
        verify(signSide).setLine(0, "[spawn_here]");
    }

    @Test
    void testWriteSignSpawnHereNullIslandPastesNormally() {
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(Side.FRONT)).thenReturn(List.of("[spawn_here]", "", "", ""));

        DefaultPasteUtil.writeSign(null, block, bpBlock, Side.FRONT);

        verify(block, never()).setType(any());
        verify(signSide).setLine(0, "[spawn_here]");
    }

    @Test
    void testWriteSignStartTextCallsLocalesManager() {
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(Side.FRONT)).thenReturn(List.of("[start]", "", "", ""));
        when(island.getWorld()).thenReturn(world);
        // iwm.getAddon already returns Optional.empty() from CommonTestSetup

        DefaultPasteUtil.writeSign(island, block, bpBlock, Side.FRONT);

        // Locales manager queried for each of the 4 lines
        verify(lm, times(4)).getOrDefault(any(User.class), anyString(), anyString());
    }

    @Test
    void testWriteSignStartTextNullOwnerSkipsLocales() {
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(Side.FRONT)).thenReturn(List.of("[start]", "", "", ""));
        when(island.getOwner()).thenReturn(null);
        when(island.getWorld()).thenReturn(world);

        DefaultPasteUtil.writeSign(island, block, bpBlock, Side.FRONT);

        // No User available — locales are not queried
        verify(lm, never()).getOrDefault(any(User.class), anyString(), anyString());
    }

    @Test
    void testWriteSignWallSignUsesWallSignFacing() {
        WallSign wallSign = mock(WallSign.class);
        when(wallSign.getFacing()).thenReturn(BlockFace.EAST);
        when(block.getBlockData()).thenReturn(wallSign);
        when(block.getState()).thenReturn(signState);
        when(bpBlock.getSignLines(Side.FRONT)).thenReturn(List.of("text", "", "", ""));

        DefaultPasteUtil.writeSign(island, block, bpBlock, Side.FRONT);

        // Should complete without error (WallSign branch taken)
        verify(signSide).setLine(0, "text");
    }
}
