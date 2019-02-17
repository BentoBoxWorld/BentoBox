package world.bentobox.bentobox.api.github;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Represents a release on a Github repository.
 * See https://api.github.com/repos/BentoBoxWorld/BentoBox/releases.
 * @author Poslovitch
 * @since 1.3.0
 */
public class Release {

    private final @NonNull String name;
    private final @NonNull String tag;
    private final @NonNull String url;

    private final boolean preRelease;
    private final @Nullable Date publishedAt;

    /* Release asset related fields */
    private final @Nullable String downloadUrl;
    private final long downloadSize;
    private int downloadCount;

    private Release(@NonNull Builder builder) {
        this.name = builder.name;
        this.tag = builder.tag;
        this.url = builder.url;
        this.preRelease = builder.preRelease;
        this.publishedAt = builder.publishedAt;
        this.downloadUrl = builder.downloadUrl;
        this.downloadSize = builder.downloadSize;
        this.downloadCount = builder.downloadCount;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getTag() {
        return tag;
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    public boolean isPreRelease() {
        return preRelease;
    }

    @Nullable
    public Date getPublishedAt() {
        return publishedAt;
    }

    @Nullable
    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getDownloadSize() {
        return downloadSize;
    }

    public int getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(int downloadCount) {
        this.downloadCount = downloadCount;
    }

    public static class Builder {
        private final @NonNull String name;
        private final @NonNull String tag;
        private final @NonNull String url;

        private boolean preRelease;
        private @Nullable Date publishedAt;
        private @Nullable String downloadUrl;
        private long downloadSize;
        private int downloadCount;

        public Builder(@NonNull String name, @NonNull String tag, @NonNull String url) {
            this.name = name;
            this.tag = tag;
            this.url = url;
            this.preRelease = false;
            this.downloadSize = 0L;
            this.downloadCount = 0;
        }

        public Builder preRelease(boolean preRelease) {
            this.preRelease = preRelease;
            return this;
        }

        public Builder publishedAt(@Nullable Date publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder downloadUrl(@Nullable String downloadUrl) {
            this.downloadUrl = downloadUrl;
            return this;
        }

        public Builder downloadSize(long downloadSize) {
            this.downloadSize = downloadSize;
            return this;
        }

        public Builder downloadCount(int downloadCount) {
            this.downloadCount = downloadCount;
            return this;
        }

        public Release build() {
            return new Release(this);
        }
    }

}
