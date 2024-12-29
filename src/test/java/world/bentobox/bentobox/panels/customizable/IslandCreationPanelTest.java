package world.bentobox.bentobox.panels.customizable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import io.papermc.paper.ServerBuildInfo;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBundle;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.CommandsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.mocks.ServerMocks;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Bukkit.class, BentoBox.class, ServerBuildInfo.class })
public class IslandCreationPanelTest {

    @Mock
    private User user;
    @Mock
    private IslandsManager im;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private BentoBox plugin;
    @Mock
    private Settings settings;
    @Mock
    private CompositeCommand ic;
    @Mock
    private BlueprintsManager bpm;
    @Mock
    private Inventory inv;
    @Mock
    private ItemMeta meta;
    @Mock
    private BlueprintBundle bb2;
    @Mock
    private BlueprintBundle bb3;

    /**
     * Location of the resources folder
     */
    private final Path resourcePath = Paths.get("src","test","resources");

    /**
     */
    @Before
    public void setUp() throws Exception {
        ServerMocks.newServer();

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);

        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        // Command manager
        CommandsManager cm = mock(CommandsManager.class);
        when(plugin.getCommandsManager()).thenReturn(cm);

        // Settings
        when(plugin.getSettings()).thenReturn(settings);

        // Player
        Player player = mock(Player.class);
        when(user.isOp()).thenReturn(false);
        when(user.isPlayer()).thenReturn(true);
        UUID uuid = UUID.randomUUID();
        when(user.getUniqueId()).thenReturn(uuid);
        when(user.getPlayer()).thenReturn(player);
        when(user.hasPermission(anyString())).thenReturn(true);
        User.setPlugin(plugin);
        // Set up user already
        User.getInstance(player);

        // Addon
        GameModeAddon addon = mock(GameModeAddon.class);
        when(addon.getDataFolder()).thenReturn(resourcePath.toFile());

        when(user.getTranslation(any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslation(any(World.class), any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(user.getTranslation(any(String.class), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));
        when(user.getTranslationOrNothing(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        when(user.getTranslation(any(World.class), eq("panels.island_creation.buttons.bundle.name"), any())).
            thenAnswer((Answer<String>) invocation -> invocation.getArgument(3, String.class));
        when(user.getTranslation(any(World.class), eq("panels.island_creation.buttons.bundle.description"), any())).
            thenAnswer((Answer<String>) invocation -> invocation.getArgument(3, String.class));
        when(plugin.getDescription()).thenAnswer((Answer<PluginDescriptionFile>) invocation ->
            new PluginDescriptionFile("BentoBox", "1.0", "world.bentobox.bentobox"));

        // Parent command has no aliases
        when(ic.getSubCommandAliases()).thenReturn(new HashMap<>());
        when(ic.getParameters()).thenReturn("parameters");
        when(ic.getDescription()).thenReturn("description");
        when(ic.getPermissionPrefix()).thenReturn("permission.");
        when(ic.getUsage()).thenReturn("");
        when(ic.getSubCommand(Mockito.anyString())).thenReturn(Optional.empty());
        when(ic.getAddon()).thenReturn(addon);
        World world = mock(World.class);
        when(ic.getWorld()).thenReturn(world);
        when(ic.getPlugin()).thenReturn(plugin);

        // No island for player to begin with (set it later in the tests)
        when(im.hasIsland(any(), eq(uuid))).thenReturn(false);
        // when(im.isOwner(any(), eq(uuid))).thenReturn(false);
        // Has team
        when(im.inTeam(any(), eq(uuid))).thenReturn(true);
        when(plugin.getIslands()).thenReturn(im);

        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);

        // Server & Scheduler
        BukkitScheduler sch = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sch);

        // IWM friendly name
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        when(plugin.getIWM()).thenReturn(iwm);

        // Panel inventory

        when(Bukkit.createInventory(any(), Mockito.anyInt(), anyString())).thenReturn(inv);

        // Item Factory (needed for ItemStack)
        ItemFactory itemF = mock(ItemFactory.class);
        when(itemF.getItemMeta(Mockito.any())).thenReturn(meta);
        when(Bukkit.getItemFactory()).thenReturn(itemF);

        // Bundles manager
        when(plugin.getBlueprintsManager()).thenReturn(bpm);

        // Bundles
        Map<String, BlueprintBundle> map = new HashMap<>();
        BlueprintBundle bb = mock(BlueprintBundle.class);
        when(bb.getUniqueId()).thenReturn("test");
        when(bb.getDisplayName()).thenReturn("test");
        when(bb.getIcon()).thenReturn(Material.STONE);
        when(bb.getDescription()).thenReturn(Collections.singletonList("A description"));
        when(bb.getSlot()).thenReturn(5);
        // Too small slot for panel
        when(bb2.getUniqueId()).thenReturn("test2");
        when(bb2.getDisplayName()).thenReturn("test2");
        when(bb2.getIcon()).thenReturn(Material.ACACIA_BOAT);
        when(bb2.getDescription()).thenReturn(Collections.singletonList("A description 2"));
        when(bb2.getSlot()).thenReturn(-5);
        // Too large slot for panel
        when(bb3.getUniqueId()).thenReturn("test3");
        when(bb3.getDisplayName()).thenReturn("test3");
        when(bb3.getIcon()).thenReturn(Material.BAKED_POTATO);
        when(bb3.getDescription()).thenReturn(Collections.singletonList("A description 3"));
        when(bb3.getSlot()).thenReturn(65);

        map.put("test", bb);
        map.put("test2", bb2);
        map.put("test3", bb3);
        when(bpm.getBlueprintBundles(any(GameModeAddon.class))).thenReturn(map);

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        ServerMocks.unsetBukkitServer();
    }

    /**
     * Test method for
     * {@link world.bentobox.bentobox.panels.customizable.IslandCreationPanel#openPanel(world.bentobox.bentobox.api.commands.CompositeCommand, world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testOpenPanel() {
        IslandCreationPanel.openPanel(ic, user, "", false);

        // Set correctly
        verify(inv).setItem(eq(0), any());
        verify(inv).setItem(eq(1), any());
        verify(meta).setDisplayName(eq("test"));
        verify(meta).setLore(eq(List.of("A description", "", "panels.tips.click-to-choose")));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.panels.customizable.IslandCreationPanel#openPanel(world.bentobox.bentobox.api.commands.CompositeCommand, world.bentobox.bentobox.api.user.User, java.lang.String)}.
     */
    @Test
    public void testOpenPanelSameSlot() {
        when(bb2.getSlot()).thenReturn(5);
        when(bb3.getSlot()).thenReturn(5);
        IslandCreationPanel.openPanel(ic, user, "", false);
        verify(inv).setItem(eq(0), any());
        verify(inv).setItem(eq(1), any());
        verify(meta).setDisplayName(eq("test"));
        verify(meta).setLore(eq(List.of("A description", "", "panels.tips.click-to-choose")));
        verify(inv).setItem(eq(0), any());
        verify(meta).setDisplayName(eq("test2"));
        verify(meta).setLore(eq(List.of("A description 2", "", "panels.tips.click-to-choose")));
        verify(inv).setItem(eq(1), any());
        verify(meta).setDisplayName(eq("test3"));
        verify(meta).setLore(eq(List.of("A description 3", "", "panels.tips.click-to-choose")));
    }

}
