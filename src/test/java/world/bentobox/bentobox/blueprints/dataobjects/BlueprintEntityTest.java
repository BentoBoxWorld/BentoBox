package world.bentobox.bentobox.blueprints.dataobjects;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@Ignore("Cannot mock Villager Professions anynore")
public class BlueprintEntityTest {

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


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        when(villager.getProfession())
                .thenReturn(Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft("librarian")));
        when(villager.getVillagerExperience()).thenReturn(100);
        when(villager.getVillagerLevel()).thenReturn(2);
        when(villager.getVillagerType()).thenReturn(Villager.Type.PLAINS);
        when(sheep.getColor()).thenReturn(DyeColor.BLUE);
        when(wolf.isTamed()).thenReturn(true);
        when(chestedHorse.isCarryingChest()).thenReturn(true);
        when(horse.getDomestication()).thenReturn(50);
        when(horse.getStyle()).thenReturn(Horse.Style.WHITE_DOTS);

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        Mockito.framework().clearInlineMocks();
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

        Assert.assertTrue(wolf.isTamed());
    }

    @Test
    public void testConfigureEntityWithChestedHorse() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.HORSE);
        blueprint.setChest(true);

        blueprint.configureEntity(chestedHorse);

        Assert.assertTrue(chestedHorse.isCarryingChest());
    }

    @Test
    public void testConfigureEntityWithAgeable() {
        BlueprintEntity blueprint = new BlueprintEntity();

        blueprint.setType(EntityType.COW);
        blueprint.setAdult(false);

        blueprint.configureEntity(cow);

        Assert.assertFalse(cow.isAdult());
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
        Assert.assertTrue(blueprint.getTamed());

        blueprint.setChest(true);
        Assert.assertTrue(blueprint.getChest());

        blueprint.setAdult(false);
        Assert.assertFalse(blueprint.getAdult());

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

}
