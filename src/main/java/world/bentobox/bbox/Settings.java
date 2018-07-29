package world.bentobox.bbox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import world.bentobox.bbox.api.configuration.ConfigComment;
import world.bentobox.bbox.api.configuration.ConfigEntry;
import world.bentobox.bbox.api.configuration.StoreAt;
import world.bentobox.bbox.database.BSBDbSetup.DatabaseType;
import world.bentobox.bbox.database.objects.DataObject;

/**
 * All the plugin settings are here
 * @author Tastybento
 */
@StoreAt(filename="config.yml") // Explicitly call out what name this should have.
@ConfigComment("BentoBox Configuration [version]")
@ConfigComment("This config file is dynamic and saved when the server is shutdown.")
@ConfigComment("You cannot edit it while the server is running because changes will")
@ConfigComment("be lost! Use in-game settings GUI or edit when server is offline.")
@ConfigComment("")
public class Settings implements DataObject {

    // ---------------------------------------------

    /*      GENERAL     */
    @ConfigComment("BentoBox uses bStats.org to get global data about the plugin to help improving it.")
    @ConfigComment("bStats has nearly no effect on your server's performance and the sent data is completely")
    @ConfigComment("anonymous so please consider twice if you really want to disable it.")
    @ConfigEntry(path = "general.metrics")
    private boolean metrics = true;

    @ConfigComment("Check for updates - this will tell Ops and the console if there is a new")
    @ConfigComment("version available. It contacts dev.bukkit.org to request the latest version")
    @ConfigComment("info. It does not download the latest version or change any files")
    @ConfigEntry(path = "general.check-updates")
    private boolean checkUpdates = true;

    @ConfigComment("Default language for new players.")
    @ConfigComment("This is the filename in the locale folder without .yml.")
    @ConfigComment("If this does not exist, the default en-US will be used.")
    @ConfigEntry(path = "general.default-language")
    private String defaultLanguage = "en-US";

    @ConfigComment("Use economy or not. If true, an economy plugin is required. If false, no money is used or give.")
    @ConfigComment("If there is no economy plugin present anyway, money will be automatically disabled.")
    @ConfigEntry(path = "general.use-economy")
    private boolean useEconomy = true;

    @ConfigComment("Starting money - this is how much money new players will have as their")
    @ConfigComment("balance at the start of an island.")
    @ConfigEntry(path = "general.starting-money")
    private double startingMoney = 10.0;

    // Purge
    @ConfigComment("Only islands below this level will be removed if they are abandoned and admins issue the purge command")
    @ConfigEntry(path = "general.purge.max-island-level")
    private int purgeMaxIslandLevel = 50;

    @ConfigComment("Remove user data when its island gets purged.")
    @ConfigComment("Helps a lot to avoid huge backups and can save some performance on startup,")
    @ConfigComment("but the player settings and data will be reset.")
    @ConfigEntry(path = "general.purge.remove-user-data")
    private boolean purgeRemoveUserData = false;

    // Database
    @ConfigComment("FLATFILE, MYSQL, MONGO")
    @ConfigComment("if you use MONGO, you must also run the BSBMongo plugin (not addon)")
    @ConfigComment("See https://github.com/tastybento/bsbMongo/releases/")
    @ConfigEntry(path = "general.database.type")
    private DatabaseType databaseType = DatabaseType.FLATFILE;

    @ConfigEntry(path = "general.database.host")
    private String dbHost = "localhost";

    @ConfigComment("Port 3306 is MySQL's default. Port 27017 is MongoDB's default.")
    @ConfigEntry(path = "general.database.port")
    private int dbPort = 3306;

    @ConfigEntry(path = "general.database.name")
    private String dbName = "bentobox";

    @ConfigEntry(path = "general.database.username")
    private String dbUsername = "username";

    @ConfigEntry(path = "general.database.password")
    private String dbPassword = "password";

    @ConfigComment("How often the data will be saved to file in mins. Default is 5 minutes.")
    @ConfigComment("This helps prevent issues if the server crashes.")
    @ConfigComment("Data is also saved at important points in the game.")
    @ConfigEntry(path = "general.database.backup-period")
    private int databaseBackupPeriod = 5;

    @ConfigComment("Recover super flat - if the generator does not run for some reason, you can get")
    @ConfigComment("super flat chunks (grass). To remove automatically, select this option. Turn off")
    @ConfigComment("if there are no more because it may cause lag.")
    @ConfigComment("This will regenerate any chunks with bedrock at y=0 when they are loaded")
    @ConfigEntry(path = "general.recover-super-flat")
    private boolean recoverSuperFlat = false;

    @ConfigComment("Mute death messages")
    @ConfigEntry(path = "general.mute-death-messages")
    private boolean muteDeathMessages = false;

    @ConfigComment("Allow FTB Autonomous Activator to work (will allow a pseudo player [CoFH] to place and break blocks and hang items)")
    @ConfigComment("Add other fake player names here if required")
    @ConfigEntry(path = "general.fakeplayers")
    private Set<String> fakePlayers = new HashSet<>();

    @ConfigComment("Allow obsidian to be scooped up with an empty bucket back into lava")
    @ConfigComment("This only works if there is a single block of obsidian (no obsidian within 10 blocks)")
    @ConfigComment("Recommendation is to keep this true so that newbies don't bother you or reset their")
    @ConfigComment("island unnecessarily.")
    @ConfigEntry(path = "general.allow-obsidian-scooping")
    private boolean allowObsidianScooping = true;

    @ConfigComment("Time in seconds that players have to confirm sensitive commands, e.g. island reset")
    @ConfigEntry(path = "general.confirmation-time")
    private int confirmationTime = 20;

    @ConfigEntry(path = "panel.close-on-click-outside")
    private boolean closePanelOnClickOutside = true;

    /*
     * Island
     */
    // Invites
    @ConfigComment("How long a player must wait until they can rejoin a team island after being")
    @ConfigComment("kicked in minutes. This slows the effectiveness of players repeating challenges")
    @ConfigComment("by repetitively being invited to a team island.")
    @ConfigEntry(path = "island.invite-wait")
    private int inviteWait = 60;

    // Timeout for team kick and leave commands
    @ConfigComment("Ask the player to confirm the command he is using by typing it again.")
    @ConfigComment("The 'wait' value is the number of seconds to wait for confirmation.")
    @ConfigEntry(path = "island.require-confirmation.kick")
    private boolean kickConfirmation = true;

    @ConfigEntry(path = "island.require-confirmation.kick-wait")
    private long kickWait = 10L;

    @ConfigEntry(path = "island.require-confirmation.leave")
    private boolean leaveConfirmation = true;

    @ConfigEntry(path = "island.require-confirmation.leave-wait")
    private long leaveWait = 10L;

    @ConfigEntry(path = "island.require-confirmation.reset")
    private boolean resetConfirmation = true;

    @ConfigComment("How long a player must wait before they can reset their island again in seconds")
    @ConfigEntry(path = "island.reset-wait")
    private long resetWait = 300;

    @ConfigComment("These set the minimum and maximum size of a name.")
    @ConfigEntry(path = "island.name.min-length")
    private int nameMinLength = 4;
    @ConfigEntry(path = "island.name.max-length")
    private int nameMaxLength = 20;


    // Ranks
    @ConfigEntry(path = "island.customranks")
    private Map<String, Integer> customRanks = new HashMap<>();


    //---------------------------------------------------------------------------------------/
    @ConfigComment("These settings should not be edited")
    private String uniqueId = "config";

    /**
     * @return the metrics
     */
    public boolean isMetrics() {
        return metrics;
    }

    /**
     * @return the checkUpdates
     */
    public boolean isCheckUpdates() {
        return checkUpdates;
    }

    /**
     * @return the defaultLanguage
     */
    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    /**
     * @return the useEconomy
     */
    public boolean isUseEconomy() {
        return useEconomy;
    }

    /**
     * @return the startingMoney
     */
    public double getStartingMoney() {
        return startingMoney;
    }

    /**
     * @return the purgeMaxIslandLevel
     */
    public int getPurgeMaxIslandLevel() {
        return purgeMaxIslandLevel;
    }

    /**
     * @return the purgeRemoveUserData
     */
    public boolean isPurgeRemoveUserData() {
        return purgeRemoveUserData;
    }

    /**
     * @return the databaseType
     */
    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    /**
     * @return the dbHost
     */
    public String getDbHost() {
        return dbHost;
    }

    /**
     * @return the dbPort
     */
    public int getDbPort() {
        return dbPort;
    }

    /**
     * @return the dbName
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * @return the dbUsername
     */
    public String getDbUsername() {
        return dbUsername;
    }

    /**
     * @return the dbPassword
     */
    public String getDbPassword() {
        return dbPassword;
    }

    /**
     * @return the databaseBackupPeriod
     */
    public int getDatabaseBackupPeriod() {
        return databaseBackupPeriod;
    }

    /**
     * @return the recoverSuperFlat
     */
    public boolean isRecoverSuperFlat() {
        return recoverSuperFlat;
    }

    /**
     * @return the muteDeathMessages
     */
    public boolean isMuteDeathMessages() {
        return muteDeathMessages;
    }

    /**
     * @return the fakePlayers
     */
    public Set<String> getFakePlayers() {
        return fakePlayers;
    }

    /**
     * @return the allowObsidianScooping
     */
    public boolean isAllowObsidianScooping() {
        return allowObsidianScooping;
    }

    /**
     * @return the confirmationTime
     */
    public int getConfirmationTime() {
        return confirmationTime;
    }

    /**
     * @return the closePanelOnClickOutside
     */
    public boolean isClosePanelOnClickOutside() {
        return closePanelOnClickOutside;
    }

    /**
     * @return the uniqueId
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @param metrics the metrics to set
     */
    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }

    /**
     * @param checkUpdates the checkUpdates to set
     */
    public void setCheckUpdates(boolean checkUpdates) {
        this.checkUpdates = checkUpdates;
    }

    /**
     * @param defaultLanguage the defaultLanguage to set
     */
    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    /**
     * @param useEconomy the useEconomy to set
     */
    public void setUseEconomy(boolean useEconomy) {
        this.useEconomy = useEconomy;
    }

    /**
     * @param startingMoney the startingMoney to set
     */
    public void setStartingMoney(double startingMoney) {
        this.startingMoney = startingMoney;
    }

    /**
     * @param purgeMaxIslandLevel the purgeMaxIslandLevel to set
     */
    public void setPurgeMaxIslandLevel(int purgeMaxIslandLevel) {
        this.purgeMaxIslandLevel = purgeMaxIslandLevel;
    }

    /**
     * @param purgeRemoveUserData the purgeRemoveUserData to set
     */
    public void setPurgeRemoveUserData(boolean purgeRemoveUserData) {
        this.purgeRemoveUserData = purgeRemoveUserData;
    }

    /**
     * @param databaseType the databaseType to set
     */
    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    /**
     * @param dbHost the dbHost to set
     */
    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    /**
     * @param dbPort the dbPort to set
     */
    public void setDbPort(int dbPort) {
        this.dbPort = dbPort;
    }

    /**
     * @param dbName the dbName to set
     */
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    /**
     * @param dbUsername the dbUsername to set
     */
    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    /**
     * @param dbPassword the dbPassword to set
     */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    /**
     * @param databaseBackupPeriod the databaseBackupPeriod to set
     */
    public void setDatabaseBackupPeriod(int databaseBackupPeriod) {
        this.databaseBackupPeriod = databaseBackupPeriod;
    }

    /**
     * @param recoverSuperFlat the recoverSuperFlat to set
     */
    public void setRecoverSuperFlat(boolean recoverSuperFlat) {
        this.recoverSuperFlat = recoverSuperFlat;
    }

    /**
     * @param muteDeathMessages the muteDeathMessages to set
     */
    public void setMuteDeathMessages(boolean muteDeathMessages) {
        this.muteDeathMessages = muteDeathMessages;
    }

    /**
     * @param fakePlayers the fakePlayers to set
     */
    public void setFakePlayers(Set<String> fakePlayers) {
        this.fakePlayers = fakePlayers;
    }

    /**
     * @param allowObsidianScooping the allowObsidianScooping to set
     */
    public void setAllowObsidianScooping(boolean allowObsidianScooping) {
        this.allowObsidianScooping = allowObsidianScooping;
    }

    /**
     * @param confirmationTime the confirmationTime to set
     */
    public void setConfirmationTime(int confirmationTime) {
        this.confirmationTime = confirmationTime;
    }

    /**
     * @param closePanelOnClickOutside the closePanelOnClickOutside to set
     */
    public void setClosePanelOnClickOutside(boolean closePanelOnClickOutside) {
        this.closePanelOnClickOutside = closePanelOnClickOutside;
    }

    /**
     * @param uniqueId the uniqueId to set
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    /**
     * @return the customRanks
     */
    public Map<String, Integer> getCustomRanks() {
        return customRanks;
    }

    /**
     * @param customRanks the customRanks to set
     */
    public void setCustomRanks(Map<String, Integer> customRanks) {
        this.customRanks = customRanks;
    }

    /**
     * @return the inviteWait
     */
    public int getInviteWait() {
        return inviteWait;
    }

    /**
     * @param inviteWait the inviteWait to set
     */
    public void setInviteWait(int inviteWait) {
        this.inviteWait = inviteWait;
    }

    /**
     * @return the kickConfirmation
     */
    public boolean isKickConfirmation() {
        return kickConfirmation;
    }

    /**
     * @return the kickWait
     */
    public long getKickWait() {
        return kickWait;
    }

    /**
     * @return the leaveConfirmation
     */
    public boolean isLeaveConfirmation() {
        return leaveConfirmation;
    }

    /**
     * @return the leaveWait
     */
    public long getLeaveWait() {
        return leaveWait;
    }

    /**
     * @param kickConfirmation the kickConfirmation to set
     */
    public void setKickConfirmation(boolean kickConfirmation) {
        this.kickConfirmation = kickConfirmation;
    }

    /**
     * @param kickWait the kickWait to set
     */
    public void setKickWait(long kickWait) {
        this.kickWait = kickWait;
    }

    /**
     * @param leaveConfirmation the leaveConfirmation to set
     */
    public void setLeaveConfirmation(boolean leaveConfirmation) {
        this.leaveConfirmation = leaveConfirmation;
    }

    /**
     * @param leaveWait the leaveWait to set
     */
    public void setLeaveWait(long leaveWait) {
        this.leaveWait = leaveWait;
    }

    /**
     * @return the resetWait
     */
    public long getResetWait() {
        return resetWait;
    }

    /**
     * @param resetWait the resetWait to set
     */
    public void setResetWait(long resetWait) {
        this.resetWait = resetWait;
    }

    /**
     * @return the resetConfirmation
     */
    public boolean isResetConfirmation() {
        return resetConfirmation;
    }

    /**
     * @param resetConfirmation the resetConfirmation to set
     */
    public void setResetConfirmation(boolean resetConfirmation) {
        this.resetConfirmation = resetConfirmation;
    }

    /**
     * @return the nameMinLength
     */
    public int getNameMinLength() {
        return nameMinLength;
    }

    /**
     * @return the nameMaxLength
     */
    public int getNameMaxLength() {
        return nameMaxLength;
    }

    /**
     * @param nameMinLength the nameMinLength to set
     */
    public void setNameMinLength(int nameMinLength) {
        this.nameMinLength = nameMinLength;
    }

    /**
     * @param nameMaxLength the nameMaxLength to set
     */
    public void setNameMaxLength(int nameMaxLength) {
        this.nameMaxLength = nameMaxLength;
    }

    // Getters and setters


}