package world.bentobox.bentobox.listeners;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.util.Util;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class, BentoBox.class, User.class, Util.class })
public class BlockEndDragonTest {

    @Test
    public void testBlockEndDragon() {
        BentoBox plugin = mock(BentoBox.class);
        assertNotNull(new BlockEndDragon(plugin));
    }

    @Test
    public void testOnDragonSpawnWrongEntityOkayToSpawn() {
        LivingEntity le = mock(LivingEntity.class);
        when(le.getType()).thenReturn(EntityType.AREA_EFFECT_CLOUD);
        CreatureSpawnEvent event = new CreatureSpawnEvent(le, null);
        BentoBox plugin = mock(BentoBox.class);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        // allow dragon spawn
        when(iwm.isDragonSpawn(Mockito.any())).thenReturn(true);
        BlockEndDragon bed = new BlockEndDragon(plugin);
        assertTrue(bed.onDragonSpawn(event));
        assertFalse(event.isCancelled());
    }
    
    @Test
    public void testOnDragonSpawnWrongEntityNoDragonSpawn() {
        LivingEntity le = mock(LivingEntity.class);
        when(le.getType()).thenReturn(EntityType.AREA_EFFECT_CLOUD);
        CreatureSpawnEvent event = new CreatureSpawnEvent(le, null);
        BentoBox plugin = mock(BentoBox.class);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        // allow dragon spawn
        when(iwm.isDragonSpawn(Mockito.any())).thenReturn(false);
        BlockEndDragon bed = new BlockEndDragon(plugin);
        assertTrue(bed.onDragonSpawn(event));
        assertFalse(event.isCancelled());
    }
    
    @Test
    public void testOnDragonSpawnRightEntityOkayToSpawn() {
        LivingEntity le = mock(LivingEntity.class);
        when(le.getType()).thenReturn(EntityType.ENDER_DRAGON);
        CreatureSpawnEvent event = new CreatureSpawnEvent(le, null);
        BentoBox plugin = mock(BentoBox.class);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        // allow dragon spawn
        when(iwm.isDragonSpawn(Mockito.any())).thenReturn(true);
        BlockEndDragon bed = new BlockEndDragon(plugin);
        assertTrue(bed.onDragonSpawn(event));
        assertFalse(event.isCancelled());
    }
    
    @Test
    public void testOnDragonSpawnRightEntityNotOkayToSpawn() {
        LivingEntity le = mock(LivingEntity.class);
        when(le.getType()).thenReturn(EntityType.ENDER_DRAGON);
        CreatureSpawnEvent event = new CreatureSpawnEvent(le, null);
        BentoBox plugin = mock(BentoBox.class);
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(plugin.getIWM()).thenReturn(iwm);
        // allow dragon spawn
        when(iwm.isDragonSpawn(Mockito.any())).thenReturn(false);
        BlockEndDragon bed = new BlockEndDragon(plugin);
        assertFalse(bed.onDragonSpawn(event));
        Mockito.verify(le).remove();
        Mockito.verify(le).setHealth(0);
        assertTrue(event.isCancelled());
    }

}
