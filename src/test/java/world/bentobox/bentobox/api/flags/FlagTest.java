package world.bentobox.bentobox.api.flags;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.RanksManagerTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.flags.clicklisteners.CycleClick;
import world.bentobox.bentobox.api.panels.PanelItem;
import world.bentobox.bentobox.api.panels.reader.ItemTemplateRecord;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
class FlagTest extends RanksManagerTestSetup {

    private Flag f;
    @Mock
    private Listener listener;
    private Map<String, Boolean> worldFlags;
    @Mock
    private LocalesManager testLm;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // Return world
        mockedUtil.when(() -> Util.getWorld(any())).thenAnswer((Answer<World>) invocation -> invocation.getArgument(0, World.class));

        // World Settings
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
        when(plugin.getLocalesManager()).thenReturn(testLm);
        // Setting US text is successful
        when(testLm.setTranslation(eq(Locale.US), anyString(), anyString())).thenReturn(true);

        // Flag
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).type(Flag.Type.PROTECTION).listener(listener).build();

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#hashCode()}.
     */
    @Test
    void testHashCode() {
        Flag flag1 = new Flag.Builder("id", Material.ACACIA_BOAT).build();
        Flag flag2 = new Flag.Builder("id", Material.ACACIA_BOAT).build();
        Flag flag3 = new Flag.Builder("id2", Material.ACACIA_BUTTON).build();
        assertEquals(flag1.hashCode(), flag2.hashCode());
        assertNotEquals(flag1.hashCode(), flag3.hashCode());
    }

    /**
     * Test method for .
     */
    @Test
    void testFlag() {
        assertNotNull(f);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getID()}.
     */
    @Test
    void testGetID() {
        assertEquals("flagID", f.getID());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getIcon()}.
     */
    @Test
    void testGetIcon() {
        assertEquals(Material.ACACIA_PLANKS, f.getIcon());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getListener()}.
     */
    @Test
    void testGetListener() {
        assertEquals(listener, f.getListener().get());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getListener()}.
     */
    @Test
    void testGetListenerNone() {
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).build();
        assertEquals(Optional.empty(), f.getListener());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#isSetForWorld(org.bukkit.World)}.
     */
    @Test
    void testIsSetForWorld() {
        assertFalse(f.isSetForWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#isSetForWorld(org.bukkit.World)}.
     */
    @Test
    void testIsSetForWorldWorldSetting() {
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
    void testSetSetting() {
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).type(Flag.Type.WORLD_SETTING).build();
        assertTrue(worldFlags.isEmpty());
        f.setSetting(world, true);
        assertTrue(worldFlags.get("flagID"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setDefaultSetting(boolean)}.
     */
    @Test
    void testSetDefaultSettingBoolean() {
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
    void testSetDefaultSettingWorldBoolean() {

        f.setDefaultSetting(world, true);
        assertTrue(f.isSetForWorld(world));
        f.setDefaultSetting(world, false);
        assertFalse(f.isSetForWorld(world));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setDefaultSetting(org.bukkit.World, boolean)}.
     */
    @Test
    void testSetDefaultSettingWorldBooleanNullWorldSettings() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        f.setDefaultSetting(world, true);
        verify(plugin).logError("Attempt to set default world setting for unregistered world. Register flags in onEnable.");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getType()}.
     */
    @Test
    void testGetType() {
        assertEquals(Flag.Type.PROTECTION, f.getType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getDefaultRank()}.
     */
    @Test
    void testGetDefaultRank() {
        assertEquals(RanksManager.MEMBER_RANK, f.getDefaultRank());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#hasSubPanel()}.
     */
    @Test
    void testHasSubPanel() {
        assertFalse(f.hasSubPanel());
        f = new Flag.Builder("flagID", Material.ACACIA_PLANKS).type(Flag.Type.WORLD_SETTING).usePanel(true).build();
        assertTrue(f.hasSubPanel());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#equals(java.lang.Object)}.
     */
    @Test
    void testEqualsObject() {
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
    void testGetNameReference() {
        assertEquals("protection.flags.flagID.name", f.getNameReference());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getDescriptionReference()}.
     */
    @Test
    void testGetDescriptionReference() {
        assertEquals("protection.flags.flagID.description", f.getDescriptionReference());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getHintReference()}.
     */
    @Test
    void testGetHintReference() {
        assertEquals("protection.flags.flagID.hint", f.getHintReference());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getGameModes()}.
     */
    @Test
    void testGetGameModes() {
        assertTrue(f.getGameModes().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setGameModes(java.util.Set)}.
     */
    @Test
    void testSetGameModes() {
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
    void testAddGameModeAddon() {
        GameModeAddon gameModeAddon = mock(GameModeAddon.class);
        f.addGameModeAddon(gameModeAddon);
        assertTrue(f.getGameModes().contains(gameModeAddon));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#removeGameModeAddon(world.bentobox.bentobox.api.addons.GameModeAddon)}.
     */
    @Test
    void testRemoveGameModeAddon() {
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
    void testToPanelItem() {
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

        RanksManager rm = mock(RanksManager.class);
        mockedRanksManager.when(RanksManager::getInstance).thenReturn(rm);
        when(rm.getRank(RanksManager.VISITOR_RANK)).thenReturn("Visitor");
        when(rm.getRank(RanksManager.OWNER_RANK)).thenReturn("Owner");

        PanelItem pi = f.toPanelItem(plugin, user, world, island, false);

        verify(user).getTranslation("protection.flags.flagID.name");
        verify(user).getTranslation(eq("protection.panel.flag-item.name-layout"), eq("[name]"), any());

        assertEquals(Material.ACACIA_PLANKS, pi.getItem().getType());
    }
    
    /**
     * A protection flag with no island shows the world's on/off state for the
     * flag - that is what applies off-island - and not any island ranks.
     */
    @Test
    void testToPanelItemWithoutIslandShowsWorldSettingActive() {
        User user = mockTranslatingUser();
        // Flag is on for the world, so it is allowed outside islands
        worldFlags.put("flagID", true);

        PanelItem pi = f.toPanelItem(plugin, user, world, null, false);

        assertEquals(Material.ACACIA_PLANKS, pi.getItem().getType());
        verify(user).getTranslation("protection.panel.flag-item.setting-active");
        verify(user, never()).getTranslation("protection.panel.flag-item.setting-disabled");
        verify(user).getTranslation(eq("protection.panel.flag-item.setting-layout"), eq("[description]"), any(),
                eq("[setting]"), any(), eq("[ranks]"), any(), eq("[tooltips]"), any());
        // No island means no ranks to show
        verify(user, never()).getTranslation(eq("protection.panel.flag-item.description-layout"), any(), any());
    }

    /**
     * As above, but the flag is off for the world.
     */
    @Test
    void testToPanelItemWithoutIslandShowsWorldSettingDisabled() {
        User user = mockTranslatingUser();
        worldFlags.put("flagID", false);

        f.toPanelItem(plugin, user, world, null, false);

        verify(user).getTranslation("protection.panel.flag-item.setting-disabled");
        verify(user, never()).getTranslation("protection.panel.flag-item.setting-active");
    }

    /**
     * A layout with a {@code [ranks]} placeholder gets the rank list substituted there rather
     * than appended after the layout.
     */
    @Test
    void testToPanelItemRanksPlaceholder() {
        User user = layoutUser(Map.of(
                "protection.panel.flag-item.description-layout", "[description]\n[ranks]\nfooter"));
        PanelItem pi = f.toPanelItem(plugin, user, world, island, false);

        assertEquals(List.of("desc", "+ ranks.owner", "+ ranks.sub-owner", "= ranks.member", "- ranks.trusted",
                "- ranks.coop", "- ranks.visitor", "footer"), pi.getDescription());
    }

    /**
     * A layout written before {@code [ranks]} existed still gets the rank list appended after it,
     * so customised locale files render as they did.
     */
    @Test
    void testToPanelItemRanksAppendedWithoutPlaceholder() {
        User user = layoutUser(Map.of("protection.panel.flag-item.description-layout", "[description]\nfooter"));
        PanelItem pi = f.toPanelItem(plugin, user, world, island, false);

        assertEquals(List.of("desc", "footer", "+ ranks.owner", "+ ranks.sub-owner", "= ranks.member",
                "- ranks.trusted", "- ranks.coop", "- ranks.visitor"), pi.getDescription());
    }

    /**
     * A template button overrides the icon, the name layout and the lore layout, and its action
     * tooltips are placed at {@code [tooltips]}.
     */
    @Test
    void testToPanelItemTemplateOverrides() {
        User user = layoutUser(Map.of("custom.name", "N:[name]", "custom.layout", "[description] | [tooltips]",
                "tip.key", "TIP"));
        ItemTemplateRecord template = new ItemTemplateRecord(new ItemStack(Material.DIAMOND), "custom.name",
                "custom.layout", null);
        template.addAction(new ItemTemplateRecord.ActionRecords(ClickType.LEFT, "cycle", null, "tip.key"));

        PanelItem pi = f.toPanelItem(plugin, user, world, island, false, template);

        assertEquals(Material.DIAMOND, pi.getItem().getType());
        assertEquals("N:protection.flags.flagID.name", pi.getName());
        // Ranks are appended because the custom layout has no [ranks]
        assertEquals(List.of("desc | TIP", "+ ranks.owner", "+ ranks.sub-owner", "= ranks.member", "- ranks.trusted",
                "- ranks.coop", "- ranks.visitor"), pi.getDescription());
    }

    /**
     * Template tooltips are appended after an empty line when the layout has no
     * {@code [tooltips]} placeholder, as other templated panels do.
     */
    @Test
    void testToPanelItemTooltipsAppendedWithoutPlaceholder() {
        User user = layoutUser(Map.of("protection.panel.flag-item.description-layout", "[description]",
                "tip.key", "TIP"));
        ItemTemplateRecord template = new ItemTemplateRecord(null, null, null, null);
        template.addAction(new ItemTemplateRecord.ActionRecords(ClickType.LEFT, "cycle", null, "tip.key"));

        PanelItem pi = f.toPanelItem(plugin, user, world, island, false, template);

        // Flag's own icon as the template has none
        assertEquals(Material.ACACIA_PLANKS, pi.getItem().getType());
        assertEquals(List.of("desc", "+ ranks.owner", "+ ranks.sub-owner", "= ranks.member", "- ranks.trusted",
                "- ranks.coop", "- ranks.visitor", "", "TIP"), pi.getDescription());
    }

    /**
     * A user whose translations come from the given map, with placeholder pairs applied. Keys not
     * in the map translate to themselves. Ranks are ordered as the real ranks manager orders them.
     */
    private User layoutUser(Map<String, String> texts) {
        Map<String, Integer> ranks = new LinkedHashMap<>();
        ranks.put(ADMIN_RANK_REF, ADMIN_RANK);
        ranks.put(MOD_RANK_REF, MOD_RANK);
        ranks.put(OWNER_RANK_REF, OWNER_RANK);
        ranks.put(SUB_OWNER_RANK_REF, SUB_OWNER_RANK);
        ranks.put(MEMBER_RANK_REF, MEMBER_RANK);
        ranks.put(TRUSTED_RANK_REF, TRUSTED_RANK);
        ranks.put(COOP_RANK_REF, COOP_RANK);
        ranks.put(VISITOR_RANK_REF, VISITOR_RANK);
        ranks.put(BANNED_RANK_REF, BANNED_RANK);
        when(rm.getRanks()).thenReturn(ranks);
        when(island.getFlag(any())).thenReturn(MEMBER_RANK);
        Map<String, String> all = new HashMap<>(Map.of(
                "protection.flags.flagID.description", "desc",
                "protection.panel.flag-item.allowed-rank", "+ [rank]",
                "protection.panel.flag-item.blocked-rank", "- [rank]",
                "protection.panel.flag-item.minimal-rank", "= [rank]"));
        all.putAll(texts);
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Answer<String> answer = invocation -> {
            Object[] args = invocation.getArguments();
            String text = all.getOrDefault((String) args[0], (String) args[0]);
            for (int i = 1; i + 1 < args.length; i += 2) {
                text = text.replace(String.valueOf(args[i]), String.valueOf(args[i + 1]));
            }
            return text;
        };
        when(user.getTranslation(anyString())).thenAnswer(answer);
        when(user.getTranslation(anyString(), any(String[].class))).thenAnswer(answer);
        when(user.getTranslationOrNothing(anyString())).thenReturn("");
        return user;
    }

    private User mockTranslatingUser() {
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(UUID.randomUUID());
        Answer<String> answer = invocation -> {
            StringBuilder sb = new StringBuilder();
            Arrays.stream(invocation.getArguments()).forEach(sb::append);
            sb.append("mock");
            return sb.toString();
        };
        when(user.getTranslation(any(String.class), any(), any())).thenAnswer(answer);
        when(user.getTranslation(any(String.class))).thenAnswer(answer);
        return user;
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setTranslatedName(java.util.Locale, String)}.
     */
    @Test
    void testSetTranslatedName() {
        assertFalse(f.setTranslatedName(Locale.CANADA, "Good eh?"));
        assertTrue(f.setTranslatedName(Locale.US, "Yihaa"));
    }
    
    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#setTranslatedDescription(java.util.Locale, String)}.
     */
    @Test
    void testSetTranslatedDescription() {
        assertFalse(f.setTranslatedDescription(Locale.CANADA, "Good eh?"));
        assertTrue(f.setTranslatedDescription(Locale.US, "Yihaa"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#toString()}.
     */
    @Test
    void testToString() {
        assertEquals("Flag [id=flagID]", f.toString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#compareTo(world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    void testCompareTo() {
        Flag aaa = new Flag.Builder("AAA", Material.ACACIA_DOOR).type(Flag.Type.PROTECTION).build();
        Flag bbb = new Flag.Builder("BBB", Material.ACACIA_DOOR).type(Flag.Type.PROTECTION).build();
        assertTrue(aaa.compareTo(bbb) < bbb.compareTo(aaa));
        assertEquals(0, aaa.compareTo(aaa));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag#getMinimumRank()}.
     */
    @Test
    void testMinimumRankDefaultsToVisitor() {
        Flag flag = new Flag.Builder("minDefault", Material.ACACIA_DOOR).type(Flag.Type.PROTECTION).build();
        assertEquals(RanksManager.VISITOR_RANK, flag.getMinimumRank());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.flags.Flag.Builder#minimumRank(int)}.
     */
    @Test
    void testMinimumRankSetViaBuilder() {
        Flag flag = new Flag.Builder("minMember", Material.ACACIA_DOOR)
                .type(Flag.Type.PROTECTION)
                .minimumRank(RanksManager.MEMBER_RANK)
                .build();
        assertEquals(RanksManager.MEMBER_RANK, flag.getMinimumRank());
    }

    /**
     * The auto-assigned CycleClick for a PROTECTION flag must be configured with the
     * Builder's minimumRank, so the click cycle skips ranks below the minimum.
     */
    @Test
    void testMinimumRankPropagatesToCycleClick() throws Exception {
        Flag flag = new Flag.Builder("minCycle", Material.ACACIA_DOOR)
                .type(Flag.Type.PROTECTION)
                .minimumRank(RanksManager.MEMBER_RANK)
                .build();
        PanelItem.ClickHandler handler = flag.getClickHandler();
        assertTrue(handler instanceof CycleClick, "Expected auto-assigned CycleClick handler");
        java.lang.reflect.Field minRankField = CycleClick.class.getDeclaredField("minRank");
        minRankField.setAccessible(true);
        assertEquals(RanksManager.MEMBER_RANK, minRankField.getInt(handler));
    }

    /**
     * If defaultRank is set below minimumRank, build() should clamp it up to minimumRank
     * so the flag's default value is selectable.
     */
    @Test
    void testDefaultRankClampedToMinimumRank() {
        Flag flag = new Flag.Builder("clamp", Material.ACACIA_DOOR)
                .type(Flag.Type.PROTECTION)
                .defaultRank(RanksManager.VISITOR_RANK)
                .minimumRank(RanksManager.MEMBER_RANK)
                .build();
        assertEquals(RanksManager.MEMBER_RANK, flag.getDefaultRank());
    }

    /**
     * SETTING flags should allow -1 (disabled) as defaultRank without clamping,
     * since Island.isAllowed() uses >= 0 as the enabled threshold.
     */
    @Test
    void testSettingFlagAllowsNegativeDefaultRank() {
        Flag flag = new Flag.Builder("pvp_test", Material.ARROW)
                .type(Flag.Type.SETTING)
                .defaultRank(-1)
                .build();
        assertEquals(-1, flag.getDefaultRank());
    }
}
