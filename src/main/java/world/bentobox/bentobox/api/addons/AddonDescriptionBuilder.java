package world.bentobox.bentobox.api.addons;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AddonDescriptionBuilder {
    @NonNull
    final String main;
    @NonNull
    final String name;
    @NonNull
    final String version;
    @NonNull
    String description = "";
    @NonNull
    List<String> authors = new ArrayList<>();
    @NonNull
    List<String> dependencies = new ArrayList<>();
    @NonNull
    List<String> softDependencies = new ArrayList<>();
    boolean metrics = true;
    @NonNull
    String repository = "";
    @NonNull
    Material icon = Material.PAPER;
    @NonNull
    String apiVersion = "1";
    @Nullable
    ConfigurationSection permissions;

    /**
     * @since 1.1
     */
    public AddonDescriptionBuilder(@NonNull String main, @NonNull String name, @NonNull String version) {
        this.main = main;
        this.name = name;
        this.version = version;
    }

    @NonNull
    public AddonDescriptionBuilder description(@NonNull String description) {
        this.description = description;
        return this;
    }

    @NonNull
    public AddonDescriptionBuilder authors(@NonNull String... authors) {
        this.authors = Arrays.asList(authors);
        return this;
    }

    @NonNull
    public AddonDescriptionBuilder dependencies(@NonNull List<String> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    @NonNull
    public AddonDescriptionBuilder softDependencies(@NonNull List<String> softDependencies) {
        this.softDependencies = softDependencies;
        return this;
    }

    /**
     * @since 1.1
     */
    @NonNull
    public AddonDescriptionBuilder metrics(boolean metrics) {
        this.metrics = metrics;
        return this;
    }

    /**
     * Sets the name of the GitHub repository.
     * Must follow the {@code Owner/Name} format.
     * @since 1.3.0
     */
    @NonNull
    public AddonDescriptionBuilder repository(@NonNull String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * Sets the icon representing the addon.
     * @param icon Material to set as the icon. Default is {@link Material#PAPER}.
     * @since 1.5.0
     */
    @NonNull
    public AddonDescriptionBuilder icon(@NonNull Material icon) {
        this.icon = icon;
        return this;
    }

    /**
     * Sets the minimum BentoBox version this addon requires in order to work properly.
     * @param apiVersion the minimum BentoBox version this addon requires in order to work properly.
     * @since 1.11.0
     * @see AddonDescription#getApiVersion()
     */
    @NonNull
    public AddonDescriptionBuilder apiVersion(@NonNull String apiVersion) {
        this.apiVersion = apiVersion;
        return this;
    }

    @NonNull
    public AddonDescription build() {
        return new AddonDescription(this);
    }

    /**
     * Sets the permission config section. Taken from the addon.yml
     * @param permissions - YAML configuration section
     * @return Builder
     * @since 1.13.0
     */
    @NonNull
    public AddonDescriptionBuilder permissions(ConfigurationSection permissions) {
        this.permissions = permissions;
        return this;
    }
}
