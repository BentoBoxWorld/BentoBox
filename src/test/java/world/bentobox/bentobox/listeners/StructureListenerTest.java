package world.bentobox.bentobox.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.generator.structure.Structure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.papermc.paper.event.world.StructuresLocateEvent;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;

/**
 * Tests for {@link StructureListener}.
 */
@ExtendWith(MockitoExtension.class)
class StructureListenerTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private Settings settings;
    @Mock
    private GameModeAddon gameMode;
    @Mock
    private WorldSettings worldSettings;
    @Mock
    private World world;

    private StructureListener listener;

    @BeforeEach
    public void setUp() {
        MockBukkit.mock();
        lenient().when(plugin.getSettings()).thenReturn(settings);
        // Global default disables ancient_city and trial_chambers everywhere.
        lenient().when(settings.getDisabledStructures())
                .thenReturn(List.of("ancient_city", "trial-chambers"));
        lenient().when(gameMode.getWorldSettings()).thenReturn(worldSettings);
        lenient().when(worldSettings.getWorldName()).thenReturn("bskyblock_world");
        // Per-world override: force-enable ancient_city, and disable mineshaft (not in global).
        lenient().when(worldSettings.getStructureSettings())
                .thenReturn(Map.of("ancient_city", true, "mineshaft", false));
        lenient().when(world.getName()).thenReturn("bskyblock_world");
        listener = new StructureListener(plugin, gameMode);
    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    private AsyncStructureSpawnEvent spawnEvent(String structureKey) {
        Structure structure = mock(Structure.class);
        lenient().when(structure.getKey()).thenReturn(NamespacedKey.minecraft(structureKey));
        AsyncStructureSpawnEvent e = mock(AsyncStructureSpawnEvent.class);
        lenient().when(e.getWorld()).thenReturn(world);
        lenient().when(e.getStructure()).thenReturn(structure);
        return e;
    }

    private StructuresLocateEvent locateEvent(String... structureKeys) {
        List<Structure> structures = Arrays.stream(structureKeys).map(key -> {
            Structure structure = mock(Structure.class);
            lenient().when(structure.getKey()).thenReturn(NamespacedKey.minecraft(key));
            return structure;
        }).map(Structure.class::cast).toList();
        StructuresLocateEvent e = mock(StructuresLocateEvent.class);
        lenient().when(e.getWorld()).thenReturn(world);
        lenient().when(e.getStructures()).thenReturn(structures);
        lenient().when(e.getOrigin()).thenReturn(mock(Location.class));
        return e;
    }

    /**
     * Gives the test world a custom generator whose {@code shouldGenerateStructures}
     * reports {@code generates}.
     */
    private void withGenerator(boolean generates) {
        ChunkGenerator generator = mock(ChunkGenerator.class);
        lenient().when(generator.shouldGenerateStructures(any(WorldInfo.class), any(Random.class), anyInt(), anyInt()))
                .thenReturn(generates);
        lenient().when(world.getGenerator()).thenReturn(generator);
    }

    // --- Spawn suppression -------------------------------------------------

    @Test
    void testGlobalDisabledStructureIsCancelled() {
        // trial_chambers is disabled by the global list (note hyphen/underscore normalisation).
        AsyncStructureSpawnEvent e = spawnEvent("trial_chambers");
        listener.onStructureSpawn(e);
        verify(e).setCancelled(true);
    }

    @Test
    void testPerWorldOverrideDisablesStructureNotInGlobalList() {
        AsyncStructureSpawnEvent e = spawnEvent("mineshaft");
        listener.onStructureSpawn(e);
        verify(e).setCancelled(true);
    }

    @Test
    void testPerWorldOverrideForceEnablesGloballyDisabledStructure() {
        // Global disables ancient_city, but this world's override sets it true — override wins.
        AsyncStructureSpawnEvent e = spawnEvent("ancient_city");
        listener.onStructureSpawn(e);
        verify(e, never()).setCancelled(true);
    }

    @Test
    void testUnlistedStructureGeneratesNormally() {
        AsyncStructureSpawnEvent e = spawnEvent("village_plains");
        listener.onStructureSpawn(e);
        verify(e, never()).setCancelled(true);
    }

    @Test
    void testSpawnInNetherWorldIsMatchedByName() {
        when(world.getName()).thenReturn("bskyblock_world_nether");
        AsyncStructureSpawnEvent e = spawnEvent("trial_chambers");
        listener.onStructureSpawn(e);
        verify(e).setCancelled(true);
    }

    @Test
    void testSpawnOutsideGameModeWorldIsIgnored() {
        when(world.getName()).thenReturn("some_other_world");
        AsyncStructureSpawnEvent e = spawnEvent("trial_chambers");
        listener.onStructureSpawn(e);
        verify(e, never()).setCancelled(true);
    }

    // --- Locate suppression ------------------------------------------------

    @Test
    void testLocateAllDisabledIsCancelled() {
        StructuresLocateEvent e = locateEvent("trial_chambers");
        listener.onStructuresLocate(e);
        verify(e).setCancelled(true);
        verify(e, never()).setStructures(anyList());
    }

    @Test
    void testLocateMixedNarrowsToEnabledOnly() {
        // trial_chambers disabled (global), mineshaft disabled (override) — only ancient_city
        // (override-enabled) survives.
        StructuresLocateEvent e = locateEvent("trial_chambers", "mineshaft", "ancient_city");
        listener.onStructuresLocate(e);
        verify(e, never()).setCancelled(true);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Structure>> captor = ArgumentCaptor.forClass(List.class);
        verify(e).setStructures(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("ancient_city", captor.getValue().get(0).getKey().getKey());
    }

    @Test
    void testLocateAllEnabledIsUntouched() {
        StructuresLocateEvent e = locateEvent("village_plains", "ancient_city");
        listener.onStructuresLocate(e);
        verify(e, never()).setCancelled(true);
        verify(e, never()).setStructures(anyList());
    }

    @Test
    void testLocateOutsideGameModeWorldIsIgnored() {
        when(world.getName()).thenReturn("some_other_world");
        StructuresLocateEvent e = locateEvent("trial_chambers");
        listener.onStructuresLocate(e);
        verify(e, never()).setCancelled(true);
        verify(e, never()).setStructures(anyList());
    }

    // --- Structureless-world locate suppression ----------------------------

    @Test
    void testLocateInStructurelessWorldIsCancelledWithoutConfig() {
        // Generator never places structures — even structures on no disabled list
        // (e.g. a cartographer map's monument search) must not be scanned for.
        withGenerator(false);
        StructuresLocateEvent e = locateEvent("monument", "mansion");
        listener.onStructuresLocate(e);
        verify(e).setCancelled(true);
        verify(e, never()).setStructures(anyList());
    }

    @Test
    void testLocateInStructurelessWorldKeepsForceEnabledStructure() {
        // ancient_city is force-enabled by the per-world override, so it survives
        // even though the world generates no structures.
        withGenerator(false);
        StructuresLocateEvent e = locateEvent("monument", "ancient_city");
        listener.onStructuresLocate(e);
        verify(e, never()).setCancelled(true);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Structure>> captor = ArgumentCaptor.forClass(List.class);
        verify(e).setStructures(captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals("ancient_city", captor.getValue().get(0).getKey().getKey());
    }

    @Test
    void testLocateWithStructureGeneratingGeneratorIsUntouched() {
        // Generator places structures (e.g. SkyGrid, Boxed with allow-structures) —
        // unlisted structures search normally.
        withGenerator(true);
        StructuresLocateEvent e = locateEvent("monument", "village_plains");
        listener.onStructuresLocate(e);
        verify(e, never()).setCancelled(true);
        verify(e, never()).setStructures(anyList());
    }

    @Test
    void testStructurelessWorldDoesNotAffectSpawnSuppression() {
        // The generator layer applies to searches only; spawn events for enabled
        // structures pass through (they cannot fire in a structureless world anyway).
        withGenerator(false);
        AsyncStructureSpawnEvent e = spawnEvent("village_plains");
        listener.onStructureSpawn(e);
        verify(e, never()).setCancelled(true);
    }
}
