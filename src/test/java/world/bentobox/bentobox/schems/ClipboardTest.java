package world.bentobox.bentobox.schems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Bukkit.class})
public class ClipboardTest {

    private BentoBox plugin;
    private File schemFolder;
    private Location loc;
    private User user;
    private Location loc2;
    private Sheep sheep;
    private Horse horse;
    private Block block;
    private World world;
    private BukkitScheduler sched;

    @Before
    public void setUp() throws Exception {
        plugin = mock(BentoBox.class);
        schemFolder = mock(File.class);
        when(schemFolder.exists()).thenReturn(true);
        loc = mock(Location.class);
        world = mock(World.class);
        block = mock(Block.class);
        when(block.getType()).thenReturn(Material.GRASS);
        when(block.getLocation()).thenReturn(loc);
        
        BlockData bd = mock(BlockData.class);
        when(bd.getAsString()).thenReturn("Block_data");
        when(block.getBlockData()).thenReturn(bd);
        when(world.getBlockAt(Mockito.anyInt(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(block);
        when(loc.getWorld()).thenReturn(world);
        when(loc.getBlockX()).thenReturn(1);
        when(loc.getBlockY()).thenReturn(2);
        when(loc.getBlockZ()).thenReturn(3);
        when(loc.getBlock()).thenReturn(block);
        when(loc.toVector()).thenReturn(new Vector(1,2,3));
        
        loc2 = mock(Location.class);
        when(loc2.getWorld()).thenReturn(world);
        when(loc2.getBlockX()).thenReturn(2);
        when(loc2.getBlockY()).thenReturn(3);
        when(loc2.getBlockZ()).thenReturn(4);
        when(loc2.getBlock()).thenReturn(block);
        // Living entities
        
        List<LivingEntity> ents = new ArrayList<>();
        Pig pig = mock(Pig.class);
        Player player = mock(Player.class);
        Cow cow = mock(Cow.class);
        Creeper creeper = mock(Creeper.class);
        sheep = mock(Sheep.class);
        horse = mock(Horse.class);
        when(pig.getLocation()).thenReturn(loc);
        when(cow.getLocation()).thenReturn(loc);
        when(creeper.getLocation()).thenReturn(loc);
        when(player.getLocation()).thenReturn(loc);
        when(sheep.getLocation()).thenReturn(loc);
        when(horse.getLocation()).thenReturn(loc);
        
        when(pig.getType()).thenReturn(EntityType.PIG);
        when(player.getType()).thenReturn(EntityType.PLAYER);
        when(cow.getType()).thenReturn(EntityType.COW);
        when(creeper.getType()).thenReturn(EntityType.CREEPER);
        when(sheep.getType()).thenReturn(EntityType.SHEEP);
        when(sheep.getColor()).thenReturn(DyeColor.LIGHT_BLUE);
        when(horse.getType()).thenReturn(EntityType.HORSE);
        when(horse.getColor()).thenReturn(Color.CREAMY);
        when(horse.getStyle()).thenReturn(Style.BLACK_DOTS);

        HorseInventory inv = mock(HorseInventory.class);
        when(horse.getInventory()).thenReturn(inv);
        
        // UUIDs (I'm going to assume these will all be unique (prays to god of randomness)
        when(creeper.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(cow.getUniqueId()).thenReturn(UUID.randomUUID());
        when(pig.getUniqueId()).thenReturn(UUID.randomUUID());
        when(sheep.getUniqueId()).thenReturn(UUID.randomUUID());
        when(horse.getUniqueId()).thenReturn(UUID.randomUUID());

        ents.add(creeper);
        ents.add(player);
        ents.add(cow);
        ents.add(pig);
        ents.add(sheep);
        ents.add(horse);
        when(world.getLivingEntities()).thenReturn(ents);
        
        user = mock(User.class);
        User.setPlugin(plugin);
        when(user.getLocation()).thenReturn(loc);
        
        // Scheduler
        PowerMockito.mockStatic(Bukkit.class);
        sched = mock(BukkitScheduler.class);
        when(Bukkit.getScheduler()).thenReturn(sched);
        
    }

    @Test
    public void testClipboard() {
        when(schemFolder.exists()).thenReturn(false);
        new Clipboard(plugin, schemFolder); 
        Mockito.verify(schemFolder).mkdirs();

    }

    @Test
    public void testSetGetPos1() {
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        assertNull(cb.getPos1());
        cb.setPos1(loc);
        assertEquals(loc, cb.getPos1());
        assertNull(cb.getOrigin());
    }

    @Test
    public void testSetGetPos2() {
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        assertNull(cb.getPos2());
        cb.setPos2(loc);
        assertEquals(loc, cb.getPos2());
    }

    @Test
    public void testSetGetOrigin() {
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        assertNull(cb.getOrigin());
        cb.setOrigin(loc);
        assertEquals(loc, cb.getOrigin());
    }

    @Test
    public void testCopyNoPos1Pos2() {
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        cb.copy(user, false);
        Mockito.verify(user).sendMessage(Mockito.eq("commands.admin.schem.need-pos1-pos2"));
    }
    
    @Test
    public void testCopyNoPos2() {
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        cb.setPos1(loc);
        cb.copy(user, false);
        Mockito.verify(user).sendMessage(Mockito.eq("commands.admin.schem.need-pos1-pos2"));
    }
    
    @Test
    public void testCopy() {
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        cb.setPos1(loc);
        cb.setPos2(loc2);
        cb.copy(user, false);
        Mockito.verify(user).sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, "8");
    }
    
    @Test
    public void testCopySigns() {
        when(block.getType()).thenReturn(Material.SIGN);
        Sign bs = mock(Sign.class);
        String[] lines = {"line1", "line2", "line3", "line4"};
        when(bs.getLines()).thenReturn(lines);
        when(block.getState()).thenReturn(bs);
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        cb.setPos1(loc);
        cb.setPos2(loc2);
        cb.copy(user, false);
        Mockito.verify(user).sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, "8");
        // Every block is a sign, so this should be called 8 times
        Mockito.verify(bs, Mockito.times(8)).getLines();
    }

    @Test
    public void testCopyChests() {
        when(block.getType()).thenReturn(Material.CHEST);
        Chest bs = mock(Chest.class);
        Inventory inv = mock(Inventory.class);
        ItemStack[] contents = { new ItemStack(Material.ACACIA_BOAT, 1), new ItemStack(Material.GLASS, 23)};
        when(inv.getContents()).thenReturn(contents);
        when(bs.getInventory()).thenReturn(inv);
        when(block.getState()).thenReturn(bs);
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        cb.setPos1(loc);
        cb.setPos2(loc2);
        cb.copy(user, false);
        Mockito.verify(user).sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, "8");
        // Every block is a sign, so this should be called 8 times
        Mockito.verify(bs, Mockito.times(8)).getInventory();
    }
    
    @Test
    public void testCopyCreatureSpawners() {
        when(block.getType()).thenReturn(Material.SPAWNER);
        CreatureSpawner bs = mock(CreatureSpawner.class);
        when(bs.getSpawnedType()).thenReturn(EntityType.CAVE_SPIDER);
        when(block.getState()).thenReturn(bs);
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        cb.setPos1(loc);
        cb.setPos2(loc2);
        cb.copy(user, false);
        Mockito.verify(user).sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, "8");
        // Every block is a sign, so this should be called 8 times
        Mockito.verify(bs, Mockito.times(8)).getMaxNearbyEntities();
    }
    
    @Test
    public void testCopyAir() {
        // No entities
        when(world.getLivingEntities()).thenReturn(new ArrayList<>());
        when(block.getType()).thenReturn(Material.AIR);
        BlockState bs = mock(BlockState.class);
        when(block.getState()).thenReturn(bs);
        Clipboard cb = new Clipboard(plugin, schemFolder); 
        cb.setPos1(loc);
        cb.setPos2(loc2);
        // Do not copy air
        cb.copy(user, false);
        Mockito.verify(user).sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, "0");
        cb.copy(user, true);
        Mockito.verify(user).sendMessage("commands.admin.schem.copied-blocks", TextVariables.NUMBER, "8");
    }

    
    @Test
    public void testPasteIslandNoData() {
        Clipboard cb = new Clipboard(plugin, schemFolder);
        Island island = mock(Island.class);
        when(island.getCenter()).thenReturn(loc);
        cb.pasteIsland(world, island, () -> {});
        Mockito.verify(plugin).logError(Mockito.eq("Clipboard has no block data in it to paste!"));
     // Verify the task is run
        Mockito.verify(sched).runTaskLater(Mockito.eq(plugin), Mockito.any(Runnable.class), Mockito.eq(2L));
    }
    
    @Test
    public void testPasteIslandWithData() {
        Clipboard cb = new Clipboard(plugin, schemFolder);
        Island island = mock(Island.class);
        when(island.getCenter()).thenReturn(loc);
        cb.setPos1(loc);
        cb.setPos2(loc2);
        cb.copy(user, false);
        cb.pasteIsland(world, island, () -> {});
        // This is set just once because the coords of the block are always the same
        Mockito.verify(block).setBlockData(Mockito.any());
        // Verify the entities are spawned
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.PIG));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.CREEPER));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.HORSE));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.SHEEP));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.COW));
        // Player should NOT spawn!!
        Mockito.verify(world, Mockito.never()).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.PLAYER));

        // Verify the task is run
        Mockito.verify(sched).runTaskLater(Mockito.eq(plugin), Mockito.any(Runnable.class), Mockito.eq(2L));
    }

    @Test
    public void testPasteClipboardNoData() {
        Clipboard cb = new Clipboard(plugin, schemFolder);
        cb.pasteClipboard(loc);
        Mockito.verify(plugin).logError(Mockito.eq("Clipboard has no block data in it to paste!"));
    }
    
    @Test
    public void testPasteClipboard() {
        Clipboard cb = new Clipboard(plugin, schemFolder);
        cb.setPos1(loc);
        cb.setPos2(loc2);
        cb.copy(user, false);
        cb.pasteClipboard(loc);
        // This is set just once because the coords of the block are always the same
        Mockito.verify(block).setBlockData(Mockito.any());
        // Verify the entities are spawned
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.PIG));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.CREEPER));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.HORSE));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.SHEEP));
        Mockito.verify(world).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.COW));
        // Player should NOT spawn!!
        Mockito.verify(world, Mockito.never()).spawnEntity(Mockito.eq(null), Mockito.eq(EntityType.PLAYER));
    }

    @Test
    public void testIsFull() {
        Clipboard cb = new Clipboard(plugin, schemFolder);
        assertFalse(cb.isFull());
        cb.setPos1(loc);
        cb.setPos2(loc2);
        cb.copy(user, false);
        assertTrue(cb.isFull());
    }

    /*
     * Will not test the file system methods
     */

}
