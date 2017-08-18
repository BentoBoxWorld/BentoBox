package us.tastybento.bskyblock.schematics;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

import us.tastybento.bskyblock.BSkyBlock;
import us.tastybento.bskyblock.config.Settings;
import us.tastybento.bskyblock.generators.IslandWorld;
import us.tastybento.bskyblock.util.VaultHelper;

public class SchematicsMgr {
    private BSkyBlock plugin;
    private static HashMap<String, Schematic> schematics = new HashMap<String, Schematic>();

    /**
     * Class to hold all schematics that are available
     * @param plugin
     */
    public SchematicsMgr(BSkyBlock plugin) {
        this.plugin = plugin;
        loadSchematics();
    }

    /**
     * Loads schematics. If the default
     * island is not included, it will be made
     */
    public void loadSchematics() {
        // Check if there is a schematic folder and make it if it does not exist
        File schematicFolder = new File(plugin.getDataFolder(), "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdir();
        }
        // Clear the schematic list that is kept in memory
        schematics.clear();
        // Load the default schematic if it exists
        // Set up the default schematic
        File schematicFile = new File(schematicFolder, "island.schematic");
        File netherFile = new File(schematicFolder, "nether.schematic");
        if (!schematicFile.exists()) {
            //plugin.getLogger().info("Default schematic does not exist...");
            // Only copy if the default exists
            if (plugin.getResource("schematics/island.schematic") != null) {
                plugin.getLogger().info("Default schematic does not exist, saving it...");
                plugin.saveResource("schematics/island.schematic", false);
                // Add it to schematics
                try {
                    schematics.put("default",new Schematic(plugin, schematicFile));
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not load default schematic!");
                    e.printStackTrace();
                }
                // If this is repeated later due to the schematic config, fine, it will only add info
            } else {
                // No islands.schematic in the jar, so just make the default using 
                // built-in island generation
                schematics.put("default",new Schematic(plugin));
            }
            plugin.getLogger().info("Loaded default nether schematic");
        } else {
            // It exists, so load it
            try {
                schematics.put("default",new Schematic(plugin, schematicFile));
                plugin.getLogger().info("Loaded default island schematic.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not load default schematic!");
                e.printStackTrace();
            }
        }
        // Add the nether default too
        if (!netherFile.exists()) {
            if (plugin.getResource("schematics/nether.schematic") != null) {
                plugin.saveResource("schematics/nether.schematic", false);

                // Add it to schematics
                try {
                    Schematic netherIsland = new Schematic(plugin, netherFile);
                    netherIsland.setVisible(false);
                    schematics.put("nether", netherIsland);
                    plugin.getLogger().info("Loaded default nether schematic.");
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not load default nether schematic!");
                    e.printStackTrace();
                }
            } else {
                plugin.getLogger().severe("Could not find default nether schematic!");
            }
        } else {
            // It exists, so load it
            try {
                Schematic netherIsland = new Schematic(plugin, netherFile);
                netherIsland.setVisible(false);
                schematics.put("nether", netherIsland);
                plugin.getLogger().info("Loaded default nether schematic.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not load default nether schematic!");
                e.printStackTrace();
            }
        }
        // Set up some basic settings
        if (schematics.containsKey("default")) {
            schematics.get("default").setName("Island");
            schematics.get("default").setDescription("");
            schematics.get("default").setPartnerName("nether");
            schematics.get("default").setBiome(Settings.defaultBiome);
            schematics.get("default").setIcon(Material.GRASS);
            if (Settings.chestItems.length == 0) {
                schematics.get("default").setUseDefaultChest(false);
            }
            schematics.get("default").setOrder(0);
        }
        if (schematics.containsKey("nether")) {
            schematics.get("nether").setName("NetherBlock Island");
            schematics.get("nether").setDescription("Nether Island");
            schematics.get("nether").setPartnerName("default");
            schematics.get("nether").setBiome(Biome.HELL);
            schematics.get("nether").setIcon(Material.NETHERRACK);
            schematics.get("nether").setVisible(false);
            schematics.get("nether").setPasteEntities(true);
            if (Settings.chestItems.length == 0) {
                schematics.get("nether").setUseDefaultChest(false);
            }
        }

        // TODO: Load other settings from config.yml
    }

    /**
     * Get schematic with name
     * @param name
     * @return schematic or null if it does not exist
     */
    public Schematic getSchematic(String name) {
        return schematics.get(name);
    }

    /**
     * List schematics this player can access. If @param ignoreNoPermission is true, then only
     * schematics with a specific permission set will be checked. I.e., no common schematics will
     * be returned (including the default one).
     * @param player
     * @param ignoreNoPermission
     * @return List of schematics this player can use based on their permission level
     */
    public List<Schematic> getSchematics(Player player, boolean ignoreNoPermission) {
        List<Schematic> result = new ArrayList<>();
        // Find out what schematics this player can choose from
        //Bukkit.getLogger().info("DEBUG: Checking schematics for " + player.getName());
        for (Schematic schematic : schematics.values()) {
            //Bukkit.getLogger().info("DEBUG: schematic name is '"+ schematic.getName() + "'");
            //Bukkit.getLogger().info("DEBUG: perm is " + schematic.getPerm());
            if ((!ignoreNoPermission && schematic.getPerm().isEmpty()) || VaultHelper.hasPerm(player, schematic.getPerm())) {
                //Bukkit.getLogger().info("DEBUG: player can use this schematic");
                // Only add if it's visible
                if (schematic.isVisible()) {
                    // Check if it's a nether island, but the nether is not enables
                    if (schematic.getBiome().equals(Biome.HELL)) {
                        if (Settings.netherGenerate && IslandWorld.getNetherWorld() != null) {
                            result.add(schematic);
                        }
                    } else {
                        result.add(schematic);
                    }
                }

            }
        }
        // Sort according to order
        Collections.sort(result, (s1, s2) -> (s2.getOrder() < s1.getOrder()) ? 1 : -1);
        return result;
    }

    public HashMap<String, Schematic> getAll() {
        return schematics;
    }

}
