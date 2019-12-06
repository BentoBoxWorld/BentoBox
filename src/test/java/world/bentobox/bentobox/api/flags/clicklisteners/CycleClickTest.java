package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.events.flags.FlagProtectionChangeEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class })
public class CycleClickTest {

    private static final Integer PROTECTION_RANGE = 200;
    private static final Integer X = 600;
    private static final Integer Y = 120;
    private static final Integer Z = 10000;
    @Mock
    private BentoBox plugin;
    private UUID uuid;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private Flag flag;
    @Mock
    private TabbedPanel panel;
    @Mock
    private Inventory inv;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private RanksManager rm;
    @Mock
    private PluginManager pim;
    @Mock
    private SettingsTab settingsTab;

    /**
     * @throws java.lang.Exception - exception
     */
    @Before
    public void setUp() throws Exception {

        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        World world = mock(World.class);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        User.setPlugin(plugin);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.hasPermission(anyString())).thenReturn(true);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Notifier
        Notifier notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        // Island Banned list initialization
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(any())).thenReturn(false);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(X);
        when(loc.getBlockY()).thenReturn(Y);
        when(loc.getBlockZ()).thenReturn(Z);
        when(island.getCenter()).thenReturn(loc);
        when(island.getProtectionRange()).thenReturn(PROTECTION_RANGE);
        // Island is not locked by default
        when(island.isAllowed(any(), any())).thenReturn(true);
        // Island owner is user by default
        when(island.getOwner()).thenReturn(uuid);

        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);

        // Common from to's
        Location outside = mock(Location.class);
        when(outside.getWorld()).thenReturn(world);
        when(outside.getBlockX()).thenReturn(X + PROTECTION_RANGE + 1);
        when(outside.getBlockY()).thenReturn(Y);
        when(outside.getBlockZ()).thenReturn(Z);

        Location inside = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 1);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);

        Location inside2 = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 2);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(eq(inside))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(eq(inside2))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(eq(outside))).thenReturn(Optional.empty());
        when(im.getIslandAt(any())).thenReturn(opIsland);

        FlagsManager fm = mock(FlagsManager.class);
        when(fm.getFlag(anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Ranks Manager
        when(plugin.getRanksManager()).thenReturn(rm);

        // Provide a current rank value - member
        when(island.getFlag(any())).thenReturn(RanksManager.MEMBER_RANK);
        // Set up up and down ranks
        when(rm.getRankUpValue(eq(RanksManager.VISITOR_RANK))).thenReturn(RanksManager.COOP_RANK);
        when(rm.getRankUpValue(eq(RanksManager.COOP_RANK))).thenReturn(RanksManager.TRUSTED_RANK);
        when(rm.getRankUpValue(eq(RanksManager.TRUSTED_RANK))).thenReturn(RanksManager.MEMBER_RANK);
        when(rm.getRankUpValue(eq(RanksManager.MEMBER_RANK))).thenReturn(RanksManager.OWNER_RANK);
        when(rm.getRankDownValue(eq(RanksManager.OWNER_RANK))).thenReturn(RanksManager.MEMBER_RANK);
        when(rm.getRankDownValue(eq(RanksManager.MEMBER_RANK))).thenReturn(RanksManager.TRUSTED_RANK);
        when(rm.getRankDownValue(eq(RanksManager.TRUSTED_RANK))).thenReturn(RanksManager.COOP_RANK);
        when(rm.getRankDownValue(eq(RanksManager.COOP_RANK))).thenReturn(RanksManager.VISITOR_RANK);

        // IslandWorldManager
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);

        // Event
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Active tab
        when(panel.getActiveTab()).thenReturn(settingsTab);
        when(settingsTab.getIsland()).thenReturn(island);


    }

    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testNoPremission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        CycleClick udc = new CycleClick("LOCK");
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, 5));
        verify(user).sendMessage(eq("general.errors.no-permission"), eq("[permission]"), eq("bskyblock.settings.LOCK"));
    }

    @Test
    public void testUpDownClick() {
        CycleClick udc = new CycleClick("LOCK");
        assertNotNull(udc);
    }

    @Test
    public void testOnLeftClick() {
        final int SLOT = 5;
        CycleClick udc = new CycleClick("LOCK");
        // Rank starts at member
        // Click left
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.OWNER_RANK));
        // Check rollover
        // Clicking when Owner should go to Visitor
        when(island.getFlag(any())).thenReturn(RanksManager.OWNER_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.VISITOR_RANK));
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    @Test
    public void testOnLeftClickSetMinMax() {
        // Provide a current rank value - coop
        when(island.getFlag(any())).thenReturn(RanksManager.COOP_RANK);
        final int SLOT = 5;
        CycleClick udc = new CycleClick("LOCK", RanksManager.COOP_RANK, RanksManager.MEMBER_RANK);
        // Rank starts at member
        // Click left
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.TRUSTED_RANK));
        // Check rollover
        // Clicking when Member should go to Coop
        when(island.getFlag(any())).thenReturn(RanksManager.MEMBER_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.COOP_RANK));
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    @Test
    public void testOnRightClick() {
        final int SLOT = 5;
        CycleClick udc = new CycleClick("LOCK");
        // Rank starts at member
        // Right click - down rank to Trusted
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.TRUSTED_RANK));
        // Check rollover
        // Clicking when Visitor should go to Owner
        when(island.getFlag(any())).thenReturn(RanksManager.VISITOR_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.OWNER_RANK));
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    @Test
    public void testOnRightClickMinMaxSet() {
        // Provide a current rank value - coop
        when(island.getFlag(any())).thenReturn(RanksManager.TRUSTED_RANK);
        final int SLOT = 5;
        CycleClick udc = new CycleClick("LOCK", RanksManager.COOP_RANK, RanksManager.MEMBER_RANK);
        // Rank starts at member
        // Right click
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.COOP_RANK));
        // Check rollover
        // Clicking when Coop should go to Member
        when(island.getFlag(any())).thenReturn(RanksManager.COOP_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.MEMBER_RANK));
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    @Test
    public void testAllClicks() {
        // Test all possible click types
        CycleClick udc = new CycleClick("LOCK");
        Arrays.asList(ClickType.values()).forEach(c -> assertTrue(udc.onClick(panel, user, c, 0)));
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    @Test
    public void testNotOwner() {
        UUID u = UUID.randomUUID();
        when(island.getOwner()).thenReturn(u);
        verify(plugin, Mockito.never()).getRanksManager();

    }

    @Test
    public void testNullIsland() {
        when(im.getIsland(any(), any(UUID.class))).thenReturn(null);
        verify(plugin, Mockito.never()).getRanksManager();
    }

}
