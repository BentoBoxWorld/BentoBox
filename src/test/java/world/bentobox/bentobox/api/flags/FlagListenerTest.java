package world.bentobox.bentobox.api.flags;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;

import org.bukkit.Bukkit;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.user.User;

/**
 * Tests for the addon why-debug report API in {@link FlagListener}.
 */
class FlagListenerTest extends CommonTestSetup {

    // Concrete subclass for testing the abstract FlagListener
    static class TestFlagListener extends FlagListener {
    }

    private TestFlagListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        User.setPlugin(plugin);

        // World name used as the metadata key prefix
        when(world.getName()).thenReturn("bskyblock_world");

        // Location in the world
        when(location.toVector()).thenReturn(new Vector(10, 64, 20));

        // Enable why-debug on mockPlayer for this world, issuer is the same player (uuid)
        when(mockPlayer.getMetadata("bskyblock_world_why_debug"))
                .thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, true)));
        when(mockPlayer.getMetadata("bskyblock_world_why_debug_issuer"))
                .thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, uuid.toString())));

        // Ensure mockPlayer is registered as a User so issuer lookup works
        User.getInstance(mockPlayer);

        // Mock Bukkit.getOnlinePlayers() to include mockPlayer
        @SuppressWarnings("unchecked")
        Collection<? extends org.bukkit.entity.Player> online = Collections.singletonList(mockPlayer);
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(online);

        listener = new TestFlagListener();
        listener.setPlugin(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that report(loc, message) sends message to issuer when why-debug is enabled.
     */
    @Test
    void testReportLocMessage() {
        listener.report(location, "Custom addon message");
        checkSpigotMessage("Why: Custom addon message - INFO in world bskyblock_world at 10,64,20");
    }

    /**
     * Test that report(addon, loc, message) includes the addon name as prefix.
     */
    @Test
    void testReportAddonLocMessage() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "MyAddon", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);

        listener.report(addon, location, "Custom addon message");
        checkSpigotMessage("Why: [MyAddon] Custom addon message - INFO in world bskyblock_world at 10,64,20");
    }

    /**
     * Test that report(addon, loc, message, reason) uses the given reason.
     */
    @Test
    void testReportAddonLocMessageReason() {
        Addon addon = mock(Addon.class);
        AddonDescription desc = new AddonDescription.Builder("main", "MyAddon", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);

        listener.report(addon, location, "Something was bypassed", FlagListener.Reason.BYPASS);
        checkSpigotMessage("Why: [MyAddon] Something was bypassed - BYPASS in world bskyblock_world at 10,64,20");
    }

    /**
     * Test that report(null addon, loc, message) skips the prefix when addon is null.
     */
    @Test
    void testReportNullAddonLocMessage() {
        listener.report(null, location, "No addon message");
        checkSpigotMessage("Why: No addon message - INFO in world bskyblock_world at 10,64,20");
    }

    /**
     * Test all Reason enum values report without error.
     */
    @Test
    void testReportAllReasons() {
        for (FlagListener.Reason reason : FlagListener.Reason.values()) {
            listener.report(null, location, "Test message", reason);
            checkSpigotMessage("Why: Test message - " + reason.name() + " in world bskyblock_world at 10,64,20", 1);
        }
    }

    /**
     * Test that report does nothing when location world is null.
     */
    @Test
    void testReportNullWorld() {
        when(location.getWorld()).thenReturn(null);
        // Should not throw and should not send any message
        listener.report(location, "Should not be sent");
        checkSpigotMessage("Why: Should not be sent", 0);
    }

    /**
     * Test that report does nothing when no players have why-debug enabled.
     */
    @Test
    void testReportNoPlayersWithDebug() {
        // Override: all metadata returns the non-debug default
        when(mockPlayer.getMetadata(anyString()))
                .thenReturn(Collections.singletonList(new FixedMetadataValue(plugin, false)));
        listener.report(location, "Should not be sent");
        checkSpigotMessage("Why: Should not be sent", 0);
    }

    /**
     * Test that report does nothing gracefully when no players are online.
     */
    @Test
    void testReportNoOnlinePlayers() {
        mockedBukkit.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
        // Should not throw
        listener.report(location, "No one is watching");
        checkSpigotMessage("Why: No one is watching", 0);
    }

}

