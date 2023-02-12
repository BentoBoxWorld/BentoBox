package world.bentobox.bentobox.listeners.flags;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.listeners.flags.protection.TestWorldSettings;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * Common items for testing. Don't forget to use super.setUp()!
 *
 * Sets up BentoBox plugin, pluginManager and ItemFactory.
 * Location, world, playersManager and player.
 * IWM, Addon and WorldSettings. IslandManager with one
 * island with protection and nothing allowed by default.
 * Owner of island is player with same UUID.
 * Locales, placeholders.
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
public abstract class AbstractCommonSetup {

    protected UUID uuid = UUID.randomUUID();

    @Mock
    protected Player player;
    @Mock
    protected PluginManager pim;
    @Mock
    protected ItemFactory itemFactory;
    @Mock
    protected Location location;
    @Mock
    protected World world;
    @Mock
    protected IslandWorldManager iwm;
    @Mock
    protected IslandsManager im;
    @Mock
    protected Island island;
    @Mock
    protected BentoBox plugin;
    @Mock
    protected PlayerInventory inv;
    @Mock
    protected Notifier notifier;
    @Mock
    protected FlagsManager fm;


    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);

        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        when(location.toVector()).thenReturn(new Vector(0,0,0));

        // Players Manager and meta data
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        Players players = mock(Players.class);
        when(players.getMetaData()).thenReturn(Optional.empty());
        when(pm.getPlayer(any(UUID.class))).thenReturn(players);

        // Player
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getLocation()).thenReturn(location);
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("tastybento");
        when(player.getInventory()).thenReturn(inv);

        User.setPlugin(plugin);
        User.getInstance(player);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        @Nullable
        WorldSettings worldSet = new TestWorldSettings();
        when(iwm.getWorldSettings(any())).thenReturn(worldSet);

        // Island Manager
        when(plugin.getIslands()).thenReturn(im);
        Optional<Island> optionalIsland = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optionalIsland);

        // Island - nothing is allowed by default
        when(island.isAllowed(any())).thenReturn(false);
        when(island.isAllowed(any(User.class), any())).thenReturn(false);
        when(island.getOwner()).thenReturn(uuid);
        when(island.getMemberSet()).thenReturn(ImmutableSet.of(uuid));

        // Enable reporting from Flags class
        MetadataValue mdv = new FixedMetadataValue(plugin, "_why_debug");
        when(player.getMetadata(anyString())).thenReturn(Collections.singletonList(mdv));

        // Locales & Placeholders
        LocalesManager lm = mock(LocalesManager.class);
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);

        // Fake players
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(mock(World.class));
        // Util translate color codes (used in user translate methods)
        when(Util.translateColorCodes(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

}
