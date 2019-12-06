package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class ItemFrameListenerTest {

    @Mock
    private Enderman enderman;
    @Mock
    private World world;
    @Mock
    private ItemFrame entity;
    @Mock
    private Location location;

    @Before
    public void setUp() {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        PluginManager pim = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(pim);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        // Location
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
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Monsters and animals
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
        Island island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(optional);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        // Item Frame
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);

        // Not allowed to start
        Flags.ITEM_FRAME_DAMAGE.setSetting(world, false);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link ItemFrameListener#onItemFrameDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnItemFrameDamageEntityDamageByEntityEvent() {
        ItemFrameListener ifl = new ItemFrameListener();
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(enderman, entity, cause , 0);
        ifl.onItemFrameDamage(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link ItemFrameListener#onItemFrameDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testNotItemFrame() {
        ItemFrameListener ifl = new ItemFrameListener();
        Creeper creeper = mock(Creeper.class);
        when(creeper.getLocation()).thenReturn(location);
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(enderman, creeper, cause , 0);
        ifl.onItemFrameDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link ItemFrameListener#onItemFrameDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testProjectile() {
        ItemFrameListener ifl = new ItemFrameListener();
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Projectile p = mock(Projectile.class);
        when(p.getShooter()).thenReturn(enderman);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, entity, cause , 0);
        ifl.onItemFrameDamage(e);
        assertTrue(e.isCancelled());
    }

    /**
     * Test method for {@link ItemFrameListener#onItemFrameDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testPlayerProjectile() {
        ItemFrameListener ifl = new ItemFrameListener();
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        Projectile p = mock(Projectile.class);
        Player player = mock(Player.class);
        when(p.getShooter()).thenReturn(player);
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, entity, cause , 0);
        ifl.onItemFrameDamage(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link ItemFrameListener#onItemFrameDamage(org.bukkit.event.hanging.HangingBreakByEntityEvent)}.
     */
    @Test
    public void testOnItemFrameDamageHangingBreakByEntityEvent() {
        ItemFrameListener ifl = new ItemFrameListener();
        HangingBreakByEntityEvent e = new HangingBreakByEntityEvent(entity, enderman);
        ifl.onItemFrameDamage(e);
        assertTrue(e.isCancelled());
    }

}
