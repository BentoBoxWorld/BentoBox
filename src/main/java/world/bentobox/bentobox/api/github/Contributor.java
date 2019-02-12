package world.bentobox.bentobox.api.github;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Represents a Contributor on a GitHub repository.
 * @author Poslovitch
 * @since 1.3.0
 */
public class Contributor {

    private final @NonNull String name;
    private final @NonNull String profile;
    private int commits;

    public Contributor(@NonNull String name, int commits) {
        this.name = name;
        this.profile = "https://github.com/" + name;
        this.commits = commits;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getProfile() {
        return profile;
    }

    public int getCommits() {
        return commits;
    }

    public void setCommits(int commits) {
        this.commits = commits;
    }
}
