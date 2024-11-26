package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.flags.InvincibleVistorFlagDamageRemovalEvent;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.mocks.ServerMocks;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class InvincibleVisitorsListenerTest {

    @Mock
    private IslandWorldManager iwm;
    private InvincibleVisitorsListener listener;
    @Mock
    private Panel panel;
    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    private List<String> ivSettings;
    @Mock
    private Player player;
    private Optional<Island> optionalIsland;
    @Mock
    private GameModeAddon addon;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private PluginManager pim;

    @Before
    public void setUp() throws Exception {
        ServerMocks.newServer();
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.getPermissionPrefix(any())).thenReturn("bskyblock.");
        Optional<GameModeAddon> optionalAddon = Optional.of(addon);
        when(iwm.getAddon(any())).thenReturn(optionalAddon);
        when(plugin.getIWM()).thenReturn(iwm);

        listener = new InvincibleVisitorsListener();

        when(panel.getInventory()).thenReturn(mock(Inventory.class));
        when(panel.getName()).thenReturn("panel");
        Map<Integer, PanelItem> map = new HashMap<>();
        List<String> sortedNames = Arrays.stream(EntityDamageEvent.DamageCause.values()).map(DamageCause::name)
                .map(Util::prettifyText).sorted().toList();
        int i = 0;
        for (String name : sortedNames) {
            PanelItem pi = mock(PanelItem.class);
            when(pi.getName()).thenReturn(name);
            map.put(i++, pi);
        }
        when(panel.getItems()).thenReturn(map);
        // Sometimes use Mockito.withSettings().verboseLogging()
        when(user.inWorld()).thenReturn(true);
        when(user.getWorld()).thenReturn(world);
        when(player.getWorld()).thenReturn(world);
        when(location.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);
        when(player.getLocation()).thenReturn(location);
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        when(user.getTranslation(anyString())).thenReturn("panel");
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(player.getUniqueId()).thenReturn(uuid);
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(mock(World.class));
        when(Util.prettifyText(anyString())).thenCallRealMethod();
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();
        // Util translate color codes (used in user translate methods)
        when(Util.translateColorCodes(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        FlagsManager fm = mock(FlagsManager.class);
        Flag flag = mock(Flag.class);
        when(flag.isSetForWorld(any())).thenReturn(false);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(mock(ItemStack.class));
        when(flag.toPanelItem(any(), eq(user), any(), any(), eq(false))).thenReturn(item);
        when(fm.getFlag(anyString())).thenReturn(Optional.of(flag));
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Island Manager
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        @Nullable
        Location location = mock(Location.class);
        Vector vector = mock(Vector.class);
        when(location.toVector()).thenReturn(vector);
        when(island.getProtectionCenter()).thenReturn(location);
        when(im.getIsland(any(World.class), any(User.class))).thenReturn(island);
        optionalIsland = Optional.of(island);
        // Visitor
        when(im.userIsOnIsland(any(), any())).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // IV Settings
        ivSettings = new ArrayList<>();
        ivSettings.add(EntityDamageEvent.DamageCause.CRAMMING.name());
        ivSettings.add(EntityDamageEvent.DamageCause.VOID.name());
        when(iwm.getIvSettings(any())).thenReturn(ivSettings);

        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta imeta = mock(ItemMeta.class);
        when(itemF.getItemMeta(any())).thenReturn(imeta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        Inventory top = mock(Inventory.class);
        when(top.getSize()).thenReturn(9);
        when(panel.getInventory()).thenReturn(top);

        when(Bukkit.createInventory(any(), anyInt(), any())).thenReturn(top);
    }

    @After
    public void tearDown() {
        ServerMocks.unsetBukkitServer();
        User.clearUsers();
        framework().clearInlineMocks();
    }

    @Test
    public void testOnClickWrongWorld() {
        when(user.inWorld()).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(user).sendMessage("general.errors.wrong-world");
    }

    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(anyString())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.admin.settings.INVINCIBLE_VISITORS");
    }

    @Test
    public void testOnClickNotIVPanel() {
        ClickType clickType = ClickType.LEFT;
        int slot = 5;
        when(panel.getName()).thenReturn("not_panel");
        listener.onClick(panel, user, clickType, slot );
        // Should open inv visitors
        verify(user).closeInventory();
        verify(player).openInventory(any(Inventory.class));
    }

    @Test
    public void testOnClickIVPanel() {
        ClickType clickType = ClickType.LEFT;
        ivSettings.clear();
        when(panel.getName()).thenReturn("panel");
        // Make the panel

        // Test all damage causes to make sure they can be clicked on and off
        for (int slot = 0; slot < DamageCause.values().length; slot++) {
            // Get the damage type
            DamageCause dc = Arrays.stream(EntityDamageEvent.DamageCause.values()).sorted(Comparator.comparing(DamageCause::name)).toList().get(slot);
            // IV settings should be empty
            assertFalse(ivSettings.contains(dc.name()));
            // Click on the icon
            listener.onClick(panel, user, clickType, slot);
            // Should keep panel open
            verify(user, never()).closeInventory();
            // IV settings should now have the damage cause in it
            assertTrue(ivSettings.contains(dc.name()));

            // Click on it again
            listener.onClick(panel, user, clickType, slot );
            // Should keep panel open
            verify(user, never()).closeInventory();
            // IV settings should not have the damage cause in it anymore
            assertFalse(ivSettings.contains(dc.name()));
        }
        // The values should be saved twice because there are two clicks
        verify(addon, times(DamageCause.values().length * 2)).saveWorldSettings();
    }

    @Test
    public void testOnVisitorGetDamageNotPlayer() {
        LivingEntity le = mock(LivingEntity.class);
        EntityDamageEvent e = new EntityDamageEvent(le, EntityDamageEvent.DamageCause.CRAMMING, null, 0D);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotInWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CRAMMING, null, 0D);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotInIvSettings() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, null, 0D);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotVisitor() {
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CRAMMING, null, 0D);
        when(im.userIsOnIsland(any(), any())).thenReturn(true);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotVoid() {
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CRAMMING, null, 0D);
        listener.onVisitorGetDamage(e);
        assertTrue(e.isCancelled());
        verify(player, never()).setGameMode(eq(GameMode.SPECTATOR));
        verify(pim).callEvent(any(InvincibleVistorFlagDamageRemovalEvent.class));
    }

    @Test
    public void testOnVisitorGetDamageNPC() {
        when(player.hasMetadata(eq("NPC"))).thenReturn(true);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CRAMMING, null, 0D);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }


    @Test
    public void testOnVisitorGetDamageVoidIslandHere() {
        when(im.getIslandAt(any())).thenReturn(optionalIsland);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.VOID, null, 0D);
        // Player should be teleported to this island
        listener.onVisitorGetDamage(e);
        assertTrue(e.isCancelled());
        verify(pim).callEvent(any(InvincibleVistorFlagDamageRemovalEvent.class));
    }

    @Test
    public void testOnVisitorGetDamageVoidNoIslandHerePlayerHasNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(false);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.VOID, null, 0D);
        // Player should die
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
        verify(pim).callEvent(any(InvincibleVistorFlagDamageRemovalEvent.class));
    }

    @Test
    public void testOnVisitorGetDamageVoidPlayerHasIsland() {
        // No island at this location
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        // Player has an island
        when(im.hasIsland(any(), any(UUID.class))).thenReturn(true);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.VOID, null, 0D);
        // Player should be teleported to their island
        listener.onVisitorGetDamage(e);
        assertTrue(e.isCancelled());
        verify(im).homeTeleportAsync(any(), eq(player));
        verify(pim).callEvent(any(InvincibleVistorFlagDamageRemovalEvent.class));
    }
}
