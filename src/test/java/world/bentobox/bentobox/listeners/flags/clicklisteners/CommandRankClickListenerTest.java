package world.bentobox.bentobox.listeners.flags.clicklisteners;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.managers.RanksManagerBeforeClassTest;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, Util.class})
public class CommandRankClickListenerTest extends RanksManagerBeforeClassTest {
    @Mock
    private User user;
    @Mock
    private World world;
    @Mock
    private TabbedPanel panel;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private @NonNull Inventory inv;
    @Mock
    private GameModeAddon gma;

    private CommandRankClickListener crcl;
    @Mock
    private Player player;
    @Mock
    private IslandsManager im;
    @Mock
    private @Nullable Island island;

    private UUID uuid = UUID.randomUUID();
    private RanksManager rm;
    @Mock
    private CommandsManager cm;
    @Mock
    private SettingsTab tab;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    	super.setUp();

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        
        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.isAllowed(user, Flags.CHANGE_SETTINGS)).thenReturn(true);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.MEMBER_RANK);
        // IM
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIsland(world, uuid)).thenReturn(island);
        when(im.getIsland(world, user)).thenReturn(island);
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getAddon(any())).thenReturn(Optional.of(gma));
        when(iwm.getPermissionPrefix(world)).thenReturn("oneblock.");
        // Panel
        when(panel.getInventory()).thenReturn(inv);
        when(panel.getWorld()).thenReturn(Optional.of(world));
        when(panel.getName()).thenReturn("protection.flags.COMMAND_RANKS.name");
        when(panel.getActiveTab()).thenReturn(tab);
        // Tab
        when(tab.getIsland()).thenReturn(island);
        // User
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getPlayer()).thenReturn(player);
        when(user.inWorld()).thenReturn(true);
        when(user.getWorld()).thenReturn(world);
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(),anyString(),anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Util
        PowerMockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        when(Util.getWorld(any())).thenReturn(world);
        // RanksManager
        rm = new RanksManager();
        when(plugin.getRanksManager()).thenReturn(rm);
        // Commands Manager
        when(plugin.getCommandsManager()).thenReturn(cm);
        Map<String, CompositeCommand> map = new HashMap<>();
        CompositeCommand cc = mock(CompositeCommand.class);
        when(cc.getWorld()).thenReturn(world);
        when(cc.isConfigurableRankCommand()).thenReturn(true);
        when(cc.getName()).thenReturn("test");
        when(cc.getSubCommands()).thenReturn(Collections.emptyMap());
        map.put("test", cc);
        when(cm.getCommands()).thenReturn(map);
        crcl = new CommandRankClickListener();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClickWrongWorld() {
        when(user.inWorld()).thenReturn(false);
        assertTrue(crcl.onClick(panel, user, ClickType.LEFT, 0));
        verify(user).sendMessage("general.errors.wrong-world");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        assertTrue(crcl.onClick(panel, user, ClickType.LEFT, 0));
        verify(user).sendMessage("general.errors.no-permission", TextVariables.PERMISSION, "oneblock.settings.COMMAND_RANKS");
        verify(player).playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClickNoFlag() {
        when(island.isAllowed(user, Flags.CHANGE_SETTINGS)).thenReturn(false);
        assertTrue(crcl.onClick(panel, user, ClickType.LEFT, 0));
        verify(user).sendMessage("general.errors.insufficient-rank", TextVariables.RANK, "ranks.visitor");
        verify(player).playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClickDifferentPanelName() {
        when(panel.getName()).thenReturn("different");
        assertTrue(crcl.onClick(panel, user, ClickType.LEFT, 0));
        verify(inv, never()).setItem(eq(0), any());
        verify(user).closeInventory();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClick() {
        assertTrue(crcl.onClick(panel, user, ClickType.LEFT, 0));
        verify(inv).setItem(eq(0), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener#onClick(world.bentobox.bentobox.api.panels.Panel, world.bentobox.bentobox.api.user.User, org.bukkit.event.inventory.ClickType, int)}.
     */
    @Test
    public void testOnClickTooManyCommands() {
        Map<String, CompositeCommand> map = new HashMap<>();
        for (int i = 0; i < 55; i++) {
            CompositeCommand cc = mock(CompositeCommand.class);
            when(cc.getWorld()).thenReturn(world);
            when(cc.isConfigurableRankCommand()).thenReturn(true);
            when(cc.getName()).thenReturn("test" + i);
            when(cc.getSubCommands()).thenReturn(Collections.emptyMap());
            map.put("test" + i, cc);
        }
        when(cm.getCommands()).thenReturn(map);

        assertTrue(crcl.onClick(panel, user, ClickType.LEFT, 0));
        verify(plugin).logError("Number of rank setting commands is too big for GUI");
    }



    /**
     * Test method for {@link world.bentobox.bentobox.listeners.flags.clicklisteners.CommandRankClickListener#getPanelItem(java.lang.String, world.bentobox.bentobox.api.user.User, org.bukkit.World)}.
     */
    @Test
    public void testGetPanelItem() {
        assertTrue(crcl.onClick(panel, user, ClickType.LEFT, 0));
        PanelItem pi = crcl.getPanelItem("test", user, world);
        assertEquals(Material.MAP, pi.getItem().getType());
        assertEquals("protection.panel.flag-item.description-layout", pi.getDescription().get(0));
        assertEquals("protection.panel.flag-item.minimal-rankranks.member", pi.getDescription().get(1));
        assertEquals("protection.panel.flag-item.allowed-rankranks.sub-owner", pi.getDescription().get(2));
        assertEquals("protection.panel.flag-item.allowed-rankranks.owner", pi.getDescription().get(3));
        assertTrue(pi.getClickHandler().isPresent());
        assertEquals("test", pi.getName());
    }

}
