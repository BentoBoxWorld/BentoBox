package world.bentobox.bentobox.hooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.Nullable;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.util.DataKey;
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

    public CitizensHook() {
        super("Citizens", Material.PLAYER_HEAD);
    }

    public static DataKey toDataKey(String serializedData) {
        DataKey dataKey = new MemoryDataKey();
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(serializedData);
        } catch (InvalidConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        insertIntoDataKey(dataKey, "", y);
        return dataKey;
    }

    private static void insertIntoDataKey(DataKey dataKey, String parentPath, ConfigurationSection config2) {
        // Manually set the keys and see where it breaks
        dataKey.setString("name", "bentest");
        dataKey.setString("uuid", UUID.randomUUID().toString());
        dataKey.setString("traits.type", "PLAYER");
        dataKey.setDouble("traits.location.bodyYaw", -111.14996);
        dataKey.setDouble("traits.location.x", 0.0);
        dataKey.setDouble("traits.location.y", 0.0);
        dataKey.setDouble("traits.location.z", 0.0);
        dataKey.setDouble("traits.location.yaw", -111.15);
        dataKey.setDouble("traits.location.pitch", 42.0);
        dataKey.setBoolean("spawned", true);
        dataKey.setString("traitnames", "type,location,spawned");
        dataKey.setString("metadata.cached-skin-uuid", "f4579458-a42d-431c-bbec-f7183536f633");
        dataKey.setString("skintrait.signature",
                "QBxcn0hclFKgSvGXjivL5W6F43uPuUFQgrvumsIPZEpdzR+LKXgl+OCfdoFCvjF303mDcpvMPcAJ9XEcbA/JLbeGkcfUupw326vjsz422lABA8Uys8yR/3lKD+KXfmvtpqiuOphMLvE21vVZQb0uP9g+1XgFO6puttcB3vGmenIM7jFE3uyQ8ma44VMqv/QBz8RHCw6jn+HsuIqS/VBQ/wv+/FVDOYd+qq4nbIXEyfZK/mvRlq4+AaTskxL4N6OkKqb1mREvmyZLbjFpoWTAmnPUHpUqc3yuky+v63mUpah7uEGwfO3FymBkSrAxgBSs1rASst9nS8M3icEBft+ea+roYBH1DLz4QNDKSIINFcpejPWHzLkCY20EW0Dn0Eaam5+Ps16aBPQ55bFX+ztrqSrRuVsFB0SuyxpXu6tA7OF28umJ+tn8345HxyjGvV84gjwzwAn+FrBrskxnSwPxnIHffht1W0m00e+8+ykKla2/J//66A352TZEIkVIfrKh8X7x/A6Y1UGItSDaOEA51Dna1OMZzsYJ7u0cBc4k7XIzJtGucVoV9tMzvhq3vmyTVwD6GEEMtPXhl3jXmklkAB4MIS1Of49tva2/KwPndmOXn2kFMEMzADtpOapIjCtR1y2uWmG9QWSyRY0bEma9dkCgZcPj56xoxcGHwaznm6s=");
        dataKey.setString("skintrait.textureRaw",
                "ewogICJ0aW1lc3RhbXAiIDogMTczMjgyMDQ1ODU0MCwKICAicHJvZmlsZUlkIiA6ICJmNDU3OTQ1OGE0MmQ0MzFjYmJlY2Y3MTgzNTM2ZjYzMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJCZW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2FiODM4NThlYmM4ZWU4NWMzZTU0YWIxM2FhYmZjYzFlZjJhZDQ0NmQ2YTkwMGU0NzFjM2YzM2I3ODkwNmE1YiIKICAgIH0KICB9Cn0=");

        /*
         * metadata:
        cached-skin-uuid: f4579458-a42d-431c-bbec-f7183536f633
        cached-skin-uuid-name: ben
        name: ben
        uuid: 93837773-5f4b-4337-8f2f-96a85c9665eb
        traits:
        type: PLAYER
        location:
        bodyYaw: -111.14996
        world: bskyblock_world
        x: -1.2024
        y: 85.0
        z: -1.2274
        yaw: -111.15
        pitch: 42.0
        spawned: true
        skintrait:
        signature: QBxcn0hclFKgSvGXjivL5W6F43uPuUFQgrvumsIPZEpdzR+LKXgl+OCfdoFCvjF303mDcpvMPcAJ9XEcbA/JLbeGkcfUupw326vjsz422lABA8Uys8yR/3lKD+KXfmvtpqiuOphMLvE21vVZQb0uP9g+1XgFO6puttcB3vGmenIM7jFE3uyQ8ma44VMqv/QBz8RHCw6jn+HsuIqS/VBQ/wv+/FVDOYd+qq4nbIXEyfZK/mvRlq4+AaTskxL4N6OkKqb1mREvmyZLbjFpoWTAmnPUHpUqc3yuky+v63mUpah7uEGwfO3FymBkSrAxgBSs1rASst9nS8M3icEBft+ea+roYBH1DLz4QNDKSIINFcpejPWHzLkCY20EW0Dn0Eaam5+Ps16aBPQ55bFX+ztrqSrRuVsFB0SuyxpXu6tA7OF28umJ+tn8345HxyjGvV84gjwzwAn+FrBrskxnSwPxnIHffht1W0m00e+8+ykKla2/J//66A352TZEIkVIfrKh8X7x/A6Y1UGItSDaOEA51Dna1OMZzsYJ7u0cBc4k7XIzJtGucVoV9tMzvhq3vmyTVwD6GEEMtPXhl3jXmklkAB4MIS1Of49tva2/KwPndmOXn2kFMEMzADtpOapIjCtR1y2uWmG9QWSyRY0bEma9dkCgZcPj56xoxcGHwaznm6s=
        textureRaw: ewogICJ0aW1lc3RhbXAiIDogMTczMjgyMDQ1ODU0MCwKICAicHJvZmlsZUlkIiA6ICJmNDU3OTQ1OGE0MmQ0MzFjYmJlY2Y3MTgzNTM2ZjYzMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJCZW4iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvN2FiODM4NThlYmM4ZWU4NWMzZTU0YWIxM2FhYmZjYzFlZjJhZDQ0NmQ2YTkwMGU0NzFjM2YzM2I3ODkwNmE1YiIKICAgIH0KICB9Cn0=
        owner:
        uuid: 36b2293a-ac0a-4f73-bc35-b12af97bee2b
        traitnames: type,scoreboardtrait,location,spawned,skintrait,inventory,owner
        navigator:
        speedmodifier: 1.0
        avoidwater: false
        usedefaultstuckaction: false
         */

        config2.getKeys(true).forEach(key -> {
            if (key.contains("metadatahdhdhdhdhhd")) {
                String path = parentPath.isEmpty() ? key : (parentPath + "." + key);
                Object value = config2.get(key);
                if (value instanceof MemorySection config) {
                    insertIntoDataKey(dataKey, path, config);
                } else {
                    BentoBox.getInstance().logDebug("Setting: " + path + " to " + value);
                    dataKey.setRaw(path, value);
                }
            }
        });
    }


    public String serializeNPC(NPC npc) {
        if (npc == null) {
            throw new IllegalArgumentException("NPC cannot be null.");
        }

        MemoryDataKey dataKey = new MemoryDataKey();
        npc.save(dataKey);
        // Convert MemoryDataKey to a YAML string
        YamlConfiguration yaml = new YamlConfiguration();
        for (Entry<String, Object> en : dataKey.getValuesDeep().entrySet()) {
            BentoBox.getInstance().logDebug("Serial key = " + en.getKey() + " = " + en.getValue());
            yaml.set(en.getKey(), en.getValue());
        }
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
        DataKey npcDataKey = toDataKey(serializedData);
        NPCRegistry registry = CitizensAPI.getNPCRegistry();
        try {
            String mobTypeName = npcDataKey.getString("traits.type", EntityType.PLAYER.name());
            String name = npcDataKey.getString("name", "");
            // TODO This does not take any (version-specific) mob type migrations into account (e.g.
            // for pig zombies). However, these migrations are currently also broken in Citizens
            // itself (SimpleNPCDataStore does not account for mob type migrations either).
            EntityType mobType = EntityType.valueOf(mobTypeName);
            NPC npc = registry.createNPC(mobType, name);
            npc.setBukkitEntityType(EntityType.valueOf(mobTypeName));
            npc.load(npcDataKey); // Load the serialized data into the NPC
            return npc.spawn(location);
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
