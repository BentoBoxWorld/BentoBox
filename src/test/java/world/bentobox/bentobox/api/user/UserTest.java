package world.bentobox.bentobox.api.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.AbstractCommonSetup;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.Addon;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.AddonDescription.Builder;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.metadata.MetaDataValue;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class, Util.class })
public class UserTest extends AbstractCommonSetup {

    private static final String TEST_TRANSLATION = "mock &a translation &b [test]";
    private static final String TEST_TRANSLATION_WITH_COLOR = "mock §atranslation §b[test]";
    @Mock
    private LocalesManager lm;

    private User user;

    private UUID uuid;
    @Mock
    private CommandSender sender;
    @Mock
    private Server server;
    @Mock
    private PlayersManager pm;
    private @Nullable Players players;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        uuid = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(uuid);

        ItemFactory itemFactory = mock(ItemFactory.class);

        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(mockPlayer);
        when(Bukkit.getPluginManager()).thenReturn(pim);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getServer()).thenReturn(server);

        // Player
        when(mockPlayer.getServer()).thenReturn(server);
        when(server.getOnlinePlayers()).thenReturn(Collections.emptySet());
        when(sender.spigot()).thenReturn(spigot);
        @NonNull
        World world = mock(World.class);
        when(world.getName()).thenReturn("BSkyBlock");
        when(mockPlayer.getWorld()).thenReturn(world);

        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        when(iwm.getFriendlyName(world)).thenReturn("BSkyBlock-Fiendly");

        user = User.getInstance(mockPlayer);

        // Locales
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn(TEST_TRANSLATION);
        when(lm.get(any())).thenReturn(TEST_TRANSLATION);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        // This will just return the value of the second argument of replacePlaceholders. i.e., it won't change anything
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));

        when(plugin.getPlayers()).thenReturn(pm);
        players = new Players();
        when(pm.getPlayer(any())).thenReturn(players);
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
        assertEquals(mockPlayer, user.getPlayer());
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
        User.removePlayer(mockPlayer);
        // If the player has been removed from the cache, then code will ask server for player
        // Return null and check if instance is null will show that the player is not in the cache
        when(Bukkit.getPlayer(any(UUID.class))).thenReturn(null);
    }

    @Test
    public void testSetPlugin() {
        BentoBox plugin = mock(BentoBox.class);
        User.setPlugin(plugin);
        user.addPerm("testing123");
        verify(mockPlayer).addAttachment(eq(plugin), eq("testing123"), eq(true));
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
        when(mockPlayer.getInventory()).thenReturn(value);
        assertEquals(value, mockPlayer.getInventory());
        User user = User.getInstance(mockPlayer);
        assertNotNull(user.getInventory());
        assertEquals(value, user.getInventory());
    }

    @Test
    public void testGetLocation() {
        Location loc = mock(Location.class);
        when(mockPlayer.getLocation()).thenReturn(loc);
        User user = User.getInstance(mockPlayer);
        assertNotNull(user.getLocation());
        assertEquals(loc, user.getLocation());
    }

    @Test
    public void testGetName() {
        String name = "tastybento";
        when(mockPlayer.getName()).thenReturn(name);
        User user = User.getInstance(mockPlayer);
        assertNotNull(user.getName());
        assertEquals(name, user.getName());

    }

    @Test
    public void testGetPlayer() {
        User user = User.getInstance(mockPlayer);
        assertEquals(mockPlayer, user.getPlayer());
    }

    @Test
    public void testIsPlayer() {
        User user = User.getInstance(sender);
        assertFalse(user.isPlayer());
        user = User.getInstance(mockPlayer);
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
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        user = User.getInstance(mockPlayer);
        assertEquals(uuid, user.getUniqueId());
    }

    @Test
    public void testHasPermission() {
        // default behaviors
        assertTrue(user.hasPermission(""));
        assertTrue(user.hasPermission(null));

        // test if player has the permission
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);
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
        when(mockPlayer.isOnline()).thenReturn(true);
        assertTrue(user.isOnline());
    }

    @Test
    public void testIsOp() {
        when(mockPlayer.isOp()).thenReturn(true);
        assertTrue(user.isOp());
    }

    @Test
    public void testGetTranslation() {
        assertEquals(TEST_TRANSLATION_WITH_COLOR, user.getTranslation("a.reference"));
    }

    /**
     * Test for {@link User#getTranslationNoColor(String, String...)}
     */
    @Test
    public void testGetTranslationNoColor() {
        assertEquals(TEST_TRANSLATION, user.getTranslationNoColor("a.reference"));
    }

    @Test
    public void testGetTranslationWithVariable() {
        assertEquals("mock §atranslation §bvariable", user.getTranslation("a.reference", "[test]", "variable"));
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

        User user = User.getInstance(mockPlayer);
        assertEquals("", user.getTranslationOrNothing("fake.reference"));
        assertEquals("", user.getTranslationOrNothing("fake.reference", "[test]", "variable"));
    }

    @Test
    public void testSendMessage() {
        user.sendMessage("a.reference");
        checkSpigotMessage(TEST_TRANSLATION_WITH_COLOR);
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
        verify(mockPlayer, never()).sendMessage(eq(TEST_TRANSLATION));
        checkSpigotMessage("mockmockmock");
    }

    @Test
    public void testSendMessageBlankTranslation() {
        // Nothing - blank translation
        when(lm.get(any(), any())).thenReturn("");
        user.sendMessage("a.reference");
        checkSpigotMessage("a.reference", 0);
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
        verify(mockPlayer, never()).sendMessage(anyString());
    }

    @Test
    public void testSendMessageColorsAndSpaces() {
        when(lm.get(any(), any())).thenReturn(ChatColor.COLOR_CHAR + "6 Hello there");
        user.sendMessage("a.reference");
        checkSpigotMessage(ChatColor.COLOR_CHAR + "6Hello there");
    }

    @Test
    public void testSendRawMessage() {
        String raw = ChatColor.RED + "" + ChatColor.BOLD + "test message";
        user.sendRawMessage(raw);
        checkSpigotMessage(raw);
    }

    @Test
    public void testSendRawMessageNullUser() {
        String raw = ChatColor.RED + "" + ChatColor.BOLD + "test message";
        user = User.getInstance((CommandSender)null);
        user.sendRawMessage(raw);
        checkSpigotMessage(raw, 0);
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
        verify(mockPlayer, times(GameMode.values().length)).setGameMode(any());
    }

    @Test
    public void testTeleport() {
        when(mockPlayer.teleport(any(Location.class))).thenReturn(true);
        Location loc = mock(Location.class);
        user.teleport(loc);
        verify(mockPlayer).teleport(loc);
    }

    @Test
    public void testGetWorld() {
        World world = mock(World.class);
        when(mockPlayer.getWorld()).thenReturn(world);
        User user = User.getInstance(mockPlayer);
        assertEquals(world, user.getWorld());
    }

    @Test
    public void testCloseInventory() {
        user.closeInventory();
        verify(mockPlayer).closeInventory();
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
        verify(mockPlayer).updateInventory();
    }

    @Test
    public void testPerformCommand() {
        user.performCommand("test");
        verify(mockPlayer).performCommand("test");
    }

    @Test
    public void testEqualsObject() {
        User user1 = User.getInstance(UUID.randomUUID());
        User user2 = User.getInstance(UUID.randomUUID());
        assertEquals(user1, user1);
        assertNotEquals(user1, user2);
        assertNotEquals(null, user1);
        assertNotEquals(user2, user1);
        assertNotEquals(null, user2);
        assertNotEquals("a string", user2);

        user1 = User.getInstance((UUID)null);
        assertNotEquals(user2, user1);
    }

    @Test
    public void testHashCode() {
        UUID uuid = UUID.randomUUID();
        User user1 = User.getInstance(uuid);
        User user2 = User.getInstance(uuid);
        assertEquals(user1, user2);
        assertTrue(user1.hashCode() == user2.hashCode());
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
        when(mockPlayer.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(mockPlayer);
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
        when(mockPlayer.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(mockPlayer);
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
        when(mockPlayer.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(mockPlayer);
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
        when(mockPlayer.getEffectivePermissions()).thenReturn(permSet);
        User u = User.getInstance(mockPlayer);
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
        when(mockPlayer.getEffectivePermissions()).thenReturn(Collections.singleton(pai));
        User u = User.getInstance(mockPlayer);
        assertEquals(3, u.getPermissionValue("bskyblock.max", 22));
    }

    @Test
    public void testMetaData() {
        User u = User.getInstance(mockPlayer);
        assertTrue(u.getMetaData().get().isEmpty());
        // Store a string in a new key
        assertFalse(u.putMetaData("string", new MetaDataValue("a string")).isPresent());
        // Store an int in a new key
        assertFalse(u.putMetaData("int", new MetaDataValue(1234)).isPresent());
        // Overwrite the string with the same key
        assertEquals("a string", u.putMetaData("string", new MetaDataValue("a new string")).get().asString());
        // Get the new string with the same key
        assertEquals("a new string", u.getMetaData("string").get().asString());
        // Try to get a non-existent key
        assertFalse(u.getMetaData("boogie").isPresent());
        // Remove existing key
        assertEquals(1234, u.removeMetaData("int").get().asInt());
        assertFalse(u.getMetaData("int").isPresent());
        // Try to remove non-existent key
        assertFalse(u.removeMetaData("ggogg").isPresent());
        // Set the meta data as blank
        assertFalse(u.getMetaData().get().isEmpty());
        u.setMetaData(new HashMap<>());
        assertTrue(u.getMetaData().get().isEmpty());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getInstance(org.bukkit.OfflinePlayer)}.
     */
    @Test
    public void testGetInstanceOfflinePlayer() {
        OfflinePlayer op = mock(OfflinePlayer.class);
        when(op.getUniqueId()).thenReturn(uuid);
        @NonNull
        User offlineUser = User.getInstance(op);
        // Get it again and it should be the same because the UUID is the same
        User again = User.getInstance(op);
        assertEquals(offlineUser, again);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getOfflinePlayer()}.
     */
    @Test
    public void testGetOfflinePlayer() {
        User.clearUsers();
        OfflinePlayer op = mock(OfflinePlayer.class);
        when(op.getUniqueId()).thenReturn(uuid);
        @NonNull
        User offlineUser = User.getInstance(op);
        assertEquals(op, offlineUser.getOfflinePlayer());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#isOfflinePlayer()}.
     */
    @Test
    public void testIsOfflinePlayer() {
        User.clearUsers();
        OfflinePlayer op = mock(OfflinePlayer.class);
        when(op.getUniqueId()).thenReturn(uuid);
        @NonNull
        User offlineUser = User.getInstance(op);
        assertTrue(offlineUser.isOfflinePlayer());
        User.clearUsers();
        User s = User.getInstance(sender);
        assertFalse(s.isOfflinePlayer());
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        assertTrue(p.isOfflinePlayer());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#addPerm(java.lang.String)}.
     */
    @Test
    public void testAddPerm() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        p.addPerm("test.perm");
        verify(mockPlayer).addAttachment(plugin, "test.perm", true);
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#removePerm(java.lang.String)}.
     */
    @Test
    public void testRemovePerm() {
        User.clearUsers();
        // No perms to start
        when(mockPlayer.getEffectivePermissions()).thenReturn(Collections.emptySet());
        when(mockPlayer.hasPermission(anyString())).thenReturn(false);
        User p = User.getInstance(mockPlayer);
        assertTrue(p.removePerm("test.perm"));
        verify(mockPlayer).recalculatePermissions();
        // Has the perm
        PermissionAttachmentInfo pi = mock(PermissionAttachmentInfo.class);
        when(pi.getPermission()).thenReturn("test.perm");
        PermissionAttachment attachment = mock(PermissionAttachment.class);
        when(pi.getAttachment()).thenReturn(attachment);
        when(mockPlayer.getEffectivePermissions()).thenReturn(Set.of(pi));
        assertTrue(p.removePerm("test.perm"));
        verify(mockPlayer).removeAttachment(attachment);
    }



    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getTranslation(org.bukkit.World, java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testGetTranslationWorldStringStringArray() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        // No addon
        World world = mock(World.class);
        assertEquals("mock §atranslation §btastybento", p.getTranslation(world, "test.ref", "[test]", "tastybento"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getTranslation(org.bukkit.World, java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testGetTranslationWorldStringStringArrayWwithAddon() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        World world = mock(World.class);

        GameModeAddon gameAddon = mock(GameModeAddon.class);
        AddonDescription desc = new Builder("main", "gameAddon", "1.0").build();
        when(gameAddon.getDescription()).thenReturn(desc);
        when(iwm.getAddon(any(World.class))).thenReturn(Optional.of(gameAddon));
        assertEquals("mock §atranslation §btastybento", p.getTranslation(world, "test.ref", "[test]", "tastybento"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getTranslation(java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testGetTranslationStringStringArray() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        assertEquals("mock §atranslation §btastybento", p.getTranslation("test.ref", "[test]", "tastybento"));
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#notify(java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testNotifyStringStringArray() {
        Notifier notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        p.notify(TEST_TRANSLATION, "[test]", "tastybento");
        verify(notifier).notify(any(User.class), eq("mock §atranslation §btastybento"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#notify(org.bukkit.World, java.lang.String, java.lang.String[])}.
     */
    @Test
    public void testNotifyWorldStringStringArray() {
        Notifier notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        World world = mock(World.class);

        GameModeAddon gameAddon = mock(GameModeAddon.class);
        AddonDescription desc = new Builder("main", "gameAddon", "1.0").build();
        when(gameAddon.getDescription()).thenReturn(desc);
        when(iwm.getAddon(any(World.class))).thenReturn(Optional.of(gameAddon));
        p.notify(world, TEST_TRANSLATION, "[test]", "tastybento");
        verify(notifier).notify(any(User.class), eq("mock §atranslation §btastybento"));

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getLocale()}.
     */
    @Test
    public void testGetLocaleDefaultLanguage() {
        Settings settings = mock(Settings.class);
        when(settings.getDefaultLanguage()).thenReturn("en-US");
        when(plugin.getSettings()).thenReturn(settings);
        User.clearUsers();
        User console = User.getInstance(sender);
        assertEquals(Locale.US, console.getLocale());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getLocale()}.
     */
    @Test
    public void testGetLocale() {
        Settings settings = mock(Settings.class);
        when(settings.getDefaultLanguage()).thenReturn("en-US");
        when(plugin.getSettings()).thenReturn(settings);
        when(pm.getLocale(uuid)).thenReturn("fr-FR");
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        assertEquals(Locale.FRANCE, p.getLocale());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#inWorld()}.
     */
    @Test
    public void testInWorld() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        when(mockPlayer.getLocation()).thenReturn(mock(Location.class));
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        assertFalse(p.inWorld());
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        assertTrue(p.inWorld());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#spawnParticle(org.bukkit.Particle, java.lang.Object, double, double, double)}.
     */
    @Test
    public void testSpawnParticleParticleObjectDoubleDoubleDoubleError() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        try {
            p.spawnParticle(Particle.DUST, 4, 0.0d, 0.0d, 0.0d);
        } catch (Exception e) {
            assertEquals("A non-null DustOptions must be provided when using Particle.DUST as particle.",
                    e.getMessage());
        }
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#spawnParticle(org.bukkit.Particle, java.lang.Object, double, double, double)}.
     */
    @Test
    public void testSpawnParticleParticleObjectDoubleDoubleDouble() {
        User.clearUsers();
        Location loc = mock(Location.class);
        when(mockPlayer.getLocation()).thenReturn(loc);
        when(loc.toVector()).thenReturn(new Vector(1,1,1));
        when(server.getViewDistance()).thenReturn(16);

        User p = User.getInstance(mockPlayer);
        p.spawnParticle(Particle.SHRIEK, 4, 0.0d, 0.0d, 0.0d);
        verify(mockPlayer).spawnParticle(Particle.SHRIEK, 0.0d, 0.0d, 0.0d, 1, 4);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#spawnParticle(org.bukkit.Particle, java.lang.Object, double, double, double)}.
     */
    @Test
    public void testSpawnParticleParticleObjectDoubleDoubleDoubleRedstone() {
        User.clearUsers();
        Location loc = mock(Location.class);
        when(mockPlayer.getLocation()).thenReturn(loc);
        when(loc.toVector()).thenReturn(new Vector(1,1,1));
        when(server.getViewDistance()).thenReturn(16);

        User p = User.getInstance(mockPlayer);
        DustOptions dust = mock(DustOptions.class);
        p.spawnParticle(Particle.DUST, dust, 0.0d, 0.0d, 0.0d);
        verify(mockPlayer).spawnParticle(Particle.DUST, 0.0d, 0.0d, 0.0d, 1, 0, 0, 0, 1, dust);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#spawnParticle(org.bukkit.Particle, org.bukkit.Particle.DustOptions, double, double, double)}.
     */
    @Test
    public void testSpawnParticleParticleDustOptionsDoubleDoubleDouble() {
        User.clearUsers();
        Location loc = mock(Location.class);
        when(mockPlayer.getLocation()).thenReturn(loc);
        when(loc.toVector()).thenReturn(new Vector(1,1,1));
        when(server.getViewDistance()).thenReturn(16);

        User p = User.getInstance(mockPlayer);
        DustOptions dust = mock(DustOptions.class);
        p.spawnParticle(Particle.DUST, dust, 0.0d, 0.0d, 0.0d);
        verify(mockPlayer).spawnParticle(Particle.DUST, 0.0d, 0.0d, 0.0d, 1, 0, 0, 0, 1, dust);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#spawnParticle(org.bukkit.Particle, org.bukkit.Particle.DustOptions, int, int, int)}.
     */
    @Test
    public void testSpawnParticleParticleDustOptionsIntIntInt() {
        User.clearUsers();
        Location loc = mock(Location.class);
        when(mockPlayer.getLocation()).thenReturn(loc);
        when(loc.toVector()).thenReturn(new Vector(1,1,1));
        when(server.getViewDistance()).thenReturn(16);

        User p = User.getInstance(mockPlayer);
        DustOptions dust = mock(DustOptions.class);
        p.spawnParticle(Particle.DUST, dust, 0, 0, 0);
        verify(mockPlayer).spawnParticle(Particle.DUST, 0.0d, 0.0d, 0.0d, 1, 0, 0, 0, 1, dust);

    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#setAddon(world.bentobox.bentobox.api.addons.Addon)}.
     */
    @Test
    public void testSetAddon() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        Addon addon = mock(Addon.class);
        when(addon.getDescription()).thenReturn(new Builder("main", "gameAddon", "1.0").build());
        p.setAddon(addon);
        p.getTranslation(TEST_TRANSLATION);
        verify(addon, times(3)).getDescription();
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#getMetaData()}.
     */
    @Test
    public void testGetMetaData() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        when(pm.getPlayer(uuid)).thenReturn(players);
        assertEquals(Optional.of(new HashMap<>()), p.getMetaData());
    }

    /**
     * Test method for {@link world.bentobox.bentobox.api.user.User#setMetaData(java.util.Map)}.
     */
    @Test
    public void testSetMetaData() {
        User.clearUsers();
        User p = User.getInstance(mockPlayer);
        when(pm.getPlayer(uuid)).thenReturn(players);
        Map<String, MetaDataValue> metaData = new HashMap<>();
        p.setMetaData(metaData);
        assertEquals(Optional.of(metaData), p.getMetaData());
    }

}
