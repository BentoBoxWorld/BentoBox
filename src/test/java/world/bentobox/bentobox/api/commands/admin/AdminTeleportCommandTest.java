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
import java.util.Collections;
import java.util.HashMap;
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
import org.mockito.invocation.InvocationOnMock;
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
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class})
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


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
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
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.isPlayer()).thenReturn(true);
        User.setPlugin(plugin);

        // Parent command has no aliases
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ac.getTopLabel()).thenReturn("bskyblock");
        when(ac.getWorld()).thenReturn(world);

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
        when(im.isOwner(any(),any())).thenReturn(true);
        when(im.getOwner(any(),any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        when(user.getTranslation(Mockito.anyString(),Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0, String.class);
            }});

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
        // Util
        PowerMockito.mockStatic(Util.class, Mockito.RETURNS_MOCKS);
        when(Util.getUUID(anyString())).thenCallRealMethod();
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
        AdminTeleportCommand c = new AdminTeleportCommand(ac,"tp");
        assertEquals("tp",c.getLabel());
        c = new AdminTeleportCommand(ac,"tpnether");
        assertEquals("tpnether",c.getLabel());
        c = new AdminTeleportCommand(ac,"tpend");
        assertEquals("tpend",c.getLabel());
    }

    /**
     * Test no args
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgs() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.canExecute(user, "tp", new ArrayList<>()));
        verify(user).sendMessage(eq("commands.help.header"), eq(TextVariables.LABEL), eq(null));
    }

    @Test
    public void testExecuteUserStringListOfStringUnknownTarget() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.canExecute(user, "tp", Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq(TextVariables.NAME), eq("tastybento"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetNoIsland() {
        when(pm.getUUID(eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.canExecute(user, "tp", Collections.singletonList("tastybento")));
        verify(user).sendMessage(eq("general.errors.player-has-no-island"));
    }

    /**
     * Test for {@link AdminTeleportCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIsland() {
        when(pm.getUUID(eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", Collections.singletonList("tastybento")));
        assertTrue(atc.execute(user, "tp", Collections.singletonList("tastybento")));
        verify(user).getTranslation(eq("commands.admin.tp.manual"), eq("[location]"), eq("0 0 0"));
    }

    /**
     * Test for {@link AdminTeleportCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandSpawnPoint() {
        when(island.getSpawnPoint(any())).thenReturn(spawnPoint);
        when(pm.getUUID(eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", Collections.singletonList("tastybento")));
        assertTrue(atc.execute(user, "tp", Collections.singletonList("tastybento")));
        verify(user).getTranslation(eq("commands.admin.tp.manual"), eq("[location]"), eq("0 0 0"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetIsTeamMember() {
        when(pm.getUUID(eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.canExecute(user, "tp", Collections.singletonList("tastybento")));
        assertTrue(atc.execute(user, "tp", Collections.singletonList("tastybento")));
        verify(iwm, Mockito.never()).getNetherWorld(any());
        verify(iwm, Mockito.never()).getEndWorld(any());
        verify(user).getTranslation(eq("commands.admin.tp.manual"), eq("[location]"), eq("0 0 0"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandNether() {
        when(pm.getUUID(eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpnether");
        assertTrue(atc.canExecute(user, "tpnether", Collections.singletonList("tastybento")));
        assertTrue(atc.execute(user, "tpnether", Collections.singletonList("tastybento")));
        verify(iwm).getNetherWorld(any());
        verify(iwm, Mockito.never()).getEndWorld(any());
        verify(user).getTranslation(eq("commands.admin.tp.manual"), eq("[location]"), eq("0 0 0"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandEnd() {
        when(pm.getUUID(eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.canExecute(user, "tpend", Collections.singletonList("tastybento")));
        assertTrue(atc.execute(user, "tpend", Collections.singletonList("tastybento")));
        verify(iwm, Mockito.never()).getNetherWorld(any());
        verify(iwm).getEndWorld(any());
        verify(user).getTranslation(eq("commands.admin.tp.manual"), eq("[location]"), eq("0 0 0"));
    }





















}
