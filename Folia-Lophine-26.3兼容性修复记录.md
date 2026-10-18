# BentoBox Folia / Lophine 26.3 兼容性修复记录

## 结论

核心 BentoBox、Bank、Biomes、Challenges、Level、Warps 已按 Lophine `26.3.build.41-alpha` 测试基线完成修复和验证。

核心构建使用 `api-version: 26.1`，核心与已验证 addon 声明 `folia-supported: true`。正式服没有部署，所有破坏性验证只在隔离服 `lophine-26.3-test` 完成。

## 核心 BentoBox 修复

### Folia 调度

- 新增并统一使用 `TaskScheduler`，区分全局线程、异步线程、区域线程、实体线程和玩家线程。
- 将原先直接使用 Bukkit 主线程调度的启动、Housekeeping、玩家操作和等待逻辑迁移到对应的 Folia 调度器。
- 对跨线程操作使用 `CompletableFuture` 等待结果，调度失败和任务异常会传播给调用方。
- 玩家离线、实体失去归属区域、区域任务失败时返回明确失败结果，不静默吞掉异常。

### 建岛、重置和并发

- 建岛使用按世界和位置的锁，避免同一位置并发创建多个岛。
- reset 按清理、生成、保存和状态更新的顺序执行，取消和失败会恢复正确状态。
- 修复旧 owner 索引、换主共享目标和并发访问时的缓存一致性。
- 验证了并发建岛、访问、reset、取消 reset 和重启后的岛屿持久化。

### 世界生成和重生

- `WorldRegeneratorImpl` 只重放生成器实际覆写的现代生成阶段和 populator。
- 明确拒绝 legacy `generateChunkData` 路径，避免在 Folia 上错误调用旧接口。
- 保持世界、Nether 和 End 三维生成行为一致。

### 跨维度传送门（Nether / End）

根因：Folia 的传送门实现是异步的，`Entity.handlePortal()` 改为 `PortalProcessor.portalAsync()` → `portalToAsync(server.getLevel(...))`，目标维度在服务端代码里写死为服务器默认的 `minecraft:the_nether` / `minecraft:the_end`。Paper 的 `EntityPortalReadyEvent` 和 `PlayerPortalEvent` 在这条路径上都不会触发，所以基于 Paper 事件的重定向在 Folia 上永远不会执行，BentoBox 世界始终漏进默认维度。

修复：

- 接入 Luminol(Lophine) 的 `me.earthme.luminol.api.entity.PreEntityPortalEvent`（原生传送开始前触发、可取消）：新增 `FoliaPortalBridge`，用 `classExists` 判断 + `Bukkit.getPluginManager().registerEvent` 反射动态注册，BentoBox 不依赖 Luminol API 也能编译和在 Paper 上运行。
- 桥接判定：只接管 BentoBox 世界（`inWorld`）且该维度在配置中启用的传送。取消原生传送后合成等价的 `PlayerPortalEvent`（Nether → `NETHER_PORTAL`，End → `END_PORTAL`）走事件总线，复用现有 `onPlayerPortalEvent` / `portalProcess` 路由，保护类监听器（PortalListener 等）照常生效。非 BentoBox 世界和配置中关闭的维度不接管，标准维度（服务器自己的 `the_nether` / `the_end`）不受影响。
- 自定义维度（`islands: true`）和标准共享维度（`islands: false`）都由 BentoBox 决定目标世界，与 Paper 一致：`islands: false` 时进入 addon 自己的共享地狱/末地世界，从那里返回时回到自己的岛（BentoBox 配置注释里写明的行为）。差异只在于执行者——Paper 由服务端执行 `PlayerPortalEvent#setTo`，Folia 原生传送已被取消，所以由 BentoBox 自己执行：
  - `onPlayerPortalEvent` 的 NORMAL→Nether 标准维度分支、`handleToStandardNetherOrEnd`、`handleFromStandardNetherOrEnd` 增加 `TaskScheduler.isFolia()` 守卫的手动安全点传送，Paper 路径不变。
  - `getSpawnLocation(World)`：Folia 下跳过 `isSafeLocation(world.getSpawnLocation())`（`isSafeLocation` 会读目标世界方块，而这条路径跑在来源世界的区域线程上，会抛 `Cannot read world asynchronously`），直接返回世界出生点，由后续安全点传送兜底。
- 维度归属判定（`FoliaPortalBridge#environmentOf`）：正常情况看原生目标维度（目标为末地 → `THE_END`），但原生目标为主世界、且传送门位于末地世界时按 `THE_END` 处理。因为原版从末地离开时原生目标写死为主世界，如果按目标判定会被误当成地狱门，把玩家送进 addon 地狱而不是 addon 主世界。
- `portalProcess`：Folia 下 `create-and-link-portals: true` 时不再"交给服务端传送"（原生传送已被取消，服务端无法再建门），改为走 BentoBox 安全点传送；Nether 落点为蓝图自带的传送门或最近安全点。
- 删除 Folia 上永不触发的死代码：`PortalReadyListener`、`PlayerTeleportListener#onPlayerTeleport` 和注册分支 `hasPortalReadyApi()`。
- 回归测试：新增 `FoliaPortalBridgeTest`（接管/不接管判定、取消、事件合成）并在 `PlayerTeleportListenerTest` 增加 Folia 手动传送用例。

### Housekeeping 和 region 文件清理

Folia 在线运行时不能由插件直接删除服务器正在使用的 `.mca` 文件。现在的处理方式是：

1. 在线扫描只读取并筛选可清理的 region。
2. 将世界绝对路径、region 坐标和关联 island ID 写入 `plugins/BentoBox/database/deferred-purge.yml`。
3. 在线阶段不删除 region、entities 或 poi 文件，也不提前删除数据库 island row。
4. 下次启动创建 `PurgeRegionsService` 时，在世界和插件业务完全运行前恢复清单，删除三类文件。
5. 文件全部删除成功后才记录待收尾 island ID。
6. `islandsManager.load()` 完成后，才把恢复的 island ID 放入数据库收尾队列。
7. 数据库删除成功后才清理岛屿缓存；数据库删除失败会保留队列，下一次继续重试。

清理清单具备以下保护：

- 任一 region、entities 或 poi 文件删除失败，整条任务保留。
- 文件删除不完整时不会删除数据库 island row。
- 相同世界和相同 region 坐标的重复任务不会重复追加。
- 清单保存失败时 `delete()` 返回失败，调用方可以保留数据库状态并报告错误。
- 删除完成后清单变为 `purges: []`。
- 在线 Folia 不再执行不安全的直接 region 删除。

### Purge 边界修复

- 删除 sidecar 文件失败时保留主 region 文件，下一轮可以重新发现并重试。
- 部分 region 删除成功、部分失败时不执行数据库收尾。
- 跨 region 的岛屿如果仍有 residual region，保留数据库行。
- deleted sweep 的 island row 延迟到安全时机收尾。
- pinned chunk 会阻止不安全的清理，避免缓存中的区块被重新写回文件。
- 普通服务器仍保留原有同步 chunk eviction 和清理路径；Folia 使用区域任务并等待所有区域任务完成。

## Addon 修复

### Bank

- 固定 Vault / Economy 上下文，避免异步调用期间上下文漂移。
- 对同一账户的并发支付串行化，避免余额检查和扣款之间发生竞态。
- 支付成功、支付失败、退款失败和未知支付结果分别处理。
- 未知结果不会盲目重试，避免重复扣款。

### Biomes

- 固定 provider、账户、世界和金额上下文。
- 跨区块、跨区域线程更新使用 Folia 区域调度。
- 购买、缺货、退款和重复请求均有明确补偿路径。

### Challenges、Level、Warps

- 修正与核心 Folia 生命周期和调度器的交互。
- 保持 addon enable、world hook、Level ranking、Warps hook 和事件回调顺序。
- 重新构建并在隔离 Lophine 服共同加载验证。

## 测试结果

### 核心自动化测试

执行：

```text
gradlew.bat test shadowJar --no-daemon
```

结果：

- 3757 tests completed
- 0 failures
- 0 errors
- 13 skipped
- `shadowJar` 构建成功

门传送修复后：

- 3772 tests completed
- 0 failures
- 0 errors
- 13 skipped
- `shadowJar` 和探针 jar 构建成功
- 其中 `FoliaPortalBridgeTest` 9 项（接管/不接管判定、取消、事件合成、维度归属），`PlayerTeleportListenerTest` 覆盖 Folia 手动传送（含标准维度自传送）用例

延期清理专用回归测试覆盖：

- Folia 区域驱逐失败时不删除文件。
- 文件删除失败时不加入恢复队列、不删除 island row。
- 三类文件全部恢复删除后才进入数据库收尾。
- 数据库删除失败时保留 pending ID。
- 重复恢复不会重复执行同一批 region 清理。
- islands load 完成前不会执行数据库收尾。

### 隔离 Lophine 服

隔离服加载了：

- BentoBox
- Vault
- XConomy
- Bank
- Biomes
- Challenges
- Level
- Warps

已经实测通过：

- 三维世界生成。
- 并发建岛、访问和 reset。
- 取消 reset 的安全性。
- 重启后的岛屿持久化。
- 世界保存。
- pinned chunk 清理保护。
- Biomes 跨区块更新。
- Bank 并发扣款。
- Vault 扣款。
- Bank/Vault 退款。
- 重复购买幂等。
- 缺货退款。
- 隔离服启动时五个 addon 正常启用。
- 隔离服真实假人跨维度传送验收（`-Dprobe.portalOnly=true`）。两组配置各跑一条完整链：

  生产同配置（BSkyBlock `nether.islands: true` / `end.islands: true`、`create-and-link-portals: false`）：

  - Nether：`bskyblock_world` 传送门 → `bskyblock_world_nether`。
  - Nether（`create-and-link-portals: true`，早前另测）：→ `bskyblock_world_nether`。
  - End：`bskyblock_world` 末地门 → `bskyblock_world_the_end`。
  - 返程（地狱）：`bskyblock_world_nether` 传送门 → `bskyblock_world`（不会掉进服务器默认主世界）。
  - 标准维度不受影响：服务器自己的主世界 `bskyblock` 传送门 → 服务器自己的地狱 `bskyblock_nether`（桥接不接管非 BentoBox 世界）。
  - 同一次运行中 `EntityPortalReadyEvent` 与 `PlayerTeleportEvent(NETHER_PORTAL)` 的诊断均未出现，确认删除的是死代码。
  - 日志证据：`C:\服务器\空岛服务器-test\portal-final-console.log`（`PORTAL_PROBE_PASSED: nether, end, nether return and standard portals route correctly`），门内等待加路由约 2-3 秒。Folia 不发 `PlayerChangedWorldEvent`，探针按世界轮询判断。

  共享维度配置（临时把 `nether.islands` / `end.islands` 改为 `false`，验证 Paper 的"标准维度"语义在 Folia 上同样成立，跑完已还原为 `true`）：

  - Nether：`bskyblock_world` 传送门 → addon 自己的 `bskyblock_world_nether`（不是服务器的 `bskyblock_nether`）。
  - End：`bskyblock_world` 末地门 → `bskyblock_world_the_end`。
  - 返程（地狱）：`bskyblock_world_nether` 传送门 → `bskyblock_world`（回到自己的岛，符合配置注释"entering a portal will return them back to their islands"）。
  - 标准维度仍然不受影响：`bskyblock` → `bskyblock_nether`。
  - 日志证据：`C:\服务器\空岛服务器-test\portal-shared5-console.log`。

  探针说明：该测试服没有岛屿蓝图，`createIsland` 只建记录不贴方块，所以返程用例会自己用 `BlueprintPaster` 在岛中心贴一小块地面，模拟真实岛屿（第一次跑失败就是因为目的地是虚空、安全点搜索找不到落点）。这也意味着 `islands: false` 下"没有岛、addon 世界也没有 spawn 岛"的玩家没有可用落点，见"已知限制"。

- 末地出口门（addon 末地里的 `minecraft:end_portal`）在 lophine-26.3 上不触发任何可挂钩事件，见下方"已知限制"。
- 正式服尚未部署，尚未做正式服真实玩家门传送验收。

此外，使用隔离服中的独立恢复 fixture 验证了：

- `region/r.-987.654.mca` 被删除。
- `entities/r.-987.654.mca` 被删除。
- `poi/r.-987.654.mca` 被删除。
- `deferred-purge.yml` 清空为 `purges: []`。
- BentoBox 重启后继续正常加载并通过启动探针。

## 已知限制

### 末地出口门在 lophine-26.3 上无法接管

在 addon 末地世界（`bskyblock_world_the_end`）里进入 `minecraft:end_portal` 方块时，lophine-26.3 不触发任何可挂钩事件：

- `PreEntityPortalEvent`（桥接依赖的事件）不触发——假人验收里该次传送前后没有任何事件日志。
- `PlayerRespawnEvent`（原版 Paper 上 exit portal 走的事件，BentoBox 的 `onPlayerExitPortal` 依赖它）不触发，因此 BentoBox 现有的末地出口路由在 Folia 上是死代码。
- `EntityTeleportAsyncEvent`、`PostEntityPortalEvent` 也不触发这次传送，且它们本身不可取消。

结果：玩家会被送到服务器自己的主世界（原生目标写死为主世界），而不是 addon 主世界；假人验收日志为 `PORTAL_END_RETURN_UNSUPPORTED: landed in bskyblock`。BentoBox 侧当前无事件可用，属于服务端 API 缺口，无法在插件内修复。

影响范围和缓解：

- 只有在 addon 末地里真的存在 `minecraft:end_portal`（例如打完末影龙生成的出口门、或蓝图自带出口门）时才会遇到；玩家用 `/is` 回岛不受影响。
- 需要彻底避免时，可在 BSkyBlock 配置里禁止末影龙/末地出口门。
- 如果 Luminol 后续把这条路径也接进 `PreEntityPortalEvent`，桥接的维度归属判定已经把它当作 `THE_END` 处理并路由到 addon 主世界（`FoliaPortalBridgeTest#testLeavingTheAddonEndIsReroutedAsAnEndPortal` 覆盖），届时无需再改代码。

### 其他

- Folia 下 `create-and-link-portals: true` 时不会新建落点传送门：原生传送已被取消，服务端不再有机会建门，落点改为蓝图自带的门或最近安全点。
- `islands: false`（共享维度）下，返回时 BentoBox 的落点是"该玩家的岛；没有岛则用 addon 世界的 spawn 岛、再不行用世界出生点"。在 Folia 上世界出生点不会再做安全性预检（见上文 `getSpawnLocation`），如果那里是虚空，安全点搜索找不到落点，玩家会留在原地。有岛的正常玩家不受影响；需要照顾无岛玩家时，给 addon 世界配一个 spawn 岛即可。
- 非玩家实体穿门不由桥接处理（直接放行给服务端），末地 gateway 同理（Luminol 在这条路径上只暴露不可修改的 `PortalLocateEvent`）。

## 构建产物和范围

核心发布包：

```text
BentoBox/build/libs/BentoBox-3.23.4-SNAPSHOT-LOCAL.jar
```

已检查发布 jar 不包含 `FoliaProbe`、探针类或测试探针代码。探针只存在于隔离服插件目录，未进入发布包。

正式服 `C:\服务器\多线程26.3服务端` 未停服、未替换 jar、未修改存档。`C:\服务器\空岛服务器` 不属于本次 BentoBox 测试范围。

## 运行注意事项

Folia 在线清理会产生延期清单，文件删除发生在后续启动早期。这是为了避免和在线 region 文件句柄竞争。必须保持 BentoBox 数据目录可写；如果清单无法保存，清理操作会报告失败并保留数据库数据。

本次完成的是代码、自动化测试和隔离服验证。正式服部署、正式服停服窗口和正式存档备份仍需单独安排，当前没有执行部署。
