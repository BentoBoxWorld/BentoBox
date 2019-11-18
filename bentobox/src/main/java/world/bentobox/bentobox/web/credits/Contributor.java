package world.bentobox.bentobox.web.credits;

import org.eclipse.jdt.annotation.NonNull;

/**
 *
 * @since 1.9.0
 * @author Poslovitch
 */
public class Contributor {

    private @NonNull String name;
    private int commits;

    public Contributor(@NonNull String name, int commits) {
        this.name = name;
        this.commits = commits;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getCommits() {
        return commits;
    }

    @NonNull
    public String getURL() {
        return "https://github.com/" + name;
    }
}
