package world.bentobox.bentobox.web.catalog;

import org.bukkit.Material;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

/**
 * @author Poslovitch
 * @since 1.5.0
 */
public class CatalogEntry {

    private final int slot;
    /**
     * Defaults to {@link Material#PAPER}.
     */
    private @NonNull
    final Material icon;
    private @NonNull
    final String name;
    private @NonNull
    final String description;
    private @Nullable
    final String topic;
    private @Nullable
    final String tag;
    private @NonNull
    final String repository;

    public CatalogEntry(@NonNull JsonObject object) {
        this.slot = object.get("slot").getAsInt();
        Material material = Material.getMaterial(object.get("icon").getAsString());
        this.icon = (material != null) ? material : Material.PAPER;
        this.name = object.get("name").getAsString();
        this.description = object.get("description").getAsString();
        this.repository = object.get("repository").getAsString();
        this.topic = object.get("topic").getAsString();
        this.tag = (!(object.get("tag") instanceof JsonNull)) ? object.get("tag").getAsString() : null;
    }

    public int getSlot() {
        return slot;
    }

    @NonNull
    public Material getIcon() {
        return icon;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getTopic() {
        return topic;
    }

    @Nullable
    public String getTag() {
        return tag;
    }

    @NonNull
    public String getRepository() {
        return repository;
    }
}
