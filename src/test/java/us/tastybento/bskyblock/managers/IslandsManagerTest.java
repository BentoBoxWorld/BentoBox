package us.tastybento.bskyblock.managers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.material.TrapDoor;
import org.bukkit.plugin.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { BSkyBlock.class })
public class IslandsManagerTest {

    @Mock
    static BSkyBlock plugin = mock(BSkyBlock.class);
    private static World world;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Server server = mock(Server.class);
        world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        Bukkit.setServer(server);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());

    }

    @Test
    public void testIsSafeLocation() {
        Settings settings = mock(Settings.class);

        when(plugin.getSettings()).thenReturn(settings);


        IslandsManager manager = new IslandsManager(plugin);

        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        Block space1 = mock(Block.class);
        Block ground = mock(Block.class);
        Block space2 = mock(Block.class);

        when(location.getBlock()).thenReturn(space1);

        when(ground.getType()).thenReturn(Material.GRASS);
        when(space1.getType()).thenReturn(Material.AIR);
        when(space2.getType()).thenReturn(Material.AIR);
        when(space1.getRelative(BlockFace.DOWN)).thenReturn(ground);
        when(space1.getRelative(BlockFace.UP)).thenReturn(space2);

        BlockState blockState = mock(BlockState.class);
        when(ground.getState()).thenReturn(blockState);

        // Closed trapdoor
        TrapDoor trapDoor = mock(TrapDoor.class);
        when(trapDoor.isOpen()).thenReturn(false);
        when(blockState.getData()).thenReturn(trapDoor);

        // Happy path
        assertTrue(manager.isSafeLocation(location));

        // Try all different types of ground
        for (Material m : Material.values()) {
            when(ground.getType()).thenReturn(m);
            if (m.equals(Material.AIR) 
                    || !m.isSolid() 
                    || ground.getType().equals(Material.CACTUS) 
                    || ground.getType().equals(Material.BOAT) 
                    || ground.getType().equals(Material.FENCE)
                    || ground.getType().equals(Material.NETHER_FENCE) 
                    || ground.getType().equals(Material.SIGN_POST) 
                    || ground.getType().equals(Material.WALL_SIGN)) {
                assertFalse("Materials : " + m , manager.isSafeLocation(location));
            } else {
                assertTrue("Materials : " + m , manager.isSafeLocation(location));
            }     
        }
        // Put ground back to GRASS
        when(ground.getType()).thenReturn(Material.GRASS);
        // Try all different types of lock around feet
        for (Material m : Material.values()) {
            when(space1.getType()).thenReturn(m);
            if (m.isSolid()) {
                if (m.equals(Material.SIGN_POST) || m.equals(Material.WALL_SIGN)) {
                    assertTrue("Materials : " + m , manager.isSafeLocation(location));
                } else {
                    assertFalse("Materials : " + m , manager.isSafeLocation(location));
                }
            } else {
                if (m.equals(Material.LAVA) || m.equals(Material.STATIONARY_LAVA) || m.equals(Material.PORTAL) || m.equals(Material.ENDER_PORTAL)) {
                    assertFalse("Materials : " + m , manager.isSafeLocation(location));
                } else {
                    assertTrue("Materials : " + m , manager.isSafeLocation(location));
                }
            }     
        }
        when(space1.getType()).thenReturn(Material.AIR);
        // Try all different types of lock around feet
        for (Material m : Material.values()) {
            when(space2.getType()).thenReturn(m);
            if (m.isSolid()) {
                if (m.equals(Material.SIGN_POST) || m.equals(Material.WALL_SIGN)) {
                    assertTrue("Materials : " + m , manager.isSafeLocation(location));
                } else {
                    assertFalse("Materials : " + m , manager.isSafeLocation(location));
                }
            } else {
                if (m.equals(Material.LAVA) || m.equals(Material.STATIONARY_LAVA) || m.equals(Material.PORTAL) || m.equals(Material.ENDER_PORTAL)) {
                    assertFalse("Materials : " + m , manager.isSafeLocation(location));
                } else {
                    assertTrue("Materials : " + m , manager.isSafeLocation(location));
                }
            }     
        }
        
        // In liquid
        when(settings.getAcidDamage()).thenReturn(0);
        when(ground.getType()).thenReturn(Material.GRASS);
        when(space1.getType()).thenReturn(Material.STATIONARY_WATER);
        when(space2.getType()).thenReturn(Material.STATIONARY_WATER);
        when(space1.isLiquid()).thenReturn(true);
        when(space2.isLiquid()).thenReturn(true);
        assertFalse("Submerged", manager.isSafeLocation(location));
        // In acid
        when(settings.getAcidDamage()).thenReturn(10);
        assertFalse("Submerged", manager.isSafeLocation(location));

        when(settings.getAcidDamage()).thenReturn(0);
        when(space2.getType()).thenReturn(Material.AIR);
        when(space2.isLiquid()).thenReturn(false);
        assertTrue("In up to waist", manager.isSafeLocation(location));
        when(settings.getAcidDamage()).thenReturn(10);
        assertFalse("In acid", manager.isSafeLocation(location));
        when(settings.getAcidDamage()).thenReturn(0);
    }

}
