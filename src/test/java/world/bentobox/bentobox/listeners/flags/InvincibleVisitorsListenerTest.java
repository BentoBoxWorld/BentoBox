package world.bentobox.bentobox.listeners.flags;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.panels.Panel;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BentoBox.class, Util.class, Bukkit.class })
public class InvincibleVisitorsListenerTest {

    private IslandWorldManager iwm;
    private InvincibleVisitorsListener listener;
    private Panel panel;
    private User user;
    private IslandsManager im;
    private List<String> ivSettings;
    private Player player;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Island World Manager
        iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(Mockito.any())).thenReturn(true);
        when(iwm.getPermissionPrefix(Mockito.any())).thenReturn("bskyblock");
        when(plugin.getIWM()).thenReturn(iwm);

        listener = new InvincibleVisitorsListener();

        panel = mock(Panel.class);
        when(panel.getInventory()).thenReturn(mock(Inventory.class));
        when(panel.getName()).thenReturn("panel");
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.inWorld()).thenReturn(true);
        when(user.getWorld()).thenReturn(mock(World.class));
        when(user.getLocation()).thenReturn(mock(Location.class));
        player = mock(Player.class);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);
        when(user.getTranslation(Mockito.anyString())).thenReturn("panel");
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        FlagsManager fm = mock(FlagsManager.class);
        Flag flag = mock(Flag.class);
        when(flag.isSetForWorld(Mockito.any())).thenReturn(false);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(mock(ItemStack.class));
        when(flag.toPanelItem(Mockito.any(), Mockito.eq(user))).thenReturn(item);
        when(fm.getFlagByID(Mockito.anyString())).thenReturn(flag);
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Island Manager
        im = mock(IslandsManager.class);
        Island island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(im.getIsland(Mockito.any(World.class), Mockito.any(User.class))).thenReturn(island);
        // Visitor
        when(im.userIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(false);
        when(plugin.getIslands()).thenReturn(im);

        // IV Settings
        ivSettings = new ArrayList<>();
        ivSettings.add(EntityDamageEvent.DamageCause.CRAMMING.name());
        ivSettings.add(EntityDamageEvent.DamageCause.VOID.name());
        when(iwm.getIvSettings(Mockito.any())).thenReturn(ivSettings);

        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta imeta = mock(ItemMeta.class);
        when(itemF.getItemMeta(Mockito.any())).thenReturn(imeta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);

        Inventory top = mock(Inventory.class);
        when(top.getSize()).thenReturn(9);
        when(panel.getInventory()).thenReturn(top);

        when(Bukkit.createInventory(Mockito.any(), Mockito.anyInt(), Mockito.any())).thenReturn(top);

    }

    @Test
    public void testOnClickWrongWorld() {
        when(user.inWorld()).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(user).sendMessage("general.errors.wrong-world");
    }

    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.admin.settings.INVINCIBLE_VISITORS");
    }

    @Test
    public void testOnClickNotIVPanel() {
        ClickType clickType = ClickType.LEFT;
        int slot = 5;
        when(panel.getName()).thenReturn("not_panel");
        listener.onClick(panel, user, clickType, slot );
        // Should open inv visitors
        Mockito.verify(user).closeInventory();
        Mockito.verify(player).openInventory(Mockito.any(Inventory.class));
    }

    @Test
    public void testOnClickIVPanel() {
        ClickType clickType = ClickType.LEFT;
        int slot = 5; // FALL damage
        when(panel.getName()).thenReturn("panel");

        // IV settings should be empty
        assertFalse(ivSettings.contains("FALL"));
        // Click on the FALL icon
        listener.onClick(panel, user, clickType, slot );
        // Should keep panel open
        Mockito.verify(user, Mockito.never()).closeInventory();
        // IV settings should now have FALL in it
        assertTrue(ivSettings.contains("FALL"));

        // Click on it again
        listener.onClick(panel, user, clickType, slot );
        // Should keep panel open
        Mockito.verify(user, Mockito.never()).closeInventory();
        // IV settings should not have FALL in it anymore
        assertFalse(ivSettings.contains("FALL"));


    }

    @Test
    public void testOnVisitorGetDamageNotPlayer() {
        LivingEntity le = mock(LivingEntity.class);
        EntityDamageEvent e = new EntityDamageEvent(le, EntityDamageEvent.DamageCause.CRAMMING, 0D);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotInWorld() {
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CRAMMING, 0D);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotInIvSettings() {
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.BLOCK_EXPLOSION, 0D);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotVisitor() {
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CRAMMING, 0D);
        when(im.userIsOnIsland(Mockito.any(), Mockito.any())).thenReturn(true);
        listener.onVisitorGetDamage(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testOnVisitorGetDamageNotVoid() {
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CRAMMING, 0D);
        listener.onVisitorGetDamage(e);
        assertTrue(e.isCancelled());
        Mockito.verify(player, Mockito.never()).setGameMode(Mockito.eq(GameMode.SPECTATOR));
    }

    @Test
    public void testOnVisitorGetDamageVoid() {
        // For testing, have no island to teleport to
        when(im.getIslandAt(Mockito.any())).thenReturn(Optional.empty());
        EntityDamageEvent e = new EntityDamageEvent(player, EntityDamageEvent.DamageCause.VOID, 0D);
        listener.onVisitorGetDamage(e);
        assertTrue(e.isCancelled());
        Mockito.verify(player).setGameMode(Mockito.eq(GameMode.SPECTATOR));
        Mockito.verify(im).getIslandAt(Mockito.any());
    }

}
