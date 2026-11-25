import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.util.zip.ZipFile
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
    implementation(libs.triumph.gui)

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
    // Resolved at runtime rather than shaded, so the MySQL backend needs no jar variant of its own
    paperLibrary(libs.mariadb)

    compileOnly(libs.sqlite)

    // The SQL layer is the one part testable off a server, so the drivers come back for tests
    testImplementation(libs.junit.jupiter)
    // Gson and the Bukkit types the storage classes name are on the server classpath, not ours
    testImplementation(libs.paper.api)
    testImplementation(libs.jdbi.core)
    testImplementation(libs.jdbi.sqlobject)
    testImplementation(libs.hikari)
    testRuntimeOnly(libs.sqlite)
    testRuntimeOnly(libs.mariadb)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
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
    minimize {
        exclude(dependency("dev.triumphteam:.*:.*"))
        // The driver is only ever loaded by name, so minimization drops it and leaves the
        // native libraries behind with nothing able to load them
        exclude(dependency("org.xerial:sqlite-jdbc:.*"))
    }
}

// Setup Spellbrook Build - the SQLite jar plus the module that knows their magic events.
// One file on the server, and the core plugin stays free of any mention of SB-Magic.
val spellbrook: Configuration by configurations.creating
dependencies { spellbrook(project(":codex-spellbrook")) }

val shadowWithSpellbrook by tasks.registering(ShadowJar::class) {
    group = "build"
    archiveClassifier.set("spellbrook")
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get(), sqlite, spellbrook)
    // Without this the module's service entry is overwritten by this project's rather than
    // added to it, and a manager nothing lists is a manager that never runs
    mergeServiceFiles()
    minimize {
        exclude(dependency("dev.triumphteam:.*:.*"))
        exclude(dependency("org.xerial:sqlite-jdbc:.*"))
        // Nothing references the module by name, so minimization would drop it and leave a
        // service entry pointing at a class that is not there
        exclude(project(":codex-spellbrook"))
    }
}

val librariesJson = layout.buildDirectory.file("generated/plugin-yml/Paper/paper-libraries.json")

tasks {
    build {
        dependsOn(shadowJar)
        dependsOn(shadowWithSQLite)
        dependsOn(shadowWithSpellbrook)
        finalizedBy("exportJars")
    }

    /**
     * Strip the API registry back out of the generated libraries list.
     *
     * plugin-yml writes every declared repository into paper-libraries.json, and the loader hands
     * each one to the Maven resolver at boot. That registry needs a token no server has, so left
     * in it turns every startup into a failed authenticated lookup.
     */
    named("generatePaperPluginDescription") {
        doLast {
            val file = librariesJson.get().asFile
            if (!file.isFile) return@doLast

            @Suppress("UNCHECKED_CAST")
            val root = JsonSlurper().parse(file) as MutableMap<String, Any>

            @Suppress("UNCHECKED_CAST")
            val repositories = root["repositories"] as? MutableMap<String, Any> ?: return@doLast

            repositories.entries.removeIf { (_, url) ->
                url.toString().startsWith("https://maven.pkg.github.com")
            }
            file.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(root)))
        }
    }

    /**
     * Fail the build rather than ship a jar whose driver cannot be fetched.
     *
     * The MySQL backend now refuses to start without its driver, so a libraries list the server
     * cannot read turns a silent fallback into a plugin that does not enable.
     */
    val verifyLibraries = register("verifyLibraries") {
        dependsOn("generatePaperPluginDescription")
        val file = librariesJson

        doLast {
            val text = file.get().asFile.readText()
            check(!text.contains("maven.pkg.github.com")) {
                "The API registry is in paper-libraries.json - servers have no token for it"
            }
            check(text.contains("mariadb-java-client")) {
                "The MariaDB driver is missing from paper-libraries.json, so the MySQL backend " +
                    "would have no driver to resolve"
            }
        }
    }

    shadowJar {
        archiveClassifier.set("")
        minimize {
            exclude(dependency("dev.triumphteam:.*:.*"))
        }
        dependencies { exclude(dependency("org.xerial:sqlite-jdbc")) }
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = Charsets.UTF_8.name()
        options.compilerArgs = listOf("-parameters")
    }

    /**
     * Fail the build rather than ship a variant whose driver was minimized away.
     *
     * The driver is only ever loaded by name, so nothing references it by symbol and the shadow
     * minimizer is free to drop it while keeping its native libraries.
     */
    val verifyDrivers = register("verifyDrivers") {
        dependsOn(shadowWithSQLite, shadowWithSpellbrook)
        val jars = listOf(
            shadowWithSQLite.flatMap { it.archiveFile },
            shadowWithSpellbrook.flatMap { it.archiveFile }
        )

        doLast {
            jars.forEach { jar ->
                val file = jar.get().asFile
                ZipFile(file).use { zip ->
                    check(zip.getEntry("org/sqlite/JDBC.class") != null) {
                        "${file.name} bundles the SQLite native libraries but not the driver itself"
                    }
                }
            }
        }
    }

    register<Copy>("exportJars") {
        val shadowJar = named<ShadowJar>("shadowJar")
        val shadowWithSqlite = named<ShadowJar>("shadowWithSQLite")
        val shadowSpellbrook = named<ShadowJar>("shadowWithSpellbrook")

        dependsOn(verifyLibraries, verifyDrivers, shadowJar, shadowWithSqlite, shadowSpellbrook)

        from(
            shadowJar.flatMap { it.archiveFile },
            shadowWithSqlite.flatMap { it.archiveFile },
            shadowSpellbrook.flatMap { it.archiveFile }
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
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        // Only the spellbrook build has anything that uses this, and it is optional there
        // too. joinClasspath because a probe for its event classes fails without it.
        register("SB-Magic") {
            required = false
            joinClasspath = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}

