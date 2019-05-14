package world.bentobox.bentobox.api.commands.island;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.commands.CompositeCommand;
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

    private CompositeCommand ic;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private PlayersManager pm;
    private World world;
    private IslandWorldManager iwm;
    @Mock
    private BlueprintsManager bpm;

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
        s = mock(Settings.class);
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // User, sometime use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);

        // Parent command has no aliases
        ic = mock(CompositeCommand.class);
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        // World
        world = mock(World.class);
        when(ic.getWorld()).thenReturn(world);

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);


        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);


        // IWM friendly name
        iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        when(bpm.validate(any(), any())).thenReturn("custom");
    }

    /**
     * Test method for .
     */
    @Test
    public void testNoIsland() {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Test the reset command
        // Does not have island
        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.no-island");
    }

    @Test
    public void testNotOwner() {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.not-owner");
    }

    @Test
    public void testHasTeam() {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("commands.island.reset.must-remove-members");
    }

    @Test
    public void testNoResetsLeft() {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);

        // Block based on no resets left
        when(pm.getResets(eq(world),eq(uuid))).thenReturn(3);

        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("commands.island.reset.none-left");
    }

    @Test
    public void testNoConfirmationRequired() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        // Now has no team
        when(im.inTeam(any(), eq(uuid))).thenReturn(false);
        // Give the user some resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(2);
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
        // Verify that panel was shown
        verify(bpm).showPanel(any(), eq(user), eq(irc.getLabel()));
    }

    @Test
    public void testUnlimitedResets() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        when(im.isOwner(any(), eq(uuid))).thenReturn(true);
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

    @Test
    public void testConfirmationRequired() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
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
    }

    @Test
    public void testNoConfirmationRequiredUnknownBlueprint() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // No such bundle
        when(bpm.validate(any(), any())).thenReturn(null);
        // Reset command, no confirmation required
        assertFalse(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
        verify(user).sendMessage(
                "commands.island.create.unknown-blueprint"
                );
    }

    @Test
    public void testNoConfirmationRequiredBlueprintNoPerm() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // No permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(false);
        // Reset command, no confirmation required
        assertFalse(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
    }

    @Test
    public void testNoConfirmationRequiredCustomSchemHasPermission() throws IOException {
        IslandResetCommand irc = new IslandResetCommand(ic);
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
    }
}
