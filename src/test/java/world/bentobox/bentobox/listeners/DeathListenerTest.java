package world.bentobox.bentobox.listeners;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.managers.PlayersManager;

public class DeathListenerTest extends CommonTestSetup {

    private PlayersManager pm;
    private WorldSettings worldSettings;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        when(iwm.getVisitorBannedCommands(any())).thenReturn(new ArrayList<>());

        pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        worldSettings = mock(WorldSettings.class);
        when(worldSettings.isDeathsCounted()).thenReturn(true);
        // Deaths counted
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(worldSettings );

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testOnPlayerDeathEventDeathsCounted() {
        // Test
        DeathListener dl = new DeathListener(plugin);

        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, new ArrayList<>(), 0, 0, 0, 0, "died");
        dl.onPlayerDeath(e);
        Mockito.verify(pm).addDeath(world, uuid);
    }

    @Test
    public void testOnPlayerDeathEventDeathsNotCounted() {
        when(worldSettings.isDeathsCounted()).thenReturn(false);
        // Test
        DeathListener dl = new DeathListener(plugin);

        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, new ArrayList<>(), 0, 0, 0, 0, "died");
        dl.onPlayerDeath(e);
        Mockito.verify(pm, Mockito.never()).addDeath(world, uuid);
    }

    @Test
    public void testOnPlayerDeathEventDeathsCountedNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        // Test
        DeathListener dl = new DeathListener(plugin);

        PlayerDeathEvent e = getPlayerDeathEvent(mockPlayer, new ArrayList<>(), 0, 0, 0, 0, "died");
        dl.onPlayerDeath(e);
        Mockito.verify(pm, Mockito.never()).addDeath(world, uuid);
    }


}
