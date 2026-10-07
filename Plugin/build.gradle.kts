import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    id("java")
    // goooler fork (8.1.8) is broken on Gradle 9+; gradleup 9.x is the maintained fork
    // with ASM support for Java 25 class files (velocity-api 4 ships major version 69).
    id("com.gradleup.shadow") version "9.6.1"
    id("java-library")
    // Built from the vendored sources in ../buildSrc: the artifact this plugin id resolves to
    // (libby-gradle-plugin:plugin:1.2.1) is 404 on every public repository, so it used to be
    // installed by hand into ~/.m2 and the build only worked on that one machine.
    id("xyz.kyngs.libby.plugin")
    id("xyz.kyngs.mcupload.plugin").version("0.3.4")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

mcupload {
    file = tasks.shadowJar
    swallowErrors = true
    platforms {
        modrinth {
            loaders = listOf("paper", "purpur", "bungeecord", "waterfall", "velocity")
            projectId = "tL0SCXYq"
            gameVersions = listOf(
                "1.21.4", "1.21.3", "1.21.2", "1.21.1", "1.21",
                "1.20.6", "1.20.5", "1.20.4", "1.20.3", "1.20.2", "1.20.1", "1.20",
                "1.19.4", "1.19.3", "1.19.2", "1.19.1", "1.19",
                "1.18.2", "1.18.1", "1.18",
                "1.17.1", "1.17",
                "1.16.5", "1.16.4", "1.16.3", "1.16.2", "1.16.1", "1.16",
                "1.15.2", "1.15.1", "1.15",
                "1.14.4", "1.14.3", "1.14.2", "1.14.1", "1.14",
                "1.13.2", "1.13.1", "1.13",
            )
            token = System.getenv("MODRINTH_TOKEN")
        }
        polymart {
            apiKey = System.getenv("POLYMART_TOKEN")
            resourceId = "2179"
        }
        github {
            token = System.getenv("GITHUB_TOKEN")
            repository = "kyngs/LibreLogin"
        }
        discord {
            webhookUrl = System.getenv("DISCORD_WEBHOOK_URL")
            configureEmbed {
                setColor(0x0398FC)
            }
        }
    }
    datasource {
        file {
            readmeFile = "README.md"
            changelogFile = "CHANGELOG.md"
        }
    }
}

repositories {
    // mavenLocal()
    maven { url = uri("https://repo.opencollab.dev/maven-snapshots/") }
    maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
    maven { url = uri("https://hub.spigotmc.org/nexus/") }
    maven { url = uri("https://repo.kyngs.xyz/public/") }
    maven { url = uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/") }
    maven { url = uri("https://repo.dmulloy2.net/repository/public/") }
    maven { url = uri("https://repo.alessiodp.com/releases/") }
    maven { url = uri("https://jitpack.io/") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
}

// Blossom (net.kyori.blossom) 1.3.1 still uses the JavaPluginConvention that Gradle 9 removed,
// and its 2.x rewrite dropped replaceToken(), so the only token this build needs
// (@version@ in VelocityBootstrap) is substituted by a plain copy of the source tree.
val replaceVersionTokens = tasks.register<Copy>("replaceVersionTokens") {
    from("src/main/java")
    into(layout.buildDirectory.dir("generated/version-tokens"))
    filteringCharset = "UTF-8"
    filter(ReplaceTokens::class, "tokens" to mapOf("version" to version.toString()))
}

sourceSets {
    main {
        // Replaces the default src/main/java with the same tree, tokens substituted.
        java.setSrcDirs(listOf(replaceVersionTokens))
    }
}

// Single source of truth for the package relocations: Shadow applies them to the libraries
// bundled into the jar, and Libby has to apply the same ones to the libraries it downloads
// at runtime, otherwise the two sets of classes end up in different packages.
val libraryRelocations = mapOf(
    "co.aikar.acf" to "xyz.kyngs.librelogin.lib.acf",
    "com.github.benmanes.caffeine" to "xyz.kyngs.librelogin.lib.caffeine",
    "com.typesafe.config" to "xyz.kyngs.librelogin.lib.hocon",
    "com.zaxxer.hikari" to "xyz.kyngs.librelogin.lib.hikari",
    "org.mariadb" to "xyz.kyngs.librelogin.lib.mariadb",
    "org.bstats" to "xyz.kyngs.librelogin.lib.metrics",
    "org.intellij" to "xyz.kyngs.librelogin.lib.intellij",
    "org.jetbrains" to "xyz.kyngs.librelogin.lib.jetbrains",
    "io.leangen.geantyref" to "xyz.kyngs.librelogin.lib.reflect",
    "org.spongepowered.configurate" to "xyz.kyngs.librelogin.lib.configurate",
    "net.byteflux.libby" to "xyz.kyngs.librelogin.lib.libby",
    "org.postgresql" to "xyz.kyngs.librelogin.lib.postgresql",
    "com.github.retrooper.packetevents" to "xyz.kyngs.librelogin.lib.packetevents.api",
    "io.github.retrooper.packetevents" to "xyz.kyngs.librelogin.lib.packetevents.platform",
)

tasks.withType<ShadowJar> {
    archiveFileName.set("LibreLogin.jar")

    dependencies {
        exclude(dependency("org.slf4j:.*:.*"))
        exclude(dependency("org.checkerframework:.*:.*"))
        exclude(dependency("com.google.errorprone:.*:.*"))
        exclude(dependency("com.google.protobuf:.*:.*"))
    }

    libraryRelocations.forEach { (from, to) -> relocate(from, to) }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<Jar> {
    from("../LICENSE.txt")
}

libby {
    excludeDependency("org.slf4j:.*:.*")
    excludeDependency("org.checkerframework:.*:.*")
    excludeDependency("com.google.errorprone:.*:.*")
    excludeDependency("com.google.protobuf:.*:.*")

    // Often redeploys the same version, so calculating checksum causes false flags
    noChecksumDependency("com.github.retrooper.packetevents:.*:.*")

    libraryRelocations.forEach { (from, to) -> relocate(from, to) }
}

dependencies {
    //API
    implementation(project(":API"))

    //Velocity
    // NOTE: The proxy configuration (player info forwarding) is accessed via reflection because
    // PaperMC no longer publishes the velocity-proxy artifact, and the public ProxyConfig API
    // does not expose the forwarding mode/secret.
    annotationProcessor("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")

    //MySQL
    libby("org.mariadb.jdbc:mariadb-java-client:3.5.1")
    libby("com.zaxxer:HikariCP:6.2.1")

    //SQLite
    libby("org.xerial:sqlite-jdbc:3.47.1.0")

    //PostgreSQL
    libby("org.postgresql:postgresql:42.7.5")

    //ACF
    libby("com.github.kyngs.commands:acf-velocity:7d5bf7cac0")
    libby("com.github.kyngs.commands:acf-bungee:7d5bf7cac0")
    libby("com.github.kyngs.commands:acf-paper:7d5bf7cac0")

    //Utils
    libby("com.github.ben-manes.caffeine:caffeine:3.2.0")
    libby("org.spongepowered:configurate-hocon:4.1.2")
    libby("at.favre.lib:bcrypt:0.10.2")
    libby("dev.samstevens.totp:totp:1.7.1")
    compileOnly("dev.simplix:protocolize-api:2.4.2")
    libby("org.bouncycastle:bcprov-jdk18on:1.80")
    libby("org.apache.commons:commons-email:1.6.0")
    // DO NOT UPGRADE TO 4.15.0 OR ABOVE BEFORE TESTING WATERFALL AND BUNGEECORD COMPATIBILITY!!!
    libby("net.kyori:adventure-text-minimessage:5.2.0")
    libby("com.github.kyngs:LegacyMessage:0.2.0")

    //Geyser
    compileOnly("org.geysermc.floodgate:api:2.2.0-SNAPSHOT")
    //LuckPerms
    compileOnly("net.luckperms:api:5.4")

    //Bungeecord
    compileOnly("net.md-5:bungeecord-api:26.1-R0.1-SNAPSHOT")
    compileOnly("com.github.ProxioDev.ValioBungee:RedisBungee-Bungee:0.12.5")
    libby("net.kyori:adventure-platform-bungeecord:4.1.2")

    //BStats
    libby("org.bstats:bstats-velocity:3.0.2")
    libby("org.bstats:bstats-bungeecord:3.0.2")
    libby("org.bstats:bstats-bukkit:3.0.2")

    //Paper
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    //compileOnly "com.comphenix.protocol:ProtocolLib:5.1.0"
    libby("com.github.retrooper:packetevents-spigot:2.7.0")
    compileOnly("io.netty:netty-transport:4.1.108.Final")
    compileOnly("com.mojang:datafixerupper:5.0.28") //I hate this so much
    compileOnly("org.apache.logging.log4j:log4j-core:2.23.1")

    //Libby
    implementation("xyz.kyngs.libby:libby-bukkit:1.6.0")
    implementation("xyz.kyngs.libby:libby-velocity:1.6.0")
    implementation("xyz.kyngs.libby:libby-bungee:1.6.0")
    implementation("xyz.kyngs.libby:libby-paper:1.6.0")

    //NanoLimboPlugin
    compileOnly("com.github.bivashy.NanoLimboPlugin:api:1.0.15")
}

tasks.withType<ProcessResources> {
    outputs.upToDateWhen { false }
    filesMatching("plugin.yml") {
        expand(mapOf("version" to version))
    }
    filesMatching("bungee.yml") {
        expand(mapOf("version" to version))
    }
    filesMatching("paper-plugin.yml") {
        expand(mapOf("version" to version))
    }
}