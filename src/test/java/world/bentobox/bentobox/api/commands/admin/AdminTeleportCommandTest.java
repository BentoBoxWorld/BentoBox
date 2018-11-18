package world.bentobox.bentobox.api.commands.admin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class})
public class AdminTeleportCommandTest {

    private CompositeCommand ac;
    private User user;
    private IslandsManager im;
    private PlayersManager pm;
    private UUID notUUID;
    private Player p;
    private IslandWorldManager iwm;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Player
        p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        UUID uuid = UUID.randomUUID();
        notUUID = UUID.randomUUID();
        while(notUUID.equals(uuid)) {
            notUUID = UUID.randomUUID();
        }
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        User.setPlugin(plugin);

        // Parent command has no aliases
        ac = mock(CompositeCommand.class);
        when(ac.getSubCommandAliases()).thenReturn(new HashMap<>());

        // Island World Manager
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);


        // Player has island to begin with
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        when(im.hasIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(true);
        when(im.isOwner(Mockito.any(),Mockito.any())).thenReturn(true);
        when(im.getOwner(Mockito.any(),Mockito.any())).thenReturn(uuid);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);

        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenReturn("mock translation");
        when(plugin.getLocalesManager()).thenReturn(lm);

        when(user.getTranslation(Mockito.anyString(),Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgumentAt(0, String.class);
            }});

        // Island location
        Location location = mock(Location.class);
        Vector vector = mock(Vector.class);
        when(vector.toLocation(Mockito.any())).thenReturn(location);
        when(location.toVector()).thenReturn(vector);
        when(im.getIslandLocation(Mockito.any(), Mockito.any())).thenReturn(location);
        // We do no actually want to teleport in this test, so return no island
        Optional<Island> nothing = Optional.empty();
        when(im.getIslandAt(Mockito.any())).thenReturn(nothing );
    }


    /**
     * Test all the various commands
     */
    @Test
    public void testExecuteUserStringListOfString() {
        new AdminTeleportCommand(ac,"tp");
        new AdminTeleportCommand(ac,"tpnether");
        new AdminTeleportCommand(ac,"tpend");
    }

    /**
     * Test no args
     */
    @Test
    public void testExecuteUserStringListOfStringEmptyArgs() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.execute(user, "tp", new ArrayList<>()));
        Mockito.verify(user).sendMessage(Mockito.anyString());
    }

    @Test
    public void testExecuteUserStringListOfStringUnknownTarget() {
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.execute(user, "tp", Collections.singletonList("tastybento")));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.unknown-player"), Mockito.eq(TextVariables.NAME), Mockito.eq("tastybento"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetNoIsland() {
        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertFalse(atc.execute(user, "tp", Collections.singletonList("tastybento")));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.player-has-no-island"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIsland() {
        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.execute(user, "tp", Collections.singletonList("tastybento")));
        Mockito.verify(user).getTranslation(Mockito.eq("commands.admin.tp.manual"), Mockito.eq("[location]"), Mockito.eq("0 0 0"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetIsTeamMember() {
        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(false);
        when(im.inTeam(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tp");
        assertTrue(atc.execute(user, "tp", Collections.singletonList("tastybento")));
        Mockito.verify(iwm, Mockito.never()).getNetherWorld(Mockito.any());
        Mockito.verify(iwm, Mockito.never()).getEndWorld(Mockito.any());
        Mockito.verify(user).getTranslation(Mockito.eq("commands.admin.tp.manual"), Mockito.eq("[location]"), Mockito.eq("0 0 0"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandNether() {
        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpnether");
        assertTrue(atc.execute(user, "tpnether", Collections.singletonList("tastybento")));
        Mockito.verify(iwm).getNetherWorld(Mockito.any());
        Mockito.verify(iwm, Mockito.never()).getEndWorld(Mockito.any());
        Mockito.verify(user).getTranslation(Mockito.eq("commands.admin.tp.manual"), Mockito.eq("[location]"), Mockito.eq("0 0 0"));
    }

    @Test
    public void testExecuteUserStringListOfStringKnownTargetHasIslandEnd() {
        when(pm.getUUID(Mockito.eq("tastybento"))).thenReturn(notUUID);
        when(im.hasIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(true);
        AdminTeleportCommand atc = new AdminTeleportCommand(ac,"tpend");
        assertTrue(atc.execute(user, "tpend", Collections.singletonList("tastybento")));
        Mockito.verify(iwm, Mockito.never()).getNetherWorld(Mockito.any());
        Mockito.verify(iwm).getEndWorld(Mockito.any());
        Mockito.verify(user).getTranslation(Mockito.eq("commands.admin.tp.manual"), Mockito.eq("[location]"), Mockito.eq("0 0 0"));
    }





















}
