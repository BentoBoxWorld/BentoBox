package world.bentobox.bentobox.blueprints.dataobjects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord;

/**
 * @author tastybento
 *
 */
public class BlueprintEntityTest extends CommonTestSetup {

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
    public void testConfigureEntityWithVillager() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.VILLAGER);
        blueprint.setProfession(Profession.LIBRARIAN);
        blueprint.setExperience(100);
        blueprint.setVillagerType(Villager.Type.PLAINS);

        blueprint.configureEntity(villager);

        assertEquals(Profession.LIBRARIAN, villager.getProfession());
        assertEquals(100, villager.getVillagerExperience());
        assertEquals(2, villager.getVillagerLevel());
        assertEquals(Villager.Type.PLAINS, villager.getVillagerType());
    }

    @Test
    public void testConfigureEntityWithColorable() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.SHEEP);
        blueprint.setColor(DyeColor.BLUE);

        blueprint.configureEntity(sheep);

        assertEquals(DyeColor.BLUE, sheep.getColor());
    }

    @Test
    public void testConfigureEntityWithTameable() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.WOLF);
        blueprint.setTamed(true);

        blueprint.configureEntity(wolf);

        assertTrue(wolf.isTamed());
    }

    @Test
    public void testConfigureEntityWithChestedHorse() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.HORSE);
        blueprint.setChest(true);
        

        blueprint.configureEntity(chestedHorse);

        assertTrue(chestedHorse.isCarryingChest());
    }

    @Test
    public void testConfigureEntityWithAgeable() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.COW);
        blueprint.setAdult(false);

        blueprint.configureEntity(cow);

        assertFalse(cow.isAdult());
    }

    @Test
    public void testConfigureEntityWithAbstractHorse() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.HORSE);
        blueprint.setDomestication(50);

        blueprint.configureEntity(horse);

        assertEquals(50, horse.getDomestication());
    }

    @Test
    public void testConfigureEntityWithHorse() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.HORSE);
        blueprint.setStyle(Style.WHITE_DOTS);

        blueprint.configureEntity(horse);

        assertEquals(Style.WHITE_DOTS, horse.getStyle());
    }

    @Test
    public void testGettersAndSetters() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setColor(DyeColor.RED);
        assertEquals(DyeColor.RED, blueprint.getColor());

        blueprint.setType(EntityType.CREEPER);
        assertEquals(EntityType.CREEPER, blueprint.getType());

        blueprint.setCustomName("My Entity");
        assertEquals("My Entity", blueprint.getCustomName());

        blueprint.setTamed(true);
        assertTrue(blueprint.getTamed());

        blueprint.setChest(true);
        assertTrue(blueprint.getChest());

        blueprint.setAdult(false);
        assertFalse(blueprint.getAdult());

        blueprint.setDomestication(75);
        assertEquals(75, blueprint.getDomestication().intValue());

        Map<Integer, ItemStack> inventory = new HashMap<>();
        inventory.put(1, new ItemStack(Material.DIAMOND));
        blueprint.setInventory(inventory);
        assertEquals(inventory, blueprint.getInventory());

        blueprint.setStyle(Style.WHITE);
        assertEquals(Style.WHITE, blueprint.getStyle());

        blueprint.setLevel(5);
        assertEquals(5, blueprint.getLevel().intValue());

        blueprint.setProfession(Profession.FARMER);
        assertEquals(Profession.FARMER, blueprint.getProfession());

        blueprint.setExperience(500);
        assertEquals(500, blueprint.getExperience().intValue());

        blueprint.setVillagerType(Villager.Type.TAIGA);
        assertEquals(Villager.Type.TAIGA, blueprint.getVillagerType());
    }

    @Test
    public void testMythicMobs() {
        BlueprintEntity blueprint = new BlueprintEntity();
        MythicMobRecord mmr = new MythicMobRecord("string", "string2", 10D, 1F, "string3");
        blueprint.setMythicMobsRecord(mmr);
        assertEquals(mmr, blueprint.getMythicMobsRecord());
    }

    @Test
    public void testIsGlowing() {
        blueprint.setGlowing(true);
        assertTrue(blueprint.isGlowing());
    }

    @Test
    public void testIsGravity() {
        blueprint.setGravity(false);
        assertFalse(blueprint.isGravity());
    }

    @Test
    public void testIsVisualFire() {
        blueprint.setVisualFire(true);
        assertTrue(blueprint.isVisualFire());
    }

    @Test
    public void testIsSilent() {
        blueprint.setSilent(true);
        assertTrue(blueprint.isSilent());
    }

    @Test
    public void testIsInvulnerable() {
        blueprint.setInvulnerable(true);
        assertTrue(blueprint.isInvulnerable());
    }

    @Test
    public void testFireTicks() {
        blueprint.setFireTicks(20);
        assertEquals(20, blueprint.getFireTicks());
    }

    @Test
    public void testSetDisplay() {
        when(location.getWorld()).thenReturn(mockWorld);
        when(location.clone()).thenReturn(location);
        when(mockWorld.spawn(any(Location.class), eq(Display.class))).thenReturn(display);
        blueprint.storeDisplay(display);
        blueprint.setDisplay(location);

        assertNotNull(blueprint.displayRec);
    }

}
