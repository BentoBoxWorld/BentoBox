/**
 * BentoBox Build Configuration (Gradle Kotlin DSL)
 * 
 * This is a direct translation of the Maven pom.xml to Gradle's Kotlin DSL.
 * It defines the complete build process for BentoBox, a Paper plugin framework for island-type gamemodes.
 * 
 * Key Features:
 * - Java 21 compilation with module access configuration for testing
 * - Shadow JAR creation with minimization and class relocation
 * - Maven artifact publishing to CodeMC repository  
 * - JaCoCo code coverage reporting for SonarCloud
 * - Placeholder expansion in resource files (plugin.yml, config.yml)
 * - CI/CD integration with environment variable-based versioning
 */

// ============================================================================
// PLUGINS
// ============================================================================
// 
// Applies Gradle plugins that extend build capabilities with new tasks and features
//
plugins {
    // Standard Java development plugin - provides compile, test, jar, and javadoc tasks
    // Documentation: https://docs.gradle.org/current/userguide/java_plugin.html
    java
    
    // Maven publishing plugin - enables artifact publication to Maven repositories
    // Translates Maven's <distributionManagement> section
    // Documentation: https://docs.gradle.org/current/userguide/publishing_maven.html
    `maven-publish` 

    // JaCoCo plugin - generates test code coverage reports
    // Translates Maven's jacoco-maven-plugin
    // Used to track test coverage metrics for SonarCloud integration
    id("jacoco")

    // Shadow JAR plugin (v9.3.0 from com.gradleup - the actively maintained fork)
    // Creates an "uber JAR" that bundles all dependencies into a single file
    // - Version 9.3.0 provides Java 21 compatibility (critical for this project)
    // - Translates Maven's maven-shade-plugin
    // - Handles JAR minimization, class relocation, and exclusion logic
    id("com.gradleup.shadow") version "9.3.0" 
}

// ============================================================================
// PROJECT COORDINATES AND VERSIONING
// ============================================================================
//
// Sets up the Maven artifact coordinates (group ID, artifact ID, version)
// and implements CI/CD-aware versioning based on environment variables.
//
// Translates Maven: <groupId>, <artifactId>, <version>, and <profiles>
//

// Maven group ID - reverse domain notation identifying the organization
group = "world.bentobox"

// Base version properties from <properties> in Maven
val buildVersion = "3.10.2"
val buildNumberDefault = "-LOCAL"
val snapshotSuffix = "-SNAPSHOT"

// Initialize version with default (local) values
var finalBuildNumber = buildNumberDefault
var finalRevision = "$buildVersion$finalBuildNumber$snapshotSuffix"

/**
 * CI/CD Profile Logic
 * 
 * Activated when BUILD_NUMBER environment variable is set (Jenkins CI builds)
 * Sets version to: 3.10.2-b<BUILD_NUMBER>-SNAPSHOT
 * Example: 3.10.2-b42-SNAPSHOT for build #42
 */
val envBuildNumber = System.getenv("BUILD_NUMBER")
if (!envBuildNumber.isNullOrBlank()) {
    finalBuildNumber = "-b$envBuildNumber"
    finalRevision = "$buildVersion$finalBuildNumber$snapshotSuffix"
}

/**
 * Master/Release Profile Logic
 * 
 * Activated when GIT_BRANCH environment variable equals "origin/master"
 * Sets version to: 3.10.2 (removes -SNAPSHOT suffix for release builds)
 * This ensures release builds have clean version numbers without pre-release markers
 */
val envGitBranch = System.getenv("GIT_BRANCH")
if (envGitBranch == "origin/master") {
    finalBuildNumber = "" 
    finalRevision = buildVersion 
}

// Apply the calculated version to the project
version = finalRevision

// ============================================================================
// GLOBAL PROPERTIES AND DEPENDENCY VERSIONS
// ============================================================================
//
// Centralizes all version numbers for dependencies and Java toolchain.
// Defined in two places:
// 1. Kotlin local variables (for use in the build script)
// 2. 'extra' properties map (for use in resource filtering like plugin.yml)
//
// This dual approach ensures consistency between:
// - Build classpath versions (what we compile against)
// - Runtime library declarations in plugin.yml (what the server loads)
//
// Translates Maven: <properties> section
//

val javaVersion = "21"

// Library versions - centralized for easy updates
val paperVersion = "1.21.10-R0.1-SNAPSHOT"
val spigotVersion = "1.21.10-R0.1-SNAPSHOT"

// Testing framework versions
val junitVersion = "5.10.2"
val mockitoVersion = "5.11.0"
val mockBukkitVersion = "v1.21-SNAPSHOT"

// Dependency versions - these must match what's declared in plugin.yml resources
val bstatsVersion = "3.0.2"
val mysqlVersion = "8.0.27"        // For plugin.yml: library declaration and resource filtering
val mariadbVersion = "3.0.5"       // For plugin.yml: library declaration and resource filtering
val postgresqlVersion = "42.2.18"  // For plugin.yml: library declaration and resource filtering
val mongodbVersion = "3.12.12"     // For plugin.yml: library declaration and resource filtering
val hikaricpVersion = "5.0.1"      // For plugin.yml: library declaration and resource filtering

// Plugin API versions
val vaultVersion = "1.7.1"
val placeholderapiVersion = "2.11.7"

// World/addon management versions
val myworldsVersion = "2.90"

/**
 * Store all versions in 'extra' properties map
 * 
 * These values are available globally in this build file as: extra["mysql.version"]
 * More importantly, they're used in the processResources task to replace placeholders
 * in plugin.yml and config.yml during the build.
 * 
 * Example: ${mysql.version} in plugin.yml becomes 8.0.27 in the built JAR
 */
extra["java.version"] = javaVersion
extra["mysql.version"] = mysqlVersion
extra["mariadb.version"] = mariadbVersion
extra["postgresql.version"] = postgresqlVersion
extra["mongodb.version"] = mongodbVersion
extra["hikaricp.version"] = hikaricpVersion

// ============================================================================
// JAVA CONFIGURATION
// ============================================================================
//
// Configures the Java compilation environment:
// - Sets Java 21 as the target/source version
// - Configures UTF-8 source file encoding
//
// The 'toolchain' feature ensures consistent Java versions across different
// developer machines and CI environments without requiring a pre-installed JDK
// matching the exact version. Gradle can automatically download the specified version.
//

java {
    // Configures Java toolchain for compilation and runtime
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

// Apply UTF-8 encoding to all Java compilation tasks
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8" // Ensures consistent encoding across all platforms
}

// ============================================================================
// MAVEN REPOSITORIES
// ============================================================================
//
// Defines all Maven repositories from which dependencies are fetched during build.
// Repositories are searched in order, so more specific repositories should come first.
//
// Translates Maven: <repositories> section
//

repositories {
    // Central Maven Repository - standard location for Java/Gradle libraries
    // Contains most open-source Java projects
    mavenCentral() 

    // CodeMC Repository - hosts Bukkit plugin dependencies and custom libraries
    maven("https://repo.codemc.io/repository/maven-public/")
    
    // PaperMC Repository - official repository for Paper API
    maven("https://repo.papermc.io/repository/maven-public/")
    
    // Spigot Repository - Bukkit/Spigot specific plugins and APIs
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    
    // Sonatype Snapshots - pre-release builds of many Java libraries
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    
    // JitPack - builds dependencies directly from GitHub repositories
    // Used for projects that don't publish to central repositories
    maven("https://jitpack.io")
    
    // Multiverse Repository - for Multiverse world management plugins
    maven("https://repo.onarandombox.com/content/repositories/thirdparty/")
    
    // Mythic Mobs Repository - for advanced mob framework
    maven("https://mvn.lumine.io/repository/maven/")
    
    // Vault Repository - economy/permission plugin APIs
    maven("https://repo.extendedclip.com/contents/repositories/releases/")
    
    // PlaceholderAPI Repository - placeholder expansion framework
    maven("https://repo.extendedclip.com/contents/repositories/public/")
    
    // Slimefun Repository - advanced item/machine framework
    maven("https://repo.github.com/Slimefun/Slimefun4/")
    
    // ItemsAdder Repository - custom items and blocks
    maven("https://repo.oraxen.com/releases")
    
    // Oraxen Repository - custom item framework (alternative source)
    maven("https://repo.oraxen.com/releases/")
    
    // FancyNpcs Repository - NPC framework
    maven("https://repo.oliver.media/releases")
    
    // Znpcs+ Repository - NPC framework (alternative)
    maven("https://repo.pyr.lol/releases")
    
    // FancyHolograms Repository - hologram display framework
    maven("https://repo.oliver.media/releases")
}

// ============================================================================
// DEPENDENCIES
// ============================================================================
//
// Declares all external libraries needed for compilation and runtime.
// Dependencies are organized into three scopes:
//
// 1. TEST DEPENDENCIES
//    - Used only during test compilation and execution
//    - NOT included in the final JAR
//    - Examples: JUnit, Mockito, testing frameworks
//
// 2. PROVIDED DEPENDENCIES (compileOnly)
//    - Available during compilation but NOT packaged in JAR
//    - Provided by the server at runtime
//    - Examples: Paper API, plugin APIs (Vault, PlaceholderAPI)
//    - Reduces JAR size by avoiding duplicates with server
//
// 3. IMPLEMENTATION DEPENDENCIES
//    - Compiled and bundled (shaded) into the final JAR
//    - Available at compile time and runtime
//    - Shadow plugin handles packaging and relocation
//
// Transitive Dependency Exclusions:
// - Excluded spigot-api from older plugins that would bring in outdated versions
// - Excluded Oraxen's unnecessary dependencies to reduce JAR size
//
// Translates Maven: <dependencies> and <exclusions> sections
//

dependencies {
    
    // ========================================================================
    // TEST DEPENDENCIES
    // ========================================================================
    
    // JUnit 5 (Jupiter) Bill of Materials (BOM)
    // BOM ensures all JUnit 5 artifacts are version-aligned and compatible
    // Must come before individual JUnit dependencies
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    
    // JUnit Platform Launcher - required at runtime to discover and execute tests
    // Version MUST match the engine version to avoid "OutputDirectoryCreator not available" errors
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")
    
    // JUnit 5 Jupiter - the main testing framework API and implementation
    testImplementation("org.junit.jupiter:junit-jupiter")
    
    // Mockito - mocking library for creating test doubles and verifying behavior
    // junit-jupiter variant provides automatic mock extension for @Mock annotations
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    
    // MockBukkit - in-memory Bukkit server mock
    // Allows testing Bukkit plugins without a running server
    testImplementation("com.github.MockBukkit:MockBukkit:$mockBukkitVersion")
    
    // Awaitility - utility library for testing asynchronous code
    // Provides convenient assertions with timeouts for async operations
    testImplementation("org.awaitility:awaitility:4.2.2")
    
    // Required APIs for test compilation
    // These provide classes that main code uses, needed for test compilation
    testImplementation("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    testImplementation("com.github.MilkBowl:VaultAPI:$vaultVersion")
    testImplementation("me.clip:placeholderapi:$placeholderapiVersion")
    testImplementation("commons-lang:commons-lang:2.6")

    // ========================================================================
    // PROVIDED DEPENDENCIES (compileOnly)
    // ========================================================================
    // These are available at compile time but NOT packaged into the JAR
    // The server provides them at runtime, so including them would create duplicates
    
    // Paper API - Modern Bukkit server fork with better APIs and performance
    // Note: Paper API includes Spigot API, so we don't need to include it separately
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    
    // Spigot NMS (Net Minecraft Server) - Low-level Minecraft server access
    // Used for advanced operations like chunk deletion, world cloning, etc.
    // Exclude spigot-api because Paper already provides a newer version
    compileOnly("org.spigotmc:spigot:$spigotVersion") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    
    // Database drivers - provided scope because servers might supply their own
    compileOnly("org.mongodb:mongodb-driver:$mongodbVersion")
    compileOnly("com.zaxxer:HikariCP:$hikaricpVersion") // Connection pooling library
    
    // Server plugin APIs - soft dependencies provided by other plugins on the server
    compileOnly("com.github.MilkBowl:VaultAPI:$vaultVersion") // Economy API
    compileOnly("me.clip:placeholderapi:$placeholderapiVersion") // Placeholder framework
    
    // World and island management plugins
    compileOnly("com.bergerkiller.bukkit:MyWorlds:$myworldsVersion") {
        // MyWorlds includes an old spigot-api version that would conflict
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    
    // Mythic Mobs - advanced mob framework
    compileOnly("io.lumine:Mythic-Dist:5.9.5")
    
    // Multiverse - multiple world management plugin (two versions for compatibility)
    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.0.0-SNAPSHOT") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("com.onarandombox.multiversecore:multiverse-core:4.3.16") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    
    // Language/localization utilities
    compileOnly("com.github.apachezy:LangUtils:3.2.2")
    
    // Custom items and blocks frameworks
    compileOnly("com.github.Slimefun:Slimefun4:RC-37") // Slimefun framework
    compileOnly("dev.lone:api-itemsadder:4.0.2-beta-release-11") // ItemsAdder framework
    
    // NPC and hologram frameworks
    compileOnly("de.oliver:FancyNpcs:2.4.4") // NPC creation framework
    compileOnly("lol.pyr:znpcsplus-api:2.0.0-SNAPSHOT") // Alternative NPC framework
    compileOnly("de.oliver:FancyHolograms:2.4.1") // Hologram display framework
    
    // BentoBox Level addon - provides island leveling system
    // Critical addon that extends BentoBox with leveling mechanics
    compileOnly("world.bentobox:level:2.21.3-SNAPSHOT")

    // ========================================================================
    // IMPLEMENTATION DEPENDENCIES
    // ========================================================================
    // These are compiled and shaded (bundled) into the final JAR
    // The shadow plugin includes them in the uber JAR and optionally relocates them
    
    // bStats metrics - anonymous usage statistics sent to bStats dashboard
    // Helps the BentoBox project understand usage patterns
    // Users can opt-out in their configuration
    implementation("org.bstats:bstats-bukkit:$bstatsVersion")
    
    // XML/JSON processing utilities
    implementation("javax.xml.bind:jaxb-api:2.3.0") // Java XML binding (for annotations)
    implementation("com.github.Marcono1234:gson-record-type-adapter-factory:0.3.0") // JSON serialization
    implementation("org.eclipse.jdt:org.eclipse.jdt.annotation:2.2.600") // Java annotations
    
    // Multilib - multi-library loading support from the Multipaper project
    // Allows dynamic loading of different library versions at runtime
    implementation("com.github.puregero:multilib:1.1.13")

    // Oraxen - custom item and block framework
    // Marked as compileOnly, but shadow plugin includes it during build
    compileOnly("io.th0rgal:oraxen:1.193.1") {
        // Exclude many unnecessary Oraxen dependencies to reduce JAR size
        // We only need the core API, not all the extra features
        exclude(group = "me.clip", module = "placeholderapi")
        exclude(group = "com.comphenix.protocol", module = "ProtocolLib")
        exclude(group = "me.constant", module = "ConversationLib")
        exclude(group = "com.zaxxer", module = "HikariCP")
        exclude(group = "com.github.MilkBowl", module = "VaultAPI")
        exclude(group = "com.github.seyfahni", module = "UltimateBans")
        exclude(group = "net.luckperms", module = "api")
        exclude(group = "org.postgresql", module = "postgresql")
        exclude(group = "com.mysql", module = "mysql-connector-java")
        exclude(group = "com.h2database", module = "h2")
        exclude(group = "org.mongodb", module = "mongodb-driver-core")
        exclude(group = "org.mongodb", module = "bson")
        exclude(group = "redis.clients", module = "jedis")
        exclude(group = "de.tr7zw", module = "item-nbt-api")
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "com.zaxxer", module = "HikariCP")
        exclude(group = "com.github.craftmend", module = "Openpay")
    }
}

// ============================================================================
// RESOURCE FILTERING (Placeholder Expansion)
// ============================================================================
//
// Processes resource files (plugin.yml, config.yml) to replace Maven property
// placeholders with actual values. This ensures library versions declared in
// plugin.yml match what's shaded into the JAR.
//
// When the Paper server loads this plugin, plugin.yml contains the exact library
// versions that were included in the build, enabling Paper's automatic library
// download feature to work correctly.
//
// Example:
//   Input:  libraries:
//             - 'mysql:mysql-connector-java:${mysql.version}'
//   Output: libraries:
//             - 'mysql:mysql-connector-java:8.0.27'
//
// This task runs during 'gradle assemble' and for 'shadowJar' task
//

tasks.named<ProcessResources>("processResources") {
    // Process ONLY plugin.yml and config.yml, leave other files untouched
    filesMatching(listOf("plugin.yml", "config.yml")) {
        // For each line in the matching files, perform string replacements
        filter { line ->
            line
                // Replace database driver versions with actual values
                .replace("\${mysql.version}", mysqlVersion)
                .replace("\${mariadb.version}", mariadbVersion)
                .replace("\${postgresql.version}", postgresqlVersion)
                .replace("\${mongodb.version}", mongodbVersion)
                .replace("\${hikaricp.version}", hikaricpVersion)
                // Replace project version (for about/info display)
                .replace("\${project.version}", project.version.toString())
        }
    }
}

// ============================================================================
// SHADOW JAR CONFIGURATION
// ============================================================================
//
// Configures the shadow plugin to create an "uber JAR" - a self-contained
// JAR that includes all runtime dependencies bundled together.
//
// Key features:
// - Minimization: Removes unused classes to reduce JAR size
// - Class Relocation: Moves shaded packages to prevent conflicts
// - Selective Inclusion: Only packages needed by BentoBox are included
//
// Translates Maven: <plugin>maven-shade-plugin</plugin> configuration
//

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    // Minimize the JAR by removing unused classes
    minimize()
    
    // Exclude specific artifacts that don't need to be shaded
    // These are handled by other means (server provides them, not needed, etc.)
    exclude(
        "org.junit.jupiter:junit-jupiter",
        "org.junit.jupiter:junit-jupiter-api",
        "org.mockito:mockito-core",
        "org.mockito:mockito-junit-jupiter"
    )
    
    // Relocate (rename) package names to avoid conflicts with server or other plugins
    // This prevents version conflicts when multiple plugins use the same library
    
    // bstats metrics - rename to bentobox namespace
    relocate("org.bstats", "world.bentobox.bstats")
    
    // PaperMC utility library - rename to avoid conflicts
    relocate("io.papermc.lib", "world.bentobox.papermc.lib")
    
    // Multilib library loading - rename to bentobox namespace
    relocate("com.github.puregero.multilib", "world.bentobox.multilib")
    
    // Set the manifest attribute for the main entry point (plugin loader)
    manifest {
        attributes("Main-Class" to "world.bentobox.BentoBox")
    }
}

// Make shadowJar the default JAR for building
// This ensures 'gradle build' produces the shaded JAR instead of a thin JAR
artifacts {
    archives(tasks.shadowJar)
}

// ============================================================================
// TESTING CONFIGURATION
// ============================================================================
//
// Configures JUnit 5 test execution with special Java 21 settings.
//
// Java 21 requires module access configuration for test frameworks to work:
// - --enable-preview: Enables Java preview features
// - --add-opens: Allows test frameworks to access internal JDK modules
//
// Without these flags, tests would fail with "illegal access" errors
//

tasks.named<Test>("test") {
    // Enable JUnit 5 (Jupiter) test support
    useJUnitPlatform()
    
    // Java 21 requires preview features and special module access
    // These flags allow testing frameworks to work with Java's module system
    
    // Enable preview features for Java 21
    jvmArgs("--enable-preview")
    
    // Allow dynamic agent loading needed by Mockito and other testing libraries
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    
    // Add module opens for JUnit Platform and testing frameworks
    // These modules contain classes that test frameworks need to access
    val moduleOpens = listOf(
        "java.base",
        "java.logging",
        "java.lang.management",
        "java.desktop",
        "jdk.compiler",
        "jdk.management",
        "jdk.unsupported",
        "java.instrument",
        "java.base/java.lang",
        "java.base/java.lang.invoke",
        "java.base/java.nio",
        "java.base/java.util",
        "java.base/java.util.concurrent",
        "java.base/java.util.concurrent.atomic",
        "java.base/java.util.concurrent.locks",
        "java.base/java.util.stream",
        "java.base/java.lang.reflect",
        "java.base/java.lang.module",
        "jdk.unsupported/sun.misc",
        "jdk.unsupported/sun.reflect",
        "java.base/sun.nio.ch",
        "java.base/sun.reflect.annotation"
    )
    
    // Apply all module opens for JUnit Platform runner
    moduleOpens.forEach { module ->
        jvmArgs("--add-opens", "$module=ALL-UNNAMED")
    }
}

// ============================================================================
// CODE COVERAGE (JaCoCo)
// ============================================================================
//
// Generates code coverage reports showing which lines of code were executed
// during tests. Used for SonarCloud integration to track test coverage metrics.
//
// Translates Maven: jacoco-maven-plugin configuration
//

jacoco {
    // JaCoCo tool version to use
    toolVersion = "0.8.10"
}

tasks.named<JacocoReport>("jacocoTestReport") {
    // Generate both XML (for CI) and HTML (for viewing) reports
    reports {
        xml.required = true  // For CI integration (SonarCloud)
        html.required = true // For local browsing (target/reports/jacoco/test/html/index.html)
    }
    
    // Exclude certain packages from coverage reporting
    // These are typically generated code or testing utilities
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.map { file ->
            fileTree(file) {
                // Exclude BentoBox addon implementations (covered separately per addon)
                exclude("**/addon/**")
                // Exclude generated code (annotation processors, etc.)
                exclude("**/generated/**")
            }
        }))
    }
}

// Run JaCoCo report generation after tests complete
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

// ============================================================================
// SOURCE AND JAVADOC JAR GENERATION
// ============================================================================
//
// Creates additional JAR artifacts for publication:
// - sources JAR: Contains the original Java source code
// - javadoc JAR: Contains generated API documentation
//
// These help developers using BentoBox as a library to understand the API
//

// Register task to create sources JAR
val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Register task to create javadoc JAR
val javadocJar = tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// Configure Javadoc generation
tasks.named<Javadoc>("javadoc") {
    // Disable strict Javadoc checking (many projects have missing/malformed docs)
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// ============================================================================
// MAVEN PUBLISHING CONFIGURATION
// ============================================================================
//
// Configures publication of the built artifacts (JAR + sources + javadoc)
// to the bentoboxworld Maven repository on CodeMC.
//
// Allows other projects to use BentoBox as a library via dependency declarations:
//   implementation("world.bentobox:bentobox:3.10.2")
//
// Translates Maven: <distributionManagement> and <dependencies> sections
//

publishing {
    repositories {
        // Publication target: CodeMC bentoboxworld repository
        maven {
            name = "CodeMC"
            url = uri("https://repo.codemc.io/repository/bentoboxworld/")
            
            // Publishing requires authentication via gradle.properties:
            // codeMcUsername=<username>
            // codeMcPassword=<password>
            credentials(PasswordCredentials::class)
        }
    }
    
    publications {
        // Define the Maven publication containing all artifacts
        register<MavenPublication>("mavenJava") {
            // Use the shadowJar task output as the main artifact
            artifact(tasks.shadowJar)
            
            // Include sources JAR for IDE integration
            artifact(sourcesJar)
            
            // Include javadoc JAR for API reference
            artifact(javadocJar)
            
            // Set publication coordinates
            groupId = "world.bentobox"
            artifactId = "bentobox"
            version = project.version.toString()
            
            // Define the POM (Project Object Model) structure
            // This metadata helps dependency managers understand the artifact
            pom {
                name.set("BentoBox")
                description.set("Island plugin framework for Paper")
                url.set("https://github.com/BentoBoxWorld/BentoBox")
            }
        }
    }
}

// ============================================================================
// BUILD VERIFICATION
// ============================================================================
//
// This comment block summarizes the complete build workflow:
//
// 1. SOURCES
//    └─ src/main/java/**/*.java (Java source code)
//    └─ src/main/resources/plugin.yml (Plugin metadata)
//    └─ src/main/resources/config.yml (Configuration template)
//    └─ src/test/java/**/*.java (Test source code)
//
// 2. COMPILATION
//    └─ Compile main sources against Paper API + other provided APIs
//    └─ Compile test sources against JUnit 5, Mockito, MockBukkit
//
// 3. RESOURCE FILTERING
//    └─ Replace ${mysql.version}, ${mongodb.version}, etc. in plugin.yml
//    └─ Replace ${project.version} with actual build version
//
// 4. TESTING
//    └─ Run tests with JUnit 5 + special Java 21 module configuration
//    └─ Generate coverage reports (JaCoCo)
//
// 5. SHADING
//    └─ Minimize (remove unused classes)
//    └─ Include implementation dependencies (bstats, multilib, etc.)
//    └─ Relocate packages to prevent conflicts
//    └─ Create shadowJar: build/libs/bentobox-3.10.2-...-all.jar
//
// 6. PUBLISHING
//    └─ Upload JAR, sources, javadoc to CodeMC repository
//    └─ Users can then: implementation("world.bentobox:bentobox:...")
//
