package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.listeners.flags.protection.TestWorldSettings;
import world.bentobox.bentobox.lists.Flags;

public class CreeperListenerTest extends CommonTestSetup {

    private CreeperListener cl;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        WorldSettings worldSet = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSet);

        cl = new CreeperListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionNotCreeper() {
        List<Block> list = new ArrayList<>();
        Entity entity = mock(Entity.class);
        when(entity.getType()).thenReturn(EntityType.TNT);
        when(iwm.inWorld(location)).thenReturn(true);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0, null);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionNotInWorld() {
        List<Block> list = new ArrayList<>();
        Entity entity = mock(Entity.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.CREEPER);
        when(iwm.inWorld(location)).thenReturn(false);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0, null);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionCreeperInWorldDamageOK() {
        List<Block> list = new ArrayList<>();
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        Creeper entity = mock(Creeper.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.CREEPER);
        when(iwm.inWorld(location)).thenReturn(true);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0, null);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
        assertFalse(event.blockList().isEmpty()); // No clearing of block list
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onExplosion(org.bukkit.event.entity.EntityExplodeEvent)}.
     */
    @Test
    public void testOnExplosionCreeperInWorldDamageNOK() {
        Flags.CREEPER_DAMAGE.setSetting(world, false);
        List<Block> list = new ArrayList<>();
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        list.add(mock(Block.class));
        Creeper entity = mock(Creeper.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.CREEPER);
        when(iwm.inWorld(location)).thenReturn(true);
        EntityExplodeEvent event = new EntityExplodeEvent(entity, location, list, 0, null);
        cl.onExplosion(event);
        assertFalse(event.isCancelled());
        assertTrue(event.blockList().isEmpty()); // No clearing of block list
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityCancelled() {
        Flags.CREEPER_GRIEFING.setSetting(world, false);
        Creeper creeper = mock(Creeper.class);
        when(location.getWorld()).thenReturn(world);
        when(creeper.getLocation()).thenReturn(location);
        when(mockPlayer.getInventory()).thenReturn(inv);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.FLINT_AND_STEEL);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(iwm.inWorld(location)).thenReturn(true);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, creeper, EquipmentSlot.OFF_HAND);
        cl.onPlayerInteractEntity(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityAllowed() {
        Flags.CREEPER_GRIEFING.setSetting(world, true);
        Creeper creeper = mock(Creeper.class);
        when(location.getWorld()).thenReturn(world);
        when(creeper.getLocation()).thenReturn(location);
        when(mockPlayer.getInventory()).thenReturn(inv);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.FLINT_AND_STEEL);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(iwm.inWorld(location)).thenReturn(true);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, creeper, EquipmentSlot.OFF_HAND);
        cl.onPlayerInteractEntity(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityNotCreeper() {
        Flags.CREEPER_GRIEFING.setSetting(world, false);
        when(mockPlayer.getInventory()).thenReturn(inv);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.FLINT_AND_STEEL);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(iwm.inWorld(location)).thenReturn(true);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, mockPlayer, EquipmentSlot.OFF_HAND);
        cl.onPlayerInteractEntity(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityOnIsland() {
        Flags.CREEPER_GRIEFING.setSetting(world, false);
        Creeper creeper = mock(Creeper.class);
        when(location.getWorld()).thenReturn(world);
        when(creeper.getLocation()).thenReturn(location);
        when(mockPlayer.getInventory()).thenReturn(inv);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.FLINT_AND_STEEL);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(iwm.inWorld(location)).thenReturn(true);
        when(im.locationIsOnIsland(mockPlayer, location)).thenReturn(true);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, creeper, EquipmentSlot.OFF_HAND);
        cl.onPlayerInteractEntity(e);
        assertFalse(e.isCancelled());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityWrongWorld() {
        Flags.CREEPER_GRIEFING.setSetting(world, false);
        Creeper creeper = mock(Creeper.class);
        when(location.getWorld()).thenReturn(world);
        when(creeper.getLocation()).thenReturn(location);
        when(mockPlayer.getInventory()).thenReturn(inv);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.FLINT_AND_STEEL);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(iwm.inWorld(location)).thenReturn(false);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, creeper, EquipmentSlot.OFF_HAND);
        cl.onPlayerInteractEntity(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.CreeperListener#onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractEntityNothingInHand() {
        Flags.CREEPER_GRIEFING.setSetting(world, false);
        Creeper creeper = mock(Creeper.class);
        when(location.getWorld()).thenReturn(world);
        when(creeper.getLocation()).thenReturn(location);
        when(mockPlayer.getInventory()).thenReturn(inv);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.AIR);
        when(inv.getItemInMainHand()).thenReturn(item);
        when(iwm.inWorld(location)).thenReturn(true);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, creeper, EquipmentSlot.OFF_HAND);
        cl.onPlayerInteractEntity(e);
        assertFalse(e.isCancelled());
    }

}
