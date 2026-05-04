plugins {
    id("fabric-loom") version "1.4.4"
    id("maven-publish")
}

version = "1.0.0"
group = "com.example"

base {
    archivesName.set("kingdoms-of-origin")
}

repositories {
    mavenCentral()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
    maven {
        name = "BlueMap"
        url = uri("https://repo.bluecolored.de/releases")
    }
    maven {
        name = "Dynmap"
        url = uri("https://repo.mikeprimm.com/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.20.1")
    mappings("net.fabricmc:yarn:1.20.1+build.10:v2")
    modImplementation("net.fabricmc:fabric-loader:0.15.11")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.92.2+1.20.1")

    // SQLite JDBC — bundled into the output jar
    implementation("org.xerial:sqlite-jdbc:3.45.0.0")
    include("org.xerial:sqlite-jdbc:3.45.0.0")

    // SnakeYAML — bundled into the output jar
    implementation("org.yaml:snakeyaml:2.2")
    include("org.yaml:snakeyaml:2.2")

    // Origins + transitive deps — soft/optional, compile-only; local JARs
    modCompileOnly(fileTree("libs") { include("origins-fabric-*.jar") })
    modCompileOnly(fileTree("libs") { include("cardinal-components-*.jar") })
    modCompileOnly(fileTree("libs") { include("calio-*.jar") })

    // BlueMap + Dynmap — soft/optional map integrations, compile-only
    compileOnly("de.bluecolored:bluemap-api:2.7.5")
    compileOnly("us.dynmap:DynmapCoreAPI:3.4-beta-3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

configurations.matching { it.isCanBeResolved }.configureEach {
    attributes {
        attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${base.archivesName.get()}" }
    }
}
