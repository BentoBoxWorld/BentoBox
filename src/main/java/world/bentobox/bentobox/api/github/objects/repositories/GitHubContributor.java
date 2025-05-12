package world.bentobox.bentobox.api.github.objects.repositories;

/**
 * Represents a contributor to a GitHub repository.
 */
public class GitHubContributor {

    private final String username;
    private final int contributionsAmount;

    public GitHubContributor(String username, int contributionsAmount) {
        this.username = username;
        this.contributionsAmount = contributionsAmount;
    }

    public String getUsername() {
        return username;
    }

    public int getContributionsAmount() {
        return contributionsAmount;
    }
}
