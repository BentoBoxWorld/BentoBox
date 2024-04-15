package world.bentobox.bentobox.hooks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MobExecutor;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class, MythicBukkit.class })
public class MythicMobsHookTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private PluginManager pim;
    @Mock
    private Plugin mythicMobs;
    @Mock
    private Location location;
    @Mock
    private World world;
    // DUT
    MythicMobsHook hook;
    @Mock
    private MythicBukkit mythicBukkit;
    @Mock
    private MobExecutor mm;
    @Mock
    private MythicMob mythicMob;
    @Mock
    private ActiveMob activeMob;
    @Mock
    private Entity entity;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(pim.getPlugin("MythicMobs")).thenReturn(mythicMobs);
        // Location
        when(world.getName()).thenReturn("bskyblock");
        when(location.getWorld()).thenReturn(world);
        // Entity
        when(entity.getUniqueId()).thenReturn(UUID.randomUUID());
        // MythicMobs
        PowerMockito.mockStatic(MythicBukkit.class, Mockito.RETURNS_MOCKS);
        when(MythicBukkit.inst()).thenReturn(mythicBukkit);
        when(mythicBukkit.getMobManager()).thenReturn(mm);
        when(mm.getMythicMob(anyString())).thenReturn(Optional.of(mythicMob));
        when(activeMob.getDisplayName()).thenReturn("Minion");
        when(activeMob.getMobType()).thenReturn("GIANT");
        when(activeMob.getStance()).thenReturn("default");
        when(activeMob.getLevel()).thenReturn(2.5D);
        when(activeMob.getPower()).thenReturn(33.2F);
        when(mm.getActiveMob(any())).thenReturn(Optional.of(activeMob));

        hook = new MythicMobsHook();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.MythicMobsHook#hook()}.
     */
    @Test
    public void testHook() {
        assertTrue(hook.hook());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.MythicMobsHook#getFailureCause()}.
     */
    @Test
    public void testGetFailureCause() {
        assertNull(hook.getFailureCause());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.MythicMobsHook#MythicMobsHook()}.
     */
    @Test
    public void testMythicMobsHook() {
        assertNotNull(hook);
        assertEquals(Material.CREEPER_HEAD, hook.getIcon());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.MythicMobsHook#isMythicMob(org.bukkit.entity.Entity)}.
     */
    @Test
    public void testIsMythicMob() {
        assertFalse(hook.isMythicMob(entity));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.MythicMobsHook#getMythicMob(org.bukkit.entity.Entity)}.
     */
    @Test
    public void testGetMythicMob() {
        MythicMobRecord mmr = hook.getMythicMob(entity);
        assertEquals("GIANT", mmr.type());
        assertEquals("Minion", mmr.displayName());
        assertEquals("default", mmr.stance());
        assertEquals(2.5D, mmr.level(), 0D);
        assertEquals(33.2F, mmr.power(), 0F);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.MythicMobsHook#spawnMythicMob(world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord, org.bukkit.Location)}.
     */
    @Test
    public void testSpawnMythicMobNoPLugin() {
        MythicMobRecord mmr = hook.getMythicMob(entity);
        assertFalse(hook.spawnMythicMob(mmr, location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.hooks.MythicMobsHook#spawnMythicMob(world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord, org.bukkit.Location)}.
     */
    @Test
    public void testSpawnMythicMobHasPlugin() {
        when(mythicMobs.isEnabled()).thenReturn(true);
        MythicMobRecord mmr = hook.getMythicMob(entity);
        assertTrue(hook.spawnMythicMob(mmr, location));
        verify(mm).getMythicMob("GIANT");
    }

}
