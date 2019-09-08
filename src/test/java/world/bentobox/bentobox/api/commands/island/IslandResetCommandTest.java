package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.island.NewIsland;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, NewIsland.class })
public class IslandResetCommandTest {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Settings s;
    @Mock
    private IslandsManager im;
    @Mock
    private PlayersManager pm;
    @Mock
    private World world;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private @Nullable Island island;
    @Mock
    private PluginManager pim;

    private IslandResetCommand irc;
    private UUID uuid;

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

        // Settings
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // User
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        // World
        when(ic.getWorld()).thenReturn(world);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        // Event
        when(Bukkit.getPluginManager()).thenReturn(pim);


        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        when(bpm.validate(any(), any())).thenReturn("custom");

        // Give the user some resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(3);

        // Island team members
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(uuid);
        when(island.getMemberSet()).thenReturn(members.build());


        // The command
        irc = new IslandResetCommand(ic);
    }


    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testNoIsland() {
        // Test the reset command
        // Does not have island
        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testNoResetsLeft() {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);

        // Block based on no resets left
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(0);

        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("commands.island.reset.none-left");
        // Verify event
        verify(pim, never()).callEvent(any(IslandBaseEvent.class));
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Ignore("NPE")
    @Test
    public void testNoConfirmationRequired() throws IOException {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);

        // Reset command, no confirmation required
        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // TODO Verify that panel was shown
        // verify(bpm).showPanel(any(), eq(user), eq(irc.getLabel()));
        // Verify event
        verify(pim).callEvent(any(IslandBaseEvent.class));
        // Verify messaging
        verify(user).sendMessage("commands.island.create.creating-island");
    }

    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testUnlimitedResets() throws IOException {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);
        // Test with unlimited resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(-1);

        // Reset
        assertTrue(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
    }

    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testNoPaste() throws IOException {
        irc = new IslandResetCommand(ic, true);
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);
        // Test with unlimited resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(-1);

        // Reset
        assertTrue(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(builder, never()).noPaste();
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testConfirmationRequired() throws IOException {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(1);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);

        // Require confirmation
        when(s.isResetConfirmation()).thenReturn(true);
        when(s.getConfirmationTime()).thenReturn(20);

        // Reset
        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // Check for message
        verify(user).sendMessage("commands.confirmation.confirm", "[seconds]", String.valueOf(s.getConfirmationTime()));

        // Send command again to confirm
        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // Some more checking can go here...
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testNoConfirmationRequiredUnknownBlueprint() throws IOException {
        // No such bundle
        when(bpm.validate(any(), any())).thenReturn(null);
        // Reset command, no confirmation required
        assertFalse(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
        verify(user).sendMessage(
                "commands.island.create.unknown-blueprint"
                );
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Test
    public void testNoConfirmationRequiredBlueprintNoPerm() throws IOException {
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // No permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(false);
        // Reset command, no confirmation required
        assertFalse(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
    }

    /**
     * Test method for {@link IslandResetCommand#execute(User, String, java.util.List)}
     */
    @Ignore("NPE")
    @Test
    public void testNoConfirmationRequiredCustomSchemHasPermission() throws IOException {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(1);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Old island mock
        Island oldIsland = mock(Island.class);
        when(im.getIsland(any(), eq(uuid))).thenReturn(oldIsland);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        PowerMockito.mockStatic(NewIsland.class);
        when(NewIsland.builder()).thenReturn(builder);

        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        // Reset command, no confirmation required
        assertTrue(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        // Verify event
        verify(pim).callEvent(any(IslandBaseEvent.class));

    }
}
