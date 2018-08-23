package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class DeathListenerTest {


    @Test
    public void testDeathListener() {
        assertNotNull(new DeathListener(mock(BentoBox.class)));
    }

    @Test
    public void testOnPlayerDeathEvent() {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(Mockito.any())).thenReturn(true);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock");
        when(iwm.getVisitorBannedCommands(Mockito.any())).thenReturn(new ArrayList<>());
        when(plugin.getIWM()).thenReturn(iwm);

        // Player
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(mock(Location.class));
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
       
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        
        // Test
        DeathListener dl = new DeathListener(plugin);
              
        PlayerDeathEvent e = new PlayerDeathEvent(player, new ArrayList<>(), 0, 0, 0, 0, "died");
        dl.onPlayerDeathEvent(e);
        Mockito.verify(pm).addDeath(world, uuid);
    }

}
