package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WanderingTrader;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class HurtingListenerTest {

    @Mock
    private Enderman enderman;
    @Mock
    private LocalesManager lm;
    @Mock
    private Notifier notifier;
    @Mock
    private Island island;
    @Mock
    private Player player;
    @Mock
    private Location location;
    @Mock
    private World world;
    @Mock
    private FishHook hookEntity;

    private User user;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pim = mock(PluginManager.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        PowerMockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);

        // Worlds
        IslandWorldManager iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Monsters and animals
        when(enderman.getLocation()).thenReturn(location);
        when(enderman.getWorld()).thenReturn(world);
        when(enderman.getType()).thenReturn(EntityType.ENDERMAN);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optional);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(mock(World.class));

        // Locales
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> invocation.getArgument(1, String.class);
        when(lm.get(any(), any())).thenAnswer(answer);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Utils
        when(Util.isPassiveEntity(any())).thenCallRealMethod();
        when(Util.isHostileEntity(any())).thenCallRealMethod();
        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();


        // User & player
        when(player.getLocation()).thenReturn(location);
        user = User.getInstance(player);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
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
        verify(notifier).notify(eq(user), eq("protection.protected"));
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
        verify(notifier, never()).notify(eq(user), eq("protection.protected"));
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
        verify(notifier).notify(eq(user), eq("protection.protected"));
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
        verify(notifier, never()).notify(eq(user), eq("protection.protected"));
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
        verify(notifier).notify(eq(user), eq("protection.protected"));
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
        verify(notifier, never()).notify(eq(user), eq("protection.protected"));
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
        verify(notifier).notify(eq(user), eq("protection.protected"));
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
        verify(notifier, never()).notify(eq(user), eq("protection.protected"));
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
        verify(notifier).notify(eq(user), eq("protection.protected"));
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
        verify(notifier).notify(eq(user), eq("protection.protected"));
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
        verify(notifier, never()).notify(eq(user), eq("protection.protected"));
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
        verify(notifier, never()).notify(eq(user), eq("protection.protected"));
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
