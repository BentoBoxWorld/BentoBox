package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 *
 */
public class BreedingListenerTest extends CommonTestSetup {

    private ItemStack itemInMainHand;
    private ItemStack itemInOffHand;

    private static final EntityType ENTITY_TYPE = EntityType.COW;
    private static final Material BREEDABLE_WITH = Material.WHEAT;
    private static final Material NOT_BREEDABLE_WITH = Material.SEAGRASS;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        // Monsters and animals
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        Cow cow = mock(Cow.class);
        when(cow.getLocation()).thenReturn(location);

        itemInMainHand = mock(ItemStack.class);
        when(itemInMainHand.getType()).thenReturn(Material.AIR);
        itemInOffHand = mock(ItemStack.class);
        when(itemInOffHand.getType()).thenReturn(Material.AIR);
        when(inv.getItemInMainHand()).thenReturn(itemInMainHand);
        when(inv.getItemInOffHand()).thenReturn(itemInOffHand);
        when(mockPlayer.getInventory()).thenReturn(inv);

    }
    
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotAnimal() {
        Entity clickedEntity = mock(Entity.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position);
        new BreedingListener().onPlayerInteract(e);
        assertFalse(e.isCancelled(), "Not animal failed");
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalNothingInMainHand() {
        Animals clickedEntity = mock(Animals.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position);
        new BreedingListener().onPlayerInteract(e);
        assertFalse(e.isCancelled(), "Animal, nothing in main hand failed");
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalNothingInOffHand() {
        Animals clickedEntity = mock(Animals.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position, EquipmentSlot.OFF_HAND);
        new BreedingListener().onPlayerInteract(e);
        assertFalse(e.isCancelled(), "Animal, nothing in off hand failed");
    }


    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInMainHandNotRightWorld() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(ENTITY_TYPE);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse(e.isCancelled(), "Animal, breeding item in main hand, wrong world failed " + breedingMat);

        // verify breeding was prevented
        verify(clickedEntity, never()).setBreed(false);
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInMainHand() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.COW);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertTrue(e.isCancelled(), "Animal, breeding item in main hand failed " + breedingMat);

        // verify breeding was prevented
        verify(clickedEntity).setBreed(false);
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInOffHandNotRightWorld() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position, EquipmentSlot.OFF_HAND);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInOffHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse(e.isCancelled(), "Animal, breeding item in off hand, wrong world failed " + breedingMat);

        // verify breeding was not prevented
        verify(clickedEntity, never()).setBreed(false);
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInOffHand() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(ENTITY_TYPE);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position, EquipmentSlot.OFF_HAND);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;
        when(itemInOffHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertTrue(e.isCancelled(), "Animal, breeding item in off hand failed " + breedingMat);

        // verify breeding was prevented
        verify(clickedEntity).setBreed(false);
    }

    @Test
    public void testOnPlayerIntereactAnimalBreedingWrongFood() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.COW);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(mockPlayer, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = NOT_BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse(e.isCancelled(), "Animal, breeding item in main hand was prevented " + breedingMat);

        // verify breeding was not prevented
        verify(clickedEntity, never()).setBreed(false);
    }
}
