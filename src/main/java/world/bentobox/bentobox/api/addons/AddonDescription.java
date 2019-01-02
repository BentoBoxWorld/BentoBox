package world.bentobox.bentobox.api.addons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author tastybento, Poslovitch
 */
public final class AddonDescription {

    private String main;
    private String name;
    private String version;
    private String description;
    private List<String> authors;
    private List<String> dependencies;
    private List<String> softDependencies;

    private AddonDescription(Builder builder) {
        this.main = builder.main;
        this.name = builder.name;
        this.version = builder.version;
        this.description = builder.description;
        this.authors = builder.authors;
        this.dependencies = builder.dependencies;
        this.softDependencies = builder.softDependencies;
    }

    public String getName() {
        return name;
    }

    public String getMain() {
        return main;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAuthors() {
        return authors;
    }

    /**
     * @return the dependencies
     */
    public List<String> getDependencies() {
        return dependencies;
    }

    /**
     * @return the softDependencies
     */
    public List<String> getSoftDependencies() {
        return softDependencies;
    }

    public static class Builder {
        private String main;
        private String name;
        private String version;
        private String description;
        private List<String> authors = new ArrayList<>();
        private List<String> dependencies = new ArrayList<>();
        private List<String> softDependencies = new ArrayList<>();

        public Builder(String main, String name) {
            this.main = main;
            this.name = name;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder authors(String... authors) {
            this.authors = Arrays.asList(authors);
            return this;
        }

        public Builder dependencies(List<String> dependencies) {
            this.dependencies = dependencies;
            return this;
        }

        public Builder softDependencies(List<String> softDependencies) {
            this.softDependencies = softDependencies;
            return this;
        }

        public AddonDescription build() {
            return new AddonDescription(this);
        }
    }
}
