package world.bentobox.bentobox.versions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.bukkit.Bukkit;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Checks and ensures the current server software is compatible with BentoBox.
 * @author Poslovitch
 */
public class ServerCompatibility {

    // ---- SINGLETON ----

    private static ServerCompatibility instance = new ServerCompatibility();

    public static ServerCompatibility getInstance() {
        return instance;
    }

    private ServerCompatibility() { }

    // ---- CONTENT ----

    private Compatibility result;

    public enum Compatibility {
        /**
         * The server software is compatible with the current version of BentoBox.
         * There shouldn't be any issues.
         */
        COMPATIBLE(true),

        /**
         * The server software might not be compatible but is supported.
         * Issues might occur.
         */
        SUPPORTED(true),

        /**
         * The server software is not supported, even though BentoBox may work fine.
         * Issues are likely and will receive limited support.
         */
        NOT_SUPPORTED(true),

        /**
         * The server software is explicitly not supported and incompatible.
         * BentoBox won't run on it: that's pointless to try to run it.
         */
        INCOMPATIBLE(false);

        private boolean canLaunch;

        Compatibility(boolean canLaunch) {
            this.canLaunch = canLaunch;
        }

        public boolean isCanLaunch() {
            return canLaunch;
        }
    }

    /**
     * Provides a list of server software.
     * Any software that is not listed here is implicitly considered as "INCOMPATIBLE".
     */
    public enum ServerSoftware {
        CRAFTBUKKIT(Compatibility.INCOMPATIBLE),
        BUKKIT(Compatibility.INCOMPATIBLE),
        GLOWSTONE(Compatibility.INCOMPATIBLE),
        SPIGOT(Compatibility.COMPATIBLE),
        PAPER(Compatibility.SUPPORTED),
        TUINITY(Compatibility.SUPPORTED),
        AIRPLANE(Compatibility.SUPPORTED),
        PURPUR(Compatibility.SUPPORTED),
        TACOSPIGOT(Compatibility.NOT_SUPPORTED),
        AKARIN(Compatibility.NOT_SUPPORTED),
        /**
         * @since 1.14.0
         */
        UNKNOWN(Compatibility.INCOMPATIBLE);

        private Compatibility compatibility;
        /**
         * @since 1.14.0
         */
        private String name;

        ServerSoftware(Compatibility compatibility) {
            this.compatibility = compatibility;
        }

        /**
         * @return the name
         * @since 1.14.0
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         * @since 1.14.0
         */
        public ServerSoftware setName(String name) {
            this.name = name;
            return this;
        }

        public Compatibility getCompatibility() {
            return compatibility;
        }
    }

    /**
     * Provides a list of server versions.
     * Any version that is not listed here is implicitly considered as "INCOMPATIBLE".
     */
    public enum ServerVersion {
        V1_13(Compatibility.INCOMPATIBLE),
        V1_13_1(Compatibility.INCOMPATIBLE),
        V1_13_2(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.5.0
         */
        V1_14(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.5.0
         */
        V1_14_1(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.5.0
         */
        V1_14_2(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.6.0
         */
        V1_14_3(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.6.0
         */
        V1_14_4(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.9.2
         */
        V1_15(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.10.0
         */
        V1_15_1(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.11.0
         */
        V1_15_2(Compatibility.INCOMPATIBLE),
        /**
         * @since 1.14.0
         */
        V1_16_1(Compatibility.NOT_SUPPORTED),
        /**
         * @since 1.15.0
         */
        V1_16_2(Compatibility.NOT_SUPPORTED),
        /**
         * @since 1.15.1
         */
        V1_16_3(Compatibility.NOT_SUPPORTED),

        /**
         * @since 1.15.3
         */
        V1_16_4(Compatibility.COMPATIBLE),

        /**
         * @since 1.16.0
         */
        V1_16_5(Compatibility.COMPATIBLE)
        ;

        private Compatibility compatibility;

        ServerVersion(Compatibility compatibility) {
            this.compatibility = compatibility;
        }

        public Compatibility getCompatibility() {
            return compatibility;
        }

        @Override
        @NonNull
        public String toString() {
            return super.toString().substring(1).replace("_", ".");
        }

        /**
         * @since 1.5.0
         */
        @NonNull
        public static List<ServerVersion> getVersions(@NonNull Compatibility... compatibility) {
            List<ServerVersion> versions = new LinkedList<>();
            for (ServerVersion version : values()) {
                if (Arrays.asList(compatibility).contains(version.getCompatibility())) {
                    versions.add(version);
                }
            }
            return versions;
        }
    }

    /**
     * Checks the compatibility with the current server software and returns the {@link Compatibility}.
     * Note this is a one-time calculation: further calls won't change the result.
     * @return the {@link Compatibility}.
     */
    public Compatibility checkCompatibility() {
        if (result == null) {
            // Check the server version first
            ServerVersion version = getServerVersion();

            if (version == null || version.getCompatibility().equals(Compatibility.INCOMPATIBLE)) {
                // 'Version = null' means that it's not listed. And therefore, it's implicitly incompatible.
                result = Compatibility.INCOMPATIBLE;
                return result;
            }

            // Now, check the server software
            ServerSoftware software = getServerSoftware();

            if (software.getCompatibility().equals(Compatibility.INCOMPATIBLE)) {
                result = Compatibility.INCOMPATIBLE;
                return result;
            }

            if (software.getCompatibility().equals(Compatibility.NOT_SUPPORTED) || version.getCompatibility().equals(Compatibility.NOT_SUPPORTED)) {
                result = Compatibility.NOT_SUPPORTED;
                return result;
            }

            if (software.getCompatibility().equals(Compatibility.SUPPORTED) || version.getCompatibility().equals(Compatibility.SUPPORTED)) {
                result = Compatibility.SUPPORTED;
                return result;
            }

            // Nothing's wrong, the server is compatible.
            result = Compatibility.COMPATIBLE;
            return result;
        }

        return result;
    }

    /**
     * Returns the {@link ServerSoftware} entry corresponding to the current server software, may be null.
     * @return the {@link ServerSoftware} run by this server or null.
     * @since 1.3.0
     */
    @NonNull
    public ServerSoftware getServerSoftware() {
        String[] parts = Bukkit.getServer().getVersion().split("-");
        if (parts.length < 2) {
            return ServerSoftware.UNKNOWN.setName(Bukkit.getServer().getVersion().toUpperCase(Locale.ENGLISH));
        }
        String serverSoftware = Bukkit.getServer().getVersion().split("-")[1];
        try {
            return ServerSoftware.valueOf(serverSoftware.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return ServerSoftware.UNKNOWN.setName(serverSoftware.toUpperCase(Locale.ENGLISH));
        }
    }

    /**
     * Returns the {@link ServerVersion} entry corresponding to the current server software, may be null.
     * @return the {@link ServerVersion} run by this server or null.
     * @since 1.3.0
     */
    @Nullable
    public ServerVersion getServerVersion() {
        String serverVersion = Bukkit.getServer().getBukkitVersion().split("-")[0].replace(".", "_");
        try {
            return ServerVersion.valueOf("V" + serverVersion.toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Returns whether the server runs on the specified versions.
     * @param versions the {@link ServerVersion}s to check.
     * @return {@code true} if the server runs on one of the specified versions, {@code false} otherwise.
     * @since 1.5.0
     */
    public boolean isVersion(@NonNull ServerVersion... versions) {
        return Arrays.asList(versions).contains(getServerVersion());
    }

    /**
     * Returns whether the server runs on the specified softwares.
     * @param softwares the {@link ServerSoftware}s to check.
     * @return {@code true} if the server runs on on of these softwares, {@code false} otherwise.
     * @since 1.5.0
     */
    public boolean isSoftware(@NonNull ServerSoftware... softwares) {
        return Arrays.asList(softwares).contains(getServerSoftware());
    }
}
