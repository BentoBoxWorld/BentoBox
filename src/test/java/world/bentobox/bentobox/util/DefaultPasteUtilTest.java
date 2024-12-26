package world.bentobox.bentobox.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.addons.AddonDescription;
import world.bentobox.bentobox.api.addons.GameModeAddon;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity.MythicMobRecord;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.hooks.MythicMobsHook;
import world.bentobox.bentobox.managers.HooksManager;
import world.bentobox.bentobox.managers.IslandWorldManager;
import world.bentobox.bentobox.managers.LocalesManager;
import world.bentobox.bentobox.managers.PlayersManager;

/**
 * @author tastybento
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ BentoBox.class, Bukkit.class })
public class DefaultPasteUtilTest {

    @Mock
    private BentoBox plugin;
    @Mock
    private Block block;
    @Mock
    private Island island;
    @Mock
    private BlueprintBlock bpSign;
    private Side side;
    @Mock
    private User user;
    @Mock
    private Player player;
    @Mock
    private IslandWorldManager iwm;
    @Mock
    private GameModeAddon addon;
    @Mock(extraInterfaces = {WallSign.class})
    BlockData wallSignData;
    @Mock(extraInterfaces = {org.bukkit.block.data.type.Sign.class})
    BlockData signData;
    @Mock(extraInterfaces = {org.bukkit.block.Sign.class})
    BlockState sign;

    @Mock
    private PlayersManager pm;
    @Mock
    private MythicMobsHook mythicMobsHook;
    @Mock
    private BlueprintEntity blueprintEntity;
    @Mock
    private Location location;
    @Mock
    private LivingEntity livingEntity;
    @Mock
    private World world;
    @Mock
    private HooksManager hooksManager;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Bukkit.class, Mockito.RETURNS_MOCKS);
        // Set up plugin
        Whitebox.setInternalState(BentoBox.class, "instance", plugin);
        AddonDescription desc = new AddonDescription.Builder("", "", "").build();
        when(addon.getDescription()).thenReturn(desc);
        when(plugin.getIWM()).thenReturn(iwm);
        when(iwm.getAddon(any())).thenReturn(Optional.of(addon));
        side = Side.FRONT;
        UUID uuid = UUID.randomUUID();
        when(player.getName()).thenReturn("username");
        when(player.getUniqueId()).thenReturn(uuid);
        when(island.getOwner()).thenReturn(uuid);
        User.getInstance(player);
        when(((WallSign)wallSignData).getFacing()).thenReturn(BlockFace.NORTH);
        when(((org.bukkit.block.data.type.Sign)signData).getRotation()).thenReturn(BlockFace.NORTH);

        when(pm.getName(any())).thenReturn("tastybento");
        LocalesManager localesManager = mock(LocalesManager.class);
        when(plugin.getLocalesManager()).thenReturn(localesManager);
        when(localesManager.getOrDefault(any(), anyString(), anyString())).thenReturn("translated");

        when(location.getWorld()).thenReturn(world);
        // Hooks
        when(hooksManager.getHook("MythicMobs")).thenReturn(Optional.of(mythicMobsHook));
        when(plugin.getHooks()).thenReturn(hooksManager);

        when(plugin.getPlayers()).thenReturn(pm);

        // Blueprint Entity
        when(blueprintEntity.getType()).thenReturn(EntityType.PLAYER);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        User.clearUsers();
    }

    @Test
    public void testWriteSignWithSpawnHere() {
        List<String> lines = Collections.singletonList(TextVariables.SPAWN_HERE);
        Mockito.when(bpSign.getSignLines(side)).thenReturn(lines);

        when(block.getBlockData()).thenReturn(wallSignData);
        when(block.getType()).thenReturn(Material.BAMBOO_WALL_SIGN);
        when(block.getWorld()).thenReturn(world);

        DefaultPasteUtil.writeSign(island, block, bpSign, side);

        verify(block).setType(Material.AIR);

        ArgumentCaptor<Location> spawnPointCaptor = ArgumentCaptor.forClass(Location.class);
        verify(island).setSpawnPoint(any(), spawnPointCaptor.capture());
        Location spawnPoint = spawnPointCaptor.getValue();

        Assert.assertEquals(block.getWorld(), spawnPoint.getWorld());
        Assert.assertEquals(block.getX() + 0.5D, spawnPoint.getX(), 0.001);
        Assert.assertEquals(block.getY(), spawnPoint.getY(), 0.001);
        Assert.assertEquals(block.getZ() + 0.5D, spawnPoint.getZ(), 0.001);
        Assert.assertEquals(Util.blockFaceToFloat(BlockFace.SOUTH), spawnPoint.getYaw(), 0.001);
        Assert.assertEquals(30F, spawnPoint.getPitch(), 0.001);
    }

    @Test
    public void testWriteSignWithStartText() {
        List<String> lines = Collections.singletonList(TextVariables.START_TEXT);
        when(bpSign.getSignLines(side)).thenReturn(lines);
        when(block.getState()).thenReturn(sign);
        when(((Sign) sign).getSide(side)).thenReturn(mock(SignSide.class));
        when(block.getType()).thenReturn(Material.BAMBOO_SIGN);
        when(block.getBlockData()).thenReturn(signData);

        DefaultPasteUtil.writeSign(island, block, bpSign, side);

        //verify(((Sign) sign), times(4)).setLine(anyInt(), anyString());
        verify(((Sign) sign).getSide(side), times(4)).setLine(anyInt(), anyString());

        ArgumentCaptor<String> lineCaptor = ArgumentCaptor.forClass(String.class);
        verify(((Sign) sign).getSide(side), times(4)).setLine(anyInt(), lineCaptor.capture());

        List<String> capturedLines = lineCaptor.getAllValues();
        Assert.assertEquals(Arrays.asList("translated", "translated", "translated", "translated"), capturedLines);
    }

    @Test
    public void testWriteSignWithoutSpecialText() {
        List<String> lines = Arrays.asList(TextVariables.START_TEXT, "Line 2", "Line 3", "Line 4");
        List<String> linesTranslated = Arrays.asList("translated", "translated", "translated", "translated");
        when(bpSign.getSignLines(side)).thenReturn(lines);

        when(block.getBlockData()).thenReturn(signData);
        when(block.getState()).thenReturn(sign);
        when(((Sign) sign).getSide(side)).thenReturn(mock(SignSide.class));
        when(block.getType()).thenReturn(Material.BAMBOO_SIGN);

        DefaultPasteUtil.writeSign(island, block, bpSign, side);

        verify(((Sign) sign).getSide(side), times(4)).setLine(anyInt(), anyString());

        ArgumentCaptor<String> lineCaptor = ArgumentCaptor.forClass(String.class);
        verify(((Sign) sign).getSide(side), times(4)).setLine(anyInt(), lineCaptor.capture());

        List<String> capturedLines = lineCaptor.getAllValues();
        Assert.assertEquals(linesTranslated, capturedLines);
    }

    @Ignore
    @Test
    public void testSpawnBlueprintEntity_WithMythicMobs() {
        // Set up conditions to satisfy the mythic mobs spawning logic
        MythicMobRecord mmr = new MythicMobRecord("string", "string2", 10D, 1F, "string3");
        when(blueprintEntity.getMythicMobsRecord()).thenReturn(mmr);
        when(mythicMobsHook.spawnMythicMob(mmr, location)).thenReturn(true);
        // This test works fine if there is a System.out.println() in the code. I assume some optimization is being done in compilation

        assertFalse(DefaultPasteUtil.spawnBlueprintEntity(blueprintEntity, location, island));

        // Verify the mythic mob was spawned, and the method returned early
        verify(mythicMobsHook).spawnMythicMob(mmr, location);
        verify(world, never()).spawnEntity(any(Location.class), any(EntityType.class));
    }

    @Test
    public void testSpawnBlueprintEntity_WithoutMythicMobs() {
        // Set up conditions where MythicMobs should not be spawned
        when(hooksManager.getHook("MythicMobs")).thenReturn(Optional.empty());

        assertTrue(DefaultPasteUtil.spawnBlueprintEntity(blueprintEntity, location, island));

        // Verify a regular entity was spawned instead
        verify(world).spawnEntity(location, blueprintEntity.getType());
    }

}
