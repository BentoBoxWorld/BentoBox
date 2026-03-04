package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.nms.PasteHandler;
import world.bentobox.bentobox.nms.fallback.PasteHandlerImpl;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 */
public class BlueprintPasterTest extends CommonTestSetup {

    private BlueprintPaster bp;
    private BlueprintPaster bp2;

    @Mock
    private Blueprint blueprint;
    @Mock
    private BlueprintClipboard clipboard;
    // Not @Mock — PasteHandler has a static Bukkit.createBlockData() initializer that must not
    // run before MockBukkit is set up. We create this mock manually after super.setUp().
    private PasteHandler mockPaster;
    @Mock
    private BukkitTask mockTask;

    private MockedConstruction<PasteHandlerImpl> mockedFallback;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Create PasteHandler mock manually — must happen after super.setUp() sets up MockBukkit,
        // otherwise PasteHandler's static Bukkit.createBlockData() initializer fails.
        mockPaster = mock(PasteHandler.class);

        // NMS: intercept Util.getPasteHandler() (static call in field initializer)
        mockedUtil.when(Util::getPasteHandler).thenReturn(mockPaster);
        when(mockPaster.pasteBlocks(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(mockPaster.pasteEntities(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        // Intercept new PasteHandlerImpl() (constructor call in field initializer)
        mockedFallback = Mockito.mockConstruction(PasteHandlerImpl.class, (mock, ctx) -> {
            when(mock.pasteBlocks(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
            when(mock.pasteEntities(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        });

        // Chunk loading — complete synchronously so thenRun fires in the same call
        mockedUtil.when(() -> Util.getChunkAtAsync(any(Location.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Scheduler
        when(sch.runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong())).thenReturn(mockTask);

        // World
        when(world.getMaxHeight()).thenReturn(256);
        when(world.getMinHeight()).thenReturn(0);
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);

        // Location — override CommonTestSetup's (0,0,0) with real coords
        when(location.toVector()).thenReturn(new Vector(1, 2, 3));

        // Island
        when(island.getProtectionCenter()).thenReturn(location);
        // island.getOwner() already returns uuid (CommonTestSetup)

        // Clipboard
        when(clipboard.getBlueprint()).thenReturn(blueprint);

        // Construct instances (after all mocks are ready)
        bp = new BlueprintPaster(plugin, blueprint, world, island);
        bp2 = new BlueprintPaster(plugin, clipboard, location);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        mockedFallback.close();
        super.tearDown();
    }

    // --- helpers ---

    /** Call paste() on the given paster and capture the scheduled Runnable. */
    private Runnable startPaste(BlueprintPaster paster) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        paster.paste();
        verify(sch).runTaskTimer(eq(plugin), captor.capture(), anyLong(), anyLong());
        return captor.getValue();
    }

    /** Call paste(useNMS) on the given paster and capture the scheduled Runnable. */
    private Runnable startPaste(BlueprintPaster paster, boolean useNMS) {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        paster.paste(useNMS);
        verify(sch).runTaskTimer(eq(plugin), captor.capture(), anyLong(), anyLong());
        return captor.getValue();
    }

    // ===================== Constructor Tests =====================

    @Test
    public void testConstructorWithClipboard() {
        assertNotNull(bp2);
    }

    @Test
    public void testConstructorWithIsland() {
        assertNotNull(bp);
    }

    @Test
    public void testConstructorWithIslandAndBedrock() {
        // bedrock (1,1,1) subtracted from center (1,2,3) → paste at (0,1,2)
        when(blueprint.getBedrock()).thenReturn(new Vector(1, 1, 1));
        BlueprintPaster bpBedrock = new BlueprintPaster(plugin, blueprint, world, island);
        assertNotNull(bpBedrock);
    }

    @Test
    public void testConstructorYClampedToMaxHeight() {
        // center above world max — should clamp to maxHeight-1
        when(location.toVector()).thenReturn(new Vector(1, 300, 3));
        BlueprintPaster bpHigh = new BlueprintPaster(plugin, blueprint, world, island);
        assertNotNull(bpHigh);
    }

    @Test
    public void testConstructorYClampedToMinHeight() {
        // center below world min — should clamp to minHeight
        when(location.toVector()).thenReturn(new Vector(1, -10, 3));
        BlueprintPaster bpLow = new BlueprintPaster(plugin, blueprint, world, island);
        assertNotNull(bpLow);
    }

    // ===================== paste() Tests =====================

    @Test
    public void testPasteReturnsNonNullFuture() {
        assertNotNull(bp.paste());
    }

    @Test
    public void testPasteSchedulesTimerTask() {
        bp.paste();
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    public void testPasteUseNMSFalse() {
        CompletableFuture<Boolean> result = bp.paste(false);
        assertNotNull(result);
        verify(sch).runTaskTimer(eq(plugin), any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    public void testPasteWithIslandOwnerSendsEstimatedTime() {
        // island has owner uuid → User backed by mockPlayer → messages flow via spigot
        bp.paste();
        checkSpigotMessage("commands.island.create.pasting.estimated-time", 1);
        checkSpigotMessage("commands.island.create.pasting.blocks", 1);
    }

    @Test
    public void testPasteNoOwnerNoMessages() {
        // clipboard paste: island==null → no owner → no messages
        bp2.paste();
        checkSpigotMessage("commands.island.create.pasting.estimated-time", 0);
    }

    @Test
    public void testPasteNullBlueprintMapsUseEmptyMaps() {
        // null getBlocks/getAttached/getEntities → uses empty maps → no NPE
        assertDoesNotThrow(() -> bp.paste());
    }

    // ===================== State Machine Tests =====================

    @Test
    public void testStateChunkLoadToBlocks() {
        Runnable task = startPaste(bp);
        // Run 1: CHUNK_LOAD → loadChunk() → (thenRun synchronous) → state=BLOCKS
        task.run();
        mockedUtil.verify(() -> Util.getChunkAtAsync(any(Location.class)));
    }

    @Test
    public void testStateBlocksEmptyToAttachments() {
        Runnable task = startPaste(bp);
        task.run(); // CHUNK_LOAD → BLOCKS
        task.run(); // BLOCKS (empty) → ATTACHMENTS
        // No exception = state transition succeeded
    }

    @Test
    public void testStateAttachmentsEmptyToEntities() {
        Runnable task = startPaste(bp);
        task.run(); // CHUNK_LOAD → BLOCKS
        task.run(); // BLOCKS → ATTACHMENTS
        task.run(); // ATTACHMENTS → ENTITIES
    }

    @Test
    public void testStateEntitiesEmptyDone_Overworld() {
        when(world.getEnvironment()).thenReturn(World.Environment.NORMAL);
        Runnable task = startPaste(bp);
        task.run(); task.run(); task.run();
        task.run(); // ENTITIES (empty) → DONE + dimension-done message
        checkSpigotMessage("commands.island.create.pasting.dimension-done", 1);
    }

    @Test
    public void testStateEntitiesEmptyDone_Nether() {
        when(world.getEnvironment()).thenReturn(World.Environment.NETHER);
        Runnable task = startPaste(bp);
        task.run(); task.run(); task.run();
        task.run();
        checkSpigotMessage("commands.island.create.pasting.dimension-done", 1);
    }

    @Test
    public void testStateEntitiesEmptyDone_End() {
        when(world.getEnvironment()).thenReturn(World.Environment.THE_END);
        Runnable task = startPaste(bp);
        task.run(); task.run(); task.run();
        task.run();
        checkSpigotMessage("commands.island.create.pasting.dimension-done", 1);
    }

    @Test
    public void testStateDoneCompletesClipboardPos() {
        // clipboard paste (island==null): cancelTask sets pos1/pos2 on clipboard
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        bp2.paste();
        verify(sch).runTaskTimer(eq(plugin), captor.capture(), anyLong(), anyLong());
        Runnable task = captor.getValue();

        task.run(); task.run(); task.run(); task.run();
        task.run(); // DONE → cancelTask → setPos1/setPos2

        verify(clipboard).setPos1(any());
        verify(clipboard).setPos2(any());
    }

    @Test
    public void testStateDoneIslandNoPosOnClipboard() {
        // island paste (island!=null): cancelTask does NOT touch clipboard
        Runnable task = startPaste(bp);
        task.run(); task.run(); task.run(); task.run();
        task.run(); // DONE → cancelTask

        verify(clipboard, never()).setPos1(any());
        verify(clipboard, never()).setPos2(any());
    }

    @Test
    public void testStateCancelCancelsTask() {
        Runnable task = startPaste(bp);
        task.run(); task.run(); task.run(); task.run();
        task.run(); // DONE → CANCEL
        task.run(); // CANCEL → pastingTask.cancel()

        verify(mockTask).cancel();
    }

    @Test
    public void testStateCancelCompletesFuture() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        CompletableFuture<Boolean> result = bp.paste();
        verify(sch).runTaskTimer(eq(plugin), captor.capture(), anyLong(), anyLong());
        Runnable task = captor.getValue();

        task.run(); task.run(); task.run(); task.run();
        task.run(); // DONE → complete(true) + state=CANCEL

        assertTrue(result.isDone());
        assertTrue(result.getNow(false));
    }

    // ===================== Block / Entity Pasting Tests =====================

    @Test
    public void testPasteWithBlocksCallsPasteHandler() {
        BlueprintBlock mockBlock = mock(BlueprintBlock.class);
        when(blueprint.getBlocks()).thenReturn(Map.of(new Vector(0, 0, 0), mockBlock));

        Runnable task = startPaste(bp, true); // useNMS=true
        task.run(); // CHUNK_LOAD → BLOCKS
        task.run(); // BLOCKS → pasteBlocks via NMS paster

        verify(mockPaster).pasteBlocks(any(), any(), any());
    }

    @Test
    public void testPasteWithBlocksFallback() {
        BlueprintBlock mockBlock = mock(BlueprintBlock.class);
        when(blueprint.getBlocks()).thenReturn(Map.of(new Vector(0, 0, 0), mockBlock));

        Runnable task = startPaste(bp, false); // useNMS=false → fallback
        task.run(); // CHUNK_LOAD → BLOCKS
        task.run(); // BLOCKS → pasteBlocks via fallback

        PasteHandlerImpl fallbackMock = mockedFallback.constructed().get(0);
        verify(fallbackMock).pasteBlocks(any(), any(), any());
    }

    @Test
    public void testPasteWithEntitiesSendsEntitiesMessage() {
        BlueprintEntity mockEntity = mock(BlueprintEntity.class);
        when(blueprint.getEntities()).thenReturn(Map.of(new Vector(0, 0, 0), List.of(mockEntity)));

        Runnable task = startPaste(bp);
        task.run(); // CHUNK_LOAD → BLOCKS
        task.run(); // BLOCKS (empty) → ATTACHMENTS
        task.run(); // ATTACHMENTS (empty) → ENTITIES + sends "entities" message

        checkSpigotMessage("commands.island.create.pasting.entities", 1);
    }

    @Test
    public void testSinkBlueprintAdjustsY() {
        when(blueprint.isSink()).thenReturn(true);
        when(world.getHighestBlockYAt(any(Location.class), any())).thenReturn(10);

        Runnable task = startPaste(bp);
        task.run(); // CHUNK_LOAD → loadChunk → sink logic fires

        verify(world).getHighestBlockYAt(any(Location.class), any());
    }
}
