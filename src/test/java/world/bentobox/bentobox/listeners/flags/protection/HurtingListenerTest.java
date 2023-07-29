package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bukkit.Bukkit;
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
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.listeners.flags.AbstractCommonSetup;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class HurtingListenerTest extends AbstractCommonSetup {

    @Mock
    private Enderman enderman;
    @Mock
    private FishHook hookEntity;

    private User user;

    /**
     */
    @Override
    @Before
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

        // User & player
        user = User.getInstance(player);
    }

    /**
     * Test method for {@link HurtingListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamageMonsteronMonster() {
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(enderman, enderman, null, 0);
        HurtingListener hl = new HurtingListener();
        hl.onEntityDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamagePlayeronMonster() {
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(player, enderman, null, 0);
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
        when(player.isOp()).thenReturn(true);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(player, enderman, null, 0);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        verify(notifier).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowArmorStandCatching() {
        ArmorStand entity = mock(ArmorStand.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        verify(notifier).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowMonsterCatching() {
        Monster entity = mock(Monster.class);
        when(entity.getLocation()).thenReturn(location);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
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
    @Ignore("Not yet implemented")
    @Test
    public void testOnPlayerFeedParrots() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Ignore("Not yet implemented")
    @Test
    public void testOnSplashPotionSplash() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent)}.
     */
    @Ignore("Not yet implemented")
    @Test
    public void testOnLingeringPotionSplash() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Ignore("Not yet implemented")
    @Test
    public void testOnLingeringPotionDamage() {
        //fail("Not yet implemented"); // TODO
    }

}
