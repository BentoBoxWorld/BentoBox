package us.tastybento.bskyblock;

/**
 * Contains the plugin constants.
 * @author tastybento
 */
public class Constants {
    // ----------------- Constants -----------------
    // Game Type BSKYBLOCK or ACIDISLAND
    public enum GameType {
        BSKYBLOCK, ACIDISLAND, BOTH
    }

    public static final GameType GAMETYPE = GameType.BSKYBLOCK;
    // Permission prefix
    public static final String PERMPREFIX = "bskyblock.";
    // The island command
    public static final String ISLANDCOMMAND = "island";
    // The spawn command (Essentials spawn for example)
    public static final String SPAWNCOMMAND = "spawn";
    // Admin command
    public static final String ADMINCOMMAND = "bsadmin";

}