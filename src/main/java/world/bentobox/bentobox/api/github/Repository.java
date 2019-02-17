package world.bentobox.bentobox.api.github;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a GitHub repository.
 * @author Poslovitch
 * @since 1.3.0
 */
public class Repository {

    private final @NonNull String owner;
    private final @NonNull String name;

    private final @NonNull List<Contributor> contributors;
    private final @NonNull List<Release> releases;

    private int stars;
    private int forks;
    private int openIssues;

    private @Nullable Date latestCommit;

    private Repository(@NonNull Builder builder) {
        this.owner = builder.owner;
        this.name = builder.name;
        this.contributors = builder.contributors;
        this.releases = builder.releases;

        this.stars = builder.stars;
        this.forks = builder.forks;
        this.openIssues = builder.openIssues;
        this.latestCommit = builder.latestCommit;
    }

    @NonNull
    public String getOwner() {
        return owner;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public List<Contributor> getContributors() {
        return contributors;
    }

    @NonNull
    public List<Release> getReleases() {
        return releases;
    }

    public int getStars() {
        return stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public int getForks() {
        return forks;
    }

    public void setForks(int forks) {
        this.forks = forks;
    }

    public int getOpenIssues() {
        return openIssues;
    }

    public void setOpenIssues(int openIssues) {
        this.openIssues = openIssues;
    }

    @NonNull
    public String getUrl() {
        return "https://github.com/" + getOwner() + "/" + getName();
    }

    @Nullable
    public Date getLatestCommit() {
        return latestCommit;
    }

    public void setLatestCommit(@Nullable Date latestCommit) {
        this.latestCommit = latestCommit;
    }

    public static class Builder {
        private final @NonNull String owner;
        private final @NonNull String name;
        private final @NonNull List<Contributor> contributors;
        private final @NonNull List<Release> releases;

        private int stars;
        private int forks;
        private int openIssues;
        private @Nullable Date latestCommit;

        public Builder(@NonNull String owner, @NonNull String name) {
            this.owner = owner;
            this.name = name;
            this.contributors = new LinkedList<>();
            this.releases = new LinkedList<>();
        }

        public Builder contributors(@NonNull Contributor... contributors) {
            this.contributors.addAll(Arrays.asList(contributors));
            return this;
        }

        public Builder releases(@NonNull Release... releases) {
            this.releases.addAll(Arrays.asList(releases));
            return this;
        }

        public Builder stars(int stars) {
            this.stars = stars;
            return this;
        }

        public Builder forks(int forks) {
            this.forks = forks;
            return this;
        }

        public Builder openIssues(int openIssues) {
            this.openIssues = openIssues;
            return this;
        }

        public Builder latestCommit(@Nullable Date latestCommit) {
            this.latestCommit = latestCommit;
            return this;
        }

        public Repository build() {
            return new Repository(this);
        }
    }
}
