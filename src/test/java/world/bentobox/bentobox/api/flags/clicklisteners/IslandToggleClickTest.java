package world.bentobox.bentobox.api.flags.clicklisteners;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
@PrepareForTest({BentoBox.class, Util.class })
public class IslandToggleClickTest {

    private IslandWorldManager iwm;
    private IslandToggleClick listener;
    private Panel panel;
    private User user;
    private Flag flag;
    private IslandsManager im;
    private Island island;
    private UUID uuid;

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

        listener = new IslandToggleClick("test");

        panel = mock(Panel.class);
        when(panel.getInventory()).thenReturn(mock(Inventory.class));
        // Sometimes use Mockito.withSettings().verboseLogging()
        user = mock(User.class);
        when(user.getWorld()).thenReturn(mock(World.class));
        when(user.getLocation()).thenReturn(mock(Location.class));
        when(user.getPlayer()).thenReturn(mock(Player.class));
        when(user.hasPermission(Mockito.anyString())).thenReturn(true);
        uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        FlagsManager fm = mock(FlagsManager.class);
        flag = mock(Flag.class);
        when(flag.isSetForWorld(Mockito.any())).thenReturn(false);
        PanelItem item = mock(PanelItem.class);
        when(item.getItem()).thenReturn(mock(ItemStack.class));
        when(flag.toPanelItem(Mockito.any(), Mockito.eq(user))).thenReturn(item);
        when(fm.getFlagByID(Mockito.anyString())).thenReturn(flag);
        when(plugin.getFlagsManager()).thenReturn(fm);

        // Island Manager
        im = mock(IslandsManager.class);
        island = mock(Island.class);
        when(island.getOwner()).thenReturn(uuid);
        when(im.getIsland(Mockito.any(World.class), Mockito.any(User.class))).thenReturn(island);
        when(plugin.getIslands()).thenReturn(im);
    }

    @Test
    public void testOnClickWrongWorld() {
        when(iwm.inWorld(Mockito.any())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(user).sendMessage("general.errors.wrong-world");
    }

    @Test
    public void testOnClickNoPermission() {
        when(user.hasPermission(Mockito.anyString())).thenReturn(false);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(user).sendMessage("general.errors.no-permission", "[permission]", "bskyblock.settings.test");
    }

    @Test
    public void testOnClick() {
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(island).toggleFlag(flag);
    }

    @Test
    public void testOnClickNoIsland() {
        when(im.getIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(null);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(island, Mockito.never()).toggleFlag(flag);
    }

    @Test
    public void testOnClickNotOwner() {
        // Pick a different UUID from owner
        UUID u = UUID.randomUUID();
        while(u.equals(uuid)) {
            u = UUID.randomUUID();
        }
        when(island.getOwner()).thenReturn(u);
        listener.onClick(panel, user, ClickType.LEFT, 0);
        Mockito.verify(island, Mockito.never()).toggleFlag(flag);
    }

}
