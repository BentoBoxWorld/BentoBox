package us.tastybento.bskyblock.island.builders;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Chest;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.Constants;
import us.tastybento.bskyblock.Constants.GameType;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Fired when a team event happens.
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandBuilder {

    public enum IslandType {
        ISLAND,
        NETHER,
        END
    }

    private static final String PLAYER_PLACEHOLDER = "[player]";

    private Island island;
    private World world;
    private IslandType type = IslandType.ISLAND;
    private List<ItemStack> chestItems;
    private UUID playerUUID;
    private String playerName;

    //TODO support companions?

    public IslandBuilder(BSkyBlock plugin, Island island) {
        this.island = island;
        world = island.getWorld();
    }

    /**
     * @param type the type to set
     */
    public IslandBuilder setType(IslandType type) {
        this.type = type;
        return this;
    }

    /**
     * @param player - the player the player to set
     */
    public IslandBuilder setPlayer(Player player) {
        playerUUID = player.getUniqueId();
        playerName = player.getName();
        return this;
    }

    /**
     * @param list the default chestItems to set
     */
    public IslandBuilder setChestItems(List<ItemStack> list) {
        chestItems = list;
        return this;
    }

    public void build() {
        // Switch on island type
        if (type == IslandType.ISLAND) {
            world = island.getWorld();
            if (Constants.GAMETYPE == GameType.ACIDISLAND) {
                generateAcidIslandBlocks();
            } else {
                generateIslandBlocks();
            }
        } else if (type == IslandType.NETHER) {
            world = Bukkit.getWorld(island.getWorld().getName() + "_nether");
            if (world == null) {
                return;
            }
            generateNetherBlocks();
        } else if (type == IslandType.END) {
            world = Bukkit.getWorld(island.getWorld().getName() + "_the_end");
            if (world == null) {
                return;
            }
            generateEndBlocks();
        }
        // Do other stuff
    }

    /**
     * Creates the AcidIsland default island block by block
     */
    private void generateAcidIslandBlocks() {
        // AcidIsland
        // Build island layer by layer
        // Start from the base
        // half sandstone; half sand
        int x = island.getCenter().getBlockX();
        int z = island.getCenter().getBlockZ();
        int islandHeight = island.getCenter().getBlockY();

        int y = 0;
        for (int x_space = x - 4; x_space <= x + 4; x_space++) {
            for (int z_space = z - 4; z_space <= z + 4; z_space++) {
                Block b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.BEDROCK);
            }
        }
        for (y = 1; y < islandHeight + 5; y++) {
            for (int x_space = x - 4; x_space <= x + 4; x_space++) {
                for (int z_space = z - 4; z_space <= z + 4; z_space++) {
                    Block b = world.getBlockAt(x_space, y, z_space);
                    if (y < (islandHeight / 2)) {
                        b.setType(Material.SANDSTONE);
                    } else {
                        b.setType(Material.SAND);
                    }
                }
            }
        }
        // Then cut off the corners to make it round-ish
        for (y = 0; y < islandHeight + 5; y++) {
            for (int x_space = x - 4; x_space <= x + 4; x_space += 8) {
                for (int z_space = z - 4; z_space <= z + 4; z_space += 8) {
                    Block b = world.getBlockAt(x_space, y, z_space);
                    b.setType(Material.STATIONARY_WATER);
                }
            }
        }
        // Add some grass
        for (y = islandHeight + 4; y < islandHeight + 5; y++) {
            for (int x_space = x - 2; x_space <= x + 2; x_space++) {
                for (int z_space = z - 2; z_space <= z + 2; z_space++) {
                    Block blockToChange = world.getBlockAt(x_space, y, z_space);
                    blockToChange.setType(Material.GRASS);
                }
            }
        }
        // Place bedrock - MUST be there (ensures island are not
        // overwritten
        Block b = world.getBlockAt(x, islandHeight, z);
        b.setType(Material.BEDROCK);
        // Then add some more dirt in the classic shape
        y = islandHeight + 3;
        for (int x_space = x - 2; x_space <= x + 2; x_space++) {
            for (int z_space = z - 2; z_space <= z + 2; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.DIRT);
            }
        }
        b = world.getBlockAt(x - 3, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 3, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 3);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 3);
        b.setType(Material.DIRT);
        y = islandHeight + 2;
        for (int x_space = x - 1; x_space <= x + 1; x_space++) {
            for (int z_space = z - 1; z_space <= z + 1; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.DIRT);
            }
        }
        b = world.getBlockAt(x - 2, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 2, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 2);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 2);
        b.setType(Material.DIRT);
        y = islandHeight + 1;
        b = world.getBlockAt(x - 1, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 1, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 1);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 1);
        b.setType(Material.DIRT);

        // Add island items
        y = islandHeight;
        // Add tree (natural)
        Location treeLoc = new Location(world, x, y + 5D, z);
        world.generateTree(treeLoc, TreeType.ACACIA);

        // Place a helpful sign in front of player
        placeSign(x, islandHeight + 5, z + 3);
        // Place the chest - no need to use the safe spawn function
        // because we
        // know what this island looks like
        placeChest(x, islandHeight + 5, z + 1);
    }

    private void generateIslandBlocks() {
        // Skyblock
        // Build island layer by layer
        // Start from the base
        // half sandstone; half sand
        int x = island.getCenter().getBlockX();
        int z = island.getCenter().getBlockZ();
        int islandHeight = island.getCenter().getBlockY();

        World world = island.getCenter().getWorld();
        int y = 0;
        // Add some grass
        for (y = islandHeight + 4; y < islandHeight + 5; y++) {
            for (int x_space = x - 3; x_space <= x + 3; x_space++) {
                for (int z_space = z - 3; z_space <= z + 3; z_space++) {
                    world.getBlockAt(x_space, y, z_space).setType(Material.GRASS);
                }
            }
        }

        // Then cut off the corners to make it round-ish
        for (int x_space = x - 3; x_space <= x + 3; x_space += 6) {
            for (int z_space = z - 3; z_space <= z + 3; z_space += 6) {
                world.getBlockAt(x_space, y-1, z_space).setType(Material.AIR);
            }
        }
        // Place bedrock - MUST be there (ensures island are not
        // overwritten
        Block b = world.getBlockAt(x, islandHeight, z);
        b.setType(Material.BEDROCK);
        // Then add some more dirt in the classic shape
        y = islandHeight + 3;
        for (int x_space = x - 2; x_space <= x + 2; x_space++) {
            for (int z_space = z - 2; z_space <= z + 2; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.DIRT);
            }
        }
        b = world.getBlockAt(x - 3, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 3, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 3);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 3);
        b.setType(Material.DIRT);
        y = islandHeight + 2;
        for (int x_space = x - 1; x_space <= x + 1; x_space++) {
            for (int z_space = z - 1; z_space <= z + 1; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.DIRT);
            }
        }
        b = world.getBlockAt(x - 2, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 2, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 2);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 2);
        b.setType(Material.DIRT);
        y = islandHeight + 1;
        b = world.getBlockAt(x - 1, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x + 1, y, z);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z - 1);
        b.setType(Material.DIRT);
        b = world.getBlockAt(x, y, z + 1);
        b.setType(Material.DIRT);

        // Add island items
        y = islandHeight;
        // Add tree (natural)
        Location treeLoc = new Location(world, x, y + 5D, z);
        world.generateTree(treeLoc, TreeType.TREE);

        // Place a helpful sign in front of player
        placeSign(x, islandHeight + 5, z + 3);
        // Place the chest - no need to use the safe spawn function
        // because we
        // know what this island looks like
        placeChest(x, islandHeight + 5, z + 1);
    }

    private void generateNetherBlocks() {
        // Nether block
        int x = island.getCenter().getBlockX();
        int z = island.getCenter().getBlockZ();
        int islandHeight = island.getCenter().getBlockY();

        int y = 0;
        for (y = islandHeight + 4; y < islandHeight + 5; y++) {
            for (int x_space = x - 3; x_space <= x + 3; x_space++) {
                for (int z_space = z - 3; z_space <= z + 3; z_space++) {
                    world.getBlockAt(x_space, y, z_space).setType(Material.NETHER_BRICK);
                }
            }
        }
        // Then cut off the corners to make it round-ish
        for (int x_space = x - 3; x_space <= x + 3; x_space += 6) {
            for (int z_space = z - 3; z_space <= z + 3; z_space += 6) {
                world.getBlockAt(x_space, y-1, z_space).setType(Material.AIR);
            }
        }
        // Place bedrock - MUST be there (ensures island are not
        // overwritten
        Block b = world.getBlockAt(x, islandHeight, z);
        b.setType(Material.BEDROCK);
        // Then add some more dirt in the classic shape
        y = islandHeight + 3;
        for (int x_space = x - 2; x_space <= x + 2; x_space++) {
            for (int z_space = z - 2; z_space <= z + 2; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.NETHERRACK);
            }
        }
        b = world.getBlockAt(x - 3, y, z);
        b.setType(Material.SOUL_SAND);
        b = world.getBlockAt(x + 3, y, z);
        b.setType(Material.SOUL_SAND);
        b = world.getBlockAt(x, y, z - 3);
        b.setType(Material.SOUL_SAND);
        b = world.getBlockAt(x, y, z + 3);
        b.setType(Material.SOUL_SAND);
        y = islandHeight + 2;
        for (int x_space = x - 1; x_space <= x + 1; x_space++) {
            for (int z_space = z - 1; z_space <= z + 1; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.GRAVEL);
            }
        }
        b = world.getBlockAt(x - 2, y, z);
        b.setType(Material.QUARTZ_ORE);
        b = world.getBlockAt(x + 2, y, z);
        b.setType(Material.QUARTZ_ORE);
        b = world.getBlockAt(x, y, z - 2);
        b.setType(Material.QUARTZ_ORE);
        b = world.getBlockAt(x, y, z + 2);
        b.setType(Material.QUARTZ_ORE);
        y = islandHeight + 1;
        b = world.getBlockAt(x - 1, y, z);
        b.setType(Material.MAGMA);
        b = world.getBlockAt(x + 1, y, z);
        b.setType(Material.MAGMA);
        b = world.getBlockAt(x, y, z - 1);
        b.setType(Material.MAGMA);
        b = world.getBlockAt(x, y, z + 1);
        b.setType(Material.MAGMA);

        // Place a helpful sign in front of player
        placeSign(x, islandHeight + 5, z + 3);
        // Place the chest - no need to use the safe spawn function
        // because we know what this island looks like
        placeChest(x, islandHeight + 5, z + 1);
    }

    private void generateEndBlocks() {
        // Nether block
        int x = island.getCenter().getBlockX();
        int z = island.getCenter().getBlockZ();
        int islandHeight = island.getCenter().getBlockY();

        int y = 0;
        // Add some grass
        for (y = islandHeight + 4; y < islandHeight + 5; y++) {
            for (int x_space = x - 3; x_space <= x + 3; x_space++) {
                for (int z_space = z - 3; z_space <= z + 3; z_space++) {
                    world.getBlockAt(x_space, y, z_space).setType(Material.END_BRICKS);
                }
            }
        }
        // Then cut off the corners to make it round-ish
        for (int x_space = x - 3; x_space <= x + 3; x_space += 6) {
            for (int z_space = z - 3; z_space <= z + 3; z_space += 6) {
                world.getBlockAt(x_space, y-1, z_space).setType(Material.AIR);
            }
        }
        // Place bedrock - MUST be there (ensures island are not
        // overwritten
        Block b = world.getBlockAt(x, islandHeight, z);
        b.setType(Material.BEDROCK);
        // Then add some more dirt in the classic shape
        y = islandHeight + 3;
        for (int x_space = x - 2; x_space <= x + 2; x_space++) {
            for (int z_space = z - 2; z_space <= z + 2; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.ENDER_STONE);
            }
        }
        b = world.getBlockAt(x - 3, y, z);
        b.setType(Material.OBSIDIAN);
        b = world.getBlockAt(x + 3, y, z);
        b.setType(Material.OBSIDIAN);
        b = world.getBlockAt(x, y, z - 3);
        b.setType(Material.OBSIDIAN);
        b = world.getBlockAt(x, y, z + 3);
        b.setType(Material.OBSIDIAN);
        y = islandHeight + 2;
        for (int x_space = x - 1; x_space <= x + 1; x_space++) {
            for (int z_space = z - 1; z_space <= z + 1; z_space++) {
                b = world.getBlockAt(x_space, y, z_space);
                b.setType(Material.ENDER_STONE);
            }
        }
        b = world.getBlockAt(x - 2, y, z);
        b.setType(Material.ENDER_STONE);
        b = world.getBlockAt(x + 2, y, z);
        b.setType(Material.ENDER_STONE);
        b = world.getBlockAt(x, y, z - 2);
        b.setType(Material.ENDER_STONE);
        b = world.getBlockAt(x, y, z + 2);
        b.setType(Material.ENDER_STONE);
        y = islandHeight + 1;
        b = world.getBlockAt(x - 1, y, z);
        b.setType(Material.ENDER_STONE);
        b = world.getBlockAt(x + 1, y, z);
        b.setType(Material.ENDER_STONE);
        b = world.getBlockAt(x, y, z - 1);
        b.setType(Material.ENDER_STONE);
        b = world.getBlockAt(x, y, z + 1);
        b.setType(Material.ENDER_STONE);

        // Add island items
        y = islandHeight;
        // Spawn an ender crystal
        world.spawnEntity(new Location(world, x, y + 5D, z), EntityType.ENDER_CRYSTAL);

        // Place a helpful sign in front of player
        placeSign(x, islandHeight + 5, z + 3);
        // Place the chest - no need to use the safe spawn function
        // because we know what this island looks like
        placeChest(x, islandHeight + 5, z + 1);
    }

    private void placeSign(int x, int y, int z) {
        Block blockToChange = world.getBlockAt(x, y, z);
        blockToChange.setType(Material.SIGN_POST);
        if (playerUUID != null) {
            Sign sign = (Sign) blockToChange.getState();
            User user = User.getInstance(playerUUID);

            // Sets the lines of the sign
            sign.setLine(0, user.getTranslation("new-island.sign.line0", PLAYER_PLACEHOLDER, playerName));
            sign.setLine(1, user.getTranslation("new-island.sign.line1", PLAYER_PLACEHOLDER, playerName));
            sign.setLine(2, user.getTranslation("new-island.sign.line2", PLAYER_PLACEHOLDER, playerName));
            sign.setLine(3, user.getTranslation("new-island.sign.line3", PLAYER_PLACEHOLDER, playerName));

            ((org.bukkit.material.Sign) sign.getData()).setFacingDirection(BlockFace.NORTH);
            sign.update();
        }
    }

    private void placeChest(int x, int y, int z) {
        // Fill the chest and orient it correctly
        Block blockToChange = world.getBlockAt(x, y, z);
        blockToChange.setType(Material.CHEST);
        BlockState state = blockToChange.getState();
        Chest chest = new Chest(BlockFace.SOUTH);
        state.setData(chest);
        state.update();
        if (!chestItems.isEmpty()) {
            InventoryHolder chestBlock = (InventoryHolder) state;
            for (ItemStack item: chestItems) {
                chestBlock.getInventory().addItem(item);
            }
        }
    }
}


