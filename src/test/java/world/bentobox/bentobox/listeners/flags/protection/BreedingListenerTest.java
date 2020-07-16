package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Vector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlaceholdersManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {Bukkit.class, BentoBox.class, Flags.class, Util.class} )
public class BreedingListenerTest {

    private Location location;
    private BentoBox plugin;
    private Notifier notifier;
    private Player player;
    private PlayerInventory inventory;
    private ItemStack itemInMainHand;
    private ItemStack itemInOffHand;
    private IslandWorldManager iwm;

    private static final EntityType ENTITY_TYPE = EntityType.COW;
    private static final Material BREEDABLE_WITH = Material.WHEAT;
    private static final Material NOT_BREEDABLE_WITH = Material.SEAGRASS;

    @Before
    public void setUp() {
        // Set up plugin
        plugin = mock(BentoBox.class);
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);

        Server server = mock(Server.class);
        World world = mock(World.class);
        when(server.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(server.getWorld("world")).thenReturn(world);
        when(server.getVersion()).thenReturn("BSB_Mocking");

        PluginManager pim = mock(PluginManager.class);

        ItemFactory itemFactory = mock(ItemFactory.class);
        when(server.getItemFactory()).thenReturn(itemFactory);

        PowerMockito.mockStatic(Bukkit.class);
        when(Bukkit.getServer()).thenReturn(server);
        when(Bukkit.getPluginManager()).thenReturn(pim);


        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        when(Bukkit.getLogger()).thenReturn(Logger.getAnonymousLogger());
        location = mock(Location.class);
        when(location.getWorld()).thenReturn(world);
        when(location.getBlockX()).thenReturn(0);
        when(location.getBlockY()).thenReturn(0);
        when(location.getBlockZ()).thenReturn(0);
        PowerMockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
        iwm = mock(IslandWorldManager.class);
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);
        when(plugin.getIWM()).thenReturn(iwm);

        // Monsters and animals
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        Cow cow = mock(Cow.class);
        when(cow.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        when(plugin.getSettings()).thenReturn(settings);
        when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // Player and player inventory. Start with nothing in hands
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        itemInMainHand = mock(ItemStack.class);
        when(itemInMainHand.getType()).thenReturn(Material.AIR);
        itemInOffHand = mock(ItemStack.class);
        when(itemInOffHand.getType()).thenReturn(Material.AIR);
        when(inventory.getItemInMainHand()).thenReturn(itemInMainHand);
        when(inventory.getItemInOffHand()).thenReturn(itemInOffHand);
        when(player.getInventory()).thenReturn(inventory);
        User.setPlugin(plugin);

        // Locales
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        Answer<String> answer = invocation -> (String)Arrays.asList(invocation.getArguments()).get(1);
        when(lm.get(any(), any())).thenAnswer(answer);

        // Placeholders
        PlaceholdersManager placeholdersManager = mock(PlaceholdersManager.class);
        when(plugin.getPlaceholdersManager()).thenReturn(placeholdersManager);
        when(placeholdersManager.replacePlaceholders(any(), any())).thenAnswer(answer);

        // Player name
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(any())).thenReturn("tastybento");
        when(plugin.getPlayers()).thenReturn(pm);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);
        Map<String, Boolean> worldFlags = new HashMap<>();
        when(ws.getWorldFlags()).thenReturn(worldFlags);

        // Island manager
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        Optional<Island> optional = Optional.of(island);
        when(im.getProtectedIslandAt(any())).thenReturn(optional);

        // Notifier
        notifier = mock(Notifier.class);
        when(plugin.getNotifier()).thenReturn(notifier);

        PowerMockito.mockStatic(Util.class);
        when(Util.getWorld(any())).thenReturn(mock(World.class));
        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

    }

    @After
    public void tearDown() {
        User.clearUsers();
        framework().clearInlineMocks();
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotAnimal() {
        Entity clickedEntity = mock(Entity.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        new BreedingListener().onPlayerInteract(e);
        assertFalse("Not animal failed", e.isCancelled());
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalNothingInMainHand() {
        Animals clickedEntity = mock(Animals.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        new BreedingListener().onPlayerInteract(e);
        assertFalse("Animal, nothing in main hand failed", e.isCancelled());
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalNothingInOffHand() {
        Animals clickedEntity = mock(Animals.class);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, EquipmentSlot.OFF_HAND);
        new BreedingListener().onPlayerInteract(e);
        assertFalse("Animal, nothing in off hand failed", e.isCancelled());
    }


    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInMainHandNotRightWorld() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(ENTITY_TYPE);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse("Animal, breeding item in main hand, wrong world failed " + breedingMat, e.isCancelled());

        // verify breeding was prevented
        verify(clickedEntity, never()).setBreed(false);
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInMainHand() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.COW);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertTrue("Animal, breeding item in main hand failed " + breedingMat, e.isCancelled());

        // verify breeding was prevented
        verify(clickedEntity).setBreed(false);
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInOffHandNotRightWorld() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(iwm.inWorld(any(World.class))).thenReturn(false);
        when(iwm.inWorld(any(Location.class))).thenReturn(false);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, EquipmentSlot.OFF_HAND);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;

        when(itemInOffHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse("Animal, breeding item in off hand, wrong world failed " + breedingMat, e.isCancelled());

        // verify breeding was not prevented
        verify(clickedEntity, never()).setBreed(false);
    }

    /**
     * Test method for {@link BreedingListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent)}.
     */
    @Test
    public void testOnPlayerInteractAnimalBreedingFoodInOffHand() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(ENTITY_TYPE);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position, EquipmentSlot.OFF_HAND);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = BREEDABLE_WITH;
        when(itemInOffHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertTrue("Animal, breeding item in off hand failed " + breedingMat, e.isCancelled());

        // verify breeding was prevented
        verify(clickedEntity).setBreed(false);
    }

    @Test
    public void testOnPlayerIntereactAnimalBreedingWrongFood() {
        Animals clickedEntity = mock(Animals.class);
        when(clickedEntity.getLocation()).thenReturn(location);
        when(clickedEntity.getType()).thenReturn(EntityType.COW);
        Vector position = new Vector(0,0,0);
        PlayerInteractAtEntityEvent e = new PlayerInteractAtEntityEvent(player, clickedEntity, position);
        BreedingListener bl = new BreedingListener();

        Material breedingMat = NOT_BREEDABLE_WITH;

        when(itemInMainHand.getType()).thenReturn(breedingMat);
        bl.onPlayerInteract(e);
        assertFalse("Animal, breeding item in main hand was prevented " + breedingMat, e.isCancelled());

        // verify breeding was not prevented
        verify(clickedEntity, never()).setBreed(false);
    }
}
