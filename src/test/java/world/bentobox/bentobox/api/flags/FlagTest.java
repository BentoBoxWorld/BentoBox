package world.bentobox.bentobox.api.flags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
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
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Util.class, Bukkit.class, RanksManager.class })
public class FlagTest {

    private Flag f;
    @Mock
    private Listener listener;
    @Mock
    private World world;
    private Map<String, Boolean> worldFlags;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private BentoBox plugin;
    @Mock
    private LocalesManager lm;

    /**
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        PowerMockito.mockStatic(Util.class);
        // Return world
        when(Util.getWorld(any())).thenAnswer((Answer<World>) invocation -> invocation.getArgument(0, World.class));

        // World Settings
        when(plugin.getIWM()).thenReturn(iwm);
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        GameModeAddon gma = mock(GameModeAddon.class);
        Optional<GameModeAddon> opGma = Optional.of(gma );
        when(iwm.getAddon(any())).thenReturn(opGma);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        ItemFactory itemF = mock(ItemFactory.class);
        ItemMeta im = mock(ItemMeta.class);
        when(itemF.getItemMeta(any())).thenReturn(im);
        when(Bukkit.getItemFactory()).thenReturn(itemF);
        
        // Locales manager
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Setting US text is successful
        when(lm.setTranslation(eq(Locale.US), anyString(), anyString())).thenReturn(true);

        // Flag
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).type(Flag.Type.PROTECTION).listener(listener).build();

    }

    /**
     */
    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#hashCode()}.
     */
    @Test
    public void testHashCode() {
        Flag flag1 = new Flag.Builder("id", Material.ACACIA_BOAT).build();
        Flag flag2 = new Flag.Builder("id", Material.ACACIA_BOAT).build();
        Flag flag3 = new Flag.Builder("id2", Material.ACACIA_BUTTON).build();
        assertTrue(flag1.hashCode() == flag2.hashCode());
        assertFalse(flag1.hashCode() == flag3.hashCode());
    }

    /**
     * Test method for .
     */
    @Test
    public void testFlag() {
        assertNotNull(f);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getID()}.
     */
    @Test
    public void testGetID() {
        assertEquals("flagID", f.getID());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getIcon()}.
     */
    @Test
    public void testGetIcon() {
        assertEquals(Material.ACACIA_PLANKS, f.getIcon());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getListener()}.
     */
    @Test
    public void testGetListener() {
        assertEquals(listener, f.getListener().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getListener()}.
     */
    @Test
    public void testGetListenerNone() {
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).build();
        assertEquals(Optional.empty(), f.getListener());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#isSetForWorld(org.bukkit.World)}.
     */
    @Test
    public void testIsSetForWorld() {
        assertFalse(f.isSetForWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#isSetForWorld(org.bukkit.World)}.
     */
    @Test
    public void testIsSetForWorldWorldSetting() {
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).type(Flag.Type.WORLD_SETTING).build();
        // Nothing in world flags
        assertFalse(f.isSetForWorld(world));
        worldFlags.put("flagID", true);
        assertTrue(f.isSetForWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setSetting(org.bukkit.World, boolean)}.
     */
    @Test
    public void testSetSetting() {
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).type(Flag.Type.WORLD_SETTING).build();
        assertTrue(worldFlags.isEmpty());
        f.setSetting(world, true);
        assertTrue(worldFlags.get("flagID"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setDefaultSetting(boolean)}.
     */
    @Test
    public void testSetDefaultSettingBoolean() {
        f.setDefaultSetting(true);
        // Checking will set it to the default
        assertTrue(f.isSetForWorld(world));
        f.setDefaultSetting(false);
        // Checking again will use the previous default
        assertTrue(f.isSetForWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setDefaultSetting(org.bukkit.World, boolean)}.
     */
    @Test
    public void testSetDefaultSettingWorldBoolean() {

        f.setDefaultSetting(world, true);
        assertTrue(f.isSetForWorld(world));
        f.setDefaultSetting(world, false);
        assertFalse(f.isSetForWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setDefaultSetting(org.bukkit.World, boolean)}.
     */
    @Test
    public void testSetDefaultSettingWorldBooleanNullWorldSettings() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        f.setDefaultSetting(world, true);
        verify(plugin).logError("Attempt to set default world setting for unregistered world. Register flags in onEnable.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals(Flag.Type.PROTECTION, f.getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getDefaultRank()}.
     */
    @Test
    public void testGetDefaultRank() {
        assertEquals(RanksManager.MEMBER_RANK, f.getDefaultRank());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#hasSubPanel()}.
     */
    @Test
    public void testHasSubPanel() {
        assertFalse(f.hasSubPanel());
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).type(Flag.Type.WORLD_SETTING).usePanel(true).build();
        assertTrue(f.hasSubPanel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        Flag flag1 = null;

        assertNotEquals(null, f);
        int i = 45;
        assertNotEquals(f, i);

        assertEquals(f, f);

        Flag f2 = new Flag.Builder("flagID2", Material.ACACIA_PLANKS).type(Flag.Type.WORLD_SETTING).usePanel(true).build();
        assertNotEquals(f, f2);
        assertNotEquals(f2, flag1);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getNameReference()}.
     */
    @Test
    public void testGetNameReference() {
        assertEquals("protection.flags.flagID.name", f.getNameReference());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getDescriptionReference()}.
     */
    @Test
    public void testGetDescriptionReference() {
        assertEquals("protection.flags.flagID.description", f.getDescriptionReference());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getHintReference()}.
     */
    @Test
    public void testGetHintReference() {
        assertEquals("protection.flags.flagID.hint", f.getHintReference());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getGameModes()}.
     */
    @Test
    public void testGetGameModes() {
        assertTrue(f.getGameModes().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setGameModes(java.util.Set)}.
     */
    @Test
    public void testSetGameModes() {
        Set<GameModeAddon> set = new HashSet<>();
        set.add(mock(GameModeAddon.class));
        assertTrue(f.getGameModes().isEmpty());
        f.setGameModes(set);
        assertFalse(f.getGameModes().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#addGameModeAddon(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testAddGameModeAddon() {
        GameModeAddon gameModeAddon = mock(GameModeAddon.class);
        f.addGameModeAddon(gameModeAddon);
        assertTrue(f.getGameModes().contains(gameModeAddon));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#removeGameModeAddon(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    public void testRemoveGameModeAddon() {
        GameModeAddon gameModeAddon = mock(GameModeAddon.class);
        f.addGameModeAddon(gameModeAddon);
        assertTrue(f.getGameModes().contains(gameModeAddon));
        f.removeGameModeAddon(gameModeAddon);
        assertTrue(f.getGameModes().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#toPanelItem(BentoBox, User, Island, boolean)}.
     */
    @Test
    public void testToPanelItem() {
        BentoBox plugin = mock(BentoBox.class);

        IslandsManager im = mock(IslandsManager.class);

        Island island = mock(Island.class);
        when(island.getFlag(any())).thenReturn(RanksManager.VISITOR_RANK);

        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Answer<String> answer = invocation -> {
            StringBuilder sb = new StringBuilder();
            Arrays.stream(invocation.getArguments()).forEach(sb::append);
            sb.append("mock");
            return sb.toString();
        };

        when(user.getTranslation(any(String.class),any(),any())).thenAnswer(answer);

        when(im.getIsland(any(), any(UUID.class))).thenReturn(island);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        Optional<Island> oL = Optional.of(island);
        when(im.getIslandAt(any(Location.class))).thenReturn(oL);
        when(plugin.getIslands()).thenReturn(im);

        PowerMockito.mockStatic(RanksManager.class);
        RanksManager rm = mock(RanksManager.class);
        when(RanksManager.getInstance()).thenReturn(rm);
        when(rm.getRank(RanksManager.VISITOR_RANK)).thenReturn("Visitor");
        when(rm.getRank(RanksManager.OWNER_RANK)).thenReturn("Owner");


        PanelItem pi = f.toPanelItem(plugin, user, world, island, false);

        verify(user).getTranslation("protection.flags.flagID.name");
        verify(user).getTranslation(eq("protection.panel.flag-item.name-layout"), any());

        assertEquals(Material.ACACIA_PLANKS, pi.getItem().getType());
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setTranslatedName(java.util.Locale, String)}.
     */
    @Test
    public void testSetTranslatedName() {
        assertFalse(f.setTranslatedName(Locale.CANADA, "Good eh?"));
        assertTrue(f.setTranslatedName(Locale.US, "Yihaa"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setTranslatedDescription(java.util.Locale, String)}.
     */
    @Test
    public void testSetTranslatedDescription() {
        assertFalse(f.setTranslatedDescription(Locale.CANADA, "Good eh?"));
        assertTrue(f.setTranslatedDescription(Locale.US, "Yihaa"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#toString()}.
     */
    @Test
    public void testToString() {
        assertEquals("Flag [id=flagID]", f.toString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#compareTo(world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testCompareTo() {
        Flag aaa = new Flag.Builder("AAA", Material.ACACIA_DOOR).type(Flag.Type.PROTECTION).build();
        Flag bbb = new Flag.Builder("BBB", Material.ACACIA_DOOR).type(Flag.Type.PROTECTION).build();
        assertTrue(aaa.compareTo(bbb) < bbb.compareTo(aaa));
        assertEquals(0, aaa.compareTo(aaa));
    }
}
