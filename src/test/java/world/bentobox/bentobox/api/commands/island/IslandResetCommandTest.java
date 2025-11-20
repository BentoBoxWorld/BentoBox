package world.bentobox.bentobox.api.commands.island;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.island.NewIsland;

/**
 * @author tastybento
 *
 */
public class IslandResetCommandTest extends AbstractCommonSetup {

    @Mock
    private CompositeCommand ic;
    @Mock
    private User user;
    @Mock
    private Settings s;
    @Mock
    private PlayersManager pm;
    @Mock
    private BlueprintsManager bpm;

    private IslandResetCommand irc;

    private UUID uuid;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Mockito.mockStatic(IslandsManager.class, Mockito.RETURNS_MOCKS);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(s.getResetCooldown()).thenReturn(0);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.spigot()).thenReturn(spigot);
        when(p.isOnline()).thenReturn(true);
        User.getInstance(p);

        // User
        User.setPlugin(plugin);

        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.isOnline()).thenReturn(true);
        when(user.getPlayer()).thenReturn(p);
        when(user.getTranslation(any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getTopLabel()).thenReturn("island");
        // World
        when(ic.getWorld()).thenReturn(world);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        BukkitTask task = mock(BukkitTask.class);
        when(sch.runTaskLater(any(), any(Runnable.class), any(Long.class))).thenReturn(task);

        mockedBukkit.when(() -> Bukkit.getScheduler()).thenReturn(sch);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");

        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);
        when(bpm.validate(any(), any())).thenReturn("custom");

        // Give the user some resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(3);

        // Island team members
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        Builder<UUID> members = new ImmutableSet.Builder<>();
        members.add(uuid);
        // Put a team on the island
        for (int j = 0; j < 11; j++) {
            UUID temp = UUID.randomUUID();
            when(mockPlayer.getUniqueId()).thenReturn(temp);
            User.getInstance(mockPlayer);
            members.add(temp);
        }
        when(island.getMemberSet()).thenReturn(members.build());
        when(location.clone()).thenReturn(location);
        when(island.getCenter()).thenReturn(location);
        when(island.getHistory()).thenReturn(Collections.emptyList());
        when(island.getSpawnPoint()).thenReturn(Collections.emptyMap());

        // Addon
        GameModeAddon addon1 = mock(GameModeAddon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "BSkyBlock", "1.0.0").build();
        when(addon1.getDescription()).thenReturn(desc);
        when(ic.getAddon()).thenReturn(addon1);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(Mockito.any(), Mockito.any()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);

        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(phm.replacePlaceholders(any(), any())).thenAnswer(invocation -> invocation.getArgument(1, String.class));
        // Placeholder manager
        when(plugin.getPlaceholdersManager()).thenReturn(phm);

        // The command
        irc = new IslandResetCommand(ic);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }
    /**
     * Test method for
     * {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testNoIsland() {
        // Test the reset command
        // Does not have island
        assertFalse(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
        verify(user).sendMessage("general.errors.not-owner");
    }

    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testNoResetsLeft() {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        //when(im.isOwner(any(), eq(uuid))).thenReturn(true);
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
    @Test
    public void testNoConfirmationRequired() throws Exception {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Set so no confirmation required
        when(s.isResetConfirmation()).thenReturn(false);

        // Mock up NewIsland builder
        NewIsland.Builder builder = mock(NewIsland.Builder.class);
        when(builder.player(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.name(any())).thenReturn(builder);
        when(builder.addon(any())).thenReturn(builder);
        when(builder.build()).thenReturn(mock(Island.class));
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(() -> NewIsland.builder()).thenReturn(builder);

        // Reset command, no confirmation required
        assertTrue(irc.execute(user, irc.getLabel(), Collections.emptyList()));
        // TODO Verify that panel was shown
        // verify(bpm).showPanel(any(), eq(user), eq(irc.getLabel()));
        // Verify event (13 * 2)
        verify(pim, times(14)).callEvent(any(IslandBaseEvent.class));
        // Verify messaging
        verify(user).sendMessage("commands.island.create.creating-island");
        verify(user, never()).sendMessage(eq("commands.island.reset.kicked-from-island"), eq(TextVariables.GAMEMODE), anyString());
        // Only 11 because the leader should not see this
        checkSpigotMessage("commands.island.reset.kicked-from-island", 11);
    }

    /**
     * Test method for {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testUnlimitedResets() throws Exception {
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
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(() -> NewIsland.builder()).thenReturn(builder);
        // Test with unlimited resets
        when(pm.getResetsLeft(eq(world), eq(uuid))).thenReturn(-1);

        // Reset
        assertTrue(irc.canExecute(user, irc.getLabel(), Collections.emptyList()));
    }

    /**
     * Test method for
     * {@link IslandResetCommand#canExecute(User, String, java.util.List)}
     */
    @Test
    public void testNoPaste() throws Exception {
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
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(() -> NewIsland.builder()).thenReturn(builder);
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
    public void testConfirmationRequired() throws Exception {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        //when(im.isOwner(any(), eq(uuid))).thenReturn(true);
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
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(() -> NewIsland.builder()).thenReturn(builder);

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
    @Test
    public void testNoConfirmationRequiredCustomSchemHasPermission() throws Exception {
        // Now has island, but is not the owner
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // Now is owner, but still has team
        //when(im.isOwner(any(), eq(uuid))).thenReturn(true);
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
        MockedStatic<NewIsland> mockedNewIsland = Mockito.mockStatic(NewIsland.class);
        mockedNewIsland.when(() -> NewIsland.builder()).thenReturn(builder);

        // Bundle exists
        when(bpm.validate(any(), any())).thenReturn("custom");
        // Has permission
        when(bpm.checkPerm(any(), any(), any())).thenReturn(true);
        // Reset command, no confirmation required
        assertTrue(irc.execute(user, irc.getLabel(), Collections.singletonList("custom")));
        verify(user).sendMessage("commands.island.create.creating-island");
        // Verify event (13 * 2)
        verify(pim, times(14)).callEvent(any(IslandBaseEvent.class));

    }
}