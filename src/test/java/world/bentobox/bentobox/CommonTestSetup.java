package world.bentobox.bentobox;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player.Spigot;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.tags.MaterialTagMock;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import com.google.common.collect.ImmutableSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.Notifier;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.database.objects.Players;
import world.bentobox.bentobox.managers.BlueprintsManager;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.HooksManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;
/**
 * Common items for testing. Don't forget to use super.setUp()!
 * <p>
 * Sets up BentoBox plugin, pluginManager and ItemFactory.
 * Location, world, playersManager and player.
 * IWM, Addon and WorldSettings. IslandManager with one
 * island with protection and nothing allowed by default.
 * Owner of island is player with same UUID.
 * Locales, placeholders.
 * @author tastybento
 *
 */
public abstract class CommonTestSetup {
    protected UUID uuid = UUID.randomUUID();
    @Mock
    protected Player mockPlayer;
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
    @Mock
    protected Spigot spigot;
    @Mock
    protected HooksManager hooksManager;
    @Mock
    protected BlueprintsManager bm;
    protected ServerMock server;
    protected MockedStatic<Bukkit> mockedBukkit;
    protected MockedStatic<Util> mockedUtil;
    protected AutoCloseable closeable;
    @Mock
    protected BukkitScheduler sch;
    @Mock
    protected LocalesManager lm;
    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // Processes the @Mock annotations and initializes the field
        closeable = MockitoAnnotations.openMocks(this);
        server = MockBukkit.mock();
        // Re-init Tag static fields BEFORE creating the Bukkit static mock.
        // Tag.<NAME> fields call Bukkit.getServer().getTag(...) on first JVM load.
        // If Mockito's RETURNS_DEEP_STUBS interceptor is already active when we do
        // the Unsafe write, it intercepts the read-back and returns the stale mock.
        // Doing this before mockStatic(Bukkit.class) ensures the written value sticks.
        reinitTagFields();
        // Bukkit
        // Set up plugin
        WhiteBox.setInternalState(BentoBox.class, "instance", plugin);
        // Register the static mock
        mockedBukkit = Mockito.mockStatic(Bukkit.class, Mockito.RETURNS_DEEP_STUBS);
        mockedBukkit.when(Bukkit::getMinecraftVersion).thenReturn("1.21.10");
        mockedBukkit.when(Bukkit::getBukkitVersion).thenReturn("");
        mockedBukkit.when(Bukkit::getPluginManager).thenReturn(pim);
        mockedBukkit.when(Bukkit::getItemFactory).thenReturn(itemFactory);
        mockedBukkit.when(Bukkit::getServer).thenReturn(server);
        // Location
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        when(location.toVector()).thenReturn(new Vector(0,0,0));
        when(location.clone()).thenReturn(location); // Paper
        // Players Manager and meta data
        PlayersManager pm = mock(PlayersManager.class);
        when(plugin.getPlayers()).thenReturn(pm);
        Players players = mock(Players.class);
        when(players.getMetaData()).thenReturn(Optional.empty());
        when(pm.getPlayer(any(UUID.class))).thenReturn(players);
        // Player
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.getLocation()).thenReturn(location);
        when(mockPlayer.getWorld()).thenReturn(world);
        when(mockPlayer.getName()).thenReturn("tastybento");
        when(mockPlayer.getInventory()).thenReturn(inv);
        when(mockPlayer.spigot()).thenReturn(spigot);
        when(mockPlayer.getType()).thenReturn(EntityType.PLAYER);
        when(mockPlayer.getWorld()).thenReturn(world);
        User.setPlugin(plugin);
        User.clearUsers();
        User.getInstance(mockPlayer);
        // IWM
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.getFriendlyName(any())).thenReturn("BSkyBlock");
        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());
        // World Settings
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
        when(island.getMemberSet(anyInt())).thenReturn(ImmutableSet.of(uuid));
        when(island.getMemberSet(anyInt(), anyBoolean())).thenReturn(ImmutableSet.of(uuid));
        // Enable reporting from Flags class
        MetadataValue mdv = new FixedMetadataValue(plugin, "_why_debug");
        when(mockPlayer.getMetadata(anyString())).thenReturn(Collections.singletonList(mdv));
        // Locales & Placeholders
        when(lm.get(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        PlaceholdersManager phm = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(phm);
        when(phm.replacePlaceholders(any(), any())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(1, String.class));
        when(plugin.getLocalesManager()).thenReturn(lm);
        // Notifier
        when(plugin.getNotifier()).thenReturn(notifier);
        // Fake players
        Settings settings = new Settings();
        when(plugin.getSettings()).thenReturn(settings);
        //Util
        mockedUtil = Mockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        mockedUtil.when(() -> Util.getWorld(any())).thenReturn(mock(World.class));
        Util.setPlugin(plugin);
        // Util
        mockedUtil.when(() -> Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();
        // Server & Scheduler
        mockedBukkit.when(Bukkit::getScheduler).thenReturn(sch);
        // Hooks
        when(hooksManager.getHook(anyString())).thenReturn(Optional.empty());
        when(plugin.getHooks()).thenReturn(hooksManager);
        // Blueprints Manager
        when(plugin.getBlueprintsManager()).thenReturn(bm);
    }
    /**
     * Reflectively re-sets every static {@link Tag} field to the real
     * {@link MaterialTagMock} supplied by the current MockBukkit
     * {@link ServerMock}.
     * <p>
     * Must be called <em>after</em> {@link MockBukkit#mock()} and
     * <em>before</em> {@code Mockito.mockStatic(Bukkit.class)}.
     * <p>
     * Tags not present in MockBukkit's registry (e.g. Paper-only tags) are
     * replaced with an empty {@link MaterialTagMock} so that
     * {@code isTagged()} returns {@code false} instead of throwing
     * {@code NotAMockException} at runtime.
     */
    @SuppressWarnings({"unchecked", "java:S3011"})
    private void reinitTagFields() {
        sun.misc.Unsafe unsafe;
        try {
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        } catch (Exception e) {
            return; // Cannot obtain Unsafe – skip tag reinit
        }
        for (java.lang.reflect.Field field : Tag.class.getDeclaredFields()) {
            int mod = field.getModifiers();
            if (!java.lang.reflect.Modifier.isStatic(mod)) continue;
            if (!Tag.class.isAssignableFrom(field.getType())) continue;
            NamespacedKey key = NamespacedKey.minecraft(
                    field.getName().toLowerCase(java.util.Locale.ROOT));
            // Ask the server for the MaterialTagMock (blocks first, then items).
            // Paper prefixes item-tag fields with ITEMS_ but Minecraft's namespaced
            // key omits that prefix (e.g. Tag.ITEMS_BOATS → "boats").
            Tag<?> tag = server.getTag(Tag.REGISTRY_BLOCKS, key, Material.class);
            if (tag == null) {
                tag = server.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
            }
            // Try stripping an "items_" prefix for Paper-style ITEMS_X fields
            if (tag == null && key.getKey().startsWith("items_")) {
                NamespacedKey stripped = NamespacedKey.minecraft(key.getKey().substring("items_".length()));
                tag = server.getTag(Tag.REGISTRY_ITEMS, stripped, Material.class);
                if (tag == null) {
                    tag = server.getTag(Tag.REGISTRY_BLOCKS, stripped, Material.class);
                }
            }
            // Fall back to an empty tag so isTagged() returns false safely
            if (tag == null) {
                tag = new MaterialTagMock(key);
            }
            try {
                field.setAccessible(true);
                long offset = unsafe.staticFieldOffset(field);
                unsafe.putObject(unsafe.staticFieldBase(field), offset, tag);
            } catch (Exception ignored) {
                // If Unsafe write fails, leave the field as-is
            }
        }
    }
    /**
     * @throws Exception
     */
    @AfterEach
    public void tearDown() throws Exception {
        // IMPORTANT: Explicitly close the mock to prevent leakage
        mockedBukkit.closeOnDemand();
        mockedUtil.closeOnDemand();
        closeable.close();
        MockBukkit.unmock();
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
        deleteAll(new File("database"));
        deleteAll(new File("database_backup"));
    }
    protected static void deleteAll(File file) throws IOException {
        if (file.exists()) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
    /**
     * Check that spigot sent the message
     * @param expectedMessage - message to check
     */
    public void checkSpigotMessage(String expectedMessage) {
        checkSpigotMessage(expectedMessage, 1);
    }
    public void checkSpigotMessage(String expectedMessage, int expectedOccurrences) {
        // Capture the argument passed to player.sendMessage(Component)
        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        // Verify that sendMessage() was called at least 0 times (capture any sent messages)
        verify(mockPlayer, atLeast(0)).sendMessage(captor.capture());
        // Get all captured Components
        List<Component> capturedMessages = captor.getAllValues();
        // Count the number of occurrences of the expectedMessage in the captured messages
        LegacyComponentSerializer legacy = LegacyComponentSerializer.builder().character('\u00A7').hexColors().build();
        long actualOccurrences = capturedMessages.stream()
                .map(legacy::serialize)
                .filter(messageText -> messageText.contains(expectedMessage))
                .count();
        // Assert that the number of occurrences matches the expectedOccurrences
        assertEquals(expectedOccurrences,
                actualOccurrences, "Expected message occurrence mismatch: " + expectedMessage);
    }
    /**
     * Get the explode event
     * @param entity - entity
     * @param l - location
     * @param list - list of blocks
     * @return EntityExplodeEvent
     */
    public EntityExplodeEvent getExplodeEvent(Entity entity, Location l, List<Block> list) {
        return new EntityExplodeEvent(entity, l, list, 0, null);
    }
    public PlayerDeathEvent getPlayerDeathEvent(Player player, List<ItemStack> drops, int droppedExp, int newExp,
            int newTotalExp, int newLevel, @Nullable String deathMessage) {
        //Technically this null is not allowed, but it works right now
        return new PlayerDeathEvent(player, null, drops, droppedExp, newExp,
                newTotalExp, newLevel, deathMessage);
    }
}
