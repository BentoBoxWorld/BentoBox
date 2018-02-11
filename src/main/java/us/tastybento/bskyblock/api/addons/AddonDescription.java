package us.tastybento.bskyblock.api.addons;

import java.util.Arrays;
import java.util.List;

/**
 * @author Tastybento, Poslovitch
 */
public final class AddonDescription {

    private String main;
    private String name;
    private String version;
    private String description;
    private List<String> authors;

    public AddonDescription() {}

    public AddonDescription(String main, String name, String version, String description, List<String> authors) {
        this.main = main;
        this.name = name;
        this.version = version;
        this.description = description;
        this.authors = authors;
    }

    /**
     * @param main the main to set
     */
    public void setMain(String main) {
        this.main = main;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param authors the authors to set
     */
    public void setAuthors(List<String> authors) {
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

    public static class AddonDescriptionBuilder{

        private AddonDescription description;

        public AddonDescriptionBuilder(String name){
            description = new AddonDescription();
            description.setName(name);
        }

        public AddonDescriptionBuilder withAuthor(String... authors){
            description.setAuthors(Arrays.asList(authors));
            return this;
        }

        public AddonDescriptionBuilder withDescription(String desc){
            description.setDescription(desc);
            return this;
        }

        public AddonDescriptionBuilder withVersion(String version){
            description.setVersion(version);
            return this;
        }

        public AddonDescription build(){
            return description;
        }

    }
}
