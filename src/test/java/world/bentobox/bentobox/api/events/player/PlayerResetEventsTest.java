package world.bentobox.bentobox.api.events.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests for the player-reset event family:
 * {@link PlayerResetEnderChestEvent}, {@link PlayerResetExpEvent},
 * {@link PlayerResetHealthEvent}, {@link PlayerResetHungerEvent},
 * {@link PlayerResetInventoryEvent}, {@link PlayerResetMoneyEvent},
 * {@link PlayerTamedRemovalEvent}, and {@link PlayerUnknownEvent}.
 *
 * <p>Each event is verified for:</p>
 * <ul>
 *   <li>Correct construction (non-null, correct island/world/player accessors)</li>
 *   <li>Default cancelled state (false)</li>
 *   <li>Cancel / un-cancel round-trip</li>
 * </ul>
 * {@link PlayerResetInventoryEvent} additionally exercises its {@link org.bukkit.event.HandlerList}.
 *
 * @author tastybento
 */
class PlayerResetEventsTest extends CommonTestSetup {

    /** A fresh UUID that represents the player being reset. */
    private UUID playerUUID;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        playerUUID = UUID.randomUUID();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    // -----------------------------------------------------------------------
    // PlayerResetEnderChestEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerResetEnderChestEvent#PlayerResetEnderChestEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     * Verifies that the event is created and its accessors return the expected values.
     */
    @Test
    void testResetEnderChestEventConstruction() {
        PlayerResetEnderChestEvent event = new PlayerResetEnderChestEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerResetEnderChestEvent} is not cancelled by default.
     */
    @Test
    void testResetEnderChestEventNotCancelledByDefault() {
        PlayerResetEnderChestEvent event = new PlayerResetEnderChestEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerResetEnderChestEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testResetEnderChestEventCancellation() {
        PlayerResetEnderChestEvent event = new PlayerResetEnderChestEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    // -----------------------------------------------------------------------
    // PlayerResetExpEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerResetExpEvent#PlayerResetExpEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     */
    @Test
    void testResetExpEventConstruction() {
        PlayerResetExpEvent event = new PlayerResetExpEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerResetExpEvent} is not cancelled by default.
     */
    @Test
    void testResetExpEventNotCancelledByDefault() {
        PlayerResetExpEvent event = new PlayerResetExpEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerResetExpEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testResetExpEventCancellation() {
        PlayerResetExpEvent event = new PlayerResetExpEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    // -----------------------------------------------------------------------
    // PlayerResetHealthEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerResetHealthEvent#PlayerResetHealthEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     */
    @Test
    void testResetHealthEventConstruction() {
        PlayerResetHealthEvent event = new PlayerResetHealthEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerResetHealthEvent} is not cancelled by default.
     */
    @Test
    void testResetHealthEventNotCancelledByDefault() {
        PlayerResetHealthEvent event = new PlayerResetHealthEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerResetHealthEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testResetHealthEventCancellation() {
        PlayerResetHealthEvent event = new PlayerResetHealthEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    // -----------------------------------------------------------------------
    // PlayerResetHungerEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerResetHungerEvent#PlayerResetHungerEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     */
    @Test
    void testResetHungerEventConstruction() {
        PlayerResetHungerEvent event = new PlayerResetHungerEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerResetHungerEvent} is not cancelled by default.
     */
    @Test
    void testResetHungerEventNotCancelledByDefault() {
        PlayerResetHungerEvent event = new PlayerResetHungerEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerResetHungerEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testResetHungerEventCancellation() {
        PlayerResetHungerEvent event = new PlayerResetHungerEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    // -----------------------------------------------------------------------
    // PlayerResetInventoryEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerResetInventoryEvent#PlayerResetInventoryEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     */
    @Test
    void testResetInventoryEventConstruction() {
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerResetInventoryEvent} is not cancelled by default.
     */
    @Test
    void testResetInventoryEventNotCancelledByDefault() {
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerResetInventoryEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testResetInventoryEventCancellation() {
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    /**
     * Test method for {@link PlayerResetInventoryEvent#getHandlers()}.
     * Verifies that the instance handler list is non-null.
     */
    @Test
    void testResetInventoryEventGetHandlers() {
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        assertNotNull(event.getHandlers());
    }

    /**
     * Test method for {@link PlayerResetInventoryEvent#getHandlerList()}.
     * Verifies that the static handler list is non-null and the same object returned
     * by the instance method.
     */
    @Test
    void testResetInventoryEventGetHandlerList() {
        PlayerResetInventoryEvent event = new PlayerResetInventoryEvent(world, island, playerUUID);
        assertNotNull(PlayerResetInventoryEvent.getHandlerList());
        assertSame(PlayerResetInventoryEvent.getHandlerList(), event.getHandlers());
    }

    // -----------------------------------------------------------------------
    // PlayerResetMoneyEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerResetMoneyEvent#PlayerResetMoneyEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     */
    @Test
    void testResetMoneyEventConstruction() {
        PlayerResetMoneyEvent event = new PlayerResetMoneyEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerResetMoneyEvent} is not cancelled by default.
     */
    @Test
    void testResetMoneyEventNotCancelledByDefault() {
        PlayerResetMoneyEvent event = new PlayerResetMoneyEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerResetMoneyEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testResetMoneyEventCancellation() {
        PlayerResetMoneyEvent event = new PlayerResetMoneyEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    // -----------------------------------------------------------------------
    // PlayerTamedRemovalEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerTamedRemovalEvent#PlayerTamedRemovalEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     */
    @Test
    void testTamedRemovalEventConstruction() {
        PlayerTamedRemovalEvent event = new PlayerTamedRemovalEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerTamedRemovalEvent} is not cancelled by default.
     */
    @Test
    void testTamedRemovalEventNotCancelledByDefault() {
        PlayerTamedRemovalEvent event = new PlayerTamedRemovalEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerTamedRemovalEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testTamedRemovalEventCancellation() {
        PlayerTamedRemovalEvent event = new PlayerTamedRemovalEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    // -----------------------------------------------------------------------
    // PlayerUnknownEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for
     * {@link PlayerUnknownEvent#PlayerUnknownEvent(org.bukkit.World, world.bentobox.bentobox.database.objects.Island, UUID)}.
     */
    @Test
    void testUnknownEventConstruction() {
        PlayerUnknownEvent event = new PlayerUnknownEvent(world, island, playerUUID);
        assertNotNull(event);
        assertSame(island, event.getIsland());
        assertSame(world, event.getWorld());
        assertSame(playerUUID, event.getPlayerUUID());
    }

    /**
     * Verifies that a newly created {@link PlayerUnknownEvent} is not cancelled by default.
     */
    @Test
    void testUnknownEventNotCancelledByDefault() {
        PlayerUnknownEvent event = new PlayerUnknownEvent(world, island, playerUUID);
        assertFalse(event.isCancelled());
    }

    /**
     * Verifies that {@link PlayerUnknownEvent} can be cancelled and then un-cancelled.
     */
    @Test
    void testUnknownEventCancellation() {
        PlayerUnknownEvent event = new PlayerUnknownEvent(world, island, playerUUID);
        event.setCancelled(true);
        assertTrue(event.isCancelled());
        event.setCancelled(false);
        assertFalse(event.isCancelled());
    }

    // -----------------------------------------------------------------------
    // Null island (valid edge case – island may be null on some resets)
    // -----------------------------------------------------------------------

    /**
     * Verifies that all events tolerate a {@code null} island without throwing.
     */
    @Test
    void testEventsAcceptNullIsland() {
        assertNotNull(new PlayerResetEnderChestEvent(world, null, playerUUID));
        assertNotNull(new PlayerResetExpEvent(world, null, playerUUID));
        assertNotNull(new PlayerResetHealthEvent(world, null, playerUUID));
        assertNotNull(new PlayerResetHungerEvent(world, null, playerUUID));
        assertNotNull(new PlayerResetInventoryEvent(world, null, playerUUID));
        assertNotNull(new PlayerResetMoneyEvent(world, null, playerUUID));
        assertNotNull(new PlayerTamedRemovalEvent(world, null, playerUUID));
        assertNotNull(new PlayerUnknownEvent(world, null, playerUUID));
    }
}

