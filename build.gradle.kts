// Apply necessary plugins for Java development, publishing, testing, and shading
plugins {
    // Standard Java development plugin
    java
    
    // For publishing the artifact (translates <distributionManagement>)
    `maven-publish` 

    // For code coverage (translates jacoco-maven-plugin)
    id("jacoco")

    // For shading, minimization, and relocation (translates maven-shade-plugin)
    id("com.gradleup.shadow") version "9.3.0"
}

// --- Project Coordinates and Versioning (Translates <groupId>, <artifactId>, <version> and <profiles>) ---

group = "world.bentobox" // From <groupId>

// Base properties from <properties>
val buildVersion = "3.10.2"
val buildNumberDefault = "-LOCAL"
val snapshotSuffix = "-SNAPSHOT"

// CI/CD Logic (Translates Maven <profiles>)
var finalBuildNumber = buildNumberDefault
var finalRevision = "$buildVersion$finalBuildNumber$snapshotSuffix"

// 'ci' profile logic: Activated by env.BUILD_NUMBER
val envBuildNumber = System.getenv("BUILD_NUMBER")
if (!envBuildNumber.isNullOrBlank()) {
    // Override only if necessary (as per POM comment)
    finalBuildNumber = "-b$envBuildNumber"
    finalRevision = "$buildVersion$finalBuildNumber$snapshotSuffix"
}

// 'master' profile logic: Activated by env.GIT_BRANCH == origin/master
val envGitBranch = System.getenv("GIT_BRANCH")
if (envGitBranch == "origin/master") {
    // Override revision to remove -SNAPSHOT and set build number to empty string
    finalBuildNumber = "" // Empties build number variable.
    finalRevision = buildVersion 
}

version = finalRevision

// --- Global Properties (Translates remaining <properties>) ---

// Dependency versions (used in dependencies block and throughout)
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

// Also store in extra properties for resource filtering
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
// Define variables used in <finalName> and build logic
extra["build.version"] = buildVersion
extra["build.number"] = finalBuildNumber
extra["revision"] = finalRevision


// --- Java Configuration ---

// Configures source/target compatibility and toolchain
java {
    // Uses Java 21 toolchain (as remembered and specified in POM)
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8" // From <project.build.sourceEncoding>
}


// --- Repositories (Translates <repositories>) ---

repositories {
    // Default repository for most libraries
    mavenCentral() 

    // Custom Repositories from POM
    maven("https://jitpack.io") { name = "JitPack" }
    maven("https://repo.codemc.org/repository/maven-public") { name = "CodeMC-Public" }
    maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
    maven("https://libraries.minecraft.net/") { name = "MinecraftLibs" }
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots") { name = "Spigot-Snapshots" }
    maven("https://repo.codemc.io/repository/nms/") { name = "NMS-Repo" }
    maven("https://ci.mg-dev.eu/plugin/repository/everything") { name = "MG-Dev-CI" }
    maven("https://repo.onarandombox.com/multiverse-releases") { name = "Multiverse-Releases" }
    maven("https://repo.onarandombox.com/multiverse-snapshots") { name = "Multiverse-Snapshots" }
    maven("https://mvn.lumine.io/repository/maven-public/") { name = "Lumine-Releases" }
    maven("https://repo.clojars.org/") { name = "Clojars" }
    maven("https://repo.fancyplugins.de/releases") { name = "FancyPlugins-Releases" }
    maven("https://repo.pyr.lol/snapshots") { name = "Pyr-Snapshots" }
    maven("https://maven.devs.beer/") { name = "MatteoDev" }
    maven("https://repo.oraxen.com/releases") { name = "Oraxen" }
    maven("https://repo.codemc.org/repository/bentoboxworld/") { name = "BentoBoxWorld-Repo" }
    maven("https://repo.extendedclip.com/releases/") { name = "Placeholder-API-Releases" }
}


// --- Dependencies (Translates <dependencies>) ---

dependencies {
    // --- Test Dependencies (<scope>test</scope>) ---
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    // Ensure JUnit Platform launcher matches the engine/platform versions
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$junitVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("com.github.MockBukkit:MockBukkit:$mockBukkitVersion")
    testImplementation("org.awaitility:awaitility:4.2.2")
    // Paper API for test compilation
    testImplementation("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    // Vault API for test compilation
    testImplementation("com.github.MilkBowl:VaultAPI:$vaultVersion")
    // PlaceholderAPI for test compilation (provides PlaceholderExpansion)
    testImplementation("me.clip:placeholderapi:$placeholderapiVersion")

    // --- Provided/Compile-Only Dependencies (<scope>provided</scope>) ---
    //compileOnly("io.papermc.paper:paper-api:$paperVersion")
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    // Note: Paper API includes Spigot API, so we don't need to include it separately
    
    // Spigot NMS - Used for chunk deletion and pasting
    compileOnly("org.spigotmc:spigot:$spigotVersion") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    compileOnly("org.mongodb:mongodb-driver:$mongodbVersion")
    compileOnly("com.zaxxer:HikariCP:$hikaricpVersion")
    compileOnly("com.github.MilkBowl:VaultAPI:$vaultVersion")
    compileOnly("me.clip:placeholderapi:$placeholderapiVersion")
    compileOnly("com.bergerkiller.bukkit:MyWorlds:$myworldsVersion") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("io.lumine:Mythic-Dist:5.9.5")
    compileOnly("org.mvplugins.multiverse.core:multiverse-core:5.0.0-SNAPSHOT") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("com.onarandombox.multiversecore:multiverse-core:4.3.16") {
        exclude(group = "org.spigotmc", module = "spigot-api")
    }
    compileOnly("com.github.apachezy:LangUtils:3.2.2")
    compileOnly("com.github.Slimefun:Slimefun4:RC-37")
    compileOnly("dev.lone:api-itemsadder:4.0.2-beta-release-11")
    compileOnly("de.oliver:FancyNpcs:2.4.4")
    compileOnly("lol.pyr:znpcsplus-api:2.0.0-SNAPSHOT")
    compileOnly("de.oliver:FancyHolograms:2.4.1")
    compileOnly("world.bentobox:level:2.21.3-SNAPSHOT")

    // Apache Commons Lang (provides NumberUtils) - available at test time
    compileOnly("commons-lang:commons-lang:2.6")
    testImplementation("commons-lang:commons-lang:2.6")

    // --- Implementation Dependencies (Default scope) ---
    implementation("org.bstats:bstats-bukkit:$bstatsVersion")
    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("com.github.Marcono1234:gson-record-type-adapter-factory:0.3.0")
    implementation("org.eclipse.jdt:org.eclipse.jdt.annotation:2.2.600")
    implementation("com.github.puregero:multilib:1.1.13")

    // Oraxen with exclusions
    compileOnly("io.th0rgal:oraxen:1.193.1") {
        // Translates <exclusions>
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


// --- Build Configuration (Translates <build> and <plugins>) ---

// Resource Filtering (Translates <resources>)
tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Copy all resources
    from(sourceSets.main.get().resources.srcDirs)
    
    // Apply filtering to plugin.yml and config.yml with direct token replacement
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

// Copy 'locales' without filtering
tasks.register<Copy>("copyLocales") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/resources/locales")
    into("${tasks.processResources.get().destinationDir}/locales")
}

// Ensure compileTestJava depends on copyLocales
tasks.compileTestJava {
    dependsOn("copyLocales")
}

// Custom finalName (Translates <finalName>)
tasks.jar {
    archiveFileName.set("${project.name}-${project.version}.jar")
}


// --- Shading Plugin Configuration (Translates maven-shade-plugin) ---

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    // Translates <minimizeJar>true</minimizeJar>
    minimize()

    // Artifact exclusion (Translates <artifactSet><excludes>)
    exclude(
        "org.apache.maven:*:*",
        "com.google.code.gson:*:*",
        "org.mongodb:*:*",
        "org.eclipse.jdt:*:*"
    )

    // Relocations (Translates <relocations>)
    relocate("org.bstats", "world.bentobox.bentobox.util.metrics")
    relocate("io.papermc.lib", "world.bentobox.bentobox.paperlib")
    relocate("com.github.puregero.multilib", "world.bentobox.bentobox.multilib")
    // Remove the "-all" suffix
    archiveClassifier.set("")
}

// Ensure the shaded jar is the primary artifact when 'build' is run
tasks.build {
    dependsOn(tasks.shadowJar)
}

// --- Testing Configuration (Translates maven-surefire-plugin) ---

tasks.test {
    useJUnitPlatform()

    // Add --add-opens from maven-surefire-plugin <argLine> for Java 21 compatibility
    val openModules = listOf(
        "java.base/java.lang", "java.base/java.math", "java.base/java.io", "java.base/java.util",
        "java.base/java.util.stream", "java.base/java.text", "java.base/java.util.regex",
        "java.base/java.nio.channels.spi", "java.base/sun.nio.ch", "java.base/java.net",
        "java.base/java.util.concurrent", "java.base/sun.nio.fs", "java.base/sun.nio.cs",
        "java.base/java.nio.file", "java.base/java.nio.charset", "java.base/java.lang.reflect",
        "java.logging/java.util.logging", "java.base/java.lang.ref", "java.base/java.util.jar",
        "java.base/java.util.zip", "java.base/java.security", "java.base/jdk.internal.misc"
    )

    jvmArgs("--enable-preview", "-XX:+EnableDynamicAgentLoading")
    for (module in openModules) {
        jvmArgs("--add-opens", "$module=ALL-UNNAMED")
    }
}

// --- JaCoCo Configuration (Translates jacoco-maven-plugin) ---

tasks.jacocoTestReport {
    reports {
        xml.required.set(true) // Used for tools like SonarCloud
        html.required.set(true)
    }

    // Translates <excludes>
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude("**/*Names*", "org/bukkit/Material*")
        }
    )
}

// --- Javadoc and Source Jar (Translates maven-javadoc-plugin & maven-source-plugin) ---

// Configures the Javadoc task
tasks.javadoc {
    source = sourceSets.main.get().allJava
    options {
        (this as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            source = javaVersion
        }
    }
}

// Creates the -sources.jar file (Translates maven-source-plugin execution)
tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

// Creates the -javadoc.jar file (Translates maven-javadoc-plugin execution)
tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

// --- Publishing (Translates <distributionManagement>) ---

// Attaches source/javadoc to the publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            // Use the shaded JAR as the main artifact
            artifact(tasks.shadowJar.get()) {
                builtBy(tasks.shadowJar)
            }
            
            // Attach sources and Javadocs
            artifact(tasks.getByName("sourcesJar"))
            artifact(tasks.getByName("javadocJar"))

            // Set coordinates
            groupId = project.group as String
            artifactId = rootProject.name
            version = project.version as String
        }
    }
    
    // Defines the repository (Translates <repository> in <distributionManagement>)
    repositories {
        maven {
            name = "bentoboxworld"
            url = uri("https://repo.codemc.org/repository/bentoboxworld/")
        }
    }
}

base {
    archivesName.set("BentoBox")
}
