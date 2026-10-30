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
@ConfigComment("BentoBox v[version] 配置文件。")
@ConfigComment("")
public class Settings implements ConfigObject {

    /* GENERAL */
    @ConfigComment("新玩家的默认语言。")
    @ConfigComment("这是 locale 文件夹中的文件名，不带 .yml 后缀。")
    @ConfigComment("如果该文件不存在，将使用默认的 en-US。")
    @ConfigEntry(path = "general.default-language")
    private String defaultLanguage = "en-US";

    @ConfigComment("是否使用经济系统。为 true 时需要经济插件；为 false 则不使用也不发放金钱。")
    @ConfigComment("如果服务器上根本没有经济插件，金钱功能会自动禁用。")
    @ConfigEntry(path = "general.use-economy")
    private boolean useEconomy = true;

    @ConfigComment("玩家重置岛屿时是否收取蓝图包费用。")
    @ConfigComment("为 false 时，只有创建岛屿会收费。默认 false。")
    @ConfigEntry(path = "general.charge-for-blueprint-on-reset")
    private boolean chargeForBlueprintOnReset = false;

    @ConfigComment("向 bStats 提交匿名、汇总的使用统计（https://bstats.org/plugin/bukkit/BentoBox/3555）。")
    @ConfigComment("不会发送任何个人数据 —— 详见 https://github.com/BentoBoxWorld/.github/blob/master/PRIVACY.md")
    @ConfigComment("设为 false 只会关闭 BentoBox 自己的统计；全局开关在")
    @ConfigComment("plugins/bStats/config.yml 中，可关闭服务器上所有插件的 bStats。")
    @ConfigComment("修改此项需要重启服务器。")
    @ConfigEntry(path = "general.metrics", since = "3.22.3")
    private boolean metrics = true;

    /* COMMANDS */
    @ConfigComment("BentoBox 加载完所有世界和 addon 后执行的命令。")
    @ConfigComment("命令以控制台身份执行。")
    @ConfigComment("例如在这里设置 Multiverse 的世界别名，或任何需要")
    @ConfigComment("在插件完全加载后执行的操作。")
    @ConfigEntry(path = "general.ready-commands", since = "1.24.2")
    private List<String> readyCommands = new ArrayList<>();

    // Database
    @ConfigComment("JSON、MYSQL、MARIADB、MONGODB、SQLITE、POSTGRESQL。")
    @ConfigComment("数据库迁移选项有：")
    @ConfigComment("  JSON2MARIADB、JSON2MYSQL、JSON2MONGODB、JSON2SQLITE、JSON2POSTGRESQL")
    @ConfigComment("  MYSQL2JSON、MARIADB2JSON、MONGODB2JSON、SQLITE2JSON、POSTGRESQL2JSON")
    @ConfigComment("如果需要其它组合，请提交功能请求。")
    @ConfigComment("最低版本要求：")
    @ConfigComment("   MySQL 5.7 或更高")
    @ConfigComment("   MariaDB 10.2.3 或更高")
    @ConfigComment("   MongoDB 3.6 或更高")
    @ConfigComment("   SQLite 3.28 或更高")
    @ConfigComment("   PostgreSQL 9.4 或更高")
    @ConfigComment("迁移选项用于从一种数据库类型迁移到另一种，使用 /bbox migrate 命令。")
    @ConfigComment("JSON 是基于文件的数据库。")
    @ConfigComment("MYSQL 可能不兼容所有实现：如果可用，请使用专用类型（例如 MARIADB）。")
    @ConfigComment("BentoBox 使用 HikariCP 连接 SQL 数据库。")
    @ConfigComment("如果使用 MONGODB，还必须安装 BSBMongo 插件（不是 addon）。")
    @ConfigComment("参见 https://github.com/tastybento/bsbMongo/releases/。")
    @ConfigEntry(path = "general.database.type", video = "https://youtu.be/FFzCk5-y7-g")
    private DatabaseType databaseType = DatabaseType.JSON;

    @ConfigEntry(path = "general.database.host")
    private String databaseHost = "localhost";

    @ConfigComment("3306 是 MySQL 的默认端口，27017 是 MongoDB 的默认端口。")
    @ConfigEntry(path = "general.database.port")
    private int databasePort = 3306;

    @ConfigEntry(path = "general.database.name")
    private String databaseName = "bentobox";

    @ConfigEntry(path = "general.database.username")
    private String databaseUsername = "username";

    @ConfigEntry(path = "general.database.password")
    private String databasePassword = "password";

    @ConfigComment("数据写入文件的间隔（分钟）。默认 5 分钟。")
    @ConfigComment("这有助于在服务器崩溃时减少损失。")
    @ConfigComment("游戏中的关键时点也会自动保存。")
    @ConfigEntry(path = "general.database.backup-period")
    private int databaseBackupPeriod = 5;

    @ConfigComment("每 tick 保存多少个玩家。默认 200")
    @ConfigComment("如果保存时出现卡顿，可以调小。")
    @ConfigComment("不要设得太小，否则可能丢数据！")
    @ConfigEntry(path = "general.database.max-saved-players-per-tick")
    private int maxSavedPlayersPerTick = 20;

    @ConfigComment("每 tick 保存多少座岛屿。默认 200")
    @ConfigComment("如果保存时出现卡顿，可以调小。")
    @ConfigComment("不要设得太小，否则可能丢数据！")
    @ConfigEntry(path = "general.database.max-saved-islands-per-tick")
    private int maxSavedIslandsPerTick = 20;

    @ConfigComment("同时与 SQL 数据库保持的活动连接数。")
    @ConfigComment("默认 10。")
    @ConfigEntry(path = "general.database.max-pool-size", since = "1.21.0")
    private int maximumPoolSize = 10;

    @ConfigComment("是否对 MongoDB、MariaDB、MySQL 和 PostgreSQL 数据库启用 SSL 连接。")
    @ConfigEntry(path = "general.database.use-ssl", since = "1.12.0")
    private boolean useSSL = false;

    @ConfigComment("数据库表前缀。会给数据库表名加上前缀。文件型数据库不使用此项。")
    @ConfigComment("只能使用 A-Z、a-z、0-9 这些字符。非法字符会被替换成下划线。")
    @ConfigComment("如果多个 BentoBox 实例共用同一个数据库，请设成唯一的值。")
    @ConfigComment("注意长度限制 —— 数据库表名通常最多 63 个字符")
    @ConfigEntry(path = "general.database.prefix-character", since = "1.13.0")
    private String databasePrefix = "";

    @ConfigComment("应用到连接池的自定义数据源属性。")
    @ConfigComment("具体可用值请查阅你所使用的 SQL 驱动实现。")
    @ConfigComment("示例：")
    @ConfigComment("  custom-properties:")
    @ConfigComment("    cachePrepStmts: 'true'")
    @ConfigComment("    prepStmtCacheSize: '250'")
    @ConfigComment("    prepStmtCacheSqlLimit: '2048'")
    @ConfigEntry(path = "general.database.custom-properties", since = "1.21.0")
    private Map<String, String> customPoolProperties = new HashMap<>();

    @ConfigComment("MongoDB 客户端连接 URI，用于覆盖默认连接选项。")
    @ConfigComment("参见：https://docs.mongodb.com/manual/reference/connection-string/")
    @ConfigEntry(path = "general.database.mongodb-connection-uri", since = "1.14.0")
    private String mongodbConnectionUri = "";

    @ConfigComment("允许 FTB Autonomous Activator 工作（允许伪玩家 [CoFH] 放置/破坏方块和悬挂物品）")
    @ConfigComment("如有需要，可在这里添加其它伪玩家名称")
    @ConfigEntry(path = "general.fakeplayers", experimental = true)
    private Set<String> fakePlayers = new HashSet<>();

    @ConfigComment("弹飞力度。玩家传送过去撞上敌对生物时，把它们弹开多远。")
    @ConfigComment("2.5 会弹开几个方块，5 会弹得很远，1 几乎没效果。")
    @ConfigEntry(path = "general.flingback")
    private double flingback = 2.5D;

    @ConfigComment("传送时清除敌对生物。如果管理设置中的世界 flag 已设置，它们会被清除而不是弹飞。")
    @ConfigEntry(path = "general.teleport-remove-mobs")
    private boolean teleportRemoveMobs = false;

    @ConfigComment("命令拼写建议（Did-you-mean）。")
    @ConfigComment("当玩家输错命令时，例如把 /oneblock team 输成 /teams，")
    @ConfigComment("BentoBox 会提示最接近的 BentoBox 命令。玩家可以点击该建议，")
    @ConfigComment("或输入 'yes' 直接执行。")
    @ConfigEntry(path = "general.did-you-mean.unknown-commands", since = "3.20.0")
    private boolean didYouMeanUnknownCommands = true;

    @ConfigComment("当玩家输错子命令时，例如把 /island team invite 输成 /island invit，")
    @ConfigComment("BentoBox 会提示最接近的子命令，而不是直接报错。")
    @ConfigEntry(path = "general.did-you-mean.subcommands", since = "3.20.0")
    private boolean didYouMeanSubcommands = true;

    @ConfigComment("使用 Paper 的 Brigadier 命令系统注册命令，而不是旧的命令映射表。")
    @ConfigComment("Brigadier 可以让客户端在玩家输入时实时显示子命令和补全项，而不用")
    @ConfigComment("等到按下回车或 TAB。")
    @ConfigComment("如果与其它插件冲突，可以关闭此项回到旧注册方式。")
    @ConfigEntry(path = "general.brigadier-commands", since = "3.22.0")
    private boolean useBrigadierCommands = true;

    /* PANELS */
    @ConfigComment("面板点击冷却。单位为毫秒。防止玩家在界面中连点。")
    @ConfigEntry(path = "panel.click-cooldown-ms")
    private long clickCooldownMs = 1000;
    
    @ConfigComment("是否在玩家点击物品栏界面外部时关闭面板。")
    @ConfigEntry(path = "panel.close-on-click-outside")
    private boolean closePanelOnClickOutside = true;

    @ConfigComment("定义用于填充大多数面板空隙（如标题栏等）的物品材质。")
    @ConfigEntry(path = "panel.filler-material", since = "1.14.0")
    private Material panelFillerMaterial = Material.LIGHT_BLUE_STAINED_GLASS_PANE;

    @ConfigComment("玩家头颅纹理是从 Mojang API 还是 mc-heads.net 缓存服务器获取。")
    @ConfigComment("Mojang API 有时会变慢，且对玩家数据请求有限流，开启此项可以")
    @ConfigComment("比 Mojang API 更快地获取玩家头颅。")
    @ConfigEntry(path = "panel.use-cache-server", since = "1.16.0")
    private boolean useCacheServer = false;

    @ConfigComment("玩家皮肤纹理链接在本地缓存中保存多久后重新请求。")
    @ConfigComment("单位：分钟。")
    @ConfigComment("值为 0 表示直到服务器重启都不清理缓存。")
    @ConfigEntry(path = "panel.head-cache-time", since = "1.14.1")
    private long playerHeadCacheTime = 60;

    @ConfigComment("每个任务请求多少个玩家头颅。")
    @ConfigComment("设得太大可能导致被头颅 API 临时封禁。")
    @ConfigEntry(path = "panel.heads-per-call", since = "1.16.0")
    private int headsPerCall = 9;

    @ConfigComment("每个头颅请求任务之间间隔多少 tick。")
    @ConfigComment("设得太大可能导致被头颅 API 临时封禁。")
    @ConfigEntry(path = "panel.ticks-between-calls", since = "1.16.0", needsRestart = true)
    private long ticksBetweenCalls = 10;

    /* DYNMAP */
    @ConfigComment("是否在 Dynmap 上显示每座岛中心的标记（房子图标）。")
    @ConfigComment("在岛屿密集的世界，这些图标会淹没地图。设为 false 可隐藏。")
    @ConfigComment("/!\\ 修改后需要重启服务器或执行 /bbox reload 才会生效。")
    @ConfigEntry(path = "dynmap.island-markers", since = "3.17.1")
    private boolean dynmapIslandMarkers = true;

    @ConfigComment("是否在 Dynmap 上绘制每座岛的保护区域边框。")
    @ConfigComment("/!\\ 修改后需要重启服务器或执行 /bbox reload 才会生效。")
    @ConfigEntry(path = "dynmap.island-areas", since = "3.17.1")
    private boolean dynmapIslandAreas = true;

    /* BLUEMAP */
    @ConfigComment("是否在 BlueMap 上显示每座岛中心的标记（图钉）。")
    @ConfigComment("如果你自己管理岛屿标记、不需要 BentoBox 的图层，请设为 false。")
    @ConfigComment("/!\\ 修改后需要重启服务器或执行 /bbox reload 才会生效。")
    @ConfigEntry(path = "bluemap.island-markers", since = "3.18.1")
    private boolean bluemapIslandMarkers = true;

    @ConfigComment("是否在 BlueMap 上绘制每座岛的保护区域边框。")
    @ConfigComment("/!\\ 修改后需要重启服务器或执行 /bbox reload 才会生效。")
    @ConfigEntry(path = "bluemap.island-areas", since = "3.18.1")
    private boolean bluemapIslandAreas = true;

    @ConfigComment("岛屿 POI 标记自定义图标的路径，相对于 BlueMap 的 web 根目录")
    @ConfigComment("（例如 \"assets/island.png\" 或 \"assets/island.svg\"）。留空则使用 BlueMap 的")
    @ConfigComment("默认 POI 图标。")
    @ConfigEntry(path = "bluemap.marker-icon", since = "3.18.1")
    private String bluemapMarkerIcon = "";

    @ConfigComment("自定义图标的像素锚点 —— 图片中落在岛屿位置上的那个点。")
    @ConfigComment("marker-icon 为空时忽略此项。")
    @ConfigEntry(path = "bluemap.marker-icon-anchor-x", since = "3.18.1")
    private int bluemapMarkerIconAnchorX = 25;
    @ConfigEntry(path = "bluemap.marker-icon-anchor-y", since = "3.18.1")
    private int bluemapMarkerIconAnchorY = 45;

    @ConfigComment("岛屿 POI 标记在地图上的可见距离范围（格）。")
    @ConfigComment("调大 min-distance 可在缩得很远时隐藏标记；调小 max-distance 可在")
    @ConfigComment("放大时隐藏标记。默认范围实际上等于\"始终可见\"。")
    @ConfigEntry(path = "bluemap.marker-min-distance", since = "3.18.1")
    private double bluemapMarkerMinDistance = 0.0;
    @ConfigEntry(path = "bluemap.marker-max-distance", since = "3.18.1")
    private double bluemapMarkerMaxDistance = 10000000.0;

    @ConfigComment("保护区域矩形的颜色和样式。颜色为十六进制字符串。")
    @ConfigEntry(path = "bluemap.area-line-color", since = "3.18.1")
    private String bluemapAreaLineColor = "#3388FF";
    @ConfigEntry(path = "bluemap.area-fill-color", since = "3.18.1")
    private String bluemapAreaFillColor = "#3388FF";
    @ConfigComment("填充不透明度，0.0（全透明）到 1.0（不透明）。")
    @ConfigEntry(path = "bluemap.area-fill-opacity", since = "3.18.1")
    private double bluemapAreaFillOpacity = 0.15;
    @ConfigComment("边框线宽（像素）。")
    @ConfigEntry(path = "bluemap.area-line-width", since = "3.18.1")
    private int bluemapAreaLineWidth = 2;

    /*
     * Logs
     */
    @ConfigComment("是否在服务器日志中记录超平坦区块的重生成。")
    @ConfigComment("如果需要重生成的超平坦区块很多，日志会刷屏。")
    @ConfigComment("不过，超平坦区块重生成对性能开销较大，建议保持")
    @ConfigComment("为 true，这样你能知道是否有重生成正在进行。")
    @ConfigEntry(path = "logs.clean-super-flat-chunks", since = "1.2.0")
    private boolean logCleanSuperFlatChunks = true;

    @ConfigComment("是否在服务器日志中记录从 GitHub 下载数据。")
    @ConfigEntry(path = "logs.github-download-data", since = "1.5.0")
    private boolean logGithubDownloadData = true;

    /*
     * Island
     */
    @ConfigComment("是否覆盖岛屿距离不一致的检查。出于安全考虑，当游戏模式配置中的")
    @ConfigComment("岛屿距离与数据库记录的不一致时，BentoBox 通常会拒绝启动。")
    @ConfigComment("此项会跳过该检查。正常情况永远不需要开启；如果你不清楚它的作用，")
    @ConfigComment("请保持 false")
    @ConfigEntry(path = "island.override-safety-check")
    private boolean overrideSafetyCheck = false;

    // Number of islands
    @ConfigComment("玩家默认可以拥有的并行岛屿数量。")
    @ConfigComment("可能被各游戏模式自己的配置覆盖。")
    @ConfigEntry(path = "island.concurrent-islands")
    private int islandNumber = 1;

    @ConfigComment("隐藏已用尽的蓝图。")
    @ConfigComment("玩家有并行岛屿时，蓝图可以设置最大使用次数。")
    @ConfigComment("设为 true 时，已用尽的蓝图不会显示在岛屿创建菜单中。")
    @ConfigEntry(path = "island.hide-used-blueprints", since = "2.3.0")
    private boolean hideUsedBlueprints = false;

    // Cooldowns
    @ConfigComment("玩家被踢出团队岛屿后，需要等待多久才能重新加入（分钟）。")
    @ConfigComment("这会降低玩家通过反复被邀请进团队岛屿")
    @ConfigComment("来重复刷挑战的效率。")
    @ConfigEntry(path = "island.cooldown.time.invite")
    private int inviteCooldown = 60;

    @ConfigComment("玩家需要等待多久才能 coop 一名玩家（分钟）。")
    @ConfigEntry(path = "island.cooldown.time.coop")
    private int coopCooldown = 5;

    @ConfigComment("玩家需要等待多久才能 trust 一名玩家（分钟）。")
    @ConfigEntry(path = "island.cooldown.time.trust")
    private int trustCooldown = 5;

    @ConfigComment("玩家解封某人后，需要等待多久才能再次封禁该玩家（分钟）。")
    @ConfigEntry(path = "island.cooldown.time.ban")
    private int banCooldown = 10;

    @ConfigComment("玩家需要等待多久才能再次重置岛屿（秒）。")
    @ConfigEntry(path = "island.cooldown.time.reset")
    private int resetCooldown = 300;

    @ConfigComment("玩家首次创建岛屿时是否也应用重置冷却。")
    @ConfigEntry(path = "island.cooldown.options.set-reset-cooldown-on-create", since = "1.2.0")
    private boolean resetCooldownOnCreate = true;

    // Timeout for team kick and leave commands
    @ConfigComment("玩家确认敏感命令（例如岛屿重置）的时限（秒）。")
    @ConfigEntry(path = "island.confirmation.time")
    private int confirmationTime = 10;

    // Timeout for team kick and leave commands
    @ConfigComment("玩家执行传送命令（例如 /island go）前需要站立的时长（秒）。")
    @ConfigEntry(path = "island.delay.time")
    private int delayTime = 0;

    @ConfigComment("要求玩家重新输入命令来确认操作。")
    @ConfigEntry(path = "island.confirmation.commands.kick")
    private boolean kickConfirmation = true;

    @ConfigEntry(path = "island.confirmation.commands.leave")
    private boolean leaveConfirmation = true;

    @ConfigEntry(path = "island.confirmation.commands.reset")
    private boolean resetConfirmation = true;

    @ConfigComment("要求被邀请者确认 trust 或 coop 邀请。")
    @ConfigComment("出于安全考虑，团队邀请始终需要确认。")
    @ConfigEntry(path = "island.confirmation.invites", since = "1.8.0")
    private boolean inviteConfirmation = false;

    @ConfigComment("模态对话框选项。对话框是玩家在敏感流程中看到的")
    @ConfigComment("\"无法点掉的菜单\"。需要支持对话框的服务器")
    @ConfigComment("（Minecraft 26+）；在旧版服务器上，无论下面怎么设置，")
    @ConfigComment("所有选项都会自动回退到传统的聊天/命令方式。")
    @ConfigComment("把敏感命令的确认显示为模态的 [确认]/[取消] 对话框，")
    @ConfigComment("而不是要求玩家重新输入命令。")
    @ConfigEntry(path = "island.dialogs.confirmations", since = "3.21.0")
    private boolean dialogConfirmations = true;

    @ConfigComment("玩家收到团队邀请时自动弹出 [接受]/[拒绝] 对话框，")
    @ConfigComment("而不是等玩家自己输入接受/拒绝命令。")
    @ConfigEntry(path = "island.dialogs.team-invites", since = "3.21.0")
    private boolean dialogTeamInvites = true;

    @ConfigComment("玩家不带名称执行 island go/home 命令、且有多个岛屿或家可选时，")
    @ConfigComment("显示一个\"每个目的地一个按钮\"的选择界面。")
    @ConfigEntry(path = "island.dialogs.go-picker", since = "3.21.0")
    private boolean dialogGoPicker = true;

    @ConfigComment("服务器安装了多个游戏模式时，向新玩家显示一个不可关闭的")
    @ConfigComment("\"选择你的游戏\"对话框。默认关闭：它比较打扰，而且")
    @ConfigComment("点击某个游戏会直接替玩家执行该游戏模式的命令。")
    @ConfigEntry(path = "island.dialogs.game-mode-selection", since = "3.21.0")
    private boolean dialogGameModeSelection = false;

    @ConfigComment("岛屿自定义名称的最小长度。")
    @ConfigEntry(path = "island.name.min-length")
    private int nameMinLength = 4;
    @ConfigComment("岛屿自定义名称的最大长度。")
    @ConfigEntry(path = "island.name.max-length")
    private int nameMaxLength = 20;
    @ConfigComment("要求岛屿自定义名称在其所属游戏模式内唯一。")
    @ConfigComment("因此每个游戏模式中只允许一座岛使用相同名称。")
    @ConfigComment("注意：岛屿名称纯粹是装饰性的，不用于程序识别岛屿。")
    @ConfigEntry(path = "island.name.uniqueness", since = "1.7.0")
    private boolean nameUniqueness = false;

    @ConfigComment("传送时清除敌对生物的范围半径")
    @ConfigComment("如果启用了传送时清除敌对生物，则会以玩家为中心")
    @ConfigComment("清除这个大小的范围。例如 5 表示玩家周围 10 x 10 x 10 的立方体")
    @ConfigComment("注意不要设得太大。不适用于原版下界或末地传送。")
    @ConfigEntry(path = "island.clear-radius", since = "1.6.0")
    private int clearRadius = 5;

    @ConfigComment("下界门搜索的最小半径。设得太小可能出现重复的传送门。")
    @ConfigComment("原版默认值为 128。")
    @ConfigEntry(path = "island.portal-search-radius", since = "1.16.2")
    private int minPortalSearchRadius = 64;

    @ConfigComment("粘贴蓝图时每 tick 粘贴多少个方块。")
    @ConfigComment("数值越小越不容易卡顿，但粘贴会稍慢。")
    @ConfigComment("反之，数值越大粘贴越快，但很快就会被")
    @ConfigComment("需要加载的区块数量拖累，常常导致服务器卡死。")
    @ConfigEntry(path = "island.paste-speed")
    private int pasteSpeed = 64;
    
    @ConfigComment("岛屿等级清理保护")
    @ConfigComment("高于此等级的岛屿即使很旧也不会被清理。需要 Level 插件。")
    @ConfigComment("如果没有安装 Level 插件，则不做此检查。")
    @ConfigEntry(path = "island.purge-level")
    private int islandPurgeLevel = 10;

    @ConfigComment("岛屿删除：每个世界每秒重生成多少个区块。")
    @ConfigComment("如果有下界和末地，每秒会重生成这个数量的 3 倍。")
    @ConfigComment("数值越小越不容易卡顿，但删除会花更长时间。")
    @ConfigComment("设为 0 会保留岛屿方块（不推荐）。")
    @ConfigEntry(path = "island.delete-speed", since = "1.7.0")
    private int deleteSpeed = 1;


    /**
     * @deprecated No longer bound to config. The chunk-by-chunk deletion
     *             pipeline has been removed. Slated for removal.
     */
    @Deprecated(since = "3.14.0", forRemoval = true)
    private boolean slowDeletion = false;

    // Island deletion housekeeping
    @ConfigComment("日常维护：定期自动清理无用的 region 文件。")
    @ConfigComment("玩家重置岛屿时，旧岛屿会被孤立（标记为")
    @ConfigComment("可删除），其 region 文件之后由计划清理回收。")
    @ConfigComment("")
    @ConfigComment("警告：日常维护会从磁盘删除 .mca region 文件。它使用与")
    @ConfigComment("/bbox purge regions 命令相同的保护条件（在线玩家、岛屿等级、")
    @ConfigComment("purge-protected flag、出生岛，以及无主但不可删除的岛屿")
    @ConfigComment("都会被跳过），但按设计就是破坏性操作。")
    @ConfigComment("")
    @ConfigComment("--- 已删除岛屿清扫（安全，默认开启）---")
    @ConfigComment("回收被明确标记为可删除的岛屿的 region 文件（例如 /is reset）。")
    @ConfigComment("只删除 BentoBox 自己软删除的岛屿 —— 绝不会碰")
    @ConfigComment("活跃或从未访问过的岛屿。开启此项并保持 age-sweep 关闭，")
    @ConfigComment("即可\"开箱即用\"地回收重置岛屿占用的磁盘空间。")
    @ConfigEntry(path = "island.deletion.housekeeping.deleted-sweep.enabled", since = "3.15.0")
    private boolean housekeepingDeletedEnabled = true;

    @ConfigComment("已删除岛屿清扫的运行间隔（小时）。")
    @ConfigEntry(path = "island.deletion.housekeeping.deleted-sweep.interval-hours", since = "3.15.0")
    private int housekeepingDeletedIntervalHours = 24;

    @ConfigComment("")
    @ConfigComment("--- 按时间清扫（需手动开启，更激进）---")
    @ConfigComment("回收 .mca 文件早于 min-age-days 的 region 文件，无论")
    @ConfigComment("这些岛屿是否被标记为可删除。可能删除从未重置、")
    @ConfigComment("只是被遗弃的岛屿。默认关闭 —— 只有在你希望")
    @ConfigComment("自动回收所有陈旧、无人访问的岛屿时才开启。")
    @ConfigEntry(path = "island.deletion.housekeeping.age-sweep.enabled", since = "3.15.0")
    private boolean housekeepingAgeEnabled = false;

    @ConfigComment("按时间清扫的运行间隔（天）。")
    @ConfigEntry(path = "island.deletion.housekeeping.age-sweep.interval-days", since = "3.15.0")
    private int housekeepingIntervalDays = 30;

    @ConfigComment("符合按时间清理条件的 region 文件的最小年龄（天）。")
    @ConfigEntry(path = "island.deletion.housekeeping.age-sweep.min-age-days", since = "3.15.0")
    private int housekeepingRegionAgeDays = 60;

    // Chunk pre-generation settings
    @ConfigComment("")
    @ConfigComment("区块预生成设置。")
    @ConfigComment("在预计的未来岛屿位置周围预生成区块，")
    @ConfigComment("以减少创建或重置岛屿时的卡顿。")
    @ConfigComment("")
    @ConfigComment("启用或禁用区块预生成。")
    @ConfigEntry(path = "island.pregeneration.enabled", since = "3.14.0")
    private boolean pregenEnabled = false;

    @ConfigComment("每个游戏模式世界为多少座未来岛屿预生成区块。")
    @ConfigComment("数值越大占用磁盘越多，但能降低创建岛屿时的卡顿概率。")
    @ConfigEntry(path = "island.pregeneration.islands-ahead", since = "3.14.0")
    private int pregenIslandsAhead = 3;

    @ConfigComment("每 tick 批次最多派发多少个异步区块生成请求。")
    @ConfigComment("数值越小对服务器影响越小。Paper 会异步处理这些请求。")
    @ConfigComment("建议范围：2-10。")
    @ConfigEntry(path = "island.pregeneration.chunks-per-tick", since = "3.14.0")
    private int pregenChunksPerTick = 4;

    @ConfigComment("预生成批次之间的间隔 tick 数。")
    @ConfigComment("1 = 每 tick（最快），20 = 每秒一次（最温和）。")
    @ConfigEntry(path = "island.pregeneration.tick-interval", since = "3.14.0")
    private int pregenTickInterval = 5;

    @ConfigComment("默认情况下，如果目的地不安全，插件会尝试在目的地周围")
    @ConfigComment("搜索安全位置，然后会尝试从目的地向上、向下扩展 y 坐标。")
    @ConfigComment("此设置限制 y 坐标扩展的距离。")
    @ConfigComment("设为 0 或更低时，插件不会扩展 y 坐标。")
    @ConfigEntry(path = "island.safe-spot-search-vertical-range", since = "1.19.1")
    private int safeSpotSearchVerticalRange = 400;

    @ConfigComment("默认情况下，如果目的地不安全，插件会尝试在目的地周围搜索安全位置。")
    @ConfigComment("此项可修改搜索该位置的距离。数值越大搜索越久。")
    @ConfigComment("该值也用于不同维度之间有效的下界门链接。")
    @ConfigEntry(path = "island.safe-spot-search-range", since = "1.21.0")
    private int safeSpotSearchRange = 16;

    @ConfigComment("玩家既没有岛屿也没有设置出生点时，以被驱逐玩家身份执行的命令。")
    @ConfigComment("通常由 EssentialsX 之类的插件提供。")
    @ConfigComment("留空表示这种情况下什么都不做。")
    @ConfigEntry(path = "island.expel.command", since = "3.11.3")
    private String expelCommand = "spawn";

    @ConfigComment("玩家用空桶舀黑曜石（OBSIDIAN_SCOOPING flag）时，")
    @ConfigComment("检查黑曜石方块周围多大半径（格）内是否有其它黑曜石。")
    @ConfigComment("如果这个半径内还有黑曜石，则拒绝舀取。")
    @ConfigComment("设为 0 可禁用检查，允许舀取任何孤立的黑曜石方块。")
    @ConfigComment("范围：0 到 15。默认 2。")
    @ConfigEntry(path = "island.obsidian-scooping-radius", since = "3.11.3")
    private int obsidianScoopingRadius = 2;

    @ConfigComment("玩家使用 OBSIDIAN_SCOOPING flag 把黑曜石舀进熔岩桶后")
    @ConfigComment("应用的冷却时长（分钟）。冷却期间玩家")
    @ConfigComment("不能舀取另一块黑曜石。这可以防止通过在水边")
    @ConfigComment("快速舀取黑曜石来复制熔岩桶的漏洞。")
    @ConfigComment("最小值为 1 分钟。默认 1 分钟。")
    @ConfigComment("注意：修改此值需要重启服务器才会生效。")
    @ConfigEntry(path = "island.obsidian-scooping-cooldown", since = "3.11.4")
    private int obsidianScoopingCooldown = 1;

    @ConfigComment("新形成的、可舀回熔岩的黑曜石上方显示全息提示的")
    @ConfigComment("时长（秒）。该全息提示提醒玩家可以用空桶")
    @ConfigComment("右键黑曜石取回熔岩。")
    @ConfigComment("设为 0 或更低可完全关闭提示。默认 5 秒。")
    @ConfigEntry(path = "island.obsidian-scooping-lava-tip-duration", since = "3.14.0")
    private int obsidianScoopingLavaTipDuration = 5;

    @ConfigComment("每座岛保留的历史（日志）记录条数上限。达到上限后")
    @ConfigComment("会先丢弃最旧的记录，这样长期存在的岛屿不会在内存")
    @ConfigComment("和数据库里无限增长。")
    @ConfigComment("设为 0（或负数）表示不限历史记录。这是默认值，")
    @ConfigComment("现有服务器的行为保持不变。")
    @ConfigComment("注意：历史记录设上限后，一旦最旧的 JOINED 记录")
    @ConfigComment("被丢弃，历史成员占位符可能会少算。")
    @ConfigEntry(path = "island.history.max-entries", since = "3.22.4")
    private int islandHistoryMaxEntries = 0;

    /* WORLD */
    @ConfigComment("默认在每个 BentoBox 游戏模式世界（主世界、下界、末地）")
    @ConfigComment("禁用原版结构。列出要停止生成、并在结构搜索中跳过的")
    @ConfigComment("结构键名（/locate、末影之眼、探险家/藏宝图、")
    @ConfigComment("海豚和制图师村民交易）。用这种方式抑制结构，")
    @ConfigComment("还能防止这些搜索一直扫描到世界边界并卡死服务器。")
    @ConfigComment("键名就是原版结构 id：在游戏内对 '/locate structure ' 按 TAB 补全")
    @ConfigComment("可看到完整列表（'minecraft:mansion' 在这里写作 'mansion'）。键名可用 '-' 或 '_'，")
    @ConfigComment("大小写不限，例如 trial_chambers、ancient-city。")
    @ConfigComment("要阻止最常让虚空世界服务器卡死的地图搜索")
    @ConfigComment("（制图师探险家地图交易、海豚、箱子藏宝图），")
    @ConfigComment("请禁用这些地图所寻找的结构：")
    @ConfigComment("  disabled-structures:")
    @ConfigComment("  - monument")
    @ConfigComment("  - mansion")
    @ConfigComment("  - trial_chambers")
    @ConfigComment("  - buried_treasure")
    @ConfigComment("生成器完全不放置结构的世界（例如经典空岛虚空")
    @ConfigComment("世界）会自动跳过这些搜索 —— 无需为它们添加条目。")
    @ConfigComment("游戏模式可以在自己的配置中按结构覆盖此项，")
    @ConfigComment("既能禁用更多结构，也能强制启用本列表禁用的结构。")
    @ConfigComment("留空（默认）不禁止任何结构，行为保持不变。")
    @ConfigEntry(path = "world.disabled-structures", since = "3.19.1")
    private List<String> disabledStructures = new ArrayList<>();

    /* WEB */
    @ConfigComment("是否允许 BentoBox 连接 GitHub 获取更新和 addon 数据。")
    @ConfigComment("禁用后会关闭更新检查，以及其它依赖从 GitHub API")
    @ConfigComment("下载数据的部分功能。")
    @ConfigComment("不会发送任何数据。")
    @ConfigEntry(path = "web.github.download-data", since = "1.5.0")
    private boolean githubDownloadData = true;

    @ConfigComment("每次连接 GitHub API 的间隔（分钟）。")
    @ConfigComment("这样可以获取最新信息。")
    @ConfigComment("不过 GitHub API 数据并非即时更新，因此该值不能小于 60 分钟。")
    @ConfigComment("设为 0 表示 BentoBox 只在启动时下载数据。")
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

    public boolean isChargeForBlueprintOnReset() {
        return chargeForBlueprintOnReset;
    }

    public void setChargeForBlueprintOnReset(boolean chargeForBlueprintOnReset) {
        this.chargeForBlueprintOnReset = chargeForBlueprintOnReset;
    }

    /**
     * @return whether anonymous usage statistics are submitted to bStats
     * @since 3.22.3
     */
    public boolean isMetrics() {
        return metrics;
    }

    /**
     * @param metrics whether anonymous usage statistics are submitted to bStats
     * @since 3.22.3
     */
    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
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
     * @return whether sensitive-command confirmations should use a modal dialog
     * @since 3.21.0
     */
    public boolean isDialogConfirmations() {
        return dialogConfirmations;
    }

    /**
     * @param dialogConfirmations whether to use a modal dialog for confirmations
     * @since 3.21.0
     */
    public void setDialogConfirmations(boolean dialogConfirmations) {
        this.dialogConfirmations = dialogConfirmations;
    }

    /**
     * @return whether team invites should auto-open an accept/decline dialog
     * @since 3.21.0
     */
    public boolean isDialogTeamInvites() {
        return dialogTeamInvites;
    }

    /**
     * @param dialogTeamInvites whether team invites should open a dialog
     * @since 3.21.0
     */
    public void setDialogTeamInvites(boolean dialogTeamInvites) {
        this.dialogTeamInvites = dialogTeamInvites;
    }

    /**
     * @return whether the island go/home command shows a destination picker dialog
     * @since 3.21.0
     */
    public boolean isDialogGoPicker() {
        return dialogGoPicker;
    }

    /**
     * @param dialogGoPicker whether the go/home command shows a picker dialog
     * @since 3.21.0
     */
    public void setDialogGoPicker(boolean dialogGoPicker) {
        this.dialogGoPicker = dialogGoPicker;
    }

    /**
     * @return whether brand new players see a game-mode selection dialog
     * @since 3.21.0
     */
    public boolean isDialogGameModeSelection() {
        return dialogGameModeSelection;
    }

    /**
     * @param dialogGameModeSelection whether new players see a game-mode dialog
     * @since 3.21.0
     */
    public void setDialogGameModeSelection(boolean dialogGameModeSelection) {
        this.dialogGameModeSelection = dialogGameModeSelection;
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
     * Whether BentoBox shows a marker (house icon) on Dynmap at the center of every island.
     *
     * @return true if island markers should be shown
     * @since 3.17.1
     */
    public boolean isDynmapIslandMarkers() {
        return dynmapIslandMarkers;
    }

    /**
     * Sets whether BentoBox shows a marker (house icon) on Dynmap at the center of every island.
     *
     * @param dynmapIslandMarkers whether island markers should be shown
     * @since 3.17.1
     */
    public void setDynmapIslandMarkers(boolean dynmapIslandMarkers) {
        this.dynmapIslandMarkers = dynmapIslandMarkers;
    }

    /**
     * Whether BentoBox draws the protected area border box of every island on Dynmap.
     *
     * @return true if island area boxes should be drawn
     * @since 3.17.1
     */
    public boolean isDynmapIslandAreas() {
        return dynmapIslandAreas;
    }

    /**
     * Sets whether BentoBox draws the protected area border box of every island on Dynmap.
     *
     * @param dynmapIslandAreas whether island area boxes should be drawn
     * @since 3.17.1
     */
    public void setDynmapIslandAreas(boolean dynmapIslandAreas) {
        this.dynmapIslandAreas = dynmapIslandAreas;
    }

    /**
     * Whether BentoBox shows a marker (pin) on BlueMap at the center of every island.
     *
     * @return true if island markers should be shown
     * @since 3.18.1
     */
    public boolean isBluemapIslandMarkers() {
        return bluemapIslandMarkers;
    }

    /**
     * Sets whether BentoBox shows a marker (pin) on BlueMap at the center of every island.
     *
     * @param bluemapIslandMarkers whether island markers should be shown
     * @since 3.18.1
     */
    public void setBluemapIslandMarkers(boolean bluemapIslandMarkers) {
        this.bluemapIslandMarkers = bluemapIslandMarkers;
    }

    /**
     * Whether BentoBox draws the protected area border box of every island on BlueMap.
     *
     * @return true if island area boxes should be drawn
     * @since 3.18.1
     */
    public boolean isBluemapIslandAreas() {
        return bluemapIslandAreas;
    }

    /**
     * Sets whether BentoBox draws the protected area border box of every island on BlueMap.
     *
     * @param bluemapIslandAreas whether island area boxes should be drawn
     * @since 3.18.1
     */
    public void setBluemapIslandAreas(boolean bluemapIslandAreas) {
        this.bluemapIslandAreas = bluemapIslandAreas;
    }

    /**
     * Path to a custom icon for the island POI marker, relative to BlueMap's web root. Blank
     * means use BlueMap's default POI icon.
     * @return the icon path, possibly blank
     * @since 3.18.1
     */
    public String getBluemapMarkerIcon() {
        return bluemapMarkerIcon;
    }

    /**
     * Sets the custom icon path for the island POI marker.
     * @param bluemapMarkerIcon icon path relative to BlueMap's web root, or blank for default
     * @since 3.18.1
     */
    public void setBluemapMarkerIcon(String bluemapMarkerIcon) {
        this.bluemapMarkerIcon = bluemapMarkerIcon;
    }

    /**
     * @return the X pixel anchor of the custom island marker icon
     * @since 3.18.1
     */
    public int getBluemapMarkerIconAnchorX() {
        return bluemapMarkerIconAnchorX;
    }

    /**
     * @param bluemapMarkerIconAnchorX the X pixel anchor of the custom island marker icon
     * @since 3.18.1
     */
    public void setBluemapMarkerIconAnchorX(int bluemapMarkerIconAnchorX) {
        this.bluemapMarkerIconAnchorX = bluemapMarkerIconAnchorX;
    }

    /**
     * @return the Y pixel anchor of the custom island marker icon
     * @since 3.18.1
     */
    public int getBluemapMarkerIconAnchorY() {
        return bluemapMarkerIconAnchorY;
    }

    /**
     * @param bluemapMarkerIconAnchorY the Y pixel anchor of the custom island marker icon
     * @since 3.18.1
     */
    public void setBluemapMarkerIconAnchorY(int bluemapMarkerIconAnchorY) {
        this.bluemapMarkerIconAnchorY = bluemapMarkerIconAnchorY;
    }

    /**
     * @return the minimum distance (blocks) at which the island POI marker is visible
     * @since 3.18.1
     */
    public double getBluemapMarkerMinDistance() {
        return bluemapMarkerMinDistance;
    }

    /**
     * @param bluemapMarkerMinDistance the minimum distance (blocks) at which the marker is visible
     * @since 3.18.1
     */
    public void setBluemapMarkerMinDistance(double bluemapMarkerMinDistance) {
        this.bluemapMarkerMinDistance = bluemapMarkerMinDistance;
    }

    /**
     * @return the maximum distance (blocks) at which the island POI marker is visible
     * @since 3.18.1
     */
    public double getBluemapMarkerMaxDistance() {
        return bluemapMarkerMaxDistance;
    }

    /**
     * @param bluemapMarkerMaxDistance the maximum distance (blocks) at which the marker is visible
     * @since 3.18.1
     */
    public void setBluemapMarkerMaxDistance(double bluemapMarkerMaxDistance) {
        this.bluemapMarkerMaxDistance = bluemapMarkerMaxDistance;
    }

    /**
     * @return the protected-area rectangle border colour as a hex string
     * @since 3.18.1
     */
    public String getBluemapAreaLineColor() {
        return bluemapAreaLineColor;
    }

    /**
     * @param bluemapAreaLineColor the protected-area rectangle border colour as a hex string
     * @since 3.18.1
     */
    public void setBluemapAreaLineColor(String bluemapAreaLineColor) {
        this.bluemapAreaLineColor = bluemapAreaLineColor;
    }

    /**
     * @return the protected-area rectangle fill colour as a hex string
     * @since 3.18.1
     */
    public String getBluemapAreaFillColor() {
        return bluemapAreaFillColor;
    }

    /**
     * @param bluemapAreaFillColor the protected-area rectangle fill colour as a hex string
     * @since 3.18.1
     */
    public void setBluemapAreaFillColor(String bluemapAreaFillColor) {
        this.bluemapAreaFillColor = bluemapAreaFillColor;
    }

    /**
     * @return the protected-area rectangle fill opacity, 0.0 to 1.0
     * @since 3.18.1
     */
    public double getBluemapAreaFillOpacity() {
        return bluemapAreaFillOpacity;
    }

    /**
     * @param bluemapAreaFillOpacity the protected-area rectangle fill opacity, 0.0 to 1.0
     * @since 3.18.1
     */
    public void setBluemapAreaFillOpacity(double bluemapAreaFillOpacity) {
        this.bluemapAreaFillOpacity = bluemapAreaFillOpacity;
    }

    /**
     * @return the protected-area rectangle border line width in pixels
     * @since 3.18.1
     */
    public int getBluemapAreaLineWidth() {
        return bluemapAreaLineWidth;
    }

    /**
     * @param bluemapAreaLineWidth the protected-area rectangle border line width in pixels
     * @since 3.18.1
     */
    public void setBluemapAreaLineWidth(int bluemapAreaLineWidth) {
        this.bluemapAreaLineWidth = bluemapAreaLineWidth;
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
     * @deprecated The chunk-by-chunk deletion pipeline is being removed. This
     *             setting no longer has any effect and will be deleted in a
     *             future release. Configure the housekeeping auto-purge instead.
     */
    @Deprecated(since = "3.14.0", forRemoval = true)
    public boolean isSlowDeletion() {
        return slowDeletion;
    }

    /**
     * Sets slow deletion.
     *
     * @param slowDeletion the slow deletion
     * @deprecated See {@link #isSlowDeletion()}.
     */
    @Deprecated(since = "3.14.0", forRemoval = true)
    public void setSlowDeletion(boolean slowDeletion) {
        this.slowDeletion = slowDeletion;
    }

    /**
     * Returns whether islands, when reset, should be kept or deleted.
     *
     * @return always {@code false} — islands are now soft-deleted on reset and
     *         their region files reaped asynchronously by the housekeeping purge.
     * @since 1.13.0
     * @deprecated No longer configurable. Islands are soft-deleted on reset.
     *             This method always returns {@code false} and will be removed
     *             in a future release.
     */
    @Deprecated(since = "3.15.0", forRemoval = true)
    public boolean isKeepPreviousIslandOnReset() {
        return false;
    }

    /**
     * No-op — the keep-previous-island setting is no longer configurable.
     *
     * @param keepPreviousIslandOnReset ignored
     * @since 1.13.0
     * @deprecated No longer configurable. Islands are soft-deleted on reset.
     */
    @Deprecated(since = "3.15.0", forRemoval = true)
    public void setKeepPreviousIslandOnReset(boolean keepPreviousIslandOnReset) {
        // no-op
    }

    /**
     * @return whether the deleted-island sweep is enabled (reaps regions for
     *         islands explicitly flagged deletable, e.g. from {@code /is reset}).
     * @since 3.15.0
     */
    public boolean isHousekeepingDeletedEnabled() {
        return housekeepingDeletedEnabled;
    }

    /**
     * @param housekeepingDeletedEnabled whether the deleted-island sweep is enabled.
     * @since 3.15.0
     */
    public void setHousekeepingDeletedEnabled(boolean housekeepingDeletedEnabled) {
        this.housekeepingDeletedEnabled = housekeepingDeletedEnabled;
    }

    /**
     * @return whether the age-based sweep is enabled (reaps regions for islands
     *         whose .mca files are older than {@link #getHousekeepingRegionAgeDays()}).
     * @since 3.15.0
     */
    public boolean isHousekeepingAgeEnabled() {
        return housekeepingAgeEnabled;
    }

    /**
     * @param housekeepingAgeEnabled whether the age-based sweep is enabled.
     * @since 3.15.0
     */
    public void setHousekeepingAgeEnabled(boolean housekeepingAgeEnabled) {
        this.housekeepingAgeEnabled = housekeepingAgeEnabled;
    }

    /**
     * @return how often the age-based sweep runs, in days.
     * @since 3.15.0
     */
    public int getHousekeepingIntervalDays() {
        return housekeepingIntervalDays;
    }

    /**
     * @param housekeepingIntervalDays how often the age-based sweep runs, in days.
     * @since 3.15.0
     */
    public void setHousekeepingIntervalDays(int housekeepingIntervalDays) {
        this.housekeepingIntervalDays = housekeepingIntervalDays;
    }

    /**
     * @return minimum age (in days) of region files eligible for the age-based purge.
     * @since 3.15.0
     */
    public int getHousekeepingRegionAgeDays() {
        return housekeepingRegionAgeDays;
    }

    /**
     * @param housekeepingRegionAgeDays minimum age (in days) of region files
     *                                  considered for auto-purge.
     * @since 3.15.0
     */
    public void setHousekeepingRegionAgeDays(int housekeepingRegionAgeDays) {
        this.housekeepingRegionAgeDays = housekeepingRegionAgeDays;
    }

    /**
     * @return how often the deleted-islands sweep runs, in hours. {@code 0}
     *         disables the deleted sweep.
     * @since 3.15.0
     */
    public int getHousekeepingDeletedIntervalHours() {
        return housekeepingDeletedIntervalHours;
    }

    /**
     * @param housekeepingDeletedIntervalHours how often the deleted sweep runs
     *                                         in hours. {@code 0} disables it.
     * @since 3.15.0
     */
    public void setHousekeepingDeletedIntervalHours(int housekeepingDeletedIntervalHours) {
        this.housekeepingDeletedIntervalHours = housekeepingDeletedIntervalHours;
    }

    /**
     * @return whether chunk pre-generation is enabled
     * @since 3.15.0
     */
    public boolean isPregenEnabled() {
        return pregenEnabled;
    }

    /**
     * @param pregenEnabled whether chunk pre-generation is enabled
     * @since 3.15.0
     */
    public void setPregenEnabled(boolean pregenEnabled) {
        this.pregenEnabled = pregenEnabled;
    }

    /**
     * @return number of future islands to pre-generate per game mode
     * @since 3.15.0
     */
    public int getPregenIslandsAhead() {
        return pregenIslandsAhead;
    }

    /**
     * @param pregenIslandsAhead number of future islands to pre-generate
     * @since 3.15.0
     */
    public void setPregenIslandsAhead(int pregenIslandsAhead) {
        this.pregenIslandsAhead = pregenIslandsAhead;
    }

    /**
     * @return max async chunk generation requests per tick batch
     * @since 3.15.0
     */
    public int getPregenChunksPerTick() {
        return pregenChunksPerTick;
    }

    /**
     * @param pregenChunksPerTick max async chunk requests per tick
     * @since 3.15.0
     */
    public void setPregenChunksPerTick(int pregenChunksPerTick) {
        this.pregenChunksPerTick = pregenChunksPerTick;
    }

    /**
     * @return ticks between pre-generation batches
     * @since 3.15.0
     */
    public int getPregenTickInterval() {
        return pregenTickInterval;
    }

    /**
     * @param pregenTickInterval ticks between batches
     * @since 3.15.0
     */
    public void setPregenTickInterval(int pregenTickInterval) {
        this.pregenTickInterval = pregenTickInterval;
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
     * @return the flingback
     */
    public double getFlingback() {
        return flingback;
    }

    /**
     * @param flingback the flingback to set
     */
    public void setFlingback(double flingback) {
        this.flingback = flingback;
    }

    /**
     * @return the teleportRemoveMobs
     */
    public boolean isTeleportRemoveMobs() {
        return teleportRemoveMobs;
    }

    /**
     * @param teleportRemoveMobs the teleportRemoveMobs to set
     */
    public void setTeleportRemoveMobs(boolean teleportRemoveMobs) {
        this.teleportRemoveMobs = teleportRemoveMobs;
    }

    /**
     * @return an immutable list of readyCommands
     */
    public List<String> getReadyCommands() {
        return List.copyOf(Objects.requireNonNullElse(readyCommands, Collections.emptyList()));
    }

    /**
     * @param readyCommands the readyCommands to set
     */
    public void setReadyCommands(List<String> readyCommands) {
        this.readyCommands = readyCommands;
    }

    /**
     * Gets the command to run as the expelled player when they have no island and no spawn is set.
     *
     * @return the expel command
     */
    public String getExpelCommand() {
        return expelCommand;
    }

    /**
     * Sets the command to run as the expelled player when they have no island and no spawn is set.
     *
     * @param expelCommand the expel command to set
     */
    public void setExpelCommand(String expelCommand) {
        this.expelCommand = expelCommand;
    }

    /**
     * Gets the radius used by the OBSIDIAN_SCOOPING flag to check for nearby obsidian blocks.
     *
     * @return the obsidian scooping radius (0-15)
     * @since 3.11.3
     */
    public int getObsidianScoopingRadius() {
        return obsidianScoopingRadius;
    }

    /**
     * Sets the radius used by the OBSIDIAN_SCOOPING flag to check for nearby obsidian blocks.
     *
     * @param obsidianScoopingRadius the radius to set (0-15)
     * @since 3.11.3
     */
    public void setObsidianScoopingRadius(int obsidianScoopingRadius) {
        this.obsidianScoopingRadius = Math.clamp(obsidianScoopingRadius, 0, 15);
    }

    /**
     * Gets the cooldown duration (in minutes) for the OBSIDIAN_SCOOPING flag.
     * After a player scoops obsidian into a lava bucket, they must wait this long
     * before they can scoop again. This prevents lava bucket duplication exploits.
     *
     * @return the obsidian scooping cooldown in minutes (minimum 1)
     * @since 3.11.4
     */
    public int getObsidianScoopingCooldown() {
        return obsidianScoopingCooldown;
    }

    /**
     * Sets the cooldown duration (in minutes) for the OBSIDIAN_SCOOPING flag.
     * After a player scoops obsidian into a lava bucket, they must wait this long
     * before they can scoop again. This prevents lava bucket duplication exploits.
     *
     * @param obsidianScoopingCooldown the cooldown duration in minutes (minimum 1)
     * @since 3.11.4
     */
    public void setObsidianScoopingCooldown(int obsidianScoopingCooldown) {
        this.obsidianScoopingCooldown = Math.max(1, obsidianScoopingCooldown);
    }

    /**
     * Gets the duration (in seconds) for showing the lava tip hologram above
     * newly formed obsidian blocks that can be scooped.
     *
     * @return the lava tip duration in seconds; 0 or less means disabled
     * @since 3.15.0
     */
    public int getObsidianScoopingLavaTipDuration() {
        return obsidianScoopingLavaTipDuration;
    }

    /**
     * Sets the duration (in seconds) for showing the lava tip hologram above
     * newly formed obsidian blocks that can be scooped.
     *
     * @param obsidianScoopingLavaTipDuration the duration in seconds; 0 or less disables
     * @since 3.15.0
     */
    public void setObsidianScoopingLavaTipDuration(int obsidianScoopingLavaTipDuration) {
        this.obsidianScoopingLavaTipDuration = obsidianScoopingLavaTipDuration;
    }

    /**
     * Gets the maximum number of history entries kept per island.
     *
     * @return the cap; 0 or less means unlimited
     * @since 3.22.4
     */
    public int getIslandHistoryMaxEntries() {
        return islandHistoryMaxEntries;
    }

    /**
     * Sets the maximum number of history entries kept per island.
     *
     * @param islandHistoryMaxEntries the cap; 0 or less means unlimited
     * @since 3.22.4
     */
    public void setIslandHistoryMaxEntries(int islandHistoryMaxEntries) {
        this.islandHistoryMaxEntries = islandHistoryMaxEntries;
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

    /**
     * @return the hideUsedBlueprints
     */
    public boolean isHideUsedBlueprints() {
        return hideUsedBlueprints;
    }

    /**
     * @param hideUsedBlueprints the hideUsedBlueprints to set
     */
    public void setHideUsedBlueprints(boolean hideUsedBlueprints) {
        this.hideUsedBlueprints = hideUsedBlueprints;
    }

    /**
     * @return the overrideSafetyCheck
     */
    public boolean isOverrideSafetyCheck() {
        return overrideSafetyCheck;
    }

    /**
     * @param overrideSafetyCheck the overrideSafetyCheck to set
     */
    public void setOverrideSafetyCheck(boolean overrideSafetyCheck) {
        this.overrideSafetyCheck = overrideSafetyCheck;
    }

    /**
     * @return the clickCooldownMs
     */
    public long getClickCooldownMs() {
        if (clickCooldownMs < 50) {
            clickCooldownMs = 50;
        }
        return clickCooldownMs;
    }

    /**
     * @param clickCooldownMs the clickCooldownMs to set
     */
    public void setClickCooldownMs(long clickCooldownMs) {
        this.clickCooldownMs = clickCooldownMs;
    }

    /**
     * @return the islandPurgeLevel
     */
    public int getIslandPurgeLevel() {
        return islandPurgeLevel;
    }

    /**
     * @param islandPurgeLevel the islandPurgeLevel to set
     */
    public void setIslandPurgeLevel(int islandPurgeLevel) {
        this.islandPurgeLevel = islandPurgeLevel;
    }

    /**
     * Vanilla structures disabled by default in every BentoBox game mode world.
     * @return the list of disabled structure keys, never {@code null}
     * @since 3.19.1
     */
    public List<String> getDisabledStructures() {
        return disabledStructures == null ? new ArrayList<>() : disabledStructures;
    }

    /**
     * @param disabledStructures the disabledStructures to set
     * @since 3.19.1
     */
    public void setDisabledStructures(List<String> disabledStructures) {
        this.disabledStructures = disabledStructures;
    }

    /**
     * @return whether unknown root commands trigger did-you-mean suggestions
     * @since 3.20.0
     */
    public boolean isDidYouMeanUnknownCommands() {
        return didYouMeanUnknownCommands;
    }

    /**
     * @param didYouMeanUnknownCommands the didYouMeanUnknownCommands to set
     * @since 3.20.0
     */
    public void setDidYouMeanUnknownCommands(boolean didYouMeanUnknownCommands) {
        this.didYouMeanUnknownCommands = didYouMeanUnknownCommands;
    }

    /**
     * @return whether unknown subcommands trigger did-you-mean suggestions
     * @since 3.20.0
     */
    public boolean isDidYouMeanSubcommands() {
        return didYouMeanSubcommands;
    }

    /**
     * @param didYouMeanSubcommands the didYouMeanSubcommands to set
     * @since 3.20.0
     */
    public void setDidYouMeanSubcommands(boolean didYouMeanSubcommands) {
        this.didYouMeanSubcommands = didYouMeanSubcommands;
    }

    /**
     * @return whether commands are registered with Paper's Brigadier system
     * @since 3.22.0
     */
    public boolean isUseBrigadierCommands() {
        return useBrigadierCommands;
    }

    /**
     * @param useBrigadierCommands the useBrigadierCommands to set
     * @since 3.22.0
     */
    public void setUseBrigadierCommands(boolean useBrigadierCommands) {
        this.useBrigadierCommands = useBrigadierCommands;
    }

}
