package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * @author tastybento
 *
 */
public class LimitMobsListenerTest extends CommonTestSetup {

   private List<String> list = new ArrayList<>();
    private LimitMobsListener lml;
    @Mock
    private LivingEntity zombie;
    @Mock
    private LivingEntity skelly;
    @Mock
    private LivingEntity jockey;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
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
    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
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
