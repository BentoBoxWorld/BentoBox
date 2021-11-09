/**
 *
 */
package world.bentobox.bentobox.database.objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.eclipse.jdt.annotation.NonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.flags.Flag.Type;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Pair;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class})
public class IslandTest {

    private final UUID uuid = UUID.randomUUID();
    private final UUID m = UUID.randomUUID();
    private Island i;
    @Mock
    private @NonNull Location location;
    @Mock
    private BentoBox plugin;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private World world;
    @Mock
    private User user;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Max range
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getIslandDistance(any())).thenReturn(400);

        // Location
        //when(location.getWorld()).thenReturn(world);
        when(location.clone()).thenReturn(location);
        when(world.getName()).thenReturn("bskyblock_world");

        // User
        when(user.getUniqueId()).thenReturn(uuid);

        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getOnlinePlayers()).thenReturn(Collections.emptyList());

        FlagsManager fm = new FlagsManager(plugin);
        // Flags
        when(plugin.getFlagsManager()).thenReturn(fm);

        i = new Island(new Island(location, uuid , 100));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#Island(org.bukkit.Location, java.util.UUID, int)}.
     */
    @Test
    public void testIslandLocationUUIDInt() {
        assertEquals("Location{world=null,x=0.0,y=0.0,z=0.0,pitch=0.0,yaw=0.0}", i.getCenter().toString());
        assertEquals(uuid, i.getOwner());
        assertEquals(400, i.getRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#Island(world.bentobox.bentobox.database.objects.Island)}.
     */
    @Test
    public void testIslandIsland() {
        assertEquals("Location{world=null,x=0.0,y=0.0,z=0.0,pitch=0.0,yaw=0.0}", i.getCenter().toString());
        assertEquals(uuid, i.getOwner());
        assertEquals(400, i.getRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#addMember(java.util.UUID)}.
     */
    @Test
    public void testAddMember() {
        i.addMember(m);
        assertTrue(i.getMemberSet().contains(m));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#ban(java.util.UUID, java.util.UUID)}.
     */
    @Test
    public void testBan() {
        i.ban(uuid, m);
        assertTrue(i.isBanned(m));
        assertFalse(i.isBanned(uuid));
        assertFalse(i.isBanned(UUID.randomUUID()));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getBanned()}.
     */
    @Test
    public void testGetBanned() {
        assertTrue(i.getBanned().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#unban(java.util.UUID, java.util.UUID)}.
     */
    @Test
    public void testUnban() {
        i.ban(uuid, m);
        assertTrue(i.isBanned(m));
        i.unban(uuid, m);
        assertFalse(i.isBanned(m));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getCenter()}.
     */
    @Test
    public void testGetCenter() {
        assertEquals("Location{world=null,x=0.0,y=0.0,z=0.0,pitch=0.0,yaw=0.0}", i.getCenter().toString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getCreatedDate()}.
     */
    @Test
    public void testGetCreatedDate() {
        assertTrue(i.getCreatedDate() > 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getFlag(world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testGetFlag() {
        assertEquals(500, i.getFlag(Flags.BREAK_BLOCKS));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getFlags()}.
     */
    @Test
    public void testGetFlags() {
        assertTrue(i.getFlags().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMembers()}.
     */
    @Test
    public void testGetMembers() {
        assertEquals(1, i.getMembers().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMemberSet()}.
     */
    @Test
    public void testGetMemberSet() {
        assertEquals(1, i.getMembers().size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMemberSet(int)}.
     */
    @Test
    public void testGetMemberSetInt() {
        assertFalse(i.getMemberSet(500).isEmpty());
        assertEquals(1, i.getMemberSet(500).size());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMemberSet(int, boolean)}.
     */
    @Test
    public void testGetMemberSetIntBoolean() {
        assertFalse(i.getMemberSet(500, true).isEmpty());
        assertTrue(i.getMemberSet(500, false).isEmpty());
        assertFalse(i.getMemberSet(1000, true).isEmpty());
        assertFalse(i.getMemberSet(1000, false).isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMinProtectedX()}.
     */
    @Test
    public void testGetMinProtectedX() {
        assertEquals(-100, i.getMinProtectedX());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxProtectedX()}.
     */
    @Test
    public void testGetMaxProtectedX() {
        assertEquals(100, i.getMaxProtectedX());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMinProtectedZ()}.
     */
    @Test
    public void testGetMinProtectedZ() {
        assertEquals(-100, i.getMinProtectedZ());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxProtectedZ()}.
     */
    @Test
    public void testGetMaxProtectedZ() {
        assertEquals(100, i.getMaxProtectedZ());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMinX()}.
     */
    @Test
    public void testGetMinX() {
        assertEquals(-400, i.getMinX());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxX()}.
     */
    @Test
    public void testGetMaxX() {
        assertEquals(400, i.getMaxX());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMinZ()}.
     */
    @Test
    public void testGetMinZ() {
        assertEquals(-400, i.getMinZ());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxZ()}.
     */
    @Test
    public void testGetMaxZ() {
        assertEquals(400, i.getMaxZ());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getName()}.
     */
    @Test
    public void testGetName() {
        assertNull(i.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getOwner()}.
     */
    @Test
    public void testGetOwner() {
        assertEquals(uuid, i.getOwner());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isOwned()}.
     */
    @Test
    public void testIsOwned() {
        assertTrue(i.isOwned());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isUnowned()}.
     */
    @Test
    public void testIsUnowned() {
        assertFalse(i.isUnowned());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getProtectionRange()}.
     */
    @Test
    public void testGetProtectionRange() {
        assertEquals(100, i.getProtectionRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxEverProtectionRange()}.
     */
    @Test
    public void testGetMaxEverProtectionRange() {
        assertEquals(100, i.getMaxEverProtectionRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setMaxEverProtectionRange(int)}.
     */
    @Test
    public void testSetMaxEverProtectionRange() {
        i.setMaxEverProtectionRange(50);
        assertEquals(100, i.getMaxEverProtectionRange());
        i.setMaxEverProtectionRange(150);
        assertEquals(150, i.getMaxEverProtectionRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getPurgeProtected()}.
     */
    @Test
    public void testGetPurgeProtected() {
        assertFalse(i.getPurgeProtected());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getRange()}.
     */
    @Test
    public void testGetRange() {
        assertEquals(400, i.getRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getRank(world.bentobox.bentobox.api.user.User)}.
     */
    @Test
    public void testGetRankUser() {
        assertEquals(RanksManager.OWNER_RANK, i.getRank(user));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getRank(java.util.UUID)}.
     */
    @Test
    public void testGetRankUUID() {
        assertEquals(RanksManager.OWNER_RANK, i.getRank(uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getUniqueId()}.
     */
    @Test
    public void testGetUniqueId() {
        assertFalse(i.getUniqueId().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getUpdatedDate()}.
     */
    @Test
    public void testGetUpdatedDate() {
        assertTrue(i.getUpdatedDate() > 0);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getWorld()}.
     */
    @Test
    public void testGetWorld() {
        assertNull(i.getWorld());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getX()}.
     */
    @Test
    public void testGetX() {
        assertEquals(0, i.getX());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getY()}.
     */
    @Test
    public void testGetY() {
        assertEquals(0, i.getY());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getZ()}.
     */
    @Test
    public void testGetZ() {
        assertEquals(0, i.getZ());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#inIslandSpace(int, int)}.
     */
    @Test
    public void testInIslandSpaceIntInt() {
        assertTrue(i.inIslandSpace(0,0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#inIslandSpace(org.bukkit.Location)}.
     */
    @Test
    public void testInIslandSpaceLocation() {
        i.setWorld(world);
        when(location.getWorld()).thenReturn(world);
        assertTrue(i.inIslandSpace(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#inIslandSpace(world.bentobox.bentobox.util.Pair)}.
     */
    @Test
    public void testInIslandSpacePairOfIntegerInteger() {
        assertTrue(i.inIslandSpace(new Pair<Integer, Integer>(0,0)));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getBoundingBox()}.
     */
    @Test
    public void testGetBoundingBox() {
        i.setWorld(world);
        when(location.getWorld()).thenReturn(world);
        assertNotNull(i.getBoundingBox());
        assertEquals("BoundingBox [minX=-400.0, minY=0.0, minZ=-400.0, maxX=400.0, maxY=0.0, maxZ=400.0]", i.getBoundingBox().toString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getVisitors()}.
     */
    @Test
    public void testGetVisitors() {
        assertTrue(i.getVisitors().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#hasVisitors()}.
     */
    @Test
    public void testHasVisitors() {
        assertFalse(i.hasVisitors());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getPlayersOnIsland()}.
     */
    @Test
    public void testGetPlayersOnIsland() {
        assertTrue(i.getPlayersOnIsland().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#hasPlayersOnIsland()}.
     */
    @Test
    public void testHasPlayersOnIsland() {
        assertFalse(i.hasPlayersOnIsland());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isAllowed(world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testIsAllowedFlag() {
        assertFalse(i.isAllowed(Flags.PVP_OVERWORLD));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isAllowed(world.bentobox.bentobox.api.user.User, world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testIsAllowedUserFlag() {
        assertTrue(i.isAllowed(user, Flags.BREAK_BLOCKS));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isBanned(java.util.UUID)}.
     */
    @Test
    public void testIsBanned() {
        assertFalse(i.isBanned(uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isSpawn()}.
     */
    @Test
    public void testIsSpawn() {
        assertFalse(i.isSpawn());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#onIsland(org.bukkit.Location)}.
     */
    @Test
    public void testOnIsland() {
        i.setWorld(world);
        when(location.getWorld()).thenReturn(world);
        assertTrue(i.onIsland(location));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getProtectionBoundingBox()}.
     */
    @Test
    public void testGetProtectionBoundingBox() {
        i.setWorld(world);
        assertNotNull(i.getProtectionBoundingBox());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#removeMember(java.util.UUID)}.
     */
    @Test
    public void testRemoveMember() {
        i.removeMember(uuid);
        assertTrue(i.getMemberSet().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setCenter(org.bukkit.Location)}.
     */
    @Test
    public void testSetCenter() {
        when(location.getWorld()).thenReturn(world);
        i.setCenter(location);
        assertEquals(location, i.getCenter());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setCreatedDate(long)}.
     */
    @Test
    public void testSetCreatedDate() {
        i.setCreatedDate(123456L);
        assertEquals(123456L, i.getCreatedDate());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setFlag(world.bentobox.bentobox.api.flags.Flag, int)}.
     */
    @Test
    public void testSetFlagFlagInt() {
        i.setFlag(Flags.BREAK_BLOCKS, 100);
        assertTrue(i.isChanged());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setFlag(world.bentobox.bentobox.api.flags.Flag, int, boolean)}.
     */
    @Test
    public void testSetFlagFlagIntBoolean() {
        Flag f = Flags.values().stream().filter(fl -> fl.hasSubflags()).findFirst().orElse(null);
        if (f != null) {
            i.setFlag(f, 100, true);
            assertTrue(i.isChanged());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setFlags(java.util.Map)}.
     */
    @Test
    public void testSetFlags() {
        i.setFlags(Collections.emptyMap());
        assertTrue(i.getFlags().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setFlagsDefaults()}.
     */
    @Test
    public void testSetFlagsDefaults() {
        i.setFlagsDefaults();
        assertFalse(i.getFlags().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setMembers(java.util.Map)}.
     */
    @Test
    public void testSetMembers() {
        i.setMembers(Collections.emptyMap());
        assertTrue(i.getMembers().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setName(java.lang.String)}.
     */
    @Test
    public void testSetName() {
        i.setName("hello");
        assertEquals("hello", i.getName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setOwner(java.util.UUID)}.
     */
    @Test
    public void testSetOwner() {
        UUID owner = UUID.randomUUID();
        i.setOwner(owner);
        assertEquals(owner, i.getOwner());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setProtectionRange(int)}.
     */
    @Test
    public void testSetProtectionRange() {
        i.setProtectionRange(0);
        assertEquals(0, i.getProtectionRange());
        i.setProtectionRange(50);
        assertEquals(50, i.getProtectionRange());
        i.setProtectionRange(100);
        assertEquals(100, i.getProtectionRange());
        i.setProtectionRange(1000);
        assertEquals(1000, i.getProtectionRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#updateMaxEverProtectionRange()}.
     */
    @Test
    public void testUpdateMaxEverProtectionRange() {
        i.setProtectionRange(1000);
        assertEquals(1000, i.getMaxEverProtectionRange());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setPurgeProtected(boolean)}.
     */
    @Test
    public void testSetPurgeProtected() {
        i.setPurgeProtected(true);
        assertTrue(i.getPurgeProtected());
        i.setPurgeProtected(false);
        assertFalse(i.getPurgeProtected());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setRange(int)}.
     */
    @Test
    public void testSetRange() {
        i.setRange(123);
        assertEquals(123, i.getRange());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setRank(world.bentobox.bentobox.api.user.User, int)}.
     */
    @Test
    public void testSetRankUserInt() {
        i.setRank(user, 600);
        assertEquals(600, i.getRank(user));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setRank(java.util.UUID, int)}.
     */
    @Test
    public void testSetRankUUIDInt() {
        i.setRank(uuid, 603);
        assertEquals(603, i.getRank(uuid));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setRanks(java.util.Map)}.
     */
    @Test
    public void testSetRanks() {
        UUID u = UUID.randomUUID();
        i.setRanks(Collections.singletonMap(u, 123));
        assertEquals(123, i.getRank(u));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setSpawn(boolean)}.
     */
    @Test
    public void testSetSpawn() {
        assertFalse(i.isSpawn());
        i.setSpawn(true);
        assertTrue(i.isSpawn());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getSpawnPoint()}.
     */
    @Test
    public void testGetSpawnPoint() {
        assertTrue(i.getSpawnPoint().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setSpawnPoint(java.util.Map)}.
     */
    @Test
    public void testSetSpawnPointMapOfEnvironmentLocation() {
        Map<Environment, Location> m = new EnumMap<>(Environment.class);
        m.put(Environment.THE_END, location);
        i.setSpawnPoint(m);
        assertEquals(location, i.getSpawnPoint(Environment.THE_END));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setUniqueId(java.lang.String)}.
     */
    @Test
    public void testSetUniqueId() {
        String u = UUID.randomUUID().toString();
        i.setUniqueId(u);
        assertEquals(u, i.getUniqueId());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setUpdatedDate(long)}.
     */
    @Test
    public void testSetUpdatedDate() {
        i.setUpdatedDate(566789L);
        assertEquals(566789L, i.getUpdatedDate());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setWorld(org.bukkit.World)}.
     */
    @Test
    public void testSetWorld() {
        World w = Mockito.mock(World.class);
        i.setWorld(w);
        assertEquals(w, i.getWorld());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#toggleFlag(world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testToggleFlagFlag() {
        assertFalse(i.isAllowed(Flags.PVP_END));
        i.toggleFlag(Flags.PVP_END);
        assertTrue(i.isAllowed(Flags.PVP_END));
        i.toggleFlag(Flags.PVP_END);
        assertFalse(i.isAllowed(Flags.PVP_END));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#toggleFlag(world.bentobox.bentobox.api.flags.Flag, boolean)}.
     */
    @Test
    public void testToggleFlagFlagBoolean() {
        Flag f = Flags.values().stream().filter(fl -> fl.hasSubflags())
                .filter(fl -> fl.getType().equals(Type.SETTING))
                .findFirst().orElse(null);
        if (f != null) {
            i.toggleFlag(f, true);
            assertTrue(i.isAllowed(f));
            i.toggleFlag(f, true);
            assertFalse(i.isAllowed(f));
        } else {
            System.out.println("No settings flag with subflags yet");
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setSettingsFlag(world.bentobox.bentobox.api.flags.Flag, boolean)}.
     */
    @Test
    public void testSetSettingsFlagFlagBoolean() {
        i.setSettingsFlag(Flags.PVP_NETHER, true);
        assertTrue(i.isAllowed(Flags.PVP_NETHER));
        i.setSettingsFlag(Flags.PVP_NETHER, false);
        assertFalse(i.isAllowed(Flags.PVP_NETHER));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setSettingsFlag(world.bentobox.bentobox.api.flags.Flag, boolean, boolean)}.
     */
    @Test
    public void testSetSettingsFlagFlagBooleanBoolean() {
        i.setSettingsFlag(Flags.PVP_NETHER, true, true);
        assertTrue(i.isAllowed(Flags.PVP_NETHER));
        i.setSettingsFlag(Flags.PVP_NETHER, false, true);
        assertFalse(i.isAllowed(Flags.PVP_NETHER));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setSpawnPoint(org.bukkit.World.Environment, org.bukkit.Location)}.
     */
    @Test
    public void testSetSpawnPointEnvironmentLocation() {
        i.setSpawnPoint(Environment.THE_END, location);
        assertEquals(location, i.getSpawnPoint(Environment.THE_END));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getSpawnPoint(org.bukkit.World.Environment)}.
     */
    @Test
    public void testGetSpawnPointEnvironment() {
        i.setSpawnPoint(Environment.THE_END, location);
        assertEquals(location, i.getSpawnPoint(Environment.THE_END));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#removeRank(java.lang.Integer)}.
     */
    @Test
    public void testRemoveRank() {
        assertFalse(i.getMembers().isEmpty());
        i.removeRank(RanksManager.OWNER_RANK);
        assertTrue(i.getMembers().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getHistory()}.
     */
    @Test
    public void testGetHistory() {
        assertTrue(i.getHistory().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#log(world.bentobox.bentobox.api.logs.LogEntry)}.
     */
    @Test
    public void testLog() {
        LogEntry le = Mockito.mock(LogEntry.class);
        i.log(le);
        assertEquals(le, i.getHistory().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setHistory(java.util.List)}.
     */
    @Test
    public void testSetHistory() {
        LogEntry le = Mockito.mock(LogEntry.class);
        i.setHistory(List.of(le));
        assertEquals(le, i.getHistory().get(0));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isDoNotLoad()}.
     */
    @Test
    public void testIsDoNotLoad() {
        assertFalse(i.isDoNotLoad());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setDoNotLoad(boolean)}.
     */
    @Test
    public void testSetDoNotLoad() {
        i.setDoNotLoad(true);
        assertTrue(i.isDoNotLoad());
        i.setDoNotLoad(false);
        assertFalse(i.isDoNotLoad());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isDeleted()}.
     */
    @Test
    public void testIsDeleted() {
        assertFalse(i.isDeleted());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setDeleted(boolean)}.
     */
    @Test
    public void testSetDeleted() {
        i.setDeleted(true);
        assertTrue(i.isDeleted());
        i.setDeleted(false);
        assertFalse(i.isDeleted());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getGameMode()}.
     */
    @Test
    public void testGetGameMode() {
        assertNull(i.getGameMode());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setGameMode(java.lang.String)}.
     */
    @Test
    public void testSetGameMode() {
        i.setGameMode("BSkyBlock");
        assertEquals("BSkyBlock", i.getGameMode());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#hasNetherIsland()}.
     */
    @Test
    public void testHasNetherIsland() {
        i.setWorld(world);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.getNetherWorld(world)).thenReturn(world);
        Block block = Mockito.mock(Block.class);
        when(block.getType()).thenReturn(Material.BEDROCK);
        when(world.getBlockAt(any())).thenReturn(block);
        assertTrue(i.hasNetherIsland());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#hasEndIsland()}.
     */
    @Test
    public void testHasEndIsland() {
        i.setWorld(world);
        when(iwm.isEndGenerate(any())).thenReturn(true);
        when(iwm.isEndIslands(any())).thenReturn(true);
        when(iwm.getEndWorld(world)).thenReturn(world);
        Block block = Mockito.mock(Block.class);
        when(block.getType()).thenReturn(Material.BEDROCK);
        when(world.getBlockAt(any())).thenReturn(block);
        assertTrue(i.hasEndIsland());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isCooldown(world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testIsCooldown() {
        assertFalse(i.isCooldown(Flags.BREAK_BLOCKS));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setCooldown(world.bentobox.bentobox.api.flags.Flag)}.
     */
    @Test
    public void testSetCooldown() {
        assertTrue(i.getCooldowns().isEmpty());
        i.setCooldown(Flags.BREAK_BLOCKS);
        assertFalse(i.getCooldowns().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getCooldowns()}.
     */
    @Test
    public void testGetCooldowns() {
        assertTrue(i.getCooldowns().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setCooldowns(java.util.Map)}.
     */
    @Test
    public void testSetCooldowns() {
        i.setCooldowns(Collections.singletonMap(Flags.BREAK_BLOCKS, 123L));
        assertFalse(i.getCooldowns().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getCommandRanks()}.
     */
    @Test
    public void testGetCommandRanks() {
        assertNull(i.getCommandRanks());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setCommandRanks(java.util.Map)}.
     */
    @Test
    public void testSetCommandRanks() {
        i.setCommandRanks(Collections.singletonMap("hello", 123));
        assertFalse(i.getCommandRanks().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getRankCommand(java.lang.String)}.
     */
    @Test
    public void testGetRankCommand() {
        assertEquals(RanksManager.OWNER_RANK, i.getRankCommand("test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setRankCommand(java.lang.String, int)}.
     */
    @Test
    public void testSetRankCommand() {
        i.setRankCommand("test", RanksManager.COOP_RANK);
        assertEquals(RanksManager.COOP_RANK, i.getRankCommand("test"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isReserved()}.
     */
    @Test
    public void testIsReserved() {
        assertFalse(i.isReserved());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setReserved(boolean)}.
     */
    @Test
    public void testSetReserved() {
        i.setReserved(true);
        assertTrue(i.isReserved());
        i.setReserved(false);
        assertFalse(i.isReserved());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMetaData()}.
     */
    @Test
    public void testGetMetaData() {
        assertTrue(i.getMetaData().get().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setMetaData(java.util.Map)}.
     */
    @Test
    public void testSetMetaData() {
        MetaDataValue meta = new MetaDataValue("hello");
        i.setMetaData(Collections.singletonMap("test", meta));
        assertFalse(i.getMetaData().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#isChanged()}.
     */
    @Test
    public void testIsChanged() {
        assertTrue(i.isChanged());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setChanged()}.
     */
    @Test
    public void testSetChanged() {
        Island ii = new Island();
        ii.setChanged();
        assertTrue(ii.isChanged());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setChanged(boolean)}.
     */
    @Test
    public void testSetChangedBoolean() {
        i.setChanged(false);
        assertFalse(i.isChanged());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getProtectionCenter()}.
     */
    @Test
    public void testGetProtectionCenter() {
        assertEquals("Location{world=null,x=0.0,y=0.0,z=0.0,pitch=0.0,yaw=0.0}", i.getProtectionCenter().toString());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setProtectionCenter(org.bukkit.Location)}.
     * @throws IOException if the location is not in island space
     */
    @Test
    public void testSetProtectionCenter() throws IOException {
        i.setWorld(world);
        when(world.getName()).thenReturn("bskyblock_wworld");
        when(location.getWorld()).thenReturn(world);
        i.setProtectionCenter(location);
        assertEquals(location, i.getProtectionCenter());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getHomes()}.
     */
    @Test
    public void testGetHomes() {
        assertTrue(i.getHomes().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getHome(java.lang.String)}.
     */
    @Test
    public void testGetHome() {
        assertNull(i.getHome("default"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setHomes(java.util.Map)}.
     */
    @Test
    public void testSetHomes() {
        i.setHomes(Collections.singletonMap("hello", location));
        assertFalse(i.getHomes().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#addHome(java.lang.String, org.bukkit.Location)}.
     */
    @Test
    public void testAddHome() {
        i.addHome("backyard", location);
        assertEquals(location, i.getHome("backyard"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#removeHome(java.lang.String)}.
     */
    @Test
    public void testRemoveHome() {
        testAddHome();
        i.removeHome("backyard");
        assertTrue(i.getHomes().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#renameHome(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testRenameHome() {
        testAddHome();
        assertTrue(i.renameHome("backyard", "new"));
        assertFalse(i.renameHome("new", "new"));
        assertFalse(i.renameHome("nhelloew", "hfhhf"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxHomes()}.
     */
    @Test
    public void testGetMaxHomes() {
        assertNull(i.getMaxHomes());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setMaxHomes(java.lang.Integer)}.
     */
    @Test
    public void testSetMaxHomes() {
        i.setMaxHomes(23);
        assertEquals(23, i.getMaxHomes().intValue());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxMembers()}.
     */
    @Test
    public void testGetMaxMembers() {
        assertTrue(i.getMaxMembers().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setMaxMembers(java.util.Map)}.
     */
    @Test
    public void testSetMaxMembersMapOfIntegerInteger() {
        i.setMaxMembers(Collections.singletonMap(2345, 400));
        assertFalse(i.getMaxMembers().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#getMaxMembers(int)}.
     */
    @Test
    public void testGetMaxMembersInt() {
        assertNull(i.getMaxMembers(1000));
        i.setMaxMembers(Collections.singletonMap(1000, 400));
        assertEquals(400, i.getMaxMembers(1000).intValue());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#setMaxMembers(int, java.lang.Integer)}.
     */
    @Test
    public void testSetMaxMembersIntInteger() {
        i.setMaxMembers(1000, 400);
        assertEquals(400, i.getMaxMembers(1000).intValue());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.database.objects.Island#toString()}.
     */
    @Test
    public void testToString() {
        assertFalse(i.toString().isBlank());
    }

}
