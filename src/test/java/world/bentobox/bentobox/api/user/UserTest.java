package world.bentobox.bentobox.api.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;


@RunWith(PowerMockRunner.class)
public class UserTest {

    private static Player player;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Server server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        player = mock(Player.class);
        when(server.getPlayer(Mockito.any(UUID.class))).thenReturn(player);

        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        Bukkit.setServer(server);

        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());


    }

    private CommandSender sender;

    @Before
    public void setUp() throws Exception {
        sender = mock(CommandSender.class);
        User.clearUsers();
        User.setPlugin(null);
    }

    @Test
    public void testGetInstanceCommandSender() {
        User user = User.getInstance(sender);
        assertNotNull(user);
        assertEquals(sender,user.getSender());
    }

    @Test
    public void testGetInstancePlayer() {
        Player player = mock(Player.class);
        User user = User.getInstance(player);
        assertNotNull(user);
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
        User.removePlayer(player);
    }

    @Test
    public void testSetPlugin() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
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
        User user = User.getInstance(player);
        assertEquals(uuid, user.getUniqueId());
    }

    @Test
    public void testHasPermission() {
        when(player.hasPermission(Mockito.anyString())).thenReturn(true);
        User user = User.getInstance(player);
        assertTrue(user.hasPermission(""));
        assertTrue(user.hasPermission("perm"));
    }

    @Test
    public void testIsOnline() {
        when(player.isOnline()).thenReturn(true);
        User user = User.getInstance(player);
        assertTrue(user.isOnline());
    }

    @Test
    public void testIsOp() {
        when(player.isOp()).thenReturn(true);
        User user = User.getInstance(player);
        assertTrue(user.isOp());
    }

    @Test
    public void testGetTranslation() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation [test]");

        User user = User.getInstance(player);
        assertEquals("mock translation [test]", user.getTranslation("a.reference"));
        assertEquals("mock translation variable", user.getTranslation("a.reference", "[test]", "variable"));

        // Test no translation found
        when(lm.get(any(), any())).thenReturn(null);
        assertEquals("a.reference", user.getTranslation("a.reference"));

    }

    @Test
    public void testGetTranslationOrNothing() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Return the original string to pretend that a translation could not be found
        when(lm.get(any(), any())).thenReturn("fake.reference");

        User user = User.getInstance(player);
        assertEquals("", user.getTranslationOrNothing("fake.reference"));
        assertEquals("", user.getTranslationOrNothing("fake.reference", "[test]", "variable"));
    }

    @Test
    public void testSendMessage() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        String translation = ChatColor.RED + "" + ChatColor.BOLD + "test translation";
        when(lm.get(any(), any())).thenReturn(translation);

        Player pl = mock(Player.class);

        User user = User.getInstance(pl);
        user.sendMessage("a.reference");
        Mockito.verify(pl).sendMessage(translation);
    }

    @Test
    public void testSendMessageNullUser() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        String translation = ChatColor.RED + "" + ChatColor.BOLD + "test translation";
        when(lm.get(any(), any())).thenReturn(translation);

        Player pl = mock(Player.class);

        User user = User.getInstance(UUID.randomUUID());
        user.sendMessage("a.reference");
        Mockito.verify(pl, Mockito.never()).sendMessage(Mockito.anyString());

    }

    @Test
    public void testSendMessageBlankTranslation() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        // Nothing - blank translation
        when(lm.get(any(), any())).thenReturn("");
        when(plugin.getLocalesManager()).thenReturn(lm);

        Player pl = mock(Player.class);
        User user = User.getInstance(pl);
        user.sendMessage("a.reference");
        Mockito.verify(pl, Mockito.never()).sendMessage(Mockito.anyString());

    }

    @Test
    public void testSendMessageOnlyColors() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);

        Player pl = mock(Player.class);
        User user = User.getInstance(pl);

        // Nothing - just color codes
        StringBuilder allColors = new StringBuilder();
        for (ChatColor cc : ChatColor.values()) {
            allColors.append(cc);
        }
        when(lm.get(any(), any())).thenReturn(allColors.toString());
        user.sendMessage("a.reference");
        Mockito.verify(pl, Mockito.never()).sendMessage(Mockito.anyString());

    }

    @Test
    public void testSendRawMessage() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        String raw = ChatColor.RED + "" + ChatColor.BOLD + "test message";

        Player pl = mock(Player.class);

        User user = User.getInstance(pl);
        user.sendRawMessage(raw);
        Mockito.verify(pl).sendMessage(raw);


    }

    @Test
    public void testSendRawMessageNullUser() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        String raw = ChatColor.RED + "" + ChatColor.BOLD + "test message";

        Player pl = mock(Player.class);

        User user = User.getInstance(UUID.randomUUID());
        user.sendRawMessage(raw);
        Mockito.verify(pl, Mockito.never()).sendMessage(Mockito.anyString());

    }

    @Test
    public void testNotifyStringStringArrayNotifyOK() {
        BentoBox plugin = mock(BentoBox.class);
        Notifier notifier = mock(Notifier.class);

        when(plugin.getNotifier()).thenReturn(notifier);
        User.setPlugin(plugin);
        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        String translation = ChatColor.RED + "" + ChatColor.BOLD + "test translation";
        when(lm.get(any(), any())).thenReturn(translation);

        Player pl = mock(Player.class);
        User user = User.getInstance(pl);

        // Set notify
        when(notifier.notify(Mockito.any(), Mockito.eq(translation))).thenReturn(true);

        user.notify("a.reference");
        Mockito.verify(notifier).notify(user, translation);

    }


    @Test
    public void testSetGameMode() {
        Player pl = mock(Player.class);
        User user = User.getInstance(pl);
        for (GameMode gm: GameMode.values()) {
            user.setGameMode(gm);
        }
        Mockito.verify(pl, Mockito.times(GameMode.values().length)).setGameMode(Mockito.any());

    }

    @Test
    public void testTeleport() {
        Player pl = mock(Player.class);
        User user = User.getInstance(pl);
        when(pl.teleport(Mockito.any(Location.class))).thenReturn(true);
        Location loc = mock(Location.class);
        user.teleport(loc);
        Mockito.verify(pl).teleport(loc);

    }

    @Test
    public void testGetWorld() {
        Player pl = mock(Player.class);
        World world = mock(World.class);
        when(pl.getWorld()).thenReturn(world);
        User user = User.getInstance(pl);
        assertEquals(world, user.getWorld());

    }

    @Test
    public void testCloseInventory() {
        Player pl = mock(Player.class);
        User user = User.getInstance(pl);
        user.closeInventory();
        Mockito.verify(pl).closeInventory();
    }

    @Test
    public void testGetLocalePlayer() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getLocale(Mockito.any())).thenReturn("en-US");

        // Confirm that Locale object is correctly obtained
        Locale locale = Locale.US;
        Player pl = mock(Player.class);
        User user = User.getInstance(pl);
        assertEquals(locale, user.getLocale());
    }

    @Test
    public void testGetLocaleConsole() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        when(pm.getLocale(Mockito.any())).thenReturn("en-US");

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
        Player pl = mock(Player.class);
        User user = User.getInstance(pl);
        user.updateInventory();
        Mockito.verify(pl).updateInventory();
    }

    @Test
    public void testPerformCommand() {
        Player pl = mock(Player.class);
        User user = User.getInstance(pl);
        user.performCommand("test");
        Mockito.verify(pl).performCommand("test");
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

}
