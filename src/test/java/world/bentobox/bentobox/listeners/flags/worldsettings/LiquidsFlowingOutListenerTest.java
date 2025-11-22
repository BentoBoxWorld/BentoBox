package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockFromToEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;

/**
 * Tests {@link world.bentobox.bentobox.listeners.flags.worldsettings.LiquidsFlowingOutListener}.
 * @author Poslovitch, tastybento
 * @since 1.3.0
 */
public class LiquidsFlowingOutListenerTest extends CommonTestSetup {

    /* Blocks */
    private Block from;
    private Block to;

    /* Event */
    private BlockFromToEvent event;


    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        /* Blocks */
        from = mock(Block.class);
        when(from.isLiquid()).thenReturn(true);
        to = mock(Block.class);

        /* World */
        when(from.getWorld()).thenReturn(world);

        // Give them locations
        Location fromLocation = new Location(world, 0, 0, 0);
        when(from.getLocation()).thenReturn(fromLocation);

        Location toLocation = new Location(world, 1, 0, 0);
        when(to.getLocation()).thenReturn(toLocation);

        /* Event */
        event = new BlockFromToEvent(from, to);

        // By default everything is in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        /* Flags */
        // By default, it is not allowed
        Flags.LIQUIDS_FLOWING_OUT.setSetting(world, false);

        /* Islands */
        // By default, there should be no island's protection range at toLocation.
        when(im.getProtectedIslandAt(toLocation)).thenReturn(Optional.empty());
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Asserts that the event is never cancelled when the 'from' block is not in the world.
     */
    @Test
    public void testFromIsNotInWorld() {
        // Not in world
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);

        // Run
        new LiquidsFlowingOutListener().onLiquidFlow(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Asserts that the event is never cancelled when {@link Flags#LIQUIDS_FLOWING_OUT} is allowed.
     */
    @Test
    public void testFlagIsAllowed() {
        // Allowed
        Flags.LIQUIDS_FLOWING_OUT.setSetting(world, true);

        // Run
        new LiquidsFlowingOutListener().onLiquidFlow(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Asserts that the event is never cancelled when the liquid flows vertically.
     */
    @Test
    public void testLiquidFlowsVertically() {
        // "To" is at (1,0,0)
        // Set "from" at (1,1,0) so that the vector's y coordinate != 0, which means the liquid flows vertically.
        when(from.getLocation()).thenReturn(new Location(world, 1, 1, 0));

        // Run
        new LiquidsFlowingOutListener().onLiquidFlow(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Asserts that the event is never cancelled when the liquid flows to a location in an island's protection range.
     */
    @Test
    public void testLiquidFlowsToLocationInIslandProtectionRange() {
        // There's a protected island at the "to"
        when(im.getProtectedIslandAt(to.getLocation())).thenReturn(Optional.of(island));

        // Run
        new LiquidsFlowingOutListener().onLiquidFlow(event);
        assertFalse(event.isCancelled());
    }

    /**
     * Asserts that the event is cancelled when liquid flows from one island's protection range into different island's range,
     * e.g., when islands abut.
     * Test for {@link LiquidsFlowingOutListener#onLiquidFlow(BlockFromToEvent)}
     */
    @Test
    public void testLiquidFlowsToAdjacentIsland() {
        // There's a protected island at the "to"
        when(im.getProtectedIslandAt(eq(to.getLocation()))).thenReturn(Optional.of(island));
        // There is another island at the "from"
        Island fromIsland = mock(Island.class);
        when(im.getProtectedIslandAt(eq(from.getLocation()))).thenReturn(Optional.of(fromIsland));
        // Run
        new LiquidsFlowingOutListener().onLiquidFlow(event);
        assertTrue(event.isCancelled());
    }

    /**
     * Asserts that the event is cancelled with the default configuration provided in {@link LiquidsFlowingOutListenerTest#setUp()}.
     */
    @Test
    public void testLiquidFlowIsBlocked() {
        // Run
        new LiquidsFlowingOutListener().onLiquidFlow(event);
        assertTrue(event.isCancelled());
    }
}
