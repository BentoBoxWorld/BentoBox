package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * @author tastybento
 *
 */
public class PetTeleportListenerTest extends CommonTestSetup {

    private PetTeleportListener ptl;
    @Mock
    private Tameable tamed;
    @Mock
    private AnimalTamer tamer;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Island
        when(this.island.inTeam(uuid)).thenReturn(true);
        when(tamed.isTamed()).thenReturn(true);
        when(tamed.getOwner()).thenReturn(tamer);
        when(tamer.getUniqueId()).thenReturn(uuid);
        ptl = (PetTeleportListener) Flags.PETS_STAY_AT_HOME.getListener().get();
        ptl.setPlugin(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportNotTameable() {
        EntityTeleportEvent e = new EntityTeleportEvent(mockPlayer, location, location);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportNullTo() {
        EntityTeleportEvent e = new EntityTeleportEvent(mockPlayer, location, null);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportWrongWorld() {
        when(iwm.inWorld(location)).thenReturn(false);
        EntityTeleportEvent e = new EntityTeleportEvent(tamed, location, location);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportFlagNotSet() {
        Flags.PETS_STAY_AT_HOME.setSetting(world, false);
        EntityTeleportEvent e = new EntityTeleportEvent(tamed, location, location);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportFlagSetGoingHome() {
        EntityTeleportEvent e = new EntityTeleportEvent(tamed, location, location);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportFlagSetNoIsland() {
        Location l = mock(Location.class);
        when(im.getProtectedIslandAt(l)).thenReturn(Optional.empty());
        EntityTeleportEvent e = new EntityTeleportEvent(tamed, location, l);
        ptl.onPetTeleport(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportFlagSetNotHome() {
        Location l = mock(Location.class);
        Island otherIsland = mock(Island.class);
        when(otherIsland.getMemberSet()).thenReturn(ImmutableSet.of());
        when(im.getProtectedIslandAt(l)).thenReturn(Optional.of(otherIsland ));
        EntityTeleportEvent e = new EntityTeleportEvent(tamed, location, l);
        ptl.onPetTeleport(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportFlagSetTamedButNoOwner() {
        when(tamed.getOwner()).thenReturn(null);
        EntityTeleportEvent e = new EntityTeleportEvent(tamed, location, location);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportFlagSetNotTamed() {
        when(tamed.isTamed()).thenReturn(false);
        EntityTeleportEvent e = new EntityTeleportEvent(tamed, location, location);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

}
