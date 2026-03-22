package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpawnProtectionListener}
 * @since 2.6.0
 */
class SpawnProtectionListenerTest extends CommonTestSetup {

    private SpawnProtectionListener listener;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // WorldSettings and World Flags
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // World
        when(world.getName()).thenReturn("bskyblock_world");

        // By default everything is in world
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        // Default: flag disabled
        Flags.SPAWN_PROTECTION.setSetting(world, false);

        // Default: player IS at spawn
        when(im.isAtSpawn(any())).thenReturn(true);

        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(world);

        listener = new SpawnProtectionListener();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Not a player - event should not be cancelled
     */
    @Test
    void testOnPlayerVoidDamageNotPlayer() {
        LivingEntity le = mock(LivingEntity.class);
        EntityDamageEvent e = new EntityDamageEvent(le, DamageCause.VOID, null, 0D);
        Flags.SPAWN_PROTECTION.setSetting(world, true);
        listener.onPlayerVoidDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Damage cause is not VOID - event should not be cancelled
     */
    @Test
    void testOnPlayerVoidDamageNotVoid() {
        EntityDamageEvent e = new EntityDamageEvent(mockPlayer, DamageCause.FALL, null, 0D);
        Flags.SPAWN_PROTECTION.setSetting(world, true);
        listener.onPlayerVoidDamage(e);
        assertFalse(e.isCancelled());
        verify(im, never()).spawnTeleport(any(), any());
    }

    /**
     * Player not in a BentoBox world - event should not be cancelled
     */
    @Test
    void testOnPlayerVoidDamageNotInWorld() {
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        Flags.SPAWN_PROTECTION.setSetting(world, true);
        EntityDamageEvent e = new EntityDamageEvent(mockPlayer, DamageCause.VOID, null, 0D);
        listener.onPlayerVoidDamage(e);
        assertFalse(e.isCancelled());
        verify(im, never()).spawnTeleport(any(), any());
    }

    /**
     * SPAWN_PROTECTION flag is disabled - event should not be cancelled
     */
    @Test
    void testOnPlayerVoidDamageFlagDisabled() {
        // Flag is false by default in setUp
        EntityDamageEvent e = new EntityDamageEvent(mockPlayer, DamageCause.VOID, null, 0D);
        listener.onPlayerVoidDamage(e);
        assertFalse(e.isCancelled());
        verify(im, never()).spawnTeleport(any(), any());
    }

    /**
     * Player is NOT at the spawn island - event should not be cancelled
     */
    @Test
    void testOnPlayerVoidDamageNotAtSpawn() {
        Flags.SPAWN_PROTECTION.setSetting(world, true);
        when(im.isAtSpawn(any())).thenReturn(false);
        EntityDamageEvent e = new EntityDamageEvent(mockPlayer, DamageCause.VOID, null, 0D);
        listener.onPlayerVoidDamage(e);
        assertFalse(e.isCancelled());
        verify(im, never()).spawnTeleport(any(), any());
    }

    /**
     * All conditions met - event should be cancelled and player teleported to spawn
     */
    @Test
    void testOnPlayerVoidDamageAtSpawn() {
        Flags.SPAWN_PROTECTION.setSetting(world, true);
        EntityDamageEvent e = new EntityDamageEvent(mockPlayer, DamageCause.VOID, null, 0D);
        listener.onPlayerVoidDamage(e);
        assertTrue(e.isCancelled());
        verify(im).spawnTeleport(world, mockPlayer);
    }
}
