package us.tastybento.bskyblock.island.builders;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Sign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.api.localization.TextVariables;
import us.tastybento.bskyblock.database.objects.Island;
import us.tastybento.bskyblock.util.Util;

/**
 * Generates islands
 *
 * @author tastybento
 * @since 1.0
 */
public class IslandBuilder {

    private Island island;
    private World world;
    private Environment type = Environment.NORMAL;
    private UUID playerUUID;
    private String playerName;
    private BSkyBlock plugin;
    private Map<Environment, Clipboard> islandSchems = new EnumMap<>(Environment.class);
    private Location spawnPoint;
    private Runnable task;

    //TODO support companions?

    public IslandBuilder(BSkyBlock plugin, Island island) {
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
            islandSchems.put(Environment.NORMAL, cb);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.logError("Could not load default island");
        }
        if (plugin.getSettings().isNetherGenerate() && plugin.getSettings().isNetherIslands()) {
            try {
                Clipboard cbn = new Clipboard(plugin);
                cbn.load("nether-island");
                islandSchems.put(Environment.NETHER, cbn);
            } catch (IOException | InvalidConfigurationException e) {
                plugin.logError("Could not load default nether island");
            }
        }
        if (plugin.getSettings().isEndGenerate() && plugin.getSettings().isEndIslands()) {
            try {
                Clipboard cbe = new Clipboard(plugin);
                cbe.load("end-island");
                islandSchems.put(Environment.THE_END, cbe);
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
    public IslandBuilder setType(Environment type) {
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
     * The task to run when the island is built
     * @param task
     * @return IslandBuilder
     */
    public IslandBuilder run(Runnable task) {
        this.task = task;
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
        case THE_END:
            world = Bukkit.getWorld(island.getWorld().getName() + "_the_end");
            if (world == null) {
                return;
            }
            loc = island.getCenter().toVector().toLocation(world);
            break;
        default:
            break;
        }
        islandSchems.get(type).paste(loc);
        // Do other stuff
        // Handle signs - signs are attachable, so they are not there until 1 tick after pasting
        if (playerUUID != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> islandSchems.get(type).getSigns().forEach(this::writeSign), 2L);
        }
        if (task != null) {
            Bukkit.getScheduler().runTaskLater(plugin, task, 3L);
        }
    }

    private void writeSign(Location loc, List<String> lines) {
        Sign sign = (Sign) loc.getBlock().getState();
        org.bukkit.material.Sign s = (org.bukkit.material.Sign) sign.getData();
        lines.forEach(plugin::log);
        // Handle spawn sign
        if (!lines.isEmpty() && lines.get(0).equalsIgnoreCase(TextVariables.SPAWN_HERE)) {
            loc.getBlock().setType(Material.AIR);
            // Orient to face same direction as sign
            spawnPoint = new Location(loc.getWorld(), loc.getBlockX() + 0.5D, loc.getBlockY(),
                    loc.getBlockZ() + 0.5D, Util.blockFaceToFloat(s.getFacing().getOppositeFace()), 30F);
            return;
        }
        // Sub in player's name
        for (int i = 0 ; i < lines.size(); i++) {
            sign.setLine(i, lines.get(i).replace(TextVariables.NAME, playerName));
        }
        sign.update();
    }

    /**
     * @return the spawnPoint
     */
    public Optional<Location> getSpawnPoint() {
        return Optional.ofNullable(spawnPoint);
    }
}


