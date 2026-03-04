package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

        // Server – needed by onLingeringPotionSplash which calls getPlugin().getServer().getScheduler()
        when(plugin.getServer()).thenReturn(server);
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

    // -----------------------------------------------------------------------
    // onPlayerFeedParrots
    // -----------------------------------------------------------------------

    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(PlayerInteractEntityEvent)}.
     * Non-parrot entity → no flag check.
     */
    @Test
    public void testOnPlayerFeedParrotsNotParrot() {
        Villager villager = mock(Villager.class);
        when(villager.getLocation()).thenReturn(location);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, villager, EquipmentSlot.HAND);
        new HurtingListener().onPlayerFeedParrots(e);
        verify(notifier, never()).notify(any(), any());
    }

    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(PlayerInteractEntityEvent)}.
     * Tamed parrot + COOKIE in main hand, island disallows → HURT_TAMED_ANIMALS blocked.
     */
    @Test
    public void testOnPlayerFeedParrotsTamedCookieMainHandDisallowed() {
        Parrot parrot = mock(Parrot.class);
        when(parrot.getLocation()).thenReturn(location);
        when(parrot.isTamed()).thenReturn(true);
        ItemStack cookie = mock(ItemStack.class);
        when(cookie.getType()).thenReturn(Material.COOKIE);
        when(inv.getItemInMainHand()).thenReturn(cookie);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, parrot, EquipmentSlot.HAND);
        new HurtingListener().onPlayerFeedParrots(e);
        verify(notifier).notify(user, "protection.protected");
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(PlayerInteractEntityEvent)}.
     * Tamed parrot + COOKIE in main hand, island allows → no notification.
     */
    @Test
    public void testOnPlayerFeedParrotsTamedCookieMainHandAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        Parrot parrot = mock(Parrot.class);
        when(parrot.getLocation()).thenReturn(location);
        when(parrot.isTamed()).thenReturn(true);
        ItemStack cookie = mock(ItemStack.class);
        when(cookie.getType()).thenReturn(Material.COOKIE);
        when(inv.getItemInMainHand()).thenReturn(cookie);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, parrot, EquipmentSlot.HAND);
        new HurtingListener().onPlayerFeedParrots(e);
        verify(notifier, never()).notify(any(), any());
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(PlayerInteractEntityEvent)}.
     * Untamed parrot + COOKIE in main hand → HURT_ANIMALS blocked.
     */
    @Test
    public void testOnPlayerFeedParrotsUntamedCookieMainHandDisallowed() {
        Parrot parrot = mock(Parrot.class);
        when(parrot.getLocation()).thenReturn(location);
        when(parrot.isTamed()).thenReturn(false);
        ItemStack cookie = mock(ItemStack.class);
        when(cookie.getType()).thenReturn(Material.COOKIE);
        when(inv.getItemInMainHand()).thenReturn(cookie);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, parrot, EquipmentSlot.HAND);
        new HurtingListener().onPlayerFeedParrots(e);
        verify(notifier).notify(user, "protection.protected");
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(PlayerInteractEntityEvent)}.
     * Tamed parrot + COOKIE in off hand → HURT_TAMED_ANIMALS blocked.
     */
    @Test
    public void testOnPlayerFeedParrotsTamedCookieOffHandDisallowed() {
        Parrot parrot = mock(Parrot.class);
        when(parrot.getLocation()).thenReturn(location);
        when(parrot.isTamed()).thenReturn(true);
        ItemStack cookie = mock(ItemStack.class);
        when(cookie.getType()).thenReturn(Material.COOKIE);
        when(inv.getItemInOffHand()).thenReturn(cookie);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, parrot, EquipmentSlot.OFF_HAND);
        new HurtingListener().onPlayerFeedParrots(e);
        verify(notifier).notify(user, "protection.protected");
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(PlayerInteractEntityEvent)}.
     * Parrot + non-COOKIE item in main hand → no flag check.
     */
    @Test
    public void testOnPlayerFeedParrotsNonCookieItem() {
        Parrot parrot = mock(Parrot.class);
        when(parrot.getLocation()).thenReturn(location);
        ItemStack stick = mock(ItemStack.class);
        when(stick.getType()).thenReturn(Material.STICK);
        when(inv.getItemInMainHand()).thenReturn(stick);
        PlayerInteractEntityEvent e = new PlayerInteractEntityEvent(mockPlayer, parrot, EquipmentSlot.HAND);
        new HurtingListener().onPlayerFeedParrots(e);
        verify(notifier, never()).notify(any(), any());
    }

    // -----------------------------------------------------------------------
    // onSplashPotionSplash
    // -----------------------------------------------------------------------

    /** Helper: build a ThrownPotion mock with one PotionEffect. */
    private ThrownPotion mockThrownPotion(PotionEffectType effectType) {
        ThrownPotion tp = mock(ThrownPotion.class);
        PotionEffect effect = mock(PotionEffect.class);
        when(effect.getType()).thenReturn(effectType);
        Collection<PotionEffect> effects = Collections.singletonList(effect);
        when(tp.getEffects()).thenReturn(effects);
        return tp;
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(PotionSplashEvent)}.
     * Shooter is a non-player entity → nothing happens.
     */
    @Test
    public void testOnSplashPotionSplashNonPlayerShooter() {
        ThrownPotion tp = mock(ThrownPotion.class);
        Monster shooter = mock(Monster.class);
        when(tp.getShooter()).thenReturn(shooter);
        Map<LivingEntity, Double> map = new HashMap<>();
        Monster target = mock(Monster.class);
        when(target.getLocation()).thenReturn(location);
        map.put(target, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new HurtingListener().onSplashPotionSplash(e);
        verify(notifier, never()).notify(any(), any());
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(PotionSplashEvent)}.
     * Attacker is in the affected list (self-damage) → skipped.
     */
    @Test
    public void testOnSplashPotionSplashSelfDamage() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(mockPlayer, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new HurtingListener().onSplashPotionSplash(e);
        verify(notifier, never()).notify(any(), any());
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(PotionSplashEvent)}.
     * Monster in splash radius, island disallows → HURT_MONSTERS blocked, effects removed.
     */
    @Test
    public void testOnSplashPotionSplashMonsterDisallowed() {
        PotionEffectType effectType = mock(PotionEffectType.class);
        ThrownPotion tp = mockThrownPotion(effectType);
        when(tp.getShooter()).thenReturn(mockPlayer);
        Monster monster = mock(Monster.class);
        when(monster.getLocation()).thenReturn(location);
        when(monster.getType()).thenReturn(EntityType.ZOMBIE); // prevent isPassiveEntity null-type fallback
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(monster, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new HurtingListener().onSplashPotionSplash(e);
        verify(notifier).notify(user, "protection.protected");
        verify(monster).removePotionEffect(effectType);
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(PotionSplashEvent)}.
     * Monster in splash radius, island allows → no notification, no effect removal.
     */
    @Test
    public void testOnSplashPotionSplashMonsterAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        PotionEffectType effectType = mock(PotionEffectType.class);
        ThrownPotion tp = mockThrownPotion(effectType);
        when(tp.getShooter()).thenReturn(mockPlayer);
        Monster monster = mock(Monster.class);
        when(monster.getLocation()).thenReturn(location);
        when(monster.getType()).thenReturn(EntityType.ZOMBIE); // prevent isPassiveEntity null-type fallback
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(monster, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new HurtingListener().onSplashPotionSplash(e);
        verify(notifier, never()).notify(any(), any());
        verify(monster, never()).removePotionEffect(any());
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(PotionSplashEvent)}.
     * Passive animal in splash radius, island disallows → HURT_ANIMALS blocked.
     */
    @Test
    public void testOnSplashPotionSplashAnimalDisallowed() {
        PotionEffectType effectType = mock(PotionEffectType.class);
        ThrownPotion tp = mockThrownPotion(effectType);
        when(tp.getShooter()).thenReturn(mockPlayer);
        Animals animal = mock(Animals.class);
        when(animal.getLocation()).thenReturn(location);
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(animal, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new HurtingListener().onSplashPotionSplash(e);
        verify(notifier).notify(user, "protection.protected");
        verify(animal).removePotionEffect(effectType);
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(PotionSplashEvent)}.
     * Villager in splash radius, island disallows → HURT_VILLAGERS blocked.
     */
    @Test
    public void testOnSplashPotionSplashVillagerDisallowed() {
        PotionEffectType effectType = mock(PotionEffectType.class);
        ThrownPotion tp = mockThrownPotion(effectType);
        when(tp.getShooter()).thenReturn(mockPlayer);
        Villager villager = mock(Villager.class);
        when(villager.getLocation()).thenReturn(location);
        when(villager.getType()).thenReturn(EntityType.VILLAGER);
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(villager, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new HurtingListener().onSplashPotionSplash(e);
        verify(notifier).notify(user, "protection.protected");
        verify(villager).removePotionEffect(effectType);
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(PotionSplashEvent)}.
     * Tamed animal in splash radius, island disallows → HURT_TAMED_ANIMALS blocked.
     */
    @Test
    public void testOnSplashPotionSplashTamedAnimalDisallowed() {
        PotionEffectType effectType = mock(PotionEffectType.class);
        ThrownPotion tp = mockThrownPotion(effectType);
        when(tp.getShooter()).thenReturn(mockPlayer);
        // Parrot is both Tameable and Animals
        Parrot parrot = mock(Parrot.class);
        when(parrot.getLocation()).thenReturn(location);
        Map<LivingEntity, Double> map = new HashMap<>();
        map.put(parrot, 10D);
        PotionSplashEvent e = new PotionSplashEvent(tp, map);
        new HurtingListener().onSplashPotionSplash(e);
        verify(notifier).notify(user, "protection.protected");
        verify(parrot).removePotionEffect(effectType);
    }

    // -----------------------------------------------------------------------
    // onLingeringPotionSplash
    // -----------------------------------------------------------------------

    /**
     * Test method for {@link HurtingListener#onLingeringPotionSplash(LingeringPotionSplashEvent)}.
     * Non-player shooter → nothing stored, no scheduler invoked.
     */
    @Test
    public void testOnLingeringPotionSplashNonPlayerShooter() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mock(Monster.class));
        org.bukkit.entity.AreaEffectCloud cloud = mock(org.bukkit.entity.AreaEffectCloud.class);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        new HurtingListener().onLingeringPotionSplash(e);
        verify(cloud, never()).getEntityId();
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionSplash(LingeringPotionSplashEvent)}.
     * Player shooter → entity ID stored, scheduler task registered.
     */
    @Test
    public void testOnLingeringPotionSplashPlayerShooter() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        org.bukkit.entity.AreaEffectCloud cloud = mock(org.bukkit.entity.AreaEffectCloud.class);
        when(cloud.getEntityId()).thenReturn(42);
        when(cloud.getDuration()).thenReturn(200);
        LingeringPotionSplashEvent e = new LingeringPotionSplashEvent(tp, cloud);
        new HurtingListener().onLingeringPotionSplash(e);
        // Entity ID is called for storage + for duration lookup + for the scheduled removal
        verify(cloud, Mockito.atLeastOnce()).getEntityId();
    }

    // -----------------------------------------------------------------------
    // onLingeringPotionDamage
    // -----------------------------------------------------------------------

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(EntityDamageByEntityEvent)}.
     * Wrong DamageCause → event ignored.
     */
    @Test
    public void testOnLingeringPotionDamageWrongCause() {
        Monster cloud = mock(Monster.class);
        when(cloud.getEntityId()).thenReturn(99);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(cloud, enderman, DamageCause.MAGIC, null, 5);
        new HurtingListener().onLingeringPotionDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(EntityDamageByEntityEvent)}.
     * Correct cause but damager not in thrownPotions map → event ignored.
     */
    @Test
    public void testOnLingeringPotionDamageNotInMap() {
        Monster cloud = mock(Monster.class);
        when(cloud.getEntityId()).thenReturn(99);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(cloud, enderman, DamageCause.ENTITY_ATTACK, null, 5);
        new HurtingListener().onLingeringPotionDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(EntityDamageByEntityEvent)}.
     * Damager is in thrownPotions, hits a monster, island disallows → HURT_MONSTERS blocked.
     */
    @Test
    public void testOnLingeringPotionDamageMonsterDisallowed() {
        // First, fire a lingering potion to populate thrownPotions
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        org.bukkit.entity.AreaEffectCloud cloud = mock(org.bukkit.entity.AreaEffectCloud.class);
        when(cloud.getEntityId()).thenReturn(7);
        when(cloud.getDuration()).thenReturn(100);
        LingeringPotionSplashEvent splash = new LingeringPotionSplashEvent(tp, cloud);
        HurtingListener hl = new HurtingListener();
        hl.onLingeringPotionSplash(splash);

        // Now simulate area-effect damage from the cloud entity (entity id 7)
        Monster cloudEntity = mock(Monster.class); // acts as the AreaEffectCloud entity in the damage event
        when(cloudEntity.getEntityId()).thenReturn(7);
        when(enderman.getLocation()).thenReturn(location);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(cloudEntity, enderman, DamageCause.ENTITY_ATTACK, null, 5);
        hl.onLingeringPotionDamage(e);
        assertTrue(e.isCancelled());
        verify(notifier).notify(user, "protection.protected");
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(EntityDamageByEntityEvent)}.
     * Damager in thrownPotions, hits monster, island allows → not cancelled.
     */
    @Test
    public void testOnLingeringPotionDamageMonsterAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        org.bukkit.entity.AreaEffectCloud cloud = mock(org.bukkit.entity.AreaEffectCloud.class);
        when(cloud.getEntityId()).thenReturn(8);
        when(cloud.getDuration()).thenReturn(100);
        HurtingListener hl = new HurtingListener();
        hl.onLingeringPotionSplash(new LingeringPotionSplashEvent(tp, cloud));

        Monster cloudEntity = mock(Monster.class);
        when(cloudEntity.getEntityId()).thenReturn(8);
        when(enderman.getLocation()).thenReturn(location);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(cloudEntity, enderman, DamageCause.ENTITY_ATTACK, null, 5);
        hl.onLingeringPotionDamage(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(EntityDamageByEntityEvent)}.
     * Self-damage (attacker == entity) → skipped.
     */
    @Test
    public void testOnLingeringPotionDamageSelfDamage() {
        ThrownPotion tp = mock(ThrownPotion.class);
        when(tp.getShooter()).thenReturn(mockPlayer);
        org.bukkit.entity.AreaEffectCloud cloud = mock(org.bukkit.entity.AreaEffectCloud.class);
        when(cloud.getEntityId()).thenReturn(9);
        when(cloud.getDuration()).thenReturn(100);
        HurtingListener hl = new HurtingListener();
        hl.onLingeringPotionSplash(new LingeringPotionSplashEvent(tp, cloud));

        Monster cloudEntity = mock(Monster.class);
        when(cloudEntity.getEntityId()).thenReturn(9);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(cloudEntity, mockPlayer, DamageCause.ENTITY_ATTACK, null, 5);
        hl.onLingeringPotionDamage(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    // -----------------------------------------------------------------------
    // onPlayerShootEvent
    // -----------------------------------------------------------------------

    /**
     * Test method for {@link HurtingListener#onPlayerShootEvent(EntityShootBowEvent)}.
     * Non-player entity type → firework not stored.
     */
    @Test
    public void testOnPlayerShootEventNotPlayer() {
        HurtingListener hl = new HurtingListener();
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(100);
        EntityShootBowEvent e = new EntityShootBowEvent(enderman, null, null, firework, EquipmentSlot.HAND, 1F, false);
        hl.onPlayerShootEvent(e);

        // Verify nothing was stored: a subsequent onFireworkDamage should have no effect
        when(firework.getLocation()).thenReturn(location);
        EntityDamageByEntityEvent dmg = new EntityDamageByEntityEvent(firework, enderman, DamageCause.ENTITY_EXPLOSION, null, 5);
        hl.onFireworkDamage(dmg);
        assertFalse(dmg.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onPlayerShootEvent(EntityShootBowEvent)}.
     * Player shoots an arrow (not a firework) → not stored.
     */
    @Test
    public void testOnPlayerShootEventNotFirework() {
        HurtingListener hl = new HurtingListener();
        Projectile arrow = mock(Projectile.class);
        EntityShootBowEvent e = new EntityShootBowEvent(mockPlayer, null, null, arrow, EquipmentSlot.HAND, 1F, false);
        hl.onPlayerShootEvent(e);
        verify(notifier, never()).notify(any(), any());
    }

    /**
     * Test method for {@link HurtingListener#onPlayerShootEvent(EntityShootBowEvent)}.
     * Player shoots a firework → stored; subsequent onFireworkDamage on monster is blocked.
     */
    @Test
    public void testOnPlayerShootEventPlayerFireworkStoredThenDamageBlocked() {
        HurtingListener hl = new HurtingListener();
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(101);
        EntityShootBowEvent shoot = new EntityShootBowEvent(mockPlayer, null, null, firework, EquipmentSlot.HAND, 1F, false);
        hl.onPlayerShootEvent(shoot);

        // Now the firework damages a monster
        when(enderman.getLocation()).thenReturn(location);
        EntityDamageByEntityEvent dmg = new EntityDamageByEntityEvent(firework, enderman, DamageCause.ENTITY_EXPLOSION, null, 5);
        hl.onFireworkDamage(dmg);
        assertTrue(dmg.isCancelled());
        verify(notifier).notify(user, "protection.protected");
    }

    // -----------------------------------------------------------------------
    // onFireworkDamage
    // -----------------------------------------------------------------------

    /**
     * Test method for {@link HurtingListener#onFireworkDamage(EntityDamageByEntityEvent)}.
     * Damager is not a Firework → event ignored.
     */
    @Test
    public void testOnFireworkDamageDamagerNotFirework() {
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(enderman, enderman, DamageCause.ENTITY_EXPLOSION, null, 5);
        new HurtingListener().onFireworkDamage(e);
        assertFalse(e.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

    /**
     * Test method for {@link HurtingListener#onFireworkDamage(EntityDamageByEntityEvent)}.
     * Damager is a Firework but not tracked in firedFireworks → event ignored.
     */
    @Test
    public void testOnFireworkDamageFireworkNotInMap() {
        Firework firework = mock(Firework.class);
        when(firework.getEntityId()).thenReturn(200);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(firework, enderman, DamageCause.ENTITY_EXPLOSION, null, 5);
        new HurtingListener().onFireworkDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link HurtingListener#onFireworkDamage(EntityDamageByEntityEvent)}.
     * Player-fired firework, island allows → not cancelled.
     */
    @Test
    public void testOnFireworkDamageAllowed() {
        when(island.isAllowed(any(), any())).thenReturn(true);
        HurtingListener hl = new HurtingListener();
        Firework firework = mock(Firework.class);
        EntityShootBowEvent shoot = new EntityShootBowEvent(mockPlayer, null, null, firework, EquipmentSlot.HAND, 1F, false);
        hl.onPlayerShootEvent(shoot);

        when(enderman.getLocation()).thenReturn(location);
        EntityDamageByEntityEvent dmg = new EntityDamageByEntityEvent(firework, enderman, DamageCause.ENTITY_EXPLOSION, null, 5);
        hl.onFireworkDamage(dmg);
        assertFalse(dmg.isCancelled());
        verify(notifier, never()).notify(any(), any());
    }

}
