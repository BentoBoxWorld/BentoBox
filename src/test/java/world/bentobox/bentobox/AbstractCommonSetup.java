package world.bentobox.bentobox;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
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
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.ImmutableSet;

import net.md_5.bungee.api.chat.TextComponent;
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
@RunWith(PowerMockRunner.class)
public abstract class AbstractCommonSetup {

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


    public void setUp() throws Exception {
        // Bukkit
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        when(Bukkit.getBukkitVersion()).thenReturn("");
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
        when(mockPlayer.getUniqueId()).thenReturn(uuid);
        when(mockPlayer.getLocation()).thenReturn(location);
        when(mockPlayer.getWorld()).thenReturn(world);
        when(mockPlayer.getName()).thenReturn("tastybento");
        when(mockPlayer.getInventory()).thenReturn(inv);
        when(mockPlayer.spigot()).thenReturn(spigot);

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
        when(mockPlayer.getMetadata(anyString())).thenReturn(Collections.singletonList(mdv));

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

        PowerMockito.mockStatic(Util.class, Mockito.CALLS_REAL_METHODS);
        when(Util.getWorld(any())).thenReturn(mock(World.class));

        // Util
        when(Util.findFirstMatchingEnum(any(), any())).thenCallRealMethod();
        // Util translate color codes (used in user translate methods)
        //when(Util.translateColorCodes(anyString())).thenAnswer((Answer<String>) invocation -> invocation.getArgument(0, String.class));

        // Tags
        for (Material m : Material.values()) {
            if (m.name().contains("_SIGN")) {
                when(Tag.ALL_SIGNS.isTagged(m)).thenReturn(true);
                when(Tag.SIGNS.isTagged(m)).thenReturn(true);
            }
            if (m.name().contains("_WALL_SIGN")) {
                when(Tag.WALL_SIGNS.isTagged(m)).thenReturn(true);
            }
            if (m.name().contains("_TRAPDOOR")) {
                when(Tag.TRAPDOORS.isTagged(m)).thenReturn(true);
            }
            if (m.name().contains("FENCE")) {
                when(Tag.FENCES.isTagged(m)).thenReturn(true);
            }
            if (m.name().contains("_DOOR")) {
                when(Tag.DOORS.isTagged(m)).thenReturn(true);
            }
            if (m.name().contains("_BOAT") || m.name().contains("_RAFT")) {
                when(Tag.ITEMS_BOATS.isTagged(m)).thenReturn(true);
            }

        }
    }

    /**
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Check that spigot sent the message
     * @param message - message to check
     */
    public void checkSpigotMessage(String expectedMessage) {
        checkSpigotMessage(expectedMessage, 1);
    }

    /*
    public void checkSpigotMessage(String expectedMessage, boolean shouldBePresent) {
        // Capture the argument passed to spigot().sendMessage(...) if messages are sent
        ArgumentCaptor<TextComponent> captor = ArgumentCaptor.forClass(TextComponent.class);
    
        if (shouldBePresent) {
            // If we expect a message to be present, verify that sendMessage() was called at least once
            verify(spigot, atLeastOnce()).sendMessage(captor.capture());
    
            // Get all captured TextComponents
            List<TextComponent> capturedMessages = captor.getAllValues();
    
            // Check if any captured message contains the expected text
            boolean messageFound = capturedMessages.stream()
                    .map(component -> component.toPlainText()) // Convert each TextComponent to plain text
                .anyMatch(messageText -> messageText.contains(expectedMessage));  // Check if the expected message is present
    
            // Assert that the message was found
            assertTrue("Expected message not found: " + expectedMessage, messageFound);
    
        } else {
            // If we expect no messages with this text, capture any sent messages to ensure none match the given message
            verify(spigot, atLeast(0)).sendMessage(captor.capture());
    
            // Get all captured TextComponents
            List<TextComponent> capturedMessages = captor.getAllValues();
    
            // Check that none of the captured messages contain the forbidden text
            boolean messageFound = capturedMessages.stream().map(component -> component.toPlainText()) // Convert each TextComponent to plain text
                    .anyMatch(messageText -> messageText.contains(expectedMessage)); // Check if the message is present
    
            // Assert that the message was NOT found
            assertFalse("Unexpected message found: " + expectedMessage, messageFound);
        }
    }*/

    public void checkSpigotMessage(String expectedMessage, int expectedOccurrences) {
        // Capture the argument passed to spigot().sendMessage(...) if messages are sent
        ArgumentCaptor<TextComponent> captor = ArgumentCaptor.forClass(TextComponent.class);

        // Verify that sendMessage() was called at least 0 times (capture any sent messages)
        verify(spigot, atLeast(0)).sendMessage(captor.capture());

        // Get all captured TextComponents
        List<TextComponent> capturedMessages = captor.getAllValues();

        // Count the number of occurrences of the expectedMessage in the captured messages
        long actualOccurrences = capturedMessages.stream().map(component -> component.toPlainText()) // Convert each TextComponent to plain text
                .filter(messageText -> messageText.contains(expectedMessage)) // Check if the message contains the expected text
                .count(); // Count how many times the expected message appears

        // Assert that the number of occurrences matches the expectedOccurrences
        assertEquals("Expected message occurrence mismatch: " + expectedMessage, expectedOccurrences,
                actualOccurrences);
    }

    /**
     * Get the explode event
     * @param entity
     * @param l
     * @param list
     * @return
     */
    public EntityExplodeEvent getExplodeEvent(Entity entity, Location l, List<Block> list) {
        //return new EntityExplodeEvent(entity, l, list, 0, null);
        return new EntityExplodeEvent(entity, l, list, 0);
    }

    public PlayerDeathEvent getPlayerDeathEvent(Player player, List<ItemStack> drops, int droppedExp, int newExp,
            int newTotalExp, int newLevel, @Nullable String deathMessage) {
        //return new PlayerDeathEvent(player, null, drops, droppedExp, newExp, newTotalExp, newLevel, deathMessage);
        return new PlayerDeathEvent(player, drops, droppedExp, newExp, newTotalExp, newLevel, deathMessage);
    }

}
