package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.papermc.paper.ServerBuildInfo;
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
import world.bentobox.bentobox.mocks.ServerMocks;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

@Ignore("Needs update to work with PaperAPI")
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, User.class, Util.class, RanksManager.class , ServerBuildInfo.class})
public class CycleClickTest {

    private static final Integer PROTECTION_RANGE = 200;
    private static final Integer X = 600;
    private static final Integer Y = 120;
    private static final Integer Z = 10000;
    private static final int SLOT = 5;
    private static final String LOCK = "LOCK";
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
    private PluginManager pim;
    @Mock
    private SettingsTab settingsTab;
    @Mock
    private RanksManager rm;
    private List<String> hiddenFlags;
    @Mock
    private @NonNull Player p;


    /**
     * @throws java.lang.Exception - exception
     */
    @Before
    public void setUp() throws Exception {
        ServerMocks.newServer();
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        World world = mock(World.class);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // User
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
        // when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
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
        when(flag.getID()).thenReturn(LOCK);
        when(fm.getFlag(anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Provide a current rank value - member
        when(island.getFlag(any())).thenReturn(RanksManager.MEMBER_RANK);
        // Set up up and down ranks
        PowerMockito.mockStatic(RanksManager.class);
        when(RanksManager.getInstance()).thenReturn(rm);
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
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();

        // Event
        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Active tab
        when(panel.getActiveTab()).thenReturn(settingsTab);
        when(panel.getWorld()).thenReturn(Optional.of(world));
        when(panel.getName()).thenReturn("name");
        when(settingsTab.getIsland()).thenReturn(island);

        // Hidden flags
        hiddenFlags = new ArrayList<>();
        when(iwm.getHiddenFlags(world)).thenReturn(hiddenFlags);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testNoPremission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        CycleClick udc = new CycleClick(LOCK);
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, 5));
        verify(user).sendMessage(eq("general.errors.no-permission"), eq("[permission]"), eq("bskyblock.settings.LOCK"));
    }

    @Test
    public void testUpDownClick() {
        CycleClick udc = new CycleClick(LOCK);
        assertNotNull(udc);
    }

    /**
     * Test for {@link CycleClick#onClick(world.bentobox.bentobox.api.panels.Panel, User, ClickType, int)}
     */
    @Test
    public void testOnLeftClick() {
        final int SLOT = 5;
        CycleClick udc = new CycleClick(LOCK);
        // Rank starts at member
        // Click left
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(flag, RanksManager.OWNER_RANK);
        // Check rollover
        // Clicking when Owner should go to Visitor
        when(island.getFlag(any())).thenReturn(RanksManager.OWNER_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(eq(flag), eq(RanksManager.VISITOR_RANK));
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    /**
     * Test for {@link CycleClick#onClick(world.bentobox.bentobox.api.panels.Panel, User, ClickType, int)}
     */
    @Test
    public void testOnLeftClickSetMinMax() {
        // Provide a current rank value - coop
        when(island.getFlag(any())).thenReturn(RanksManager.COOP_RANK);
        final int SLOT = 5;
        CycleClick udc = new CycleClick(LOCK, RanksManager.COOP_RANK, RanksManager.MEMBER_RANK);
        // Rank starts at member
        // Click left
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(flag, RanksManager.TRUSTED_RANK);
        // Check rollover
        // Clicking when Member should go to Coop
        when(island.getFlag(any())).thenReturn(RanksManager.MEMBER_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        verify(island).setFlag(flag, RanksManager.COOP_RANK);
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    /**
     * Test for {@link CycleClick#onClick(world.bentobox.bentobox.api.panels.Panel, User, ClickType, int)}
     */
    @Test
    public void testOnRightClick() {
        final int SLOT = 5;
        CycleClick udc = new CycleClick(LOCK);
        // Rank starts at member
        // Right click - down rank to Trusted
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(flag, RanksManager.TRUSTED_RANK);
        // Check rollover
        // Clicking when Visitor should go to Owner
        when(island.getFlag(any())).thenReturn(RanksManager.VISITOR_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(flag, RanksManager.OWNER_RANK);
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    /**
     * Test for {@link CycleClick#onClick(world.bentobox.bentobox.api.panels.Panel, User, ClickType, int)}
     */
    @Test
    public void testOnRightClickMinMaxSet() {
        // Provide a current rank value - coop
        when(island.getFlag(any())).thenReturn(RanksManager.TRUSTED_RANK);
        final int SLOT = 5;
        CycleClick udc = new CycleClick(LOCK, RanksManager.COOP_RANK, RanksManager.MEMBER_RANK);
        // Rank starts at member
        // Right click
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(flag, RanksManager.COOP_RANK);
        // Check rollover
        // Clicking when Coop should go to Member
        when(island.getFlag(any())).thenReturn(RanksManager.COOP_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        verify(island).setFlag(flag, RanksManager.MEMBER_RANK);
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    /**
     * Test for {@link CycleClick#onClick(world.bentobox.bentobox.api.panels.Panel, User, ClickType, int)}
     */
    @Test
    public void testAllClicks() {
        // Test all possible click types
        CycleClick udc = new CycleClick(LOCK);
        Arrays.asList(ClickType.values()).forEach(c -> assertTrue(udc.onClick(panel, user, c, 0)));
        verify(pim, times(2)).callEvent(any(FlagProtectionChangeEvent.class));
    }

    @Test
    public void testNoWorld() {
        CycleClick udc = new CycleClick(LOCK);
        when(panel.getWorld()).thenReturn(Optional.empty());
        assertTrue(udc.onClick(panel, user, ClickType.SHIFT_LEFT, SLOT));
        verify(plugin).logError("Panel name has no world associated with it. Please report this bug to the author.");
    }

    /**
     * Test for {@link CycleClick#onClick(world.bentobox.bentobox.api.panels.Panel, User, ClickType, int)}
     */
    @Test
    public void testOnShiftLeftClickNotOp() {
        CycleClick udc = new CycleClick(LOCK);
        // Click shift left
        assertTrue(udc.onClick(panel, user, ClickType.SHIFT_LEFT, SLOT));
        verify(user, never()).sendMessage(anyString());
    }

    /**
     * Test for {@link CycleClick#onClick(world.bentobox.bentobox.api.panels.Panel, User, ClickType, int)}
     */
    @Test
    public void testOnShiftLeftClickIsOp() {
        when(user.isOp()).thenReturn(true);
        CycleClick udc = new CycleClick(LOCK);
        // Click shift left
        assertTrue(hiddenFlags.isEmpty());
        assertTrue(udc.onClick(panel, user, ClickType.SHIFT_LEFT, SLOT));
        assertFalse(hiddenFlags.isEmpty());
        assertEquals(LOCK, hiddenFlags.get(0));
        // Click shift left again to remove flag
        assertTrue(udc.onClick(panel, user, ClickType.SHIFT_LEFT, SLOT));
        assertTrue(hiddenFlags.isEmpty());
        // Verify sounds
        verify(p, times(2)).playSound((Location) null, (Sound) null, 1F, 1F);
    }
}
