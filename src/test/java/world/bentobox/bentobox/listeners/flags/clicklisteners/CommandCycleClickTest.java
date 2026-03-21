package world.bentobox.bentobox.listeners.flags.clicklisteners;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Sound;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.TabbedPanel;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.panels.settings.SettingsTab;
import world.bentobox.bentobox.util.Util;

class CommandCycleClickTest extends RanksManagerTestSetup {

    @Mock
    private User user;
    @Mock
    private TabbedPanel panel;
    @Mock
    private @NonNull Inventory inv;
    @Mock
    private GameModeAddon gma;
    @Mock
    private CommandsManager cm;
    @Mock
    private SettingsTab tab;
    @Mock
    private CommandRankClickListener commandRankClickListener;

    private CommandCycleClick ccc;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Island
        when(island.getOwner()).thenReturn(uuid);
        when(island.isAllowed(user, Flags.CHANGE_SETTINGS)).thenReturn(true);
        when(island.getRankCommand(anyString())).thenReturn(RanksManager.MEMBER_RANK);
        // IM
        when(im.getIsland(world, uuid)).thenReturn(island);
        when(im.getIsland(world, user)).thenReturn(island);
        // IWM
        when(iwm.getAddon(any())).thenReturn(Optional.of(gma));
        when(iwm.getPermissionPrefix(world)).thenReturn("oneblock.");
        when(iwm.getHiddenFlags(any())).thenReturn(new ArrayList<>());
        // Panel
        when(panel.getInventory()).thenReturn(inv);
        when(panel.getWorld()).thenReturn(Optional.of(world));
        when(panel.getIsland()).thenReturn(island);
        when(panel.getActiveTab()).thenReturn(tab);
        // Tab
        when(tab.getIsland()).thenReturn(island);
        // User
        when(user.isOp()).thenReturn(false);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getPlayer()).thenReturn(mockPlayer);
        when(user.inWorld()).thenReturn(true);
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);
        when(user.getTranslation(anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(anyString(), anyString(), anyString()))
                .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);
        // Commands Manager
        when(plugin.getCommandsManager()).thenReturn(cm);
        Map<String, CompositeCommand> map = new HashMap<>();
        CompositeCommand cc = mock(CompositeCommand.class);
        when(cc.getWorld()).thenReturn(world);
        when(cc.isConfigurableRankCommand()).thenReturn(true);
        when(cc.getName()).thenReturn("test");
        when(cc.getSubCommands()).thenReturn(Collections.emptyMap());
        when(cc.testPermission(any())).thenReturn(true);
        map.put("test", cc);
        when(cm.getCommands()).thenReturn(map);
        // CommandRankClickListener mock returns a PanelItem
        PanelItem panelItem = mock(PanelItem.class);
        when(panelItem.getItem()).thenReturn(mock(ItemStack.class));
        when(commandRankClickListener.getPanelItem(anyString(), any(), any())).thenReturn(panelItem);
        // RanksManager
        when(rm.getRankUpValue(RanksManager.MEMBER_RANK)).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(rm.getRankDownValue(RanksManager.MEMBER_RANK)).thenReturn(RanksManager.COOP_RANK);

        ccc = new CommandCycleClick(commandRankClickListener, "test");
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testOnClickLeftIncreaseRank() {
        assertTrue(ccc.onClick(panel, user, ClickType.LEFT, 0));
        verify(island).setRankCommand("test", RanksManager.SUB_OWNER_RANK);
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
        verify(inv).setItem(eq(0), any());
    }

    @Test
    void testOnClickLeftWrapAroundFromOwner() {
        when(island.getRankCommand("test")).thenReturn(RanksManager.OWNER_RANK);
        assertTrue(ccc.onClick(panel, user, ClickType.LEFT, 0));
        verify(island).setRankCommand("test", RanksManager.MEMBER_RANK);
    }

    @Test
    void testOnClickRightDecreaseRank() {
        when(island.getRankCommand("test")).thenReturn(RanksManager.SUB_OWNER_RANK);
        when(rm.getRankDownValue(RanksManager.SUB_OWNER_RANK)).thenReturn(RanksManager.MEMBER_RANK);
        assertTrue(ccc.onClick(panel, user, ClickType.RIGHT, 0));
        verify(island).setRankCommand("test", RanksManager.MEMBER_RANK);
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1F, 1F);
    }

    @Test
    void testOnClickRightWrapAroundFromMember() {
        // MEMBER_RANK is the lowest, so it wraps to OWNER_RANK
        when(island.getRankCommand("test")).thenReturn(RanksManager.MEMBER_RANK);
        assertTrue(ccc.onClick(panel, user, ClickType.RIGHT, 0));
        verify(island).setRankCommand("test", RanksManager.OWNER_RANK);
    }

    @Test
    void testOnClickNotAllowed() {
        when(island.isAllowed(user, Flags.CHANGE_SETTINGS)).thenReturn(false);
        assertTrue(ccc.onClick(panel, user, ClickType.LEFT, 0));
        verify(island, never()).setRankCommand(anyString(), eq(RanksManager.SUB_OWNER_RANK));
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
    }

    @Test
    void testOnClickNullIsland() {
        when(panel.getIsland()).thenReturn(null);
        assertTrue(ccc.onClick(panel, user, ClickType.LEFT, 0));
        verify(island, never()).setRankCommand(anyString(), eq(RanksManager.SUB_OWNER_RANK));
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
    }

    @Test
    void testOnClickNullOwner() {
        when(island.getOwner()).thenReturn(null);
        assertTrue(ccc.onClick(panel, user, ClickType.LEFT, 0));
        verify(island, never()).setRankCommand(anyString(), eq(RanksManager.SUB_OWNER_RANK));
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_METAL_HIT, 1F, 1F);
    }

    @Test
    void testOnClickShiftLeftOp() {
        when(user.isOp()).thenReturn(true);
        assertTrue(ccc.onClick(panel, user, ClickType.SHIFT_LEFT, 0));
        // Should add COMMAND_RANK:test to hidden flags
        List<String> hiddenFlags = iwm.getHiddenFlags(world);
        assertTrue(hiddenFlags.contains("COMMAND_RANK:test"));
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_GLASS_BREAK, 1F, 1F);
        verify(gma).saveWorldSettings();
    }

    @Test
    void testOnClickShiftLeftOpToggleOff() {
        when(user.isOp()).thenReturn(true);
        List<String> hiddenFlags = new ArrayList<>();
        hiddenFlags.add("COMMAND_RANK:test");
        when(iwm.getHiddenFlags(any())).thenReturn(hiddenFlags);
        assertTrue(ccc.onClick(panel, user, ClickType.SHIFT_LEFT, 0));
        // Should remove COMMAND_RANK:test from hidden flags
        assertTrue(hiddenFlags.isEmpty());
        verify(mockPlayer).playSound(user.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1F, 1F);
    }

    @Test
    void testOnClickShiftLeftNonOp() {
        when(user.isOp()).thenReturn(false);
        assertTrue(ccc.onClick(panel, user, ClickType.SHIFT_LEFT, 0));
        // Should not modify hidden flags
        verify(gma, never()).saveWorldSettings();
    }

    @Test
    void testOnClickMiddleClickIgnored() {
        assertTrue(ccc.onClick(panel, user, ClickType.MIDDLE, 0));
        verify(island, never()).setRankCommand(anyString(), eq(RanksManager.SUB_OWNER_RANK));
        verify(inv).setItem(eq(0), any());
    }
}
