package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import static org.mockito.Mockito.*;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class PetTeleportListenerTest extends AbstractCommonSetup {

    private PetTeleportListener ptl;
    @Mock
    private Tameable tamed;
    @Mock
    private AnimalTamer tamer;

    /**
     * @throws java.lang.Exception
     */
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        when(tamed.isTamed()).thenReturn(true);
        when(tamed.getOwner()).thenReturn(tamer);
        when(tamer.getUniqueId()).thenReturn(uuid);
        ptl = (PetTeleportListener) Flags.PETS_STAY_AT_HOME.getListener().get();
        ptl.setPlugin(plugin);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportNotTameable() {
        EntityTeleportEvent e = new EntityTeleportEvent(player, location, location);
        ptl.onPetTeleport(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.worldsettings.PetTeleportListener#onPetTeleport(org.bukkit.event.entity.EntityTeleportEvent)}.
     */
    @Test
    public void testOnPetTeleportNullTo() {
        EntityTeleportEvent e = new EntityTeleportEvent(player, location, null);
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
