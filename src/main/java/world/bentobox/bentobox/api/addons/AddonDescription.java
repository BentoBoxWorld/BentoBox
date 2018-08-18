package world.bentobox.bentobox.api.addons;

import java.util.ArrayList;
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
    private List<String> authors = new ArrayList<>();
    private List<String> depend = new ArrayList<>();

    public AddonDescription() {}

    public AddonDescription(String main, String name, String version, String description, List<String> authors, List<String> depend) {
        this.main = main;
        this.name = name;
        this.version = version;
        this.description = description;
        this.authors = authors;
        this.depend = depend;
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

    /**
     * @return the loadAfter
     */
    public List<String> getDependencies() {
        return depend;
    }

    /**
     * @param loadAfter the loadAfter to set
     */
    public void setLoadAfter(List<String> loadAfter) {
        this.depend = loadAfter;
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

        public AddonDescriptionBuilder withDepend(List<String> addons) {
            description.setLoadAfter(addons);
            return this;
        }

        public AddonDescription build(){
            return description;
        }

    }
}
