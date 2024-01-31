package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.managers.IslandWorldManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class LimitMobsListenerTest {

    @Mock
    private IslandWorldManager iwm;
    @Mock
    private @NonNull World world;
    private List<String> list = new ArrayList<>();
    private LimitMobsListener lml;
    @Mock
    private LivingEntity zombie;
    @Mock
    private LivingEntity skelly;
    @Mock
    private LivingEntity jockey;
    @Mock
    private Location location;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        when(plugin.getIWM()).thenReturn(iwm);
        list.add("SKELETON");
        when(iwm.getMobLimitSettings(world)).thenReturn(list);
        when(iwm.inWorld(world)).thenReturn(true);
        when(iwm.inWorld(location)).thenReturn(true);
        when(location.getWorld()).thenReturn(world);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        when(zombie.getLocation()).thenReturn(location);
        when(skelly.getType()).thenReturn(EntityType.SKELETON);
        when(skelly.getLocation()).thenReturn(location);
        when(jockey.getType()).thenReturn(EntityType.SPIDER);
        when(jockey.getLocation()).thenReturn(location);
        when(jockey.getPassengers()).thenReturn(List.of(skelly));

        lml = new LimitMobsListener();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.LimitMobsListener#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    public void testOnMobSpawn() {
        CreatureSpawnEvent e = new CreatureSpawnEvent(skelly, SpawnReason.NATURAL);
        lml.onMobSpawn(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.LimitMobsListener#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    public void testOnMobSpawnNotInWorld() {
        when(location.getWorld()).thenReturn(mock(World.class));
        CreatureSpawnEvent e = new CreatureSpawnEvent(skelly, SpawnReason.NATURAL);
        lml.onMobSpawn(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.LimitMobsListener#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    public void testOnMobSpawnOkayToSpawn() {
        CreatureSpawnEvent e = new CreatureSpawnEvent(zombie, SpawnReason.NATURAL);
        lml.onMobSpawn(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.LimitMobsListener#onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent)}.
     */
    @Test
    public void testOnMobSpawnJockey() {
        CreatureSpawnEvent e = new CreatureSpawnEvent(jockey, SpawnReason.JOCKEY);
        lml.onMobSpawn(e);
        assertTrue(e.isCancelled());
    }

}
