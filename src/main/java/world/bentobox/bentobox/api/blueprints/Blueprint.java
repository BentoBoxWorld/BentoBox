package world.bentobox.bentobox.api.blueprints;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.bukkit.Material;
import org.bukkit.World;
import org.eclipse.jdt.annotation.NonNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.zip.ZipFile;

/**
 * @since 1.5.0
 * @author Poslovitch
 */
public class Blueprint {

    public static final @NonNull String FILE_EXTENSION = "blueprint";

    private @NonNull String name;
    private String displayName;
    private @NonNull Material icon = Material.PAPER;
    private List<String> description; //TODO
    private World.Environment environment;

    public Blueprint(@NonNull String name, @NonNull ZipFile zip) throws IOException {
        this.name = name;
        try (JsonReader reader = new Gson().newJsonReader(new InputStreamReader(zip.getInputStream(zip.getEntry("properties.json"))))) {
            readProperties(reader);
        }

        //System.out.println(new Gson().toJson(this));
    }

    private void readProperties(@NonNull JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            String field = reader.nextName();
            switch (field) {
                case "displayName":
                    displayName = reader.nextString();
                    break;
                case "icon":
                    icon = Material.valueOf(reader.nextString());
                    break;
                case "environment":
                    environment = World.Environment.valueOf(reader.nextString());
                    break;
                default:
                    reader.skipValue();
                    break;
            }
        }
        reader.endObject();
    }

    @NonNull
    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    @NonNull
    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }

    public World.Environment getEnvironment() {
        return environment;
    }
}
