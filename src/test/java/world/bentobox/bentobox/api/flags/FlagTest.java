package world.bentobox.bentobox.api.flags;

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
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Util.class, Bukkit.class })
public class FlagTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

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
        Flag flag1 = new Flag(null, null, null, null, 0, null, false);
        Flag flag2 = new Flag(null, null, null, null, 0, null, false);
        assertTrue(flag1.hashCode() == flag2.hashCode());
    }

    @Test
    public void testFlag() {
        assertNotNull(new Flag(null, null, null, null, 0, null, false));
    }

    @Test
    public void testGetID() {
        Flag id = new Flag("id", null, null, null, 0, null, false);
        assertEquals("id", id.getID());
    }

    @Test
    public void testGetIcon() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, null, 0, null, false);
        assertEquals(Material.ACACIA_DOOR, id.getIcon());
    }

    @Test
    public void testGetListener() {
        Listener l = mock(Listener.class);
        Flag id = new Flag("id", Material.ACACIA_DOOR, l, null, 0, null, false);
        Optional<Listener> ol = Optional.ofNullable(l);
        assertEquals(ol, id.getListener());
        id = new Flag("id", Material.ACACIA_DOOR, null, null, 0, null, false);
        assertEquals(Optional.empty(), id.getListener());
    }

    @Test
    public void testIsDefaultSetting() {
        Type type = Type.SETTING;
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, type , 0, null, false);
        assertFalse(id.isSetForWorld(mock(World.class)));
        id = new Flag("id", Material.ACACIA_DOOR, null, type, 0, null, false);
        id.setDefaultSetting(true);
        assertTrue(id.isSetForWorld(mock(World.class)));
    }

    @Test
    public void testSetDefaultSetting() {
        Type type = Type.SETTING;
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, type, 0, null, false);
        assertFalse(id.isSetForWorld(mock(World.class)));
        id.setDefaultSetting(true);
        assertTrue(id.isSetForWorld(mock(World.class)));
        id.setDefaultSetting(false);
        assertFalse(id.isSetForWorld(mock(World.class)));
        
    }
    
    @Test
    public void testIsDefaultSetting_World_Setting() {
        Type type = Type.WORLD_SETTING;
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, type , 0, null, false);
        assertFalse(id.isSetForWorld(mock(World.class)));
        // Default can only be set once with world settings, so use a new id for flag
        id = new Flag("id2", Material.ACACIA_DOOR, null, type, 0, null, false);
        id.setDefaultSetting(true);
        assertTrue(id.isSetForWorld(mock(World.class)));
    }

    @Test
    public void testGetType() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, Flag.Type.PROTECTION, 0, null, false);
        assertEquals(Flag.Type.PROTECTION,id.getType());
        id = new Flag("id", Material.ACACIA_DOOR, null, Flag.Type.SETTING, 0, null, false);
        assertEquals(Flag.Type.SETTING,id.getType());
    }

    @Test
    public void testGetDefaultRank() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, Flag.Type.PROTECTION, 100, null, false);
        assertEquals(100, id.getDefaultRank());
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsObject() {
        Flag flag1 = null;
        Flag flag2 = new Flag(null, null, null, null, 0, null, false);
        
        assertFalse(flag2.equals(null));
        int i = 45;
        assertFalse(flag2.equals(i));
        
        flag1 = new Flag(null, null, null, null, 0, null, false);
        flag2 = flag1;
        assertTrue(flag1.equals(flag2));
        assertTrue(flag2.equals(flag1));
        
        flag2 = new Flag("id", Material.ACACIA_DOOR, null, Flag.Type.PROTECTION, 0, null, false);
        assertFalse(flag1.equals(flag2));
        assertFalse(flag2.equals(flag1));
        
    }

    @Test
    public void testToPanelItem() throws Exception {
        BentoBox plugin = mock(BentoBox.class);
        
        IslandsManager im = mock(IslandsManager.class);
        
        Island island = mock(Island.class);
        when(island.getFlag(Mockito.any())).thenReturn(RanksManager.VISITOR_RANK);
        
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Answer<String> answer = invocation -> {
            StringBuilder sb = new StringBuilder();
            Arrays.stream(invocation.getArguments()).forEach(sb::append);
            sb.append("mock");
            return sb.toString();
        };
        
        when(user.getTranslation(Mockito.anyVararg())).thenAnswer(answer);
        when(user.getTranslation(Mockito.any(),Mockito.any(),Mockito.any())).thenAnswer(answer);
        
        when(im.getIsland(Mockito.any(), Mockito.any(UUID.class))).thenReturn(island);
        when(im.getIsland(Mockito.any(), Mockito.any(User.class))).thenReturn(island);
        Optional<Island> oL = Optional.of(island);
        when(im.getIslandAt(Mockito.any(Location.class))).thenReturn(oL);
        when(plugin.getIslands()).thenReturn(im);
        
        RanksManager rm = mock(RanksManager.class);
        when(plugin.getRanksManager()).thenReturn(rm);
        when(rm.getRank(Mockito.eq(RanksManager.VISITOR_RANK))).thenReturn("Visitor");
        when(rm.getRank(Mockito.eq(RanksManager.OWNER_RANK))).thenReturn("Owner");
        
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, Flag.Type.PROTECTION, 0, null, false);
        
        PanelItem pi = id.toPanelItem(plugin, user);
        
        verify(user).getTranslation(Mockito.eq("protection.flags.id.name"));
        verify(user).getTranslation(Mockito.eq("protection.panel.flag-item.name-layout"), Mockito.anyVararg());
        
        assertEquals(Material.ACACIA_DOOR, pi.getItem().getType());
        
    }

    @Test
    public void testToString() {
        Flag id = new Flag("id", Material.ACACIA_DOOR, null, Flag.Type.PROTECTION, 0, null, false);
        assertEquals("Flag [id=id, icon=ACACIA_DOOR, listener=null, type=PROTECTION, defaultSetting=false, defaultRank=0, clickHandler=null, subPanel=false]", id.toString());
    }

    @Test
    public void testCompareTo() {
        Flag aaa = new Flag("AAA", Material.ACACIA_DOOR, null, Flag.Type.PROTECTION, 0, null, false);
        Flag bbb = new Flag("BBB", Material.ACACIA_DOOR, null, Flag.Type.PROTECTION, 0, null, false);
        assertTrue(aaa.compareTo(bbb) < bbb.compareTo(aaa));
        assertTrue(aaa.compareTo(aaa) == 0);
    }

}
