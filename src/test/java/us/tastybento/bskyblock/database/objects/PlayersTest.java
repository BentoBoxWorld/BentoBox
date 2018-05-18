package us.tastybento.bskyblock.database.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class})
public class PlayersTest {
    
    private Settings s;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);
        s = mock(Settings.class);
        when(s.getResetLimit()).thenReturn(3);
        when(s.getDeathsMax()).thenReturn(3);
        when(plugin.getSettings()).thenReturn(s);
    }
    
    private BSkyBlock plugin;

    @Test
    public void testPlayersBSkyBlockUUID() {
        assertNotNull(new Players(plugin, UUID.randomUUID()));
    }

    @Test
    public void testSetHomeLocationLocation() {
        Players p = new Players(plugin, UUID.randomUUID());
        Location l = mock(Location.class);
        World w = mock(World.class);
        when(w.getName()).thenReturn("world");
        when(l.getWorld()).thenReturn(w);
        p.setHomeLocation(l, 5);
        assertEquals(l, p.getHomeLocation(w, 5));
        assertNotEquals(l, p.getHomeLocation(w, 0));
        p.clearHomeLocations(w);
        assertTrue(p.getHomeLocations(w).isEmpty());
    }

    @Test
    public void testDeaths() {
        Players p = new Players(plugin, UUID.randomUUID());
        assertTrue(p.getDeaths() == 0);
        p.addDeath();
        assertTrue(p.getDeaths() == 1);
        p.addDeath();
        assertTrue(p.getDeaths() == 2);
        p.addDeath();
        assertTrue(p.getDeaths() == 3);
        p.addDeath();
        assertTrue(p.getDeaths() == 3);
        p.addDeath();
        assertTrue(p.getDeaths() == 3);
        p.setDeaths(10);
        assertTrue(p.getDeaths() == 3);
        p.setDeaths(0);
        assertTrue(p.getDeaths() == 0);
    }

    @Test
    public void testInviteCoolDownTime() throws InterruptedException {
        when(s.getInviteWait()).thenReturn(1);
        Players p = new Players(plugin, UUID.randomUUID());
        // Check a null location
        assertTrue(p.getInviteCoolDownTime(null) == 0);
        // Real location
        Location l = mock(Location.class);
        // Should be no cooldown
        assertTrue(p.getInviteCoolDownTime(l) == 0);
        // Start the timer
        p.startInviteCoolDownTimer(l);        
        // More than 0 cooldown
        assertTrue(p.getInviteCoolDownTime(l) > 0);
    }
}
