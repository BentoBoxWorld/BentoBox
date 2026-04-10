package world.bentobox.bentobox.database.objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.api.logs.LogEntry;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.RanksManager;
import world.bentobox.bentobox.util.Pair;
import world.bentobox.bentobox.util.Util;

class IslandTest extends CommonTestSetup {

    private Island island; // real Island under test (shadows the mock in CommonTestSetup)
    private Location center;
    private UUID ownerUUID;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // IWM stubs needed by the Island constructor and spatial methods
        when(iwm.getIslandDistance(any())).thenReturn(100);
        when(iwm.getDefaultIslandFlags(any())).thenReturn(Collections.emptyMap());
        when(iwm.getDefaultIslandSettings(any())).thenReturn(Collections.emptyMap());

        // Util.sameWorld for spatial tests
        mockedUtil.when(() -> Util.sameWorld(any(), any())).thenReturn(true);

        // World environment
        when(world.getEnvironment()).thenReturn(Environment.NORMAL);
        when(world.getMinHeight()).thenReturn(-64);
        when(world.getMaxHeight()).thenReturn(320);

        // Create a real Location for the center
        center = new Location(world, 0, 64, 0);

        ownerUUID = UUID.randomUUID();

        // Create the real Island under test
        island = new Island(center, ownerUUID, 50);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // ======================== Constructors ========================

    @Test
    void testNoArgConstructor() {
        Island i = new Island();
        assertNotNull(i.getUniqueId());
        assertNull(i.getOwner());
        assertTrue(i.getMembers().isEmpty());
    }

    @Test
    void testLocationConstructor() {
        assertEquals(ownerUUID, island.getOwner());
        assertEquals(0, island.getCenter().getBlockX());
        assertEquals(64, island.getCenter().getBlockY());
        assertEquals(0, island.getCenter().getBlockZ());
        assertEquals(100, island.getRange()); // from iwm.getIslandDistance
        assertEquals(50, island.getRawProtectionRange());
        assertEquals(RanksManager.OWNER_RANK, island.getRank(ownerUUID));
        assertTrue(island.getCreatedDate() > 0);
    }

    @Test
    void testCopyConstructor() {
        island.setName("TestIsland");
        island.addMember(UUID.randomUUID());
        island.addHome("home1", new Location(world, 10, 65, 10));

        Island copy = new Island(island);

        assertEquals(island.getUniqueId(), copy.getUniqueId());
        assertEquals(island.getName(), copy.getName());
        assertEquals(island.getOwner(), copy.getOwner());
        assertEquals(island.getMembers().size(), copy.getMembers().size());
        assertEquals(island.getHomes().size(), copy.getHomes().size());
    }

    @Test
    void testCopyConstructorDeepCopy() {
        island.addHome("test", new Location(world, 5, 65, 5));
        Island copy = new Island(island);

        // Modifying copy's homes shouldn't affect original
        copy.addHome("newHome", new Location(world, 20, 65, 20));
        assertFalse(island.getHomes().containsKey("newhome"));
    }

    // ======================== Member Management ========================

    @Test
    void testAddMember() {
        UUID memberUUID = UUID.randomUUID();
        island.addMember(memberUUID);
        assertEquals(RanksManager.MEMBER_RANK, island.getRank(memberUUID));
    }

    @Test
    void testAddMemberIdempotent() {
        UUID memberUUID = UUID.randomUUID();
        island.addMember(memberUUID);
        island.addMember(memberUUID); // should not throw or change rank
        assertEquals(RanksManager.MEMBER_RANK, island.getRank(memberUUID));
    }

    @Test
    void testBan() {
        UUID targetUUID = UUID.randomUUID();
        boolean result = island.ban(ownerUUID, targetUUID);
        assertTrue(result);
        assertTrue(island.isBanned(targetUUID));
        assertEquals(RanksManager.BANNED_RANK, island.getRank(targetUUID));
        assertFalse(island.getHistory().isEmpty());
    }

    @Test
    void testBanExistingMember() {
        UUID memberUUID = UUID.randomUUID();
        island.addMember(memberUUID);
        island.ban(ownerUUID, memberUUID);
        assertTrue(island.isBanned(memberUUID));
        assertFalse(island.getMemberSet().contains(memberUUID));
    }

    @Test
    void testUnban() {
        UUID targetUUID = UUID.randomUUID();
        island.ban(ownerUUID, targetUUID);
        boolean result = island.unban(ownerUUID, targetUUID);
        assertTrue(result);
        assertFalse(island.isBanned(targetUUID));
    }

    @Test
    void testUnbanNotBanned() {
        UUID targetUUID = UUID.randomUUID();
        boolean result = island.unban(ownerUUID, targetUUID);
        assertFalse(result);
    }

    @Test
    void testGetBanned() {
        UUID banned1 = UUID.randomUUID();
        UUID banned2 = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        island.ban(ownerUUID, banned1);
        island.ban(ownerUUID, banned2);
        island.addMember(member);

        Set<UUID> bannedSet = island.getBanned();
        assertEquals(2, bannedSet.size());
        assertTrue(bannedSet.contains(banned1));
        assertTrue(bannedSet.contains(banned2));
        assertFalse(bannedSet.contains(member));
    }

    @Test
    void testRemoveMember() {
        UUID memberUUID = UUID.randomUUID();
        island.addMember(memberUUID);
        island.removeMember(memberUUID);
        assertEquals(RanksManager.VISITOR_RANK, island.getRank(memberUUID));
    }

    @Test
    void testRemoveMemberNotPresent() {
        UUID randomUUID = UUID.randomUUID();
        assertDoesNotThrow(() -> island.removeMember(randomUUID));
    }

    @Test
    void testSetOwner() {
        UUID newOwner = UUID.randomUUID();
        island.setOwner(newOwner);
        assertEquals(newOwner, island.getOwner());
        assertEquals(RanksManager.OWNER_RANK, island.getRank(newOwner));
        // Previous owner should be demoted to member
        assertEquals(RanksManager.MEMBER_RANK, island.getRank(ownerUUID));
    }

    @Test
    void testSetOwnerNull() {
        island.setOwner(null);
        assertNull(island.getOwner());
    }

    @Test
    void testGetRankUUID() {
        assertEquals(RanksManager.OWNER_RANK, island.getRank(ownerUUID));
        assertEquals(RanksManager.VISITOR_RANK, island.getRank(UUID.randomUUID()));
    }

    @Test
    void testGetRankUser() {
        User user = mock(User.class);
        when(user.getUniqueId()).thenReturn(ownerUUID);
        assertEquals(RanksManager.OWNER_RANK, island.getRank(user));

        User stranger = mock(User.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertEquals(RanksManager.VISITOR_RANK, island.getRank(stranger));
    }

    @Test
    void testSetRankNullUUID() {
        assertDoesNotThrow(() -> island.setRank((UUID) null, RanksManager.MEMBER_RANK));
    }

    @Test
    void testSetRankUpdates() {
        UUID memberUUID = UUID.randomUUID();
        island.setRank(memberUUID, RanksManager.TRUSTED_RANK);
        assertEquals(RanksManager.TRUSTED_RANK, island.getRank(memberUUID));
        island.setRank(memberUUID, RanksManager.MEMBER_RANK);
        assertEquals(RanksManager.MEMBER_RANK, island.getRank(memberUUID));
    }

    @Test
    void testGetMemberSet() {
        UUID member = UUID.randomUUID();
        island.addMember(member);
        // Owner + member
        assertTrue(island.getMemberSet().contains(ownerUUID));
        assertTrue(island.getMemberSet().contains(member));
    }

    @Test
    void testGetMemberSetMinimumRank() {
        UUID coop = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        island.setRank(coop, RanksManager.COOP_RANK);
        island.addMember(member);

        // MEMBER_RANK and above
        var memberSet = island.getMemberSet(RanksManager.MEMBER_RANK);
        assertTrue(memberSet.contains(ownerUUID));
        assertTrue(memberSet.contains(member));
        assertFalse(memberSet.contains(coop));

        // COOP_RANK and above includes coop
        var coopSet = island.getMemberSet(RanksManager.COOP_RANK);
        assertTrue(coopSet.contains(coop));
    }

    @Test
    void testGetMemberSetExactRank() {
        UUID member = UUID.randomUUID();
        island.addMember(member);

        // Exact MEMBER_RANK only (not including above)
        var exactMembers = island.getMemberSet(RanksManager.MEMBER_RANK, false);
        assertTrue(exactMembers.contains(member));
        assertFalse(exactMembers.contains(ownerUUID)); // owner has OWNER_RANK

        // Including above ranks
        var inclusive = island.getMemberSet(RanksManager.MEMBER_RANK, true);
        assertTrue(inclusive.contains(ownerUUID));
    }

    @Test
    void testInTeam() {
        assertTrue(island.inTeam(ownerUUID));
        assertFalse(island.inTeam(UUID.randomUUID()));
    }

    @Test
    void testHasTeam() {
        // Only owner - no team
        assertFalse(island.hasTeam());
        // Add a member
        island.addMember(UUID.randomUUID());
        assertTrue(island.hasTeam());
    }

    @Test
    void testRemoveRank() {
        UUID coop1 = UUID.randomUUID();
        UUID coop2 = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        island.setRank(coop1, RanksManager.COOP_RANK);
        island.setRank(coop2, RanksManager.COOP_RANK);
        island.addMember(member);

        island.removeRank(RanksManager.COOP_RANK);

        assertEquals(RanksManager.VISITOR_RANK, island.getRank(coop1));
        assertEquals(RanksManager.VISITOR_RANK, island.getRank(coop2));
        assertEquals(RanksManager.MEMBER_RANK, island.getRank(member)); // unaffected
    }

    @Test
    void testIsBanned() {
        UUID target = UUID.randomUUID();
        assertFalse(island.isBanned(target));
        island.ban(ownerUUID, target);
        assertTrue(island.isBanned(target));
    }

    // ======================== Spatial Methods ========================

    @Test
    void testGetMinMaxXZ() {
        // center at 0, range 100
        assertEquals(-100, island.getMinX());
        assertEquals(100, island.getMaxX());
        assertEquals(-100, island.getMinZ());
        assertEquals(100, island.getMaxZ());
    }

    @Test
    void testGetMinMaxProtectedXZ() {
        // center at 0, protection range 50
        assertEquals(-50, island.getMinProtectedX());
        assertEquals(50, island.getMaxProtectedX());
        assertEquals(-50, island.getMinProtectedZ());
        assertEquals(50, island.getMaxProtectedZ());
    }

    @Test
    void testGetMinMaxProtectedClampedToRange() {
        // Set protection range larger than range - should be clamped
        island.setProtectionRange(200);
        // getProtectionRange() caps at getRange() which is 100
        assertEquals(100, island.getProtectionRange());
        assertEquals(-100, island.getMinProtectedX());
        assertEquals(100, island.getMaxProtectedX());
    }

    @Test
    void testInIslandSpaceCoords() {
        // center 0, range 100, so island space is -100 to 99
        assertTrue(island.inIslandSpace(0, 0));
        assertTrue(island.inIslandSpace(-100, -100));
        assertTrue(island.inIslandSpace(99, 99));
        assertFalse(island.inIslandSpace(100, 100));
        assertFalse(island.inIslandSpace(-101, 0));
    }

    @Test
    void testInIslandSpacePair() {
        assertTrue(island.inIslandSpace(new Pair<>(0, 0)));
        assertFalse(island.inIslandSpace(new Pair<>(200, 200)));
    }

    @Test
    void testInIslandSpaceLocation() {
        Location insideLoc = mock(Location.class);
        when(insideLoc.getWorld()).thenReturn(world);
        when(insideLoc.getBlockX()).thenReturn(50);
        when(insideLoc.getBlockZ()).thenReturn(50);

        Location outsideLoc = mock(Location.class);
        when(outsideLoc.getWorld()).thenReturn(world);
        when(outsideLoc.getBlockX()).thenReturn(200);
        when(outsideLoc.getBlockZ()).thenReturn(200);

        assertTrue(island.inIslandSpace(insideLoc));
        assertFalse(island.inIslandSpace(outsideLoc));
    }

    @Test
    void testOnIsland() {
        Location insideLoc = mock(Location.class);
        when(insideLoc.getWorld()).thenReturn(world);
        when(insideLoc.getBlockX()).thenReturn(10);
        when(insideLoc.getBlockZ()).thenReturn(10);
        assertTrue(island.onIsland(insideLoc));

        // Outside protection range but inside island range
        Location outsideProtection = mock(Location.class);
        when(outsideProtection.getWorld()).thenReturn(world);
        when(outsideProtection.getBlockX()).thenReturn(80);
        when(outsideProtection.getBlockZ()).thenReturn(80);
        assertFalse(island.onIsland(outsideProtection));
    }

    @Test
    void testGetBoundingBox() {
        BoundingBox bb = island.getBoundingBox();
        assertNotNull(bb);
        assertEquals(-100, bb.getMinX());
        assertEquals(100, bb.getMaxX());
        assertEquals(-100, bb.getMinZ());
        assertEquals(100, bb.getMaxZ());
    }

    @Test
    void testGetProtectionBoundingBox() {
        BoundingBox bb = island.getProtectionBoundingBox();
        assertNotNull(bb);
        assertEquals(-50, bb.getMinX());
        assertEquals(50, bb.getMaxX());
        assertEquals(-50, bb.getMinZ());
        assertEquals(50, bb.getMaxZ());
    }

    @Test
    void testGetBoundingBoxDisabledDimension() {
        // Nether not enabled
        when(iwm.isNetherGenerate(any())).thenReturn(false);
        assertNull(island.getBoundingBox(Environment.NETHER));
    }

    // ======================== Flags ========================

    @Test
    void testGetFlagDefault() {
        Flag flag = new Flag.Builder("TEST_FLAG", Material.STONE).build();
        // Default rank for PROTECTION is MEMBER_RANK (500)
        assertEquals(RanksManager.MEMBER_RANK, island.getFlag(flag));
    }

    @Test
    void testSetFlag() {
        Flag flag = new Flag.Builder("TEST_FLAG", Material.STONE).build();
        // Must first put the flag so setFlag sees it exists
        island.getFlags().put(flag.getID(), RanksManager.MEMBER_RANK);
        island.setFlag(flag, RanksManager.VISITOR_RANK);
        assertEquals(RanksManager.VISITOR_RANK, island.getFlag(flag));
    }

    @Test
    void testIsAllowedFlag() {
        Flag flag = new Flag.Builder("TEST_SETTING", Material.STONE)
                .type(Flag.Type.SETTING).build();
        // Positive value = allowed
        island.setSettingsFlag(flag, true);
        assertTrue(island.isAllowed(flag));

        // Negative value = not allowed
        island.setSettingsFlag(flag, false);
        assertFalse(island.isAllowed(flag));
    }

    @Test
    void testIsAllowedUserFlag() {
        Flag flag = new Flag.Builder("TEST_FLAG", Material.STONE).build();
        // Owner should have sufficient rank
        User ownerUser = mock(User.class);
        when(ownerUser.getUniqueId()).thenReturn(ownerUUID);
        when(ownerUser.isOp()).thenReturn(false);
        assertTrue(island.isAllowed(ownerUser, flag));

        // Visitor should not
        User visitor = mock(User.class);
        when(visitor.getUniqueId()).thenReturn(UUID.randomUUID());
        when(visitor.isOp()).thenReturn(false);
        assertFalse(island.isAllowed(visitor, flag));
    }

    @Test
    void testIsAllowedOpBypass() {
        Flag flag = new Flag.Builder("TEST_FLAG", Material.STONE).build();
        User opUser = mock(User.class);
        when(opUser.getUniqueId()).thenReturn(UUID.randomUUID());
        when(opUser.isOp()).thenReturn(true);
        assertTrue(island.isAllowed(opUser, flag));
    }

    @Test
    void testToggleFlag() {
        Flag flag = new Flag.Builder("TEST_SETTING", Material.STONE)
                .type(Flag.Type.SETTING).build();
        island.setSettingsFlag(flag, true);
        assertTrue(island.isAllowed(flag));

        island.toggleFlag(flag);
        assertFalse(island.isAllowed(flag));

        island.toggleFlag(flag);
        assertTrue(island.isAllowed(flag));
    }

    @Test
    void testSetSettingsFlag() {
        Flag flag = new Flag.Builder("SETTING_FLAG", Material.STONE)
                .type(Flag.Type.SETTING).build();
        island.setSettingsFlag(flag, true);
        assertEquals(1, island.getFlag(flag));

        island.setSettingsFlag(flag, false);
        assertEquals(-1, island.getFlag(flag));
    }

    @Test
    void testSetFlagsDefaults() {
        when(fm.getFlags()).thenReturn(Collections.emptyList());
        when(plugin.getFlagsManager()).thenReturn(fm);
        island.setFlagsDefaults();
        assertNotNull(island.getFlags());
    }

    // ======================== Homes ========================

    @Test
    void testGetHomesLazyInit() {
        Island i = new Island();
        assertNotNull(i.getHomes());
        assertTrue(i.getHomes().isEmpty());
    }

    @Test
    void testAddAndGetHome() {
        Location homeLoc = new Location(world, 10, 65, 10);
        island.addHome("myhome", homeLoc);
        assertEquals(homeLoc, island.getHome("myhome"));
    }

    @Test
    void testGetHomeCaseInsensitive() {
        Location homeLoc = new Location(world, 10, 65, 10);
        island.addHome("MyHome", homeLoc);
        assertEquals(homeLoc, island.getHome("myhome"));
        assertEquals(homeLoc, island.getHome("MYHOME"));
    }

    @Test
    void testGetHomeNotFound() {
        // Should return protection center + 0.5, 0, 0.5
        Location home = island.getHome("nonexistent");
        assertNotNull(home);
    }

    @Test
    void testRemoveHome() {
        island.addHome("removeme", new Location(world, 10, 65, 10));
        assertTrue(island.removeHome("removeme"));
        assertFalse(island.removeHome("removeme")); // already removed
    }

    @Test
    void testRemoveHomes() {
        island.addHome("", new Location(world, 0, 65, 0)); // default home
        island.addHome("extra1", new Location(world, 10, 65, 10));
        island.addHome("extra2", new Location(world, 20, 65, 20));

        assertTrue(island.removeHomes());
        // Default home should remain
        assertTrue(island.getHomes().containsKey(""));
        assertFalse(island.getHomes().containsKey("extra1"));
    }

    @Test
    void testRenameHome() {
        island.addHome("old", new Location(world, 10, 65, 10));
        assertTrue(island.renameHome("old", "new"));
        assertFalse(island.getHomes().containsKey("old"));
        assertTrue(island.getHomes().containsKey("new"));
    }

    @Test
    void testRenameHomeFailsIfNewNameExists() {
        island.addHome("old", new Location(world, 10, 65, 10));
        island.addHome("new", new Location(world, 20, 65, 20));
        assertFalse(island.renameHome("old", "new"));
    }

    @Test
    void testRenameHomeFailsIfOldNotFound() {
        assertFalse(island.renameHome("nonexistent", "new"));
    }

    @Test
    void testSetMaxHomes() {
        island.setMaxHomes(5);
        assertEquals(5, island.getMaxHomes());
        island.setMaxHomes(null);
        assertNull(island.getMaxHomes());
    }

    // ======================== Bonus Ranges ========================

    @Test
    void testGetProtectionRangeWithBonus() {
        island.addBonusRange("addon1", 20, "bonus.message");
        assertEquals(70, island.getProtectionRange()); // 50 + 20
    }

    @Test
    void testGetProtectionRangeCappedByRange() {
        island.addBonusRange("addon1", 200, "bonus.message");
        // Capped at range (100)
        assertEquals(100, island.getProtectionRange());
    }

    @Test
    void testGetBonusRange() {
        island.addBonusRange("addon1", 20, "msg");
        assertEquals(20, island.getBonusRange("addon1"));
        assertEquals(0, island.getBonusRange("nonexistent"));
    }

    @Test
    void testGetBonusRangeRecord() {
        island.addBonusRange("addon1", 20, "msg");
        assertTrue(island.getBonusRangeRecord("addon1").isPresent());
        assertFalse(island.getBonusRangeRecord("nonexistent").isPresent());
    }

    @Test
    void testClearBonusRange() {
        island.addBonusRange("addon1", 20, "msg");
        island.addBonusRange("addon2", 10, "msg2");
        island.clearBonusRange("addon1");
        assertEquals(0, island.getBonusRange("addon1"));
        assertEquals(10, island.getBonusRange("addon2"));
    }

    @Test
    void testClearAllBonusRanges() {
        island.addBonusRange("addon1", 20, "msg");
        island.addBonusRange("addon2", 10, "msg2");
        island.clearAllBonusRanges();
        assertTrue(island.getBonusRanges().isEmpty());
    }

    // ======================== Cooldowns ========================

    @Test
    void testIsCooldownActive() {
        Flag flag = mock(Flag.class);
        when(flag.getID()).thenReturn("TEST_COOLDOWN");
        when(flag.getCooldown()).thenReturn(60); // 60 seconds
        island.setCooldown(flag);
        assertTrue(island.isCooldown(flag));
    }

    @Test
    void testIsCooldownExpired() {
        Flag flag = mock(Flag.class);
        when(flag.getID()).thenReturn("TEST_COOLDOWN");
        // Put a cooldown in the past
        island.getCooldowns().put("TEST_COOLDOWN", System.currentTimeMillis() - 1000);
        assertFalse(island.isCooldown(flag));
    }

    @Test
    void testIsCooldownAbsent() {
        Flag flag = mock(Flag.class);
        when(flag.getID()).thenReturn("NO_COOLDOWN");
        assertFalse(island.isCooldown(flag));
    }

    // ======================== Command Ranks ========================

    @Test
    void testSetRankCommand() {
        island.setRankCommand("/island team", RanksManager.MEMBER_RANK);
        assertEquals(RanksManager.MEMBER_RANK, island.getCommandRanks().get("/island team"));
    }

    @Test
    void testSetRankCommandNoOpIfSame() {
        island.setRankCommand("/island team", RanksManager.MEMBER_RANK);
        island.setRankCommand("/island team", RanksManager.MEMBER_RANK); // no-op
        assertEquals(RanksManager.MEMBER_RANK, island.getCommandRanks().get("/island team"));
    }

    // ======================== Properties & Change Detection ========================

    @Test
    void testSetChanged() {
        island.clearChanged();
        assertFalse(island.isChanged());
        island.setChanged();
        assertTrue(island.isChanged());
    }

    @Test
    void testBeginDeferSaves() {
        assertFalse(island.isDeferSaves());
        island.beginDeferSaves();
        assertTrue(island.isDeferSaves());
    }

    @Test
    void testEndDeferSaves() {
        island.beginDeferSaves();
        assertTrue(island.isDeferSaves());
        island.endDeferSaves();
        assertFalse(island.isDeferSaves());
    }

    @Test
    void testSetChangedWhileDeferred() {
        island.beginDeferSaves();
        island.clearChanged();
        // setChanged should mark as changed but not trigger save while deferred
        island.setChanged();
        assertTrue(island.isChanged());
        assertTrue(island.isDeferSaves());
    }

    @Test
    void testEndDeferSavesTriggersUpdateWhenChanged() {
        island.beginDeferSaves();
        island.clearChanged();
        island.setChanged();
        assertTrue(island.isChanged());
        // End deferring - since changed is true, this will attempt to save
        island.endDeferSaves();
        assertFalse(island.isDeferSaves());
    }

    @Test
    void testNestedDeferSaves() {
        // Multiple callers can defer simultaneously (reference-counted)
        island.beginDeferSaves();
        island.beginDeferSaves();
        assertTrue(island.isDeferSaves());
        island.endDeferSaves();
        assertTrue(island.isDeferSaves()); // Still deferred (one remaining)
        island.endDeferSaves();
        assertFalse(island.isDeferSaves()); // Now fully ended
    }

    @Test
    void testEndDeferSavesNeverGoesNegative() {
        // Calling endDeferSaves without beginDeferSaves should not go negative
        island.endDeferSaves();
        assertFalse(island.isDeferSaves());
        // Subsequent begin/end should still work
        island.beginDeferSaves();
        assertTrue(island.isDeferSaves());
        island.endDeferSaves();
        assertFalse(island.isDeferSaves());
    }

    @Test
    void testSetName() {
        island.setName("MyIsland");
        assertEquals("MyIsland", island.getName());
    }

    @Test
    void testSetNameEmpty() {
        island.setName("");
        assertNull(island.getName());
    }

    @Test
    void testSetNameNull() {
        island.setName("test");
        island.setName(null);
        assertNull(island.getName());
    }

    @Test
    void testSetProtectionRange() {
        island.setProtectionRange(75);
        assertEquals(75, island.getRawProtectionRange());
    }

    @Test
    void testSetRange() {
        island.setRange(200);
        assertEquals(200, island.getRange());
    }

    @Test
    void testSetCenter() {
        Location newCenter = new Location(world, 500, 64, 500);
        island.setCenter(newCenter);
        assertEquals(500, island.getCenter().getBlockX());
        assertEquals(500, island.getCenter().getBlockZ());
    }

    @Test
    void testSetCreatedDate() {
        island.setCreatedDate(12345L);
        assertEquals(12345L, island.getCreatedDate());
    }

    @Test
    void testSetUpdatedDate() {
        island.setUpdatedDate(99999L);
        assertEquals(99999L, island.getUpdatedDate());
    }

    @Test
    void testSetWorld() {
        World newWorld = mock(World.class);
        island.setWorld(newWorld);
        assertEquals(newWorld, island.getWorld());
    }

    @Test
    void testSetPurgeProtected() {
        assertFalse(island.isPurgeProtected());
        island.setPurgeProtected(true);
        assertTrue(island.isPurgeProtected());
    }

    @Test
    void testSetDoNotLoad() {
        assertFalse(island.isDoNotLoad());
        island.setDoNotLoad(true);
        assertTrue(island.isDoNotLoad());
    }

    @Test
    void testSetDeleted() {
        assertFalse(island.isDeleted());
        island.setDeleted(true);
        assertTrue(island.isDeleted());
    }

    @Test
    void testSetSpawn() {
        when(fm.getFlags()).thenReturn(Collections.emptyList());
        when(plugin.getFlagsManager()).thenReturn(fm);

        island.setSpawn(true);
        assertTrue(island.isSpawn());
        assertNull(island.getOwner()); // owner cleared
        assertTrue(island.getMembers().isEmpty()); // members cleared
    }

    @Test
    void testSetSpawnFalse() {
        // Setting spawn to false when already false should be no-op
        assertFalse(island.isSpawn());
        island.setSpawn(false); // should not throw
        assertFalse(island.isSpawn());
    }

    @Test
    void testSetReserved() {
        assertFalse(island.isReserved());
        island.setReserved(true);
        assertTrue(island.isReserved());
        island.setReserved(false);
        assertFalse(island.isReserved());
    }

    @Test
    void testSetGameMode() {
        island.setGameMode("BSkyBlock");
        assertEquals("BSkyBlock", island.getGameMode());
    }

    @Test
    void testSetUniqueId() {
        island.setUniqueId("custom-id");
        assertEquals("custom-id", island.getUniqueId());
    }

    @Test
    void testSetFlags() {
        Map<String, Integer> flags = new HashMap<>();
        flags.put("FLAG1", 500);
        island.setFlags(flags);
        assertEquals(flags, island.getFlags());
    }

    @Test
    void testSetMembers() {
        Map<UUID, Integer> members = new HashMap<>();
        UUID m1 = UUID.randomUUID();
        members.put(m1, RanksManager.MEMBER_RANK);
        island.setMembers(members);
        assertEquals(RanksManager.MEMBER_RANK, island.getRank(m1));
    }

    @Test
    void testSetHistory() {
        List<org.bukkit.event.entity.EntityExplodeEvent> empty = Collections.emptyList();
        island.setHistory(Collections.emptyList());
        assertTrue(island.getHistory().isEmpty());
    }

    @Test
    void testLog() {
        var entry = new LogEntry.Builder(LogEntry.LogType.BAN)
                .data("player", "test").build();
        island.log(entry);
        assertFalse(island.getHistory().isEmpty());
    }

    // ======================== Protection Center ========================

    @Test
    void testGetProtectionCenterDefault() {
        // When location is null, returns center
        assertEquals(island.getCenter().getBlockX(), island.getProtectionCenter().getBlockX());
    }

    @Test
    void testSetProtectionCenter() throws IOException {
        Location newCenter = mock(Location.class);
        when(newCenter.getWorld()).thenReturn(world);
        when(newCenter.getBlockX()).thenReturn(10);
        when(newCenter.getBlockY()).thenReturn(64);
        when(newCenter.getBlockZ()).thenReturn(10);
        when(newCenter.clone()).thenReturn(newCenter);
        island.setProtectionCenter(newCenter);
        assertEquals(10, island.getProtectionCenter().getBlockX());
    }

    @Test
    void testSetProtectionCenterOutsideIslandSpace() {
        Location outsideLoc = mock(Location.class);
        when(outsideLoc.getWorld()).thenReturn(world);
        when(outsideLoc.getBlockX()).thenReturn(500);
        when(outsideLoc.getBlockY()).thenReturn(64);
        when(outsideLoc.getBlockZ()).thenReturn(500);
        when(outsideLoc.clone()).thenReturn(outsideLoc);
        assertThrows(IOException.class, () -> island.setProtectionCenter(outsideLoc));
    }

    // ======================== Spawn Points ========================

    @Test
    void testSetSpawnPoint() {
        Location spawnLoc = new Location(world, 5, 65, 5);
        island.setSpawnPoint(Environment.NORMAL, spawnLoc);
        assertNotNull(island.getSpawnPoint(Environment.NORMAL));
    }

    @Test
    void testGetSpawnPointNull() {
        assertNull(island.getSpawnPoint(Environment.NETHER));
    }

    // ======================== MaxMembers ========================

    @Test
    void testGetMaxMembersLazyInit() {
        Island i = new Island();
        assertNotNull(i.getMaxMembers());
        assertTrue(i.getMaxMembers().isEmpty());
    }

    @Test
    void testSetMaxMembersByRank() {
        island.setMaxMembers(RanksManager.MEMBER_RANK, 10);
        assertEquals(10, island.getMaxMembers(RanksManager.MEMBER_RANK));
        assertNull(island.getMaxMembers(RanksManager.COOP_RANK));
    }

    // ======================== Max Ever Protection Range ========================

    @Test
    void testGetMaxEverProtectionRange() {
        // Initially set to protectionRange (50)
        assertEquals(50, island.getMaxEverProtectionRange());
    }

    @Test
    void testSetMaxEverProtectionRange() {
        island.setMaxEverProtectionRange(75);
        assertEquals(75, island.getMaxEverProtectionRange());
    }

    @Test
    void testSetMaxEverProtectionRangeCappedByRange() {
        // Range is 100, so max ever should be capped at 100
        island.setMaxEverProtectionRange(200);
        assertEquals(100, island.getMaxEverProtectionRange());
    }

    @Test
    void testSetMaxEverProtectionRangeNoDecrease() {
        island.setMaxEverProtectionRange(75);
        island.setMaxEverProtectionRange(50); // should not decrease
        assertEquals(75, island.getMaxEverProtectionRange());
    }

    // ======================== Metadata ========================

    @Test
    void testGetMetaDataLazyInit() {
        Island i = new Island();
        assertTrue(i.getMetaData().isPresent());
        assertTrue(i.getMetaData().get().isEmpty());
    }

    @Test
    void testSetMetaData() {
        Map<String, MetaDataValue> meta = new HashMap<>();
        meta.put("key", new MetaDataValue("value"));
        island.setMetaData(meta);
        assertTrue(island.getMetaData().isPresent());
        assertEquals("value", island.getMetaData().get().get("key").asString());
    }

    // ======================== Primaries ========================

    @Test
    void testSetPrimary() {
        UUID user = UUID.randomUUID();
        assertFalse(island.isPrimary(user));
        island.setPrimary(user);
        assertTrue(island.isPrimary(user));
    }

    @Test
    void testRemovePrimary() {
        UUID user = UUID.randomUUID();
        island.setPrimary(user);
        island.removePrimary(user);
        assertFalse(island.isPrimary(user));
    }

    @Test
    void testSetPrimaries() {
        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();
        island.setPrimaries(Set.of(u1, u2));
        assertTrue(island.isPrimary(u1));
        assertTrue(island.isPrimary(u2));
    }

    // ======================== Equals / HashCode / ToString ========================

    @Test
    void testEqualsAndHashCode() {
        Island other = new Island();
        other.setUniqueId(island.getUniqueId());
        assertEquals(island, other);
        assertEquals(island.hashCode(), other.hashCode());
    }

    @Test
    void testNotEquals() {
        Island other = new Island();
        // Different unique IDs
        assertNotEquals(island, other);
    }

    @Test
    void testEqualsSameObject() {
        assertEquals(island, island);
    }

    @Test
    void testEqualsNull() {
        assertNotEquals(null, island);
    }

    @Test
    void testEqualsDifferentClass() {
        assertNotEquals("not an island", island);
    }

    @Test
    void testToString() {
        String str = island.toString();
        assertNotNull(str);
        assertTrue(str.contains("Island"));
        assertTrue(str.contains(island.getUniqueId()));
    }

    // ======================== Owned / Unowned ========================

    @Test
    void testIsOwned() {
        assertTrue(island.isOwned());
    }

    @Test
    void testIsUnowned() {
        island.setOwner(null);
        assertTrue(island.isUnowned());
        assertFalse(island.isOwned());
    }

    // ======================== Deletable ========================

    @Test
    void testSetDeletable() {
        assertFalse(island.isDeletable());
        island.setDeletable(true);
        assertTrue(island.isDeletable());
    }

    // ======================== Dimension Methods ========================

    @Test
    void testGetWorldNormal() {
        assertEquals(world, island.getWorld(Environment.NORMAL));
    }

    @Test
    void testGetWorldNetherDisabled() {
        when(iwm.isNetherGenerate(any())).thenReturn(false);
        assertNull(island.getWorld(Environment.NETHER));
    }

    @Test
    void testGetWorldNetherEnabled() {
        World netherWorld = mock(World.class);
        when(iwm.isNetherGenerate(any())).thenReturn(true);
        when(iwm.isNetherIslands(any())).thenReturn(true);
        when(iwm.getNetherWorld(any())).thenReturn(netherWorld);
        assertEquals(netherWorld, island.getWorld(Environment.NETHER));
    }

    @Test
    void testGetWorldEndDisabled() {
        when(iwm.isEndGenerate(any())).thenReturn(false);
        assertNull(island.getWorld(Environment.THE_END));
    }

    @Test
    void testGetXYZ() {
        assertEquals(0, island.getX());
        assertEquals(64, island.getY());
        assertEquals(0, island.getZ());
    }

    // ======================== Cooldowns Map ========================

    @Test
    void testSetCooldowns() {
        Map<String, Long> cooldowns = new HashMap<>();
        cooldowns.put("flag1", System.currentTimeMillis() + 60000);
        island.setCooldowns(cooldowns);
        assertEquals(cooldowns, island.getCooldowns());
    }

    // ======================== Command Ranks Map ========================

    @Test
    void testSetCommandRanks() {
        Map<String, Integer> ranks = new HashMap<>();
        ranks.put("/cmd", RanksManager.MEMBER_RANK);
        island.setCommandRanks(ranks);
        assertEquals(ranks, island.getCommandRanks());
    }

    // ======================== Spawn Point Map ========================

    @Test
    void testSetSpawnPointMap() {
        Map<Environment, Location> spawnPoints = new java.util.EnumMap<>(Environment.class);
        Location loc = new Location(world, 5, 65, 5);
        spawnPoints.put(Environment.NORMAL, loc);
        island.setSpawnPoint(spawnPoints);
        assertEquals(loc, island.getSpawnPoint().get(Environment.NORMAL));
    }

    // ======================== GetCenter clone ========================

    @Test
    void testGetCenterReturnsClone() {
        Location c1 = island.getCenter();
        Location c2 = island.getCenter();
        assertNotSame(c1, c2);
        assertEquals(c1.getBlockX(), c2.getBlockX());
    }
}
