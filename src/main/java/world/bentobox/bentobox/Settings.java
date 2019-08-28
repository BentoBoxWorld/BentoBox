package world.bentobox.bentobox;

import java.util.HashSet;
import java.util.Set;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;

/**
 * All the plugin settings are here
 * @author tastybento
 */
@StoreAt(filename="config.yml") // Explicitly call out what name this should have.
@ConfigComment("BentoBox Configuration [version]")
@ConfigComment("This config file is dynamic and is updated right after BentoBox loaded its settings from it.")
@ConfigComment("You can edit it while the server is online and you can do '/bbox reload' to take the changes into account.")
@ConfigComment("However, it is a better practice to edit this file while the server is offline.")
public class Settings implements ConfigObject {

    // ---------------------------------------------

    /*      GENERAL     */
    @ConfigComment("Default language for new players.")
    @ConfigComment("This is the filename in the locale folder without .yml.")
    @ConfigComment("If this does not exist, the default en-US will be used.")
    @ConfigEntry(path = "general.default-language")
    private String defaultLanguage = "en-US";

    @ConfigComment("Use economy or not. If true, an economy plugin is required. If false, no money is used or give.")
    @ConfigComment("If there is no economy plugin present anyway, money will be automatically disabled.")
    @ConfigEntry(path = "general.use-economy")
    private boolean useEconomy = true;

    // Database
    @ConfigComment("JSON, MYSQL, MARIADB (10.2.3+), MONGODB, SQLITE, POSTGRESQL and YAML(deprecated).")
    @ConfigComment("Transition database options are:")
    @ConfigComment("  YAML2JSON, YAML2MARIADB, YAML2MYSQL, YAML2MONGODB, YAML2SQLITE")
    @ConfigComment("  JSON2MARIADB, JSON2MYSQL, JSON2MONGODB, JSON2SQLITE, JSON2POSTGRESQL")
    @ConfigComment("  MYSQL2JSON, MARIADB2JSON, MONGODB2JSON, SQLITE2JSON, POSTGRESQL2JSON")
    @ConfigComment("If you need others, please make a feature request.")
    @ConfigComment("Transition options enable migration from one database type to another. Use /bbox migrate.")
    @ConfigComment("YAML and JSON are file-based databases.")
    @ConfigComment("MYSQL might not work with all implementations: if available, use a dedicated database type (e.g. MARIADB).")
    @ConfigComment("If you use MONGODB, you must also run the BSBMongo plugin (not addon).")
    @ConfigComment("See https://github.com/tastybento/bsbMongo/releases/.")
    @ConfigEntry(path = "general.database.type", video = "https://youtu.be/FFzCk5-y7-g")
    private DatabaseType databaseType = DatabaseType.JSON;

    @ConfigEntry(path = "general.database.host")
    private String databaseHost = "localhost";

    @ConfigComment("Port 3306 is MySQL's default. Port 27017 is MongoDB's default.")
    @ConfigEntry(path = "general.database.port")
    private int databasePort = 3306;

    @ConfigEntry(path = "general.database.name")
    private String databaseName = "bentobox";

    @ConfigEntry(path = "general.database.username")
    private String databaseUsername = "username";

    @ConfigEntry(path = "general.database.password")
    private String databasePassword = "password";

    @ConfigComment("How often the data will be saved to file in mins. Default is 5 minutes.")
    @ConfigComment("This helps prevent issues if the server crashes.")
    @ConfigComment("Data is also saved at important points in the game.")
    @ConfigEntry(path = "general.database.backup-period")
    private int databaseBackupPeriod = 5;

    @ConfigComment("Allow FTB Autonomous Activator to work (will allow a pseudo player [CoFH] to place and break blocks and hang items)")
    @ConfigComment("Add other fake player names here if required")
    @ConfigEntry(path = "general.fakeplayers", experimental = true)
    private Set<String> fakePlayers = new HashSet<>();

    @ConfigEntry(path = "panel.close-on-click-outside")
    private boolean closePanelOnClickOutside = true;

    /*
     * Logs
     */
    @ConfigComment("Toggle whether superflat chunks regeneration should be logged in the server logs or not.")
    @ConfigComment("It can be spammy if there are a lot of superflat chunks to regenerate.")
    @ConfigComment("However, as superflat chunks regeneration can be performance-intensive, it is recommended to keep")
    @ConfigComment("this setting set to true, as it will help you know if there are regenerations taking place.")
    @ConfigEntry(path = "logs.clean-super-flat-chunks", since = "1.2.0")
    private boolean logCleanSuperFlatChunks = true;

    @ConfigComment("Toggle whether downloading data from GitHub should be logged in the server logs or not.")
    @ConfigEntry(path = "logs.github-download-data", since = "1.5.0")
    private boolean logGithubDownloadData = true;

    /*
     * Island
     */
    // Cooldowns
    @ConfigComment("How long a player must wait until they can rejoin a team island after being kicked in minutes.")
    @ConfigComment("This slows the effectiveness of players repeating challenges")
    @ConfigComment("by repetitively being invited to a team island.")
    @ConfigEntry(path = "island.cooldown.time.invite")
    private int inviteCooldown = 60;

    @ConfigComment("How long a player must wait until they can coop a player in minutes.")
    @ConfigEntry(path = "island.cooldown.time.coop")
    private int coopCooldown = 5;

    @ConfigComment("How long a player must wait until they can trust a player in minutes.")
    @ConfigEntry(path = "island.cooldown.time.trust")
    private int trustCooldown = 5;

    @ConfigComment("How long a player must wait until they can ban a player after unbanning them. In minutes.")
    @ConfigEntry(path = "island.cooldown.time.ban")
    private int banCooldown = 10;

    @ConfigComment("How long a player must wait before they can reset their island again in seconds.")
    @ConfigEntry(path = "island.cooldown.time.reset")
    private int resetCooldown = 300;

    @ConfigComment("Whether the reset cooldown should be applied when the player creates an island for the first time or not.")
    @ConfigEntry(path = "island.cooldown.options.set-reset-cooldown-on-create", since = "1.2.0")
    private boolean resetCooldownOnCreate = true;

    // Timeout for team kick and leave commands
    @ConfigComment("Time in seconds that players have to confirm sensitive commands, e.g. island reset.")
    @ConfigEntry(path = "island.confirmation.time")
    private int confirmationTime = 10;

    // Timeout for team kick and leave commands
    @ConfigComment("Time in seconds that players have to stand still before teleport commands activate, e.g. island go.")
    @ConfigEntry(path = "island.delay.time")
    private int delayTime = 0;

    @ConfigComment("Ask the player to confirm the command he is using by typing it again.")
    @ConfigEntry(path = "island.confirmation.commands.kick")
    private boolean kickConfirmation = true;

    @ConfigEntry(path = "island.confirmation.commands.leave")
    private boolean leaveConfirmation = true;

    @ConfigEntry(path = "island.confirmation.commands.reset")
    private boolean resetConfirmation = true;

    @ConfigComment("These set the minimum and maximum size of a name.")
    @ConfigEntry(path = "island.name.min-length")
    private int nameMinLength = 4;
    @ConfigEntry(path = "island.name.max-length")
    private int nameMaxLength = 20;

    @ConfigComment("Remove hostile mob on teleport box radius")
    @ConfigComment("If hostile mobs are cleared on player teleport, then this sized box will be cleared")
    @ConfigComment("around the player. e.g. 5 means a 10 x 10 x 10 box around the player")
    @ConfigComment("Be careful not to make this too big. Does not cover standard nether or end teleports.")
    @ConfigEntry(path = "island.clear-radius", since = "1.6.0")
    private int clearRadius = 5;

    @ConfigComment("Number of blocks to paste per tick when pasting blueprints")
    @ConfigComment("Smaller values will help reduce noticeable lag but will make pasting take longer")
    @ConfigEntry(path = "island.paste-speed")
    private int pasteSpeed = 1000;

    // Automated ownership transfer
    @ConfigComment("Toggles the automated ownership transfer.")
    @ConfigComment("It automatically transfers the ownership of an island to one of its members in case the current owner is inactive.")
    @ConfigComment("More precisely, it transfers the ownership of the island to the player who's active, whose rank is the highest")
    @ConfigComment("and who's been part of the island the longest time.")
    @ConfigComment("Setting this to 'false' will disable the feature.")
    @ConfigEntry(path = "island.automated-ownership-transfer.enable", hidden = true)
    private boolean enableAutoOwnershipTransfer = false;

    @ConfigComment("Time in days since the island owner's last disconnection before they are considered inactive.")
    @ConfigEntry(path = "island.automated-ownership-transfer.inactivity-threshold", hidden = true)
    private int autoOwnershipTransferInactivityThreshold = 30;

    @ConfigComment("Ranks are being considered when transferring the island ownership to one of its member.")
    @ConfigComment("Ignoring ranks will result in the island ownership being transferred to the player who's active and")
    @ConfigComment("who's been member of the island the longest time.")
    @ConfigEntry(path = "island.automated-ownership-transfer.ignore-ranks", hidden = true)
    private boolean autoOwnershipTransferIgnoreRanks = false;

    /* WEB */
    @ConfigComment("BentoBox uses bStats.org to get global data about the plugin to help improving it.")
    @ConfigComment("bStats has nearly no effect on your server's performance and the sent data is completely")
    @ConfigComment("anonymous so please consider twice if you really want to disable it.")
    @ConfigEntry(path = "web.metrics")
    private boolean metrics = true;

    @ConfigComment("Toggle whether BentoBox can connect to GitHub to get data about updates and addons.")
    @ConfigComment("Disabling this will result in the deactivation of the update checker and of some other")
    @ConfigComment("features that rely on the data downloaded from the GitHub API.")
    @ConfigComment("It does not send any data.")
    @ConfigEntry(path = "web.github.download-data", since = "1.5.0")
    private boolean githubDownloadData = true;

    @ConfigComment("Time in minutes between each connection to the GitHub API.")
    @ConfigComment("This allows for up-to-the-minute information gathering.")
    @ConfigComment("However, as the GitHub API data does not get updated instantly, this value cannot be set to less than 60 minutes.")
    @ConfigComment("Setting this to 0 will make BentoBox download data only at startup.")
    @ConfigEntry(path = "web.github.connection-interval", since = "1.5.0")
    private int githubConnectionInterval = 120;

    @ConfigEntry(path = "web.updater.check-updates.bentobox", since = "1.3.0", hidden = true)
    private boolean checkBentoBoxUpdates = true;

    @ConfigEntry(path = "web.updater.check-updates.addons", since = "1.3.0", hidden = true)
    private boolean checkAddonsUpdates = true;

    //---------------------------------------------------------------------------------------/
    // Getters and setters

    public boolean isMetrics() {
        return metrics;
    }

    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public boolean isUseEconomy() {
        return useEconomy;
    }

    public void setUseEconomy(boolean useEconomy) {
        this.useEconomy = useEconomy;
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(DatabaseType databaseType) {
        this.databaseType = databaseType;
    }

    public String getDatabaseHost() {
        return databaseHost;
    }

    public void setDatabaseHost(String databaseHost) {
        this.databaseHost = databaseHost;
    }

    public int getDatabasePort() {
        return databasePort;
    }

    public void setDatabasePort(int databasePort) {
        this.databasePort = databasePort;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public int getDatabaseBackupPeriod() {
        return databaseBackupPeriod;
    }

    public void setDatabaseBackupPeriod(int databaseBackupPeriod) {
        this.databaseBackupPeriod = databaseBackupPeriod;
    }

    public Set<String> getFakePlayers() {
        return fakePlayers;
    }

    public void setFakePlayers(Set<String> fakePlayers) {
        this.fakePlayers = fakePlayers;
    }

    public boolean isClosePanelOnClickOutside() {
        return closePanelOnClickOutside;
    }

    public void setClosePanelOnClickOutside(boolean closePanelOnClickOutside) {
        this.closePanelOnClickOutside = closePanelOnClickOutside;
    }

    public int getInviteCooldown() {
        return inviteCooldown;
    }

    public void setInviteCooldown(int inviteCooldown) {
        this.inviteCooldown = inviteCooldown;
    }

    public int getCoopCooldown() {
        return coopCooldown;
    }

    public void setCoopCooldown(int coopCooldown) {
        this.coopCooldown = coopCooldown;
    }

    public int getTrustCooldown() {
        return trustCooldown;
    }

    public void setTrustCooldown(int trustCooldown) {
        this.trustCooldown = trustCooldown;
    }

    public int getBanCooldown() {
        return banCooldown;
    }

    public void setBanCooldown(int banCooldown) {
        this.banCooldown = banCooldown;
    }

    public int getResetCooldown() {
        return resetCooldown;
    }

    public void setResetCooldown(int resetCooldown) {
        this.resetCooldown = resetCooldown;
    }

    public int getConfirmationTime() {
        return confirmationTime;
    }

    public void setConfirmationTime(int confirmationTime) {
        this.confirmationTime = confirmationTime;
    }

    public boolean isKickConfirmation() {
        return kickConfirmation;
    }

    public void setKickConfirmation(boolean kickConfirmation) {
        this.kickConfirmation = kickConfirmation;
    }

    public boolean isLeaveConfirmation() {
        return leaveConfirmation;
    }

    public void setLeaveConfirmation(boolean leaveConfirmation) {
        this.leaveConfirmation = leaveConfirmation;
    }

    public boolean isResetConfirmation() {
        return resetConfirmation;
    }

    public void setResetConfirmation(boolean resetConfirmation) {
        this.resetConfirmation = resetConfirmation;
    }

    public int getNameMinLength() {
        return nameMinLength;
    }

    public void setNameMinLength(int nameMinLength) {
        this.nameMinLength = nameMinLength;
    }

    public int getNameMaxLength() {
        return nameMaxLength;
    }

    public void setNameMaxLength(int nameMaxLength) {
        this.nameMaxLength = nameMaxLength;
    }

    /**
     * @param pasteSpeed the pasteSpeed to set
     */
    public void setPasteSpeed(int pasteSpeed) {
        this.pasteSpeed = pasteSpeed;
    }

    /**
     * @return paste speed in blocks per tick
     */
    public int getPasteSpeed() {
        return this.pasteSpeed;
    }

    public boolean isEnableAutoOwnershipTransfer() {
        return enableAutoOwnershipTransfer;
    }

    public void setEnableAutoOwnershipTransfer(boolean enableAutoOwnershipTransfer) {
        this.enableAutoOwnershipTransfer = enableAutoOwnershipTransfer;
    }

    public int getAutoOwnershipTransferInactivityThreshold() {
        return autoOwnershipTransferInactivityThreshold;
    }

    public void setAutoOwnershipTransferInactivityThreshold(int autoOwnershipTransferInactivityThreshold) {
        this.autoOwnershipTransferInactivityThreshold = autoOwnershipTransferInactivityThreshold;
    }

    public boolean isAutoOwnershipTransferIgnoreRanks() {
        return autoOwnershipTransferIgnoreRanks;
    }

    public void setAutoOwnershipTransferIgnoreRanks(boolean autoOwnershipTransferIgnoreRanks) {
        this.autoOwnershipTransferIgnoreRanks = autoOwnershipTransferIgnoreRanks;
    }

    public boolean isLogCleanSuperFlatChunks() {
        return logCleanSuperFlatChunks;
    }

    public void setLogCleanSuperFlatChunks(boolean logCleanSuperFlatChunks) {
        this.logCleanSuperFlatChunks = logCleanSuperFlatChunks;
    }

    public boolean isResetCooldownOnCreate() {
        return resetCooldownOnCreate;
    }

    public void setResetCooldownOnCreate(boolean resetCooldownOnCreate) {
        this.resetCooldownOnCreate = resetCooldownOnCreate;
    }

    public boolean isGithubDownloadData() {
        return githubDownloadData;
    }

    public void setGithubDownloadData(boolean githubDownloadData) {
        this.githubDownloadData = githubDownloadData;
    }

    public int getGithubConnectionInterval() {
        return githubConnectionInterval;
    }

    public void setGithubConnectionInterval(int githubConnectionInterval) {
        this.githubConnectionInterval = githubConnectionInterval;
    }

    public boolean isCheckBentoBoxUpdates() {
        return checkBentoBoxUpdates;
    }

    public void setCheckBentoBoxUpdates(boolean checkBentoBoxUpdates) {
        this.checkBentoBoxUpdates = checkBentoBoxUpdates;
    }

    public boolean isCheckAddonsUpdates() {
        return checkAddonsUpdates;
    }

    public void setCheckAddonsUpdates(boolean checkAddonsUpdates) {
        this.checkAddonsUpdates = checkAddonsUpdates;
    }

    public boolean isLogGithubDownloadData() {
        return logGithubDownloadData;
    }

    public void setLogGithubDownloadData(boolean logGithubDownloadData) {
        this.logGithubDownloadData = logGithubDownloadData;
    }

    public int getDelayTime() {
        return delayTime;
    }

    /**
     * @param delayTime the delayTime to set
     */
    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    /**
     * @return the clearRadius
     */
    public int getClearRadius() {
        if (clearRadius < 0) clearRadius = 0;
        return clearRadius;
    }

    /**
     * @param clearRadius the clearRadius to set. Cannot be negative.
     */
    public void setClearRadius(int clearRadius) {
        if (clearRadius < 0) clearRadius = 0;
        this.clearRadius = clearRadius;
    }


}