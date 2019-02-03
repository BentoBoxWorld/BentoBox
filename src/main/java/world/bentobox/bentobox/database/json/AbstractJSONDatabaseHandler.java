package world.bentobox.bentobox.database.json;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.api.flags.Flag;
import world.bentobox.bentobox.database.AbstractDatabaseHandler;
import world.bentobox.bentobox.database.DatabaseConnector;
import world.bentobox.bentobox.database.json.adapters.FlagAdapter;
import world.bentobox.bentobox.database.json.adapters.ItemStackTypeAdapter;
import world.bentobox.bentobox.database.json.adapters.LocationAdapter;
import world.bentobox.bentobox.database.json.adapters.PotionEffectTypeAdapter;
import world.bentobox.bentobox.database.json.adapters.WorldAdapter;

/**
 * Abstract class that handles insert/select-operations into/from a database.
 * It also provides {@link #getGson()}.
 *
 * @author Poslovitch, tastybento
 *
 * @param <T>
 */
public abstract class AbstractJSONDatabaseHandler<T> extends AbstractDatabaseHandler<T> {

    private Gson gson;

    /**
     * Constructor
     *
     * @param plugin
     * @param type              The type of the objects that should be created and filled with
     *                          values from the database or inserted into the database
     * @param databaseConnector Contains the settings to create a connection to the database
     */
    protected AbstractJSONDatabaseHandler(BentoBox plugin, Class<T> type, DatabaseConnector databaseConnector) {
        super(plugin, type, databaseConnector);

        // Build the Gson

        // excludeFieldsWithoutExposeAnnotation - this means that every field to be stored should use @Expose
        // enableComplexMapKeySerialization - forces GSON to use TypeAdapters even for Map keys
        GsonBuilder builder = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().enableComplexMapKeySerialization();
        // Register adapters
        builder.registerTypeAdapter(Location.class, new LocationAdapter()) ;
        builder.registerTypeAdapter(World.class, new WorldAdapter());
        builder.registerTypeAdapter(Flag.class, new FlagAdapter(plugin));
        builder.registerTypeAdapter(PotionEffectType.class, new PotionEffectTypeAdapter());
        builder.registerTypeAdapter(ItemStack.class, new ItemStackTypeAdapter());
        // Keep null in the database
        builder.serializeNulls();
        // Allow characters like < or > without escaping them
        builder.disableHtmlEscaping();

        gson = builder.create();
    }

    protected Gson getGson() {
        return gson;
    }
}
