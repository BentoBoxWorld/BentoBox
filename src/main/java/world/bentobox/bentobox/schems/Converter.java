package world.bentobox.bentobox.schems;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.eclipse.jdt.annotation.NonNull;

import world.bentobox.bentobox.blueprints.Blueprint;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintBlock;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintCreatureSpawner;
import world.bentobox.bentobox.blueprints.dataobjects.BlueprintEntity;

/**
 * This class converts a schem to a blueprint
 * @author tastybento
 * @since 1.5.0
 */
public class Converter {

    private static final String ATTACHED_YAML_PREFIX = "attached.";
    private static final String BEDROCK = "bedrock";
    private static final String BLOCKS_YAML_PREFIX = "blocks.";
    private static final String COLOR = "color";
    private static final String ENTITIES_YAML_PREFIX = "entities.";
    private static final String INVENTORY = "inventory";
    private static final String LINES = "lines";

    public Blueprint convert(@NonNull YamlConfiguration bc) {
        Blueprint bp = new Blueprint();
        // Bedrock
        if (bc.contains(BEDROCK)) {
            bp.setBedrock(getVector(bc.getString(BEDROCK)));
        }
        // Normal blocks
        if (bc.isConfigurationSection(BLOCKS_YAML_PREFIX)) {
            bp.setBlocks(bc.getConfigurationSection(BLOCKS_YAML_PREFIX).getKeys(false).stream()
                    // make configuration section from key
                    .map(k -> bc.getConfigurationSection(BLOCKS_YAML_PREFIX + k))
                    // Check the config section contains block data key "bd"
                    .filter(cs -> cs.contains("bd"))
                    // Convert it
                    .map(this::convertBlock)
                    // Collect into a map
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            // Legacy entities
            Map<Vector, List<BlueprintEntity>> le = bc.getConfigurationSection(BLOCKS_YAML_PREFIX).getKeys(false).stream()
                    // make configuration section from key
                    .map(k -> bc.getConfigurationSection(BLOCKS_YAML_PREFIX + k))
                    // Check the config section contains block data key "entities"
                    .filter(cs -> cs.contains("entity"))
                    // Convert it
                    .map(this::convertLegacyEntity)
                    // Collect into a map
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (le != null) {
                bp.setEntities(le);
            }
        }
        // Attached blocks
        if (bc.isConfigurationSection(ATTACHED_YAML_PREFIX)) {
            bp.setAttached(bc.getConfigurationSection(ATTACHED_YAML_PREFIX).getKeys(false).stream()
                    // make configuration section from key
                    .map(k -> bc.getConfigurationSection(ATTACHED_YAML_PREFIX + k))
                    // Check the config section contains block data key "bd"
                    .filter(cs -> cs.contains("bd"))
                    // Convert it
                    .map(this::convertBlock)
                    // Collect into a map
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        // Entities
        if (bc.isConfigurationSection(ENTITIES_YAML_PREFIX)) {
            bp.setEntities(bc.getConfigurationSection(ENTITIES_YAML_PREFIX).getKeys(false).stream()
                    // make configuration section from key
                    .map(k -> bc.getConfigurationSection(ENTITIES_YAML_PREFIX + k))
                    // Convert it
                    .map(this::convertEntity)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        return bp;

    }

    private Entry<Vector, List<BlueprintEntity>> convertLegacyEntity(ConfigurationSection config) {
        ConfigurationSection en = config.getConfigurationSection("entity");
        // Vector
        Vector vector = getVector(config.getName());
        // Create a list of entities at this position
        List<BlueprintEntity> list = en.getKeys(false).stream()
                .map(en::getConfigurationSection)
                .map(this::createEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new AbstractMap.SimpleEntry<>(vector, list);

    }

    private Entry<Vector, BlueprintBlock> convertBlock(ConfigurationSection config) {
        String blockData = config.getString("bd");
        // Make block
        BlueprintBlock block = new BlueprintBlock(blockData);
        // Signs
        if (config.contains(LINES)) {
            block.setSignLines(config.getStringList(LINES));
        }
        // Chests, in general
        if (config.isConfigurationSection(INVENTORY)) {
            ConfigurationSection inv = config.getConfigurationSection(INVENTORY);
            block.setInventory(
                    inv.getKeys(false).stream()
                    .collect(Collectors.toMap(Integer::valueOf, i -> (ItemStack)inv.get(i)))
                    );
        }
        // Mob spawners
        if (blockData.equals("minecraft:spawner")) {
            BlueprintCreatureSpawner spawner = new BlueprintCreatureSpawner();
            spawner.setSpawnedType(EntityType.valueOf(config.getString("spawnedType", "PIG")));
            spawner.setMaxNearbyEntities(config.getInt("maxNearbyEntities", 16));
            spawner.setMaxSpawnDelay(config.getInt("maxSpawnDelay", 2*60*20));
            spawner.setMinSpawnDelay(config.getInt("minSpawnDelay", 5*20));
            spawner.setDelay(config.getInt("delay", -1));
            spawner.setRequiredPlayerRange(config.getInt("requiredPlayerRange", 16));
            spawner.setSpawnRange(config.getInt("spawnRange", 4));
            block.setCreatureSpawner(spawner);
        }

        // Vector
        Vector vector = getVector(config.getName());

        // Return entry
        return new AbstractMap.SimpleEntry<>(vector, block);

    }

    private Entry<Vector, List<BlueprintEntity>> convertEntity(ConfigurationSection en) {
        // Position
        Vector vector = getVector(en.getName());
        // Create a list of entities at this position
        List<BlueprintEntity> list = en.getKeys(false).stream()
                .map(en::getConfigurationSection)
                .map(this::createEntity)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new AbstractMap.SimpleEntry<>(vector, list);

    }


    /**
     * Try to create a blueprint entity
     * @param cs - yaml configuration section
     * @return blueprint entity, or null if it fails
     */
    private BlueprintEntity createEntity(ConfigurationSection cs) {
        try {
            BlueprintEntity be = new BlueprintEntity();
            if (cs.contains("type")) {
                be.setType(EntityType.valueOf(cs.getString("type")));
            }
            if (cs.contains("name")) {
                be.setCustomName(cs.getString("name"));
            }
            if (cs.contains(COLOR)) {
                be.setColor(DyeColor.valueOf(cs.getString(COLOR)));
            }
            if (cs.contains("tamed")) {
                be.setTamed(cs.getBoolean("tamed"));
            }
            if (cs.contains("chest")) {
                be.setChest(cs.getBoolean("chest"));
            }
            if (!cs.getBoolean("adult")) {
                be.setAdult(false);
            }
            if (cs.contains("style")) {
                be.setStyle(Horse.Style.valueOf(cs.getString("style", "NONE")));
            }
            if (cs.contains("domestication")) {
                be.setDomestication(cs.getInt("domestication"));
            }
            if (cs.isConfigurationSection(INVENTORY)) {
                ConfigurationSection inv = cs.getConfigurationSection(INVENTORY);
                be.setInventory(inv.getKeys(false).stream()
                        .collect(Collectors.toMap(Integer::valueOf, i ->  (ItemStack)inv.get(i))));
            }
            return be;
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to import entity, skipping...");
        }
        return null;
    }

    private Vector getVector(String name) {
        String[] pos = name.split(",");
        int x = Integer.valueOf(pos[0]);
        int y = Integer.valueOf(pos[1]);
        int z = Integer.valueOf(pos[2]);
        return new Vector(x,y,z);
    }

}
