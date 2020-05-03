package world.bentobox.bentobox.api.addons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author tastybento, Poslovitch
 */
public final class AddonDescription {

    private final @NonNull String main;
    private final @NonNull String name;
    private final @NonNull String version;
    private final @NonNull String description;
    private final @NonNull List<String> authors;
    private final @NonNull List<String> dependencies;
    private final @NonNull List<String> softDependencies;
    private final @Nullable ConfigurationSection permissions;
    /**
     * Whether the addon should be included in Metrics or not.
     * @since 1.1
     */
    private final boolean metrics;
    /**
     * Name of the GitHub repository of the addon or an empty String.
     * It follows a {@code Owner/Name} format.
     * @since 1.3.0
     */
    private final @NonNull String repository;
    /**
     * Icon representing the addon in various menus.
     * @since 1.5.0
     */
    private final @NonNull Material icon;
    /**
     * Minimum BentoBox version this addon requires in order to work properly.
     * Defaults to {@code "1"}.
     * @since 1.11.0
     */
    private final @NonNull String apiVersion;

    private AddonDescription(@NonNull Builder builder) {
        this.main = builder.main;
        this.name = builder.name;
        this.version = builder.version;
        this.description = builder.description;
        this.authors = builder.authors;
        this.dependencies = builder.dependencies;
        this.softDependencies = builder.softDependencies;
        this.metrics = builder.metrics;
        this.repository = builder.repository;
        this.icon = builder.icon;
        this.apiVersion = builder.apiVersion;
        this.permissions = builder.permissions;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getMain() {
        return main;
    }

    @NonNull
    public String getVersion() {
        return version;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    @NonNull
    public List<String> getAuthors() {
        return authors;
    }

    /**
     * @return the dependencies
     */
    @NonNull
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * @return the softDependencies
     */
    @NonNull
    public List<String> getSoftDependencies() {
        return softDependencies;
    }

    /**
     * Returns whether the addon should be included in Metrics or not.
     * @return {@code true} if the addon should be included in Metrics reports, {@code false} otherwise.
     * @since 1.1
     */
    public boolean isMetrics() {
        return metrics;
    }

    /**
     * Returns the name of the GitHub repository of the addon.
     * It follows a {@code Owner/Name} format.
     * @return the name of the GitHub repository of the addon or an empty String.
     * @since 1.3.0
     */
    @NonNull
    public String getRepository() {
        return repository;
    }

    /**
     * Returns the material representing the addon as an icon.
     * @return the material representing the addon as an icon.
     * @since 1.5.0
     */
    @NonNull
    public Material getIcon() {
        return icon;
    }

    /**
     * Returns the minimum BentoBox version this addon requires in order to work properly.
     * <br/>
     * Examples:
     * <ul>
     *     <li>{@code "1"} means that the addon relies on BentoBox {@code 1.0.0} or higher.</li>
     *     <li>Similarly, {@code "2"} sets the requirement to BentoBox {@code 2.0.0} or higher.</li>
     *     <li>
     *         More specific versions can be provided:
     *         <ul>
     *             <li>{@code "1.10"} -> BentoBox {@code 1.10.0} or higher.</li>
     *             <li>{@code "1.9.2"} -> BentoBox {@code 1.9.2} or higher.</li>
     *         </ul>
     *     </li>
     * </ul>
     * Defaults to {@code "1"}.
     * @return the minimum BentoBox version this addon requires in order to work properly.
     */
    @NonNull
    public String getApiVersion() {
        return apiVersion;
    }

    /**
     * @return the permissions
     * @since 1.13.0
     */
    public ConfigurationSection getPermissions() {
        return permissions;
    }

    public static class Builder {
        private @NonNull String main;
        private @NonNull String name;
        private @NonNull String version;
        private @NonNull String description = "";
        private @NonNull List<String> authors = new ArrayList<>();
        private @NonNull List<String> dependencies = new ArrayList<>();
        private @NonNull List<String> softDependencies = new ArrayList<>();
        private boolean metrics = true;
        private @NonNull String repository = "";
        private @NonNull Material icon = Material.PAPER;
        private @NonNull String apiVersion = "1";
        private @Nullable ConfigurationSection permissions;

        /**
         * @since 1.1
         */
        public Builder(@NonNull String main, @NonNull String name, @NonNull String version) {
            this.main = main;
            this.name = name;
            this.version = version;
        }

        @NonNull
        public Builder description(@NonNull String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public Builder authors(@NonNull String... authors) {
            this.authors = Arrays.asList(authors);
            return this;
        }

        @NonNull
        public Builder dependencies(@NonNull List<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        @NonNull
        public Builder softDependencies(@NonNull List<String> softDependencies) {
            this.softDependencies = softDependencies;
            return this;
        }

        /**
         * @since 1.1
         */
        @NonNull
        public Builder metrics(boolean metrics) {
            this.metrics = metrics;
            return this;
        }

        /**
         * Sets the name of the GitHub repository.
         * Must follow the {@code Owner/Name} format.
         * @since 1.3.0
         */
        @NonNull
        public Builder repository(@NonNull String repository) {
            this.repository = repository;
            return this;
        }

        /**
         * Sets the icon representing the addon.
         * @param icon Material to set as the icon. Default is {@link Material#PAPER}.
         * @since 1.5.0
         */
        @NonNull
        public Builder icon(@NonNull Material icon) {
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
        public Builder apiVersion(@NonNull String apiVersion) {
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
        public Builder permissions(ConfigurationSection permissions) {
            this.permissions = permissions;
            return this;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "AddonDescription [" + (name != null ? "name=" + name + ", " : "")
                + (version != null ? "version=" + version : "") + "]";
    }
}
