package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class HurtingListenerTest extends CommonTestSetup {

    @Mock
    private Enderman enderman;
    @Mock
    private FishHook hookEntity;

    private User user;

    /**
     */
    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Monsters and animals
        when(enderman.getLocation()).thenReturn(location);
        when(enderman.getWorld()).thenReturn(world);
        when(enderman.getType()).thenReturn(EntityType.ENDERMAN);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);

        // Utils
        when(Util.isPassiveEntity(any())).thenCallRealMethod();
        when(Util.isHostileEntity(any())).thenCallRealMethod();
        when(Util.isTamableEntity(any())).thenCallRealMethod();

        // User & player
        user = User.getInstance(mockPlayer);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link HurtingListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageMonsteronMonster() {
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(enderman, enderman, null, null, 0);
        HurtingListener hl = new HurtingListener();
        hl.onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePlayeronMonster() {
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(mockPlayer, enderman, null, null, 0);
        HurtingListener hl = new HurtingListener();
        hl.onEntityDamage(e);
        assertTrue(e.isCancelled());

        verify(notifier).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePlayeronMonsterOp() {
        when(mockPlayer.isOp()).thenReturn(true);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(mockPlayer, enderman, null, null, 0);
        HurtingListener hl = new HurtingListener();
        hl.onEntityDamage(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowArmorStandCatching() {
        ArmorStand entity = mock(ArmorStand.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        verify(notifier, Mockito.atLeastOnce()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowArmorStandCatching() {
        ArmorStand entity = mock(ArmorStand.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(any(), any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        verify(notifier, never()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowAnimalCatching() {
        Animals entity = mock(Animals.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        verify(notifier).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowAnimalsCatching() {
        Animals entity = mock(Animals.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(any(), any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        verify(notifier, never()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowMonsterCatching() {
        Monster entity = mock(Monster.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        verify(notifier, Mockito.atLeastOnce()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowMonsterCatching() {
        Monster entity = mock(Monster.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(any(), any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        verify(notifier, never()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowVillagerCatching() {
        Villager entity = mock(Villager.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.VILLAGER);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        verify(notifier).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowWanderingTraderCatching() {
        WanderingTrader entity = mock(WanderingTrader.class);
        when(entity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        verify(notifier).notify(user, "protection.protected");
    }


    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowVillagerCatching() {
        Villager entity = mock(Villager.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.VILLAGER);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(any(), any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        verify(notifier, never()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowWanderingTraderCatching() {
        WanderingTrader entity = mock(WanderingTrader.class);
        when(entity.getLocation()).thenReturn(location);
        when(entity.getType()).thenReturn(EntityType.WANDERING_TRADER);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(mockPlayer, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(any(), any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        verify(notifier, never()).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Disabled("Not yet implemented")
    @Test
    public void testOnPlayerFeedParrots() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Disabled("Not yet implemented")
    @Test
    public void testOnSplashPotionSplash() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent)}.
     */
    @Disabled("Not yet implemented")
    @Test
    public void testOnLingeringPotionSplash() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Disabled("Not yet implemented")
    @Test
    public void testOnLingeringPotionDamage() {
        //fail("Not yet implemented"); // TODO
    }

}
