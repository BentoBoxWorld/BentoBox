package world.bentobox.bentobox.hooks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.util.Pair;

/**
 * Tests the region-level clearing of Slimefun block data (#2051).
 */
class SlimefunHookTest extends CommonTestSetup {

    private MockedStatic<BlockStorage> mockedStorage;
    private BlockStorage storage;
    private Map<Location, Config> raw;
    private SlimefunHook hook;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        mockedStorage = Mockito.mockStatic(BlockStorage.class);
        storage = mock(BlockStorage.class);
        raw = new ConcurrentHashMap<>();
        when(storage.getRawStorage()).thenReturn(raw);
        mockedStorage.when(() -> BlockStorage.getStorage(world)).thenReturn(storage);
        hook = new SlimefunHook();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        mockedStorage.closeOnDemand();
        super.tearDown();
    }

    private Location slimefunBlockAt(int x, int z) {
        Location l = new Location(world, x, 64, z);
        raw.put(l, mock(Config.class));
        return l;
    }

    @Test
    void testIsInRegionsHandlesNegativeCoordinates() {
        Set<Pair<Integer, Integer>> regions = Set.of(new Pair<>(-1, 0));
        assertTrue(SlimefunHook.isInRegions(-1, 0, regions));
        assertTrue(SlimefunHook.isInRegions(-512, 511, regions));
        assertFalse(SlimefunHook.isInRegions(0, 0, regions));
        assertFalse(SlimefunHook.isInRegions(-513, 0, regions));
        assertFalse(SlimefunHook.isInRegions(-1, 512, regions));
    }

    @Test
    void testClearRegionsClearsOnlyBlocksInThePurgedRegions() {
        Location inside = slimefunBlockAt(10, 10);
        Location edge = slimefunBlockAt(511, 511);
        Location nextRegion = slimefunBlockAt(512, 10);
        Location negative = slimefunBlockAt(-1, 10);

        int cleared = hook.clearRegions(world, List.of(new Pair<>(0, 0)));

        assertEquals(2, cleared);
        mockedStorage.verify(() -> BlockStorage.clearBlockInfo(inside, true));
        mockedStorage.verify(() -> BlockStorage.clearBlockInfo(edge, true));
        mockedStorage.verify(() -> BlockStorage.clearBlockInfo(nextRegion, true), never());
        mockedStorage.verify(() -> BlockStorage.clearBlockInfo(negative, true), never());
    }

    @Test
    void testClearRegionsWorldNotKnownToSlimefun() {
        mockedStorage.when(() -> BlockStorage.getStorage(world)).thenReturn(null);

        assertEquals(0, hook.clearRegions(world, List.of(new Pair<>(0, 0))));

        mockedStorage.verify(() -> BlockStorage.clearBlockInfo(any(Location.class), anyBoolean()), never());
    }

    @Test
    void testClearRegionsNoRegions() {
        slimefunBlockAt(10, 10);
        assertEquals(0, hook.clearRegions(world, List.of()));
        mockedStorage.verify(() -> BlockStorage.clearBlockInfo(any(Location.class), anyBoolean()), never());
    }
}
