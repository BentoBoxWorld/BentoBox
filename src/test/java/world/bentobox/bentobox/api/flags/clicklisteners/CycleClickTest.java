package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class })
public class CycleClickTest {

    private static final Integer PROTECTION_RANGE = 200;
    private static final Integer X = 600;
    private static final Integer Y = 120;
    private static final Integer Z = 10000;
    private BentoBox plugin;
    private UUID uuid;
    private User user;
    private IslandsManager im;
    private Island island;
    private Flag flag;
    private Panel panel;
    private Inventory inv;
    private IslandWorldManager iwm;

    /**
     * @throws java.lang.Exception - exception
     */
    @Before
    public void setUp() throws Exception {

        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        World world = mock(World.class);

        // Settings
        Settings s = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(s);

        // Player
        Player p = mock(Player.class);
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        User.setPlugin(plugin);
        when(user.isOp()).thenReturn(false);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(p);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        PlayersManager pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
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
        island = mock(Island.class);
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(Mockito.any())).thenReturn(false);
        Location loc = mock(Location.class);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(X);
        when(loc.getBlockY()).thenReturn(Y);
        when(loc.getBlockZ()).thenReturn(Z);
        when(island.getCenter()).thenReturn(loc);
        when(island.getProtectionRange()).thenReturn(PROTECTION_RANGE);
        // Island is not locked by default
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        // Island owner is user by default
        when(island.getOwner()).thenReturn(uuid);

        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);

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
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(Mockito.eq(inside2))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(Mockito.eq(outside))).thenReturn(Optional.empty());

        PanelItem panelItem = mock(PanelItem.class);
        flag = mock(Flag.class);
        when(flag.toPanelItem(Mockito.any(), Mockito.any())).thenReturn(panelItem);
        when(panelItem.getItem()).thenReturn(mock(ItemStack.class));
        FlagsManager fm = mock(FlagsManager.class);
        when(fm.getFlagByID(Mockito.anyString())).thenReturn(flag);
        when(plugin.getFlagsManager()).thenReturn(fm);

        RanksManager rm = mock(RanksManager.class);

        when(plugin.getRanksManager()).thenReturn(rm);

        // Provide a current rank value - member
        when(island.getFlag(Mockito.any())).thenReturn(RanksManager.MEMBER_RANK);
        // Set up up and down ranks
        when(rm.getRankUpValue(Mockito.eq(RanksManager.MEMBER_RANK))).thenReturn(RanksManager.OWNER_RANK);
        when(rm.getRankDownValue(Mockito.eq(RanksManager.MEMBER_RANK))).thenReturn(RanksManager.VISITOR_RANK);

        panel = mock(Panel.class);
        inv = mock(Inventory.class);
        when(panel.getInventory()).thenReturn(inv);

        // IslandWorldManager
        iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(Mockito.any())).thenReturn(true);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock");

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(world);

    }

    @Test
    public void testNotInWorld() {
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        CycleClick udc = new CycleClick("LOCK");
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, 5));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.wrong-world"));
    }

    @Test
    public void testNoPremission() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        CycleClick udc = new CycleClick("LOCK");
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, 5));
        Mockito.verify(user).sendMessage(Mockito.eq("general.errors.no-permission"), Mockito.eq("[permission]"), Mockito.eq("bskyblock.settings.LOCK"));
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
        Mockito.verify(island).setFlag(Mockito.eq(flag), Mockito.eq(RanksManager.OWNER_RANK));
        Mockito.verify(flag).toPanelItem(Mockito.any(), Mockito.any());
        Mockito.verify(inv).setItem(Mockito.eq(SLOT), Mockito.any());
        // Check rollover
        // Clicking when Owner should go to Visitor
        when(island.getFlag(Mockito.any())).thenReturn(RanksManager.OWNER_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, SLOT));
        Mockito.verify(island).setFlag(Mockito.eq(flag), Mockito.eq(RanksManager.VISITOR_RANK));
        Mockito.verify(flag, Mockito.times(2)).toPanelItem(Mockito.any(), Mockito.any());
        Mockito.verify(inv, Mockito.times(2)).setItem(Mockito.eq(SLOT), Mockito.any());
    }

    @Test
    public void testOnRightClick() {
        final int SLOT = 5;
        CycleClick udc = new CycleClick("LOCK");
        // Rank starts at member
        // Right click
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        Mockito.verify(island).setFlag(Mockito.eq(flag), Mockito.eq(RanksManager.VISITOR_RANK));
        Mockito.verify(flag).toPanelItem(Mockito.any(), Mockito.any());
        Mockito.verify(inv).setItem(Mockito.eq(SLOT), Mockito.any());
        // Check rollover
        // Clicking when Visitor should go to Owner
        when(island.getFlag(Mockito.any())).thenReturn(RanksManager.VISITOR_RANK);
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, SLOT));
        Mockito.verify(island).setFlag(Mockito.eq(flag), Mockito.eq(RanksManager.OWNER_RANK));
        Mockito.verify(flag, Mockito.times(2)).toPanelItem(Mockito.any(), Mockito.any());
        Mockito.verify(inv, Mockito.times(2)).setItem(Mockito.eq(SLOT), Mockito.any());
    }

    @Test
    public void testAllClicks() {
        // Test all possible click types
        CycleClick udc = new CycleClick("LOCK");
        Arrays.asList(ClickType.values()).forEach(c -> assertTrue(udc.onClick(panel, user, c, 0)));
    }

    @Test
    public void testNotOwner() {
        UUID u;
        do {
            u = UUID.randomUUID();
        } while(u.equals(uuid));

        when(island.getOwner()).thenReturn(u);
        Mockito.verify(plugin, Mockito.never()).getRanksManager();

    }

    @Test
    public void testNullIsland() {
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(null);
        Mockito.verify(plugin, Mockito.never()).getRanksManager();
    }

}
