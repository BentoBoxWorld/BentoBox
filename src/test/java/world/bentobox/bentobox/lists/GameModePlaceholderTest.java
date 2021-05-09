package world.bentobox.bentobox.lists;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.TestWorldSettings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public class GameModePlaceholderTest {

    @Mock
    private GameModeAddon addon;
    @Mock
    private User user;
    @Mock
    private Island island;
    @Mock
    private PlayersManager pm;
    private UUID uuid;
    @Mock
    private World world;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandsManager im;
    private RanksManager rm = new RanksManager();
    @Mock
    private @Nullable Location location;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        uuid = UUID.randomUUID();
        when(addon.getPlayers()).thenReturn(pm);
        when(addon.getIslands()).thenReturn(im);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPermissionValue(anyString(), anyInt())).thenReturn(10);
        when(addon.getOverWorld()).thenReturn(world);
        when(addon.getPlugin()).thenReturn(plugin);
        when(addon.getPermissionPrefix()).thenReturn("MyGameMode.");
        when(island.getCenter()).thenReturn(new Location(world, 123, 456, 789));
        when(island.getOwner()).thenReturn(uuid);
        when(island.getUniqueId()).thenReturn(uuid.toString());
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));
        when(island.getMemberSet(anyInt(), anyBoolean())).thenReturn(ImmutableSet.of(uuid));
        when(island.getMemberSet(anyInt())).thenReturn(ImmutableSet.of(uuid));
        when(island.getName()).thenReturn("island");
        when(island.getRank(any(User.class))).thenReturn(RanksManager.OWNER_RANK);
        when(island.getCreatedDate()).thenReturn(123456789455L);
        WorldSettings ws = new TestWorldSettings();
        when(addon.getWorldSettings()).thenReturn(ws);
        when(pm.getName(any())).thenReturn("tastybento");
        when(plugin.getIWM()).thenReturn(iwm);
        when(plugin.getRanksManager()).thenReturn(rm);
        when(user.getTranslation(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getLocation()).thenReturn(location);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));
        when(user.isPlayer()).thenReturn(true);
        // Max members
        when(im.getMaxMembers(island, RanksManager.MEMBER_RANK)).thenReturn(10);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.lists.GameModePlaceholder#getReplacer()}.
     */
    @Test
    public void testGetReplacerIsland() {
        assertEquals("0", GameModePlaceholder.ISLAND_BANS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("123,456,789", GameModePlaceholder.ISLAND_CENTER.getReplacer().onReplace(addon, user, island));
        assertEquals("123", GameModePlaceholder.ISLAND_CENTER_X.getReplacer().onReplace(addon, user, island));
        assertEquals("456", GameModePlaceholder.ISLAND_CENTER_Y.getReplacer().onReplace(addon, user, island));
        assertEquals("789", GameModePlaceholder.ISLAND_CENTER_Z.getReplacer().onReplace(addon, user, island));
        assertEquals("1", GameModePlaceholder.ISLAND_COOPS_COUNT.getReplacer().onReplace(addon, user, island));
        // As the local time zone of the compiling machine can vary, the exact value cannot be checked.
        assertFalse(GameModePlaceholder.ISLAND_CREATION_DATE.getReplacer().onReplace(addon, user, island).isEmpty());
        assertEquals("1", GameModePlaceholder.ISLAND_MEMBERS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("tastybento", GameModePlaceholder.ISLAND_MEMBERS_LIST.getReplacer().onReplace(addon, user, island));
        assertEquals("10", GameModePlaceholder.ISLAND_MEMBERS_MAX.getReplacer().onReplace(addon, user, island));
        assertEquals("island", GameModePlaceholder.ISLAND_NAME.getReplacer().onReplace(addon, user, island));
        assertEquals("tastybento", GameModePlaceholder.ISLAND_OWNER.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.ISLAND_PROTECTION_RANGE.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.ISLAND_PROTECTION_RANGE_DIAMETER.getReplacer().onReplace(addon, user, island));
        assertEquals("1", GameModePlaceholder.ISLAND_TRUSTEES_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals(uuid.toString(), GameModePlaceholder.ISLAND_UUID.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.ISLAND_VISITORS_COUNT.getReplacer().onReplace(addon, user, island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.lists.GameModePlaceholder#getReplacer()}.
     */
    @Test
    public void testGetReplacerNullIsland() {
        island = null;
        assertEquals("", GameModePlaceholder.ISLAND_BANS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_CENTER.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_CENTER_X.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_CENTER_Y.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_CENTER_Z.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_COOPS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_CREATION_DATE.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_MEMBERS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_MEMBERS_LIST.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_MEMBERS_MAX.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_NAME.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_OWNER.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_PROTECTION_RANGE.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_PROTECTION_RANGE_DIAMETER.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_TRUSTEES_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_UUID.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.ISLAND_VISITORS_COUNT.getReplacer().onReplace(addon, user, island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.lists.GameModePlaceholder#getReplacer()}.
     */
    @Test
    public void testGetReplacerPlayer() {
        assertEquals("deaths", GameModePlaceholder.DEATHS.getPlaceholder());
        assertEquals("0", GameModePlaceholder.DEATHS.getReplacer().onReplace(addon, user, island));
        assertEquals("true", GameModePlaceholder.HAS_ISLAND.getReplacer().onReplace(addon, user, island));
        assertEquals("false", GameModePlaceholder.ON_ISLAND.getReplacer().onReplace(addon, user, island));
        assertEquals("true", GameModePlaceholder.OWNS_ISLAND.getReplacer().onReplace(addon, user, island));
        assertEquals("ranks.owner", GameModePlaceholder.RANK.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.RESETS.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.RESETS_LEFT.getReplacer().onReplace(addon, user, island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.lists.GameModePlaceholder#getReplacer()}.
     */
    @Test
    public void testGetReplacerNullPlayer() {
        user = null;
        assertEquals("", GameModePlaceholder.DEATHS.getReplacer().onReplace(addon, user, island));
        assertEquals("false", GameModePlaceholder.HAS_ISLAND.getReplacer().onReplace(addon, user, island));
        assertEquals("false", GameModePlaceholder.ON_ISLAND.getReplacer().onReplace(addon, user, island));
        assertEquals("false", GameModePlaceholder.OWNS_ISLAND.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.RANK.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.RESETS.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.RESETS_LEFT.getReplacer().onReplace(addon, user, island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.lists.GameModePlaceholder#getReplacer()}.
     */
    @Test
    public void testGetReplacerVisitedIslands() {
        assertEquals("0", GameModePlaceholder.VISITED_ISLAND_BANS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("123,456,789", GameModePlaceholder.VISITED_ISLAND_CENTER.getReplacer().onReplace(addon, user, island));
        assertEquals("123", GameModePlaceholder.VISITED_ISLAND_CENTER_X.getReplacer().onReplace(addon, user, island));
        assertEquals("456", GameModePlaceholder.VISITED_ISLAND_CENTER_Y.getReplacer().onReplace(addon, user, island));
        assertEquals("789", GameModePlaceholder.VISITED_ISLAND_CENTER_Z.getReplacer().onReplace(addon, user, island));
        assertEquals("1", GameModePlaceholder.VISITED_ISLAND_COOPS_COUNT.getReplacer().onReplace(addon, user, island));
        // As the local time zone of the compiling machine can vary, the exact value cannot be checked.
        assertFalse(GameModePlaceholder.VISITED_ISLAND_CREATION_DATE.getReplacer().onReplace(addon, user, island).isEmpty());
        assertEquals("1", GameModePlaceholder.VISITED_ISLAND_MEMBERS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("tastybento", GameModePlaceholder.VISITED_ISLAND_MEMBERS_LIST.getReplacer().onReplace(addon, user, island));
        assertEquals("10", GameModePlaceholder.VISITED_ISLAND_MEMBERS_MAX.getReplacer().onReplace(addon, user, island));
        assertEquals("island", GameModePlaceholder.VISITED_ISLAND_NAME.getReplacer().onReplace(addon, user, island));
        assertEquals("tastybento", GameModePlaceholder.VISITED_ISLAND_OWNER.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.VISITED_ISLAND_PROTECTION_RANGE.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.VISITED_ISLAND_PROTECTION_RANGE_DIAMETER.getReplacer().onReplace(addon, user, island));
        assertEquals("1", GameModePlaceholder.VISITED_ISLAND_TRUSTEES_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals(uuid.toString(), GameModePlaceholder.VISITED_ISLAND_UUID.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.VISITED_ISLAND_VISITORS_COUNT.getReplacer().onReplace(addon, user, island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.lists.GameModePlaceholder#getReplacer()}.
     */
    @Test
    public void testGetReplacerVisitedIslandsNoIsland() {
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_BANS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_CENTER.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_CENTER_X.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_CENTER_Y.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_CENTER_Z.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_COOPS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_CREATION_DATE.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_MEMBERS_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_MEMBERS_LIST.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_MEMBERS_MAX.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_NAME.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_OWNER.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_PROTECTION_RANGE.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_PROTECTION_RANGE_DIAMETER.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_TRUSTEES_COUNT.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_UUID.getReplacer().onReplace(addon, user, island));
        assertEquals("", GameModePlaceholder.VISITED_ISLAND_VISITORS_COUNT.getReplacer().onReplace(addon, user, island));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.lists.GameModePlaceholder#getReplacer()}.
     */
    @Test
    public void testGetReplacerWorld() {
        assertEquals("0", GameModePlaceholder.ISLAND_DISTANCE.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.ISLAND_DISTANCE_DIAMETER.getReplacer().onReplace(addon, user, island));
        assertEquals("friendly_name", GameModePlaceholder.WORLD_FRIENDLY_NAME.getReplacer().onReplace(addon, user, island));
        assertEquals("0", GameModePlaceholder.WORLD_ISLANDS.getReplacer().onReplace(addon, user, island));

    }
}
