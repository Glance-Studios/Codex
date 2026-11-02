// Spellbrook's own wiring for the journal, kept out of codex-paper so the core plugin carries no
// mention of their plugins. Shaded into the spellbrook variant jar rather than installed beside it.

repositories {
    // Declared here and never in codex-paper: plugin-yml writes every repository it can see into
    // paper-libraries.json, and a credentialed one would be handed to the resolver on the server.
    maven("https://maven.pkg.github.com/Glance-Studios/CollectableCodexAPI") {
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GPR_USER")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GPR_TOKEN")
        }
    }
    maven("https://maven.pkg.github.com/Spellbrook/Plugins") {
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.codex.api)
    compileOnly(libs.guice)

    // The magic events. The published artifact is the thin jar, and the classes it references from
    // gg.spellbrook.common live in the shaded jar the server runs, hence both.
    compileOnly(libs.spellbrook.magic)
    compileOnly(libs.spellbrook.common)

    compileOnly(libs.auto.service.annotations)
    annotationProcessor(libs.auto.service)
}

tasks.withType<JavaCompile> {
    options.release.set(21)
    options.encoding = Charsets.UTF_8.name()
}
