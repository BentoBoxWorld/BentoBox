package world.bentobox.bentobox;

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
    // The spawn command (Essentials spawn for example)
    public static final String SPAWNCOMMAND = "spawn";
}