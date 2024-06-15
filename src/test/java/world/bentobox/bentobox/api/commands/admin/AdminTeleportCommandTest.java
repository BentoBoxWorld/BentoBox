package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class })
public class AdminTeleportCommandTest {

    @Mock
    private CompositeCommand ac;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    private UUID notUUID;
    @Mock
    private Player p;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Island island;
    @Mock
    private Location spawnPoint;
    @Mock
    private World world;
    @Mock
    private World netherWorld;
    @Mock
    private World endWorld;
    @Mock
    private PlaceholdersManager phm;

    /**
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        Util.setPlugin(plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while (notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.hasPermission("admin.tp")).thenReturn(true);
        when(p.hasPermission("admin")).thenReturn(false);

        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.isPlayer()).thenReturn(true);
        when(user.hasPermission("admin.tp")).thenReturn(true);
        when(user.hasPermission("admin")).thenReturn(false);

        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");
        when(ac.getLabel()).thenReturn("bskyblock");
        when(ac.getWorld()).thenReturn(world);
        when(ac.getPermission()).thenReturn("admin");

        // World
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(netherWorld.getEnvironment()).thenReturn(Environment.NETHER);
        when(endWorld.getEnvironment()).thenReturn(Environment.THE_END);

        // Island World Manager
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getNetherWorld(any())).thenReturn(netherWorld);
        when(iwm.getEndWorld(any())).thenReturn(endWorld);

        // Player has island to begin with
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        // when(im.isOwner(any(),any())).thenReturn(true);
        // when(im.getOwner(any(),any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Island location
        Location location = mock(Location.class);
        Vector vector = mock(Vector.class);
        when(vector.toLocation(any())).thenReturn(location);
        when(location.toVector()).thenReturn(vector);
        when(location.getWorld()).thenReturn(world);
        when(spawnPoint.getWorld()).thenReturn(world);
        when(world.getMaxHeight()).thenReturn(255);
        when(im.getIslandLocation(any(), any())).thenReturn(location);
        // We do no actually want to teleport in this test, so return no island
        Optional<Island> nothing = Optional.empty();
        when(im.getIslandAt(any())).thenReturn(nothing);

        // Return an island for spawn checking
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        when(island.getCenter()).thenReturn(location);
        when(location.clone()).thenReturn(location);
        when(location.toVector()).thenReturn(new Vector(0, 0, 0));
        when(island.getProtectionCenter()).thenReturn(location);
        // Util
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getUUID(anyString())).thenCallRealMethod();

        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test all the various commands
     */
    @Test
    public void testExecuteUserStringListOfString() {
        AdminTeleportCommand c = new AdminTeleportCommand(ac, "tp");
        assertEquals("tp", c.getLabel());
        c = new AdminTeleportCommand(ac, "tpnether");
        assertEquals("tpnether", c.getLabel());
        c = new AdminTeleportCommand(ac, "tpend");
        assertEquals("tpend", c.getLabel());
    }

    /**
     * Test no args
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgs() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac, "tp");
        assertFalse(atc.canExecute(user, "tp", new ArrayList<>()));
        verify(user).sendMessage(eq("commands.help.header"), eq(TextVariables.LABEL), eq(null));
    }

    @Test
    public void testExecuteUserStringListOfStringUnknownTarget() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac, "tp");
        assertFalse(atc.canExecute(user, "tp", List.of("tastybento")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq(TextVariables.NAME), eq("tastybento"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetNoIsland() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.canExecute(user, "tp", List.of("tastybento")));
        verify(user).sendMessage(eq("general.errors.player-has-no-island"));
    }

    /**
     * Test for {@link AdminTeleportCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIsland() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", List.of("tastybento")));
        assertTrue(atc.execute(user, "tp", List.of("tastybento")));
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    /**
     * Test for {@link AdminTeleportCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandSpawnPoint() {
        when(island.getSpawnPoint(any())).thenReturn(spawnPoint);
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", List.of("tastybento")));
        assertTrue(atc.execute(user, "tp", List.of("tastybento")));
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetIsTeamMember() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", List.of("tastybento")));
        assertTrue(atc.execute(user, "tp", List.of("tastybento")));
        verify(iwm, Mockito.never()).getNetherWorld(any());
        verify(iwm, Mockito.never()).getEndWorld(any());
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandNether() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpnether");
        assertTrue(atc.canExecute(user, "tpnether", List.of("tastybento")));
        assertTrue(atc.execute(user, "tpnether", List.of("tastybento")));
        verify(iwm).getNetherWorld(any());
        verify(iwm, Mockito.never()).getEndWorld(any());
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandEnd() {
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.canExecute(user, "tpend", List.of("tastybento")));
        assertTrue(atc.execute(user, "tpend", List.of("tastybento")));
        verify(iwm, Mockito.never()).getNetherWorld(any());
        verify(iwm).getEndWorld(any());
        verify(user).getTranslation("commands.admin.tp.manual", "[location]", "0 0 0");
    }

    @Test
    public void testPermissionsNoRootPermission() {
        when(p.hasPermission("admin.tp")).thenReturn(true);
        when(p.hasPermission("admin")).thenReturn(false);
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.canExecute(user, "tpend", List.of("tastybento")));
        String[] list = new String[2];
        list[0] = "tpend";
        list[1] = "tastybento";
        // Should fail
        assertFalse(atc.execute(p, "tpend", list));
    }

    @Test
    public void testPermissionsHasRootPermission() {
        when(p.hasPermission("admin.tp")).thenReturn(true);
        when(p.hasPermission("admin")).thenReturn(true);
        when(pm.getUUID("tastybento")).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.canExecute(user, "tpend", List.of("tastybento")));
        String[] list = new String[2];
        list[0] = "tpend";
        list[1] = "tastybento";
        // Should pass
        assertTrue(atc.execute(p, "tpend", list));
        verify(p).hasPermission("admin.tp");
        verify(p).hasPermission("admin");
    }

}
