/**
 * BentoBox Gradle Build Configuration
 * 
 * This build script configures the compilation, testing, packaging, and publishing
 * of the BentoBox Minecraft plugin. It handles:
 * - Java 21 compilation with proper module access
 * - Multi-repository dependency resolution
 * - JAR shading and minimization
 * - Test execution with JUnit 5
 * - Code coverage reporting with JaCoCo
 * - Maven publication to the BentoBox repository
 */

// ============================================================================
// PLUGINS: Core build functionality
// ============================================================================
// Apply necessary plugins for Java development, publishing, testing, and shading
plugins {
    // Standard Java development plugin - provides compile, test, jar tasks
    java
    
    // Maven Publishing - allows publishing artifacts to Maven repositories
    `maven-publish` 

    // JaCoCo (Java Code Coverage) - generates code coverage reports for CI/CD
    id("jacoco")

    // Shadow Plugin - shades (embeds) dependencies into the final JAR and minimizes unused code
    id("com.gradleup.shadow") version "9.3.0"

    // Paperweight UserDev - simplifies development against PaperMC with proper mappings and reobfuscation
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    
    // Sonarcube
    id("org.sonarqube") version "7.2.1.6560"
}

// Add paperweight reobf configuration so the userdev plugin reobfuscates artifacts
// using the Mojang production mappings as required by paperweight-userdev.
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

// ============================================================================
// PROJECT COORDINATES & VERSIONING
// ============================================================================
// These properties define the artifact's identity in the Maven repository
group = "world.bentobox" // From <groupId>

// Base properties from <properties>
val buildVersion = "3.10.2"
val buildNumberDefault = "-LOCAL" // Local build identifier
val snapshotSuffix = "-SNAPSHOT"  // Indicates development/snapshot version

// CI/CD Logic (Translates Maven <profiles>)
// Default version format: 3.10.2-LOCAL-SNAPSHOT
var finalBuildNumber = buildNumberDefault
var finalRevision = "$buildVersion$finalBuildNumber$snapshotSuffix"

// 'ci' profile logic: Activated by env.BUILD_NUMBER from CI/CD pipeline
// Overrides build number with actual CI build number
val envBuildNumber = System.getenv("BUILD_NUMBER")
if (!envBuildNumber.isNullOrBlank()) {
    finalBuildNumber = "-b$envBuildNumber"
    finalRevision = "$buildVersion$finalBuildNumber$snapshotSuffix"
}

// 'master' profile logic: Activated when building from origin/master branch
// Removes -LOCAL and -SNAPSHOT suffixes for release builds
val envGitBranch = System.getenv("GIT_BRANCH")
if (envGitBranch == "origin/master") {
    finalBuildNumber = "" // No build number for releases
    finalRevision = buildVersion // Clean version number
}

version = finalRevision

// ============================================================================
// DEPENDENCY VERSIONS
// ============================================================================
// Centralized version management for all external dependencies
val javaVersion = "21"
val junitVersion = "5.10.2"
val mockitoVersion = "5.11.0"
val mockBukkitVersion = "v1.21-SNAPSHOT"
val mongodbVersion = "3.12.12"
val mariadbVersion = "3.0.5"
val mysqlVersion = "8.0.27"
val postgresqlVersion = "42.2.18"
val hikaricpVersion = "5.0.1"
val spigotVersion = "1.21.10-R0.1-SNAPSHOT"
val paperVersion = "1.21.10-R0.1-SNAPSHOT"
val bstatsVersion = "3.0.0"
val vaultVersion = "1.7.1"
val levelVersion = "2.21.3"
val placeholderapiVersion = "2.11.7"
val myworldsVersion = "1.19.3-v1"

// Store versions in extra properties for resource filtering (used in plugin.yml, config.yml)
extra["java.version"] = javaVersion
extra["junit.version"] = junitVersion
extra["mockito.version"] = mockitoVersion
extra["mock-bukkit.version"] = mockBukkitVersion
extra["mongodb.version"] = mongodbVersion
extra["mariadb.version"] = mariadbVersion
extra["mysql.version"] = mysqlVersion
extra["postgresql.version"] = postgresqlVersion
extra["hikaricp.version"] = hikaricpVersion
extra["spigot.version"] = spigotVersion
extra["paper.version"] = paperVersion
extra["bstats.version"] = bstatsVersion
extra["vault.version"] = vaultVersion
extra["level.version"] = levelVersion
extra["placeholderapi.version"] = placeholderapiVersion
extra["myworlds.version"] = myworldsVersion
extra["build.version"] = buildVersion
extra["build.number"] = finalBuildNumber
extra["revision"] = finalRevision


// ============================================================================
// JAVA CONFIGURATION
// ============================================================================
// Configures Java compiler and toolchain settings
java {
    // Use Java 21 toolchain for compilation (enforced regardless of JVM running Gradle)
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.withType<JavaCompile> {
    // Ensure UTF-8 encoding for all source files
    options.encoding = "UTF-8"
}


// ============================================================================
// REPOSITORIES
// ============================================================================
// Defines where dependencies are downloaded from (in order of precedence)
repositories {
    // Gradle Plugin Portal - for resolving Gradle plugins
    gradlePluginPortal()
    // PaperMC Maven Repository - for Paper API and related libraries
    maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" } // Paper API
    // Standard Maven Central Repository - most common Java libraries
    mavenCentral() 

    // Custom repositories for Minecraft and plugin-specific libraries
    maven("https://jitpack.io") { name = "JitPack" } // GitHub repository packages
    maven("https://repo.codemc.org/repository/maven-public") { name = "CodeMC-Public" }
    maven("https://libraries.minecraft.net/") { name = "MinecraftLibs" } // Official Minecraft libraries
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") { name = "Spigot-Snapshots" }
    maven("https://repo.codemc.io/repository/nms/") { name = "NMS-Repo" } // NMS (internal Minecraft code)
    maven("https://ci.mg-dev.eu/plugin/repository/everything") { name = "MG-Dev-CI" }
    maven("https://repo.onarandombox.com/multiverse-releases") { name = "Multiverse-Releases" }
    maven("https://repo.onarandombox.com/multiverse-snapshots") { name = "Multiverse-Snapshots" }
    maven("https://mvn.lumine.io/repository/maven-public/") { name = "Lumine-Releases" } // Mythic mobs
    maven("https://repo.clojars.org/") { name = "Clojars" }
    maven("https://repo.fancyplugins.de/releases") { name = "FancyPlugins-Releases" }
    maven("https://repo.pyr.lol/snapshots") { name = "Pyr-Snapshots" }
    maven("https://maven.devs.beer/") { name = "MatteoDev" }
    maven("https://repo.oraxen.com/releases") { name = "Oraxen" } // Custom items plugin
    maven("https://repo.codemc.org/repository/bentoboxworld/") { name = "BentoBoxWorld-Repo" }
    maven("https://repo.extendedclip.com/releases/") { name = "Placeholder-API-Releases" }
}


// ============================================================================
// DEPENDENCIES
// ============================================================================
// Defines all external libraries needed for compilation and testing

dependencies {
    // --- Test Dependencies: Only used during testing, not in production ---
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion") // Mocking framework
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("com.github.MockBukkit:MockBukkit:$mockBukkitVersion") // Bukkit mock server
    testImplementation("org.awaitility:awaitility:4.2.2") // Async testing helper
    testImplementation("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT") // Paper API for tests
    testImplementation("com.github.MilkBowl:VaultAPI:$vaultVersion")
    testImplementation("me.clip:placeholderapi:$placeholderapiVersion")

    // --- Provided/Compile-Only Dependencies: Available at compile time but provided by server ---
    // These are NOT shaded into the final JAR (the server provides them at runtime)
    //compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT") // Bukkit/Spigot/Paper API
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
    
    // Spigot NMS - Used for internal Minecraft code (chunk deletion and pasting)
    compileOnly("org.spigotmc:spigot:$spigotVersion") {
        exclude(group = "org.spigotmc", module = "spigot-api") // Already provided by Paper
    }

    // Optional plugins that may be installed on the server
    compileOnly("org.mongodb:mongodb-driver:$mongodbVersion")
    compileOnly("com.zaxxer:HikariCP:$hikaricpVersion") // Database connection pooling
    compileOnly("com.github.MilkBowl:VaultAPI:$vaultVersion") // Economy/permission API
    compileOnly("me.clip:placeholderapi:$placeholderapiVersion") // Placeholder API
    compileOnly("com.bergerkiller.bukkit:MyWorlds:$myworldsVersion") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("io.lumine:Mythic-Dist:5.9.5") // Mythic Mobs
    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.0.0-SNAPSHOT")
    compileOnly("com.onarandombox.multiversecore:multiverse-core:4.3.16") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("com.github.apachezy:LangUtils:3.2.2")
    compileOnly("com.github.Slimefun:Slimefun4:RC-37") // Slimefun custom items
    compileOnly("dev.lone:api-itemsadder:4.0.2-beta-release-11") // ItemsAdder custom items
    compileOnly("de.oliver:FancyNpcs:2.4.4") // NPC plugin
    compileOnly("lol.pyr:znpcsplus-api:2.0.0-SNAPSHOT") // Alternative NPC plugin
    compileOnly("de.oliver:FancyHolograms:2.4.1") // Hologram plugin
    compileOnly("world.bentobox:level:2.21.3-SNAPSHOT") // BentoBox Level addon

    // Apache Commons Lang - utility library
    compileOnly("commons-lang:commons-lang:2.6")
    testImplementation("commons-lang:commons-lang:2.6")

    // --- Implementation Dependencies: Shaded into final JAR ---
    // These are embedded in the final JAR since they're not commonly available
    implementation("org.bstats:bstats-bukkit:$bstatsVersion") // Plugin metrics
    implementation("javax.xml.bind:jaxb-api:2.3.0") // XML serialization
    implementation("com.github.Marcono1234:gson-record-type-adapter-factory:0.3.0") // JSON serialization
    implementation("org.eclipse.jdt:org.eclipse.jdt.annotation:2.2.600") // Nullability annotations
    implementation("com.github.puregero:multilib:1.1.13") // Multi-library support

    // Oraxen with custom exclusions (embed only what we need)
    compileOnly("io.th0rgal:oraxen:1.193.1") {
        exclude(group = "me.gabytm.util", module = "actions-spigot")
        exclude(group = "org.jetbrains", module = "annotations")
        exclude(group = "com.ticxo", module = "PlayerAnimator")
        exclude(group = "com.github.stefvanschie.inventoryframework", module = "IF")
        exclude(group = "io.th0rgal", module = "protectionlib")
        exclude(group = "dev.triumphteam", module = "triumph-gui")
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "com.jeff-media", module = "custom-block-data")
        exclude(group = "com.jeff-media", module = "persistent-data-serializer")
        exclude(group = "com.jeff_media", module = "MorePersistentDataTypes")
        exclude(group = "gs.mclo", module = "java")
    }
}

paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
  javaLauncher = javaToolchains.launcherFor {
    // Example scenario:
    // Paper 1.17.1 was originally built with JDK 16 and the bundle
    // has not been updated to work with 21+ (but we want to compile with a 25 toolchain)
        // Use the project's configured Java version for paperweight tools (needs Java 21+)
        languageVersion = JavaLanguageVersion.of(javaVersion)
  }
}

sonar {
  properties {
    property("sonar.projectKey", "BentoBoxWorld_BentoBox")
    property("sonar.organization", "bentobox-world")
  }
}


// ============================================================================
// RESOURCE PROCESSING
// ============================================================================
// Filters and copies resources (plugin.yml, config files, locales) to build output

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    from(sourceSets.main.get().resources.srcDirs)
    
    // Replace variables in plugin.yml and config.yml with actual version strings
    // This allows version info to be read at runtime by the plugin
    filesMatching(listOf("plugin.yml", "config.yml")) {
        filter { line ->
            line.replace("\${mysql.version}", mysqlVersion)
                .replace("\${mariadb.version}", mariadbVersion)
                .replace("\${postgresql.version}", postgresqlVersion)
                .replace("\${mongodb.version}", mongodbVersion)
                .replace("\${hikaricp.version}", hikaricpVersion)
                .replace("\${build.number}", finalBuildNumber)
                .replace("\${project.version}", project.version.toString())
                .replace("\${project.description}", project.description ?: "")
                .replace("\${revision}", project.version.toString())
        }
    }
    
    finalizedBy("copyLocales")
}

// Copy locale files without filtering (prevents corruption of translation files)
tasks.register<Copy>("copyLocales") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/resources/locales")
    into("${tasks.processResources.get().destinationDir}/locales")
}

// Ensure test compilation waits for locale files to be copied
tasks.compileTestJava {
    dependsOn("copyLocales")
}

// Set the final JAR filename to match project name and version
tasks.jar {
    archiveFileName.set("${project.name}-${project.version}.jar")
}


// ============================================================================
// JAR SHADING & MINIMIZATION
// ============================================================================
// Shadow Plugin: Embeds dependencies into JAR and removes unused code
// This creates a "fat JAR" with all required dependencies

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    // Enable minimization: removes unused classes/methods from shaded dependencies
    // Reduces JAR size significantly
    minimize()

    // Exclude these artifacts from being shaded (already provided by server or incompatible)
    exclude(
        "org.apache.maven:*:*",           // Maven tools not needed at runtime
        "com.google.code.gson:*:*",       // Often provided by server
        "org.mongodb:*:*",                // Optional dependency
        "org.eclipse.jdt:*:*"             // Optional dependency
    )

    // Relocate (rename) packages to avoid conflicts with other plugins
    // This prevents "duplicate class" errors when multiple plugins have same dependency
    relocate("org.bstats", "world.bentobox.bentobox.util.metrics")
    relocate("io.papermc.lib", "world.bentobox.bentobox.paperlib")
    relocate("com.github.puregero.multilib", "world.bentobox.bentobox.multilib")
    
    // Remove the "-all" suffix from the shaded JAR filename
    archiveClassifier.set("")
}

// Make the shaded JAR the primary artifact for the 'build' task
tasks.build {
    dependsOn(tasks.shadowJar)
}

// ============================================================================
// TEST EXECUTION
// ============================================================================
// Configures JUnit 5 testing with special Java module access for Java 21

tasks.test {
    // Use JUnit Platform (required for JUnit 5)
    useJUnitPlatform()

    // Enable Java 21 preview features and dynamic agent loading
    jvmArgs("--enable-preview", "-XX:+EnableDynamicAgentLoading")
    
    // Add --add-opens: Required for Java 21+ to allow reflection access to restricted modules
    // Necessary for mocking frameworks and other testing utilities
    val openModules = listOf(
        "java.base/java.lang", "java.base/java.math", "java.base/java.io", "java.base/java.util",
        "java.base/java.util.stream", "java.base/java.text", "java.base/java.util.regex",
        "java.base/java.nio.channels.spi", "java.base/sun.nio.ch", "java.base/java.net",
        "java.base/java.util.concurrent", "java.base/sun.nio.fs", "java.base/sun.nio.cs",
        "java.base/java.nio.file", "java.base/java.nio.charset", "java.base/java.lang.reflect",
        "java.logging/java.util.logging", "java.base/java.lang.ref", "java.base/java.util.jar",
        "java.base/java.util.zip", "java.base/java.security", "java.base/jdk.internal.misc"
    )

    for (module in openModules) {
        jvmArgs("--add-opens", "$module=ALL-UNNAMED")
    }
}

// ============================================================================
// CODE COVERAGE (JACOCO)
// ============================================================================
// Generates code coverage reports to measure test coverage

tasks.jacocoTestReport {
    reports {
        xml.required.set(true) // XML format for CI/CD tools like SonarCloud
        html.required.set(true) // HTML format for human viewing
    }

    // Exclude certain classes from coverage analysis
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude("**/*Names*", "org/bukkit/Material*") // Generated/external classes
        }
    )
}

// ============================================================================
// JAVADOC & SOURCE ARTIFACTS
// ============================================================================
// Creates additional JARs for publication: sources and javadoc

tasks.javadoc {
    source = sourceSets.main.get().allJava
    options {
        (this as StandardJavadocDocletOptions).apply {
            // Suppress warnings and keep output quiet
            addStringOption("Xdoclint:none", "-quiet")
            source = javaVersion
        }
    }
}

// Creates BentoBox-<version>-sources.jar containing all source code
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Creates BentoBox-<version>-javadoc.jar containing generated documentation
tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// ============================================================================
// PUBLICATION TO MAVEN REPOSITORY
// ============================================================================
// Publishes build artifacts to the BentoBox Maven repository

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // Use the shaded (shadow) JAR as the main artifact, not the plain JAR
            artifact(tasks.shadowJar.get()) {
                builtBy(tasks.shadowJar)
            }
            
            // Also attach source code and javadoc for developers
            artifact(tasks.getByName("sourcesJar"))
            artifact(tasks.getByName("javadocJar"))

            // Set Maven coordinates
            groupId = project.group as String
            artifactId = rootProject.name
            version = project.version as String
        }
    }
    
    // Configure publication target repository
    repositories {
        maven {
            name = "bentoboxworld"
            url = uri("https://repo.codemc.org/repository/bentoboxworld/") // Where artifacts are uploaded
        }
    }
}

// ============================================================================
// ARCHIVE NAMING
// ============================================================================
// Sets the base name for all generated artifacts

base {
    archivesName.set("BentoBox") // Final JARs will be: BentoBox-<version>.jar, etc.
}
