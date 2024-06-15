package world.bentobox.bentobox.api.commands.island.team;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.events.IslandBaseEvent;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.TeamInvite;
import world.bentobox.bentobox.database.objects.TeamInvite.Type;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.managers.RanksManagerBeforeClassTest;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class })
public class IslandTeamInviteCommandTest extends RanksManagerBeforeClassTest {

    @Mock
    private IslandTeamCommand ic;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private PluginManager pim;
    @Mock
    private PlayersManager pm;
    @Mock
    private Settings s;
    @Mock
    private User target;
    @Mock
    private User user;

    private UUID uuid;
    private UUID islandUUID;
    private IslandTeamInviteCommand itl;
    private UUID notUUID;
    @Mock
    private Player p;
    @Mock
    private @NonNull World world;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(s);

        // Data folder for panels
        when(plugin.getDataFolder())
                .thenReturn(new File("src" + File.separator + "main" + File.separator + "resources"));

        // Player & users
        PowerMockito.mockStatic(User.class);

        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getDisplayName()).thenReturn("&Ctastbento");
        when(user.isOnline()).thenReturn(true);
        // Permission to invite 3 more players
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(3);
        when(User.getInstance(uuid)).thenReturn(user);
        when(user.getTranslation(any())).thenAnswer(invocation -> invocation.getArgument(0, String.class));
        // Vanished players
        when(p.canSee(any())).thenReturn(true);

        User.setPlugin(plugin);
        // Target
        notUUID = UUID.randomUUID();
        when(target.getUniqueId()).thenReturn(notUUID);
        when(target.getPlayer()).thenReturn(p);
        when(target.isOnline()).thenReturn(true);
        when(target.getName()).thenReturn("target");
        when(target.getDisplayName()).thenReturn("&Ctarget");
        when(User.getInstance(notUUID)).thenReturn(target);

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getWorld()).thenReturn(world);
        when(ic.getPlugin()).thenReturn(plugin);

        // Island
        islandUUID = UUID.randomUUID();
        when(island.getUniqueId()).thenReturn(islandUUID.toString());
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));

        // Player has island to begin with
        when(im.hasIsland(any(), eq(uuid))).thenReturn(true);
        // when(im.isOwner(any(), eq(uuid))).thenReturn(true);
        // when(im.getOwner(any(), eq(uuid))).thenReturn(uuid);
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(im.getIsland(any(), eq(user))).thenReturn(island);
        when(im.getMaxMembers(eq(island), anyInt())).thenReturn(4);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);

        // Player Manager
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getUUID("tastybento")).thenReturn(uuid);
        when(pm.getUUID("target")).thenReturn(notUUID);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenReturn(null);
        when(plugin.getLocalesManager()).thenReturn(lm);

        // IWM friendly name
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        @NonNull
        WorldSettings ws = new TestWorldSettings();
        when(iwm.getWorldSettings(world)).thenReturn(ws);
        when(plugin.getIWM()).thenReturn(iwm);

        // Parent command
        when(ic.getTopLabel()).thenReturn("island");

        // Mock item factory (for itemstacks)
        ItemFactory itemFactory = mock(ItemFactory.class);
        ItemMeta bannerMeta = mock(ItemMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(bannerMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        Inventory inventory = mock(Inventory.class);
        when(Bukkit.createInventory(eq(null), anyInt(), any())).thenReturn(inventory);
        when(Bukkit.createInventory(eq(null), any(InventoryType.class), any())).thenReturn(inventory);

        // Command under test
        itl = new IslandTeamInviteCommand(ic);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteCoolDownActive() {
        // 10 minutes = 600 seconds
        when(s.getInviteCooldown()).thenReturn(10);
        itl.setCooldown(islandUUID, notUUID, 100);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage(eq("general.errors.you-must-wait"), eq(TextVariables.NUMBER), anyString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteDifferentPlayerInTeam() {
        when(im.inTeam(any(), any())).thenReturn(true);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage(eq("commands.island.team.invite.errors.already-on-team"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteLowRank() {
        when(island.getRank(any(User.class))).thenReturn(RanksManager.MEMBER_RANK);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.OWNER_RANK);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoIsland() {
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        when(im.inTeam(any(), any(UUID.class))).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage(eq("general.errors.no-island"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteNoTarget() {
        assertTrue(itl.canExecute(user, itl.getLabel(), Collections.emptyList()));
        // Show panel
        verify(p).openInventory(any(Inventory.class));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteOfflinePlayer() {
        when(target.isOnline()).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage(eq("general.errors.offline-player"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteVanishedPlayer() {
        when(p.canSee(any())).thenReturn(false);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage(eq("general.errors.offline-player"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSamePlayer() {
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("tastybento")));
        verify(user).sendMessage(eq("commands.island.team.invite.errors.cannot-invite-self"));
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteSuccess() {
        assertTrue(itl.canExecute(user, itl.getLabel(), List.of("target")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteUnknownPlayer() {
        when(pm.getUUID(eq("target"))).thenReturn(null);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage(eq("general.errors.unknown-player"), eq(TextVariables.NAME), eq("target"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#canExecute(User, String, java.util.List)}.
     */
    @Test
    public void testCanExecuteFullIsland() {
        when(im.getMaxMembers(eq(island), anyInt())).thenReturn(0);
        assertFalse(itl.canExecute(user, itl.getLabel(), List.of("target")));
        verify(user).sendMessage(eq("commands.island.team.invite.errors.island-is-full"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessTargetHasIsland() {
        when(im.getIsland(world, uuid)).thenReturn(island);
        when(im.hasIsland(world, notUUID)).thenReturn(true);
        testCanExecuteSuccess();
        assertTrue(itl.execute(user, itl.getLabel(), List.of("target")));
        verify(pim).callEvent(any(IslandBaseEvent.class));
        verify(user, never()).sendMessage(eq("commands.island.team.invite.removing-invite"));
        verify(ic).addInvite(Type.TEAM, uuid, notUUID, island);
        verify(user).sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, "target", TextVariables.DISPLAY_NAME, "&Ctarget");
        verify(target).sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, "tastybento", TextVariables.DISPLAY_NAME, "&Ctastbento");
        verify(target).sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, "island");
        verify(target).sendMessage("commands.island.team.invite.you-will-lose-your-island");

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteSuccessTargetHasNoIsland() {
        testCanExecuteSuccess();
        when(im.getIsland(world, uuid)).thenReturn(island);
        assertTrue(itl.execute(user, itl.getLabel(), List.of("target")));
        verify(pim).callEvent(any(IslandBaseEvent.class));
        verify(user, never()).sendMessage("commands.island.team.invite.removing-invite");
        verify(ic).addInvite(Type.TEAM, uuid, notUUID, island);
        verify(user).sendMessage("commands.island.team.invite.invitation-sent", TextVariables.NAME, "target",
                TextVariables.DISPLAY_NAME, "&Ctarget");
        verify(target).sendMessage("commands.island.team.invite.name-has-invited-you", TextVariables.NAME, "tastybento",
                TextVariables.DISPLAY_NAME, "&Ctastbento");
        verify(target).sendMessage("commands.island.team.invite.to-accept-or-reject", TextVariables.LABEL, "island");
        verify(target, never()).sendMessage("commands.island.team.invite.you-will-lose-your-island");

    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.api.commands.island.team.IslandTeamInviteCommand#execute(User, String, java.util.List)}.
     */
    @Test
    public void testExecuteTargetAlreadyInvited() {
        testCanExecuteSuccess();
        when(im.getIsland(world, uuid)).thenReturn(island);
        when(ic.isInvited(notUUID)).thenReturn(true);
        // Set up invite
        when(ic.getInviter(notUUID)).thenReturn(uuid);
        TeamInvite invite = mock(TeamInvite.class);
        when(invite.getType()).thenReturn(Type.TEAM);
        when(ic.getInvite(notUUID)).thenReturn(invite);
        assertTrue(itl.execute(user, itl.getLabel(), List.of("target")));
        verify(pim).callEvent(any(IslandBaseEvent.class));
        verify(ic).removeInvite(notUUID);
        verify(user).sendMessage("commands.island.team.invite.removing-invite");
    }

}
