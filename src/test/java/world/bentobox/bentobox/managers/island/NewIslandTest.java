package world.bentobox.bentobox.managers.island;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitScheduler;
import org.eclipse.jdt.annotation.NonNull;
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
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.events.island.IslandCreateEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent;
import world.bentobox.bentobox.api.events.island.IslandEvent.IslandEventBuilder;
import world.bentobox.bentobox.api.events.island.IslandEvent.Reason;
import world.bentobox.bentobox.api.events.island.IslandResetEvent;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.IslandDeletionManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Util.class, IslandEvent.class, Bukkit.class, IslandsManager.class })
public class NewIslandTest {

    private static final String NAME = "name";
    @Mock
    private BentoBox plugin;
    @Mock
    private World world;
    @Mock
    private GameModeAddon addon;
    @Mock
    private User user;
    @Mock
    private Island oldIsland;
    @Mock
    private IslandsManager im;
    @Mock
    private Island island;
    @Mock
    private PlayersManager pm;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private IslandCreateEvent ice;
    @Mock
    private IslandResetEvent ire;
    @Mock
    private IslandDeletionManager idm;
    @Mock
    private Location location;
    @Mock
    private Block block;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private IslandEventBuilder builder;
    @Mock
    private BlueprintBundle bpb;

    private final UUID uuid = UUID.randomUUID();
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private @NonNull Location location2;

    /**
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        BentoBox plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        PowerMockito.mockStatic(IslandsManager.class, Mockito.RETURNS_MOCKS);
        // Islands manager
        when(plugin.getIslands()).thenReturn(im);
        when(im.createIsland(any(), any())).thenReturn(island);
        when(im.getLast(any())).thenReturn(location);
        when(im.getIsland(any(), any(User.class))).thenReturn(island);
        when(im.getPrimaryIsland(any(), any())).thenReturn(island);
        when(island.isReserved()).thenReturn(true);
        // Player's manager
        when(plugin.getPlayers()).thenReturn(pm);
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        Optional<GameModeAddon> optionalAddon = Optional.of(addon);
        when(iwm.getAddon(any())).thenReturn(optionalAddon);
        when(iwm.isDeathsResetOnNewIsland(any())).thenReturn(true);
        // Island deletion manager
        when(plugin.getIslandDeletionManager()).thenReturn(idm);
        when(idm.inDeletion(any())).thenReturn(false);
        // blueprints Manager
        when(bpb.getUniqueId()).thenReturn(NAME);
        when(bpm.getBlueprintBundles(any())).thenReturn(Collections.singletonMap(NAME, bpb));
        when(plugin.getBlueprintsManager()).thenReturn(bpm);

        // User
        when(user.getPermissionValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(20);
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getName()).thenReturn("tastybento");
        when(user.getWorld()).thenReturn(world);
        when(user.getLocation()).thenReturn(location);

        // Events
        PowerMockito.mockStatic(IslandEvent.class);
        when(IslandEvent.builder()).thenReturn(builder);
        when(builder.admin(anyBoolean())).thenReturn(builder);
        when(builder.blueprintBundle(any())).thenReturn(builder);
        when(builder.deletedIslandInfo(any())).thenReturn(builder);
        when(builder.involvedPlayer(any())).thenReturn(builder);
        when(builder.island(any())).thenReturn(builder);
        when(builder.location(any())).thenReturn(builder);
        when(builder.reason(any())).thenReturn(builder);
        when(builder.oldIsland(any())).thenReturn(builder);
        when(builder.build()).thenReturn(ice);
        when(ice.getBlueprintBundle()).thenReturn(bpb);
        when(ire.getBlueprintBundle()).thenReturn(bpb);

        // Location and blocks
        when(island.getWorld()).thenReturn(world);
        when(island.getCenter()).thenReturn(location2);
        when(location.getWorld()).thenReturn(world);
        when(world.getMaxHeight()).thenReturn(5);
        when(location.getBlock()).thenReturn(block);
        when(location.distance(any())).thenReturn(320D);
        when(block.getType()).thenReturn(Material.AIR);
        when(block.isEmpty()).thenReturn(true);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(oldIsland.getWorld()).thenReturn(world);

        // Util - return the same location
        PowerMockito.mockStatic(Util.class);
        when(Util.getClosestIsland(any()))
                .thenAnswer((Answer<Location>) invocation -> invocation.getArgument(0, Location.class));

        // Bukkit Scheduler
        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getScheduler()).thenReturn(scheduler);
        when(Bukkit.getViewDistance()).thenReturn(10);

        // Addon
        when(addon.getOverWorld()).thenReturn(world);
    }

    /**
     */
    @After
    public void tearDown() {
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilderNoUser() {
        try {
            NewIsland.builder().build();
        } catch (Exception e) {
            assertEquals("Insufficient parameters. Must have a user!", e.getMessage());
        }
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilder() throws Exception {
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).oldIsland(oldIsland)
                .build();
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(eq(world), eq(uuid), eq(0));
        verify(im, never()).setHomeLocation(eq(user), any());
        verify(island).setProtectionRange(eq(20));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilderReset() throws Exception {
        when(builder.build()).thenReturn(ire);
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.RESET).oldIsland(oldIsland).build();
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice, never()).getBlueprintBundle();
        verify(ire).getBlueprintBundle();
        verify(pm).setDeaths(eq(world), eq(uuid), eq(0));
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilderNoOldIsland() throws Exception {
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).build();
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(eq(world), eq(uuid), eq(0));
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilderNoOldIslandPasteNoNMS() throws Exception {
        when(location.distance(any())).thenReturn(30D);
        NewIsland.builder().addon(addon).name(NAME).player(user).reason(Reason.CREATE).build();
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(bpm).paste(eq(addon), eq(island), eq(NAME), any(Runnable.class), eq(false));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilderNoOldIslandPasteWithNMS() throws Exception {
        NewIsland.builder().addon(addon).name(NAME).player(user).reason(Reason.CREATE).build();
        PowerMockito.mockStatic(Bukkit.class);
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(bpm).paste(eq(addon), eq(island), eq(NAME), any(Runnable.class), eq(true));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilderHasIsland() throws Exception {
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).oldIsland(oldIsland).build();
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
        verify(island).setProtectionRange(eq(20));
        verify(island).setReserved(eq(false));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    public void testBuilderHasIslandFail() throws Exception {
        when(im.getIsland(any(), any(User.class))).thenReturn(null);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).oldIsland(oldIsland).build();
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
        verify(island).setProtectionRange(eq(20));
        //verify(plugin).logError("New island for user tastybento was not reserved!");
    }

    /**
     * Test method for {@link world.bentobox.bentobox.managers.island.NewIsland#builder()}.
     */
    @Test
    @Ignore("Not done")
    public void testBuilderHasIslandFailnoReserve() throws Exception {
        when(island.isReserved()).thenReturn(false);
        when(im.hasIsland(any(), any(User.class))).thenReturn(true);
        NewIsland.builder().addon(addon).name(NAME).player(user).noPaste().reason(Reason.CREATE).oldIsland(oldIsland).build();
        // Verifications
        PowerMockito.verifyStatic(IslandsManager.class);
        IslandsManager.updateIsland(eq(island));
        verify(island).setFlagsDefaults();
        verify(scheduler).runTask(any(BentoBox.class), any(Runnable.class));
        verify(builder, times(2)).build();
        verify(bpb).getUniqueId();
        verify(ice).getBlueprintBundle();
        verify(pm).setDeaths(world, uuid, 0);
        verify(im, never()).setHomeLocation(eq(user), any());
        verify(island).setProtectionRange(eq(20));
        verify(plugin).logError("New island for user tastybento was not reserved!");
    }

}