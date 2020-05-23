package world.bentobox.bentobox.api.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.PluginManager;
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

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class })
public class UserTest {

    private static final String TEST_TRANSLATION = "mock translation [test]";
    @Mock
    private Player player;
    @Mock
    private BentoBox plugin;
    @Mock
    private LocalesManager lm;

    private User user;
    @Mock
    private IslandWorldManager iwm;

    private UUID uuid;
    @Mock
    private PluginManager pim;
    @Mock
    private CommandSender sender;
    @Mock
    private Server server;

    @Before
    public void setUp() throws Exception {
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        User.setPlugin(plugin);

        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        ItemFactory itemFactory = mock(ItemFactory.class);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(player);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);

        // Player
        when(player.getServer()).thenReturn(server);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptySet());

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        // Addon
        when(iwm .getAddon(any())).thenReturn(Optional.empty());

        user = User.getInstance(player);

        // Locales
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn(TEST_TRANSLATION);
        when(lm.get(any())).thenReturn(TEST_TRANSLATION);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        // This will just return the value of the second argument of replacePlaceholders. i.e., it won't change anything
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    @Test
    public void testGetInstanceCommandSender() {
        User user = User.getInstance(sender);
        assertNotNull(user);
        assertEquals(sender,user.getSender());
    }

    @Test
    public void testGetInstancePlayer() {
        assertEquals(player,user.getPlayer());
    }

    @Test
    public void testGetInstanceUUID() {
        UUID uuid = UUID.randomUUID();
        User user = User.getInstance(uuid);
        assertNotNull(user);
        assertEquals(uuid,user.getUniqueId());
    }

    @Test
    public void testRemovePlayer() {
        assertNotNull(User.getInstance(uuid));
        assertEquals(user, User.getInstance(uuid));
        User.removePlayer(player);
        // If the player has been removed from the cache, then code will ask server for player
        // Return null and check if instance is null will show that the player is not in the cache
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
        assertNull(User.getInstance(uuid).getPlayer());
    }

    @Test
    public void testSetPlugin() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        user.addPerm("testing123");
        verify(player).addAttachment(eq(plugin), eq("testing123"), eq(true));
    }

    @Test
    public void testGetEffectivePermissions() {
        Set<PermissionAttachmentInfo> value = new HashSet<>();
        PermissionAttachmentInfo perm = new PermissionAttachmentInfo(sender, "perm", null, false);
        value.add(perm);
        when(sender.getEffectivePermissions()).thenReturn(value );
        User user = User.getInstance(sender);
        assertEquals(value, user.getEffectivePermissions());
    }

    @Test
    public void testGetInventory() {
        PlayerInventory value = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(value);
        assertEquals(value, player.getInventory());
        User user = User.getInstance(player);
        assertNotNull(user.getInventory());
        assertEquals(value, user.getInventory());
    }

    @Test
    public void testGetLocation() {
        Location loc = mock(Location.class);
        when(player.getLocation()).thenReturn(loc);
        User user = User.getInstance(player);
        assertNotNull(user.getLocation());
        assertEquals(loc, user.getLocation());
    }

    @Test
    public void testGetName() {
        String name = "tastybento";
        when(player.getName()).thenReturn(name);
        User user = User.getInstance(player);
        assertNotNull(user.getName());
        assertEquals(name, user.getName());

    }

    @Test
    public void testGetPlayer() {
        User user = User.getInstance(player);
        assertEquals(player, user.getPlayer());
    }

    @Test
    public void testIsPlayer() {
        User user = User.getInstance(sender);
        assertFalse(user.isPlayer());
        user = User.getInstance(player);
        assertTrue(user.isPlayer());
    }

    @Test
    public void testGetSender() {
        User user = User.getInstance(sender);
        assertEquals(sender, user.getSender());
    }

    @Test
    public void testGetUniqueId() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        user = User.getInstance(player);
        assertEquals(uuid, user.getUniqueId());
    }

    @Test
    public void testHasPermission() {
        // default behaviours
        assertTrue(user.hasPermission(""));
        assertTrue(user.hasPermission(null));

        // test if player has the permission
        when(player.hasPermission(anyString())).thenReturn(true);
        assertTrue(user.hasPermission("perm"));
    }

    /**
     * Asserts that {@link User#hasPermission(String)} returns true when the user is op.
     * @since 1.3.0
     */
    @Test
    public void testHasNotPermissionButIsOp() {
        when(user.isOp()).thenReturn(true);
        assertTrue(user.hasPermission(""));
    }

    @Test
    public void testIsOnline() {
        when(player.isOnline()).thenReturn(true);
        assertTrue(user.isOnline());
    }

    @Test
    public void testIsOp() {
        when(player.isOp()).thenReturn(true);
        assertTrue(user.isOp());
    }

    @Test
    public void testGetTranslation() {
        assertEquals("mock translation [test]", user.getTranslation("a.reference"));
    }

    @Test
    public void testGetTranslationWithVariable() {
        assertEquals("mock translation variable", user.getTranslation("a.reference", "[test]", "variable"));
    }

    @Test
    public void testGetTranslationNoTranslationFound() {
        // Test no translation found
        when(lm.get(any(), any())).thenReturn(null);
        assertEquals("a.reference", user.getTranslation("a.reference"));

    }

    @Test
    public void testGetTranslationOrNothing() {
        // Return the original string to pretend that a translation could not be found
        when(lm.get(any(), any())).thenReturn("fake.reference");
        when(lm.get(any())).thenReturn("fake.reference");

        User user = User.getInstance(player);
        assertEquals("", user.getTranslationOrNothing("fake.reference"));
        assertEquals("", user.getTranslationOrNothing("fake.reference", "[test]", "variable"));
    }

    @Test
    public void testSendMessage() {
        user.sendMessage("a.reference");
        verify(player).sendMessage(eq(TEST_TRANSLATION));
    }

    @Test
    public void testSendMessageOverrideWithAddon() {
        GameModeAddon addon = mock(GameModeAddon.class);
        AddonDescription desc = new AddonDescription.Builder("mock", "name", "1.0").build();
        when(addon.getDescription()).thenReturn(desc);
        // Set addon context
        user.setAddon(addon);
        Optional<GameModeAddon> optionalAddon = Optional.of(addon);
        when(iwm .getAddon(any())).thenReturn(optionalAddon);
        when(lm.get(any(), eq("name.a.reference"))).thenReturn("mockmockmock");
        user.sendMessage("a.reference");
        verify(player, never()).sendMessage(eq(TEST_TRANSLATION));
        verify(player).sendMessage(eq("mockmockmock"));
    }

    @Test
    public void testSendMessageBlankTranslation() {
        // Nothing - blank translation
        when(lm.get(any(), any())).thenReturn("");
        user.sendMessage("a.reference");
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    public void testSendMessageOnlyColors() {
        // Nothing - just color codes
        StringBuilder allColors = new StringBuilder();
        for (ChatColor cc : ChatColor.values()) {
            allColors.append(cc);
        }
        when(lm.get(any(), any())).thenReturn(allColors.toString());
        user.sendMessage("a.reference");
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    public void testSendMessageColorsAndSpaces() {
        when(lm.get(any(), any())).thenReturn(ChatColor.COLOR_CHAR + "6 Hello there");
        user.sendMessage("a.reference");
        verify(player).sendMessage(eq(ChatColor.COLOR_CHAR + "6Hello there"));
    }

    @Test
    public void testSendRawMessage() {
        String raw = ChatColor.RED + "" + ChatColor.BOLD + "test message";
        user.sendRawMessage(raw);
        verify(player).sendMessage(raw);
    }

    @Test
    public void testSendRawMessageNullUser() {
        String raw = ChatColor.RED + "" + ChatColor.BOLD + "test message";
        user = User.getInstance((CommandSender)null);
        user.sendRawMessage(raw);
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    public void testNotifyStringStringArrayNotifyOK() {
        Notifier notifier = mock(Notifier.class);

        when(plugin.getNotifier()).thenReturn(notifier);
        String translation = ChatColor.RED + "" + ChatColor.BOLD + "test translation";
        when(lm.get(any(), any())).thenReturn(translation);

        // Set notify
        when(notifier.notify(any(), eq(translation))).thenReturn(true);

        user.notify("a.reference");
        verify(notifier).notify(user, translation);
    }


    @Test
    public void testSetGameMode() {
        for (GameMode gm: GameMode.values()) {
            user.setGameMode(gm);
        }
        verify(player, times(GameMode.values().length)).setGameMode(any());
    }

    @Test
    public void testTeleport() {
        when(player.teleport(any(Location.class))).thenReturn(true);
        Location loc = mock(Location.class);
        user.teleport(loc);
        verify(player).teleport(loc);
    }

    @Test
    public void testGetWorld() {
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        User user = User.getInstance(player);
        assertEquals(world, user.getWorld());
    }

    @Test
    public void testCloseInventory() {
        user.closeInventory();
        verify(player).closeInventory();
    }

    @Test
    public void testGetLocalePlayer() {
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getLocale(any())).thenReturn("en-US");

        // Confirm that Locale object is correctly obtained
        assertEquals(Locale.US, user.getLocale());
    }

    @Test
    public void testGetLocaleConsole() {
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getLocale(any())).thenReturn("en-US");

        // Confirm that Locale object is correctly obtained
        Locale locale = Locale.US;
        // Try for console
        User console = User.getInstance(mock(CommandSender.class));
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getDefaultLanguage()).thenReturn("en-US");
        assertEquals(locale, console.getLocale());
    }

    @Test
    public void testUpdateInventory() {
        user.updateInventory();
        verify(player).updateInventory();
    }

    @Test
    public void testPerformCommand() {
        user.performCommand("test");
        verify(player).performCommand("test");
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testEqualsObject() {
        User user1 = User.getInstance(UUID.randomUUID());
        User user2 = User.getInstance(UUID.randomUUID());
        assertTrue(user1.equals(user1));
        assertFalse(user1.equals(user2));
        assertFalse(user1.equals(null));
        assertFalse(user2.equals(user1));
        assertFalse(user2.equals(null));
        assertFalse(user2.equals("a string"));

        user1 = User.getInstance((UUID)null);
        assertFalse(user2.equals(user1));
    }

    @Test
    public void testHashCode() {
        UUID uuid = UUID.randomUUID();
        User user1 = User.getInstance(uuid);
        User user2 = User.getInstance(uuid);
        assertEquals(user1, user2);
        assertTrue(user1.hashCode() == user2.hashCode());
    }

    @Test
    public void testNullPlayer() {
        User user = User.getInstance((Player)null);
        assertNull(user);
    }

    /**
     * Test for {@link User#getPermissionValue(String, int)}
     */
    @Test
    public void testGetPermissionValue() {
        User.clearUsers();
        Set<PermissionAttachmentInfo> permSet = new HashSet<>();
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getPermission()).thenReturn("bskyblock.max.3");
        when(pai.getValue()).thenReturn(true);
        PermissionAttachmentInfo pai2 = mock(PermissionAttachmentInfo.class);
        when(pai2.getPermission()).thenReturn("bskyblock.max.7");
        when(pai2.getValue()).thenReturn(true);
        PermissionAttachmentInfo pai3 = mock(PermissionAttachmentInfo.class);
        when(pai3.getPermission()).thenReturn("bskyblock.max.33");
        when(pai3.getValue()).thenReturn(true);
        permSet.add(pai);
        permSet.add(pai2);
        permSet.add(pai3);
        when(player.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(player);
        assertEquals(33, u.getPermissionValue("bskyblock.max", 2));
    }

    /**
     * Test for {@link User#getPermissionValue(String, int)}
     */
    @Test
    public void testGetPermissionValueNegativePerm() {
        User.clearUsers();
        Set<PermissionAttachmentInfo> permSet = new HashSet<>();
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getPermission()).thenReturn("bskyblock.max.3");
        when(pai.getValue()).thenReturn(true);
        PermissionAttachmentInfo pai2 = mock(PermissionAttachmentInfo.class);
        when(pai2.getPermission()).thenReturn("bskyblock.max.7");
        when(pai2.getValue()).thenReturn(true);
        PermissionAttachmentInfo pai3 = mock(PermissionAttachmentInfo.class);
        when(pai3.getPermission()).thenReturn("bskyblock.max.33");
        when(pai3.getValue()).thenReturn(false); // Negative perm
        permSet.add(pai);
        permSet.add(pai2);
        permSet.add(pai3);
        when(player.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(player);
        assertEquals(7, u.getPermissionValue("bskyblock.max", 2));
    }

    /**
     * Test for {@link User#getPermissionValue(String, int)}
     */
    @Test
    public void testGetPermissionValueConsole() {
        User.clearUsers();
        CommandSender console = mock(CommandSender.class);
        User u = User.getInstance(console);
        assertEquals(35, u.getPermissionValue("bskyblock.max", 35));
    }

    /**
     * Test for {@link User#getPermissionValue(String, int)}
     */
    @Test
    public void testGetPermissionValueNegative() {
        User.clearUsers();
        Set<PermissionAttachmentInfo> permSet = new HashSet<>();
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getPermission()).thenReturn("bskyblock.max.3");
        when(pai.getValue()).thenReturn(true);
        PermissionAttachmentInfo pai2 = mock(PermissionAttachmentInfo.class);
        when(pai2.getPermission()).thenReturn("bskyblock.max.7");
        when(pai2.getValue()).thenReturn(true);
        PermissionAttachmentInfo pai3 = mock(PermissionAttachmentInfo.class);
        when(pai3.getPermission()).thenReturn("bskyblock.max.-1");
        when(pai3.getValue()).thenReturn(true);
        permSet.add(pai);
        permSet.add(pai2);
        permSet.add(pai3);
        when(player.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(player);
        assertEquals(-1, u.getPermissionValue("bskyblock.max", 2));
    }

    /**
     * Test for {@link User#getPermissionValue(String, int)}
     */
    @Test
    public void testGetPermissionValueStar() {
        User.clearUsers();
        Set<PermissionAttachmentInfo> permSet = new HashSet<>();
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getPermission()).thenReturn("bskyblock.max.3");
        PermissionAttachmentInfo pai2 = mock(PermissionAttachmentInfo.class);
        when(pai2.getPermission()).thenReturn("bskyblock.max.7");
        PermissionAttachmentInfo pai3 = mock(PermissionAttachmentInfo.class);
        when(pai3.getPermission()).thenReturn("bskyblock.max.*");
        permSet.add(pai);
        permSet.add(pai2);
        permSet.add(pai3);
        when(player.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(player);
        assertEquals(22, u.getPermissionValue("bskyblock.max", 22));
    }

    /**
     * Test for {@link User#getPermissionValue(String, int)}
     */
    @Test
    public void testGetPermissionValueSmall() {
        User.clearUsers();
        PermissionAttachmentInfo pai = mock(PermissionAttachmentInfo.class);
        when(pai.getPermission()).thenReturn("bskyblock.max.3");
        when(pai.getValue()).thenReturn(true);
        when(player.getEffectivePermissions()).thenReturn(Collections.singleton(pai));
        User u = User.getInstance(player);
        assertEquals(3, u.getPermissionValue("bskyblock.max", 22));
    }
}
