package world.bentobox.bentobox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.StoreAt;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;
import world.bentobox.bentobox.database.objects.DataObject;
import world.bentobox.bentobox.managers.RanksManager;

/**
 * All the plugin settings are here
 * @author tastybento
 */
@StoreAt(filename="config.yml") // Explicitly call out what name this should have.
@ConfigComment("BentoBox Configuration [version]")
@ConfigComment("This config file is dynamic and saved when the server is shutdown.")
@ConfigComment("You cannot edit it while the server is running because changes will")
@ConfigComment("be lost! Use in-game settings GUI or edit when server is offline.")
public class Settings implements DataObject {

    // ---------------------------------------------

    /*      GENERAL     */
    @ConfigComment("BentoBox uses bStats.org to get global data about the plugin to help improving it.")
    @ConfigComment("bStats has nearly no effect on your server's performance and the sent data is completely")
    @ConfigComment("anonymous so please consider twice if you really want to disable it.")
    @ConfigEntry(path = "general.metrics")
    private boolean metrics = true;

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
    @ConfigComment("YAML, JSON, MYSQL, MONGODB.")
    @ConfigComment("YAML and JSON are both file-based databases.")
    @ConfigComment("If you use MONGODB, you must also run the BSBMongo plugin (not addon).")
    @ConfigComment("See https://github.com/tastybento/bsbMongo/releases/.")
    @ConfigEntry(path = "general.database.type")
    private DatabaseType databaseType = DatabaseType.YAML;

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

    @ConfigComment("Rank required to use a command. e.g., use the invite command. Default is owner rank is required.")
    @ConfigEntry(path = "general.rank-command", experimental = true)
    private Map<String, Integer> rankCommand = new HashMap<>();

    @ConfigEntry(path = "panel.close-on-click-outside")
    private boolean closePanelOnClickOutside = true;

    /*
     * Island
     */
    // Cooldowns
    @ConfigComment("How long a player must wait until they can rejoin a team island after being")
    @ConfigComment("kicked in minutes. This slows the effectiveness of players repeating challenges")
    @ConfigComment("by repetitively being invited to a team island.")
    @ConfigEntry(path = "island.cooldown.invite")
    private int inviteCooldown = 60;

    @ConfigComment("How long a player must wait until they can coop a player in minutes.")
    @ConfigEntry(path = "island.cooldown.coop")
    private int coopCooldown = 5;

    @ConfigComment("How long a player must wait until they can trust a player in minutes.")
    @ConfigEntry(path = "island.cooldown.trust")
    private int trustCooldown = 5;

    @ConfigComment("How long a player must wait until they can ban a player.")
    @ConfigComment("after unbanning them. In minutes.")
    @ConfigEntry(path = "island.cooldown.ban")
    private int banCooldown = 10;

    @ConfigComment("How long a player must wait before they can reset their island again in seconds.")
    @ConfigEntry(path = "island.cooldown.reset")
    private int resetCooldown = 300;

    // Timeout for team kick and leave commands
    @ConfigComment("Time in seconds that players have to confirm sensitive commands, e.g. island reset")
    @ConfigEntry(path = "island.confirmation.time")
    private int confirmationTime = 10;

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

    @ConfigComment("Number of blocks to paste per tick when pasting a schem")
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

    //---------------------------------------------------------------------------------------/
    @ConfigComment("These settings should not be edited")
    private String uniqueId = "config";

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

    public Map<String, Integer> getRankCommand() {
        return rankCommand;
    }

    public int getRankCommand(String command) {
        return rankCommand.getOrDefault(command, RanksManager.OWNER_RANK);
    }

    public void setRankCommand(String command, int rank) {
        rankCommand.put(command, rank);
    }

    public void setRankCommand(Map<String, Integer> rankCommand) {
        this.rankCommand = rankCommand;
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

    /**
     * @return the uniqueId
     */
    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    /**
     * @param uniqueId the uniqueId to set
     */
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}