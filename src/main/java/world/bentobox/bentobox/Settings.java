package world.bentobox.bentobox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;

import com.google.common.collect.ImmutableList;

import world.bentobox.bentobox.api.configuration.ConfigComment;
import world.bentobox.bentobox.api.configuration.ConfigEntry;
import world.bentobox.bentobox.api.configuration.ConfigObject;
import world.bentobox.bentobox.api.configuration.StoreAt;
import world.bentobox.bentobox.database.DatabaseSetup.DatabaseType;

/**
 * All the plugin settings are here
 *
 * @author tastybento
 */
@StoreAt(filename = "config.yml") // Explicitly call out what name this should have.
@ConfigComment("BentoBox v[version] configuration file.")
@ConfigComment("")
public class Settings implements ConfigObject {

	/* GENERAL */
	@ConfigComment("Default language for new players.")
	@ConfigComment("This is the filename in the locale folder without .yml.")
	@ConfigComment("If this does not exist, the default en-US will be used.")
	@ConfigEntry(path = "general.default-language")
	private String defaultLanguage = "en-US";

	@ConfigComment("Use economy or not. If true, an economy plugin is required. If false, no money is used or given.")
	@ConfigComment("If there is no economy plugin present anyway, money will be automatically disabled.")
	@ConfigEntry(path = "general.use-economy")
	private boolean useEconomy = true;

	/* COMMANDS */
	@ConfigComment("Console commands to run when BentoBox has loaded all worlds and addons.")
	@ConfigComment("Commands are run as the console.")
	@ConfigComment("e.g. set aliases for worlds in Multiverse here, or anything you need to")
	@ConfigComment("run after the plugin is fully loaded.")
	@ConfigEntry(path = "general.ready-commands", since = "1.24.2")
	private List<String> readyCommands = new ArrayList<>();

	// Database
	@ConfigComment("JSON, MYSQL, MARIADB, MONGODB, SQLITE, and POSTGRESQL.")
	@ConfigComment("Transition database options are:")
	@ConfigComment("  JSON2MARIADB, JSON2MYSQL, JSON2MONGODB, JSON2SQLITE, JSON2POSTGRESQL")
	@ConfigComment("  MYSQL2JSON, MARIADB2JSON, MONGODB2JSON, SQLITE2JSON, POSTGRESQL2JSON")
	@ConfigComment("If you need others, please make a feature request.")
	@ConfigComment("Minimum required versions:")
	@ConfigComment("   MySQL versions 5.7 or later")
	@ConfigComment("   MariaDB versions 10.2.3 or later")
	@ConfigComment("   MongoDB versions 3.6 or later")
	@ConfigComment("   SQLite versions 3.28 or later")
	@ConfigComment("   PostgreSQL versions 9.4 or later")
	@ConfigComment("Transition options enable migration from one database type to another. Use /bbox migrate.")
	@ConfigComment("JSON is a file-based database.")
	@ConfigComment("MYSQL might not work with all implementations: if available, use a dedicated database type (e.g. MARIADB).")
	@ConfigComment("BentoBox uses HikariCP for connecting with SQL databases.")
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

	@ConfigComment("How many players will be saved in one tick. Default is 200")
	@ConfigComment("Reduce if you experience lag while saving.")
	@ConfigComment("Do not set this too low or data might get lost!")
	@ConfigEntry(path = "general.database.max-saved-players-per-tick")
	private int maxSavedPlayersPerTick = 20;

	@ConfigComment("How many islands will be saved in one tick. Default is 200")
	@ConfigComment("Reduce if you experience lag while saving.")
	@ConfigComment("Do not set this too low or data might get lost!")
	@ConfigEntry(path = "general.database.max-saved-islands-per-tick")
	private int maxSavedIslandsPerTick = 20;

	@ConfigComment("Number of active connections to the SQL database at the same time.")
	@ConfigComment("Default 10.")
	@ConfigEntry(path = "general.database.max-pool-size", since = "1.21.0")
	private int maximumPoolSize = 10;

	@ConfigComment("Enable SSL connection to MongoDB, MariaDB, MySQL and PostgreSQL databases.")
	@ConfigEntry(path = "general.database.use-ssl", since = "1.12.0")
	private boolean useSSL = false;

	@ConfigComment("Database table prefix. Adds a prefix to the database tables. Not used by flatfile databases.")
	@ConfigComment("Only the characters A-Z, a-z, 0-9 can be used. Invalid characters will become an underscore.")
	@ConfigComment("Set this to a unique value if you are running multiple BentoBox instances that share a database.")
	@ConfigComment("Be careful about length - databases usually have a limit of 63 characters for table lengths")
	@ConfigEntry(path = "general.database.prefix-character", since = "1.13.0")
	private String databasePrefix = "";

	@ConfigComment("Custom connection datasource properties that will be applied to connection pool.")
	@ConfigComment("Check available values to your SQL driver implementation.")
	@ConfigComment("Example: ")
	@ConfigComment("  custom-properties: ")
	@ConfigComment("    cachePrepStmts: 'true'")
	@ConfigComment("    prepStmtCacheSize: '250'")
	@ConfigComment("    prepStmtCacheSqlLimit: '2048'")
	@ConfigEntry(path = "general.database.custom-properties", since = "1.21.0")
	private Map<String, String> customPoolProperties = new HashMap<>();

	@ConfigComment("MongoDB client connection URI to override default connection options.")
	@ConfigComment("See: https://docs.mongodb.com/manual/reference/connection-string/")
	@ConfigEntry(path = "general.database.mongodb-connection-uri", since = "1.14.0")
	private String mongodbConnectionUri = "";

	@ConfigComment("Allow FTB Autonomous Activator to work (will allow a pseudo player [CoFH] to place and break blocks and hang items)")
	@ConfigComment("Add other fake player names here if required")
	@ConfigEntry(path = "general.fakeplayers", experimental = true)
	private Set<String> fakePlayers = new HashSet<>();

	/* PANELS */

	@ConfigComment("Toggle whether panels should be closed or not when the player clicks anywhere outside of the inventory view.")
	@ConfigEntry(path = "panel.close-on-click-outside")
	private boolean closePanelOnClickOutside = true;

	@ConfigComment("Defines the Material of the item that fills the gaps (in the header, etc.) of most panels.")
	@ConfigEntry(path = "panel.filler-material", since = "1.14.0")
	private Material panelFillerMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

	@ConfigComment("Toggle whether player head texture should be gathered from Mojang API or mc-heads.net cache server.")
	@ConfigComment("Mojang API sometime may be slow and may limit requests to the player data, so this will allow to")
	@ConfigComment("get player heads a bit faster then Mojang API.")
	@ConfigEntry(path = "panel.use-cache-server", since = "1.16.0")
	private boolean useCacheServer = false;

	@ConfigComment("Defines how long player skin texture link is stored into local cache before it is requested again.")
	@ConfigComment("Defined value is in the minutes.")
	@ConfigComment("Value 0 will not clear cache until server restart.")
	@ConfigEntry(path = "panel.head-cache-time", since = "1.14.1")
	private long playerHeadCacheTime = 60;

	@ConfigComment("Defines a number of player heads requested per tasks.")
	@ConfigComment("Setting it too large may lead to temporarily being blocked from head gatherer API.")
	@ConfigEntry(path = "panel.heads-per-call", since = "1.16.0")
	private int headsPerCall = 9;

	@ConfigComment("Defines a number of ticks between each player head request task.")
	@ConfigComment("Setting it too large may lead to temporarily being blocked from head gatherer API.")
	@ConfigEntry(path = "panel.ticks-between-calls", since = "1.16.0", needsRestart = true)
	private long ticksBetweenCalls = 10;

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
	// Number of islands
	@ConfigComment("The default number of concurrent islands a player may have.")
	@ConfigComment("This may be overridden by individual game mode config settings.")
	@ConfigEntry(path = "island.concurrent-islands")
	private int islandNumber = 1;

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

	@ConfigComment("Ask the recipient to confirm trust or coop invites.")
	@ConfigComment("Team invites will always require confirmation, for safety concerns.")
	@ConfigEntry(path = "island.confirmation.invites", since = "1.8.0")
	private boolean inviteConfirmation = false;

	@ConfigComment("Sets the minimum length an island custom name is required to have.")
	@ConfigEntry(path = "island.name.min-length")
	private int nameMinLength = 4;
	@ConfigComment("Sets the maximum length an island custom name cannot exceed.")
	@ConfigEntry(path = "island.name.max-length")
	private int nameMaxLength = 20;
	@ConfigComment("Requires island custom names to be unique in the gamemode the island is in.")
	@ConfigComment("As a result, only one island per gamemode are allowed to share the same name.")
	@ConfigComment("Note that island names are purely cosmetics and are not used as a way to programmatically identify islands.")
	@ConfigEntry(path = "island.name.uniqueness", since = "1.7.0")
	private boolean nameUniqueness = false;

	@ConfigComment("Remove hostile mob on teleport box radius")
	@ConfigComment("If hostile mobs are cleared on player teleport, then this sized box will be cleared")
	@ConfigComment("around the player. e.g. 5 means a 10 x 10 x 10 box around the player")
	@ConfigComment("Be careful not to make this too big. Does not cover standard nether or end teleports.")
	@ConfigEntry(path = "island.clear-radius", since = "1.6.0")
	private int clearRadius = 5;

	@ConfigComment("Minimum nether portal search radius. If this is too low, duplicate portals may appear.")
	@ConfigComment("Vanilla default is 128.")
	@ConfigEntry(path = "island.portal-search-radius", since = "1.16.2")
	private int minPortalSearchRadius = 64;

	@ConfigComment("Number of blocks to paste per tick when pasting blueprints.")
	@ConfigComment("Smaller values will help reduce noticeable lag but will make pasting take slightly longer.")
	@ConfigComment("On the contrary, greater values will make pasting take less time, but this benefit is quickly severely impacted by the")
	@ConfigComment("resulting amount of chunks that must be loaded to fulfill the process, which often causes the server to hang out.")
	@ConfigEntry(path = "island.paste-speed")
	private int pasteSpeed = 64;

	@ConfigComment("Island deletion: Number of chunks per world to regenerate per second.")
	@ConfigComment("If there is a nether and end then 3x this number will be regenerated per second.")
	@ConfigComment("Smaller values will help reduce noticeable lag but will make deleting take longer.")
	@ConfigComment("A setting of 0 will leave island blocks (not recommended).")
	@ConfigEntry(path = "island.delete-speed", since = "1.7.0")
	private int deleteSpeed = 1;

	// Island deletion related settings
	@ConfigComment("Toggles whether islands, when players are resetting them, should be kept in the world or deleted.")
	@ConfigComment("* If set to 'true', whenever a player resets his island, his previous island will become unowned and won't be deleted from the world.")
	@ConfigComment("  You can, however, still delete those unowned islands through purging.")
	@ConfigComment("  On bigger servers, this can lead to an increasing world size.")
	@ConfigComment("  Yet, this allows admins to retrieve a player's old island in case of an improper use of the reset command.")
	@ConfigComment("  Admins can indeed re-add the player to his old island by registering him to it.")
	@ConfigComment("* If set to 'false', whenever a player resets his island, his previous island will be deleted from the world.")
	@ConfigComment("  This is the default behaviour.")
	@ConfigEntry(path = "island.deletion.keep-previous-island-on-reset", since = "1.13.0")
	private boolean keepPreviousIslandOnReset = false;

	@ConfigComment("Toggles how the islands are deleted.")
	@ConfigComment("* If set to 'false', all islands will be deleted at once.")
	@ConfigComment("  This is fast but may cause an impact on the performance")
	@ConfigComment("  as it'll load all the chunks of the in-deletion islands.")
	@ConfigComment("* If set to 'true', the islands will be deleted one by one.")
	@ConfigComment("  This is slower but will not cause any impact on the performance.")
	@ConfigEntry(path = "island.deletion.slow-deletion", since = "1.19.1")
	private boolean slowDeletion = false;

	@ConfigComment("By default, If the destination is not safe, the plugin will try to search for a safe spot around the destination,")
	@ConfigComment("then it will try to expand the y-coordinate up and down from the destination.")
	@ConfigComment("This setting limits how far the y-coordinate will be expanded.")
	@ConfigComment("If set to 0 or lower, the plugin will not expand the y-coordinate.")
	@ConfigEntry(path = "island.safe-spot-search-vertical-range", since = "1.19.1")
	private int safeSpotSearchVerticalRange = 400;

	@ConfigComment("By default, if the destination is not safe, the plugin will try to search for a safe spot around the destination.")
	@ConfigComment("This allows to change the distance for searching this spot. Larger value will mean longer position search.")
	@ConfigComment("This value is also used for valid nether portal linking between dimension.")
	@ConfigEntry(path = "island.safe-spot-search-range", since = "1.21.0")
	private int safeSpotSearchRange = 16;

	/* WEB */
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

	// ---------------------------------------------
	// Getters and setters

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

	/**
	 * This method returns the useSSL value.
	 * 
	 * @return the value of useSSL.
	 * @since 1.12.0
	 */
	public boolean isUseSSL() {
		return useSSL;
	}

	/**
	 * This method sets the useSSL value.
	 * 
	 * @param useSSL the useSSL new value.
	 * @since 1.12.0
	 */
	public void setUseSSL(boolean useSSL) {
		this.useSSL = useSSL;
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

	/**
	 * @since 1.15.3
	 */
	public int getMaxSavedPlayersPerTick() {
		return maxSavedPlayersPerTick;
	}

	/**
	 * @since 1.15.3
	 */
	public void setMaxSavedPlayersPerTick(int maxSavedPlayersPerTick) {
		this.maxSavedPlayersPerTick = maxSavedPlayersPerTick;
	}

	/**
	 * @since 1.15.3
	 */
	public int getMaxSavedIslandsPerTick() {
		return maxSavedIslandsPerTick;
	}

	/**
	 * @since 1.15.3
	 */
	public void setMaxSavedIslandsPerTick(int maxSavedIslandsPerTick) {
		this.maxSavedIslandsPerTick = maxSavedIslandsPerTick;
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
	 * @since 1.7.0
	 */
	public boolean isNameUniqueness() {
		return nameUniqueness;
	}

	/**
	 * @since 1.7.0
	 */
	public void setNameUniqueness(boolean nameUniqueness) {
		this.nameUniqueness = nameUniqueness;
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

	/**
	 * @return the deleteSpeed
	 * @since 1.7.0
	 */
	public int getDeleteSpeed() {
		return deleteSpeed;
	}

	/**
	 * @param deleteSpeed the deleteSpeed to set
	 * @since 1.7.0
	 */
	public void setDeleteSpeed(int deleteSpeed) {
		this.deleteSpeed = deleteSpeed;
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
		if (clearRadius < 0)
			clearRadius = 0;
		return clearRadius;
	}

	/**
	 * @param clearRadius the clearRadius to set. Cannot be negative.
	 */
	public void setClearRadius(int clearRadius) {
		if (clearRadius < 0)
			clearRadius = 0;
		this.clearRadius = clearRadius;
	}

	/**
	 * @return the inviteConfirmation
	 * @since 1.8.0
	 */
	public boolean isInviteConfirmation() {
		return inviteConfirmation;
	}

	/**
	 * @param inviteConfirmation the inviteConfirmation to set
	 * @since 1.8.0
	 */
	public void setInviteConfirmation(boolean inviteConfirmation) {
		this.inviteConfirmation = inviteConfirmation;
	}

	/**
	 * @return the databasePrefix
	 */
	public String getDatabasePrefix() {
		if (databasePrefix == null)
			databasePrefix = "";
		return databasePrefix.isEmpty() ? "" : databasePrefix.replaceAll("[^a-zA-Z0-9]", "_");
	}

	/**
	 * @param databasePrefix the databasePrefix to set
	 */
	public void setDatabasePrefix(String databasePrefix) {
		this.databasePrefix = databasePrefix;
	}

	/**
	 * Returns whether islands, when reset, should be kept or deleted.
	 * 
	 * @return {@code true} if islands, when reset, should be kept; {@code false}
	 *         otherwise.
	 * @since 1.13.0
	 */
	public boolean isKeepPreviousIslandOnReset() {
		return keepPreviousIslandOnReset;
	}

	/**
	 * Sets whether islands, when reset, should be kept or deleted.
	 * 
	 * @param keepPreviousIslandOnReset {@code true} if islands, when reset, should
	 *                                  be kept; {@code false} otherwise.
	 * @since 1.13.0
	 */
	public void setKeepPreviousIslandOnReset(boolean keepPreviousIslandOnReset) {
		this.keepPreviousIslandOnReset = keepPreviousIslandOnReset;
	}

	/**
	 * Returns a MongoDB client connection URI to override default connection
	 * options.
	 *
	 * @return mongodb client connection.
	 * @see <a href=
	 *      "https://docs.mongodb.com/manual/reference/connection-string/">MongoDB
	 *      Documentation</a>
	 * @since 1.14.0
	 */
	public String getMongodbConnectionUri() {
		return mongodbConnectionUri;
	}

	/**
	 * Set the MongoDB client connection URI.
	 * 
	 * @param mongodbConnectionUri connection URI.
	 * @since 1.14.0
	 */
	public void setMongodbConnectionUri(String mongodbConnectionUri) {
		this.mongodbConnectionUri = mongodbConnectionUri;
	}

	/**
	 * Returns the Material of the item to preferably use when one needs to fill
	 * gaps in Panels.
	 * 
	 * @return the Material of the item to preferably use when one needs to fill
	 *         gaps in Panels.
	 * @since 1.14.0
	 */
	public Material getPanelFillerMaterial() {
		return panelFillerMaterial;
	}

	/**
	 * Sets the Material of the item to preferably use when one needs to fill gaps
	 * in Panels.
	 * 
	 * @param panelFillerMaterial the Material of the item to preferably use when
	 *                            one needs to fill gaps in Panels.
	 * @since 1.14.0
	 */
	public void setPanelFillerMaterial(Material panelFillerMaterial) {
		this.panelFillerMaterial = panelFillerMaterial;
	}

	/**
	 * Method Settings#getPlayerHeadCacheTime returns the playerHeadCacheTime of
	 * this object.
	 *
	 * @return the playerHeadCacheTime (type long) of this object.
	 * @since 1.14.1
	 */
	public long getPlayerHeadCacheTime() {
		return playerHeadCacheTime;
	}

	/**
	 * Method Settings#setPlayerHeadCacheTime sets new value for the
	 * playerHeadCacheTime of this object.
	 * 
	 * @param playerHeadCacheTime new value for this object.
	 * @since 1.14.1
	 */
	public void setPlayerHeadCacheTime(long playerHeadCacheTime) {
		this.playerHeadCacheTime = playerHeadCacheTime;
	}

	/**
	 * Is use cache server boolean.
	 *
	 * @return the boolean
	 * @since 1.16.0
	 */
	public boolean isUseCacheServer() {
		return useCacheServer;
	}

	/**
	 * Sets use cache server.
	 *
	 * @param useCacheServer the use cache server
	 * @since 1.16.0
	 */
	public void setUseCacheServer(boolean useCacheServer) {
		this.useCacheServer = useCacheServer;
	}

	/**
	 * Gets heads per call.
	 *
	 * @return the heads per call
	 * @since 1.16.0
	 */
	public int getHeadsPerCall() {
		return headsPerCall;
	}

	/**
	 * Sets heads per call.
	 *
	 * @param headsPerCall the heads per call
	 * @since 1.16.0
	 */
	public void setHeadsPerCall(int headsPerCall) {
		this.headsPerCall = headsPerCall;
	}

	/**
	 * Gets ticks between calls.
	 *
	 * @return the ticks between calls
	 * @since 1.16.0
	 */
	public long getTicksBetweenCalls() {
		return ticksBetweenCalls;
	}

	/**
	 * Sets ticks between calls.
	 *
	 * @param ticksBetweenCalls the ticks between calls
	 * @since 1.16.0
	 */
	public void setTicksBetweenCalls(long ticksBetweenCalls) {
		this.ticksBetweenCalls = ticksBetweenCalls;
	}

	/**
	 * @return the minPortalSearchRadius
	 */
	public int getMinPortalSearchRadius() {
		return minPortalSearchRadius;
	}

	/**
	 * @param minPortalSearchRadius the minPortalSearchRadius to set
	 */
	public void setMinPortalSearchRadius(int minPortalSearchRadius) {
		this.minPortalSearchRadius = minPortalSearchRadius;
	}

	/**
	 * Gets safe spot search vertical range.
	 *
	 * @return the safe spot search vertical range
	 */
	public int getSafeSpotSearchVerticalRange() {
		return safeSpotSearchVerticalRange;
	}

	/**
	 * Sets safe spot search vertical range.
	 *
	 * @param safeSpotSearchVerticalRange the safe spot search vertical range
	 */
	public void setSafeSpotSearchVerticalRange(int safeSpotSearchVerticalRange) {
		this.safeSpotSearchVerticalRange = safeSpotSearchVerticalRange;
	}

	/**
	 * Is slow deletion boolean.
	 *
	 * @return the boolean
	 */
	public boolean isSlowDeletion() {
		return slowDeletion;
	}

	/**
	 * Sets slow deletion.
	 *
	 * @param slowDeletion the slow deletion
	 */
	public void setSlowDeletion(boolean slowDeletion) {
		this.slowDeletion = slowDeletion;
	}

	/**
	 * Gets maximum pool size.
	 *
	 * @return the maximum pool size
	 */
	public int getMaximumPoolSize() {
		return maximumPoolSize;
	}

	/**
	 * Gets safe spot search range.
	 *
	 * @return the safe spot search range
	 */
	public int getSafeSpotSearchRange() {
		return safeSpotSearchRange;
	}

	/**
	 * Sets maximum pool size.
	 *
	 * @param maximumPoolSize the maximum pool size
	 */
	public void setMaximumPoolSize(int maximumPoolSize) {
		this.maximumPoolSize = maximumPoolSize;
	}

	/**
	 * Gets custom pool properties.
	 *
	 * @return the custom pool properties
	 */
	public Map<String, String> getCustomPoolProperties() {
		return customPoolProperties;
	}

	/**
	 * Sets custom pool properties.
	 *
	 * @param customPoolProperties the custom pool properties
	 */
	public void setCustomPoolProperties(Map<String, String> customPoolProperties) {
		this.customPoolProperties = customPoolProperties;
	}

	/**
	 * Sets safe spot search range.
	 *
	 * @param safeSpotSearchRange the safe spot search range
	 */
	public void setSafeSpotSearchRange(int safeSpotSearchRange) {
		this.safeSpotSearchRange = safeSpotSearchRange;
	}

	/**
	 * @return an immutable list of readyCommands
	 */
	public List<String> getReadyCommands() {
		return ImmutableList.copyOf(Objects.requireNonNullElse(readyCommands, Collections.emptyList()));
	}

	/**
	 * @param readyCommands the readyCommands to set
	 */
	public void setReadyCommands(List<String> readyCommands) {
		this.readyCommands = readyCommands;
	}

	/**
	 * @return the islandNumber
	 * @since 2.0.0
	 */
	public int getIslandNumber() {
		return islandNumber;
	}

	/**
	 * @param islandNumber the islandNumber to set
	 * @since 2.0.0
	 */
	public void setIslandNumber(int islandNumber) {
		this.islandNumber = islandNumber;
	}

}
