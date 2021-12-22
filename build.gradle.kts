import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `java-library`

    alias(libs.plugins.pluginyml)
    alias(libs.plugins.shadow)
}

version = "2.0.0"

the<JavaPluginExtension>().toolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()
    maven { url = uri("https://ci.athion.net/plugin/repository/tools/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://repo.codemc.io/repository/maven-public/") }
    maven { url = uri("https://papermc.io/repo/repository/maven-public/") }
    maven { url = uri("https://repo.dmulloy2.net/nexus/repository/public/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    maven { url = uri("https://mvn.intellectualsites.com/content/groups/public/") }
    maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.bundles.fawe)
    compileOnly(libs.plotsquared)
    compileOnly(libs.jsonSimple)
}

bukkit {
    name = "PlotHTTP"
    main = "com.boydti.plothttp.BukkitMain"
    authors = listOf("Empire92", "dordsor21")
    apiVersion = "1.17"
    description = "Plot HTTP Downloading"
    version = rootProject.version.toString()
    depend = listOf("PlotSquared")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set(null as String?)
    dependencies {
        relocate("net.kyori.adventure", "com.plotsquared.core.configuration.adventure")
    }
    minimize()
}

tasks.named("build").configure {
    dependsOn("shadowJar")
}
