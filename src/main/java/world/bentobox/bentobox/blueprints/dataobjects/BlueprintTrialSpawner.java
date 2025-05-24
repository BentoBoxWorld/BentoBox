package world.bentobox.bentobox.blueprints.dataobjects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.spawner.SpawnRule;
import org.bukkit.block.spawner.SpawnerEntry;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.loot.LootTable;
import org.bukkit.spawner.TrialSpawnerConfiguration;
import org.jetbrains.annotations.NotNull;

import com.google.gson.annotations.Expose;

/**
 * @author tastybento
 * @since 3.4.2
 */
public class BlueprintTrialSpawner {

    @Expose
    private boolean ominous;
    @Expose
    private EntityType spawnedType;
    @Expose
    private double addSimulEnts;
    @Expose
    private double addSpawnsB4Cool;
    @Expose
    private double baseSimEnts;
    @Expose
    private int delay;
    @Expose
    private @NotNull Map<LootTableSerial, Integer> lootTableMap = new HashMap<>();
    @Expose
    private int spawnRange;
    @Expose
    private int requiredPlayerRange;
    @Expose
    private int playerRange;
    @Expose
    private double baseSpawnsB4Cool;
    @Expose
    private List<PotentialSpawns> potentialSpawns;

    /**
     * Record of nameSpace and key
     */
    record LootTableSerial(@Expose String nameSpace, @Expose String key) {
    }

    record PotentialSpawns(@Expose String snapshot, @Expose Map<String, Object> spawnrule, @Expose int spawnWeight) {
    }

    public BlueprintTrialSpawner(boolean ominous, TrialSpawnerConfiguration spawner) {
        this.ominous = ominous;
        this.addSimulEnts = spawner.getAdditionalSimultaneousEntities();
        this.addSpawnsB4Cool = spawner.getAdditionalSpawnsBeforeCooldown();
        this.baseSimEnts = spawner.getBaseSimultaneousEntities();
        this.baseSpawnsB4Cool = spawner.getBaseSpawnsBeforeCooldown();
        this.lootTableMap = convertToLootTableSerial(spawner.getPossibleRewards());
        this.spawnRange = spawner.getSpawnRange();
        this.requiredPlayerRange = spawner.getRequiredPlayerRange();
        // Spawns
        potentialSpawns = spawner.getPotentialSpawns().stream().map(se -> {
            EntitySnapshot snapshot = se.getSnapshot();
            SpawnRule spawnRule = se.getSpawnRule();
            return new PotentialSpawns(snapshot.getAsString(), spawnRule == null ? null : spawnRule.serialize(),
                    se.getSpawnWeight());
            // Missing
            // se.getEquipment().getEquipmentLootTable();
        }).toList();

        if (potentialSpawns.isEmpty()) {
            potentialSpawns = null;
            this.spawnedType = spawner.getSpawnedType();
        }
    }

    private Map<LootTableSerial, Integer> convertToLootTableSerial(Map<LootTable, Integer> possibleRewards) {
        // Use streams to map entries from LootTable to LootTableSerial
        return possibleRewards.entrySet().stream().collect(Collectors.toMap(entry -> { // Convert LootTable to LootTableSerial
            NamespacedKey key = entry.getKey().getKey();
            return new LootTableSerial(key.getNamespace(), key.getKey());
        }, Map.Entry::getValue // Keep the Integer value unchanged
        ));
    }

    // Method to convert Map<LootTableSerial, Integer> back to Map<LootTable, Integer>
    private Map<LootTable, Integer> convertToLootTable(Map<LootTableSerial, Integer> serializedRewards) {
        Map<LootTable, Integer> result = new HashMap<>();
        for (Map.Entry<LootTableSerial, Integer> entry : serializedRewards.entrySet()) {
            LootTableSerial lootTableSerial = entry.getKey();
            Integer value = Math.max(1, entry.getValue()); // weight has to be at least 1

            // Reconstruct the NamespacedKey
            NamespacedKey key = new NamespacedKey(lootTableSerial.nameSpace(), lootTableSerial.key());

            // Fetch the LootTable using Bukkit
            LootTable lootTable = Bukkit.getLootTable(key);

            if (lootTable != null) { // Ensure the LootTable exists
                result.put(lootTable, value);
            } else {
                System.err.println("LootTable not found for key: " + key);
            }
        }
        return result;
    }

    /**
     * Configure the trial spawner
     * @param spawner trial spawner config
     * @return true if trial spawner is ominous, false if normal
     */
    public boolean configTrialSpawner(TrialSpawnerConfiguration spawner) {
        spawner.setAdditionalSimultaneousEntities((float) addSimulEnts);
        spawner.setAdditionalSpawnsBeforeCooldown((float) addSpawnsB4Cool);
        spawner.setBaseSimultaneousEntities((float) baseSimEnts);
        spawner.setBaseSpawnsBeforeCooldown((float) baseSpawnsB4Cool);
        spawner.setDelay(delay);
        spawner.setSpawnRange(spawnRange);
        spawner.setPossibleRewards(convertToLootTable(lootTableMap)); // Note to future me: if the weight in the map is zero code stops running at this pojnt!
        spawner.setRequiredPlayerRange(requiredPlayerRange);
        // Either/or spawned type
        if (spawnedType != null) {
            spawner.setSpawnedType(spawnedType);
        } else {
            spawner.setPotentialSpawns(this.potentialSpawns.stream().map(ps -> {
                EntitySnapshot snapshot = Bukkit.getEntityFactory().createEntitySnapshot(ps.snapshot());
                SpawnRule rule = ps.spawnrule() != null ? SpawnRule.deserialize(ps.spawnrule()) : null;
                return new SpawnerEntry(snapshot, ps.spawnWeight(), rule);
            }).collect(Collectors.toList()));
        }
        return this.isOminous();
    }

    /**
     * @return the ominous
     */
    public boolean isOminous() {
        return ominous;
    }

    /**
     * @param ominous the ominous to set
     */
    public void setOminous(boolean ominous) {
        this.ominous = ominous;
    }

    /**
     * @return the spawnedType
     */
    public EntityType getSpawnedType() {
        return spawnedType;
    }

    /**
     * @param spawnedType the spawnedType to set
     */
    public void setSpawnedType(EntityType spawnedType) {
        this.spawnedType = spawnedType;
    }

    /**
     * @return the addSimulEnts
     */
    public double getAddSimulEnts() {
        return addSimulEnts;
    }

    /**
     * @param addSimulEnts the addSimulEnts to set
     */
    public void setAddSimulEnts(double addSimulEnts) {
        this.addSimulEnts = addSimulEnts;
    }

    /**
     * @return the addSpawnsB4Cool
     */
    public double getAddSpawnsB4Cool() {
        return addSpawnsB4Cool;
    }

    /**
     * @param addSpawnsB4Cool the addSpawnsB4Cool to set
     */
    public void setAddSpawnsB4Cool(double addSpawnsB4Cool) {
        this.addSpawnsB4Cool = addSpawnsB4Cool;
    }

    /**
     * @return the baseSimEnts
     */
    public double getBaseSimEnts() {
        return baseSimEnts;
    }

    /**
     * @param baseSimEnts the baseSimEnts to set
     */
    public void setBaseSimEnts(double baseSimEnts) {
        this.baseSimEnts = baseSimEnts;
    }

    /**
     * @return the delay
     */
    public int getDelay() {
        return delay;
    }

    /**
     * @param delay the delay to set
     */
    public void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * @return the lootTableMap
     */
    public Map<LootTableSerial, Integer> getLootTableMap() {
        return lootTableMap;
    }

    /**
     * @param lootTableMap the lootTableMap to set
     */
    public void setLootTableMap(Map<LootTableSerial, Integer> lootTableMap) {
        this.lootTableMap = lootTableMap;
    }

    /**
     * @return the spawnRange
     */
    public int getSpawnRange() {
        return spawnRange;
    }

    /**
     * @param spawnRange the spawnRange to set
     */
    public void setSpawnRange(int spawnRange) {
        this.spawnRange = spawnRange;
    }

    /**
     * @return the requiredPlayerRange
     */
    public int getRequiredPlayerRange() {
        return requiredPlayerRange;
    }

    /**
     * @param requiredPlayerRange the requiredPlayerRange to set
     */
    public void setRequiredPlayerRange(int requiredPlayerRange) {
        this.requiredPlayerRange = requiredPlayerRange;
    }

    /**
     * @return the playerRange
     */
    public int getPlayerRange() {
        return playerRange;
    }

    /**
     * @param playerRange the playerRange to set
     */
    public void setPlayerRange(int playerRange) {
        this.playerRange = playerRange;
    }

    /**
     * @return the baseSpawnsB4Cool
     */
    public double getBaseSpawnsB4Cool() {
        return baseSpawnsB4Cool;
    }

    /**
     * @param baseSpawnsB4Cool the baseSpawnsB4Cool to set
     */
    public void setBaseSpawnsB4Cool(double baseSpawnsB4Cool) {
        this.baseSpawnsB4Cool = baseSpawnsB4Cool;
    }

    @Override
    public String toString() {
        return "BlueprintTrialSpawner [ominous=" + ominous + ", "
                + (spawnedType != null ? "spawnedType=" + spawnedType + ", " : "") + "addSimulEnts=" + addSimulEnts
                + ", addSpawnsB4Cool=" + addSpawnsB4Cool + ", baseSimEnts=" + baseSimEnts + ", delay=" + delay + ", "
                + (lootTableMap != null ? "lootTableMap=" + lootTableMap + ", " : "") + "spawnRange=" + spawnRange
                + ", requiredPlayerRange=" + requiredPlayerRange + ", playerRange=" + playerRange
                + ", baseSpawnsB4Cool=" + baseSpawnsB4Cool + "]";
    }

}
