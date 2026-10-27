package world.bentobox.bentobox.listeners.flags.protection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.inventory.meta.SkullMeta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import world.bentobox.bentobox.CommonTestSetup;
import world.bentobox.bentobox.Settings;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.lists.Flags;
import world.bentobox.bentobox.managers.FlagsManager;
import world.bentobox.bentobox.managers.IslandsManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;
import world.bentobox.bentobox.util.Util;

class FireListenerTest extends CommonTestSetup {

    private final Map<String, Boolean> worldFlags = new HashMap<>();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        worldFlags.clear();

        SkullMeta skullMeta = mock(SkullMeta.class);
        when(itemFactory.getItemMeta(any())).thenReturn(skullMeta);
        when(Bukkit.getItemFactory()).thenReturn(itemFactory);
        Mockito.mockStatic(Flags.class);

        FlagsManager flagsManager = new FlagsManager(plugin);
        when(plugin.getFlagsManager()).thenReturn(flagsManager);


        // Worlds
        when(iwm.inWorld(any(World.class))).thenReturn(true);
        when(iwm.inWorld(any(Location.class))).thenReturn(true);

        // Monsters and animals
        Zombie zombie = mock(Zombie.class);
        when(zombie.getLocation()).thenReturn(location);
        Slime slime = mock(Slime.class);
        when(slime.getLocation()).thenReturn(location);
        Cow cow = mock(Cow.class);
        when(cow.getLocation()).thenReturn(location);

        // Fake players
        Settings settings = mock(Settings.class);
        Mockito.when(plugin.getSettings()).thenReturn(settings);
        Mockito.when(settings.getFakePlayers()).thenReturn(new HashSet<>());

        // Users
        User.setPlugin(plugin);

        // Locales - final
        LocalesManager lm = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(lm);
        when(lm.get(any(), any())).thenReturn("mock translation");

        // Player name
        PlayersManager pm = mock(PlayersManager.class);
        when(pm.getName(any())).thenReturn("tastybento");
        when(plugin.getPlayers()).thenReturn(pm);

        // World Settings
        WorldSettings ws = mock(WorldSettings.class);
        when(iwm.getWorldSettings(any())).thenReturn(ws);

        when(ws.getWorldFlags()).thenReturn(worldFlags);
        GameModeAddon gma = mock(GameModeAddon.class);
        Optional<GameModeAddon> opGma = Optional.of(gma );
        when(iwm.getAddon(any())).thenReturn(opGma);

       mockedUtil.when(() -> Util.getWorld(any())).thenReturn(mock(World.class));
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void testCheckFire() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // Block on fire
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBurnEvent e = new BlockBurnEvent(block, block);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);

        // Disallow fire
        when(island.isAllowed(any())).thenReturn(false);
        when(island.isAllowed(any(), any())).thenReturn(false);
        Flags.FLINT_AND_STEEL.setDefaultSetting(false);
        assertTrue(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
        Flags.FLINT_AND_STEEL.setDefaultSetting(true);
        assertTrue(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));

        // Allow fire
        when(island.isAllowed(any())).thenReturn(true);
        when(island.isAllowed(any(), any())).thenReturn(true);
        Flags.FLINT_AND_STEEL.setDefaultSetting(false);
        assertFalse(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
        Flags.FLINT_AND_STEEL.setDefaultSetting(true);
        assertFalse(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));

        // Check with no island
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        // Fire is not allowed, so should be cancelled
        Flags.FLINT_AND_STEEL.setDefaultSetting(false);
        assertTrue(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
        // Fire allowed
        Flags.FLINT_AND_STEEL.setDefaultSetting(world, true);
        assertFalse(listener.checkFire(e, location, Flags.FLINT_AND_STEEL));
    }

    @Test
    void testOnBlockBurn() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // Block on fire
        Block block = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        BlockBurnEvent e = new BlockBurnEvent(block, block);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);

        // Disallow fire
        when(island.isAllowed(any())).thenReturn(false);
        when(island.isAllowed(any(), any())).thenReturn(false);
        Flags.FIRE_BURNING.setDefaultSetting(false);
        listener.onBlockBurn(e);
        assertTrue(e.isCancelled());
        Flags.FIRE_BURNING.setDefaultSetting(true);
        listener.onBlockBurn(e);
        assertTrue(e.isCancelled());

        // Allow fire
        when(island.isAllowed(any())).thenReturn(true);
        when(island.isAllowed(any(), any())).thenReturn(true);
        Flags.FIRE_BURNING.setDefaultSetting(false);
        listener.onBlockBurn(e);
        assertFalse(e.isCancelled());
        Flags.FIRE_BURNING.setDefaultSetting(true);
        listener.onBlockBurn(e);
        assertFalse(e.isCancelled());

        // Check with no island
        e = new BlockBurnEvent(block, block);
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        // Fire is not allowed, so should be cancelled
        Flags.FIRE_BURNING.setDefaultSetting(false);
        listener.onBlockBurn(e);
        assertTrue(e.isCancelled());
        // Fire allowed
        Flags.FIRE_BURNING.setDefaultSetting(true);
        listener.onBlockBurn(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBlockSpread() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // Block on fire spread

        Block block = mock(Block.class);
        Block fire = mock(Block.class);
        when(block.getLocation()).thenReturn(location);
        when(fire.getLocation()).thenReturn(location);
        when(fire.getType()).thenReturn(Material.FIRE);

        BlockSpreadEvent e = new BlockSpreadEvent(block, fire, null);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);

        // Disallow fire
        when(island.isAllowed(any())).thenReturn(false);
        when(island.isAllowed(any(), any())).thenReturn(false);
        Flags.FIRE_SPREAD.setDefaultSetting(false);
        listener.onBlockSpread(e);
        assertTrue(e.isCancelled());
        Flags.FIRE_SPREAD.setDefaultSetting(true);
        listener.onBlockSpread(e);
        assertTrue(e.isCancelled());

        // Allow fire spread
        when(island.isAllowed(any())).thenReturn(true);
        when(island.isAllowed(any(), any())).thenReturn(true);
        Flags.FIRE_SPREAD.setDefaultSetting(false);
        listener.onBlockSpread(e);
        assertFalse(e.isCancelled());
        Flags.FIRE_SPREAD.setDefaultSetting(true);
        listener.onBlockSpread(e);
        assertFalse(e.isCancelled());

        // Check with no island
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        // Fire spread is not allowed, so should be cancelled
        Flags.FIRE_SPREAD.setDefaultSetting(false);
        listener.onBlockSpread(e);
        assertTrue(e.isCancelled());
        // Fire allowed
        Flags.FIRE_SPREAD.setDefaultSetting(true);
        listener.onBlockSpread(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testOnBlockIgnite() {
        // Island
        IslandsManager im = mock(IslandsManager.class);
        when(plugin.getIslands()).thenReturn(im);
        Island island = mock(Island.class);
        when(im.getIslandAt(any())).thenReturn(Optional.of(island));

        // Block on fire spread

        Block fireTarget = mock(Block.class);
        when(fireTarget.getLocation()).thenReturn(location);
        when(fireTarget.getType()).thenReturn(Material.AIR);
        Block below = mock(Block.class);
        when(below.getType()).thenReturn(Material.AIR);
        when(fireTarget.getRelative(BlockFace.DOWN)).thenReturn(below);
        // Every direction is open air: not a portal frame, so the flag check applies.
        when(fireTarget.getRelative(any(BlockFace.class), Mockito.anyInt())).thenReturn(below);

        BlockIgniteEvent e = new BlockIgniteEvent(fireTarget, null, (Block) null);

        // Fire listener - remember to set the plugin for testing!
        FireListener listener = new FireListener();
        listener.setPlugin(plugin);

        // Fire is checked at the target location regardless of the ignite cause. The mocked
        // island does not allow fire, so lighting must be cancelled even though the target is air.
        listener.onBlockIgnite(e);
        assertTrue(e.isCancelled());

        // Disallow fire
        when(island.isAllowed(any())).thenReturn(false);
        when(island.isAllowed(any(), any())).thenReturn(false);
        Flags.FIRE_IGNITE.setDefaultSetting(false);
        listener.onBlockIgnite(e);
        assertTrue(e.isCancelled());
        Flags.FIRE_IGNITE.setDefaultSetting(true);
        listener.onBlockIgnite(e);
        assertTrue(e.isCancelled());

        // Allow fire spread
        when(island.isAllowed(any())).thenReturn(true);
        when(island.isAllowed(any(), any())).thenReturn(true);
        Flags.FIRE_IGNITE.setDefaultSetting(false);
        listener.onBlockIgnite(e);
        assertFalse(e.isCancelled());
        Flags.FIRE_IGNITE.setDefaultSetting(true);
        listener.onBlockIgnite(e);
        assertFalse(e.isCancelled());

        // Check with no island
        when(im.getIslandAt(any())).thenReturn(Optional.empty());
        // Fire spread is not allowed, so should be cancelled
        Flags.FIRE_IGNITE.setDefaultSetting(false);
        listener.onBlockIgnite(e);
        assertTrue(e.isCancelled());
        // Fire allowed
        Flags.FIRE_IGNITE.setDefaultSetting(true);
        listener.onBlockIgnite(e);
        assertFalse(e.isCancelled());
    }

    @Test
    void testObsidianFloorIsNotAPortalFrame() {
        // Fire on an obsidian floor must still be checked against FIRE_IGNITE.
        assertFalse(FireListener.isPortalFrame(blockAt(Map.of("0,-1,0", Material.OBSIDIAN), 0, 0, 0)));
    }

    @Test
    void testPortalFrameIsRecognised() {
        Map<String, Material> grid = new HashMap<>();
        grid.put("0,-1,0", Material.OBSIDIAN);
        grid.put("1,-1,0", Material.OBSIDIAN);
        for (int y = 0; y < 3; y++) {
            grid.put("-1," + y + ",0", Material.OBSIDIAN);
            grid.put("2," + y + ",0", Material.OBSIDIAN);
        }
        grid.put("0,3,0", Material.OBSIDIAN);
        grid.put("1,3,0", Material.OBSIDIAN);
        grid.put("-1,3,0", Material.OBSIDIAN);
        grid.put("2,3,0", Material.OBSIDIAN);
        assertTrue(FireListener.isPortalFrame(blockAt(grid, 0, 0, 0)));
    }

    @Test
    void testTallPortalFrameIsRecognised() {
        // Interior height can be up to 21, not just 3.
        Map<String, Material> grid = new HashMap<>();
        grid.put("0,-1,0", Material.OBSIDIAN);
        grid.put("1,-1,0", Material.OBSIDIAN);
        for (int y = 0; y < 4; y++) {
            grid.put("-1," + y + ",0", Material.OBSIDIAN);
            grid.put("2," + y + ",0", Material.OBSIDIAN);
        }
        grid.put("0,4,0", Material.OBSIDIAN);
        grid.put("1,4,0", Material.OBSIDIAN);
        grid.put("-1,4,0", Material.OBSIDIAN);
        grid.put("2,4,0", Material.OBSIDIAN);
        assertTrue(FireListener.isPortalFrame(blockAt(grid, 0, 0, 0)));
    }

    @Test
    void testWidePortalFrameIsRecognised() {
        // A portal can be up to 21 wide; the fire may sit in the middle column.
        Map<String, Material> grid = new HashMap<>();
        for (int x = -1; x <= 3; x++) {
            grid.put(x + ",-1,0", Material.OBSIDIAN);
            grid.put(x + ",3,0", Material.OBSIDIAN);
        }
        for (int y = 0; y < 3; y++) {
            grid.put("-1," + y + ",0", Material.OBSIDIAN);
            grid.put("3," + y + ",0", Material.OBSIDIAN);
        }
        assertTrue(FireListener.isPortalFrame(blockAt(grid, 1, 0, 0)));
    }

    /** Builds a mock block whose relatives are looked up in a material grid. */
    private static Block blockAt(Map<String, Material> grid, int x, int y, int z) {
        Block block = mock(Block.class);
        when(block.getType()).thenAnswer(i -> grid.getOrDefault(x + "," + y + "," + z, Material.AIR));
        when(block.getRelative(any(BlockFace.class))).thenAnswer(i -> {
            BlockFace face = i.getArgument(0);
            return blockAt(grid, x + face.getModX(), y + face.getModY(), z + face.getModZ());
        });
        when(block.getRelative(any(BlockFace.class), Mockito.anyInt())).thenAnswer(i -> {
            BlockFace face = i.getArgument(0);
            int n = i.getArgument(1);
            return blockAt(grid, x + face.getModX() * n, y + face.getModY() * n, z + face.getModZ() * n);
        });
        return block;
    }
}
