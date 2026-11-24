package world.bentobox.bentobox.blueprints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;

/**
 * Tests {@link BlueprintPreview}.
 */
class BlueprintPreviewTest extends CommonTestSetup {

    private static final String CHOOSE = "/island create";

    private GameModeAddon addon;
    private BlueprintBundle bundle;
    private BlueprintPreview listener;
    private User user;
    private World previewWorld;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(plugin.getName()).thenReturn("BentoBox");
        addon = mock(GameModeAddon.class);

        Blueprint bp = new Blueprint();
        Map<Vector, BlueprintBlock> blocks = new HashMap<>();
        blocks.put(new Vector(0, 0, 0), new BlueprintBlock("minecraft:bedrock"));
        blocks.put(new Vector(20, 3, -2), new BlueprintBlock("minecraft:stone"));
        bp.setBlocks(blocks);
        when(bm.getBlueprints(addon)).thenReturn(Map.of("island", bp));

        bundle = new BlueprintBundle();
        bundle.setUniqueId("default");
        bundle.setDisplayName("Default");
        bundle.setBlueprint(World.Environment.NORMAL, bp.setName("island"));

        previewWorld = mock(World.class);
        when(previewWorld.getName()).thenReturn(BlueprintPreview.WORLD_NAME);
        mockedBukkit.when(() -> Bukkit.getWorld(BlueprintPreview.WORLD_NAME)).thenReturn(previewWorld);
        mockedBukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(mockPlayer);

        when(mockPlayer.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(mockPlayer.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(mockPlayer.getServer()).thenReturn(server);
        when(mockPlayer.teleportAsync(any())).thenReturn(CompletableFuture.completedFuture(true));
        when(location.getWorld()).thenReturn(world);
        when(world.getName()).thenReturn("bskyblock_world");

        user = User.getInstance(mockPlayer);
        listener = new BlueprintPreview(plugin);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        BlueprintPreview.endAll(plugin);
        super.tearDown();
    }

    private boolean show() {
        return BlueprintPreview.show(plugin, user, addon, bundle, CHOOSE, CHOOSE);
    }

    // ---- who may preview ----

    @Test
    void notShownWhenSwitchedOff() {
        plugin.getSettings().setBlueprintPreview(false);
        assertFalse(show());
        verify(mockPlayer, never()).teleportAsync(any());
    }

    @Test
    void notShownForABundleThatIsNotAllowed() {
        bundle.setPreviewAllowed(false);
        assertFalse(show());
        assertFalse(BlueprintPreview.isPreviewing(uuid));
    }

    @Test
    void notShownWhenTheBundleHasNoBlueprint() {
        when(bm.getBlueprints(addon)).thenReturn(Map.of());
        assertFalse(show());
        checkSpigotMessage("commands.island.create.preview.nothing");
    }

    // ---- the preview itself ----

    @Test
    void showTakesThePlayerToThePreviewWorldFlying() {
        assertTrue(show());
        assertTrue(BlueprintPreview.isPreviewing(uuid));
        ArgumentCaptor<Location> to = ArgumentCaptor.forClass(Location.class);
        verify(mockPlayer).teleportAsync(to.capture());
        assertEquals(previewWorld, to.getValue().getWorld());
        verify(mockPlayer).setGameMode(GameMode.ADVENTURE);
        verify(mockPlayer).setAllowFlight(true);
        verify(mockPlayer).setFlying(true);
        verify(mockPlayer).setInvulnerable(true);
        verify(mockPlayer).sendPotionEffectChange(eq(mockPlayer), any(PotionEffect.class));
        checkSpigotMessage("commands.island.create.preview.showing");
    }

    @Test
    void endPutsThePlayerBackAsTheyWere() {
        when(mockPlayer.getAllowFlight()).thenReturn(true);
        when(mockPlayer.isFlying()).thenReturn(true);
        show();
        BlueprintPreview.end(plugin, mockPlayer, true);
        assertFalse(BlueprintPreview.isPreviewing(uuid));
        verify(mockPlayer).teleport(location);
        verify(mockPlayer).setGameMode(GameMode.SURVIVAL);
        verify(mockPlayer, atLeastOnce()).setAllowFlight(true);
        verify(mockPlayer).setInvulnerable(false);
        verify(mockPlayer).sendPotionEffectChangeRemove(mockPlayer, PotionEffectType.NIGHT_VISION);
    }

    @Test
    void timeLimitEndsThePreview() {
        show();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTaskLater(eq(plugin), task.capture(), eq(60 * 20L));
        task.getValue().run();
        assertFalse(BlueprintPreview.isPreviewing(uuid));
        checkSpigotMessage("commands.island.create.preview.ended");
    }

    @Test
    void blocksAreSentAgainShortlyAfterArriving() {
        show();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(sch).runTaskLater(eq(plugin), task.capture(), eq(20L));
        task.getValue().run();
        // The two blocks are in different chunks
        verify(mockPlayer, org.mockito.Mockito.times(2)).sendMultiBlockChange(anyMap());
    }

    // ---- listeners ----

    @Test
    void aCommandEndsThePreviewFirst() {
        show();
        listener.onCommand(new PlayerCommandPreprocessEvent(mockPlayer, CHOOSE + " default"));
        assertFalse(BlueprintPreview.isPreviewing(uuid));
        verify(mockPlayer).teleport(location);
    }

    @Test
    void quittingEndsThePreview() {
        show();
        listener.onQuit(new PlayerQuitEvent(mockPlayer, net.kyori.adventure.text.Component.empty(),
                PlayerQuitEvent.QuitReason.DISCONNECTED));
        assertFalse(BlueprintPreview.isPreviewing(uuid));
        verify(mockPlayer).setGameMode(GameMode.SURVIVAL);
    }

    @Test
    void movingOutsideIsPulledBack() {
        show();
        Location from = new Location(previewWorld, 0, 100, 0);
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, from, new Location(previewWorld, 500, 100, 0));
        listener.onMove(e);
        assertTrue(e.getTo().getX() < 100);
    }

    @Test
    void movingInsideIsLeftAlone() {
        show();
        Location to = new Location(previewWorld, 5, 103, 0);
        PlayerMoveEvent e = new PlayerMoveEvent(mockPlayer, new Location(previewWorld, 0, 100, 0), to);
        listener.onMove(e);
        assertEquals(to, e.getTo());
    }

    @Test
    void enderPearlsAreBlocked() {
        show();
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, new Location(previewWorld, 0, 100, 0),
                new Location(previewWorld, 5, 100, 0), TeleportCause.ENDER_PEARL);
        listener.onTeleport(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void beingTakenToAnotherWorldEndsThePreviewWithoutMovingThePlayer() {
        show();
        PlayerTeleportEvent e = new PlayerTeleportEvent(mockPlayer, new Location(previewWorld, 0, 100, 0),
                new Location(world, 0, 100, 0), TeleportCause.PLUGIN);
        listener.onTeleport(e);
        assertFalse(e.isCancelled());
        assertFalse(BlueprintPreview.isPreviewing(uuid));
        verify(mockPlayer, never()).teleport(any(Location.class));
        verify(mockPlayer).setGameMode(GameMode.SURVIVAL);
    }

    @Test
    void flightCannotBeSwitchedOff() {
        show();
        PlayerToggleFlightEvent e = new PlayerToggleFlightEvent(mockPlayer, false);
        listener.onToggleFlight(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void itemsCannotBeDropped() {
        show();
        PlayerDropItemEvent e = new PlayerDropItemEvent(mockPlayer, mock(org.bukkit.entity.Item.class));
        listener.onDrop(e);
        assertTrue(e.isCancelled());
    }

    @Test
    void listenersLeaveOtherPlayersAlone() {
        PlayerToggleFlightEvent e = new PlayerToggleFlightEvent(mockPlayer, false);
        listener.onToggleFlight(e);
        assertFalse(e.isCancelled());
        listener.onCommand(new PlayerCommandPreprocessEvent(mockPlayer, "/spawn"));
        verify(mockPlayer, never()).teleport(any(Location.class));
        verify(sch, never()).runTaskLater(any(), any(Runnable.class), anyLong());
    }

    // ---- helpers ----

    @Test
    void boundsHoldEveryBlock() {
        BoundingBox box = BlueprintPreview.bounds(List.of(new Vector(-2, 0, 3), new Vector(4, 5, -1)));
        assertEquals(new BoundingBox(-2, 0, -1, 5, 6, 4), box);
    }

    @Test
    void clampLeavesAPlayerInsideAlone() {
        assertNull(BlueprintPreview.clamp(new Location(null, 5, 5, 5), new BoundingBox(0, 0, 0, 10, 10, 10)));
    }

    @Test
    void clampPullsAPlayerBackInside() {
        Location inside = BlueprintPreview.clamp(new Location(null, 12, -3, 5, 90F, 10F),
                new BoundingBox(0, 0, 0, 10, 10, 10));
        assertEquals(new Location(null, 9.5, 0.5, 5, 90F, 10F), inside);
    }
}
