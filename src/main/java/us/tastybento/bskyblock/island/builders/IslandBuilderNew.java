package us.tastybento.bskyblock.island.builders;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Chest;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.localization.TextVariables;
import us.tastybento.bskyblock.api.user.User;
import us.tastybento.bskyblock.database.objects.Island;

/**
 * Generates islands
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandBuilderNew {

    public enum IslandType {
        ISLAND,
        NETHER,
        END
    }

    private Island island;
    private World world;
    private IslandType type = IslandType.ISLAND;
    private List<ItemStack> chestItems;
    private UUID playerUUID;
    private String playerName;
    private BSkyBlock plugin;
    private Map<IslandType, Clipboard> islandSchems = new EnumMap<>(IslandType.class);

    //TODO support companions?

    public IslandBuilderNew(BSkyBlock plugin, Island island) {
        this.plugin = plugin;
        this.island = island;
        world = island.getWorld();
        loadIslands();
    }

    private void loadIslands() {
        File schems = new File(plugin.getDataFolder(), "schems");
        if (!schems.exists()) {
            if (!schems.mkdirs()) {
                plugin.logError("Could not make schems folder!");
            } else {
                copySchems(schems);
            }            
        }
        
        try {
            Clipboard cb = new Clipboard(plugin);
            cb.load("island");
            islandSchems.put(IslandType.ISLAND, cb);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.logError("Could not load default island");
        }
        if (plugin.getSettings().isNetherGenerate() && plugin.getSettings().isNetherIslands()) {
            try {
                Clipboard cbn = new Clipboard(plugin);
                cbn.load("nether-island");
                islandSchems.put(IslandType.NETHER, cbn);
            } catch (IOException | InvalidConfigurationException e) {
                plugin.logError("Could not load default nether island");
            }
        }
        if (plugin.getSettings().isEndGenerate() && plugin.getSettings().isEndIslands()) {
            try {
                Clipboard cbe = new Clipboard(plugin);
                cbe.load("end-island");
                islandSchems.put(IslandType.END, cbe);
            } catch (IOException | InvalidConfigurationException e) {
                plugin.logError("Could not load default end island");
            }
        }
        plugin.log("Loaded " + islandSchems.size() + " islands");
    }

    /**
     * Copies schems from the jar file
     * @param schems2 - file containing schem
     */
    private void copySchems(File schems2) {
        plugin.saveResource("schems/island.schem", false);
        plugin.saveResource("schems/nether-island.schem", false);
        plugin.saveResource("schems/end-island.schem", false);
    }

    /**
     * @param type the type to set
     */
    public IslandBuilderNew setType(IslandType type) {
        this.type = type;
        return this;
    }

    /**
     * @param player - the player the player to set
     */
    public IslandBuilderNew setPlayer(Player player) {
        playerUUID = player.getUniqueId();
        playerName = player.getName();
        return this;
    }

    /**
     * @param list the default chestItems to set
     */
    public IslandBuilderNew setChestItems(List<ItemStack> list) {
        chestItems = list;
        return this;
    }

    public void build() {
        plugin.log("Pasting island to " + type);
        Location loc = island.getCenter();
        // Switch on island type
        switch (type) {
        case NETHER:
            world = Bukkit.getWorld(island.getWorld().getName() + "_nether");
            if (world == null) {
                return;
            }
            loc = island.getCenter().toVector().toLocation(world);
            break;
        case END:
            world = Bukkit.getWorld(island.getWorld().getName() + "_the_end");
            if (world == null) {
                return;
            }
            loc = island.getCenter().toVector().toLocation(world);
            break;
        default:
            break;
        }
        plugin.log("Pasting island to " + loc);
        islandSchems.get(type).paste(loc);
        // Do other stuff
    }

    private void placeSign(int x, int y, int z) {
        Block blockToChange = world.getBlockAt(x, y, z);
        blockToChange.setType(Material.SIGN_POST);
        if (playerUUID != null) {
            Sign sign = (Sign) blockToChange.getState();
            User user = User.getInstance(playerUUID);

            // Sets the lines of the sign
            sign.setLine(0, user.getTranslation("new-island.sign.line0", TextVariables.NAME, playerName));
            sign.setLine(1, user.getTranslation("new-island.sign.line1", TextVariables.NAME, playerName));
            sign.setLine(2, user.getTranslation("new-island.sign.line2", TextVariables.NAME, playerName));
            sign.setLine(3, user.getTranslation("new-island.sign.line3", TextVariables.NAME, playerName));

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


