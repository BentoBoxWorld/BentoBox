package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import world.bentobox.bentobox.CommonTestSetup;

/**
 * Tests the listener
 * @author tastybento
 *
 */
public class ExperiencePickupListenerTest extends CommonTestSetup {

    private EntityTargetLivingEntityEvent e;
    private ExperiencePickupListener epl;
    private Entity entity;

    /**
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        when(island.isAllowed(any(), any())).thenReturn(true);

        // Set up as valid
        entity = mock(ExperienceOrb.class);
        when(entity.getLocation()).thenReturn(location);

        TargetReason reason = TargetReason.CLOSEST_PLAYER;
        e = new EntityTargetLivingEntityEvent(entity, mockPlayer, reason);
        epl = new ExperiencePickupListener();

    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetPlayerNotAllowed() {
        // Not allowed
        when(island.isAllowed(any(), any())).thenReturn(false);
        epl.onExperienceOrbTargetPlayer(e);
        assertNull(e.getTarget());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetPlayerAllowed() {
        epl.onExperienceOrbTargetPlayer(e);
        assertNotNull(e.getTarget());
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetNotPlayer() {
        LivingEntity zombie = mock(Zombie.class);
        e = new EntityTargetLivingEntityEvent(entity, zombie, TargetReason.CLOSEST_ENTITY);
        epl.onExperienceOrbTargetPlayer(e);
        assertNotNull(e.getTarget());
        verify(notifier, never()).notify(any(), anyString());
    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetPlayerNotOrb() {
        entity = mock(ArmorStand.class);
        epl.onExperienceOrbTargetPlayer(e);
        assertNotNull(e.getTarget());
        verify(notifier, never()).notify(any(), anyString());
    }

}
