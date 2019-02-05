package world.bentobox.bentobox.api.addons;

import org.eclipse.jdt.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    /**
     * Whether the addon should be included in Metrics or not.
     * @since 1.1
     */
    private final boolean metrics;

    private AddonDescription(@NonNull Builder builder) {
        this.main = builder.main;
        this.name = builder.name;
        this.version = builder.version;
        this.description = builder.description;
        this.authors = builder.authors;
        this.dependencies = builder.dependencies;
        this.softDependencies = builder.softDependencies;
        this.metrics = builder.metrics;
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

    public static class Builder {
        private @NonNull String main;
        private @NonNull String name;
        private @NonNull String version;
        private @NonNull String description = "";
        private @NonNull List<String> authors = new ArrayList<>();
        private @NonNull List<String> dependencies = new ArrayList<>();
        private @NonNull List<String> softDependencies = new ArrayList<>();
        private boolean metrics = false;
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

        @NonNull
        public AddonDescription build() {
            return new AddonDescription(this);
        }
    }
}
