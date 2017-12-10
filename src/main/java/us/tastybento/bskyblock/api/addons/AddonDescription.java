package us.tastybento.bskyblock.api.addons;

import java.util.List;

public final class AddonDescription {

    private String main;
    private String name;
    private String version;
    private String description;
    private List<String> authors;

    public AddonDescription(String main, String name, String version, String description, List<String> authors) {
        this.main = main;
        this.name = name;
        this.version = version;
        this.description = description;
        this.authors = authors;
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
}
