package world.bentobox.bentobox.listeners;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class DeathListenerTest {

    private Player player;
    private BentoBox plugin;
    private PlayersManager pm;
    private WorldSettings worldSettings;
    private World world;
    private UUID uuid;
    private IslandWorldManager iwm;

    @Before
    public void setUp() {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock");
        when(iwm.getVisitorBannedCommands(Mockito.any())).thenReturn(new ArrayList<>());
        when(plugin.getIWM()).thenReturn(iwm);

        // Player
        player = mock(Player.class);
        world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(mock(Location.class));
        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        worldSettings = mock(WorldSettings.class);
        when(worldSettings.isDeathsCounted()).thenReturn(true);
        // Deaths counted
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(worldSettings );

    }

    @Test
    public void testOnPlayerDeathEventDeathsCounted() {
        // Test
        DeathListener dl = new DeathListener(plugin);

        PlayerDeathEvent e = new PlayerDeathEvent(player, new ArrayList<>(), 0, 0, 0, 0, "died");
        dl.onPlayerDeath(e);
        Mockito.verify(pm).addDeath(world, uuid);
    }

    @Test
    public void testOnPlayerDeathEventDeathsNotCounted() {
        when(worldSettings.isDeathsCounted()).thenReturn(false);
        // Test
        DeathListener dl = new DeathListener(plugin);

        PlayerDeathEvent e = new PlayerDeathEvent(player, new ArrayList<>(), 0, 0, 0, 0, "died");
        dl.onPlayerDeath(e);
        Mockito.verify(pm, Mockito.never()).addDeath(world, uuid);
    }

    @Test
    public void testOnPlayerDeathEventDeathsCountedNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        // Test
        DeathListener dl = new DeathListener(plugin);

        PlayerDeathEvent e = new PlayerDeathEvent(player, new ArrayList<>(), 0, 0, 0, 0, "died");
        dl.onPlayerDeath(e);
        Mockito.verify(pm, Mockito.never()).addDeath(world, uuid);
    }


}
