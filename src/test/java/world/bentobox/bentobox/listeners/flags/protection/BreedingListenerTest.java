package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class, BentoBox.class, Flags.class, Util.class} )
public class BreedingListenerTest extends AbstractCommonSetup {

    private ItemStack itemInMainHand;
    private ItemStack itemInOffHand;

    private static final EntityType ENTITY_TYPE = EntityType.COW;
    private static final Material BREEDABLE_WITH = Material.WHEAT;
    private static final Material NOT_BREEDABLE_WITH = Material.SEAGRASS;

    @Override
    @Before
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
        when(player.getInventory()).thenReturn(inv);

    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotAnimal() {
        Entity clickedEntity = mock(Entity.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        new BreedingListener().onPlayerInteract(e);
        assertFalse("Not animal failed", e.isCancelled());
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalNothingInMainHand() {
        Animals clickedEntity = mock(Animals.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        new BreedingListener().onPlayerInteract(e);
        assertFalse("Animal, nothing in main hand failed", e.isCancelled());
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalNothingInOffHand() {
        Animals clickedEntity = mock(Animals.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, EquipmentSlot.OFF_HAND);
        new BreedingListener().onPlayerInteract(e);
        assertFalse("Animal, nothing in off hand failed", e.isCancelled());
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
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse("Animal, breeding item in main hand, wrong world failed " + breedingMat, e.isCancelled());

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
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertTrue("Animal, breeding item in main hand failed " + breedingMat, e.isCancelled());

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
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, EquipmentSlot.OFF_HAND);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInOffHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse("Animal, breeding item in off hand, wrong world failed " + breedingMat, e.isCancelled());

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
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, EquipmentSlot.OFF_HAND);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;
        when(itemInOffHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertTrue("Animal, breeding item in off hand failed " + breedingMat, e.isCancelled());

        // verify breeding was prevented
        verify(clickedEntity).setBreed(false);
    }

    @Test
    public void testOnPlayerIntereactAnimalBreedingWrongFood() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.COW);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = NOT_BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse("Animal, breeding item in main hand was prevented " + breedingMat, e.isCancelled());

        // verify breeding was not prevented
        verify(clickedEntity, never()).setBreed(false);
    }
}
