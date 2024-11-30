package world.bentobox.bentobox.hooks;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.util.MemoryDataKey;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.hooks.Hook;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * Provides copy and pasting of Citizens in blueprints
 *
 * @author tastybento
 * @since 3.1.0
 */
public class CitizensHook extends Hook {

    MemoryDataKey dataKeyTest = new MemoryDataKey();

    public CitizensHook() {
        super("Citizens", Material.PLAYER_HEAD);
    }

    public String serializeNPC(NPC npc) {
        if (npc == null) {
            throw new IllegalArgumentException("NPC cannot be null.");
        }
        MemoryDataKey dataKey = new MemoryDataKey();
        npc.save(dataKey); // Save NPC data into the MemoryDataKey
        // Convert MemoryDataKey to a YAML string
        YamlConfiguration yaml = new YamlConfiguration();
        for (Entry<String, Object> en : dataKey.getValuesDeep().entrySet()) {
            BentoBox.getInstance().logDebug("Serial key = " + en.getKey() + " = " + en.getValue());
            yaml.set(en.getKey(), en.getValue());
        }
        dataKeyTest = dataKey;
        return yaml.saveToString();
    }

    /**
     * Get a map of serialized Citizens that are in a set of locations
     * @param world world where this occurs
     * @param vectorsToCopy list of locations in that world as vectors
     * @param origin 
     * @return map
     */
    public Map<Vector, List<BlueprintEntity>> getCitizensInArea(World world, List<Vector> vectorsToCopy,
            @Nullable Vector origin) {
        Map<Vector, List<BlueprintEntity>> bpEntities = new HashMap<>();
        for (NPC npc : CitizensAPI.getNPCRegistry()) {
            if (npc.isSpawned()) {
                Location npcLocation = npc.getEntity().getLocation();
                Vector spot = new Vector(npcLocation.getBlockX(), npcLocation.getBlockY(), npcLocation.getBlockZ());
                if (npcLocation.getWorld().equals(world) && vectorsToCopy.contains(spot)) {
                    BlueprintEntity cit = new BlueprintEntity();
                    cit.setType(npc.getEntity().getType()); // Must be set to be pasted
                    cit.setCitizen(serializeNPC(npc));
                    // Retrieve or create the list, then add the entity
                    List<BlueprintEntity> entities = bpEntities.getOrDefault(spot, new ArrayList<>());
                    entities.add(cit);
                    // Create position
                    Vector origin2 = origin == null ? new Vector(0, 0, 0) : origin;
                    int x = spot.getBlockX() - origin2.getBlockX();
                    int y = spot.getBlockY() - origin2.getBlockY();
                    int z = spot.getBlockZ() - origin2.getBlockZ();
                    Vector pos = new Vector(x, y, z);
                    // Store
                    bpEntities.put(pos, entities); // Update the map
                }
            }
        }
        return bpEntities;
    }

    /**
     * Spawn a Citizen
     * @param serializedData serialized data
     * @param location location
     * @return true if spawn is successful
     */
    public boolean spawnCitizen(EntityType type, String serializedData, Location location) {
        BentoBox.getInstance().logDebug("spawn Citizen at " + location + " with this data " + serializedData);
        if (serializedData == null || location == null) {
            throw new IllegalArgumentException("Serialized data and location cannot be null.");
        }

        // Load the serialized data into a YamlConfiguration
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new StringReader(serializedData));

        // Create a new MemoryDataKey from the loaded data
        MemoryDataKey dataKey = new MemoryDataKey();
        for (String key : yaml.getKeys(true)) {
            BentoBox.getInstance().logDebug("data key " + key + " = " + yaml.get(key));
            if (key.equalsIgnoreCase("metadata") || key.equalsIgnoreCase("traits")
                    || key.equalsIgnoreCase("traits.owner") || key.equalsIgnoreCase("traits.location")
                    || key.equalsIgnoreCase("navigator")) {
                continue;
            }
            dataKey.setRaw(key, yaml.get(key));
        }

        // Get the NPC details from the serialized data
        String name = dataKey.getString("name");
        //String type = dataKey.getString("traits.type");

        BentoBox.getInstance().logDebug("Entity type = " + type + " name = " + name);
        if (type == null) {
            // No luck
            return false;
        }
        // Create a new NPC and load the data
        BentoBox.getInstance().logDebug("Create a new NPC and load the data");
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
            try {
                NPC npc = registry.createNPC(type, name);

                npc.load(dataKey); // Load the serialized data into the NPC
                for (Entry<String, Object> en : dataKey.getValuesDeep().entrySet()) {
                    BentoBox.getInstance().logDebug("loaded key " + en.getKey() + " = " + en.getValue());
                }
                boolean r = npc.spawn(location); // Spawn the NPC at the specified location
                BentoBox.getInstance().logDebug("Spawn = " + r);
                if (!r) {
                    npc.load(dataKeyTest);
                    BentoBox.getInstance().logDebug(npc.spawn(location)); // Spawn the NPC at the specified location
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return false;
    }


    @Override
    public boolean hook() {
        boolean hooked = this.isPluginAvailable();
        if (!hooked) {
            BentoBox.getInstance().logError("Could not hook into Citizens");
        }
        return hooked; // The hook process shouldn't fail
    }

    @Override
    public String getFailureCause() {
        return null; // The hook process shouldn't fail
    }
}
