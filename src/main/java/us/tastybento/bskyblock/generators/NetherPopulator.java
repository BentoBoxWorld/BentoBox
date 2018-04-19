package us.tastybento.bskyblock.generators;

import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * @author tastybento
 *         Populates the Nether with appropriate blocks
 *
 */
public class NetherPopulator extends BlockPopulator {

    @Override
    public void populate(World world, Random random, Chunk source) {
        // Rough check - convert spawners to Nether spawners
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < world.getMaxHeight(); y++) {
                for (int z = 0; z < 16; z++) {
                    Block b = source.getBlock(x, y, z);
                    if (b.getType().equals(Material.MOB_SPAWNER)) {
                        CreatureSpawner cs = (CreatureSpawner) b.getState();
                        switch (random.nextInt(3)) {
                        case 0:
                            cs.setSpawnedType(EntityType.BLAZE);
                            break;
                        case 1:
                            cs.setSpawnedType(EntityType.SKELETON);
                            break;
                        case 2:
                            cs.setSpawnedType(EntityType.MAGMA_CUBE);
                            break;
                        default:
                            cs.setSpawnedType(EntityType.BLAZE);
                        }
                    } else if (b.getType().equals(Material.OBSIDIAN)) {
                        b.setType(Material.CHEST);
                        Chest cs = (Chest) b.getState();
                        Inventory chestInv = cs.getInventory();
                        // Fill it with random goodies
                        /*
                         * 2 to 5 stacks of any of the following
                         * Diamonds 1 - 3 6.85% (5/73)
                         * Iron Ingots 1 - 5 6.85% (5/73)
                         * Gold Ingots 1 - 3 20.5% (15/73)
                         * Golden Sword 1 6.85% (5/73)
                         * Golden Chestplate 1 6.85% (5/73)
                         * Flint and Steel 1 6.85% (5/73)
                         * Nether Wart 3 - 7 6.85% (5/73)
                         * Saddle 1 13.7% (10/73)
                         * Golden Horse Armor 1 11.0% (8/73)
                         * Iron Horse Armor 1 6.85% (5/73)
                         * Diamond Horse Armor 1 4.11% (3/73)
                         * Obsidian 2 - 4 2.74% (2/73)
                         */
                        // Pick how many stacks
                        int numOfStacks = 2 + random.nextInt(3);
                        // Pick the stacks
                        for (int i = 0; i < numOfStacks; i++) {
                            // Pick a random inventory slot
                            int slot = random.nextInt(chestInv.getSize());
                            // Try a few times to find an empty slot (avoids an
                            // infinite loop potential)
                            for (int j = 0; j < chestInv.getSize(); j++) {
                                if (chestInv.getItem(slot) == null) {
                                    break;
                                }
                                slot = random.nextInt(chestInv.getSize());
                            }
                            int choice = random.nextInt(73);
                            if (choice < 5) {
                                chestInv.setItem(slot, new ItemStack(Material.DIAMOND, random.nextInt(2) + 1));
                            } else if (choice < 10) {
                                chestInv.setItem(slot, new ItemStack(Material.IRON_INGOT, random.nextInt(4) + 1));
                            } else if (choice < 25) {
                                chestInv.setItem(slot, new ItemStack(Material.GOLD_INGOT, random.nextInt(2) + 1));
                            } else if (choice < 30) {
                                chestInv.setItem(slot, new ItemStack(Material.GOLD_SWORD, 1));
                            } else if (choice < 35) {
                                chestInv.setItem(slot, new ItemStack(Material.GOLD_CHESTPLATE, 1));
                            } else if (choice < 40) {
                                chestInv.setItem(slot, new ItemStack(Material.FLINT_AND_STEEL, 1));
                            } else if (choice < 45) {
                                chestInv.setItem(slot, new ItemStack(Material.NETHER_STALK, random.nextInt(4) + 3));
                            } else if (choice < 55) {
                                chestInv.setItem(slot, new ItemStack(Material.SADDLE, 1));
                            } else if (choice < 63) {
                                chestInv.setItem(slot, new ItemStack(Material.GOLD_BARDING, 1));
                            } else if (choice < 68) {
                                chestInv.setItem(slot, new ItemStack(Material.IRON_BARDING, 1));
                            } else if (choice < 71) {
                                chestInv.setItem(slot, new ItemStack(Material.DIAMOND_BARDING, 1));
                            } else {
                                chestInv.setItem(slot, new ItemStack(Material.OBSIDIAN, random.nextInt(3) + 1));
                            }
                        }

                    } else if (b.getType().equals(Material.STONE)) {
                        b.setType(Material.QUARTZ_ORE);
                    } else if (b.getType().equals(Material.DIRT)) {
                        world.generateTree(source.getBlock(x, y + 1, z).getLocation(), TreeType.BROWN_MUSHROOM);
                        b.setType(Material.SOUL_SAND);
                    } else if (b.getType().equals(Material.SOUL_SAND) && b.getRelative(BlockFace.UP).getType().equals(Material.AIR)) {
                        if (random.nextInt(9) == 1) {
                            b.getRelative(BlockFace.UP).setType(Material.NETHER_WARTS);
                        }
                    }
                }
            }
        }
    }

}
