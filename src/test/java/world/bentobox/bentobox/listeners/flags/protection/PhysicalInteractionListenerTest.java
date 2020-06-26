package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
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
@PrepareForTest( {BentoBox.class, Flags.class, Util.class, Bukkit.class} )
public class PhysicalInteractionListenerTest {

    private Location location;
    private BentoBox plugin;
    private Notifier notifier;
    private Player player;
    private ItemStack item;
    private Block clickedBlock;

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

        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
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
        IslandWorldManager iwm = mock(IslandWorldManager.class);
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

        // Users
        User.setPlugin(plugin);


        // Locales - final

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

        // Player setup
        player = mock(Player.class);
        when(player.isOp()).thenReturn(false);
        when(player.getLocation()).thenReturn(location);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        User.getInstance(player);

        // Item and clicked block
        item = mock(ItemStack.class);
        clickedBlock = mock(Block.class);

        // Addon
        when(iwm.getAddon(any())).thenReturn(Optional.empty());

        // Util strip spaces
        when(Util.stripSpaceAfterColorCodes(anyString())).thenCallRealMethod();

        // Tags
        when(Tag.PRESSURE_PLATES.isTagged(any(Material.class))).thenReturn(true);
        when(Tag.WOODEN_BUTTONS.isTagged(any(Material.class))).thenReturn(true);
    }

    @After
    public void tearDown() {
        User.clearUsers();
        Mockito.framework().clearInlineMocks();
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractNotPhysical() {
        when(clickedBlock.getType()).thenReturn(Material.STONE);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.RIGHT_CLICK_AIR, item, clickedBlock, BlockFace.UP);
        new PhysicalInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractWrongMaterial() {
        when(clickedBlock.getType()).thenReturn(Material.STONE);
        when(Tag.PRESSURE_PLATES.isTagged(clickedBlock.getType())).thenReturn(false);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        new PhysicalInteractionListener().onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmland() {
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        assertEquals(Result.DENY, e.useItemInHand());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmlandOp() {
        when(player.isOp()).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractFarmlandPermission() {
        when(player.hasPermission(anyString())).thenReturn(true);
        when(clickedBlock.getType()).thenReturn(Material.FARMLAND);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.ALLOW, e.useInteractedBlock());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractTurtleEgg() {
        when(clickedBlock.getType()).thenReturn(Material.TURTLE_EGG);
        PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onPlayerInteract(e);
        assertEquals(Result.DENY, e.useInteractedBlock());
        assertEquals(Result.DENY, e.useItemInHand());
        verify(notifier).notify(any(), eq("protection.protected"));
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent)}.
     */
    @Test
    public void testOnPlayerInteractPressurePlate() {
        Arrays.stream(Material.values()).filter(m -> m.name().contains("PRESSURE_PLATE")).forEach(p -> {
            when(clickedBlock.getType()).thenReturn(p);
            PlayerInteractEvent e  = new PlayerInteractEvent(player, Action.PHYSICAL, item, clickedBlock, BlockFace.UP);
            PhysicalInteractionListener i = new PhysicalInteractionListener();
            i.onPlayerInteract(e);
            assertEquals(Result.DENY, e.useInteractedBlock());
            assertEquals(Result.DENY, e.useItemInHand());
        });
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitNotProjectile() {
        Entity entity = mock(Entity.class);
        Block block = mock(Block.class);
        EntityInteractEvent e = new EntityInteractEvent(entity, block);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileHit(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitProjectileBlockNull() {
        Projectile entity = mock(Projectile.class);
        ProjectileSource source = mock(Creeper.class);
        when(entity.getShooter()).thenReturn(source);
        Block block = null;
        EntityInteractEvent e = new EntityInteractEvent(entity, block);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileHit(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitProjectile() {
        Projectile entity = mock(Projectile.class);
        ProjectileSource source = mock(Creeper.class);
        when(entity.getShooter()).thenReturn(source);
        Block block = mock(Block.class);
        EntityInteractEvent e = new EntityInteractEvent(entity, block);
        PhysicalInteractionListener i = new PhysicalInteractionListener();
        i.onProjectileHit(e);
        assertFalse(e.isCancelled());
    }

    /**
     * Test method for {@link PhysicalInteractionListener#onProjectileHit(org.bukkit.event.entity.EntityInteractEvent)}.
     */
    @Test
    public void testOnProjectileHitProjectilePlayer() {
        Projectile entity = mock(Projectile.class);
        ProjectileSource source = player ;
        when(entity.getShooter()).thenReturn(source);
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        Arrays.stream(Material.values())
        .filter(m -> !m.name().contains("LEGACY"))
        .filter(m -> m.name().contains("PRESSURE_PLATE") || m.name().contains("BUTTON")).forEach(p -> {
            when(block.getType()).thenReturn(p);
            EntityInteractEvent e = new EntityInteractEvent(entity, block);
            PhysicalInteractionListener i = new PhysicalInteractionListener();
            i.onProjectileHit(e);
            assertTrue(p.name() +" failed", e.isCancelled());
        });
    }
}
