# 用本仓库的 API 写附属插件

本仓库是 BentoBox 的 Folia / Lophine 26.3 适配分支。对外 API 在
`src/main/java/world/bentobox/bentobox/api/`，附属插件通过它挂到 BentoBox 上（`Addon` /
`GameModeAddon`、`IslandEvent`、`Island`、`User`、`Panel`、`Flags` 等）。

## 1. 把 API 装进本地 Maven 仓库

```bash
./gradlew publishToMavenLocal
```

产物坐标（默认版本号带 `-LOCAL` 后缀）：

```xml
<groupId>world.bentobox</groupId>
<artifactId>bentobox</artifactId>
<version>3.23.4-SNAPSHOT-LOCAL</version>
```

同时会装 `-sources.jar`。版本号由 `build.gradle.kts` 里的 `buildVersion` +
`snapshotSuffix` + `buildNumberDefault` 拼出来，换了参数就跟着变。

## 2. Gradle 依赖

```kotlin
repositories {
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("world.bentobox:bentobox:3.23.4-SNAPSHOT-LOCAL")
}

// Paper 的类：和本仓库一样走 paperweight userdev，版本号取自 build.gradle.kts 的 paperVersion
paperweight.paperDevBundle("26.3.build.41-alpha")
```

不用 paperweight 的话，也可以直接依赖 `io.papermc.paper:paper-api`，版本要和服务端一致
（Paper 的 artifact 版本号与服务端 build 号不是一回事，别照抄上面的 dev bundle 版本）。

## 3. Maven 依赖

```xml
<dependency>
  <groupId>world.bentobox</groupId>
  <artifactId>bentobox</artifactId>
  <version>3.23.4-SNAPSHOT-LOCAL</version>
  <scope>provided</scope>
</dependency>
```

**必须是 `compileOnly` / `provided`**：BentoBox 由服务端提供，打进附属插件 jar 会和主插件
类冲突。

## 4. 你的 plugin.yml

```yaml
name: MyAddon
main: com.example.myaddon.MyAddon
api-version: '1.20'
folia-supported: true
depend: [BentoBox]
```

`folia-supported: true` 是必须的：本分支跑在 Folia 上，没有这个标记服务端会拒绝加载。
跨区域操作要用 `Bukkit.getRegionScheduler()` / 实体的 `getScheduler()`，不能直接在主线程读写世界。

## 5. 编译要求

Java 25（见 `build.gradle.kts` 的 `javaVersion`）。

## 已知限制

- **发布的 pom 不带传递依赖**。上游用 `compileOnly` 引 Bukkit/Paper，所以 Bukkit、Adventure、
  Gson 这些类要你自己在插件项目里声明。
- **javadoc jar 目前是空的**（paperweight 的映射源码带 `@NotNull` 注解，javadoc 报错）。
  看文档请用 `-sources.jar`，或直接读仓库里的源码。
- 没有发布到公共 Maven 仓库。想让机器外的人也能依赖，得先决定走 JitPack（需要在
  JitPack 上跑通 paperweight 构建）还是 GitHub Packages（需要 token）。
