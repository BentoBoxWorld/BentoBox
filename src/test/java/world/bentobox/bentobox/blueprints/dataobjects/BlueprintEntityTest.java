package world.bentobox.bentobox.blueprints.dataobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bukkit.Art;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Rotation;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord;

/**
 * @author tastybento
 *
 */
class BlueprintEntityTest extends CommonTestSetup {

    @Mock
    private Villager villager;
    @Mock
    private Sheep sheep;
    @Mock
    private Wolf wolf;
    @Mock
    private ChestedHorse chestedHorse;
    @Mock
    private Cow cow;
    @Mock
    private Horse horse;
    @Mock
    private Display display;
    @Mock
    private World mockWorld;
    @Mock
    private ItemFrame itemFrame;
    @Mock
    private Painting painting;

    private BlueprintEntity blueprint;


    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(villager.getProfession())
                .thenReturn(Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft("librarian")));
        when(villager.getVillagerExperience()).thenReturn(100);
        when(villager.getVillagerLevel()).thenReturn(2);
        when(villager.getVillagerType()).thenReturn(Villager.Type.PLAINS);
        when(villager.getLocation()).thenReturn(location);
        when(sheep.getColor()).thenReturn(DyeColor.BLUE);
        when(sheep.getLocation()).thenReturn(location);
        when(wolf.isTamed()).thenReturn(true);
        when(wolf.getLocation()).thenReturn(location);
        when(chestedHorse.isCarryingChest()).thenReturn(true);
        when(chestedHorse.getLocation()).thenReturn(location);
        when(horse.getDomestication()).thenReturn(50);
        when(horse.getStyle()).thenReturn(Horse.Style.WHITE_DOTS);
        when(horse.getLocation()).thenReturn(location);
        when(cow.getLocation()).thenReturn(location);
        when(itemFrame.getLocation()).thenReturn(location);
        when(painting.getLocation()).thenReturn(location);

        blueprint = new BlueprintEntity();
        when(display.getType()).thenReturn(EntityType.PLAYER);
        when(display.isGlowing()).thenReturn(false);
        when(display.hasGravity()).thenReturn(true);
        when(display.isVisualFire()).thenReturn(false);
        when(display.isSilent()).thenReturn(false);
        when(display.isInvulnerable()).thenReturn(false);
        when(display.getFireTicks()).thenReturn(0);
        when(display.getLocation()).thenReturn(location);
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }


    @Test
    void testConfigureEntityWithVillager() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setType(EntityType.VILLAGER);
        localBlueprint.setProfession(Profession.LIBRARIAN);
        localBlueprint.setExperience(100);
        localBlueprint.setVillagerType(Villager.Type.PLAINS);

        localBlueprint.configureEntity(villager);

        assertEquals(Profession.LIBRARIAN, villager.getProfession());
        assertEquals(100, villager.getVillagerExperience());
        assertEquals(2, villager.getVillagerLevel());
        assertEquals(Villager.Type.PLAINS, villager.getVillagerType());
    }

    @Test
    void testConfigureEntityWithColorable() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setType(EntityType.SHEEP);
        localBlueprint.setColor(DyeColor.BLUE);

        localBlueprint.configureEntity(sheep);

        assertEquals(DyeColor.BLUE, sheep.getColor());
    }

    @Test
    void testConfigureEntityWithTameable() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setType(EntityType.WOLF);
        localBlueprint.setTamed(true);

        localBlueprint.configureEntity(wolf);

        assertTrue(wolf.isTamed());
    }

    @Test
    void testConfigureEntityWithChestedHorse() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setType(EntityType.HORSE);
        localBlueprint.setChest(true);


        localBlueprint.configureEntity(chestedHorse);

        assertTrue(chestedHorse.isCarryingChest());
    }

    @Test
    void testConfigureEntityWithAgeable() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setType(EntityType.COW);
        localBlueprint.setAdult(false);

        localBlueprint.configureEntity(cow);

        assertFalse(cow.isAdult());
    }

    @Test
    void testConfigureEntityWithAbstractHorse() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setType(EntityType.HORSE);
        localBlueprint.setDomestication(50);

        localBlueprint.configureEntity(horse);

        assertEquals(50, horse.getDomestication());
    }

    @Test
    void testConfigureEntityWithHorse() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setType(EntityType.HORSE);
        localBlueprint.setStyle(Style.WHITE_DOTS);

        localBlueprint.configureEntity(horse);

        assertEquals(Style.WHITE_DOTS, horse.getStyle());
    }

    @Test
    void testGettersAndSetters() {
        BlueprintEntity localBlueprint = new BlueprintEntity();

        localBlueprint.setColor(DyeColor.RED);
        assertEquals(DyeColor.RED, localBlueprint.getColor());

        localBlueprint.setType(EntityType.CREEPER);
        assertEquals(EntityType.CREEPER, localBlueprint.getType());

        localBlueprint.setCustomName("My Entity");
        assertEquals("My Entity", localBlueprint.getCustomName());

        localBlueprint.setTamed(true);
        assertTrue(localBlueprint.getTamed());

        localBlueprint.setChest(true);
        assertTrue(localBlueprint.getChest());

        localBlueprint.setAdult(false);
        assertFalse(localBlueprint.getAdult());

        localBlueprint.setDomestication(75);
        assertEquals(75, localBlueprint.getDomestication().intValue());

        Map<Integer, ItemStack> inventory = new HashMap<>();
        inventory.put(1, new ItemStack(Material.DIAMOND));
        localBlueprint.setInventory(inventory);
        assertEquals(inventory, localBlueprint.getInventory());

        localBlueprint.setStyle(Style.WHITE);
        assertEquals(Style.WHITE, localBlueprint.getStyle());

        localBlueprint.setLevel(5);
        assertEquals(5, localBlueprint.getLevel().intValue());

        localBlueprint.setProfession(Profession.FARMER);
        assertEquals(Profession.FARMER, localBlueprint.getProfession());

        localBlueprint.setExperience(500);
        assertEquals(500, localBlueprint.getExperience().intValue());

        localBlueprint.setVillagerType(Villager.Type.TAIGA);
        assertEquals(Villager.Type.TAIGA, localBlueprint.getVillagerType());
    }

    @Test
    void testMythicMobs() {
        BlueprintEntity localBlueprint = new BlueprintEntity();
        MythicMobRecord mmr = new MythicMobRecord("string", "string2", 10D, 1F, "string3");
        localBlueprint.setMythicMobsRecord(mmr);
        assertEquals(mmr, localBlueprint.getMythicMobsRecord());
    }

    @Test
    void testIsGlowing() {
        blueprint.setGlowing(true);
        assertTrue(blueprint.isGlowing());
    }

    @Test
    void testIsGravity() {
        blueprint.setGravity(false);
        assertFalse(blueprint.isGravity());
    }

    @Test
    void testIsVisualFire() {
        blueprint.setVisualFire(true);
        assertTrue(blueprint.isVisualFire());
    }

    @Test
    void testIsSilent() {
        blueprint.setSilent(true);
        assertTrue(blueprint.isSilent());
    }

    @Test
    void testIsInvulnerable() {
        blueprint.setInvulnerable(true);
        assertTrue(blueprint.isInvulnerable());
    }

    @Test
    void testFireTicks() {
        blueprint.setFireTicks(20);
        assertEquals(20, blueprint.getFireTicks());
    }

    @Test
    void testSerializeItemFrame() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        when(itemFrame.getType()).thenReturn(EntityType.ITEM_FRAME);
        when(itemFrame.getItem()).thenReturn(item);
        when(itemFrame.getRotation()).thenReturn(Rotation.CLOCKWISE);
        when(itemFrame.isFixed()).thenReturn(true);
        when(itemFrame.isVisible()).thenReturn(false);
        when(itemFrame.getItemDropChance()).thenReturn(0.5F);
        when(itemFrame.getFacing()).thenReturn(BlockFace.EAST);

        BlueprintEntity localBlueprint = new BlueprintEntity(itemFrame);

        assertEquals(BlockFace.EAST, localBlueprint.getFacing());
        assertNotNull(localBlueprint.getItemFrame());
        assertEquals(item, localBlueprint.getItemFrame().item());
        assertEquals(Rotation.CLOCKWISE, localBlueprint.getItemFrame().rotation());
        assertTrue(localBlueprint.getItemFrame().isFixed());
        assertFalse(localBlueprint.getItemFrame().isVisible());
        assertEquals(0.5F, localBlueprint.getItemFrame().dropChance());
    }

    @Test
    void testConfigureEntityWithItemFrame() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.ITEM_FRAME);
        localBlueprint.setFacing(BlockFace.SOUTH);
        localBlueprint.setItemFrame(new BlueprintEntity.ItemFrameRec(item, Rotation.FLIPPED, true, false, 0.25F));

        localBlueprint.configureEntity(itemFrame);

        verify(itemFrame).setFacingDirection(BlockFace.SOUTH, true);
        verify(itemFrame).setItem(item);
        verify(itemFrame).setRotation(Rotation.FLIPPED);
        verify(itemFrame).setFixed(true);
        verify(itemFrame).setVisible(false);
        verify(itemFrame).setItemDropChance(0.25F);
    }

    @Test
    void testConfigureEntityWithItemFrameNoStoredData() {
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.ITEM_FRAME);

        localBlueprint.configureEntity(itemFrame);

        // No facing or frame record stored (e.g. legacy blueprint) - nothing applied
        verify(itemFrame, never()).setFacingDirection(any(), anyBoolean());
        verify(itemFrame, never()).setItem(any());
    }

    @Test
    void testSerializePainting() {
        Art art = Registry.ART.get(NamespacedKey.minecraft("wanderer"));
        assertNotNull(art);
        when(painting.getType()).thenReturn(EntityType.PAINTING);
        when(painting.getFacing()).thenReturn(BlockFace.NORTH);
        when(painting.getArt()).thenReturn(art);

        BlueprintEntity localBlueprint = new BlueprintEntity(painting);

        assertEquals(BlockFace.NORTH, localBlueprint.getFacing());
        assertEquals("minecraft:wanderer", localBlueprint.getPaintingArt());
    }

    @Test
    void testConfigureEntityWithPainting() {
        Art art = Registry.ART.get(NamespacedKey.minecraft("wanderer"));
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.PAINTING);
        localBlueprint.setFacing(BlockFace.WEST);
        localBlueprint.setPaintingArt("minecraft:wanderer");

        localBlueprint.configureEntity(painting);

        verify(painting).setFacingDirection(BlockFace.WEST, true);
        verify(painting).setArt(art, true);
    }

    /**
     * Stub mockWorld.spawn to run the pre-spawn consumer on the painting mock and return it.
     */
    private void stubPaintingSpawn() {
        when(mockWorld.spawn(any(Location.class), eq(Painting.class),
                ArgumentMatchers.<Consumer<? super Painting>>any())).thenAnswer(inv -> {
                    Consumer<Painting> consumer = inv.getArgument(2);
                    consumer.accept(painting);
                    return painting;
                });
    }

    private Location capturePaintingSpawnLocation() {
        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(mockWorld).spawn(captor.capture(), eq(Painting.class),
                ArgumentMatchers.<Consumer<? super Painting>>any());
        return captor.getValue();
    }

    @Test
    void testSpawnPaintingEvenSizeFacingSouthShiftsAnchorWestAndDown() {
        stubPaintingSpawn();
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.PAINTING);
        localBlueprint.setFacing(BlockFace.SOUTH);
        localBlueprint.setPaintingArt("minecraft:bust"); // 2 wide x 2 high

        Painting result = localBlueprint.spawnPainting(new Location(mockWorld, 10.5, 64.0, 20.5));

        assertEquals(painting, result);
        Location anchor = capturePaintingSpawnLocation();
        // Facing SOUTH: center is half a block towards EAST (+x), so anchor is one block west
        assertEquals(9.5, anchor.getX());
        // Even height: center is half a block up, so anchor is one block down
        assertEquals(63.0, anchor.getY());
        assertEquals(20.5, anchor.getZ());
        verify(painting).setFacingDirection(BlockFace.SOUTH, true);
        verify(painting).setArt(Registry.ART.get(NamespacedKey.minecraft("bust")), true);
    }

    @Test
    void testSpawnPaintingEvenSizeFacingNorthOnlyShiftsDown() {
        stubPaintingSpawn();
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.PAINTING);
        localBlueprint.setFacing(BlockFace.NORTH);
        localBlueprint.setPaintingArt("minecraft:bust"); // 2 wide x 2 high

        localBlueprint.spawnPainting(new Location(mockWorld, 10.5, 64.0, 20.5));

        Location anchor = capturePaintingSpawnLocation();
        // Facing NORTH: center is half a block towards WEST (-x), still in the anchor block
        assertEquals(10.5, anchor.getX());
        assertEquals(63.0, anchor.getY());
        assertEquals(20.5, anchor.getZ());
    }

    @Test
    void testSpawnPaintingEvenWidthFacingWestShiftsAnchorNorth() {
        stubPaintingSpawn();
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.PAINTING);
        localBlueprint.setFacing(BlockFace.WEST);
        localBlueprint.setPaintingArt("minecraft:pool"); // 2 wide x 1 high

        localBlueprint.spawnPainting(new Location(mockWorld, 10.5, 64.0, 20.5));

        Location anchor = capturePaintingSpawnLocation();
        assertEquals(10.5, anchor.getX());
        // Odd height: no vertical shift
        assertEquals(64.0, anchor.getY());
        // Facing WEST: center is half a block towards SOUTH (+z), so anchor is one block north
        assertEquals(19.5, anchor.getZ());
    }

    @Test
    void testSpawnPaintingOddSizeNoAnchorShift() {
        stubPaintingSpawn();
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.PAINTING);
        localBlueprint.setFacing(BlockFace.SOUTH);
        localBlueprint.setPaintingArt("minecraft:kebab"); // 1 wide x 1 high

        localBlueprint.spawnPainting(new Location(mockWorld, 10.5, 64.0, 20.5));

        Location anchor = capturePaintingSpawnLocation();
        assertEquals(10.5, anchor.getX());
        assertEquals(64.0, anchor.getY());
        assertEquals(20.5, anchor.getZ());
    }

    @Test
    void testSpawnPaintingLegacyBlueprintNoArtNoFacing() {
        stubPaintingSpawn();
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.PAINTING);

        localBlueprint.spawnPainting(new Location(mockWorld, 10.5, 64.0, 20.5));

        // No stored data: spawn at the stored block, nothing forced
        Location anchor = capturePaintingSpawnLocation();
        assertEquals(10.5, anchor.getX());
        assertEquals(64.0, anchor.getY());
        assertEquals(20.5, anchor.getZ());
        verify(painting, never()).setFacingDirection(any(), anyBoolean());
        verify(painting, never()).setArt(any(), anyBoolean());
    }

    @Test
    void testConfigureEntityWithPaintingUnknownArt() {
        BlueprintEntity localBlueprint = new BlueprintEntity();
        localBlueprint.setType(EntityType.PAINTING);
        localBlueprint.setPaintingArt("minecraft:does_not_exist");

        localBlueprint.configureEntity(painting);

        verify(painting, never()).setArt(any(), anyBoolean());
    }

    @Test
    void testSetDisplay() {
        when(location.getWorld()).thenReturn(mockWorld);
        when(location.clone()).thenReturn(location);
        when(mockWorld.spawn(any(Location.class), eq(Display.class))).thenReturn(display);
        blueprint.storeDisplay(display);
        blueprint.setDisplay(location);

        assertNotNull(blueprint.displayRec);
    }

}
