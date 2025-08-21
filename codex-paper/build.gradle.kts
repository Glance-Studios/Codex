import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.paper.PaperPluginDescription
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    id("net.kyori.indra.git") version "3.1.3"

    // Paper environment
    id("io.papermc.paperweight.userdev")
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.triumphteam.dev/snapshots")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")

    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Glance-Studios/CollectableCodexAPI")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GPR_TOKEN")
        }
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")

    // Codex API
    //implementation(project(":codex-api"))
    implementation("com.glance.codex:codex-api:1.0.0")

    // Menus
    implementation("dev.triumphteam:triumph-gui-paper:4.0.0-SNAPSHOT")

    // Commands
    implementation("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("org.incendo:cloud-annotations:2.0.0")

    // DI
    implementation("com.google.inject:guice:7.0.0")
    implementation("com.google.inject.extensions:guice-assistedinject:7.0.0")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    // PAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Storage
    implementation("org.jdbi:jdbi3-core:3.49.5")
    implementation("org.jdbi:jdbi3-sqlobject:3.49.5")
    implementation("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.46.0.1")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Setup SQLite Build
val sqlite: Configuration by configurations.creating
dependencies { sqlite("org.xerial:sqlite-jdbc:3.46.0.1") }

val shadowWithSQLite by tasks.registering(ShadowJar::class) {
    group = "build"
    archiveClassifier.set("with-sqlite")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get(), sqlite)
    minimize()
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
        minimize()
        dependencies { exclude(dependency("org.xerial:sqlite-jdbc")) }
    }

    runServer {
        minecraftVersion("1.21.6")
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs = listOf("-parameters")
    }
}

tasks.named<RunServer>("runServer") {
    systemProperty("com.mojang.eula.agree", "true")
}

configure<PaperPluginDescription> {
    name = "CollectablesCodex"

    apiVersion = "1.21"
    //version = "Git-${indraGit.commit()?.name?.take(7) ?: "unknown"}"
    version = "1.0.0"

    main = "com.glance.codex.platform.paper.CodexPlugin"

    serverDependencies {
        create("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
        }
    }
}