plugins {
    `java-library`
    java
    id("io.freefair.lombok") version "8.11" apply false
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18" apply false
}

allprojects {
    group = "com.glance.codex"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.triumphteam.dev/snapshots")
        maven("https://jitpack.io")
        maven("https://repo.extendedclip.com/releases/")
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    apply(plugin = "io.freefair.lombok")
}