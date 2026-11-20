package world.bentobox.bentobox.hooks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.mobs.MobExecutor;
import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord;

/**
 * Tests for {@link MythicMobsHook} spawning.
 */
class MythicMobsHookTest extends CommonTestSetup {

    @Mock
    private Plugin mythicPlugin;
    @Mock
    private MythicBukkit mythic;
    @Mock
    private MobExecutor mobManager;
    @Mock
    private MythicMob mob;
    @Mock
    private ActiveMob activeMob;
    @Mock
    private AbstractEntity abstractEntity;
    @Mock
    private Entity bukkitEntity;

    private MockedStatic<MythicBukkit> mockedMythic;
    private MythicMobsHook hook;
    private Location spawn;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        when(mythicPlugin.isEnabled()).thenReturn(true);
        when(pim.getPlugin("MythicMobs")).thenReturn(mythicPlugin);
        mockedMythic = mockStatic(MythicBukkit.class);
        mockedMythic.when(MythicBukkit::inst).thenReturn(mythic);
        when(mythic.getMobManager()).thenReturn(mobManager);
        when(mobManager.getMythicMob(anyString())).thenReturn(Optional.empty());
        when(mobManager.getMythicMob("SkeletonKing")).thenReturn(Optional.of(mob));
        when(mob.spawn(any(), anyDouble())).thenReturn(activeMob);
        when(activeMob.getEntity()).thenReturn(abstractEntity);
        when(abstractEntity.getBukkitEntity()).thenReturn(bukkitEntity);
        spawn = new Location(world, 0, 64, 0);
        hook = new MythicMobsHook();
    }

    @AfterEach
    @Override
    public void tearDown() throws Exception {
        mockedMythic.close();
        super.tearDown();
    }

    @Test
    void testSpawnSetsDisplayName() {
        AtomicReference<Entity> spawned = new AtomicReference<>();
        assertTrue(hook.spawnMythicMob(new MythicMobRecord("SkeletonKing", "The King", 3, 1F, "angry"), spawn,
                spawned::set, 0L));
        verify(mob).spawn(any(), org.mockito.ArgumentMatchers.eq(3D));
        verify(activeMob).setDisplayName("The King");
        verify(activeMob).setPower(1F);
        verify(activeMob).setStance("angry");
        assertSame(bukkitEntity, spawned.get());
    }

    @Test
    void testSpawnBlankNameKeepsConfiguredName() {
        assertTrue(hook.spawnMythicMob(new MythicMobRecord("SkeletonKing", "", 1, 0F, ""), spawn, null, 0L));
        assertTrue(hook.spawnMythicMob(new MythicMobRecord("SkeletonKing", "  ", 1, 0F, ""), spawn, null, 0L));
        assertTrue(hook.spawnMythicMob(new MythicMobRecord("SkeletonKing", null, 1, 0F, ""), spawn, null, 0L));
        verify(activeMob, never()).setDisplayName(any());
    }

    @Test
    void testSpawnUnknownMob() {
        assertFalse(hook.spawnMythicMob(new MythicMobRecord("Nobody", "x", 1, 0F, ""), spawn, null, 0L));
        verify(mob, never()).spawn(any(), anyDouble());
    }

    @Test
    void testSpawnPluginDisabled() {
        when(mythicPlugin.isEnabled()).thenReturn(false);
        assertFalse(hook.spawnMythicMob(new MythicMobRecord("SkeletonKing", "x", 1, 0F, ""), spawn, null, 0L));
        verify(mob, never()).spawn(any(), anyDouble());
    }
}
