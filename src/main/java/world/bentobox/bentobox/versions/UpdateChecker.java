package world.bentobox.bentobox.versions;

import io.github.TheBusyBiscuit.GitHubWebAPI4Java.GitHubWebAPI;
import io.github.TheBusyBiscuit.GitHubWebAPI4Java.objects.repositories.GitHubRelease;
import org.eclipse.jdt.annotation.Nullable;
import world.bentobox.bentobox.BentoBox;
import world.bentobox.bentobox.util.Util;

import java.util.List;

/**
 * Checks for updates through the GitHub API.
 * @author Poslovitch
 * @since 1.14.0
 */
public class UpdateChecker {

    private final BentoBox plugin;
    private final GitHubWebAPI gitHub;
    private final String repository;
    private final String currentVersion;

    @Nullable
    private Result result;

    public UpdateChecker(GitHubWebAPI gitHub, String repository, String currentVersion) {
        this.plugin = BentoBox.getInstance();
        this.gitHub = gitHub;
        this.repository = repository;
        this.currentVersion = currentVersion;
    }

    public void checkUpdates() throws IllegalAccessException {
        String[] repo = repository.split("/");

        List<GitHubRelease> releases = gitHub.getRepository(repo[0], repo[1]).getReleases();
        if (!releases.isEmpty()) {
            for (GitHubRelease release : releases) {
                if (release.isDraft()) {
                    // Drafts should be ignored (they're not published yet)
                    continue;
                }

                if (release.isPreRelease() && !plugin.getSettings().isCheckPreReleasesUpdates()) {
                    // We don't care about pre-releases
                    continue;
                }

                String newVersion = release.getTagName();
                if (isMoreRecent(newVersion)) {
                    // We found the new version, and it should be the latest.
                    this.result = new Result(newVersion, release.isPreRelease());
                    break;
                }
            }
        } else {
            this.result = null;
        }
    }

    private boolean isMoreRecent(String newVersion) {
        String[] currentVer = currentVersion.split("\\D");
        String[] newVer = newVersion.split("\\D");

        for (int i = 0; i < currentVer.length; i++) {
            int newVersionNumber = 0;
            if (i < newVer.length && Util.isInteger(newVer[i], false)) {
                newVersionNumber = Integer.parseInt(newVer[i]);
            }
            int currentVersionNumber = Util.isInteger(currentVer[i], false) ? Integer.parseInt(currentVer[i]) : -1;

            if (newVersionNumber > currentVersionNumber) {
                return false; // The current version is greater than the "new" version -> up to date.
            }
            if (newVersionNumber < currentVersionNumber) {
                return true; // The current version is outdated
            }
            // If it is equal, go to the next number
        }

        return false; // Everything is equal, so return true
    }

    @Nullable
    public Result getResult() {
        return result;
    }

    public String getRepository() {
        return repository;
    }

    public static class Result {

        private final String version;
        private final boolean preRelease;

        public Result(String version, boolean preRelease) {
            this.version = version;
            this.preRelease = preRelease;
        }

        public String getVersion() {
            return version;
        }

        public boolean isPreRelease() {
            return preRelease;
        }
    }
}
