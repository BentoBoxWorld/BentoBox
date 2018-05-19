package us.tastybento.bskyblock.listeners.flags;

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

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Settings;
import us.tastybento.bskyblock.api.flags.Flag;
import us.tastybento.bskyblock.api.panels.Panel;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.user.Notifier;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.FlagsManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.LocalesManager;
import us.tastybento.bskyblock.managers.PlayersManager;
import us.tastybento.bskyblock.managers.RanksManager;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BSkyBlock.class, User.class })
public class UpDownClickTest {

    private static final Integer PROTECTION_RANGE = 200;
    private static final Integer X = 600;
    private static final Integer Y = 120;
    private static final Integer Z = 10000;
    private BSkyBlock plugin;
    private UUID uuid;
    private User user;
    private Settings s;
    private IslandsManager im;
    private PlayersManager pm;
    private Island island;
    private World world;
    private Location loc;
    private Location outside;
    private Location inside;
    private Notifier notifier;
    private Location inside2;
    private BukkitScheduler sch;
    private Flag flag;
    private PanelItem panelItem;
    private Panel panel;
    private Inventory inv;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        // Set up plugin
        plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        // World
        world = mock(World.class);

        // Settings
        s = mock(Settings.class);
        when(s.getResetWait()).thenReturn(0L);
        when(s.getResetLimit()).thenReturn(3);
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

        // No island for player to begin with (set it later in the tests)
        im = mock(IslandsManager.class);
        when(im.hasIsland(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(im.isOwner(Mockito.any(), Mockito.eq(uuid))).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // Has team
        pm = mock(PlayersManager.class);
        when(im.inTeam(Mockito.any(), Mockito.eq(uuid))).thenReturn(true);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        sch = mock(BukkitScheduler.class);
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // Locales      
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        // Island Banned list initialization
        island = mock(Island.class);
        when(island.getBanned()).thenReturn(new HashSet<>());
        when(island.isBanned(Mockito.any())).thenReturn(false);
        loc = mock(Location.class);
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
        outside = mock(Location.class);
        when(outside.getWorld()).thenReturn(world);
        when(outside.getBlockX()).thenReturn(X + PROTECTION_RANGE + 1);
        when(outside.getBlockY()).thenReturn(Y);
        when(outside.getBlockZ()).thenReturn(Z);

        inside = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 1);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);

        inside2 = mock(Location.class);
        when(inside.getWorld()).thenReturn(world);
        when(inside.getBlockX()).thenReturn(X + PROTECTION_RANGE - 2);
        when(inside.getBlockY()).thenReturn(Y);
        when(inside.getBlockZ()).thenReturn(Z);

        Optional<Island> opIsland = Optional.ofNullable(island);
        when(im.getProtectedIslandAt(Mockito.eq(inside))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(Mockito.eq(inside2))).thenReturn(opIsland);
        when(im.getProtectedIslandAt(Mockito.eq(outside))).thenReturn(Optional.empty());

        panelItem = mock(PanelItem.class);
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

    }

    @Test
    public void testUpDownClick() {
        UpDownClick udc = new UpDownClick("LOCK");
        assertNotNull(udc);
    }

    @Test
    public void testOnLeftClick() {
        UpDownClick udc = new UpDownClick("LOCK");
        // Click left
        assertTrue(udc.onClick(panel, user, ClickType.LEFT, 5));
        Mockito.verify(island).setFlag(Mockito.eq(flag), Mockito.eq(RanksManager.OWNER_RANK));
        Mockito.verify(flag).toPanelItem(Mockito.any(), Mockito.any());
        Mockito.verify(inv).setItem(Mockito.eq(5), Mockito.any());        
    }

    @Test
    public void testOnRightClick() {
        UpDownClick udc = new UpDownClick("LOCK");
        // Right click
        assertTrue(udc.onClick(panel, user, ClickType.RIGHT, 0));
        Mockito.verify(island).setFlag(Mockito.eq(flag), Mockito.eq(RanksManager.VISITOR_RANK));
    }
    
    @Test
    public void testAllClicks() {
        // Test all possible click types
        UpDownClick udc = new UpDownClick("LOCK");
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
