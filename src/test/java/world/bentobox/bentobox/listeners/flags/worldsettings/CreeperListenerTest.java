package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, Flags.class, Util.class , ServerBuildInfo.class})
public class CreeperListenerTest extends AbstractCommonSetup {

    private CreeperListener cl;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        cl = new CreeperListener();
    }

    @After
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
    @Ignore("PaperAPI update required to address failure")
    @Test
    public void testOnExplosionCreeperInWorldDamageNOK() {
        Flags.CREEPER_GRIEFING.setSetting(world, false);
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
