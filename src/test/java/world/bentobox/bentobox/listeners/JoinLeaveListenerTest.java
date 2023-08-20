package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.mysql.cj.x.protobuf.MysqlxCrud.CollectionOrBuilder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class})
public class JoinLeaveListenerTest {

    private static final String[] NAMES = {"adam", "ben", "cara", "dave", "ed", "frank", "freddy", "george", "harry", "ian", "joe"};

    @Mock
    private BentoBox plugin;
    @Mock
    private PlayersManager pm;
    @Mock
    private Player player;
    @Mock
    private Player coopPlayer;
    @Mock
    private World world;

    private JoinLeaveListener jll;
    @Mock
    private Players pls;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private Inventory chest;
    @Mock
    private Settings settings;
    @Mock
    private IslandsManager im;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private PlayerInventory inv;
    private Set<String> set;

    private @Nullable Island island;
    @Mock
    private GameModeAddon gameMode;
    @Mock
    private PluginManager pim;
    @Mock
    private @NonNull Location location;

    /**
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        when(world.getName()).thenReturn("worldname");

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        // Reset everything
        when(iwm.isOnLeaveResetEnderChest(any())).thenReturn(true);
        when(iwm.isOnLeaveResetInventory(any())).thenReturn(true);
        when(iwm.getOverWorlds()).thenReturn(Collections.singletonList(world));
        when(iwm.getResetEpoch(any())).thenReturn(20L);
        Optional<GameModeAddon> opGm = Optional.of(gameMode);
        when(iwm.getAddon(any())).thenReturn(opGm);
        when(gameMode.getPermissionPrefix()).thenReturn("acidisland.");
        when(iwm.getIslandDistance(any())).thenReturn(100);

        UUID uuid = UUID.randomUUID();
        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getWorld()).thenReturn(world);
        when(player.getEnderChest()).thenReturn(chest);
        when(player.getName()).thenReturn("tastybento");
        when(player.getInventory()).thenReturn(inv);
        when(player.getEffectivePermissions()).thenReturn(Collections.emptySet());

        // Player is pending kicks
        set = new HashSet<>();
        set.add("worldname");
        when(pls.getPendingKicks()).thenReturn(set);

        // Player Manager
        when(pm.getPlayer(any())).thenReturn(pls);
        when(pm.isKnown(any())).thenReturn(false);
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getName(eq(uuid))).thenReturn("tastybento");

        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // islands manager
        when(plugin.getIslands()).thenReturn(im);
        // player is owner of their island
        when(im.isOwner(any(), any())).thenReturn(true);

        // Island
        island = new Island(location, uuid, 50);
        island.setWorld(world);

        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getIslands()).thenReturn(Collections.singletonList(island));
        Map<UUID, Integer> memberMap = new HashMap<>();

        memberMap.put(uuid, RanksManager.OWNER_RANK);
        // Add a coop member
        UUID uuid2 = UUID.randomUUID();
        when(coopPlayer.getUniqueId()).thenReturn(uuid2);
        User.getInstance(coopPlayer);
        memberMap.put(uuid2, RanksManager.COOP_RANK);
        island.setMembers(memberMap);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(scheduler);

        when(Bukkit.getPluginManager()).thenReturn(pim);

        // Bukkit - online players
        Map<UUID, String> online = new HashMap<>();

        Set<Player> onlinePlayers = new HashSet<>();
        for (String name : NAMES) {
            Player p1 = mock(Player.class);
            UUID u = UUID.randomUUID();
            when(p1.getUniqueId()).thenReturn(u);
            when(p1.getName()).thenReturn(name);
            online.put(u, name);
            onlinePlayers.add(p1);
        }
        onlinePlayers.add(player);
        when(Bukkit.getOnlinePlayers()).then((Answer<Set<Player>>) invocation -> onlinePlayers);

        User.setPlugin(plugin);
        User.getInstance(player);

        // Util
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(world);
        // Util translate color codes (used in user translate methods)
        when(Util.translateColorCodes(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // user text
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        jll = new JoinLeaveListener(plugin);
    }

    /**
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinNotKnownNoAutoCreate() {
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        jll.onPlayerJoin(event);
        // Verify
        verify(pm, times(2)).addPlayer(any());
        verify(pm, times(2)).save(any());
        verify(player, never()).sendMessage(anyString());
        // Verify resets
        verify(pm).setResets(eq(world), any(), eq(0));
        // Verify inventory clear because of kick
        // Check inventory cleared
        verify(chest).clear();
        verify(inv).clear();
        assertTrue(set.isEmpty());
        verify(pm, times(2)).save(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinNullWorld() {
        when(player.getWorld()).thenReturn(null); // Null
        when(Util.getWorld(any())).thenReturn(null); // Make null
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        jll.onPlayerJoin(event);
        // Verify inventory clear because of kick
        // Check inventory cleared
        verify(chest, never()).clear();
        verify(inv, never()).clear();
        assertFalse(set.isEmpty());
        verify(pm).save(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinRangeChangeTooLargePerm() {
        PermissionAttachmentInfo pa = mock(PermissionAttachmentInfo.class);
        when(pa.getPermission()).thenReturn("acidisland.island.range.1000");
        when(pa.getValue()).thenReturn(true);
        when(player.getEffectivePermissions()).thenReturn(Collections.singleton(pa));
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        jll.onPlayerJoin(event);
        // Verify
        verify(player).sendMessage(eq("commands.admin.setrange.range-updated"));
        // Verify island setting
        assertEquals(100, island.getProtectionRange());
        // Verify log
        verify(plugin).log("Island protection range changed from 50 to 100 for tastybento due to permission.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinRangeChangeSmallerPerm() {
        PermissionAttachmentInfo pa = mock(PermissionAttachmentInfo.class);
        when(pa.getPermission()).thenReturn("acidisland.island.range.10");
        when(pa.getValue()).thenReturn(true);
        when(player.getEffectivePermissions()).thenReturn(Collections.singleton(pa));
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        jll.onPlayerJoin(event);
        // Verify
        verify(player).sendMessage(eq("commands.admin.setrange.range-updated"));
        // Verify island setting
        assertEquals(10, island.getProtectionRange());
        // Verify log
        verify(plugin).log("Island protection range changed from 50 to 10 for tastybento due to permission.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinRangeChangeSmallIncreasePerm() {
        PermissionAttachmentInfo pa = mock(PermissionAttachmentInfo.class);
        when(pa.getPermission()).thenReturn("acidisland.island.range.55");
        when(pa.getValue()).thenReturn(true);
        when(player.getEffectivePermissions()).thenReturn(Collections.singleton(pa));
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        jll.onPlayerJoin(event);
        // Verify
        verify(player).sendMessage(eq("commands.admin.setrange.range-updated"));
        // Verify island setting
        assertEquals(55, island.getProtectionRange());
        // Verify log
        verify(plugin).log("Island protection range changed from 50 to 55 for tastybento due to permission.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinRangeChangeSamePerm() {
        PermissionAttachmentInfo pa = mock(PermissionAttachmentInfo.class);
        when(pa.getPermission()).thenReturn("acidisland.island.range.50");
        when(pa.getValue()).thenReturn(true);
        when(player.getEffectivePermissions()).thenReturn(Collections.singleton(pa));
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        jll.onPlayerJoin(event);
        // Verify
        verify(player, never()).sendMessage(eq("commands.admin.setrange.range-updated"));
        // Verify that the island protection range is not changed if it is already at that value
        assertEquals(50, island.getProtectionRange());
        // Verify log
        verify(plugin, never()).log("Island protection range changed from 50 to 10 for tastybento due to permission.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent)}.
     */
    @Test
    public void testOnPlayerJoinNotKnownAutoCreate() {
        when(iwm.isCreateIslandOnFirstLoginEnabled(eq(world))).thenReturn(true);
        PlayerJoinEvent event = new PlayerJoinEvent(player, "");
        jll.onPlayerJoin(event);
        // Verify
        verify(pm, times(2)).addPlayer(any());
        verify(pm, times(2)).save(any());
        verify(player).sendMessage(eq("commands.island.create.on-first-login"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerSwitchWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerSwitchWorld() {
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        jll.onPlayerSwitchWorld(event);
        // Check inventory cleared
        verify(chest).clear();
        verify(inv).clear();
        assertTrue(set.isEmpty());
        verify(pm).save(any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerSwitchWorld(org.bukkit.event.player.PlayerChangedWorldEvent)}.
     */
    @Test
    public void testOnPlayerSwitchWorldNullWorld() {
        when(Util.getWorld(any())).thenReturn(null);
        PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(player, world);
        jll.onPlayerSwitchWorld(event);
        // These should not happen
        verify(chest, never()).clear();
        verify(inv, never()).clear();
        assertFalse(set.isEmpty());
        verify(pm, never()).save(any());
    }


    /**
     * Test method for {@link world.bentobox.bentobox.listeners.JoinLeaveListener#onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent)}.
     */
    @Test
    public void testOnPlayerQuit() {
        PlayerQuitEvent event = new PlayerQuitEvent(player, "");
        jll.onPlayerQuit(event);
        verify(coopPlayer).sendMessage(eq("commands.island.team.uncoop.all-members-logged-off"));
        // Team is now only 1 big
        assertEquals(1, island.getMembers().size());
    }

}
