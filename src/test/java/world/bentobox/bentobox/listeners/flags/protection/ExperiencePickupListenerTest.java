/**
 *
 */
package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.junit.After;
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
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Tests the listener
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class, BentoBox.class, Flags.class, Util.class} )
public class ExperiencePickupListenerTest {

    private EntityTargetLivingEntityEvent e;
    private ExperiencePickupListener epl;
    private World world;
    private IslandWorldManager iwm;
    private Notifier notifier;
    private LivingEntity targetEntity;
    private Island island;
    private Entity entity;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // World
        world = mock(World.class);

        //FlagsManager flagsManager = new FlagsManager(plugin);
        //when(plugin.getFlagsManager()).thenReturn(flagsManager);

        // Worlds
        iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> (String)Arrays.asList(invocation.getArguments()).get(1);
        when(lm.get(any(), any())).thenAnswer(answer);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

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
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(true);

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));

        // Location
        Location location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);

        // Set up as valid
        entity = mock(ExperienceOrb.class);
        targetEntity = mock(Player.class);
        when(targetEntity.getLocation()).thenReturn(location);
        when(entity.getLocation()).thenReturn(location);

        TargetReason reason = TargetReason.CLOSEST_PLAYER;
        e = new EntityTargetLivingEntityEvent(entity, targetEntity, reason);
        epl = new ExperiencePickupListener();

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // Addon
        when(iwm.getAddon(Mockito.any())).thenReturn(Optional.empty());

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetPlayerNotAllowed() {
        // Not allowed
        when(island.isAllowed(Mockito.any(), Mockito.any())).thenReturn(false);
        epl.onExperienceOrbTargetPlayer(e);
        assertNull(e.getTarget());
        Mockito.verify(notifier).notify(Mockito.any(), Mockito.eq("protection.protected"));
    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetPlayerAllowed() {
        epl.onExperienceOrbTargetPlayer(e);
        assertNotNull(e.getTarget());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetNotPlayer() {
        targetEntity = mock(Zombie.class);
        epl.onExperienceOrbTargetPlayer(e);
        assertNotNull(e.getTarget());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

    /**
     * Test method for {@link ExperiencePickupListener#onExperienceOrbTargetPlayer(org.bukkit.event.entity.EntityTargetLivingEntityEvent)}.
     */
    @Test
    public void testOnExperienceOrbTargetPlayerNotOrb() {
        entity = mock(ArmorStand.class);
        epl.onExperienceOrbTargetPlayer(e);
        assertNotNull(e.getTarget());
        Mockito.verify(notifier, Mockito.never()).notify(Mockito.any(), Mockito.anyString());
    }

}
