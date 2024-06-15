package world.bentobox.bentobox.listeners.flags.settings;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Cow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
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
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.util.Util;
import world.bentobox.bentobox.versions.ServerCompatibility;


@RunWith(PowerMockRunner.class)
@PrepareForTest( {BentoBox.class, Bukkit.class, Flags.class, Util.class })
public class MobSpawnListenerTest {

    private Location location;
    @Mock
    private BentoBox plugin;
    @Mock
    private Zombie zombie;
    @Mock
    private Slime slime;
    @Mock
    private Cow cow;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private LivingEntity livingEntity;

    @Before
    public void setUp() {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);

        Server server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        ServerCompatibility serverCompatibility = mock(ServerCompatibility.class);
        Whitebox.setInternalState(ServerCompatibility.class, "instance", serverCompatibility);
        when(serverCompatibility.getServerVersion()).thenReturn(ServerCompatibility.ServerVersion.V1_19);

        PluginManager pim = mock(PluginManager.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(Bukkit.getPluginManager()).thenReturn(pim);

        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        PowerMockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);

        // Monsters and animals
        when(zombie.getLocation()).thenReturn(location);
        when(slime.getLocation()).thenReturn(location);
        when(cow.getLocation()).thenReturn(location);
        when(zombie.getWorld()).thenReturn(world);
        when(slime.getWorld()).thenReturn(world);
        when(cow.getWorld()).thenReturn(world);

        // Worlds
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Util class
        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(Mockito.any())).thenReturn(mock(World.class));
        when(Util.isPassiveEntity(Mockito.any())).thenCallRealMethod();
        when(Util.isHostileEntity(Mockito.any())).thenCallRealMethod();

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(Mockito.any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);
        GameModeAddon gma = mock(GameModeAddon.class);
        Optional<GameModeAddon> opGma = Optional.of(gma );
        when(iwm.getAddon(any())).thenReturn(opGma);

        // Default - plugin is loaded
        when(plugin.isLoaded()).thenReturn(true);

        // Living Entity
        when(livingEntity.getLocation()).thenReturn(location);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testNotLoaded() {
        when(plugin.isLoaded()).thenReturn(false);
        CreatureSpawnEvent e = new CreatureSpawnEvent(livingEntity, SpawnReason.NATURAL);
        MobSpawnListener l = new MobSpawnListener();
        l.onMobSpawn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    public void testNotInWorld() {
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // Set up entity
        LivingEntity entity = mock(LivingEntity.class);
        when(entity.getLocation()).thenReturn(null);

        // Setup event
        CreatureSpawnEvent e = new CreatureSpawnEvent(entity, SpawnReason.NATURAL);

        // Setup the listener
        MobSpawnListener l = new MobSpawnListener();
        l.setPlugin(plugin);

        // Should not be canceled
        l.onMobSpawn(e);
    }

    @Test
    public void testOnNaturalMonsterSpawnBlocked() {
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // Block mobs
        when(island.isAllowed(Mockito.any())).thenReturn(false);

        // Setup the listener
        MobSpawnListener l = new MobSpawnListener();
        l.setPlugin(plugin);

        // Check monsters
        checkBlocked(zombie,l);
        checkBlocked(slime,l);
        // Check animal
        checkBlocked(cow,l);

    }

    private void checkBlocked(LivingEntity le, MobSpawnListener l) {
        for (SpawnReason reason: SpawnReason.values()) {
            CreatureSpawnEvent e = new CreatureSpawnEvent(le, reason);
            switch (reason) {
                // Natural
                case DEFAULT, DROWNED, JOCKEY, LIGHTNING, MOUNT, NATURAL, NETHER_PORTAL, OCELOT_BABY, PATROL, RAID, REINFORCEMENTS, SILVERFISH_BLOCK, TRAP, VILLAGE_DEFENSE, VILLAGE_INVASION -> {
                    // These should be blocked
                    l.onMobSpawn(e);
                    assertTrue("Natural spawn should be blocked: " + reason.toString(), e.isCancelled());
                }
                // Spawners
                case SPAWNER -> {
                    l.onMobSpawn(e);
                    assertTrue("Spawners spawn should be blocked: " + reason.toString(), e.isCancelled());
                }
                // Unnatural - player involved or allowed
                case BREEDING, BUILD_IRONGOLEM, BUILD_SNOWMAN, BUILD_WITHER, CURED, CUSTOM, DISPENSE_EGG, EGG, ENDER_PEARL, EXPLOSION, INFECTION, SHEARED, SHOULDER_ENTITY, SPAWNER_EGG, SLIME_SPLIT -> {
                    l.onMobSpawn(e);
                    assertFalse("Should be not blocked: " + reason.toString(), e.isCancelled());
                }
                default -> {
                }
            }
        }

    }

    @Test
    public void testOnNaturalMobSpawnUnBlocked() {
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // Allow mobs
        when(island.isAllowed(Mockito.any())).thenReturn(true);

        // Setup the listener
        MobSpawnListener l = new MobSpawnListener();
        l.setPlugin(plugin);

        // Check monsters
        checkUnBlocked(zombie,l);
        checkUnBlocked(slime,l);
        // Check animal
        checkUnBlocked(cow,l);

    }

    private void checkUnBlocked(LivingEntity le, MobSpawnListener l) {
        for (SpawnReason reason: SpawnReason.values()) {
            CreatureSpawnEvent e = new CreatureSpawnEvent(le, reason);
            l.onMobSpawn(e);
            assertFalse(e.isCancelled());
        }
    }

    @Test
    public void testOnNaturalMonsterSpawnBlockedNoIsland() {
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIslandAt(any())).thenReturn(Optional.empty());

        // Block mobs
        Flags.MONSTER_NATURAL_SPAWN.setDefaultSetting(false);
        Flags.ANIMAL_NATURAL_SPAWN.setDefaultSetting(false);
        Flags.MONSTER_SPAWNERS_SPAWN.setDefaultSetting(false);
        Flags.ANIMAL_SPAWNERS_SPAWN.setDefaultSetting(false);

        // Setup the listener
        MobSpawnListener l = new MobSpawnListener();
        l.setPlugin(plugin);

        // Check monsters
        checkBlocked(zombie,l);
        checkBlocked(slime,l);
        // Check animal
        checkBlocked(cow,l);

    }

    @Test
    public void testOnNaturalMobSpawnUnBlockedNoIsland() {
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        when(im.getIslandAt(any())).thenReturn(Optional.empty());

        // Block mobs
        Flags.MONSTER_NATURAL_SPAWN.setDefaultSetting(true);
        Flags.ANIMAL_NATURAL_SPAWN.setDefaultSetting(true);
        Flags.MONSTER_SPAWNERS_SPAWN.setDefaultSetting(true);
        Flags.ANIMAL_SPAWNERS_SPAWN.setDefaultSetting(true);

        // Setup the listener
        MobSpawnListener l = new MobSpawnListener();
        l.setPlugin(plugin);

        // Check monsters
        checkUnBlocked(zombie,l);
        checkUnBlocked(slime,l);
        // Check animal
        checkUnBlocked(cow,l);
    }

}
