import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.indra)
    alias(libs.plugins.plugin.yml)
    alias(libs.plugins.run.paper)
}

repositories {
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
    compileOnly(libs.paper.api)

    // Codex API
    implementation(libs.codex.api)
    // OR (when local dev)
    // implementation(project(":codex-api"))

    // GUI
    paperLibrary(libs.triumph.gui)

    // Commands
    paperLibrary(libs.cloud.paper)
    paperLibrary(libs.cloud.annotations)

    // DI
    paperLibrary(libs.guice)
    paperLibrary(libs.guice.assisted)
    annotationProcessor(libs.auto.service)
    compileOnly(libs.auto.service.annotations)

    // PAPI
    compileOnly(libs.placeholderapi)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Storage
    paperLibrary(libs.jdbi.core)
    paperLibrary(libs.jdbi.sqlobject)
    paperLibrary(libs.hikari)

    compileOnly(libs.sqlite)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Setup SQLite Build
val sqlite: Configuration by configurations.creating
dependencies { sqlite(libs.sqlite) }

val shadowWithSQLite by tasks.registering(ShadowJar::class) {
    group = "build"
    archiveClassifier.set("sqlite")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get(), sqlite)
    minimize()
}

tasks {
    build {
        dependsOn(shadowJar)
        dependsOn(shadowWithSQLite)
        finalizedBy("exportJars")
    }

    shadowJar {
        archiveClassifier.set("")
        minimize()
        dependencies { exclude(dependency("org.xerial:sqlite-jdbc")) }
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs = listOf("-parameters")
    }

    register<Copy>("exportJars") {
        val shadowJar = named<ShadowJar>("shadowJar")
        val shadowWithSqlite = named<ShadowJar>("shadowWithSQLite")

        dependsOn(shadowJar, shadowWithSqlite)

        from(
            shadowJar.flatMap { it.archiveFile },
            shadowWithSqlite.flatMap { it.archiveFile }
        )

        into(rootProject.layout.projectDirectory.dir("target"))
    }
}

paper {
    name = "CollectablesCodex"

    apiVersion = "1.21"
    //version = "Git-${indraGit.commit()?.name?.take(7) ?: "unknown"}"
    version = "1.0.0"

    main = "com.glance.codex.platform.paper.CodexPlugin"

    loader = "com.glance.codex.bootstrap.CodexLibLoader"
    generateLibrariesJson = true

    serverDependencies {
        register("PlaceholderAPI") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
        }
    }
}

