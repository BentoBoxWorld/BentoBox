/**
 *
 */
package world.bentobox.bentobox.listeners.flags.protection;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class HurtingListenerTest {

    private Enderman enderman;
    private LocalesManager lm;
    private Notifier notifier;
    private Island island;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        Location location = mock(Location.class);
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
        enderman = mock(Enderman.class);
        when(enderman.getLocation()).thenReturn(location);
        when(enderman.getWorld()).thenReturn(world);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(optional);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        // Locales
        lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> invocation.getArgumentAt(1, String.class);
        when(lm.get(Mockito.any(), Mockito.any())).thenAnswer(answer);

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

        // Utils
        when(Util.isPassiveEntity(Mockito.any())).thenCallRealMethod();
        when(Util.isHostileEntity(Mockito.any())).thenCallRealMethod();

    }

    /**
     * Test method for {@link HurtingListener#onEntityDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnEntityDamage() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowArmorStandCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        ArmorStand entity = mock(ArmorStand.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowArmorStandCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        ArmorStand entity = mock(ArmorStand.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowAnimalCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        Animals entity = mock(Animals.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowAnimalsCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        Animals entity = mock(Animals.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowMonsterCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        Monster entity = mock(Monster.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowMonsterCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        Monster entity = mock(Monster.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingDisallowVillagerCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        Villager entity = mock(Villager.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link HurtingListener#onFishing(org.bukkit.event.player.PlayerFishEvent)}.
     */
    @Test
    public void testOnFishingAllowVillagerCatching() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        Villager entity = mock(Villager.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        when(location.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);
        FishHook hookEntity = mock(FishHook.class);
        State state = State.CAUGHT_ENTITY;
        PlayerFishEvent e = new PlayerFishEvent(player, entity, hookEntity, state);
        HurtingListener hl = new HurtingListener();
        // Allow
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);
        hl.onFishing(e);
        // Verify
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.eq(user), Mockito.eq("protection.protected"));
    }
    /**
     * Test method for {@link HurtingListener#onPlayerFeedParrots(org.bukkit.event.player.PlayerInteractEntityEvent)}.
     */
    @Test
    public void testOnPlayerFeedParrots() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onSplashPotionSplash(org.bukkit.event.entity.PotionSplashEvent)}.
     */
    @Test
    public void testOnSplashPotionSplash() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionSplash(org.bukkit.event.entity.LingeringPotionSplashEvent)}.
     */
    @Test
    public void testOnLingeringPotionSplash() {
        //fail("Not yet implemented"); // TODO
    }

    /**
     * Test method for {@link HurtingListener#onLingeringPotionDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnLingeringPotionDamage() {
        //fail("Not yet implemented"); // TODO
    }

}
