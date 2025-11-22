package world.bentobox.bentobox.listeners.flags.worldsettings;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Location;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
public class ItemFrameListenerTest extends CommonTestSetup  {

    @Mock
    private Enderman enderman;
    @Mock
    private ItemFrame entity;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Mockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);

        // Worlds
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
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
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(Mockito.any())).thenReturn(optional);

        // Util
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(mock(World.class));

        // Item Frame
        when(entity.getWorld()).thenReturn(world);
        when(entity.getLocation()).thenReturn(location);

        // Not allowed to start
        Flags.ITEM_FRAME_DAMAGE.setSetting(world, false);

    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link ItemFrameListener#onItemFrameDamage(org.bukkit.event.entity.EntityDamageByEntityEvent)}.
     */
    @Test
    public void testOnItemFrameDamageEntityDamageByEntityEvent() {
        ItemFrameListener ifl = new ItemFrameListener();
        DamageCause cause = DamageCause.ENTITY_ATTACK;
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(enderman, entity, cause, null, 0);
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
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(enderman, creeper, cause, null, 0);
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
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, entity, cause, null, 0);
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
        EntityDamageByEntityEvent e = new EntityDamageByEntityEvent(p, entity, cause, null, 0);
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
