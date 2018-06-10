package us.tastybento.bskyblock.api.flags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.configuration.WorldSettings;
import us.tastybento.bskyblock.api.panels.PanelItem;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.managers.IslandWorldManager;
import us.tastybento.bskyblock.managers.IslandsManager;
import us.tastybento.bskyblock.managers.RanksManager;
import us.tastybento.bskyblock.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BSkyBlock.class, Util.class, Bukkit.class })
public class FlagTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BSkyBlock plugin = mock(BSkyBlock.class);
        Whitebox.setInternalState(BSkyBlock.class, "instance", plugin);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        // World Settings
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        
        PowerMockito.mockStatic(Bukkit.class);
        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta im = mock(ItemMeta.class);
        when(itemF.getItemMeta(Mockito.any())).thenReturn(im);
        when(Bukkit.getItemFactory()).thenReturn(itemF);
        
    }

    @Test
    public void testHashCode() {
        Flag flag1 = new Flag(null, null, null, false, null, 0, null);
        Flag flag2 = new Flag(null, null, null, false, null, 0, null);
        assertTrue(flag1.hashCode() == flag2.hashCode());
    }

    @Test
    public void testFlag() {
        assertNotNull(new Flag(null, null, null, false, null, 0, null));
    }

    @Test
    public void testGetID() {
        Flag id = new Flag("id", null, null, false, null, 0, null);
        assertEquals("id", id.getID());
    }

    @Test
    public void testGetIcon() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, false, null, 0, null);
        assertEquals(Material.ACACIA_DOOR, id.getIcon());
    }

    @Test
    public void testGetListener() {
        Listener l = mock(Listener.class);
        Flag id = new Flag("id", Material.ACACIA_DOOR, l, false, null, 0, null);
        Optional<Listener> ol = Optional.ofNullable(l);
        assertEquals(ol, id.getListener());
        id = new Flag("id", Material.ACACIA_DOOR, null, false, null, 0, null);
        assertEquals(Optional.empty(), id.getListener());
    }

    @Test
    public void testIsDefaultSetting() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, false, null, 0, null);
        assertFalse(id.isSetForWorld(mock(World.class)));
        id = new Flag("id", Material.ACACIA_DOOR, null, true, null, 0, null);
        assertTrue(id.isSetForWorld(mock(World.class)));
    }

    @Test
    public void testSetDefaultSetting() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, false, null, 0, null);
        assertFalse(id.isSetForWorld(mock(World.class)));
        id.setDefaultSetting(true);
        assertTrue(id.isSetForWorld(mock(World.class)));
        id.setDefaultSetting(false);
        assertFalse(id.isSetForWorld(mock(World.class)));
        
    }

    @Test
    public void testGetType() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, false, Flag.Type.PROTECTION, 0, null);
        assertEquals(Flag.Type.PROTECTION,id.getType());
        id = new Flag("id", Material.ACACIA_DOOR, null, false, Flag.Type.SETTING, 0, null);
        assertEquals(Flag.Type.SETTING,id.getType());
    }

    @Test
    public void testGetDefaultRank() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, false, Flag.Type.PROTECTION, 100, null);
        assertEquals(100, id.getDefaultRank());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsObject() {
        Flag flag1 = null;
        Flag flag2 = new Flag(null, null, null, false, null, 0, null);
        
        assertFalse(flag2.equals(null));
        int i = 45;
        assertFalse(flag2.equals(i));
        
        flag1 = new Flag(null, null, null, false, null, 0, null);
        flag2 = flag1;
        assertTrue(flag1.equals(flag2));
        assertTrue(flag2.equals(flag1));
        
        flag2 = new Flag("id", Material.ACACIA_DOOR, null, false, Flag.Type.PROTECTION, 0, null);
        assertFalse(flag1.equals(flag2));
        assertFalse(flag2.equals(flag1));
        
    }

    @Test
    public void testToPanelItem() throws Exception {
        BSkyBlock plugin = mock(BSkyBlock.class);
        
        IslandsManager im = mock(IslandsManager.class);
        
        Island island = mock(Island.class);
        when(island.getFlag(Mockito.any())).thenReturn(RanksManager.VISITOR_RANK);
        
        User user = mock(User.class, Mockito.withSettings().verboseLogging());
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Answer<String> answer = new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                StringBuilder sb = new StringBuilder();                
                Arrays.stream(invocation.getArguments()).forEach(sb::append);
                sb.append("mock");
                return sb.toString();
            }
            
        };
        
        when(user.getTranslation(Mockito.anyVararg())).thenAnswer(answer);
        when(user.getTranslation(Mockito.any(),Mockito.any(),Mockito.any())).thenAnswer(answer);
        
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(im.getIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(island);
        Optional<Island> oL = Optional.ofNullable(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oL);
        when(plugin.getIslands()).thenReturn(im);
        
        RanksManager rm = mock(RanksManager.class);
        when(plugin.getRanksManager()).thenReturn(rm);
        when(rm.getRank(Mockito.eq(RanksManager.VISITOR_RANK))).thenReturn("Visitor");
        when(rm.getRank(Mockito.eq(RanksManager.OWNER_RANK))).thenReturn("Owner");
        
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, false, Flag.Type.PROTECTION, 0, null);
        
        PanelItem pi = id.toPanelItem(plugin, user);
        
        verify(user).getTranslation(Mockito.eq("protection.flags.id.name"));
        verify(user).getTranslation(Mockito.eq("protection.panel.flag-item.name-layout"), Mockito.anyVararg());
        
        assertEquals(Material.ACACIA_DOOR, pi.getItem().getType());
        
    }

    @Test
    public void testToString() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, false, Flag.Type.PROTECTION, 0, null);
        assertEquals("Flag [id=id, icon=ACACIA_DOOR, listener=null, type=PROTECTION, defaultSetting=false, defaultRank=0, clickHandler=null]", id.toString());
    }

    @Test
    public void testCompareTo() {
        Flag aaa = new Flag("AAA", Material.ACACIA_DOOR, null, false, Flag.Type.PROTECTION, 0, null);
        Flag bbb = new Flag("BBB", Material.ACACIA_DOOR, null, false, Flag.Type.PROTECTION, 0, null);
        assertTrue(aaa.compareTo(bbb) < bbb.compareTo(aaa));
        assertTrue(aaa.compareTo(aaa) == 0);
    }

}
