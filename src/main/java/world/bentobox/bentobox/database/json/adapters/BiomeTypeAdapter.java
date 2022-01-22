//
// Created by BONNe
// Copyright - 2021
//


package world.bentobox.bentobox.database.json.adapters;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.block.Biome;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;


/**
 * Minecraft 1.18 reworked their biomes, and a lot of things were renamed or removed.
 * This adapter will address these changes in each database object, instead of manually fining it
 * per implementation.
 */
public  final class BiomeTypeAdapter extends TypeAdapter<Biome>
{
    /**
     * Map that contains string value to the actual Biome enum object.
     */
    final Map<String, Biome> biomeMap;

    public BiomeTypeAdapter() {
        this.biomeMap = new HashMap<>();

        // Put in current values.
        Arrays.stream(Biome.values()).forEach(biome -> this.biomeMap.put(biome.name(), biome));

        // Put in renamed biomes values.
        this.biomeMap.put("TALL_BIRCH_FOREST", Biome.OLD_GROWTH_BIRCH_FOREST);
        this.biomeMap.put("GIANT_TREE_TAIGA", Biome.OLD_GROWTH_PINE_TAIGA);
        this.biomeMap.put("GIANT_SPRUCE_TAIGA", Biome.OLD_GROWTH_SPRUCE_TAIGA);
        this.biomeMap.put("SNOWY_TUNDRA", Biome.SNOWY_PLAINS);
        this.biomeMap.put("JUNGLE_EDGE", Biome.SPARSE_JUNGLE);
        this.biomeMap.put("STONE_SHORE", Biome.STONY_SHORE);
        this.biomeMap.put("MOUNTAINS", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("WOODED_MOUNTAINS", Biome.WINDSWEPT_FOREST);
        this.biomeMap.put("GRAVELLY_MOUNTAINS", Biome.WINDSWEPT_GRAVELLY_HILLS);
        this.biomeMap.put("SHATTERED_SAVANNA", Biome.WINDSWEPT_SAVANNA);
        this.biomeMap.put("WOODED_BADLANDS_PLATEAU", Biome.WOODED_BADLANDS);

        // Put in removed biomes values. BONNe chose some close enough values.
        this.biomeMap.put("SNOWY_MOUNTAINS", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("DESERT_HILLS", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("MOUNTAIN_EDGE", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("SNOWY_TAIGA_HILLS", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("TAIGA_HILLS", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("TAIGA_MOUNTAINS", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("SNOWY_TAIGA_MOUNTAINS", Biome.WINDSWEPT_HILLS);
        this.biomeMap.put("WOODED_HILLS", Biome.WINDSWEPT_FOREST);
        this.biomeMap.put("SWAMP_HILLS", Biome.WINDSWEPT_FOREST);
        this.biomeMap.put("DARK_FOREST_HILLS", Biome.WINDSWEPT_FOREST);
        this.biomeMap.put("JUNGLE_HILLS", Biome.SPARSE_JUNGLE);
        this.biomeMap.put("MODIFIED_JUNGLE", Biome.SPARSE_JUNGLE);
        this.biomeMap.put("MODIFIED_JUNGLE_EDGE", Biome.SPARSE_JUNGLE);
        this.biomeMap.put("BAMBOO_JUNGLE_HILLS", Biome.SPARSE_JUNGLE);
        this.biomeMap.put("BIRCH_FOREST_HILLS", Biome.OLD_GROWTH_BIRCH_FOREST);
        this.biomeMap.put("TALL_BIRCH_HILLS", Biome.OLD_GROWTH_BIRCH_FOREST);
        this.biomeMap.put("GIANT_TREE_TAIGA_HILLS", Biome.OLD_GROWTH_PINE_TAIGA);
        this.biomeMap.put("GIANT_SPRUCE_TAIGA_HILLS", Biome.OLD_GROWTH_SPRUCE_TAIGA);
        this.biomeMap.put("MUSHROOM_FIELD_SHORE", Biome.MUSHROOM_FIELDS);
        this.biomeMap.put("BADLANDS_PLATEAU", Biome.BADLANDS);
        this.biomeMap.put("MODIFIED_WOODED_BADLANDS_PLATEAU", Biome.BADLANDS);
        this.biomeMap.put("MODIFIED_BADLANDS_PLATEAU", Biome.BADLANDS);
        this.biomeMap.put("SHATTERED_SAVANNA_PLATEAU", Biome.SAVANNA_PLATEAU);
        this.biomeMap.put("DESERT_LAKES", Biome.DESERT);
        this.biomeMap.put("MODIFIED_GRAVELLY_MOUNTAINS", Biome.WINDSWEPT_GRAVELLY_HILLS);
        this.biomeMap.put("DEEP_WARM_OCEAN", Biome.DEEP_LUKEWARM_OCEAN);
    }

    @Override
    public Biome read(JsonReader input) throws IOException
    {
        if (JsonToken.NULL.equals(input.peek())) {
            input.nextNull();
            return null;
        }

        return this.biomeMap.get(input.nextString().toUpperCase());
    }

    @Override
    public void write(JsonWriter output, Biome enumValue) throws IOException {
        output.value(enumValue != null ? enumValue.name() : null);
    }
}

