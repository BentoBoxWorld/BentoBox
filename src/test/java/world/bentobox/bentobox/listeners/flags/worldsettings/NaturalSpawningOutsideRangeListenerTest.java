package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.lists.Flags;

class NaturalSpawningOutsideRangeListenerTest extends CommonTestSetup {

    private NaturalSpawningOutsideRangeListener listener;
    @Mock
    private LivingEntity entity;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getWorld()).thenReturn(world);

        // Default: flag is NOT set (spawning outside range is blocked)
        Flags.NATURAL_SPAWNING_OUTSIDE_RANGE.setDefaultSetting(false);

        listener = new NaturalSpawningOutsideRangeListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testNaturalSpawnOutsideIslandBlocked() {
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        CreatureSpawnEvent e = new CreatureSpawnEvent(entity, SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void testNaturalSpawnInsideIslandAllowed() {
        // Island exists at location
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.of(island));
        CreatureSpawnEvent e = new CreatureSpawnEvent(entity, SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testNonNaturalSpawnOutsideIslandAllowed() {
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        CreatureSpawnEvent e = new CreatureSpawnEvent(entity, SpawnReason.SPAWNER);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testNaturalSpawnOutsideIslandFlagAllowed() {
        Flags.NATURAL_SPAWNING_OUTSIDE_RANGE.setDefaultSetting(true);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        CreatureSpawnEvent e = new CreatureSpawnEvent(entity, SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testNaturalSpawnNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        when(im.getProtectedIslandAt(any())).thenReturn(Optional.empty());
        CreatureSpawnEvent e = new CreatureSpawnEvent(entity, SpawnReason.NATURAL);
        listener.onCreatureSpawn(e);
        assertFalse(e.isCancelled());
    }
}
