package world.bentobox.bentobox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.bukkit.Material;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(BentoBox.class)
public class SettingsTest {

    private Settings s;

    /**
     */
    @Before
    public void setUp() throws Exception {
        Whitebox.setInternalState(BentoBox.class, "instance", Mockito.mock(BentoBox.class));
        // Class under test
        s = new Settings();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDefaultLanguage()}.
     */
    @Test
    public void testGetDefaultLanguage() {
        assertEquals("en-US", s.getDefaultLanguage());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDefaultLanguage(java.lang.String)}.
     */
    @Test
    public void testSetDefaultLanguage() {
        s.setDefaultLanguage("test");
        assertEquals("test", s.getDefaultLanguage());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isUseEconomy()}.
     */
    @Test
    public void testIsUseEconomy() {
        assertTrue(s.isUseEconomy());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setUseEconomy(boolean)}.
     */
    @Test
    public void testSetUseEconomy() {
        s.setUseEconomy(false);
        assertFalse(s.isUseEconomy());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabaseType()}.
     */
    @Test
    public void testGetDatabaseType() {
        assertEquals(DatabaseType.JSON, s.getDatabaseType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabaseType(world.bentobox.bentobox.database.DatabaseSetup.DatabaseType)}.
     */
    @Test
    public void testSetDatabaseType() {
        s.setDatabaseType(DatabaseType.JSON2MONGODB);
        assertEquals(DatabaseType.JSON2MONGODB, s.getDatabaseType());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabaseHost()}.
     */
    @Test
    public void testGetDatabaseHost() {
        assertEquals("localhost", s.getDatabaseHost());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabaseHost(java.lang.String)}.
     */
    @Test
    public void testSetDatabaseHost() {
        s.setDatabaseHost("remotehost");
        assertEquals("remotehost", s.getDatabaseHost());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabasePort()}.
     */
    @Test
    public void testGetDatabasePort() {
        assertEquals(3306, s.getDatabasePort());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isUseSSL()}.
     */
    @Test
    public void testIsUseSSL() {
        assertFalse(s.isUseSSL());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setUseSSL(boolean)}.
     */
    @Test
    public void testSetUseSSL() {
        s.setUseSSL(false);
        assertFalse(s.isUseSSL());
        s.setUseSSL(true);
        assertTrue(s.isUseSSL());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabasePort(int)}.
     */
    @Test
    public void testSetDatabasePort() {
        s.setDatabasePort(1234);
        assertEquals(1234, s.getDatabasePort());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabaseName()}.
     */
    @Test
    public void testGetDatabaseName() {
        assertEquals("bentobox", s.getDatabaseName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabaseName(java.lang.String)}.
     */
    @Test
    public void testSetDatabaseName() {
        s.setDatabaseName("fredthedoggy");
        assertEquals("fredthedoggy", s.getDatabaseName());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabaseUsername()}.
     */
    @Test
    public void testGetDatabaseUsername() {
        assertEquals("username", s.getDatabaseUsername());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabaseUsername(java.lang.String)}.
     */
    @Test
    public void testSetDatabaseUsername() {
        s.setDatabaseUsername("BONNe");
        assertEquals("BONNe", s.getDatabaseUsername());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabasePassword()}.
     */
    @Test
    public void testGetDatabasePassword() {
        assertEquals("password", s.getDatabasePassword());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabasePassword(java.lang.String)}.
     */
    @Test
    public void testSetDatabasePassword() {
        s.setDatabasePassword("afero");
        assertEquals("afero", s.getDatabasePassword());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabaseBackupPeriod()}.
     */
    @Test
    public void testGetDatabaseBackupPeriod() {
        assertEquals(5, s.getDatabaseBackupPeriod());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabaseBackupPeriod(int)}.
     */
    @Test
    public void testSetDatabaseBackupPeriod() {
        s.setDatabaseBackupPeriod(10);
        assertEquals(10, s.getDatabaseBackupPeriod());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getFakePlayers()}.
     */
    @Test
    public void testGetFakePlayers() {
        assertTrue(s.getFakePlayers().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setFakePlayers(java.util.Set)}.
     */
    @Test
    public void testSetFakePlayers() {
        s.setFakePlayers(Collections.singleton("npc"));
        assertTrue(s.getFakePlayers().contains("npc"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isClosePanelOnClickOutside()}.
     */
    @Test
    public void testIsClosePanelOnClickOutside() {
        assertTrue(s.isClosePanelOnClickOutside());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setClosePanelOnClickOutside(boolean)}.
     */
    @Test
    public void testSetClosePanelOnClickOutside() {
        assertTrue(s.isClosePanelOnClickOutside());
        s.setClosePanelOnClickOutside(false);
        assertFalse(s.isClosePanelOnClickOutside());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getInviteCooldown()}.
     */
    @Test
    public void testGetInviteCooldown() {
        assertEquals(60, s.getInviteCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setInviteCooldown(int)}.
     */
    @Test
    public void testSetInviteCooldown() {
        s.setInviteCooldown(99);
        assertEquals(99, s.getInviteCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getCoopCooldown()}.
     */
    @Test
    public void testGetCoopCooldown() {
        assertEquals(5, s.getCoopCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setCoopCooldown(int)}.
     */
    @Test
    public void testSetCoopCooldown() {
        s.setCoopCooldown(15);
        assertEquals(15, s.getCoopCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getTrustCooldown()}.
     */
    @Test
    public void testGetTrustCooldown() {
        assertEquals(5, s.getTrustCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setTrustCooldown(int)}.
     */
    @Test
    public void testSetTrustCooldown() {
        s.setTrustCooldown(15);
        assertEquals(15, s.getTrustCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getBanCooldown()}.
     */
    @Test
    public void testGetBanCooldown() {
        assertEquals(10, s.getBanCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setBanCooldown(int)}.
     */
    @Test
    public void testSetBanCooldown() {
        s.setBanCooldown(99);
        assertEquals(99, s.getBanCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getResetCooldown()}.
     */
    @Test
    public void testGetResetCooldown() {
        assertEquals(300, s.getResetCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setResetCooldown(int)}.
     */
    @Test
    public void testSetResetCooldown() {
        s.setResetCooldown(3);
        assertEquals(3, s.getResetCooldown());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getConfirmationTime()}.
     */
    @Test
    public void testGetConfirmationTime() {
        assertEquals(10, s.getConfirmationTime());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setConfirmationTime(int)}.
     */
    @Test
    public void testSetConfirmationTime() {
        s.setConfirmationTime(100);
        assertEquals(100, s.getConfirmationTime());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isKickConfirmation()}.
     */
    @Test
    public void testIsKickConfirmation() {
        assertTrue(s.isKickConfirmation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setKickConfirmation(boolean)}.
     */
    @Test
    public void testSetKickConfirmation() {
        assertTrue(s.isKickConfirmation());
        s.setKickConfirmation(false);
        assertFalse(s.isKickConfirmation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isLeaveConfirmation()}.
     */
    @Test
    public void testIsLeaveConfirmation() {
        assertTrue(s.isLeaveConfirmation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setLeaveConfirmation(boolean)}.
     */
    @Test
    public void testSetLeaveConfirmation() {
        assertTrue(s.isLeaveConfirmation());
        s.setLeaveConfirmation(false);
        assertFalse(s.isLeaveConfirmation());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isResetConfirmation()}.
     */
    @Test
    public void testIsResetConfirmation() {
        assertTrue(s.isResetConfirmation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setResetConfirmation(boolean)}.
     */
    @Test
    public void testSetResetConfirmation() {
        assertTrue(s.isResetConfirmation());
        s.setResetConfirmation(false);
        assertFalse(s.isResetConfirmation());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getNameMinLength()}.
     */
    @Test
    public void testGetNameMinLength() {
        assertEquals(4, s.getNameMinLength());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setNameMinLength(int)}.
     */
    @Test
    public void testSetNameMinLength() {
        assertEquals(4, s.getNameMinLength());
        s.setNameMinLength(2);
        assertEquals(2, s.getNameMinLength());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getNameMaxLength()}.
     */
    @Test
    public void testGetNameMaxLength() {
        assertEquals(20, s.getNameMaxLength());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setNameMaxLength(int)}.
     */
    @Test
    public void testSetNameMaxLength() {
        assertEquals(20, s.getNameMaxLength());
        s.setNameMaxLength(2);
        assertEquals(2, s.getNameMaxLength());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isNameUniqueness()}.
     */
    @Test
    public void testIsNameUniqueness() {
        assertFalse(s.isNameUniqueness());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setNameUniqueness(boolean)}.
     */
    @Test
    public void testSetNameUniqueness() {
        assertFalse(s.isNameUniqueness());
        s.setNameUniqueness(true);
        assertTrue(s.isNameUniqueness());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setPasteSpeed(int)}.
     */
    @Test
    public void testSetPasteSpeed() {
        assertEquals(64, s.getPasteSpeed());
        s.setPasteSpeed(100);
        assertEquals(100, s.getPasteSpeed());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getPasteSpeed()}.
     */
    @Test
    public void testGetPasteSpeed() {
        assertEquals(64, s.getPasteSpeed());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDeleteSpeed()}.
     */
    @Test
    public void testGetDeleteSpeed() {
        assertEquals(1, s.getDeleteSpeed());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDeleteSpeed(int)}.
     */
    @Test
    public void testSetDeleteSpeed() {
        assertEquals(1, s.getDeleteSpeed());
        s.setDeleteSpeed(4);
        assertEquals(4, s.getDeleteSpeed());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isEnableAutoOwnershipTransfer()}.
     */
    @Test
    public void testIsEnableAutoOwnershipTransfer() {
        assertFalse(s.isEnableAutoOwnershipTransfer());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setEnableAutoOwnershipTransfer(boolean)}.
     */
    @Test
    public void testSetEnableAutoOwnershipTransfer() {
        assertFalse(s.isEnableAutoOwnershipTransfer());
        s.setEnableAutoOwnershipTransfer(true);
        assertTrue(s.isEnableAutoOwnershipTransfer());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getAutoOwnershipTransferInactivityThreshold()}.
     */
    @Test
    public void testGetAutoOwnershipTransferInactivityThreshold() {
        assertEquals(30, s.getAutoOwnershipTransferInactivityThreshold());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setAutoOwnershipTransferInactivityThreshold(int)}.
     */
    @Test
    public void testSetAutoOwnershipTransferInactivityThreshold() {
        assertEquals(30, s.getAutoOwnershipTransferInactivityThreshold());
        s.setAutoOwnershipTransferInactivityThreshold(1234);
        assertEquals(1234, s.getAutoOwnershipTransferInactivityThreshold());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isAutoOwnershipTransferIgnoreRanks()}.
     */
    @Test
    public void testIsAutoOwnershipTransferIgnoreRanks() {
        assertFalse(s.isAutoOwnershipTransferIgnoreRanks());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setAutoOwnershipTransferIgnoreRanks(boolean)}.
     */
    @Test
    public void testSetAutoOwnershipTransferIgnoreRanks() {
        assertFalse(s.isAutoOwnershipTransferIgnoreRanks());
        s.setAutoOwnershipTransferIgnoreRanks(true);
        assertTrue(s.isAutoOwnershipTransferIgnoreRanks());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isLogCleanSuperFlatChunks()}.
     */
    @Test
    public void testIsLogCleanSuperFlatChunks() {
        assertTrue(s.isLogCleanSuperFlatChunks());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setLogCleanSuperFlatChunks(boolean)}.
     */
    @Test
    public void testSetLogCleanSuperFlatChunks() {
        assertTrue(s.isLogCleanSuperFlatChunks());
        s.setLogCleanSuperFlatChunks(false);
        assertFalse(s.isLogCleanSuperFlatChunks());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isResetCooldownOnCreate()}.
     */
    @Test
    public void testIsResetCooldownOnCreate() {
        assertTrue(s.isResetCooldownOnCreate());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setResetCooldownOnCreate(boolean)}.
     */
    @Test
    public void testSetResetCooldownOnCreate() {
        assertTrue(s.isResetCooldownOnCreate());
        s.setResetCooldownOnCreate(false);
        assertFalse(s.isResetCooldownOnCreate());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isGithubDownloadData()}.
     */
    @Test
    public void testIsGithubDownloadData() {
        assertTrue(s.isGithubDownloadData());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setGithubDownloadData(boolean)}.
     */
    @Test
    public void testSetGithubDownloadData() {
        assertTrue(s.isGithubDownloadData());
        s.setGithubDownloadData(false);
        assertFalse(s.isGithubDownloadData());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getGithubConnectionInterval()}.
     */
    @Test
    public void testGetGithubConnectionInterval() {
        assertEquals(120, s.getGithubConnectionInterval());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setGithubConnectionInterval(int)}.
     */
    @Test
    public void testSetGithubConnectionInterval() {
        assertEquals(120, s.getGithubConnectionInterval());
        s.setGithubConnectionInterval(20);
        assertEquals(20, s.getGithubConnectionInterval());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isCheckBentoBoxUpdates()}.
     */
    @Test
    public void testIsCheckBentoBoxUpdates() {
        assertTrue(s.isCheckBentoBoxUpdates());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setCheckBentoBoxUpdates(boolean)}.
     */
    @Test
    public void testSetCheckBentoBoxUpdates() {
        assertTrue(s.isCheckBentoBoxUpdates());
        s.setCheckBentoBoxUpdates(false);
        assertFalse(s.isCheckBentoBoxUpdates());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isCheckAddonsUpdates()}.
     */
    @Test
    public void testIsCheckAddonsUpdates() {
        assertTrue(s.isCheckAddonsUpdates());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setCheckAddonsUpdates(boolean)}.
     */
    @Test
    public void testSetCheckAddonsUpdates() {
        assertTrue(s.isCheckAddonsUpdates());
        s.setCheckAddonsUpdates(false);
        assertFalse(s.isCheckAddonsUpdates());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isLogGithubDownloadData()}.
     */
    @Test
    public void testIsLogGithubDownloadData() {
        assertTrue(s.isLogGithubDownloadData());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setLogGithubDownloadData(boolean)}.
     */
    @Test
    public void testSetLogGithubDownloadData() {
        assertTrue(s.isLogGithubDownloadData());
        s.setLogGithubDownloadData(false);
        assertFalse(s.isLogGithubDownloadData());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDelayTime()}.
     */
    @Test
    public void testGetDelayTime() {
        assertEquals(0, s.getDelayTime());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDelayTime(int)}.
     */
    @Test
    public void testSetDelayTime() {
        assertEquals(0, s.getDelayTime());
        s.setDelayTime(10);
        assertEquals(10, s.getDelayTime());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getClearRadius()}.
     */
    @Test
    public void testGetClearRadius() {
        assertEquals(5, s.getClearRadius());

    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setClearRadius(int)}.
     */
    @Test
    public void testSetClearRadius() {
        assertEquals(5, s.getClearRadius());
        s.setClearRadius(20);
        assertEquals(20, s.getClearRadius());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isInviteConfirmation()}.
     */
    @Test
    public void testIsInviteConfirmation() {
        assertFalse(s.isInviteConfirmation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setInviteConfirmation(boolean)}.
     */
    @Test
    public void testSetInviteConfirmation() {
        assertFalse(s.isInviteConfirmation());
        s.setInviteConfirmation(true);
        assertTrue(s.isInviteConfirmation());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getDatabasePrefix()}.
     */
    @Test
    public void testGetDatabasePrefix() {
        assertEquals("", s.getDatabasePrefix());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setDatabasePrefix(java.lang.String)}.
     */
    @Test
    public void testSetDatabasePrefix() {
        assertEquals("", s.getDatabasePrefix());
        s.setDatabasePrefix("Prefix");
        assertEquals("Prefix", s.getDatabasePrefix());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#isKeepPreviousIslandOnReset()}.
     */
    @Test
    public void testIsKeepPreviousIslandOnReset() {
        assertFalse(s.isKeepPreviousIslandOnReset());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setKeepPreviousIslandOnReset(boolean)}.
     */
    @Test
    public void testSetKeepPreviousIslandOnReset() {
        assertFalse(s.isKeepPreviousIslandOnReset());
        s.setKeepPreviousIslandOnReset(true);
        assertTrue(s.isKeepPreviousIslandOnReset());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getMongodbConnectionUri()}.
     */
    @Test
    public void testGetMongodbConnectionUri() {
        assertEquals("", s.getMongodbConnectionUri());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setMongodbConnectionUri(java.lang.String)}.
     */
    @Test
    public void testSetMongodbConnectionUri() {
        assertEquals("", s.getMongodbConnectionUri());
        s.setMongodbConnectionUri("xyz");
        assertEquals("xyz", s.getMongodbConnectionUri());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getPanelFillerMaterial()}.
     */
    @Test
    public void testGetPanelFillerMaterial() {
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, s.getPanelFillerMaterial());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setPanelFillerMaterial(org.bukkit.Material)}.
     */
    @Test
    public void testSetPanelFillerMaterial() {
        assertEquals(Material.LIGHT_BLUE_STAINED_GLASS_PANE, s.getPanelFillerMaterial());
        s.setPanelFillerMaterial(Material.ACACIA_BOAT);
        assertEquals(Material.ACACIA_BOAT, s.getPanelFillerMaterial());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#getPlayerHeadCacheTime()}.
     */
    @Test
    public void testGetPlayerHeadCacheTime() {
        assertEquals(60L, s.getPlayerHeadCacheTime());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.Settings#setPlayerHeadCacheTime(long)}.
     */
    @Test
    public void testSetPlayerHeadCacheTime() {
        assertEquals(60L, s.getPlayerHeadCacheTime());
        s.setPlayerHeadCacheTime(0L);
        assertEquals(0L, s.getPlayerHeadCacheTime());
    }

}
