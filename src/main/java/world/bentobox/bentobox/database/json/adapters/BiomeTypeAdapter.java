//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.database.json.adapters;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.block.Biome;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Enums;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * Minecraft 1.18 reworked their biomes, and a lot of things were renamed or removed.
 * This adapter will address these changes in each database object, instead of manually fining it
 * per implementation.
 */
public final class BiomeTypeAdapter extends TypeAdapter<Biome>
{
    /**
     * Map that contains string value to the actual Biome enum object.
     */
    static final Map<String, Biome> BIOMEMAP;
    static {
        Map<String, Biome> biomeMap = new HashMap<>();

        // Put in current values.
        Arrays.stream(Biome.values()).forEach(biome -> biomeMap.put(biome.name(), biome));

        // Put in renamed biomes values.
        biomeMap.put("TALL_BIRCH_FOREST", getBiome("OLD_GROWTH_BIRCH_FOREST", "TALL_BIRCH_FOREST"));
        biomeMap.put("GIANT_TREE_TAIGA", getBiome("OLD_GROWTH_PINE_TAIGA", "GIANT_TREE_TAIGA"));
        biomeMap.put("GIANT_SPRUCE_TAIGA", getBiome("OLD_GROWTH_SPRUCE_TAIGA", "GIANT_SPRUCE_TAIGA"));
        biomeMap.put("SNOWY_TUNDRA", getBiome("SNOWY_PLAINS", "SNOWY_TUNDRA"));
        biomeMap.put("JUNGLE_EDGE", getBiome("SPARSE_JUNGLE", "JUNGLE_EDGE"));
        biomeMap.put("STONE_SHORE", getBiome("STONY_SHORE", "STONE_SHORE"));
        biomeMap.put("MOUNTAINS", getBiome("WINDSWEPT_HILLS", "MOUNTAINS"));
        biomeMap.put("WOODED_MOUNTAINS", getBiome("WINDSWEPT_FOREST", "WOODED_MOUNTAINS"));
        biomeMap.put("GRAVELLY_MOUNTAINS", getBiome("WINDSWEPT_GRAVELLY_HILLS", "GRAVELLY_MOUNTAINS"));
        biomeMap.put("SHATTERED_SAVANNA", getBiome("WINDSWEPT_SAVANNA", "SHATTERED_SAVANNA"));
        biomeMap.put("WOODED_BADLANDS_PLATEAU", getBiome("WOODED_BADLANDS", "WOODED_BADLANDS_PLATEAU"));

        // Put in removed biomes values. BONNe chose some close enough values.
        biomeMap.put("SNOWY_MOUNTAINS", getBiome("WINDSWEPT_HILLS", "SNOWY_MOUNTAINS"));
        biomeMap.put("DESERT_HILLS", getBiome("WINDSWEPT_HILLS", "DESERT_HILLS"));
        biomeMap.put("MOUNTAIN_EDGE", getBiome("WINDSWEPT_HILLS", "MOUNTAIN_EDGE"));
        biomeMap.put("SNOWY_TAIGA_HILLS", getBiome("WINDSWEPT_HILLS", "SNOWY_TAIGA_HILLS"));
        biomeMap.put("TAIGA_HILLS", getBiome("WINDSWEPT_HILLS", "TAIGA_HILLS"));
        biomeMap.put("TAIGA_MOUNTAINS", getBiome("WINDSWEPT_HILLS", "TAIGA_MOUNTAINS"));
        biomeMap.put("SNOWY_TAIGA_MOUNTAINS", getBiome("WINDSWEPT_HILLS", "SNOWY_TAIGA_MOUNTAINS"));
        biomeMap.put("WOODED_HILLS", getBiome("WINDSWEPT_FOREST", "WOODED_HILLS"));
        biomeMap.put("SWAMP_HILLS", getBiome("WINDSWEPT_FOREST", "SWAMP_HILLS"));
        biomeMap.put("DARK_FOREST_HILLS", getBiome("WINDSWEPT_FOREST", "DARK_FOREST_HILLS"));
        biomeMap.put("JUNGLE_HILLS", getBiome("SPARSE_JUNGLE", "JUNGLE_HILLS"));
        biomeMap.put("MODIFIED_JUNGLE", getBiome("SPARSE_JUNGLE", "MODIFIED_JUNGLE"));
        biomeMap.put("MODIFIED_JUNGLE_EDGE", getBiome("SPARSE_JUNGLE", "MODIFIED_JUNGLE_EDGE"));
        biomeMap.put("BAMBOO_JUNGLE_HILLS", getBiome("SPARSE_JUNGLE", "BAMBOO_JUNGLE_HILLS"));
        biomeMap.put("BIRCH_FOREST_HILLS", getBiome("OLD_GROWTH_BIRCH_FOREST", "BIRCH_FOREST_HILLS"));
        biomeMap.put("TALL_BIRCH_HILLS", getBiome("OLD_GROWTH_BIRCH_FOREST", "TALL_BIRCH_HILLS"));
        biomeMap.put("GIANT_TREE_TAIGA_HILLS", getBiome("OLD_GROWTH_PINE_TAIGA", "GIANT_TREE_TAIGA_HILLS"));
        biomeMap.put("GIANT_SPRUCE_TAIGA_HILLS", getBiome("OLD_GROWTH_SPRUCE_TAIGA", "GIANT_SPRUCE_TAIGA_HILLS"));
        biomeMap.put("MUSHROOM_FIELD_SHORE", getBiome("MUSHROOM_FIELDS", "MUSHROOM_FIELD_SHORE"));
        biomeMap.put("BADLANDS_PLATEAU", getBiome("BADLANDS", "BADLANDS_PLATEAU"));
        biomeMap.put("MODIFIED_WOODED_BADLANDS_PLATEAU", getBiome("BADLANDS", "MODIFIED_WOODED_BADLANDS_PLATEAU"));
        biomeMap.put("MODIFIED_BADLANDS_PLATEAU", getBiome("BADLANDS", "MODIFIED_BADLANDS_PLATEAU"));
        biomeMap.put("SHATTERED_SAVANNA_PLATEAU", getBiome("SAVANNA_PLATEAU", "SHATTERED_SAVANNA_PLATEAU"));
        biomeMap.put("DESERT_LAKES", getBiome("DESERT", "DESERT_LAKES"));
        biomeMap.put("MODIFIED_GRAVELLY_MOUNTAINS", getBiome("WINDSWEPT_GRAVELLY_HILLS", "MODIFIED_GRAVELLY_MOUNTAINS"));
        biomeMap.put("DEEP_WARM_OCEAN", getBiome("DEEP_LUKEWARM_OCEAN", "DEEP_WARM_OCEAN"));

        BIOMEMAP = Collections.unmodifiableMap(biomeMap);
    }

    /**
     * Safely gets a biome based on string values
     * @param primary - primary biome
     * @param secondary - secondary biome
     * @return Biome or null
     */
    @Nullable
    private static Biome getBiome(String primary, String secondary) {
        return Enums.getIfPresent(Biome.class, primary).or(Enums.getIfPresent(Biome.class, secondary).orNull());
    }

    @Override
    public Biome read(JsonReader input) throws IOException
    {
        if (JsonToken.NULL.equals(input.peek())) {
            input.nextNull();
            return null;
        }

        return BIOMEMAP.get(input.nextString().toUpperCase(Locale.ENGLISH));
    }

    @Override
    public void write(JsonWriter output, Biome enumValue) throws IOException {
        output.value(enumValue != null ? enumValue.name() : null);
    }
}

